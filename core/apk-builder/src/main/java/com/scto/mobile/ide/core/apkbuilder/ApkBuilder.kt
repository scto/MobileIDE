package com.scto.mobile.ide.core.apkbuilder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ApkBuilder(private val context: Context) {

    companion object {
        private const val TAG = "ApkBuilder"
        
        fun shouldSkipTemplateLibEntry(entryName: String, replacementLibraryNames: Set<String>): Boolean {
            val fileName = entryName.substringAfterLast('/')
            return replacementLibraryNames.contains(fileName)
        }
    }

    sealed class BuildProgress {
        data class Step(val message: String, val progress: Float) : BuildProgress()
        data class Success(val apkFile: File) : BuildProgress()
        data class Error(val message: String, val cause: Throwable? = null) : BuildProgress()
    }

    suspend fun build(
        projectDir: File,
        buildType: String = "Debug", // "Debug" or "Release"
        configureProcessBuilder: ((ProcessBuilder) -> Unit)? = null,
        onProgress: (BuildProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanProjectDir = File(projectDir.absolutePath.trim())
            val pathForLog = cleanProjectDir.absolutePath.replace(" ", "[SPACE]")
            Timber.tag(TAG).i("Validating build directory: \"$pathForLog\"")

            if (!cleanProjectDir.exists() || !cleanProjectDir.isDirectory) {
                val msg = "Projektverzeichnis ungültig oder existiert nicht: \"$cleanProjectDir\""
                Timber.tag(TAG).e(msg)
                onProgress(BuildProgress.Error(msg))
                return@withContext Result.failure(IllegalArgumentException(msg))
            }

            val hasSettingsGradle = File(cleanProjectDir, "settings.gradle").exists() || File(cleanProjectDir, "settings.gradle.kts").exists()
            if (!hasSettingsGradle) {
                val msg = "Projektverzeichnis ungültig oder Gradle-Build-Dateien fehlen: \"${cleanProjectDir.name}\" enthält keine settings.gradle oder settings.gradle.kts"
                Timber.tag(TAG).e(msg)
                onProgress(BuildProgress.Error(msg))
                return@withContext Result.failure(IllegalStateException(msg))
            }

            val gradlew = File(cleanProjectDir, "gradlew")
            if (!gradlew.exists()) {
                val msg = "gradlew Executable im Projektverzeichnis nicht gefunden"
                Timber.tag(TAG).e(msg)
                onProgress(BuildProgress.Error(msg))
                return@withContext Result.failure(IllegalStateException(msg))
            }
            if (!gradlew.canExecute()) {
                gradlew.setExecutable(true)
            }

            onProgress(BuildProgress.Step("Starting Gradle build ($buildType)...", 0.1f))

            val task = "assemble$buildType"
            val pb = ProcessBuilder()
            pb.directory(cleanProjectDir)
            pb.redirectErrorStream(true) // merge stderr and stdout

            if (configureProcessBuilder != null) {
                configureProcessBuilder(pb)
            } else {
                pb.command("bash", "./gradlew", task)
                val javaHomeCandidates = listOf(
                    File("/data/user/0/com.scto.mobile.ide/local/ubuntu/usr/lib/jvm/java-17-openjdk-arm64"),
                    File("/data/user/0/com.scto.mobile.ide/local/ubuntu/usr/lib/jvm/java-17-openjdk-amd64"),
                    File("/data/user/0/com.scto.mobile.ide/usr/lib/jvm/java-17-openjdk")
                )
                val validJavaHome = javaHomeCandidates.firstOrNull { it.exists() }?.absolutePath
                if (validJavaHome != null) {
                    pb.environment()["JAVA_HOME"] = validJavaHome
                }
            }
            
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var wrapperDownloadErrorDetected = false
            var wrapperErrorMessage = ""

            while (reader.readLine().also { line = it } != null) {
                line?.let { output ->
                    Timber.tag(TAG).d("Gradle: $output")

                    if (output.contains("FileNotFoundException") && (output.contains("distributions") || output.contains("gradle"))) {
                        wrapperDownloadErrorDetected = true
                        wrapperErrorMessage = "Gradle-Wrapper Download fehlgeschlagen: Die Gradle-Distribution konnte nicht heruntergeladen werden. Bitte Internetverbindung prüfen."
                    }

                    // Basic progress heuristics
                    val progress = when {
                        output.contains("> Task :app:preBuild") -> 0.2f
                        output.contains("> Task :app:compile") -> 0.5f
                        output.contains("> Task :app:dexBuilder") -> 0.7f
                        output.contains("> Task :app:package") -> 0.9f
                        else -> -1f
                    }
                    if (progress > 0) {
                        onProgress(BuildProgress.Step(output.trim(), progress))
                    } else if (output.contains("FAILED") || output.contains("Exception")) {
                        onProgress(BuildProgress.Step(output.trim(), -1f)) // Just log
                    }
                }
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                onProgress(BuildProgress.Step("Build Successful!", 1.0f))
                
                val buildTypeLower = buildType.lowercase()
                val apkPaths = listOf(
                    "app/build/outputs/apk/$buildTypeLower/app-$buildTypeLower.apk",
                    "build/outputs/apk/$buildTypeLower/app-$buildTypeLower.apk",
                    "app/build/outputs/apk/Fdroid/$buildTypeLower/app-Fdroid-$buildTypeLower.apk",
                    "app/build/outputs/apk/Fdroid/$buildTypeLower/MobileIDE-0.0.1-$buildTypeLower.apk"
                )
                
                var foundApk: File? = null
                for (path in apkPaths) {
                    val file = File(cleanProjectDir, path)
                    if (file.exists()) {
                        foundApk = file
                        break
                    }
                }

                if (foundApk != null) {
                    onProgress(BuildProgress.Success(foundApk))
                    Result.success(foundApk)
                } else {
                    val msg = "Build succeeded but APK was not found at standard paths."
                    onProgress(BuildProgress.Error(msg))
                    Result.failure(IllegalStateException(msg))
                }
            } else {
                val msg = if (wrapperDownloadErrorDetected) {
                    wrapperErrorMessage
                } else {
                    "Gradle build failed with exit code $exitCode"
                }
                onProgress(BuildProgress.Error(msg))
                Result.failure(RuntimeException(msg))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "APK build failed")
            onProgress(BuildProgress.Error(e.message ?: "Unknown error", e))
            Result.failure(e)
        }
    }
}
