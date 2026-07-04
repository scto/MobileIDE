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

    override val installScript = File(localBinDir(), "install_bash_lsp.sh")
    override val installId = "bash_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        return File("/data/data/com.termux/files/usr/bin/bash-language-server").exists() || 
               File("/data/data/com.termux/files/usr/local/bin/bash-language-server").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        val bin = if (File("/data/data/com.termux/files/usr/bin/bash-language-server").exists()) {
            "/data/data/com.termux/files/usr/bin/bash-language-server"
        } else {
            "bash-language-server"
        }
        return LspConnectionConfig.Process(arrayOf("bash", "-c", "$bin start"))
    }
}
