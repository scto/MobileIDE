package com.scto.mobile.ide.plugin.kotlin

import android.content.Context
import com.scto.mobile.ide.extension.ExtensionAPI
import com.scto.mobile.ide.extension.ExtensionContext
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.LspRegistry
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.lsp.localBinDir
import java.io.File

class KotlinLspExtension(context: ExtensionContext) : ExtensionAPI(context) {
    override fun onInstalled() {}
    override fun onExtensionLoaded() { LspRegistry.registerServer(KotlinLspServerImpl()) }
    override fun onUpdated() {}
    override fun onUninstalled() {}
}

private class KotlinLspServerImpl : ScriptedLspServer() {
    override val id = "kotlin_lsp"
    override val languageName = "Kotlin"
    override val serverName = "Kotlin Language Server"
    override val supportedExtensions = listOf("kt", "kts")
    override val icon: Any? = null
    override val installScript = File(localBinDir(), "lsp/kotlin.sh")
    override val installId = "kotlin_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/kotlin-language-server").exists() ||
               File(distroDir, "usr/local/bin/kotlin-language-server").exists() ||
               File(distroDir, "opt/kotlin-language-server/bin/kotlin-language-server").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig =
        LspConnectionConfig.Process(arrayOf("kotlin-language-server"))
}
