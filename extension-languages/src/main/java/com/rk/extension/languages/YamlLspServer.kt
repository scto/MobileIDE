package com.rk.extension.languages

import android.content.Context
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer
import com.rk.lsp.localBinDir
import java.io.File

class YamlLspServer : ScriptedLspServer() {
    override val id = "yaml_lsp"
    override val languageName = "YAML"
    override val serverName = "YAML Language Server"
    override val supportedExtensions = listOf("yaml", "yml")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/yaml.sh")
    override val installId = "yaml_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/yaml-language-server").exists() || 
               File(distroDir, "usr/local/bin/yaml-language-server").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("yaml-language-server", "--stdio"))
    }
}
