package com.rk.extension.languages

import android.content.Context
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer
import com.rk.lsp.localBinDir
import java.io.File

class TomlLspServer : ScriptedLspServer() {
    override val id = "toml_lsp"
    override val languageName = "TOML"
    override val serverName = "Taplo TOML Language Server"
    override val supportedExtensions = listOf("toml")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/toml.sh")
    override val installId = "toml_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/taplo").exists() || 
               File(distroDir, "usr/local/bin/taplo").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("taplo", "lsp", "run"))
    }
}
