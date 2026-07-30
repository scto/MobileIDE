package com.scto.mobile.ide.features.terminal.ui.terminal

import android.os.Environment
import com.scto.mobile.ide.core.terminal.libcommons.alpineDir
import com.scto.mobile.ide.core.terminal.libcommons.alpineHomeDir
import com.scto.mobile.ide.core.terminal.libcommons.ubuntuHomeDir
import com.scto.mobile.ide.core.terminal.libcommons.application
import com.scto.mobile.ide.core.terminal.libcommons.child
import com.scto.mobile.ide.core.terminal.libcommons.createFileIfNot
import com.scto.mobile.ide.core.terminal.libcommons.localBinDir
import com.scto.mobile.ide.core.terminal.libcommons.localDir
import com.scto.mobile.ide.core.terminal.libcommons.localLibDir
import com.scto.mobile.ide.core.terminal.libcommons.pendingCommand
import com.scto.mobile.ide.core.terminal.settings.Settings
import com.scto.mobile.ide.core.common.BuildConfig
import android.app.Activity
import com.scto.mobile.ide.core.terminal.model.WorkingMode
import com.scto.mobile.ide.core.terminal.settings.ShellType
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File
import java.io.FileOutputStream

object MkSession {
    fun createSession(
        activity: Activity, sessionClient: TerminalSessionClient, session_id: String,workingMode:Int
    ): TerminalSession {
        with(activity) {
            val envVariables = mapOf(
                "ANDROID_ART_ROOT" to System.getenv("ANDROID_ART_ROOT"),
                "ANDROID_DATA" to System.getenv("ANDROID_DATA"),
                "ANDROID_I18N_ROOT" to System.getenv("ANDROID_I18N_ROOT"),
                "ANDROID_ROOT" to System.getenv("ANDROID_ROOT"),
                "ANDROID_RUNTIME_ROOT" to System.getenv("ANDROID_RUNTIME_ROOT"),
                "ANDROID_TZDATA_ROOT" to System.getenv("ANDROID_TZDATA_ROOT"),
                "BOOTCLASSPATH" to System.getenv("BOOTCLASSPATH"),
                "DEX2OATBOOTCLASSPATH" to System.getenv("DEX2OATBOOTCLASSPATH"),
                "EXTERNAL_STORAGE" to System.getenv("EXTERNAL_STORAGE")
            )

            val defaultWorkingDir = when (workingMode) {
                WorkingMode.UBUNTU,
                WorkingMode.UBUNTU_ROOT -> ubuntuHomeDir().path
                else -> alpineHomeDir().path
            }
            val workingDir = pendingCommand?.workingDir ?: defaultWorkingDir

            fun copyAsset(name: String, dest: File) {
                dest.createFileIfNot()
                dest.writeText(assets.open("terminal/$name").bufferedReader().use { it.readText() })
                dest.setExecutable(true)
            }

            val initFile = localBinDir().child("init-host")
            copyAsset("init-host.sh", initFile)
            copyAsset("init.sh", localBinDir().child("init"))
            copyAsset("setup.sh", localBinDir().child("setup"))
            copyAsset("sandbox.sh", localBinDir().child("sandbox"))
            copyAsset("utils.sh", localBinDir().child("utils"))
            copyAsset("universal_runner.sh", localBinDir().child("universal_runner"))


            val tempDir = File(cacheDir, "tmp").also { it.mkdirs() }
            val sessionTmpDir = tempDir.child(session_id).also {
                if (it.exists()) {
                    it.deleteRecursively()
                }
                it.mkdirs()
            }

            val env = mutableListOf(
                "PATH=${System.getenv("PATH")}:/sbin:${localBinDir().absolutePath}",
                "HOME=/sdcard",
                "PUBLIC_HOME=${getExternalFilesDir(null)?.absolutePath}",
                "COLORTERM=truecolor",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "BIN=${localBinDir()}",
                "DEBUG=${BuildConfig.DEBUG}",
                "PREFIX=${filesDir.parentFile!!.path}",
                "LD_LIBRARY_PATH=${localLibDir().absolutePath}",
                "LINKER=${if(File("/system/bin/linker64").exists()){"/system/bin/linker64"}else{"/system/bin/linker"}}",
                "NATIVE_LIB_DIR=${applicationInfo.nativeLibraryDir}",
                "PKG=${packageName}",
                "RISH_APPLICATION_ID=${packageName}",
                "PKG_PATH=${applicationInfo.sourceDir}",
                "PROOT_TMP_DIR=${sessionTmpDir.absolutePath}",
                "TMPDIR=${tempDir.absolutePath}",
                "MOBILEIDE_DISTRO=${if (workingMode == WorkingMode.UBUNTU || workingMode == WorkingMode.UBUNTU_ROOT) "ubuntu" else "alpine"}"
            )

            // Do NOT set PROOT_LOADER/PROOT_LOADER32 — let proot use its embedded loader.
            // External loaders from jniLibs conflict with proot's ashmem_memfd extension
            // and fail on Android 10+ due to W^X (Write XOR Execute) policy.

            val shellPath = when (Settings.default_shell) {
                ShellType.BASH -> "/bin/bash"
                ShellType.ZSH -> "/bin/zsh"
                ShellType.ASH -> "/bin/ash"
                else -> "/bin/ash"
            }
            env.add("TERMIX_SHELL=$shellPath")

            env.addAll(envVariables.map { "${it.key}=${it.value}" })

            localDir().child("stat").apply {
                if (exists().not()){
                    writeText(stat)
                }
            }

            localDir().child("vmstat").apply {
                if (exists().not()){
                    writeText(vmstat)
                }
            }

            pendingCommand?.env?.let {
                env.addAll(it)
            }

            val args: Array<String>

            val shell = if (pendingCommand == null) {
                args = arrayOf("-c", initFile.absolutePath)
                "/system/bin/sh"
            } else{
                args = pendingCommand!!.args
                pendingCommand!!.shell
            }

            pendingCommand = null
            return TerminalSession(
                shell,
                workingDir,
                args,
                env.toTypedArray(),
                Settings.scrollback_lines,
                sessionClient,
            )
        }

    }
}
