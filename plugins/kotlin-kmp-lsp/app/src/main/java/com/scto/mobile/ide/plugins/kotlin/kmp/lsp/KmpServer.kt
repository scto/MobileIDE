package com.scto.mobile.ide.plugins.kotlin.kmp.lsp


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
import com.scto.mobile.ide.extension.ExtensionContext
import java.io.File

class KmpServer(
    override val icon: Icon? = BuiltinFileType.KOTLIN.icon,
    override val supportedExtensions: List<String> = listOf("kt", "java", "swift"),
    override val installScript: File,
	val context: ExtensionContext,
) : ScriptedLspServer() {

    override val id = "kotlin-kmp-lsp"
    override val languageName = "Kotlin"
    override val serverName = "kotlin-kmp-lsp"
    override val installId = "KMP Language Server"

    private val kotlinLspVersion = "v0.24.0"

    override suspend fun isInstalled(context: Context): Boolean {
        if (!isTerminalInstalled()) {
            return false
        }
        return sandboxHomeDir().child(".lsp/kmp-lsp/kmp-lsp").exists()
    }

    override fun install(activity: Activity) {
        launchInstaller(activity, kotlinLspVersion)
    }

    override fun uninstall(activity: Activity) {
        launchInstaller(activity, "--uninstall")
    }

    override suspend fun hasUpdate(context: Context): Boolean = false

    override fun update(activity: Activity) {
        launchInstaller(activity, "--update")
    }

    override suspend fun isUpdatable(context: Context): Boolean {
        val versionFile = sandboxHomeDir().child(".lsp/kmp-lsp/version.txt")
        val currentVersionText = runCatching { versionFile.readText().trim() }.getOrNull() ?: return false
        return currentVersionText != kotlinLspVersion
    }

    override fun getConnectionConfig(): LspConnectionConfig {
    	return LspConnectionConfig.Process(arrayOf(
            sandboxHomeDir().child(".lsp/kmp-lsp/kmp-lsp").absolutePath
        ))
	}
}