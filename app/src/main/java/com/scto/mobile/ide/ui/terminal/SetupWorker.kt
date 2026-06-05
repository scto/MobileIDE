/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.ui.terminal

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SetupWorker {
    private const val TAG = "SetupWorker"

    suspend fun prepareEnvironment(context: Context) {
        withContext(Dispatchers.IO) {
            val filesDir = context.filesDir
            val terminalDir = File(filesDir, "terminal")

            // 1. Gesamten "terminal" Ordner aus den Assets rekursiv kopieren
            Log.i(TAG, "Kopiere Terminal-Ressourcen...")
            copyAssetFolder(context, "terminal", terminalDir.absolutePath)

            // 2. OS-Archiv erkennen (Ubuntu oder Alpine)
            val ubuntuTar = File(terminalDir, "ubuntu.tar.gz")
            val alpineTar = File(terminalDir, "alpine.tar.gz")
            val rootfsBin = File(terminalDir, "rootfs.bin")

            val osDir: File
            val archiveFile: File

            when {
                ubuntuTar.exists() -> {
                    osDir = File(terminalDir, "ubuntu")
                    archiveFile = ubuntuTar
                }
                alpineTar.exists() -> {
                    osDir = File(terminalDir, "alpine")
                    archiveFile = alpineTar
                }
                rootfsBin.exists() -> {
                    osDir = File(terminalDir, "alpine") // Fallback
                    archiveFile = rootfsBin
                }
                else -> {
                    Log.w(TAG, "Kein OS-Archiv (ubuntu.tar.gz, alpine.tar.gz oder rootfs.bin) gefunden!")
                    return@withContext
                }
            }

            // 3. Rootfs entpacken, falls noch nicht geschehen
            val etcDir = File(osDir, "etc")
            if (!etcDir.exists() && archiveFile.exists()) {
                osDir.mkdirs()
                val cmd = "tar -xf ${archiveFile.absolutePath} -C ${osDir.absolutePath}"
                try {
                    Log.i(TAG, "Entpacke Rootfs: $cmd")
                    val process = Runtime.getRuntime().exec(cmd)
                    process.waitFor()
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Entpacken des Rootfs", e)
                }
            }
        }
    }

    private fun copyAssetFolder(context: Context, fromAssetPath: String, toPath: String) {
        val assetManager = context.assets
        val assets =
            try {
                assetManager.list(fromAssetPath)
            } catch (e: Exception) {
                null
            }

        if (assets.isNullOrEmpty()) {
            copySingleAsset(context, fromAssetPath, File(toPath))
        } else {
            val dir = File(toPath)
            if (!dir.exists()) dir.mkdirs()

            for (asset in assets) {
                val subAssetPath = if (fromAssetPath.isEmpty()) asset else "$fromAssetPath/$asset"
                copyAssetFolder(context, subAssetPath, "$toPath/$asset")
            }
        }
    }

    private fun copySingleAsset(context: Context, assetPath: String, destFile: File) {
        // Rootfs-Archive nicht überschreiben, wenn sie schon existieren
        if (destFile.exists() && (assetPath.contains("rootfs") || assetPath.contains("tar.gz"))) return

        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }

            // Ausführungsrechte für Shell-Skripte, Binaries und Libs
            if (destFile.name.endsWith(".sh") || destFile.name == "proot" || destFile.name.contains(".so")) {
                destFile.setExecutable(true, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Asset nicht gefunden oder Kopierfehler: $assetPath", e)
        }
    }
}
