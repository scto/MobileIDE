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
import com.scto.mobile.ide.core.common.utils.LogCatcher
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

            val optionsFile = File(prefixDir, "local/setup_options.properties")
            val generalPrefs = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            val jdk = generalPrefs.getString("welcome_install_jdk_version", "17") ?: "17"
            val gradle = generalPrefs.getString("welcome_install_gradle_version", "apt") ?: "apt"
            val sdk = generalPrefs.getString("welcome_install_sdk_version", "35") ?: "35"
            val buildTools = generalPrefs.getString("welcome_install_build_tools_version", "35.0.0") ?: "35.0.0"
            val cmdline = generalPrefs.getBoolean("welcome_install_cmdline_tools", true)
            val git = generalPrefs.getBoolean("welcome_install_git", true)

            try {
                optionsFile.parentFile?.mkdirs()
                optionsFile.writeText(
                    """
                    INSTALL_JDK="$jdk"
                    INSTALL_GRADLE="$gradle"
                    INSTALL_SDK="$sdk"
                    INSTALL_BUILD_TOOLS="$buildTools"
                    INSTALL_CMDLINE_TOOLS="${if (cmdline) "true" else "false"}"
                    INSTALL_GIT="${if (git) "true" else "false"}"
                """
                        .trimIndent()
                )
            } catch (e: Exception) {
                LogCatcher.e("SetupWorker", "Failed to write setup_options.properties", e)
            }

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

            // 4. Place proot + libs in local/bin and local/lib.
            onStatusChanged?.invoke("Basis-Komponenten werden vorbereitet...")
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

            // Copy terminal script assets to local/bin and make them executable
            forceCopyAsset(context, "terminal/init-host.sh", File(binDir, "init-host"))
            forceCopyAsset(context, "terminal/init.sh", File(binDir, "init"))
            forceCopyAsset(context, "terminal/utils.sh", File(binDir, "utils"))
            forceCopyAsset(context, "terminal/setup.sh", File(binDir, "setup"))
            forceCopyAsset(context, "terminal/sandbox.sh", File(binDir, "sandbox"))
            forceCopyAsset(context, "terminal/universal_runner.sh", File(binDir, "universal_runner"))
            forceCopyAsset(context, "terminal/termux-x11.sh", File(binDir, "termux-x11"))
            forceCopyAsset(context, "terminal/bin/ideenv", File(binDir, "ideenv"))
            forceCopyAsset(context, "terminal/bin/idesetup", File(binDir, "idesetup"))

            val lspDir = File(binDir, "lsp").apply { mkdirs() }
            val lspAssets = context.assets.list("terminal/lsp") ?: emptyArray()
            for (asset in lspAssets) {
                forceCopyAsset(context, "terminal/lsp/$asset", File(lspDir, asset))
                File(lspDir, asset).setExecutable(true)
            }

            File(binDir, "init-host").setExecutable(true)
            File(binDir, "init").setExecutable(true)
            File(binDir, "utils").setExecutable(true)
            File(binDir, "setup").setExecutable(true)
            File(binDir, "sandbox").setExecutable(true)
            File(binDir, "universal_runner").setExecutable(true)
            File(binDir, "termux-x11").setExecutable(true)
            File(binDir, "ideenv").setExecutable(true)
            File(binDir, "idesetup").setExecutable(true)

            distroDir.mkdirs()
            val sandboxLink = File(prefixDir, "local/sandbox")
            var symlinkCreated = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    if (sandboxLink.exists() || java.nio.file.Files.isSymbolicLink(sandboxLink.toPath())) {
                        sandboxLink.delete()
                    }
                    java.nio.file.Files.createSymbolicLink(sandboxLink.toPath(), distroDir.toPath())
                    symlinkCreated = true
                } catch (e: Exception) {
                    LogCatcher.e("SetupWorker", "Failed to create sandbox symlink via Files", e)
                }
            }
            if (!symlinkCreated) {
                try {
                    Runtime.getRuntime()
                        .exec(arrayOf("ln", "-snf", distroDir.absolutePath, sandboxLink.absolutePath))
                        .waitFor()
                } catch (ex: Exception) {
                    LogCatcher.e("SetupWorker", "Fallback symlink creation failed", ex)
                }
            }

            // Copy rootfs archive to the cache directory as sandbox.tar.gz
            val sandboxTar = File(context.cacheDir, "sandbox.tar.gz")
            if (rootfsTar.exists()) {
                rootfsTar.copyTo(sandboxTar, overwrite = true)
            }

            // Execute setup.sh in the background to extract and install all tools
            onStatusChanged?.invoke("Extraktion und Installation wird gestartet...")
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val libProot = File(nativeLibDir, "libproot.so")
            val prootExec = if (libProot.exists()) libProot.absolutePath else File(binDir, "proot").absolutePath

            val pb = ProcessBuilder("sh", File(binDir, "setup").absolutePath, "true")
            val pbEnv = pb.environment()
            pbEnv["PATH"] = "${System.getenv("PATH")}:/sbin:${binDir.absolutePath}"
            pbEnv["HOME"] = "/home"
            pbEnv["TERM"] = "xterm-256color"
            pbEnv["LANG"] = "C.UTF-8"
            pbEnv["PREFIX"] = prefixDir.absolutePath
            pbEnv["LOCAL"] = "${prefixDir.absolutePath}/local"
            pbEnv["LD_LIBRARY_PATH"] = libDir.absolutePath
            pbEnv["LINKER"] =
                if (File("/system/bin/linker64").exists()) "/system/bin/linker64" else "/system/bin/linker"
            pbEnv["PROOT_TMP_DIR"] = context.cacheDir.absolutePath
            pbEnv["TMPDIR"] = context.cacheDir.absolutePath
            pbEnv["PROOT"] = prootExec
            pbEnv["PROOT_EXEC"] = prootExec
            pbEnv["TMP_DIR"] = context.cacheDir.absolutePath
            pbEnv["PRIVATE_DIR"] = context.filesDir.absolutePath
            pbEnv["EXT_HOME"] = "${prefixDir.absolutePath}/local/${distroName}/root"

            if (File(nativeLibDir, "libproot-loader.so").exists()) {
                pbEnv["PROOT_LOADER"] = "${nativeLibDir}/libproot-loader.so"
            }
            if (File(nativeLibDir, "libproot-loader32.so").exists()) {
                pbEnv["PROOT_LOADER32"] = "${nativeLibDir}/libproot-loader32.so"
            }

            pb.redirectErrorStream(true)
            val process = pb.start()

            // Read setup.sh output line by line, strip ANSI color sequences, and update screen status
            val ansiRegex = Regex("\u001B\\[[;\\d]*[a-zA-Z]")
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val cleanLine = (line ?: "").replace(ansiRegex, "").trim()
                    if (cleanLine.isNotEmpty()) {
                        LogCatcher.i("SetupWorker", "[setup.sh] $cleanLine")
                        withContext(Dispatchers.Main) { onStatusChanged?.invoke(cleanLine) }
                    }
                }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IllegalStateException("Setup-Skript fehlgeschlagen mit Exit-Code $exitCode")
            }

            if (sandboxTar.exists()) {
                sandboxTar.delete()
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

    private fun forceCopyAsset(context: Context, assetName: String, destFile: File) {
        try {
            destFile.parentFile?.mkdirs()
            context.assets.open(assetName).use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            LogCatcher.e("SetupWorker", "Failed to force copy asset $assetName", e)
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

            val closeBehavior = com.scto.mobile.ide.core.terminal.settings.Settings.terminal_close_behavior
            val fontSize = com.scto.mobile.ide.core.terminal.settings.Settings.terminal_font_size
            val colorScheme = com.scto.mobile.ide.core.terminal.settings.Settings.terminal_colorscheme
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
