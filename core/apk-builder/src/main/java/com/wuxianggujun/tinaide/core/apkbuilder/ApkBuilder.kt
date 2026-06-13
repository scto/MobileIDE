package com.wuxianggujun.tinaide.core.apkbuilder

import android.content.Context
import com.wuxianggujun.tinaide.core.i18n.Strings
import com.wuxianggujun.tinaide.core.i18n.strOr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Builds an APK by injecting user-compiled .so files into a template APK,
 * patching the binary AndroidManifest.xml, aligning, and signing.
 *
 * Flow:
 * 1. Extract template APK
 * 2. Patch AndroidManifest.xml (package name, app name)
 * 3. Inject .so files into lib/<abi>/
 * 4. Repackage into unsigned APK
 * 5. Align (zipalign)
 * 6. Sign (v2 + v3)
 */
class ApkBuilder(private val context: Context) {

    companion object {
        private const val TAG = "ApkBuilder"
        internal const val TERMINAL_EXECUTABLE_ASSET_PATH = "assets/tinaide_terminal/executable.bin"

        internal fun shouldSkipTemplateLibEntry(
            entryName: String,
            replacementLibraryNames: Set<String>
        ): Boolean {
            if (!entryName.startsWith("lib/")) return false
            val entryFileName = entryName.substringAfterLast("/")
            return entryFileName in replacementLibraryNames
        }
    }

    private val template = ApkTemplate(context)

    sealed class BuildProgress {
        data class Step(val message: String, val progress: Float) : BuildProgress()
        data class Success(val apkFile: File) : BuildProgress()
        data class Error(val message: String, val cause: Throwable? = null) : BuildProgress()
    }

    suspend fun build(
        config: ApkBuildConfig,
        outputApk: File,
        onProgress: (BuildProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (config.templateType == ApkTemplateType.TERMINAL && config.executableFile == null) {
                return@withContext Result.failure(
                    IllegalStateException(Strings.apk_builder_terminal_executable_missing.strOr(context))
                )
            }

            onProgress(BuildProgress.Step(Strings.apk_builder_step_prepare_template.strOr(context), 0.1f))
            val templateFile = config.templateFile?.takeIf(File::isFile)
                ?: template.getTemplateFile(config.templateType)
                ?: return@withContext Result.failure(
                    IllegalStateException(
                        Strings.apk_builder_template_missing.strOr(
                            context,
                            config.templateFile?.name ?: config.templateType.templateFileName
                        )
                    )
                )

            val workDir = File(context.cacheDir, "apk_build_${System.currentTimeMillis()}")
            workDir.mkdirs()

            try {
                val unsignedApk = File(workDir, "unsigned.apk")
                val alignedApk = File(workDir, "aligned.apk")

                val iconOverride = resolveIconOverride(templateFile, config)

                val injectMessage = if (config.templateType == ApkTemplateType.TERMINAL) {
                    Strings.apk_builder_step_inject_terminal_payload.strOr(context)
                } else {
                    Strings.apk_builder_step_inject_so.strOr(context)
                }
                onProgress(BuildProgress.Step(injectMessage, 0.3f))
                repackageWithArtifacts(templateFile, unsignedApk, config, iconOverride)

                onProgress(BuildProgress.Step(Strings.apk_builder_step_align.strOr(context), 0.6f))
                ApkAligner.align(unsignedApk, alignedApk)

                onProgress(BuildProgress.Step(Strings.apk_builder_step_sign.strOr(context), 0.8f))
                val keyStoreInfo = when (val signingConfig = config.signingConfig) {
                    ApkSigningConfig.Debug -> DebugKeyStore.getOrInstall(context)
                        ?: return@withContext Result.failure(
                            IllegalStateException(
                                Strings.apk_builder_debug_keystore_unavailable.strOr(context)
                            )
                        )
                    is ApkSigningConfig.Custom -> signingConfig.keyStoreInfo
                }

                outputApk.parentFile?.mkdirs()
                ApkSigner.sign(alignedApk, outputApk, keyStoreInfo)

                onProgress(BuildProgress.Step(Strings.apk_builder_step_completed.strOr(context), 1.0f))
                onProgress(BuildProgress.Success(outputApk))
                Timber.tag(TAG).i("APK built: ${outputApk.absolutePath} (${outputApk.length()} bytes)")
                Result.success(outputApk)
            } finally {
                workDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "APK build failed")
            onProgress(BuildProgress.Error(Strings.apk_builder_failed.strOr(context, e.message ?: ""), e))
            Result.failure(e)
        }
    }

