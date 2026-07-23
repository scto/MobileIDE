package com.scto.mobile.ide.plugins.kotlin.lsp

import android.app.Activity
import android.content.Context
import com.scto.mobile.ide.exec.isTerminalInstalled
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.BuiltinFileType
import com.scto.mobile.ide.core.common.files.sandboxHomeDir
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.exec.launchTerminal
import com.scto.mobile.ide.exec.TerminalCommand

class KotlinServer(
    override val icon: Icon? = BuiltinFileType.KOTLIN.icon,
    override val supportedExtensions: List<String> = listOf("kt", "kts"),
    override val installScript: File,
    val context: ExtensionContext,
) : ScriptedLspServer() {

    override val id = "kotlin"
    override val languageName = "Kotlin"
    override val serverName = "intellij-server"
    override val installId = "Kotlin Language Server"

    private val kotlinLspVersion = "262.8190.0"

    override suspend fun isInstalled(context: Context): Boolean {
        if (!isTerminalInstalled()) {
            return false
        }
        return sandboxHomeDir().child(".lsp/kotlin/bin/intellij-server").exists() &&
                sandboxHomeDir().child(".lsp/kotlin/bin/intellij-server").canExecute()
    }

    override fun install(activity: Activity) {
        launchInstaller(activity, kotlinLspVersion)
    }

    override fun uninstall(activity: Activity) {
        launchInstaller(activity, "--uninstall")
    }

    override fun update(activity: Activity) {
        launchInstaller(activity, "--update")
    }

    override suspend fun isUpdatable(context: Context): Boolean {
        val versionFile = sandboxHomeDir().child(".lsp/kotlin/version.txt")
        val currentVersionText = runCatching { versionFile.readText().trim() }.getOrNull() ?: return false
        return currentVersionText != kotlinLspVersion
    }

    override fun getConnectionConfig(): LspConnectionConfig {
        launchTerminal(
            activity = context.currentActivity!!,
            terminalCommand = TerminalCommand(
                exe = ".lsp/kotlin/bin/intellij-server",
                args = arrayOf("--socket", "localhost:8081"),
                id = "intellij-server",
                workingDir = "/home/",
            ),
        )
        return LspConnectionConfig.Socket("localhost", 8081)
    }
}