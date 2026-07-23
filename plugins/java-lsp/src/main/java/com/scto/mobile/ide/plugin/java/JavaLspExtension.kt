package com.scto.mobile.ide.plugin.java

import android.content.Context
import com.scto.mobile.ide.extension.ExtensionAPI
import com.scto.mobile.ide.extension.ExtensionContext
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.LspRegistry
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

class JavaLspExtension(context: ExtensionContext) : ExtensionAPI(context) {
    override fun onInstalled() {}
    override fun onExtensionLoaded() { LspRegistry.registerServer(JavaLspServerImpl()) }
    override fun onUpdated() {}
    override fun onUninstalled() {}
}

private class JavaLspServerImpl : ScriptedLspServer() {
    override val id = "java_lsp"
    override val languageName = "Java"
    override val serverName = "Eclipse JDT Language Server"
    override val supportedExtensions = listOf("java")
    override val icon: Any? = null
    override val installScript = File(localBinDir(), "lsp/java.sh")
    override val installId = "java_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/jdtls").exists() ||
               File(distroDir, "usr/local/bin/jdtls").exists() ||
               File(distroDir, "opt/jdtls/bin/jdtls").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig =
        LspConnectionConfig.Process(arrayOf("jdtls"))
}
