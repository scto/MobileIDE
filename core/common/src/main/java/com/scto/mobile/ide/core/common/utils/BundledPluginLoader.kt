package com.scto.mobile.ide.core.common.utils

import android.app.Application
import android.os.Build
import android.system.Os
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Installs plugins bundled in assets/bundled_plugins/ or assets/Plugins/ into the local extension directory.
 *
 * Supports both:
 * 1. Plugin ZIP archives (.zip) containing manifest.json, extension APK, and resources.
 * 2. Unpacked bundled plugin directories shipping manifest.json (with symlink fallback to host APK).
 */
object BundledPluginLoader {

    private const val TAG = "BundledPluginLoader"
    private val ASSETS_DIRS = listOf("bundled_plugins", "Plugins")

    fun install(app: Application) {
        val assetManager = app.assets
        val extensionsRoot = File(app.filesDir.parentFile, "local/extensions")
        extensionsRoot.mkdirs()

        // The app's own compiled APK – used if plugin manifest is provided without a separate APK.
        val appApk = File(app.applicationInfo.sourceDir)

        for (assetsDir in ASSETS_DIRS) {
            val items = assetManager.list(assetsDir) ?: continue
            for (item in items) {
                try {
                    val assetPath = "$assetsDir/$item"
                    if (item.endsWith(".zip")) {
                        installZipAsset(app, assetPath, extensionsRoot)
                    } else {
                        installDirectoryAsset(app, assetsDir, item, extensionsRoot, appApk)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to install bundled asset $item from $assetsDir", e)
                }
            }
        }
    }

    private fun installZipAsset(app: Application, assetPath: String, extensionsRoot: File) {
        val assetManager = app.assets

        // 1. Read manifest.json content directly from the ZIP stream to obtain plugin ID and version
        val manifestContent = assetManager.open(assetPath).use { readManifestFromZipStream(it) }
            ?: throw IllegalStateException("No manifest.json found in ZIP asset: $assetPath")

        val pluginId = parsePluginId(manifestContent)
            ?: throw IllegalStateException("No valid 'id' found in manifest.json for ZIP asset: $assetPath")
        val bundledVersion = parseVersion(manifestContent)

        val extDir = File(extensionsRoot, pluginId)
        val existingManifest = File(extDir, "manifest.json")

        if (extDir.exists() && existingManifest.exists()) {
            val existingVersion = parseVersion(existingManifest.readText())
            if (versionAtLeast(existingVersion, bundledVersion)) {
                Log.d(TAG, "Bundled ZIP plugin $pluginId is up-to-date ($existingVersion)")
                return
            }
            Log.i(TAG, "Updating bundled ZIP plugin $pluginId: $existingVersion -> $bundledVersion")
            extDir.deleteRecursively()
        }

        extDir.mkdirs()

        // 2. Extract ZIP contents to extension directory
        assetManager.open(assetPath).use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val targetFile = File(extDir, entry.name)
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { output ->
                            zis.copyTo(output)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        }

        Log.i(TAG, "Successfully installed bundled ZIP plugin: $pluginId v$bundledVersion")
    }

    private fun installDirectoryAsset(
        app: Application,
        assetsDir: String,
        pluginId: String,
        extensionsRoot: File,
        appApk: File
    ) {
        val assetManager = app.assets
        val extDir = File(extensionsRoot, pluginId)

        // Read bundled manifest version
        val bundledManifestText =
            assetManager.open("$assetsDir/$pluginId/manifest.json").bufferedReader().readText()

        val bundledVersion = parseVersion(bundledManifestText)

        // Check if update is needed
        val existingManifest = File(extDir, "manifest.json")
        if (extDir.exists() && existingManifest.exists()) {
            val existingVersion = parseVersion(existingManifest.readText())
            if (versionAtLeast(existingVersion, bundledVersion)) {
                Log.d(TAG, "Bundled plugin $pluginId is up-to-date ($existingVersion)")
                ensureApkSymlink(extDir, appApk)
                return
            }
            Log.i(TAG, "Updating bundled plugin $pluginId: $existingVersion -> $bundledVersion")
            extDir.deleteRecursively()
        }

        extDir.mkdirs()
        File(extDir, "manifest.json").writeText(bundledManifestText)
        ensureApkSymlink(extDir, appApk)

        Log.i(TAG, "Installed bundled directory plugin: $pluginId v$bundledVersion")
    }

    private fun readManifestFromZipStream(inputStream: java.io.InputStream): String? {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "manifest.json" || entry.name.endsWith("/manifest.json")) {
                    return zis.bufferedReader().readText()
                }
                entry = zis.nextEntry
            }
        }
        return null
    }

    private fun ensureApkSymlink(extDir: File, appApk: File) {
        val extApk = File(extDir, "extension.apk")
        if (extApk.exists() || extApk.isSymbolicLink()) {
            return
        }
        try {
            Os.symlink(appApk.absolutePath, extApk.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, "Symlink failed, falling back to hardlink for ${extDir.name}: ${e.message}")
            try {
                Os.link(appApk.absolutePath, extApk.absolutePath)
            } catch (e2: Exception) {
                Log.e(TAG, "Both symlink and hardlink failed for ${extDir.name}: ${e2.message}")
            }
        }
    }

    private fun File.isSymbolicLink(): Boolean =
        try {
            val stat =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Os.lstat(absolutePath)
                } else null
            stat != null
        } catch (e: Exception) {
            false
        }

    private fun parsePluginId(json: String): String? {
        val match = Regex(""""id"\s*:\s*"([^"]+)"""").find(json)
        return match?.groupValues?.get(1)
    }

    private fun parseVersion(json: String): String {
        val match = Regex(""""version"\s*:\s*"([^"]+)"""").find(json)
        return match?.groupValues?.get(1) ?: "0.0.0"
    }

    private fun versionAtLeast(installed: String, bundled: String): Boolean {
        val a = installed.split(".").mapNotNull { it.toIntOrNull() }
        val b = bundled.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(a.size, b.size)
        for (i in 0 until len) {
            val ai = a.getOrElse(i) { 0 }
            val bi = b.getOrElse(i) { 0 }
            if (ai < bi) return false
            if (ai > bi) return true
        }
        return true
    }
}

