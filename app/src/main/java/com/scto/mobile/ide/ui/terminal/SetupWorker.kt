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
            onStatusChanged?.invoke("Umgebung wird vorbereitet...")
            val distroName = getDistroName(context)
            val filesDir = context.filesDir
            val prefixDir = filesDir.parentFile!!

            val distroDir = File(prefixDir, "local/$distroName")
            val binDir = File(prefixDir, "local/bin")
            val libDir = File(prefixDir, "local/lib")

            // 1. Setup proot binary (prefer local jniLib libproot.so, fallback to bundled asset, then download)
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

                // Fallback to bundled proot asset
                if (!success) {
                    try {
                        LogCatcher.i("SetupWorker", "Copying bundled proot asset.")
                        copyAsset(context, "proot", prootDest)
                        prootDest.setExecutable(true)
                        success = true
                    } catch (e: Exception) {
                        LogCatcher.e("SetupWorker", "Failed to copy bundled proot asset", e)
                    }
                }

                // If both fail, try downloading as a last resort
                if (!success) {
                    try {
                        LogCatcher.i("SetupWorker", "Downloading proot as fallback.")
                        Downloader.downloadProot(context, onProgress = onProgress)
                    } catch (e: Exception) {
                        LogCatcher.e("SetupWorker", "Failed to download proot fallback", e)
                    }
                }
            }

            // 2. Setup libtalloc (prefer downloading from custom repo, fallback to bundled asset).
            onStatusChanged?.invoke("Bibliotheken werden kopiert...")
            val tallocDest = File(filesDir, "libtalloc.so.2")
            var tallocSuccess = false
            try {
                LogCatcher.i("SetupWorker", "Downloading libtalloc via Downloader.")
                Downloader.downloadTalloc(context, onProgress = onProgress)
                tallocSuccess = tallocDest.exists() && tallocDest.length() > 0L
            } catch (e: Exception) {
                LogCatcher.e("SetupWorker", "Failed to download libtalloc, falling back to assets", e)
            }

            if (!tallocSuccess) {
                LogCatcher.i("SetupWorker", "Copying fallback libtalloc from assets.")
                copyAsset(context, "libtalloc.so.2", tallocDest)
            }

            // 3. Download rootfs archive (from GitHub Releases, arch-aware).
            //    Falls back to the bundled asset if the download fails.
            val rootfsTar = File(filesDir, "$distroName.tar.gz")
            if (!rootfsTar.exists() || rootfsTar.length() == 0L) {
                onStatusChanged?.invoke("Linux RootFS wird heruntergeladen...")
                try {
                    LogCatcher.i("SetupWorker", "Downloading rootfs archive.")
                    Downloader.downloadRootFs(context, distro = distroName, onProgress = onProgress)
                } catch (e: Exception) {
                    LogCatcher.e("SetupWorker", "Rootfs download failed, copying fallback backup asset.", e)
                    onStatusChanged?.invoke("Lokales Backup wird geladen...")
                    try {
                        copyAsset(context, "$distroName.tar.gz", rootfsTar)
                    } catch (assetEx: Exception) {
                        LogCatcher.e("SetupWorker", "Failed to copy local fallback backup asset", assetEx)
                    }
                }
            }

            // 4. Extract rootfs if not already done.
            val etcDir = File(distroDir, "etc")
            if (!etcDir.exists() && rootfsTar.exists() && rootfsTar.length() > 0L) {
                onStatusChanged?.invoke(
                    "Linux RootFS wird entpackt (Bitte warten, dies kann bis zu einer Minute dauern)..."
                )
                distroDir.mkdirs()
                try {
                    LogCatcher.i(
                        "SetupWorker",
                        "Extracting tar rootfs: ${rootfsTar.absolutePath} to ${distroDir.absolutePath}",
                    )
                    val cmd = arrayOf("tar", "-xf", rootfsTar.absolutePath, "-C", distroDir.absolutePath)
                    val process = Runtime.getRuntime().exec(cmd)
                    val exitVal = process.waitFor()
                    if (exitVal != 0) {
                        val errorMsg = process.errorStream.bufferedReader().use { it.readText() }
                        LogCatcher.e("SetupWorker", "Tar extraction failed with exit code $exitVal: $errorMsg")
                        distroDir.deleteRecursively()
                    } else if (!etcDir.exists()) {
                        LogCatcher.e("SetupWorker", "Tar extraction finished but 'etc' directory does not exist.")
                        distroDir.deleteRecursively()
                    } else {
                        LogCatcher.i("SetupWorker", "Tar extraction finished successfully.")
                    }
                } catch (e: Exception) {
                    LogCatcher.e("SetupWorker", "Exception during tar extraction", e)
                    distroDir.deleteRecursively()
                }
            }

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
                    copyAsset(context, "libtalloc.so.2", File(libDir, "libtalloc.so.2"))
                }
            } else {
                copyAsset(context, "libtalloc.so.2", File(libDir, "libtalloc.so.2"))
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
}
