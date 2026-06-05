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

import com.rk.terminal.ui.screens.terminal.stat
import com.rk.terminal.ui.screens.terminal.vmstat

import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

import com.scto.mobile.ide.core.utils.WorkspaceManager

import java.io.File

object AlpineManager {
    var currentProject: String? = null
    
    private fun getTerminalDir(context: Context): File = File(context.filesDir, "terminal").apply { mkdirs() }

    fun createSession(context: Context, client: TerminalSessionClient, projectPath: String? = null): TerminalSession {
        val terminalDir = getTerminalDir(context)
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        val initHostScript = File(terminalDir, "init-host.sh")
        
        val workspacePath = WorkspaceManager.getWorkspacePath(context)
        var versionName = "Unknown"
        var versionCode = 0L
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = pInfo.versionName ?: "Unknown"
            versionCode = pInfo.longVersionCode
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val targetProjectPath = projectPath ?: currentProject ?: ""
        
        val env = mutableListOf(
            "PATH=${System.getenv("PATH")}:/sbin:${terminalDir.absolutePath}:${terminalDir.absolutePath}/tools:${terminalDir.absolutePath}/bin",
            "HOME=/root",
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
            "PREFIX=${terminalDir.absolutePath}",
            "LOCAL=${terminalDir.absolutePath}",
            "LD_LIBRARY_PATH=${terminalDir.absolutePath}/bin:$nativeLibDir",
            "LINKER=${if(File("/system/bin/linker64").exists()) "/system/bin/linker64" else "/system/bin/linker"}",
            "PROOT_TMP_DIR=${context.cacheDir.absolutePath}",
            "TMPDIR=${context.cacheDir.absolutePath}",
            "MOBILEIDE_VERSION_NAME=$versionName",
            "MOBILEIDE_VERSION_CODE=$versionCode",
            "MOBILEIDE_WORKSPACE=$workspacePath",
            "MOBILEIDE_PROJECT_DIR=$targetProjectPath"
        )

        if (File(nativeLibDir, "libproot-loader.so").exists()) {
            env.add("PROOT_LOADER=$nativeLibDir/libproot-loader.so")
        }
        
        if (File(nativeLibDir, "libproot-loader32.so").exists()) {
            env.add("PROOT_LOADER32=$nativeLibDir/libproot-loader32.so")
        }

        val statFile = File(terminalDir, "stat")
        if (!statFile.exists()) statFile.writeText(stat)
        
        val vmstatFile = File(terminalDir, "vmstat")
        if (!vmstatFile.exists()) vmstatFile.writeText(vmstat)

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

    fun buildProotCommand(context: Context, command: Array<String>): List<String> {
        val terminalDir = getTerminalDir(context)
        val alpineDir = File(terminalDir, "local/alpine")

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val libProot = File(nativeLibDir, "libproot.so")
        val prootExec = if (libProot.exists()) libProot.absolutePath else File(terminalDir, "bin/proot").absolutePath

        val args = mutableListOf<String>()
        args.add(prootExec)
        args.add("--kill-on-exit")
        args.add("--link2symlink")
        args.add("--sysvipc")
        args.add("-L")
        args.add("-0")

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

        val tmpDir = File(alpineDir, "tmp").apply { mkdirs() }
        args.add("-b")
        args.add("${tmpDir.absolutePath}:/dev/shm")

        val rootHome = File(alpineDir, "root").apply { mkdirs() }
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
        args.add("NODE_PATH=/root/lsp/node_modules")
        args.add("PATH=/root/lsp/node_modules/.bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
        args.add("LANG=C.UTF-8")
        args.add("TERM=xterm-256color")
        args.add("TMPDIR=/tmp")

        args.addAll(command)

        return args
    }

    fun getProotEnv(context: Context): Map<String, String> {
        val env = mutableMapOf<String, String>()
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val terminalDir = getTerminalDir(context)

        env["PROOT_TMP_DIR"] = context.cacheDir.absolutePath
        env["TMPDIR"] = context.cacheDir.absolutePath

        val libPath = "${context.filesDir.absolutePath}:${terminalDir.absolutePath}/lib:$nativeLibDir"
        env["LD_LIBRARY_PATH"] = libPath

        if (File(nativeLibDir, "libproot-loader.so").exists()) {
            env["PROOT_LOADER"] = "$nativeLibDir/libproot-loader.so"
        }
        if (File(nativeLibDir, "libproot-loader32.so").exists()) {
            env["PROOT_LOADER32"] = "$nativeLibDir/libproot-loader32.so"
        }
        return env
    }
}