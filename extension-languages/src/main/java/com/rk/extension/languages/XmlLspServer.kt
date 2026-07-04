package com.rk.extension.languages

import android.content.Context
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer
import com.rk.lsp.localBinDir
import java.io.File

class XmlLspServer : ScriptedLspServer() {
    override val id = "xml_lsp"
    override val languageName = "XML"
    override val serverName = "LemMinX XML Language Server"
    override val supportedExtensions = listOf("xml")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/xml.sh")
    override val installId = "xml_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "root/.lsp/lemminx/server.jar").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("java", "-jar", "/root/.lsp/lemminx/server.jar"))
    }
}
