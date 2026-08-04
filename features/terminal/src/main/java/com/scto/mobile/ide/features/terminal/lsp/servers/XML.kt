package com.scto.mobile.ide.features.terminal.lsp.servers

import android.content.Context
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

object XML : ScriptedLspServer() {
    override val id = "xml"
    override val languageName = "XML"
    override val serverName = "lemminx"
    override val supportedExtensions = listOf("xml")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/xml.sh")
    override val installId = "XML language server"

    const val LATEST_VERSION = "0.31.0"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "home/.lsp/lemminx/server.jar").exists() ||
               File(distroDir, "usr/bin/lemminx").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("java", "-jar", "/home/.lsp/lemminx/server.jar"))
    }
}