    private fun repackageWithArtifacts(
        templateApk: File,
        outputApk: File,
        config: ApkBuildConfig,
        iconOverride: IconOverride
    ) {
        val replacementLibraryNames = buildSet {
            config.soFiles.forEach { add(it.name) }
            config.sdlLibraryPath?.let { add(it.name) }
            config.preloadLibraries.forEach { add(it.name) }
        }
        ZipFile(templateApk).use { zipIn ->
            FileOutputStream(outputApk).use { fos ->
                ZipOutputStream(fos).use { zipOut ->
                    val writtenEntries = mutableSetOf<String>()

                    val entries = zipIn.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val originalData = zipIn.getInputStream(entry).use { it.readBytes() }

                        val outputData = when {
                            entry.name == "AndroidManifest.xml" ->
                                ManifestPatcher.patch(originalData, config)
                            iconOverride.appliesTo(entry.name) -> iconOverride.bytes
                            else -> originalData
                        }

                        // Skip template lib/ entries that will be replaced
                        if (shouldSkipTemplateLibEntry(entry.name, replacementLibraryNames)) {
                            continue
                        }
                        if (config.executableFile != null && entry.name == TERMINAL_EXECUTABLE_ASSET_PATH) {
                            continue
                        }

                        writeEntry(zipOut, entry.name, outputData, isStoredEntry(entry))
                        writtenEntries.add(entry.name)
                    }

                    // Inject user .so files
                    for (abi in config.targetAbis) {
                        for (soFile in config.soFiles) {
                            val entryName = "lib/$abi/${soFile.name}"
                            if (entryName !in writtenEntries) {
                                Timber.tag(TAG).d("Injecting: $entryName")
                                writeEntry(zipOut, entryName, soFile.readBytes(), stored = true)
                                writtenEntries.add(entryName)
                            }
                        }

                        // Inject SDL library if needed
                        config.sdlLibraryPath?.let { sdlLib ->
                            val entryName = "lib/$abi/${sdlLib.name}"
                            if (entryName !in writtenEntries) {
                                Timber.tag(TAG).d("Injecting SDL: $entryName")
                                writeEntry(zipOut, entryName, sdlLib.readBytes(), stored = true)
                                writtenEntries.add(entryName)
                            }
                        }

                        // Inject preload libraries
                        for (preload in config.preloadLibraries) {
                            val entryName = "lib/$abi/${preload.name}"
                            if (entryName !in writtenEntries) {
                                Timber.tag(TAG).d("Injecting preload: $entryName")
                                writeEntry(zipOut, entryName, preload.readBytes(), stored = true)
                                writtenEntries.add(entryName)
                            }
                        }
                    }

                    config.executableFile?.let { executable ->
                        if (TERMINAL_EXECUTABLE_ASSET_PATH !in writtenEntries) {
                            Timber.tag(TAG).d("Injecting terminal executable: %s", executable.absolutePath)
                            writeEntry(
                                zipOut,
                                TERMINAL_EXECUTABLE_ASSET_PATH,
                                executable.readBytes(),
                                stored = true
                            )
                            writtenEntries.add(TERMINAL_EXECUTABLE_ASSET_PATH)
                        }
                    }
                }
            }
        }
    }

    private fun writeEntry(
        zipOut: ZipOutputStream,
        name: String,
        data: ByteArray,
        stored: Boolean
    ) {
        val entry = ZipEntry(name)
        if (stored) {
            entry.method = ZipEntry.STORED
            entry.size = data.size.toLong()
            entry.compressedSize = data.size.toLong()
            val crc = CRC32()
            crc.update(data)
            entry.crc = crc.value
        } else {
            entry.method = ZipEntry.DEFLATED
        }
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }

    private fun isStoredEntry(entry: ZipEntry): Boolean =
        entry.method == ZipEntry.STORED

    private fun resolveIconOverride(
        templateApk: File,
        config: ApkBuildConfig
    ): IconOverride {
        val iconFile = config.iconFile ?: return IconOverride.EMPTY
        val paths = TemplateIconPatcher.findLauncherIconPaths(templateApk)
        if (paths.isEmpty()) {
            Timber.tag(TAG).w("Icon override requested but template has no launcher icon entries; skipping")
            return IconOverride.EMPTY
        }
        val bytes = try {
            IconRasterizer.rasterize(iconFile)
        } catch (error: Exception) {
            Timber.tag(TAG).w(error, "Failed to rasterize icon, falling back to template default")
            return IconOverride.EMPTY
        }
        Timber.tag(TAG).d("Icon override applied for paths=%s (%d bytes)", paths.joinToString(","), bytes.size)
        return IconOverride(paths, bytes)
    }

    private data class IconOverride(val paths: Set<String>, val bytes: ByteArray) {
        fun appliesTo(entryName: String): Boolean = entryName in paths

        companion object {
            val EMPTY = IconOverride(emptySet(), ByteArray(0))
        }
    }

    /** Check if a template is available for the given type. */
    fun isTemplateAvailable(type: ApkTemplateType): Boolean =
        template.isTemplateAvailable(type)
}
