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
 
// Document: java/com/example/sorarunrun/terminal/AlpineManager.kt

package com.scto.mobile.ide.ui.terminal

import android.content.Context

import com.rk.terminal.ui.screens.terminal.stat
import com.rk.terminal.ui.screens.terminal.vmstat

import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

import com.scto.mobile.ide.core.utils.WorkspaceManager

import java.io.File
import java.io.FileOutputStream

object AlpineManager {
    var currentProject: String? = null
    private fun getPrefixDir(context: Context): File = context.filesDir.parentFile!!
    private fun getLocalDir(context: Context): File = File(getPrefixDir(context), "local").apply { mkdirs() }
    private fun getBinDir(context: Context): File = File(getLocalDir(context), "bin").apply { mkdirs() }
    private fun getLibDir(context: Context): File = File(getLocalDir(context), "lib").apply { mkdirs() }

    /**
     * Build Proot command list
     * Used to start LSP background process (ProotStreamConnectionProvider)
     *
     * @param context Android context
     * @param command Array of commands to execute inside Alpine, e.g. ["vscode-html-language-server", "--stdio"]
     */
    fun buildProotCommand(context: Context, command: Array<String>): List<String> {
        val prefixDir = getPrefixDir(context)
        val alpineDir = File(prefixDir, "local/alpine")

        // [🔥 Fix 1] Get proot path with execution permissions
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val libProot = File(nativeLibDir, "libproot.so")

        // If files are not moved correctly, it won't be found here, so the first step must be done
        val prootExec = if (libProot.exists()) libProot.absolutePath else File(getBinDir(context), "proot").absolutePath

        val args = mutableListOf<String>()
        args.add(prootExec)
        args.add("--kill-on-exit")
        args.add("--link2symlink")
        args.add("--sysvipc")
        args.add("-L")
        args.add("-0")

        // [🔥 Fix 2] Mount points
        val mounts = listOf(
            "/proc", "/sys", "/dev", "/data", "/storage",
            "/system"
        )
        mounts.forEach {
            if (File(it).exists()) {
                args.add("-b")
                args.add(it)
            }
        }

        // Bind shared memory (required by Node.js)
        val tmpDir = File(alpineDir, "tmp").apply { mkdirs() }
        args.add("-b")
        args.add("${tmpDir.absolutePath}:/dev/shm")

        // [🔥 Fix 3] Must mount filesDir to absolute path for LSP to find files easily

        val rootHome = File(alpineDir, "root")
        if (!rootHome.exists()) {
            rootHome.mkdirs()
        }
        // Explicitly bind host directory to container's /root
        args.add("-b")
        args.add("${rootHome.absolutePath}:/root")

        args.add("-b")
        args.add(context.filesDir.absolutePath)
        args.add("-r")
        args.add(alpineDir.absolutePath)

        args.add("-w")
        args.add("/root")

        args.add("/usr/bin/env")
        args.add("-i")
        args.add("HOME=/root")
        // Ensure PATH contains npm's bin directory
        args.add("NODE_PATH=/root/lsp/node_modules")

        // 🔥🔥🔥 Modification 2: Add /root/lsp/node_modules/.bin to PATH (Although we plan to use absolute paths, this prevents internal call errors)
        args.add("PATH=/root/lsp/node_modules/.bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")

        args.add("LANG=C.UTF-8")
        args.add("TERM=xterm-256color")
        args.add("TMPDIR=/tmp")

        args.addAll(command)

        return args
    }

