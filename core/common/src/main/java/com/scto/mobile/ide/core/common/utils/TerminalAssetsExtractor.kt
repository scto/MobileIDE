package com.scto.mobile.ide.core.common.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object TerminalAssetsExtractor {
    private const val TAG = "TerminalAssetsExtractor"

    fun ensureAssetsExtracted(context: Context, force: Boolean = false) {
        try {
            val prefixDir = context.filesDir.parentFile ?: return
            val localDir = File(prefixDir, "local").apply { mkdirs() }
            val binDir = File(localDir, "bin").apply { mkdirs() }
            val lspDir = File(binDir, "lsp").apply { mkdirs() }

            val versionCode = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
                }
            } catch (e: Exception) {
                1L
            }

            val markerFile = File(localDir, ".extracted_v$versionCode")
            val requiredScripts = listOf(
                File(binDir, "init-host"),
                File(binDir, "init"),
                File(binDir, "setup"),
                File(binDir, "sandbox"),
                File(binDir, "utils"),
                File(binDir, "universal_runner"),
                File(binDir, "termux-x11"),
                File(lspDir, "kotlin.sh"),
                File(lspDir, "java.sh"),
                File(lspDir, "bash.sh"),
                File(lspDir, "xml.sh")
            )

            val missingOrNotExecutable = requiredScripts.any { !it.exists() || !it.canExecute() }

            if (!force && markerFile.exists() && !missingOrNotExecutable) {
                return
            }

            Log.i(TAG, "Extracting terminal and LSP assets to ${binDir.absolutePath} (force=$force, missingOrNotExecutable=$missingOrNotExecutable)...")

            // 1. Copy terminal root scripts
            val terminalScriptMap = mapOf(
                "terminal/init-host.sh" to File(binDir, "init-host"),
                "terminal/init.sh" to File(binDir, "init"),
                "terminal/setup.sh" to File(binDir, "setup"),
                "terminal/sandbox.sh" to File(binDir, "sandbox"),
                "terminal/utils.sh" to File(binDir, "utils"),
                "terminal/shared_extraction.sh" to File(binDir, "shared_extraction.sh"),
                "terminal/universal_runner.sh" to File(binDir, "universal_runner"),
                "terminal/termux-x11.sh" to File(binDir, "termux-x11")
            )

            for ((assetPath, targetFile) in terminalScriptMap) {
                copyAssetFile(context, assetPath, targetFile)
                targetFile.setExecutable(true, false)
            }

            // 2. Copy all LSP scripts under terminal/lsp/
            val lspAssets = try { context.assets.list("terminal/lsp") ?: emptyArray() } catch (e: Exception) { emptyArray() }
            for (assetName in lspAssets) {
                val targetFile = File(lspDir, assetName)
                copyAssetFile(context, "terminal/lsp/$assetName", targetFile)
                targetFile.setExecutable(true, false)
            }

            // 3. Ensure distro home and root directories exist for default distros
            val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
                .getString("selected_distro", "ubuntu") ?: "ubuntu"
            val distroDir = File(localDir, distroName).apply { mkdirs() }
            File(distroDir, "home").mkdirs()
            File(distroDir, "root").mkdirs()
            File(distroDir, "tmp").mkdirs()

            markerFile.createNewFile()
            Log.i(TAG, "Asset extraction completed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract terminal and LSP assets: ${e.message}", e)
        }
    }

    private fun copyAssetFile(context: Context, assetPath: String, destFile: File) {
        try {
            destFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.setExecutable(true, false)
        } catch (e: Exception) {
            Log.w(TAG, "Could not copy asset $assetPath to ${destFile.absolutePath}: ${e.message}")
        }
    }
}
