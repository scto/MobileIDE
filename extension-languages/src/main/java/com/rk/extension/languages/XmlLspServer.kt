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

    override val installScript = File(localBinDir(), "install_xml_lsp.sh")
    override val installId = "xml_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        return File(localBinDir(), "lemminx/lemminx.jar").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        val jar = File(localBinDir(), "lemminx/lemminx.jar").absolutePath
        return LspConnectionConfig.Process(arrayOf("java", "-jar", jar))
    }
}
