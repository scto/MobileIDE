package com.scto.mobile.ide.plugin.go

import android.app.Activity
import android.content.Context
import com.rk.exec.isTerminalInstalled
import com.rk.file.child
import com.rk.file.BuiltinFileType
import com.rk.file.sandboxHomeDir
import com.rk.icons.Icon
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer
import java.io.File
import com.rk.exec.launchTerminal
import com.rk.exec.TerminalCommand
import com.rk.activities.main.MainActivity
import com.rk.extension.ExtensionContext

class GoServer(
    override val icon: Icon? = BuiltinFileType.GO.icon,
    override val supportedExtensions: List<String> = listOf("go"),
    override val installScript: File,
	val context: ExtensionContext,
) : ScriptedLspServer() {

    override val id = "go"
    override val languageName = "Go"
    override val serverName = "gopls"
    override val installId = "Go Language Server"

    private val goLspVersion = "v0.22.0"

    override fun install(activity: Activity) {
        launchInstaller(activity, goLspVersion)
    }

    override fun uninstall(activity: Activity) {
        launchInstaller(activity, "--uninstall")
    }

    override fun update(activity: Activity) {
        launchInstaller(activity, "--update")
    }
	
	override suspend fun isInstalled(context: Context): Boolean {
        if (!isTerminalInstalled()) {
            return false
        }
        return sandboxHomeDir().child("go/bin/gopls").exists()
    }


    override suspend fun isUpdatable(context: Context): Boolean {
        val versionFile = sandboxHomeDir().child(".lsp/go/version.txt")
        val currentVersionText = runCatching { versionFile.readText().trim() }.getOrNull() ?: return false
        return currentVersionText != goLspVersion
    }

    override fun getConnectionConfig(): LspConnectionConfig {
    	return LspConnectionConfig.Process(arrayOf(
            sandboxHomeDir().child(".lsp/go/gopls").absolutePath
        ))
	}
}