    /**
     * Get host environment variables required to run Proot
     * Mainly to inject libproot-loader.so to bypass Android's Seccomp restriction
     */
    fun getProotEnv(context: Context): Map<String, String> {
        val env = mutableMapOf<String, String>()
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        env["PROOT_TMP_DIR"] = context.cacheDir.absolutePath
        env["TMPDIR"] = context.cacheDir.absolutePath

        // [🔥 Core fix 🔥]
        // Tell Linker where to find libtalloc.so.2
        // SetupWorker will copy libtalloc.so.2 to filesDir and filesDir/local/lib
        // We add both of these paths to LD_LIBRARY_PATH
        val libPath = "${context.filesDir.absolutePath}:${context.filesDir.absolutePath}/local/lib:$nativeLibDir"
        env["LD_LIBRARY_PATH"] = libPath

        if (File(nativeLibDir, "libproot-loader.so").exists()) {
            env["PROOT_LOADER"] = "$nativeLibDir/libproot-loader.so"
        }
        if (File(nativeLibDir, "libproot-loader32.so").exists()) {
            env["PROOT_LOADER32"] = "$nativeLibDir/libproot-loader32.so"
        }
        return env
    }

    // --- Create Terminal Session (for UI Terminal) ---
    fun createSession(context: Context, client: TerminalSessionClient, projectPath: String? = null): TerminalSession {
        val binDir = getBinDir(context)
        val libDir = getLibDir(context)
        val prefixDir = getPrefixDir(context)
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        // 1. Ensure script exists
        val initHostScript = File(binDir, "init-host")
        if (!initHostScript.exists()) {
            copyAsset(context, "init-host.sh", initHostScript)
            copyAsset(context, "init.sh", File(binDir, "init"))
            initHostScript.setExecutable(true)
            File(binDir, "init").setExecutable(true)
        }
        
        val workspacePath = WorkspaceManager.getWorkspacePath(context)
        var versionName = "Unknown"
        var versionCode = 0L
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = pInfo.versionName ?: "Unknown"
            versionCode =
                pInfo.longVersionCode
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val targetProjectPath = projectPath ?: currentProject ?: ""
        // 2. Environment variables (Host environment)
        val env = mutableListOf(
            "PATH=${System.getenv("PATH")}:/sbin:${binDir.absolutePath}",
            "HOME=/root",
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
            "PREFIX=${prefixDir.absolutePath}",
            "LD_LIBRARY_PATH=${libDir.absolutePath}",
            // Try to adapt to different architecture linkers
            "LINKER=${if(File("/system/bin/linker64").exists()) "/system/bin/linker64" else "/system/bin/linker"}",
            "PROOT_TMP_DIR=${context.cacheDir.absolutePath}",
            "TMPDIR=${context.cacheDir.absolutePath}",

            // Custom environment variables
            "MOBILEIDE_VERSION_NAME=$versionName",
            "MOBILEIDE_VERSION_CODE=$versionCode",
            "MOBILEIDE_WORKSPACE=$workspacePath",
            "MOBILEIDE_PROJECT_DIR=$targetProjectPath"
        )

        // Inject Loader
        if (File(nativeLibDir, "libproot-loader.so").exists()) {
            env.add("PROOT_LOADER=$nativeLibDir/libproot-loader.so")
        }
        
        if (File(nativeLibDir, "libproot-loader32.so").exists()) {
            env.add("PROOT_LOADER32=$nativeLibDir/libproot-loader32.so")
        }

        // 3. Forge system files
        val statFile = File(getLocalDir(context), "stat")
        
        if (!statFile.exists()) statFile.writeText(stat)
        
        val vmstatFile = File(getLocalDir(context), "vmstat")
        
        if (!vmstatFile.exists()) vmstatFile.writeText(vmstat)

        // 4. Start Shell
        // Note: Still using init-host.sh here, if your init-host.sh hardcodes calling ./proot
        // It might have issues on Android 10+. But in the Terminal environment, it's usually more tolerant.
        // If Terminal also reports Permission denied, you need to modify init-host.sh or directly call libproot.so here
        val shell = "/system/bin/sh"
        val args = arrayOf("-cpp", initHostScript.absolutePath)

        return TerminalSession(
            shell,
            context.filesDir.absolutePath,
            args,
            env.toTypedArray(),
            TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
            client
        )
    }

    private fun copyAsset(context: Context, assetName: String, destFile: File) {
        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}