// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.build

import android.content.Context

import com.Day.Studio.Function.ApkXmlEditor
import com.Day.Studio.Function.axmleditor.decode.AXMLDoc
import com.Day.Studio.Function.axmleditor.editor.PermissionEditor

import com.mobile.ide.core.utils.LogCatcher

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkBuilder {
    private const val OLD_PACKAGE_NAME = "com.web.webapp"
    private const val ICON_RES_1 = "res/MO.webp"
    private const val ICON_RES_2 = "res/fq.webp"

    private data class AppConfig(
        var appName: String = "WebApp",
        var appPackage: String = "com.example.webapp",
        var versionName: String = "1.0.0",
        var versionCode: String = "1",
        var iconPath: String? = null,
        val permissions: MutableList<String> = mutableListOf()
    )

    fun bin(
        context: Context,
        mRootDir: String,
        projectPath: String,
        aname: String,
        pkg: String,
        ver: String,
        code: String,
        amph: String?,
        ps: Array<String>?
    ): String {
        val bf = File(projectPath, "build").apply { if (!exists()) mkdirs() }

        val templateApk = File(context.cacheDir, "webapp_template.apk")
        val rawZipFile = File(bf, "temp_raw.zip")
        val alignedZipFile = File(bf, "temp_aligned.apk")
        val finalApkFile = File(bf, "${aname}_release.apk")

        LogCatcher.i("ApkBuilder", "========== Starting WebApp build ==========")

        try {
            rawZipFile.delete()
            alignedZipFile.delete()
            finalApkFile.delete()

            val config = AppConfig(
                appName = aname,
                appPackage = pkg,
                versionName = ver,
                versionCode = code,
                iconPath = if (!amph.isNullOrEmpty() && File(amph).exists()) amph else null
            ).apply {
                ps?.let { permissions.addAll(it) }
            }

            if (!copyAssetFile(context, "webapp_1.0.apk", templateApk)) {
                return "error: Build template not found (assets/webapp_1.0.apk)"
            }

            LogCatcher.i("ApkBuilder", ">> Merging resources...")
            mergeApk(templateApk, rawZipFile, projectPath, config)

            if (rawZipFile.length() < 1000) {
                return "error: Build failed, generated package is too small"
            }

            LogCatcher.i("ApkBuilder", ">> Running ZipAlign...")
            try {
                ZipAligner.align(rawZipFile, alignedZipFile)
            } catch (e: Exception) {
                return "error: Alignment failed - ${e.message}"
            }

            LogCatcher.i("ApkBuilder", ">> Signing...")
            val signaturePath = File(mRootDir, "WebIDE.jks").absolutePath.let { path ->
                val keyFile = File(path)
                if (keyFile.exists()) path
                else {
                    val internalKey = File(context.filesDir, "WebIDE.jks")
                    if (!internalKey.exists()) copyAssetFile(context, "WebIDE.jks", internalKey)
                    internalKey.absolutePath
                }
            }

            val signResult = signerApk(
                signaturePath, "WebIDE", "WebIDE", "WebIDE",
                alignedZipFile.absolutePath,
                finalApkFile.absolutePath
            )

            rawZipFile.delete()
            alignedZipFile.delete()

            return if (signResult && finalApkFile.length() > 0) {
                LogCatcher.i("ApkBuilder", "✅ Build successful: ${finalApkFile.absolutePath}")
                finalApkFile.absolutePath
            } else {
                "error: Signing failed"
            }
        } catch (e: Exception) {
            LogCatcher.e("ApkBuilder", "❌ Build crashed", e)
            return "error: ${e.message}"
        }
    }

    private fun mergeApk(templateFile: File, outputFile: File, projectPath: String, config: AppConfig) {
        ZipFile(templateFile).use { zipFile ->
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                zos.setLevel(5)

                zipFile.getEntry("resources.arsc")?.let { copyAsStored(zipFile, it, zos) }

                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    if (name == "resources.arsc" || name.startsWith("META-INF/") || name.startsWith("assets/")) continue

                    if (name == "AndroidManifest.xml") {
                        processManifest(zipFile, entry, zos, config)
                        continue
                    }

                    if (config.iconPath != null && (name == ICON_RES_1 || name == ICON_RES_2)) {
                        zos.putNextEntry(ZipEntry(name))
                        FileInputStream(File(config.iconPath!!)).use { fis -> copyStream(fis, zos) }
                        zos.closeEntry()
                        continue
                    }

                    zos.putNextEntry(ZipEntry(name))
                    zipFile.getInputStream(entry).use { isStream -> copyStream(isStream, zos) }
                    zos.closeEntry()
                }

                File(projectPath, "src/main/assets").takeIf { it.exists() && it.isDirectory }?.let {
                    addProjectFilesRecursively(zos, it, "assets")
                }

                File(projectPath, "webapp.json").takeIf { it.exists() }?.let { configFile ->
                    LogCatcher.i("ApkBuilder", "Packing configuration file: webapp.json")
                    zos.putNextEntry(ZipEntry("assets/webapp.json"))
                    FileInputStream(configFile).use { fis -> copyStream(fis, zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun processManifest(zipFile: ZipFile, entry: ZipEntry, zos: ZipOutputStream, config: AppConfig) {
        val tempManifest = File.createTempFile("TempManifest", ".xml")
        zipFile.getInputStream(entry).use { inputStream ->
            FileOutputStream(tempManifest).use { fos -> inputStream.copyTo(fos) }
        }

        try {
            ApkXmlEditor.setXmlPaht(tempManifest.absolutePath)
            ApkXmlEditor.setAppName(config.appName)
            ApkXmlEditor.setAppPack(config.appPackage)
            ApkXmlEditor.setAppbcode(config.versionCode.toIntOrNull() ?: 1)
            ApkXmlEditor.setAppbname(config.versionName)
            ApkXmlEditor.operation()

            config.permissions.forEach { setPermission(tempManifest.absolutePath, it, false) }
            removeTestOnly(tempManifest)

            if (config.appPackage != OLD_PACKAGE_NAME) {
                ManifestStringReplacer.batchReplaceStringInAXML(tempManifest, mapOf(
                    "$OLD_PACKAGE_NAME.androidx-startup" to "${config.appPackage}.androidx-startup",
                    "$OLD_PACKAGE_NAME.fileprovider" to "${config.appPackage}.fileprovider",
                    ".MainActivity" to "$OLD_PACKAGE_NAME.MainActivity"
                ))
            }

            ProviderAuthReplacer.replaceProviderAuthorities(tempManifest, OLD_PACKAGE_NAME, config.appPackage)
            ProviderAuthReplacer.fixProviderConflicts(tempManifest, config.appPackage)

            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            FileInputStream(tempManifest).use { fis -> copyStream(fis, zos) }
            zos.closeEntry()
        } finally {
            tempManifest.delete()
        }
    }

    fun setPermission(path: String, permission: String, remove: Boolean) {
        try {
            val file = File(path)
            val doc = AXMLDoc().apply { parse(FileInputStream(file)) }
            val pe = PermissionEditor(doc)
            val op = PermissionEditor.PermissionOpera(permission)
            
            pe.setEditorInfo(PermissionEditor.EditorInfo().with(if (remove) op.remove() else op.add()))
            pe.commit()
            
            FileOutputStream(file).use { doc.build(it) }
            doc.release()
        } catch (e: Exception) {
            LogCatcher.e("ApkBuilder", "Failed to modify permission: $permission", e)
        }
    }

    private fun addProjectFilesRecursively(zos: ZipOutputStream, file: File, zipPath: String) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { addProjectFilesRecursively(zos, it, "$zipPath/${it.name}") }
        } else {
            zos.putNextEntry(ZipEntry(zipPath))
            FileInputStream(file).use { fis -> copyStream(fis, zos) }
            zos.closeEntry()
        }
    }

    private fun copyAsStored(zipFile: ZipFile, entry: ZipEntry, zos: ZipOutputStream) {
        val data = zipFile.getInputStream(entry).use { it.readBytes() }
        val crc = CRC32().apply { update(data) }
        
        zos.putNextEntry(ZipEntry("resources.arsc").apply {
            method = ZipEntry.STORED
            size = data.size.toLong()
            compressedSize = data.size.toLong()
            setCrc(crc.value)
        })
        zos.write(data)
        zos.closeEntry()
    }

    private fun removeTestOnly(manifestFile: File) {
        val data = manifestFile.readBytes()
        val target = byteArrayOf(0x72, 0x02, 0x01, 0x01)
        var found = false
        for (i in 0 until data.size - 3) {
            if (data[i] == target[0] && data[i + 1] == target[1] && data[i + 2] == target[2] && data[i + 3] == target[3]) {
                repeat(4) { data[i + it] = 0 }
                found = true
            }
        }
        if (found) manifestFile.writeBytes(data)
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        input.copyTo(output)
    }

    private fun copyAssetFile(ctx: Context, name: String, dest: File): Boolean = try {
        ctx.assets.open(name).use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
        true
    } catch (e: IOException) { false }

    fun signerApk(keyPath: String, pass: String, alias: String, keyPass: String, inPath: String, outPath: String): Boolean = try {
        com.mcal.apksigner.ApkSigner(File(inPath), File(outPath)).apply {
            setV1SigningEnabled(true)
            setV2SigningEnabled(true)
            signRelease(File(keyPath), pass, alias, keyPass)
        }
        true
    } catch (e: Throwable) {
        e.printStackTrace()
        false
    }

    private object ManifestStringReplacer {
        private const val CHUNK_STRING_POOL = 0x001C0001

        fun batchReplaceStringInAXML(axmlFile: File, replacementMap: Map<String, String>) {
            val data = axmlFile.readBytes()
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).position(8) as ByteBuffer
            
            if (buffer.int != CHUNK_STRING_POOL) return

            val chunkSize = buffer.int
            val stringCount = buffer.int
            val styleCount = buffer.int
            val flags = buffer.int
            val stringsOffset = buffer.int
            
            val isUTF8 = (flags and 0x0100) != 0
            val stringPoolStart = buffer.position() - 28
            val offsets = IntArray(stringCount) { buffer.int }

            val strings = mutableListOf<String>()
            val dataStart = stringPoolStart + stringsOffset

            for (i in 0 until stringCount) {
                buffer.position(dataStart + offsets[i])
                if (isUTF8) {
                    val len1 = buffer.get().toInt() and 0xFF
                    val len = if (len1 and 0x80 != 0) ((len1 and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF) else len1
                    val len2 = buffer.get().toInt() and 0xFF
                    val encodedLen = if (len2 and 0x80 != 0) ((len2 and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF) else len2
                    val strBytes = ByteArray(encodedLen).also { buffer.get(it) }
                    strings.add(String(strBytes, StandardCharsets.UTF_8))
                } else {
                    val len = buffer.short.toInt() and 0xFFFF
                    val strBytes = ByteArray(len * 2).also { buffer.get(it) }
                    strings.add(String(strBytes, StandardCharsets.UTF_16LE))
                }
            }

            var modified = false
            strings.indices.forEach { i ->
                replacementMap[strings[i]]?.let {
                    strings[i] = it
                    modified = true
                }
            }

            if (!modified) return

            val poolBos = ByteArrayOutputStream()
            val newOffsets = mutableListOf<Int>()
            var currentOffset = 0

            strings.forEach { s ->
                newOffsets.add(currentOffset)
                if (isUTF8) {
                    val raw = s.toByteArray(StandardCharsets.UTF_8)
                    poolBos.write(s.length); poolBos.write(raw.size); poolBos.write(raw); poolBos.write(0)
                    currentOffset += (2 + raw.size + 1)
                } else {
                    val raw = s.toByteArray(StandardCharsets.UTF_16LE)
                    poolBos.write(s.length and 0xFF); poolBos.write((s.length shr 8) and 0xFF); poolBos.write(raw); poolBos.write(0); poolBos.write(0)
                    currentOffset += (2 + raw.size + 2)
                }
            }
            while (currentOffset % 4 != 0) { poolBos.write(0); currentOffset++ }

            val newStringData = poolBos.toByteArray()
            val fileBos = ByteArrayOutputStream()
            fileBos.write(data, 0, 8)
            
            val newChunkSize = 28 + (stringCount * 4) + (styleCount * 4) + newStringData.size
            val headerBuf = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(CHUNK_STRING_POOL); putInt(newChunkSize); putInt(stringCount); putInt(styleCount); putInt(flags)
                putInt(28 + (stringCount * 4) + (styleCount * 4)); putInt(0)
            }
            fileBos.write(headerBuf.array())
            newOffsets.forEach { off -> fileBos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(off).array()) }
            fileBos.write(newStringData)
            fileBos.write(data, stringPoolStart + chunkSize, data.size - (stringPoolStart + chunkSize))

            val finalData = fileBos.toByteArray()
            ByteBuffer.wrap(finalData).order(ByteOrder.LITTLE_ENDIAN).putInt(4, finalData.size)
            axmlFile.writeBytes(finalData)
        }
    }
}
