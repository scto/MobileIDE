package com.scto.mobile.ide.features.extensions.languages

import android.content.Context
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

class EmmetLspServer : ScriptedLspServer() {
    override val id = "emmet_lsp"
    override val languageName = "Emmet"
    override val serverName = "emmet-language-server"
    override val supportedExtensions = listOf("html", "htm", "htmx")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/emmet.sh")
    override val installId = "emmet_lsp_installer"

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
