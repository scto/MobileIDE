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
            .getString("selected_distro", "alpine") ?: "alpine"
    }

    suspend fun reinstallTerminal(context: Context) {
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
            
            prepareEnvironment(context)
            SessionManager.addNewSession(context)
        }
    }

    fun resetTerminal(context: Context) {
        val list = ArrayList(SessionManager.sessions)
        list.forEach {
            SessionManager.removeSession(it)
        }
        AlpineManager.currentProject = null
        SessionManager.addNewSession(context)
    }

    suspend fun prepareEnvironment(context: Context) {
        withContext(Dispatchers.IO) {
            val distroName = getDistroName(context)
            val filesDir = context.filesDir
            val prefixDir = filesDir.parentFile!!
            
            val distroDir = File(prefixDir, "local/$distroName")
            val binDir = File(prefixDir, "local/bin")
            val libDir = File(prefixDir, "local/lib")

            // 1. Copy binary files
            copyAsset(context, "proot", File(filesDir, "proot"))
            copyAsset(context, "libtalloc.so.2", File(filesDir, "libtalloc.so.2"))

            File(filesDir, "proot").setExecutable(true)

            // 2. Dynamisches RootFS Archiv kopieren
            val rootfsTar = File(filesDir, "$distroName.tar.gz")
            if (!rootfsTar.exists()) {
                // Erwartet, dass die Asset-Datei z.B. "ubuntu.tar.gz" oder "alpine.tar.gz" heißt
                copyAsset(context, "$distroName.tar.gz", rootfsTar)
            }

            // 3. RootFS entpacken in das jeweilige Distro-Verzeichnis
            val etcDir = File(distroDir, "etc")
            if (!etcDir.exists()) {
                distroDir.mkdirs()

                val cmd = "tar -zxf ${rootfsTar.absolutePath} -C ${distroDir.absolutePath}"
                try {
                    val process = Runtime.getRuntime().exec(cmd)
                    process.waitFor()
                    if (process.exitValue() != 0) {
                        Runtime.getRuntime()
                            .exec("tar -xf ${rootfsTar.absolutePath} -C ${distroDir.absolutePath}")
                            .waitFor()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. Init Skripte und Libs
            binDir.mkdirs()
            libDir.mkdirs()

            copyAsset(context, "libtalloc.so.2", File(libDir, "libtalloc.so.2"))
            copyAsset(context, "proot", File(binDir, "proot"))
            File(binDir, "proot").setExecutable(true)
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
