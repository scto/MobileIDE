package com.scto.mobile.ide.extension.languages

import android.content.Context
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

class JsonLspServer : ScriptedLspServer() {
    override val id = "json_lsp"
    override val languageName = "JSON"
    override val serverName = "VSCode JSON Language Server"
    override val supportedExtensions = listOf("json")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/json.sh")
    override val installId = "json_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/vscode-json-language-server").exists() || 
               File(distroDir, "usr/local/bin/vscode-json-language-server").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("vscode-json-language-server", "--stdio"))
    }
}
