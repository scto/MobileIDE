/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.ui.terminal

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SetupWorker {
    suspend fun reinstallTerminal(context: Context) {
        withContext(Dispatchers.Main) {
            val list = ArrayList(SessionManager.sessions)
            list.forEach { SessionManager.removeSession(it) }
        }
        withContext(Dispatchers.IO) {
            val filesDir = context.filesDir
            val prefixDir = filesDir.parentFile!!
            val alpineDir = File(prefixDir, "local/alpine")
            val rootfsTar = File(filesDir, "alpine.tar.gz")

            alpineDir.deleteRecursively()
            rootfsTar.delete()

            prepareEnvironment(context)
        }
        SessionManager.addNewSession(context)
    }

    suspend fun resetTerminal(context: Context) {
        withContext(Dispatchers.Main) {
            val list = ArrayList(SessionManager.sessions)
            list.forEach { SessionManager.removeSession(it) }
            AlpineManager.currentProject = null
        }
        SessionManager.addNewSession(context)
    }

    suspend fun prepareEnvironment(context: Context) {
        withContext(Dispatchers.IO) {
            val filesDir = context.filesDir
            // The parentFile here is usually /data/user/0/com.example.mytermux/
            val prefixDir = filesDir.parentFile!!
            val alpineDir = File(prefixDir, "local/alpine")
            val binDir = File(prefixDir, "local/bin")
            val libDir = File(prefixDir, "local/lib")

            // 1. Copy binary files
            copyAsset(context, "proot", File(filesDir, "proot"))
            copyAsset(context, "libtalloc.so.2", File(filesDir, "libtalloc.so.2"))

            // Ensure binary files have execution permissions
            File(filesDir, "proot").setExecutable(true)

            // 2. Copy Rootfs archive (Note: source file is rootfs.bin, target saved as alpine.tar.gz)
            val rootfsTar = File(filesDir, "alpine.tar.gz")
            if (!rootfsTar.exists()) {
                copyAsset(context, "rootfs.bin", rootfsTar)
            }

            // 3. Critical fix: Force extract Rootfs.
            // Check if the /etc directory exists, if not, it means it hasn't been extracted or extraction failed.
            val etcDir = File(alpineDir, "etc")
            if (!etcDir.exists()) {
                // Create target directory
                alpineDir.mkdirs()

                // Use system tar command to extract.
                // -z: gzip, -x: extract, -f: file, -C: target directory
                val cmd = "tar -zxf ${rootfsTar.absolutePath} -C ${alpineDir.absolutePath}"
                try {
                    val process = Runtime.getRuntime().exec(cmd)
                    process.waitFor()
                    if (process.exitValue() != 0) {
                        // If gzip extraction fails, try without the z parameter (in case some tar versions do not
                        // support it)
                        Runtime.getRuntime()
                            .exec("tar -xf ${rootfsTar.absolutePath} -C ${alpineDir.absolutePath}")
                            .waitFor()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. Ensure the init script is up-to-date (overwrite on every startup for easier debugging)
            binDir.mkdirs()
            libDir.mkdirs()

            // Copy Proot dependencies to local/lib (Required by ReTerminal logic)
            copyAsset(context, "libtalloc.so.2", File(libDir, "libtalloc.so.2"))
            // Copy Proot to local/bin
            copyAsset(context, "proot", File(binDir, "proot"))
            File(binDir, "proot").setExecutable(true)

            // 5. Copy helper scripts from assets/terminal/scripts to local/alpine/root/scripts
            val scriptsDir = File(alpineDir, "root/scripts")
            scriptsDir.mkdirs()
            val lspScriptsDir = File(scriptsDir, "lsp")
            lspScriptsDir.mkdirs()

            fun copyScriptsRecursively(assetPath: String, destDir: File) {
                try {
                    val list = context.assets.list(assetPath) ?: emptyArray()
                    for (file in list) {
                        val subAsset = "$assetPath/$file"
                        val subDest = File(destDir, file)
                        val subList = context.assets.list(subAsset)
                        val isDir = !subList.isNullOrEmpty()
                        if (isDir) {
                            subDest.mkdirs()
                            copyScriptsRecursively(subAsset, subDest)
                        } else {
                            context.assets.open(subAsset).use { input ->
                                FileOutputStream(subDest).use { output -> input.copyTo(output) }
                            }
                            subDest.setExecutable(true)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            copyScriptsRecursively("terminal/scripts", scriptsDir)
        }
    }

    private fun copyAsset(context: Context, assetName: String, destFile: File) {
        // Only copy when the file does not exist (except for scripts, which are usually small and need updates)
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
