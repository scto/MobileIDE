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

                    val linker =
                        if (File("/system/bin/linker64").exists()) "/system/bin/linker64" else "/system/bin/linker"
                    val prootCmd =
                        arrayOf(
                            linker,
                            prootDest.absolutePath,
                            "--kill-on-exit",
                            "--link2symlink",
                            "-0",
                            "-r",
                            distroDir.absolutePath,
                            "-b",
                            "/system",
                            "-b",
                            "/data",
                            "-b",
                            "/proc",
                            "/system/bin/tar",
                            "-xf",
                            rootfsTar.absolutePath,
                            "-C",
                            "/",
                        )
                    LogCatcher.i("SetupWorker", "Executing proot extraction: ${prootCmd.joinToString(" ")}")
                    var process = Runtime.getRuntime().exec(prootCmd)
                    var exitVal = process.waitFor()

                    if (exitVal != 0) {
                        LogCatcher.w(
                            "SetupWorker",
                            "Proot extraction failed with exit code $exitVal. Falling back to host tar...",
                        )
                        val hostCmd = arrayOf("tar", "-xf", rootfsTar.absolutePath, "-C", distroDir.absolutePath)
                        process = Runtime.getRuntime().exec(hostCmd)
                        exitVal = process.waitFor()
                    }

                    if (exitVal != 0 && !etcDir.exists()) {
                        val errorMsg = process.errorStream.bufferedReader().use { it.readText() }
                        LogCatcher.e("SetupWorker", "Tar extraction failed with exit code $exitVal: $errorMsg")
                        distroDir.deleteRecursively()
                    } else {
                        if (exitVal != 0) {
                            LogCatcher.w(
                                "SetupWorker",
                                "Host tar returned non-zero exit code $exitVal, but etc directory exists. Proceeding...",
                            )
                        } else {
                            LogCatcher.i("SetupWorker", "Tar extraction finished successfully.")
                        }
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
}
