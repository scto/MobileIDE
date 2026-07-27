package com.scto.mobile.ide.core.tooling.impl.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class DependencyCacheItem(
    val group: String,
    val artifact: String,
    val version: String,
    val sizeBytes: Long,
    val path: String
) {
    val coordinate: String
        get() = "$group:$artifact:$version"
}

data class StorageCacheSummary(
    val gradleCacheBytes: Long,
    val androidSdkCacheBytes: Long,
    val buildOutputsBytes: Long,
    val totalBytes: Long,
    val topDependencies: List<DependencyCacheItem>,
    val orphanedCount: Int,
    val orphanedBytes: Long,
    val lastUpdatedTimeMs: Long
)

object GradleCacheAnalyzer {

    private fun getFolderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return size
    }

    private fun getGradleCacheDir(context: Context): File {
        val userHomeDir = File(System.getProperty("user.home") ?: "/root")
        val defaultGradle = File(userHomeDir, ".gradle/caches/modules-2/files-2.1")
        if (defaultGradle.exists()) return defaultGradle

        val sandboxGradle = File(context.filesDir.parentFile ?: context.filesDir, "local/sandbox/root/.gradle/caches/modules-2/files-2.1")
        if (sandboxGradle.exists()) return sandboxGradle

        return File(context.filesDir, ".gradle_caches")
    }

    private fun getAndroidSdkDir(context: Context): File {
        val prefixDir = context.filesDir.parentFile ?: context.filesDir
        val sdkDir = File(prefixDir, "local/sandbox/root/android-sdk")
        if (sdkDir.exists()) return sdkDir
        return File(context.filesDir, "android-sdk")
    }

    suspend fun analyzeStorageCache(context: Context, projectPath: String?): StorageCacheSummary = withContext(Dispatchers.IO) {
        val gradleDir = getGradleCacheDir(context)
        val gradleCacheBytes = getFolderSize(gradleDir)

        val sdkDir = getAndroidSdkDir(context)
        val androidSdkBytes = getFolderSize(sdkDir)

        var buildOutputsBytes = 0L
        if (!projectPath.isNullOrBlank()) {
            val projDir = File(projectPath)
            if (projDir.exists()) {
                projDir.walkTopDown().forEach { f ->
                    if (f.isDirectory && f.name == "build") {
                        buildOutputsBytes += getFolderSize(f)
                    }
                }
            }
        }

        val deps = mutableListOf<DependencyCacheItem>()
        if (gradleDir.exists()) {
            gradleDir.listFiles()?.forEach { groupDir ->
                if (groupDir.isDirectory) {
                    val groupName = groupDir.name
                    groupDir.listFiles()?.forEach { artDir ->
                        if (artDir.isDirectory) {
                            val artName = artDir.name
                            artDir.listFiles()?.forEach { verDir ->
                                if (verDir.isDirectory) {
                                    val verName = verDir.name
                                    val size = getFolderSize(verDir)
                                    deps.add(
                                        DependencyCacheItem(
                                            group = groupName,
                                            artifact = artName,
                                            version = verName,
                                            sizeBytes = size,
                                            path = verDir.absolutePath
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val topDeps = deps.sortedByDescending { it.sizeBytes }.take(15)
        val orphaned = deps.drop(15)
        val orphanedSize = orphaned.sumOf { it.sizeBytes }

        StorageCacheSummary(
            gradleCacheBytes = gradleCacheBytes,
            androidSdkCacheBytes = androidSdkBytes,
            buildOutputsBytes = buildOutputsBytes,
            totalBytes = gradleCacheBytes + androidSdkBytes + buildOutputsBytes,
            topDependencies = topDeps,
            orphanedCount = orphaned.size,
            orphanedBytes = orphanedSize,
            lastUpdatedTimeMs = System.currentTimeMillis()
        )
    }

    suspend fun clearEntireGradleCache(context: Context): Long = withContext(Dispatchers.IO) {
        val dir = getGradleCacheDir(context)
        val freedBytes = getFolderSize(dir)
        try {
            dir.deleteRecursively()
            dir.mkdirs()
        } catch (e: Exception) {
            Timber.e(e, "Error clearing Gradle cache")
        }
        freedBytes
    }

    suspend fun clearOrphanedCache(context: Context): Long = withContext(Dispatchers.IO) {
        val summary = analyzeStorageCache(context, null)
        var freed = 0L
        summary.topDependencies.drop(8).forEach { item ->
            val f = File(item.path)
            if (f.exists()) {
                freed += getFolderSize(f)
                f.deleteRecursively()
            }
        }
        freed
    }

    suspend fun clearBuildOutputs(projectPath: String): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        val projDir = File(projectPath)
        if (projDir.exists()) {
            projDir.walkTopDown().forEach { f ->
                if (f.isDirectory && f.name == "build") {
                    freed += getFolderSize(f)
                    f.deleteRecursively()
                }
            }
        }
        freed
    }
}
