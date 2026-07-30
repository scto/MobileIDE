/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  Thomas Schmid  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.features.terminal.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RootFsSetupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(message: String, cause: Throwable? = null) : RootFsSetupException(message, cause)
    class IncompleteDownloadError(message: String, cause: Throwable? = null) : RootFsSetupException(message, cause)
    class StorageError(message: String, cause: Throwable? = null) : RootFsSetupException(message, cause)
    class ScriptExecutionError(message: String, cause: Throwable? = null) : RootFsSetupException(message, cause)
}

data class SetupConfig(
    val jdkVersion: String? = null,
    val buildToolsVersion: String? = null,
    val platformVersion: String? = null,
    val ndkVersion: String? = null,
    val cmakeVersion: String? = null
) {
    fun isComplete(): Boolean =
        jdkVersion != null && buildToolsVersion != null &&
        platformVersion != null && ndkVersion != null && cmakeVersion != null
}

sealed interface InstallState {
    object Idle : InstallState
    object InstallingRootfs : InstallState
    object InstallingDistribution : InstallState
    object InstallingBaseTools : InstallState
    object AwaitingJdkSelection : InstallState
    data class InstallingJdk(val version: String) : InstallState
    object AwaitingBuildToolsSelection : InstallState
    data class InstallingBuildTools(val version: String) : InstallState
    object AwaitingPlatformSelection : InstallState
    data class InstallingPlatform(val version: String) : InstallState
    object AwaitingNdkSelection : InstallState
    data class InstallingNdk(val version: String) : InstallState
    object AwaitingCmakeSelection : InstallState
    data class InstallingCmake(val version: String) : InstallState
    object Success : InstallState
    data class Error(val message: String, val isRetryable: Boolean = true) : InstallState
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
    val totalSteps: Int = 8,
    val selectedJdk: String = "openjdk-21",
    val selectedBuildTools: String = "build-tools-35.0.1",
    val selectedPlatform: String = "35",
    val selectedNdk: String = "30.0.14904198",
    val selectedCmake: String = "3.22",
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

    fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

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
         // Timber initialized
        if (isTerminalInstalled(context)) {
            _setupState.value = SetupState(isActive = false, installState = InstallState.Success, isSuccess = true)
            return
        }
        if (_setupState.value.isActive) return

        setupJob = CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            try {
                if (!isNetworkConnected(context)) {
                    throw RootFsSetupException.NetworkError("Keine Internetverbindung verfügbar. Bitte Netzwerkeinstellungen prüfen.")
                }

                // 1. Rootfs & Distribution Phase
                _setupState.value = SetupState(
                    isActive = true,
                    installState = InstallState.InstallingRootfs,
                    status = "Installiere Rootfs...",
                    currentStep = 1,
                    totalSteps = 7,
                    startTimeMs = startTime,
                    logs = listOf("Starte RootFS Download & Installation...")
                )

                prepareEnvironment(context)

                // 2. Base Development Tools Phase (git, dev tools, box64, cmdline-tools)
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.InstallingBaseTools,
                    status = "Installiere Git, Dev-Tools, Box64 & Cmdline-Tools...",
                    currentStep = 3,
                    totalSteps = 7,
                    logs = _setupState.value.logs + "Installiere Git, Basis-Entwicklungstools, Box64 & Android Cmdline-Tools..."
                )

                installSingleToolchainPackage(context, "base-tools")

