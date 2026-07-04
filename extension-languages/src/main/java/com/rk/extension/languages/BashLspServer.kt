package com.rk.extension.languages

import android.content.Context
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer
import com.rk.lsp.localBinDir
import java.io.File

class BashLspServer : ScriptedLspServer() {
    override val id = "bash_lsp"
    override val languageName = "Bash"
    override val serverName = "Bash Language Server"
    override val supportedExtensions = listOf("sh", "bash")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/bash.sh")
    override val installId = "bash_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/bash-language-server").exists() || 
               File(distroDir, "usr/local/bin/bash-language-server").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("bash-language-server", "start"))
    }
}
