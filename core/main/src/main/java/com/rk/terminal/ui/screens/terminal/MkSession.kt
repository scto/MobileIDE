package com.rk.terminal.ui.screens.terminal

import android.content.Context
import com.rk.libcommons.alpineHomeDir
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.localBinDir
import com.rk.libcommons.localDir
import com.rk.libcommons.localLibDir
import com.rk.libcommons.pendingCommand
import com.rk.settings.Settings
import com.rk.terminal.App
import com.rk.terminal.App.Companion.getTempDir
import com.rk.terminal.BuildConfig
// import com.rk.terminal.ui.activities.terminal.MainActivity // 移除
import com.rk.terminal.ui.screens.settings.WorkingMode
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

object MkSession {
    fun createSession(
        context: Context, // 改为 Context
        sessionClient: TerminalSessionClient,
        session_id: String,
        workingMode: Int
    ): TerminalSession {
        // 使用 with(context) 简化代码
        with(context) {
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

            val workingDir = pendingCommand?.workingDir ?: alpineHomeDir().path

            val initFile: File = localBinDir().child("init-host")

            if (initFile.exists().not()) {
                initFile.createFileIfNot()
                // 修复 assets 读取和类型推断问题
                val scriptContent = assets.open("init-host.sh").bufferedReader().use { it.readText() }
                initFile.writeText(scriptContent)
            }

            localBinDir().child("init").apply {
                if (exists().not()) {
                    createFileIfNot()
                    val scriptContent = assets.open("init.sh").bufferedReader().use { it.readText() }
                    writeText(scriptContent)
                }
            }

            val nativeLibraryDir = applicationInfo.nativeLibraryDir

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
                "LINKER=${if (File("/system/bin/linker64").exists()) { "/system/bin/linker64" } else { "/system/bin/linker" }}",
                "NATIVE_LIB_DIR=$nativeLibraryDir",
                "PKG=${packageName}",
                "RISH_APPLICATION_ID=${packageName}",
                "PKG_PATH=${applicationInfo.sourceDir}",
                "PROOT_TMP_DIR=${getTempDir().child(session_id).also { if (it.exists().not()) { it.mkdirs() } }}",
                "TMPDIR=${getTempDir().absolutePath}"
            )

            if (File(nativeLibraryDir).child("libproot-loader32.so").exists()) {
                env.add("PROOT_LOADER32=$nativeLibraryDir/libproot-loader32.so")
            }

            if (File(nativeLibraryDir).child("libproot-loader.so").exists()) {
                env.add("PROOT_LOADER=$nativeLibraryDir/libproot-loader.so")
            }

            if (Settings.seccomp) {
                env.add("SECCOMP=1")
            }

            env.addAll(envVariables.mapNotNull { if (it.value != null) "${it.key}=${it.value}" else null })

            localDir().child("stat").apply {
                if (exists().not()) {
                    writeText(stat)
                }
            }

            localDir().child("vmstat").apply {
                if (exists().not()) {
                    writeText(vmstat)
                }
            }

            pendingCommand?.env?.let {
                env.addAll(it)
            }

            val args: Array<String>

            val shell = if (pendingCommand == null) {
                args = if (workingMode == WorkingMode.ALPINE) {
                    arrayOf("-c", initFile.absolutePath)
                } else {
                    arrayOf()
                }
                "/system/bin/sh"
            } else {
                args = pendingCommand!!.args
                pendingCommand!!.shell
            }

            pendingCommand = null
            return TerminalSession(
                shell,
                workingDir,
                args,
                env.toTypedArray(),
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                sessionClient,
            )
        }
    }
}