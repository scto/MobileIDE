package com.scto.mobile.ide.extension.languages

import android.content.Context
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

class CppLspServer : ScriptedLspServer() {
    override val id = "cpp_lsp"
    override val languageName = "C/C++"
    override val serverName = "Clangd Language Server"
    override val supportedExtensions = listOf("c", "cpp", "h", "hpp", "cc", "cxx")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/cpp.sh")
    override val installId = "cpp_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/clangd").exists() || 
               File(distroDir, "usr/local/bin/clangd").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("clangd"))
    }
}