                // 3. Pause & Prompt JDK Selection
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.AwaitingJdkSelection,
                    status = "Basis-System & Tools installiert. Bitte OpenJDK wählen.",
                    currentStep = 4,
                    totalSteps = 7
                )
            } catch (e: Exception) {
                Timber.tag("SetupWorker").e("Setup-Fehler: ${e.message}", e)
                Timber.tag("SetupWorker").e(e, "Sequential setup failed")
                val userMsg = e.message ?: "Setup-Fehler aufgetreten"
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(userMsg, isRetryable = true),
                    error = userMsg,
                    logs = _setupState.value.logs + "FEHLER: $userMsg"
                )
            }
        }
    }

    suspend fun reinstallTerminal(
        context: Context
    ) {
        withContext(Dispatchers.IO) {
            Timber.tag("SetupWorker").i("reinstallTerminal starting...")
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

            try {
                if (!isNetworkConnected(context)) {
                    throw RootFsSetupException.NetworkError("Keine Internetverbindung verfügbar. Bitte Netzwerkeinstellungen prüfen.")
                }
                prepareEnvironment(context)
                withContext(Dispatchers.Main) {
                    _setupState.value = SetupState(isActive = false, isSuccess = true)
                    // SessionManager.addNewSession(context) removed as TerminalScreen manages sessions
                }
            } catch (e: Exception) {
                Timber.tag("SetupWorker").e("Reinstallation fehlgeschlagen: ${e.message}", e)
                Timber.tag("SetupWorker").e(e, "Reinstallation failed")
                val userMsg = e.message ?: "Reinstallation fehlgeschlagen"
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(userMsg, isRetryable = true),
                    error = userMsg,
                    logs = _setupState.value.logs + "FEHLER: $userMsg"
                )
            }
        }
    }

    fun resetTerminal(context: Context) {
        val list = ArrayList(SessionManager.sessions)
        list.forEach { SessionManager.removeSession(it) }
        DistroManager.currentProject = null
        // SessionManager.addNewSession(context) removed as TerminalScreen manages sessions
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
            Timber.tag("SetupWorker").i("prepareEnvironment starting...")
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
                Timber.tag("SetupWorker").e(e, "Failed to write setup_options.properties")
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
                        Timber.tag("SetupWorker").i("Copying native libproot.so to proot destination.")
                        libProot.copyTo(prootDest, overwrite = true)
                        prootDest.setExecutable(true)
                        success = true
                    } catch (e: Exception) {
                        Timber.tag("SetupWorker").e(e, "Failed to copy native libproot.so")
                    }
                }

                // If copy fails, try downloading
                if (!success) {
                    try {
                        Timber.tag("SetupWorker").i("Downloading proot.")
                        Downloader.downloadProot(context, onProgress = { downloaded, total ->
                            _setupState.value = _setupState.value.copy(downloadedBytes = downloaded, totalBytes = total)
                        })
                    } catch (e: Exception) {
                        Timber.tag("SetupWorker").e(e, "Failed to download proot")
                    }
                }
            }

            // 2. Setup libtalloc (downloading from custom repo).
            _setupState.value = _setupState.value.copy(status = "Bibliotheken werden kopiert...")
            val tallocDest = File(filesDir, "libtalloc.so.2")
            try {
                Timber.tag("SetupWorker").i("Downloading libtalloc via Downloader.")
                Downloader.downloadTalloc(context, onProgress = { downloaded, total ->
                    _setupState.value = _setupState.value.copy(downloadedBytes = downloaded, totalBytes = total)
                })
            } catch (e: Exception) {
                Timber.tag("SetupWorker").e(e, "Failed to download libtalloc")
            }

            // 3. Download rootfs archive (from GitHub Releases, arch-aware).
            val rootfsTar = File(filesDir, "$distroName.tar.gz")
            if (!rootfsTar.exists() || rootfsTar.length() < 1_000_000L) {
                _setupState.value = _setupState.value.copy(status = "Linux RootFS wird heruntergeladen...")
                try {
                    Timber.tag("SetupWorker").i("Downloading rootfs archive.")
                    Downloader.downloadRootFs(context, distro = distroName, onProgress = { downloaded, total ->
                        _setupState.value = _setupState.value.copy(downloadedBytes = downloaded, totalBytes = total)
                    })
                } catch (e: Exception) {
                    Timber.tag("SetupWorker").e("RootFS Download fehlgeschlagen: ${e.message}", e)
                    Timber.tag("SetupWorker").e(e, "Rootfs download failed.")
                    if (rootfsTar.exists() && rootfsTar.length() < 1_000_000L) {
                        rootfsTar.delete()
                    }
                    throw RootFsSetupException.NetworkError("RootFS Download fehlgeschlagen: ${e.message}", e)
                }
            }
            if (!rootfsTar.exists() || rootfsTar.length() < 1_000_000L) {
                rootfsTar.delete()
                throw RootFsSetupException.IncompleteDownloadError("RootFS Datei fehlt oder ist unvollständig nach dem Download.")
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
                    Timber.tag("SetupWorker").e(e, "Failed to copy downloaded libtalloc to libDir")
                }
            }

            val prootSrc = File(filesDir, "proot")
            if (prootSrc.exists()) {
                val prootFile = File(binDir, "proot")
                prootSrc.copyTo(prootFile, overwrite = true)
                setFileExecutable(prootFile)
            }

            // Copy terminal script assets to local/bin and make them executable
            forceCopyAsset(context, "terminal/shared_extraction.sh", File(binDir, "shared_extraction.sh"))
            forceCopyAsset(context, "terminal/shared_extraction.sh", File(prefixDir, "local/shared_extraction.sh"))
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

            setFileExecutable(File(binDir, "shared_extraction.sh"))
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
                    Timber.tag("SetupWorker").e(e, "Failed to create sandbox symlink via Files")
                }
            }
            if (!symlinkCreated) {
                try {
                    Runtime.getRuntime()
                        .exec(arrayOf("ln", "-snf", distroDir.absolutePath, sandboxLink.absolutePath))
                        .waitFor()
                } catch (ex: Exception) {
                    Timber.tag("SetupWorker").e(ex, "Fallback symlink creation failed")
                }
            }

            // Copy rootfs archive to the cache directory and proot_tmp as sandbox.tar.gz
            // App-private PROOT_TMP_DIR inside app files directory
            val prootTmpDir = File(context.filesDir, "usr/tmp").apply { mkdirs() }
            prootTmpDir.setReadable(true, false)
            prootTmpDir.setWritable(true, false)
            prootTmpDir.setExecutable(true, false)

            // Validate write permission
            val writeTest = File(prootTmpDir, ".write_test")
            val isWritable = try {
                if (writeTest.createNewFile() || writeTest.exists()) {
                    writeTest.delete()
                    true
                } else false
            } catch (e: Exception) { false }

            if (!isWritable) {
                Timber.tag("SetupWorker").e("PROOT_TMP_DIR is not writable: ${prootTmpDir.absolutePath}")
                throw IllegalStateException("PROOT_TMP_DIR is not writable: ${prootTmpDir.absolutePath}")
            }

            val sandboxTarCache = File(context.cacheDir, "sandbox.tar.gz")
            val sandboxTarTmp = File(prootTmpDir, "sandbox.tar.gz")
            if (rootfsTar.exists()) {
                rootfsTar.copyTo(sandboxTarCache, overwrite = true)
                rootfsTar.copyTo(sandboxTarTmp, overwrite = true)
            }

            // Pre-generate mobileide-environment.properties
            writeEnvironmentProperties(context)

            // Execute setup.sh in the background to extract and install all tools
            _setupState.value = _setupState.value.copy(
                installState = InstallState.InstallingDistribution,
                status = "Installiere Distribution...",
                currentStep = 2
            )
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
            pbEnv["PROOT_TMP_DIR"] = prootTmpDir.absolutePath
            pbEnv["TMPDIR"] = prootTmpDir.absolutePath
            pbEnv["TMP_DIR"] = prootTmpDir.absolutePath
            pbEnv["PROOT"] = prootExec
            pbEnv["PROOT_EXEC"] = prootExec
            pbEnv["PRIVATE_DIR"] = context.filesDir.absolutePath
            pbEnv["EXT_HOME"] = "${prefixDir.absolutePath}/local/${distroName}/root"

            val loader64File = listOf(
                File(nativeLibDir, "libproot-loader.so"),
                File(nativeLibDir, "libloader.so")
            ).firstOrNull { it.exists() }

            val loader32File = listOf(
                File(nativeLibDir, "libproot-loader32.so"),
                File(nativeLibDir, "libloader32.so")
            ).firstOrNull { it.exists() }

            if (loader64File != null) {
                loader64File.setExecutable(true, false)
                pbEnv["PROOT_LOADER"] = loader64File.absolutePath
            }
            if (loader32File != null) {
                loader32File.setExecutable(true, false)
                pbEnv["PROOT_LOADER32"] = loader32File.absolutePath
                pbEnv["PROOT_LOADER_32"] = loader32File.absolutePath
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
                        Timber.tag("SetupWorker").i("[setup.sh] $cleanLine")
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

            if (sandboxTarCache.exists()) {
                sandboxTarCache.delete()
            }
            if (sandboxTarTmp.exists()) {
                sandboxTarTmp.delete()
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
        val packages = mutableSetOf<String>()
        val isApk = distro.equals("alpine", ignoreCase = true)
        val customCmds = mutableListOf<String>()

        for (tool in selectedTools) {
            when {
                tool == "base-tools" -> {
                    if (isApk) {
                        packages.addAll(listOf("git", "curl", "wget", "zip", "unzip", "tar", "make", "gcc", "g++", "build-base"))
                        customCmds.add("apk add --no-cache box64 2>/dev/null || true")
                    } else {
                        packages.addAll(listOf("git", "curl", "wget", "zip", "unzip", "tar", "make", "gcc", "g++", "build-essential"))
                        customCmds.add("(apt-get update && apt-get install -y box64 2>/dev/null || (apt-get install -y wget && wget -O /tmp/box64.deb https://github.com/ptitSeb/box64/releases/download/v0.2.8/box64-debian-arm64.deb 2>/dev/null && dpkg -i /tmp/box64.deb 2>/dev/null || true))")
                    }
                    customCmds.add("(mkdir -p /root/android-sdk/cmdline-tools && wget -O /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip 2>/dev/null && unzip -o /tmp/cmdline-tools.zip -d /root/android-sdk/cmdline-tools/latest_tmp 2>/dev/null && mkdir -p /root/android-sdk/cmdline-tools/latest && cp -r /root/android-sdk/cmdline-tools/latest_tmp/*/* /root/android-sdk/cmdline-tools/latest/ 2>/dev/null || cp -r /root/android-sdk/cmdline-tools/latest_tmp/* /root/android-sdk/cmdline-tools/latest/ 2>/dev/null && rm -rf /tmp/cmdline-tools.zip /root/android-sdk/cmdline-tools/latest_tmp || true)")
                }
                tool == "openjdk-17" -> packages.add(if (isApk) "openjdk17" else "openjdk-17-jdk")
                tool == "openjdk-21" -> packages.add(if (isApk) "openjdk21" else "openjdk-21-jdk")
                tool == "openjdk-24" -> packages.add(if (isApk) "openjdk24" else "openjdk-24-jdk")
                tool == "git" -> packages.add("git")
                tool == "box64" -> {
                    if (isApk) {
                        packages.add("box64")
                    } else {
                        customCmds.add("apt-get install -y box64 2>/dev/null || (wget -O /tmp/box64.deb https://github.com/ptitSeb/box64/releases/download/v0.2.8/box64-debian-arm64.deb 2>/dev/null && dpkg -i /tmp/box64.deb 2>/dev/null || true)")
                    }
                }
                tool == "cmdline-tools" -> {
                    packages.addAll(listOf("curl", "wget", "unzip", "zip"))
                    customCmds.add("mkdir -p /root/android-sdk/cmdline-tools && wget -O /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip 2>/dev/null && unzip -o /tmp/cmdline-tools.zip -d /root/android-sdk/cmdline-tools/latest_tmp 2>/dev/null && mkdir -p /root/android-sdk/cmdline-tools/latest && cp -r /root/android-sdk/cmdline-tools/latest_tmp/*/* /root/android-sdk/cmdline-tools/latest/ 2>/dev/null || cp -r /root/android-sdk/cmdline-tools/latest_tmp/* /root/android-sdk/cmdline-tools/latest/ 2>/dev/null && rm -rf /tmp/cmdline-tools.zip /root/android-sdk/cmdline-tools/latest_tmp || true")
                }
                tool.startsWith("build-tools") -> {
                    val rawVer = tool.removePrefix("build-tools-").replace("-RC", "")
                    val ver = if (rawVer.startsWith("3")) rawVer else "35.0.1"
                    customCmds.add("yes | /root/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/root/android-sdk \"build-tools;$ver\" \"platforms;android-34\" \"platforms;android-35\" || true")
                }
                tool.startsWith("platform-") -> {
                    val apiVer = tool.removePrefix("platform-")
                    customCmds.add("yes | /root/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/root/android-sdk \"platforms;android-$apiVer\" \"platform-tools\" || true")
                }
                tool.startsWith("ndk") -> {
                    customCmds.add("yes | /root/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/root/android-sdk \"ndk-bundle\" || true")
                }
                tool == "cmake" -> packages.add("cmake")
                tool == "build-essential" -> {
                    if (isApk) {
                        packages.addAll(listOf("build-base", "git"))
                    } else {
                        packages.addAll(listOf("build-essential", "git"))
                    }
                }
            }
        }

        val dnsFix = "printf 'nameserver 8.8.8.8\\nnameserver 8.8.4.4\\nnameserver 1.1.1.1\\nnameserver 9.9.9.9\\n' > /etc/resolv.conf"

        val pkgCmd = if (packages.isNotEmpty()) {
            if (isApk) {
                "apk update && apk add --no-cache ${packages.joinToString(" ")}"
            } else {
                "DEBIAN_FRONTEND=noninteractive apt-get update -o Acquire::Retries=3 -o Acquire::http::Timeout=10 || true && DEBIAN_FRONTEND=noninteractive apt-get install -y --fix-missing ${packages.joinToString(" ")}"
            }
        } else ""

        val allCmds = mutableListOf<String>()
        allCmds.add(dnsFix)
        if (pkgCmd.isNotEmpty()) allCmds.add(pkgCmd)
        allCmds.addAll(customCmds)

        return if (allCmds.isEmpty()) "echo 'Keine Entwicklungstools ausgewählt.'" else allCmds.joinToString(" && ")
    }

    fun confirmJdkSelection(context: Context, jdkVersion: String) {
        setupJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.InstallingJdk(jdkVersion),
                    selectedJdk = jdkVersion,
                    status = "Installiere OpenJDK...",
                    logs = _setupState.value.logs + "Installiere OpenJDK $jdkVersion...",
                    currentStep = 4,
                    totalSteps = 8
                )

                installSingleToolchainPackage(context, jdkVersion)

                // Move to Phase 5: Build Tools Selection
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.AwaitingBuildToolsSelection,
                    status = "Warte auf Build Tools-Auswahl...",
                    currentStep = 5,
                    totalSteps = 8
                )
            } catch (e: Exception) {
                Timber.tag("SetupWorker").e(e, "JDK installation failed")
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
                    status = "Installiere Build Tools...",
                    logs = _setupState.value.logs + "Installiere Build Tools $buildToolsVersion...",
                    currentStep = 5,
                    totalSteps = 8
                )

                installSingleToolchainPackage(context, buildToolsVersion)

                // Move to Phase 6: Platform Selection
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.AwaitingPlatformSelection,
                    status = "Warte auf Platform SDK-Auswahl...",
                    currentStep = 6,
                    totalSteps = 8
                )
            } catch (e: Exception) {
                Timber.tag("SetupWorker").e(e, "Build Tools installation failed")
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(e.message ?: "Build-Tools-Installation fehlgeschlagen"),
                    error = e.message ?: "Build-Tools-Installation fehlgeschlagen"
                )
            }
        }
    }

    fun confirmPlatformSelection(context: Context, platformVersion: String) {
        setupJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.InstallingPlatform(platformVersion),
                    selectedPlatform = platformVersion,
                    status = "Installiere Android Platform SDK...",
                    logs = _setupState.value.logs + "Installiere Android Platform SDK android-$platformVersion...",
                    currentStep = 6,
                    totalSteps = 8
                )

                installSingleToolchainPackage(context, "platform-$platformVersion")

                // Move to Phase 7: NDK Selection
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.AwaitingNdkSelection,
                    status = "Warte auf NDK-Auswahl...",
                    currentStep = 7,
                    totalSteps = 8
                )
            } catch (e: Exception) {
                Timber.tag("SetupWorker").e(e, "Platform SDK installation failed")
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(e.message ?: "Platform SDK-Installation fehlgeschlagen"),
                    error = e.message ?: "Platform SDK-Installation fehlgeschlagen"
                )
            }
        }
    }

    fun confirmNdkSelection(context: Context, ndkVersion: String) {
        setupJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.InstallingNdk(ndkVersion),
                    selectedNdk = ndkVersion,
                    status = "Installiere NDK...",
                    logs = _setupState.value.logs + "Installiere NDK $ndkVersion...",
                    currentStep = 7,
                    totalSteps = 8
                )

                installSingleToolchainPackage(context, "ndk-$ndkVersion")

                // Move to Phase 8: CMake Selection
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.AwaitingCmakeSelection,
                    status = "Warte auf CMake-Auswahl...",
                    currentStep = 8,
                    totalSteps = 8
                )
            } catch (e: Exception) {
                Timber.tag("SetupWorker").e(e, "NDK installation failed")
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(e.message ?: "NDK-Installation fehlgeschlagen"),
                    error = e.message ?: "NDK-Installation fehlgeschlagen"
                )
            }
        }
    }

    fun confirmCmakeSelection(context: Context, cmakeVersion: String) {
        setupJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _setupState.value = _setupState.value.copy(
                    installState = InstallState.InstallingCmake(cmakeVersion),
                    selectedCmake = cmakeVersion,
                    status = "Installiere CMake...",
                    logs = _setupState.value.logs + "Installiere CMake $cmakeVersion...",
                    currentStep = 8,
                    totalSteps = 8
                )

                installSingleToolchainPackage(context, "cmake")

                writeEnvironmentProperties(context)

                // Save SetupConfig JSON to cache directory
                val cfg = SetupConfig(
                    jdkVersion = _setupState.value.selectedJdk,
                    buildToolsVersion = _setupState.value.selectedBuildTools,
                    platformVersion = _setupState.value.selectedPlatform,
                    ndkVersion = _setupState.value.selectedNdk,
                    cmakeVersion = _setupState.value.selectedCmake
                )
                try {
                    val configFile = File(context.cacheDir, "antigravity_setup_config.json")
                    val jsonStr = """
                        {
                          "jdkVersion": "${cfg.jdkVersion}",
                          "buildToolsVersion": "${cfg.buildToolsVersion}",
                          "platformVersion": "${cfg.platformVersion}",
                          "ndkVersion": "${cfg.ndkVersion}",
                          "cmakeVersion": "${cfg.cmakeVersion}"
                        }
                    """.trimIndent()
                    configFile.writeText(jsonStr)
                } catch (ex: Exception) {
                    Timber.tag("SetupWorker").e(ex, "Failed to write antigravity_setup_config.json")
                }

                // Phase 8: Completion & Persistent Settings
                val filesDir = context.filesDir
                val prefixDir = filesDir.parentFile!!
                File(prefixDir, "local/.terminal_setup_ok_DO_NOT_REMOVE").createNewFile()

                context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_terminal_installed", true)
                    .putString("installed_openjdk_version", _setupState.value.selectedJdk)
                    .putString("installed_build_tools_version", _setupState.value.selectedBuildTools)
                    .putString("installed_platform_version", _setupState.value.selectedPlatform)
                    .putString("installed_ndk_version", _setupState.value.selectedNdk)
                    .putString("installed_cmake_version", _setupState.value.selectedCmake)
                    .apply()

                _setupState.value = SetupState(
                    isActive = false,
                    installState = InstallState.Success,
                    isSuccess = true,
                    status = "Installation erfolgreich!"
                )

                withContext(Dispatchers.Main) {
                    // SessionManager.addNewSession(context) removed as TerminalScreen manages sessions
                }
            } catch (e: Exception) {
                Timber.tag("SetupWorker").e(e, "CMake installation failed")
                _setupState.value = _setupState.value.copy(
                    isActive = false,
                    installState = InstallState.Error(e.message ?: "CMake-Installation fehlgeschlagen"),
                    error = e.message ?: "CMake-Installation fehlgeschlagen"
                )
            }
        }
    }

    private fun writeEnvironmentProperties(context: Context) {
        try {
            val distroName = getDistroName(context)
            val prefixDir = context.filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$distroName")
            val envProps1 = File(distroDir, "root/etc/mobileide-environment.properties")
            val envProps2 = File(prefixDir, "local/mobileide-environment.properties")
            val envProps3 = File(context.filesDir, "mobileide-environment.properties")

            val buildToolsVer = _setupState.value.selectedBuildTools.removePrefix("build-tools-").replace("-RC", "")
            val propsMap = mutableMapOf<String, String>()
            propsMap["ANDROID_HOME"] = "/root/android-sdk"
            propsMap["ANDROID_SDK_ROOT"] = "/root/android-sdk"
            propsMap["ANDROID_NDK_HOME"] = "/root/android-sdk/ndk-bundle"
            propsMap["NDK_HOME"] = "/root/android-sdk/ndk-bundle"
            propsMap["CMAKE_HOME"] = "/usr"
            propsMap["PATH"] = "/root/android-sdk/cmdline-tools/latest/bin:/root/android-sdk/cmdline-tools/bin:/root/android-sdk/platform-tools:/root/android-sdk/build-tools/$buildToolsVer:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            propsMap["PROOT_TMP_DIR"] = File(context.filesDir, "usr/tmp").absolutePath

            val sb = java.lang.StringBuilder()
            for ((k, v) in propsMap) {
                sb.append("$k=$v\n")
            }
            val content = sb.toString()

            listOf(envProps1, envProps2, envProps3).forEach { file ->
                try {
                    file.parentFile?.mkdirs()
                    file.writeText(content)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Timber.tag("SetupWorker").e(e, "Failed to write mobileide-environment.properties")
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
                Timber.tag("SetupWorker").e(e, "Toolchain package installation failed")
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
            Timber.tag("SetupWorker").e(e, "Failed to force copy asset $assetName")
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

            Timber.tag("SetupWorker").i(sb.toString())
        } catch (e: Exception) {
            Timber.tag("SetupWorker").e(e, "Error generating terminal setup log")
        }
    }
}
