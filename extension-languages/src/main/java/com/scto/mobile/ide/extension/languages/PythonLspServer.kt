package com.scto.mobile.ide.extension.languages

import android.content.Context
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

class PythonLspServer : ScriptedLspServer() {
    override val id = "python_lsp"
    override val languageName = "Python"
    override val serverName = "Pyright Python Language Server"
    override val supportedExtensions = listOf("py")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/python.sh")
    override val installId = "python_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/pyright-langserver").exists() || 
               File(distroDir, "usr/local/bin/pyright-langserver").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("pyright-langserver", "--stdio"))
    }
}
