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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InstallState {
    object Idle : InstallState
    object InstallingRootfs : InstallState
    object InstallingDistribution : InstallState
    object AwaitingJdkSelection : InstallState
    data class InstallingJdk(val version: String) : InstallState
    object AwaitingBuildToolsSelection : InstallState
    data class InstallingBuildTools(val version: String) : InstallState
    object Success : InstallState
    data class Error(val message: String) : InstallState
}

data class SetupState(
    val isActive: Boolean = false,
    val installState: InstallState = InstallState.Idle,
    val status: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val logs: List<String> = emptyList(),
    val startTimeMs: Long = 0L,
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val selectedJdk: String = "openjdk-21",
    val selectedBuildTools: String = "build-tools-35.0.1",
    val showToolchainDialog: Boolean = false
) {
    val percentage: Float
        get() = when {
            totalBytes > 0L -> (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            totalSteps > 0 && currentStep > 0 -> (currentStep.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
            else -> -1f
        }
}

object SetupWorker {
    private val _setupState = MutableStateFlow(SetupState())
    val setupState: StateFlow<SetupState> = _setupState.asStateFlow()
    private var setupJob: Job? = null

    private fun getDistroName(context: Context): String {
        return context
            .getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
    }

    fun isTerminalInstalled(context: Context): Boolean {
        val filesDir = context.filesDir
        val prefixDir = filesDir.parentFile ?: return false
        val isInstalledPref = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getBoolean("is_terminal_installed", false)
        val isMarkerOk = File(prefixDir, "local/.terminal_setup_ok_DO_NOT_REMOVE").exists()
        return isInstalledPref && isMarkerOk
    }

    fun startSetupIfNeeded(context: Context) {
        if (isTerminalInstalled(context)) {
            if (!_setupState.value.isSuccess) {
                _setupState.value = SetupState(isActive = false, installState = InstallState.Success, isSuccess = true)
            }
            return
        }
        if (_setupState.value.isActive) return
        
        startSequentialSetup(context)
    }

    fun startSequentialSetup(context: Context) {
        if (isTerminalInstalled(context)) {
            _setupState.value = SetupState(isActive = false, installState = InstallState.Success, isSuccess = true)
            return
        }
        if (_setupState.value.isActive) return

        setupJob = CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            try {
                // 1. Rootfs Phase
                _setupState.value = SetupState(
                    isActive = true,
                    installState = InstallState.InstallingRootfs,
                    status = "Installiere Rootfs...",
                    currentStep = 1,
                    totalSteps = 5,
                    startTimeMs = startTime,
                    logs = listOf("Starte RootFS Download & Installation...")
                )

                prepareEnvironment(context)

                // 3. Pause & Prompt JDK Selection
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.AwaitingJdkSelection,
                    status = "Warte auf OpenJDK-Auswahl...",
                    currentStep = 3
                )
            } catch (e: Exception) {
                LogCatcher.e("SetupWorker", "Sequential setup failed", e)
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(e.message ?: "Setup-Fehler"),
                    error = e.message ?: "Setup-Fehler"
                )
            }
        }
    }

    suspend fun reinstallTerminal(
        context: Context
    ) {
        withContext(Dispatchers.IO) {
            LogCatcher.i("SetupWorker", "reinstallTerminal starting...")
            _setupState.value = _setupState.value.copy(status = "Alte Installation wird gelöscht...", isActive = true)
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

            prepareEnvironment(context)
            withContext(Dispatchers.Main) {
                _setupState.value = SetupState(isActive = false, isSuccess = true)
                SessionManager.addNewSession(context)
            }
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
        context: Context
    ) {
        withContext(Dispatchers.IO) {
            LogCatcher.i("SetupWorker", "prepareEnvironment starting...")
            logTerminalSetup(context)
            _setupState.value = _setupState.value.copy(status = "Umgebung wird vorbereitet...")
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
                _setupState.value = _setupState.value.copy(status = "PRoot wird eingerichtet...")
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
                        Downloader.downloadProot(context, onProgress = { downloaded, total ->
                            _setupState.value = _setupState.value.copy(downloadedBytes = downloaded, totalBytes = total)
                        })
                    } catch (e: Exception) {
                        LogCatcher.e("SetupWorker", "Failed to download proot", e)
                    }
                }
            }

            // 2. Setup libtalloc (downloading from custom repo).
            _setupState.value = _setupState.value.copy(status = "Bibliotheken werden kopiert...")
            val tallocDest = File(filesDir, "libtalloc.so.2")
            try {
                LogCatcher.i("SetupWorker", "Downloading libtalloc via Downloader.")
                Downloader.downloadTalloc(context, onProgress = { downloaded, total ->
                    _setupState.value = _setupState.value.copy(downloadedBytes = downloaded, totalBytes = total)
                })
            } catch (e: Exception) {
                LogCatcher.e("SetupWorker", "Failed to download libtalloc", e)
            }

            // 3. Download rootfs archive (from GitHub Releases, arch-aware).
            val rootfsTar = File(filesDir, "$distroName.tar.gz")
            if (!rootfsTar.exists() || rootfsTar.length() == 0L) {
                _setupState.value = _setupState.value.copy(status = "Linux RootFS wird heruntergeladen...")
                try {
                    LogCatcher.i("SetupWorker", "Downloading rootfs archive.")
                    Downloader.downloadRootFs(context, distro = distroName, onProgress = { downloaded, total ->
                        _setupState.value = _setupState.value.copy(downloadedBytes = downloaded, totalBytes = total)
                    })
                } catch (e: Exception) {
                    LogCatcher.e("SetupWorker", "Rootfs download failed.", e)
                    throw IllegalStateException("RootFS Download fehlgeschlagen. Bitte Internetverbindung prüfen.", e)
                }
            }
            if (!rootfsTar.exists() || rootfsTar.length() == 0L) {
                throw IllegalStateException("RootFS Datei fehlt nach dem Download.")
            }

            // 4. Place proot + libs in local/bin and local/lib.
            _setupState.value = _setupState.value.copy(status = "Basis-Komponenten werden vorbereitet...")
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
                val prootFile = File(binDir, "proot")
                prootSrc.copyTo(prootFile, overwrite = true)
                setFileExecutable(prootFile)
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
                val lspFile = File(lspDir, asset)
                forceCopyAsset(context, "terminal/lsp/$asset", lspFile)
                setFileExecutable(lspFile)
            }

            setFileExecutable(File(binDir, "init-host"))
            setFileExecutable(File(binDir, "init"))
            setFileExecutable(File(binDir, "utils"))
            setFileExecutable(File(binDir, "setup"))
            setFileExecutable(File(binDir, "sandbox"))
            setFileExecutable(File(binDir, "universal_runner"))
            setFileExecutable(File(binDir, "termux-x11"))
            setFileExecutable(File(binDir, "ideenv"))
            setFileExecutable(File(binDir, "idesetup"))

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
            _setupState.value = _setupState.value.copy(status = "Extraktion und Installation wird gestartet...")
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
                        withContext(Dispatchers.Main) { 
                            val currentLogs = _setupState.value.logs + cleanLine
                            _setupState.value = _setupState.value.copy(status = cleanLine, logs = currentLogs)
                        }
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

            File(prefixDir, "local/.terminal_setup_ok_DO_NOT_REMOVE").delete()

            // Post-Install Trigger: Pause setup flow and prompt user for toolchain selection
            withContext(Dispatchers.Main) {
                _setupState.value = _setupState.value.copy(
                    showToolchainDialog = true,
                    status = "Basis-System installiert. Bitte Entwicklungstools wählen.",
                    currentStep = 4
                )
            }
        }
    }

    fun dismissToolchainDialog() {
        _setupState.value = _setupState.value.copy(showToolchainDialog = false)
    }

    fun clearLogs() {
        _setupState.value = _setupState.value.copy(logs = emptyList())
    }

    fun generateToolchainCommand(selectedTools: Set<String>, distro: String): String {
        val packages = mutableListOf<String>()
        val isApk = distro.equals("alpine", ignoreCase = true)

        for (tool in selectedTools) {
            when {
                tool == "openjdk-17" -> packages.add(if (isApk) "openjdk17" else "openjdk-17-jdk")
                tool == "openjdk-21" -> packages.add(if (isApk) "openjdk21" else "openjdk-21-jdk")
                tool == "openjdk-24" -> packages.add(if (isApk) "openjdk24" else "openjdk-24-jdk")
                tool.startsWith("build-tools") -> {
                    if (isApk) {
                        packages.add("build-base")
                    } else {
                        packages.add("build-essential")
                    }
                }
                tool == "cmake" -> packages.add("cmake")
                tool == "build-essential" -> {
                    if (isApk) {
                        packages.add("build-base git")
                    } else {
                        packages.add("build-essential git")
                    }
                }
            }
        }

        if (packages.isEmpty()) return "echo 'Keine Entwicklungstools ausgewählt.'"

        return if (isApk) {
            "apk update && apk add --no-cache ${packages.joinToString(" ")}"
        } else {
            "DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -y ${packages.joinToString(" ")}"
        }
    }

    fun confirmJdkSelection(context: Context, jdkVersion: String) {
        setupJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.InstallingJdk(jdkVersion),
                    selectedJdk = jdkVersion,
                    status = "Installiere OpenJDK ($jdkVersion)...",
                    logs = _setupState.value.logs + "Installiere OpenJDK $jdkVersion...",
                    currentStep = 3
                )

                installSingleToolchainPackage(context, jdkVersion)

                // Move to Phase 4: Build Tools Selection
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.AwaitingBuildToolsSelection,
                    status = "Warte auf Build Tools-Auswahl...",
                    currentStep = 4
                )
            } catch (e: Exception) {
                LogCatcher.e("SetupWorker", "JDK installation failed", e)
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(e.message ?: "JDK-Installation fehlgeschlagen"),
                    error = e.message ?: "JDK-Installation fehlgeschlagen"
                )
            }
        }
    }

    fun confirmBuildToolsSelection(context: Context, buildToolsVersion: String) {
        setupJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.InstallingBuildTools(buildToolsVersion),
                    selectedBuildTools = buildToolsVersion,
                    status = "Installiere Build Tools ($buildToolsVersion)...",
                    logs = _setupState.value.logs + "Installiere Build Tools $buildToolsVersion...",
                    currentStep = 5
                )

                installSingleToolchainPackage(context, buildToolsVersion)

                // Phase 5: Completion & Persistent Settings
                val filesDir = context.filesDir
                val prefixDir = filesDir.parentFile!!
                File(prefixDir, "local/.terminal_setup_ok_DO_NOT_REMOVE").createNewFile()

                context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_terminal_installed", true)
                    .putString("installed_openjdk_version", _setupState.value.selectedJdk)
                    .putString("installed_build_tools_version", _setupState.value.selectedBuildTools)
                    .apply()

                _setupState.value = SetupState(
                    isActive = false,
                    installState = InstallState.Success,
                    isSuccess = true,
                    status = "Installation erfolgreich!"
                )

                withContext(Dispatchers.Main) {
                    SessionManager.addNewSession(context)
                }
            } catch (e: Exception) {
                LogCatcher.e("SetupWorker", "Build Tools installation failed", e)
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(e.message ?: "Build-Tools-Installation fehlgeschlagen"),
                    error = e.message ?: "Build-Tools-Installation fehlgeschlagen"
                )
            }
        }
    }

    private suspend fun installSingleToolchainPackage(context: Context, toolName: String) {
        val distroName = getDistroName(context)
        val cmd = generateToolchainCommand(setOf(toolName), distroName)

        val filesDir = context.filesDir
        val prefixDir = filesDir.parentFile!!
        val binDir = File(prefixDir, "local/bin")
        val libDir = File(prefixDir, "local/lib")
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val libProot = File(nativeLibDir, "libproot.so")
        val prootExec = if (libProot.exists()) libProot.absolutePath else File(binDir, "proot").absolutePath

        val initHostScript = File(binDir, "init-host")
        if (initHostScript.exists()) {
            val pb = ProcessBuilder("sh", initHostScript.absolutePath, "bash", "-c", cmd)
            val pbEnv = pb.environment()
            pbEnv["PATH"] = "${System.getenv("PATH")}:/sbin:${binDir.absolutePath}"
            pbEnv["HOME"] = "/home"
            pbEnv["TERM"] = "xterm-256color"
            pbEnv["LANG"] = "C.UTF-8"
            pbEnv["PREFIX"] = prefixDir.absolutePath
            pbEnv["LOCAL"] = "${prefixDir.absolutePath}/local"
            pbEnv["LD_LIBRARY_PATH"] = libDir.absolutePath
            pbEnv["PROOT"] = prootExec
            pbEnv["PROOT_EXEC"] = prootExec
            pbEnv["TMPDIR"] = context.cacheDir.absolutePath
            pbEnv["EXT_HOME"] = "${prefixDir.absolutePath}/local/${distroName}/root"
            pb.redirectErrorStream(true)
            
            try {
                val process = pb.start()
                val ansiRegex = Regex("\u001B\\[[;\\d]*[a-zA-Z]")
                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val cleanLine = (line ?: "").replace(ansiRegex, "").trim()
                        if (cleanLine.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                val newLogs = _setupState.value.logs + cleanLine
                                _setupState.value = _setupState.value.copy(status = cleanLine, logs = newLogs)
                            }
                        }
                    }
                }
                process.waitFor()
            } catch (e: Exception) {
                LogCatcher.e("SetupWorker", "Toolchain package installation failed", e)
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

    private fun setFileExecutable(file: File) {
        val success = file.setExecutable(true, false)
        if (!success) {
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", file.absolutePath)).waitFor()
            } catch (_: Exception) {}
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
