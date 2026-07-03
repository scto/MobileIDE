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
        onProgress: (BuildProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val gradlew = File(projectDir, "gradlew")
            if (!gradlew.exists()) {
                return@withContext Result.failure(IllegalStateException("gradlew not found in project directory"))
            }
            if (!gradlew.canExecute()) {
                gradlew.setExecutable(true)
            }

            onProgress(BuildProgress.Step("Starting Gradle build ($buildType)...", 0.1f))

            val task = "assemble$buildType"
            val pb = ProcessBuilder("bash", "./gradlew", task)
            pb.directory(projectDir)
            pb.redirectErrorStream(true) // merge stderr and stdout

            val javaHome = "/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk"
            if (File(javaHome).exists()) {
                pb.environment()["JAVA_HOME"] = javaHome
            }
            
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                line?.let { output ->
                    Timber.tag(TAG).d("Gradle: $output")
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
                        onProgress(BuildProgress.Step(output.trim(), -1f)) // Just log, don't fail yet
                    }
                }
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                onProgress(BuildProgress.Step("Build Successful!", 1.0f))
                
                // Find the APK
                val buildTypeLower = buildType.lowercase()
                val apkPaths = listOf(
                    "app/build/outputs/apk/$buildTypeLower/app-$buildTypeLower.apk",
                    "build/outputs/apk/$buildTypeLower/app-$buildTypeLower.apk"
                )
                
                var foundApk: File? = null
                for (path in apkPaths) {
                    val file = File(projectDir, path)
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
                val msg = "Gradle build failed with exit code $exitCode"
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
