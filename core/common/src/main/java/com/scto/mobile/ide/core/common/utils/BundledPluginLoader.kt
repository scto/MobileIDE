package com.scto.mobile.ide.core.common.utils

import android.app.Application
import android.os.Build
import android.system.Os
import android.util.Log
import java.io.File

/**
 * Installs plugins bundled in assets/bundled_plugins/ into the local extension directory.
 *
 * Each bundled plugin only ships a manifest.json (no separate APK), because its main class is compiled directly into
 * the host app APK via `implementation(project(...))`.
 *
 * On first run (or when a plugin directory doesn't exist yet) we:
 * 1. Copy the manifest.json from assets
 * 2. Create a symlink called "extension.apk" pointing to the app's own APK, so the extension loader's PathClassLoader
 *    can find the already-compiled classes via the parent class-loader delegation chain.
 *
 * If an existing bundled plugin's manifest version is LOWER than the one in assets, the plugin directory is deleted and
 * recreated so the manifest is refreshed.
 */
object BundledPluginLoader {

    private const val TAG = "BundledPluginLoader"
    private const val ASSETS_DIR = "bundled_plugins"

    fun install(app: Application) {
        val assetManager = app.assets
        val pluginIds = assetManager.list(ASSETS_DIR) ?: return

        val extensionsRoot = File(app.filesDir.parentFile, "local/extensions")
        extensionsRoot.mkdirs()

        // The app's own compiled APK – this is where JavaLspExtension / KotlinLspExtension live.
        val appApk = File(app.applicationInfo.sourceDir)

        for (pluginId in pluginIds) {
            try {
                val extDir = File(extensionsRoot, pluginId)

                // Read bundled manifest version
                val bundledManifestText =
                    assetManager.open("$ASSETS_DIR/$pluginId/manifest.json").bufferedReader().readText()

                val bundledVersion = parseVersion(bundledManifestText)

                // Check if update is needed
                val existingManifest = File(extDir, "manifest.json")
                if (extDir.exists() && existingManifest.exists()) {
                    val existingVersion = parseVersion(existingManifest.readText())
                    if (versionAtLeast(existingVersion, bundledVersion)) {
                        Log.d(TAG, "Bundled plugin $pluginId is up-to-date ($existingVersion)")
                        // Ensure the APK symlink exists even if manifest was already installed
                        ensureApkSymlink(extDir, appApk)
                        continue
                    }
                    Log.i(TAG, "Updating bundled plugin $pluginId: $existingVersion -> $bundledVersion")
                    extDir.deleteRecursively()
                }

                extDir.mkdirs()

                // Write manifest
                File(extDir, "manifest.json").writeText(bundledManifestText)

                // Create symlink to app APK
                ensureApkSymlink(extDir, appApk)

                Log.i(TAG, "Installed bundled plugin: $pluginId v$bundledVersion")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install bundled plugin $pluginId", e)
            }
        }
    }

    private fun ensureApkSymlink(extDir: File, appApk: File) {
        val extApk = File(extDir, "extension.apk")
        if (extApk.exists() || extApk.isSymbolicLink()) {
            // Already set up – verify it still points to the right place
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

    /** Parses "version" field from a raw JSON string (no full JSON parser dependency). */
    private fun parseVersion(json: String): String {
        val match = Regex(""""version"\s*:\s*"([^"]+)"""").find(json)
        return match?.groupValues?.get(1) ?: "0.0.0"
    }

    /** Returns true if [installed] >= [bundled] using simple semver comparison. */
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
        return true // equal
    }
}
