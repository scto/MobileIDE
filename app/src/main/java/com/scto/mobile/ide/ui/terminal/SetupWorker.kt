/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  Thomas Schmid  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.ui.terminal

import android.content.Context
import com.scto.mobile.ide.core.utils.LogCatcher
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SetupWorker {
    private fun getDistroName(context: Context): String {
        return context
            .getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
    }

    suspend fun reinstallTerminal(
        context: Context,
        onStatusChanged: ((String) -> Unit)? = null,
        onProgress: Downloader.ProgressCallback? = null,
    ) {
        withContext(Dispatchers.IO) {
            LogCatcher.i("SetupWorker", "reinstallTerminal starting...")
            onStatusChanged?.invoke("Alte Installation wird gelöscht...")
            val list = ArrayList(SessionManager.sessions)
            list.forEach { SessionManager.removeSession(it) }

            val distroName = getDistroName(context)
            val filesDir = context.filesDir
            val prefixDir = filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$distroName")
            val rootfsTar = File(filesDir, "$distroName.tar.gz")

            distroDir.deleteRecursively()
            rootfsTar.delete()
            File(prefixDir, "local/.terminal_setup_ok_DO_NOT_REMOVE").delete()

            prepareEnvironment(context, onStatusChanged = onStatusChanged, onProgress = onProgress)
            SessionManager.addNewSession(context)
        }
    }

    fun resetTerminal(context: Context) {
        val list = ArrayList(SessionManager.sessions)
        list.forEach { SessionManager.removeSession(it) }
        DistroManager.currentProject = null
        SessionManager.addNewSession(context)
    }

    /**
     * Prepares the terminal environment:
     * 1. Downloads proot binary (from GitHub Releases) if not present.
     * 2. Downloads the selected distro's rootfs archive if not present.
     * 3. Extracts the rootfs into its distro directory.
     * 4. Copies helper libs and scripts.
     *
     * @param onProgress optional progress callback forwarded to [Downloader].
     */
    suspend fun prepareEnvironment(
        context: Context,
        onStatusChanged: ((String) -> Unit)? = null,
        onProgress: Downloader.ProgressCallback? = null,
    ) {
        withContext(Dispatchers.IO) {
            LogCatcher.i("SetupWorker", "prepareEnvironment starting...")
            logTerminalSetup(context)
            onStatusChanged?.invoke("Umgebung wird vorbereitet...")
            val distroName = getDistroName(context)
            val filesDir = context.filesDir
            val prefixDir = filesDir.parentFile!!

            val distroDir = File(prefixDir, "local/$distroName")
            val binDir = File(prefixDir, "local/bin")
            val libDir = File(prefixDir, "local/lib")

            // 1. Setup proot binary (prefer local jniLib libproot.so, fallback to download)
            val prootDest = File(filesDir, "proot")
            if (!prootDest.exists() || prootDest.length() == 0L) {
                onStatusChanged?.invoke("PRoot wird eingerichtet...")
                var success = false

                // Try copying the native libproot.so (which is compiled as PIE, e_type: 3)
                val nativeLibDir = context.applicationInfo.nativeLibraryDir
                val libProot = File(nativeLibDir, "libproot.so")
                if (libProot.exists()) {
                    try {
                        LogCatcher.i("SetupWorker", "Copying native libproot.so to proot destination.")
                        libProot.copyTo(prootDest, overwrite = true)
                        prootDest.setExecutable(true)
                        success = true
                    } catch (e: Exception) {
                        LogCatcher.e("SetupWorker", "Failed to copy native libproot.so", e)
                    }
                }

                // If copy fails, try downloading
                if (!success) {
                    try {
                        LogCatcher.i("SetupWorker", "Downloading proot.")
                        Downloader.downloadProot(context, onProgress = onProgress)
                    } catch (e: Exception) {
                        LogCatcher.e("SetupWorker", "Failed to download proot", e)
                    }
                }
            }

            // 2. Setup libtalloc (downloading from custom repo).
            onStatusChanged?.invoke("Bibliotheken werden kopiert...")
            val tallocDest = File(filesDir, "libtalloc.so.2")
            try {
                LogCatcher.i("SetupWorker", "Downloading libtalloc via Downloader.")
                Downloader.downloadTalloc(context, onProgress = onProgress)
            } catch (e: Exception) {
                LogCatcher.e("SetupWorker", "Failed to download libtalloc", e)
            }

            // 3. Download rootfs archive (from GitHub Releases, arch-aware).
            val rootfsTar = File(filesDir, "$distroName.tar.gz")
            if (!rootfsTar.exists() || rootfsTar.length() == 0L) {
                onStatusChanged?.invoke("Linux RootFS wird heruntergeladen...")
                try {
                    LogCatcher.i("SetupWorker", "Downloading rootfs archive.")
                    Downloader.downloadRootFs(context, distro = distroName, onProgress = onProgress)
                } catch (e: Exception) {
                    LogCatcher.e("SetupWorker", "Rootfs download failed.", e)
                    throw IllegalStateException("RootFS Download fehlgeschlagen. Bitte Internetverbindung prüfen.", e)
                }
            }
            if (!rootfsTar.exists() || rootfsTar.length() == 0L) {
                throw IllegalStateException("RootFS Datei fehlt nach dem Download.")
            }

            // 4. Extract rootfs is deferred to setup.sh inside the terminal session
            LogCatcher.i("SetupWorker", "Deferring rootfs extraction to setup.sh inside the terminal session.")

            // 5. Place proot + libs in local/bin and local/lib.
            onStatusChanged?.invoke("Installation wird abgeschlossen...")
            binDir.mkdirs()
            libDir.mkdirs()

            val tallocFile = File(filesDir, "libtalloc.so.2")
            if (tallocFile.exists()) {
                try {
                    tallocFile.copyTo(File(libDir, "libtalloc.so.2"), overwrite = true)
                } catch (e: Exception) {
                    LogCatcher.e("SetupWorker", "Failed to copy downloaded libtalloc to libDir", e)
                }
            }

            val prootSrc = File(filesDir, "proot")
            if (prootSrc.exists()) {
                prootSrc.copyTo(File(binDir, "proot"), overwrite = true)
                File(binDir, "proot").setExecutable(true)
            }
        }
    }

    private fun copyAsset(context: Context, assetName: String, destFile: File) {
        if (!destFile.exists() || destFile.length() == 0L || assetName.contains("so") || assetName == "proot") {
            try {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logTerminalSetup(context: Context) {
        try {
            val distroName = getDistroName(context)
            val filesDir = context.filesDir
            val prefixDir = filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$distroName")
            val binDir = File(prefixDir, "local/bin")
            val libDir = File(prefixDir, "local/lib")
            val nativeLibDir = context.applicationInfo.nativeLibraryDir

            val closeBehavior = com.scto.mide.term.settings.Settings.terminal_close_behavior
            val fontSize = com.scto.mide.term.settings.Settings.terminal_font_size
            val colorScheme = com.scto.mide.term.settings.Settings.terminal_colorscheme
            val extraKeys = TerminalConfig.VIRTUAL_KEYS_JSON

            val prootFile = File(binDir, "proot")
            val tallocFile = File(libDir, "libtalloc.so.2")
            val initFile = File(binDir, "init")
            val setupFile = File(binDir, "setup")
            val sandboxFile = File(binDir, "sandbox")
            val utilsFile = File(binDir, "utils")
            val idesetupFile = File(binDir, "idesetup")
            val envProps = File(distroDir, "root/etc/mobileide-environment.properties")

            val sb = java.lang.StringBuilder()
            sb.append("\n=== TERMINAL SETUP ENVIRONMENT LOG ===\n")
            sb.append("OS Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
            sb.append("CPU ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString(", ")}\n")
            sb.append("Distro Name: $distroName\n")
            sb.append("Files Directory: ${filesDir.absolutePath}\n")
            sb.append("Prefix Directory: ${prefixDir.absolutePath}\n")
            sb.append("Distro Directory: ${distroDir.absolutePath} (exists: ${distroDir.exists()})\n")
            sb.append("Bin Directory: ${binDir.absolutePath}\n")
            sb.append("Lib Directory: ${libDir.absolutePath}\n")
            sb.append("Native Lib Directory: $nativeLibDir\n")
            sb.append("\n--- Settings ---\n")
            sb.append("Close Behavior: $closeBehavior\n")
            sb.append("Font Size: $fontSize\n")
            sb.append("Color Scheme: $colorScheme\n")
            sb.append("Extra Keys Config: $extraKeys\n")
            sb.append("\n--- Component Status ---\n")
            sb.append("proot exists: ${prootFile.exists()} (executable: ${prootFile.canExecute()})\n")
            sb.append("libtalloc exists: ${tallocFile.exists()}\n")
            sb.append("init exists: ${initFile.exists()} (executable: ${initFile.canExecute()})\n")
            sb.append("setup exists: ${setupFile.exists()} (executable: ${setupFile.canExecute()})\n")
            sb.append("sandbox exists: ${sandboxFile.exists()} (executable: ${sandboxFile.canExecute()})\n")
            sb.append("utils exists: ${utilsFile.exists()} (executable: ${utilsFile.canExecute()})\n")
            sb.append("idesetup exists: ${idesetupFile.exists()} (executable: ${idesetupFile.canExecute()})\n")
            if (envProps.exists()) {
                sb.append("mobileide-environment.properties exists: true\n")
                try {
                    sb.append("mobileide-environment.properties content:\n${envProps.readText()}\n")
                } catch (e: Exception) {
                    sb.append("Failed to read mobileide-environment.properties: ${e.message}\n")
                }
            } else {
                sb.append("mobileide-environment.properties exists: false\n")
            }
            sb.append("======================================\n")

            LogCatcher.i("SetupWorker", sb.toString())
        } catch (e: Exception) {
            LogCatcher.e("SetupWorker", "Error generating terminal setup log", e)
        }
    }
}
