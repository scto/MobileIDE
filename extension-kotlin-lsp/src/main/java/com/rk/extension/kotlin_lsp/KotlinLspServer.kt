package com.rk.extension.kotlin_lsp

import android.content.Context
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer
import com.rk.lsp.localBinDir
import java.io.File

class KotlinLspServer : ScriptedLspServer() {
    override val id = "kotlin_lsp"
    override val languageName = "Kotlin"
    override val serverName = "Kotlin Language Server"
    override val supportedExtensions = listOf("kt", "kts")
    override val icon: Any? = null
    
    // The script that will install the server when the user clicks 'Install'
    override val installScript = File(localBinDir(), "install_kotlin_lsp.sh")
    override val installId = "kotlin_lsp_installer"

    // Check if the binary exists
    override suspend fun isInstalled(context: Context): Boolean {
        return File(localBinDir(), "kotlin-language-server/bin/kotlin-language-server").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    // How the editor will start the LSP process in the background
    override fun getConnectionConfig(): LspConnectionConfig {
        val executable = File(localBinDir(), "kotlin-language-server/bin/kotlin-language-server").absolutePath
        return LspConnectionConfig.Process(arrayOf("bash", "-c", executable))
    }
}
