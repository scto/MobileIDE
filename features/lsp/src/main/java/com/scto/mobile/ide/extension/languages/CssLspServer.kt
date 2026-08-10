package com.scto.mobile.ide.features.extensions.languages

import android.content.Context
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

class CssLspServer : ScriptedLspServer() {
    override val id = "css_lsp"
    override val languageName = "CSS"
    override val serverName = "vscode-css-language-server"
    override val supportedExtensions = listOf("css", "scss", "less")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/css.sh")
    override val installId = "css_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/$serverName").exists() ||
               File(distroDir, "usr/local/bin/$serverName").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("node", "/usr/bin/$serverName", "--stdio"))
    }
}
