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
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SetupWorker {
    private fun getDistroName(context: Context): String {
        return context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
    }

    suspend fun reinstallTerminal(
        context: Context,
        onProgress: Downloader.ProgressCallback? = null,
    ) {
        withContext(Dispatchers.IO) {
            val list = ArrayList(SessionManager.sessions)
            list.forEach {
                SessionManager.removeSession(it)
            }
            
            val distroName = getDistroName(context)
            val filesDir = context.filesDir
            val prefixDir = filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$distroName")
            val rootfsTar = File(filesDir, "$distroName.tar.gz")
            
            distroDir.deleteRecursively()
            rootfsTar.delete()
            
            prepareEnvironment(context, onProgress = onProgress)
            SessionManager.addNewSession(context)
        }
    }

    fun resetTerminal(context: Context) {
        val list = ArrayList(SessionManager.sessions)
        list.forEach {
            SessionManager.removeSession(it)
        }
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
        onProgress: Downloader.ProgressCallback? = null,
    ) {
        withContext(Dispatchers.IO) {
            val distroName = getDistroName(context)
            val filesDir   = context.filesDir
            val prefixDir  = filesDir.parentFile!!

            val distroDir = File(prefixDir, "local/$distroName")
            val binDir    = File(prefixDir, "local/bin")
            val libDir    = File(prefixDir, "local/lib")

            // 1. Download proot (from GitHub Releases, arch-aware).
            //    Falls back to the bundled asset if the download fails.
            val prootDest = File(filesDir, "proot")
            if (!prootDest.exists() || prootDest.length() == 0L) {
                try {
                    Downloader.downloadProot(context, onProgress = onProgress)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback: copy bundled proot asset
                    copyAsset(context, "proot", prootDest)
                    prootDest.setExecutable(true)
                }
            }

            // 2. Always copy libtalloc (bundled, arch-specific via jniLibs).
            copyAsset(context, "libtalloc.so.2", File(filesDir, "libtalloc.so.2"))

            // 3. Download rootfs archive (from GitHub Releases, arch-aware).
            //    Falls back to the bundled asset if the download fails.
            val rootfsTar = File(filesDir, "$distroName.tar.gz")
            if (!rootfsTar.exists() || rootfsTar.length() == 0L) {
                try {
                    Downloader.downloadRootFs(
                        context,
                        distro = distroName,
                        onProgress = onProgress,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback: copy bundled asset (only ubuntu.tar.gz is bundled)
                    try {
                        copyAsset(context, "$distroName.tar.gz", rootfsTar)
                    } catch (assetEx: Exception) {
                        assetEx.printStackTrace()
                    }
                }
            }

            // 4. Extract rootfs if not already done.
            val etcDir = File(distroDir, "etc")
            if (!etcDir.exists() && rootfsTar.exists() && rootfsTar.length() > 0L) {
                distroDir.mkdirs()
                try {
                    val cmd = arrayOf("tar", "-xf", rootfsTar.absolutePath, "-C", distroDir.absolutePath)
                    val process = Runtime.getRuntime().exec(cmd)
                    process.waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 5. Place proot + libs in local/bin and local/lib.
            binDir.mkdirs()
            libDir.mkdirs()

            copyAsset(context, "libtalloc.so.2", File(libDir, "libtalloc.so.2"))

            val prootSrc = File(filesDir, "proot")
            if (prootSrc.exists()) {
                prootSrc.copyTo(File(binDir, "proot"), overwrite = true)
                File(binDir, "proot").setExecutable(true)
            }
        }
    }

    private fun copyAsset(context: Context, assetName: String, destFile: File) {
        if (!destFile.exists() || assetName.contains("so") || assetName == "proot") {
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