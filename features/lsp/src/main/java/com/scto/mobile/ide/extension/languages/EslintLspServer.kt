package com.scto.mobile.ide.features.extensions.languages

import android.content.Context
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

class EslintLspServer : ScriptedLspServer() {
    override val id = "eslint_lsp"
    override val languageName = "ESLint"
    override val serverName = "vscode-eslint-language-server"
    override val supportedExtensions = listOf("js", "jsx", "ts", "tsx")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/eslint.sh")
    override val installId = "eslint_lsp_installer"

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
