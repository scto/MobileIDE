package com.mobileide.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mobileide.app.data.LineType
import com.mobileide.app.data.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag

class TermuxBridge(private val context: Context) {
    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val TERMUX_HOME    = "/data/data/com.termux/files/home"
        const val TERMUX_PREFIX  = "/data/data/com.termux/files/usr"
    }

    fun isTermuxInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0); true
    } catch (_: Exception) { false }

    fun executeStream(command: String, workingDir: String = TERMUX_HOME): Flow<TerminalLine> = flow {
        Logger.info(LogTag.TERMUX, "exec: $command")
        Logger.info(LogTag.TERMUX, "execute: $command")
        Logger.info(LogTag.TERMUX, "execute: $command")
        emit(TerminalLine("$ $command", LineType.INPUT))
        try {
            val process = ProcessBuilder("sh", "-c", command)
                .directory(File(workingDir))
                .redirectErrorStream(false)
                .apply {
                    environment().apply {
                        put("HOME",             TERMUX_HOME)
                        put("PREFIX",           TERMUX_PREFIX)
                        put("PATH",             "$TERMUX_PREFIX/bin:$TERMUX_PREFIX/bin/applets:/usr/bin:/bin")
                        put("LD_LIBRARY_PATH",  "$TERMUX_PREFIX/lib")
                        put("LANG",             "en_US.UTF-8")
                        put("JAVA_HOME",        "$TERMUX_PREFIX/opt/openjdk")
                        put("GRADLE_USER_HOME", "$TERMUX_HOME/.gradle")
                        put("ANDROID_HOME",     "$TERMUX_HOME/android-sdk")
                        put("TERM",             "xterm-256color")
                    }
                }.start()

            val stdout = BufferedReader(InputStreamReader(process.inputStream))
            val stderr = BufferedReader(InputStreamReader(process.errorStream))
            var line: String?
            while (stdout.readLine().also { line = it } != null) {
                emit(TerminalLine(line!!, detectType(line!!)))
            }
            while (stderr.readLine().also { line = it } != null) {
                emit(TerminalLine(line!!, LineType.ERROR))
            }
            val exit = process.waitFor()
            if (exit == 0) { Logger.success(LogTag.TERMUX, "exit 0: $command"); emit(TerminalLine("✓ Exited with code 0", LineType.SUCCESS)) }
            else { Logger.error(LogTag.TERMUX, "exit $exit: $command"); emit(TerminalLine("✗ Exited with code $exit", LineType.ERROR)) }
        } catch (e: Exception) {
            Logger.error(LogTag.TERMUX, "executeStream error: ${e.message}", e.toString())
            emit(TerminalLine("Error: ${e.message}", LineType.ERROR))
        }
    }.flowOn(Dispatchers.IO)

    fun openTermux() {
        val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://termux.dev"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun gradleBuild(projectPath: String) =
        executeStream("cd '$projectPath' && gradle assembleDebug 2>&1", projectPath)

    fun gradleClean(projectPath: String) =
        executeStream("cd '$projectPath' && gradle clean 2>&1", projectPath)

    fun installApk(apkPath: String) =
        executeStream("am install -r '$apkPath' 2>&1")

    fun checkJava()   = executeStream("java -version 2>&1 && javac -version 2>&1")
    fun checkGradle() = executeStream("gradle --version 2>&1")
    fun checkGit()    = executeStream("git --version 2>&1")
    fun checkSdk()    = executeStream(
        "ls \"\${ANDROID_HOME:-\$HOME/android-sdk}/platforms/\" 2>/dev/null || echo 'SDK not configured'"
    )

    // Setup script — uses \$ to prevent Kotlin string interpolation
    fun getSetupScript() = buildString {
        appendLine("#!/data/data/com.termux/files/usr/bin/bash")
        appendLine("echo \"=== MobileIDE Termux Setup ===\"")
        appendLine("pkg update -y && pkg upgrade -y")
        appendLine("pkg install -y openjdk-17")
        appendLine("pkg install -y gradle")
        appendLine("pkg install -y git")
        appendLine("pkg install -y android-tools")
        appendLine("pkg install -y wget curl zip unzip")
        appendLine("echo 'export JAVA_HOME=\$PREFIX/opt/openjdk' >> ~/.bashrc")
        appendLine("echo 'export PATH=\$JAVA_HOME/bin:\$PATH' >> ~/.bashrc")
        appendLine("echo 'export ANDROID_HOME=\$HOME/android-sdk' >> ~/.bashrc")
        appendLine("source ~/.bashrc")
        appendLine("java -version && gradle --version")
        appendLine("echo \"=== Setup complete ===\"")
    }

    private fun detectType(line: String): LineType = when {
        line.contains("BUILD SUCCESSFUL", ignoreCase = true) -> LineType.SUCCESS
        line.contains("BUILD FAILED",     ignoreCase = true) -> LineType.ERROR
        line.startsWith("error:",          ignoreCase = true) -> LineType.ERROR
        line.startsWith("warning:",        ignoreCase = true) -> LineType.INFO
        line.startsWith(">") || line.startsWith(":") -> LineType.INFO
        else -> LineType.OUTPUT
    }
}
