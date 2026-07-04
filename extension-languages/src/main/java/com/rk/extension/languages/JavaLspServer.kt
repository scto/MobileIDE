package com.rk.extension.languages

import android.content.Context
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer
import com.rk.lsp.localBinDir
import java.io.File

class JavaLspServer : ScriptedLspServer() {
    override val id = "java_lsp"
    override val languageName = "Java"
    override val serverName = "Eclipse JDT Language Server"
    override val supportedExtensions = listOf("java")
    override val icon: Any? = null

    override val installScript = File(localBinDir(), "lsp/java.sh")
    override val installId = "java_lsp_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        return File("/data/data/com.termux/files/usr/bin/jdtls").exists() || File(localBinDir(), "jdtls/bin/jdtls").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false

    override fun getConnectionConfig(): LspConnectionConfig {
        val jdtls = if (File("/data/data/com.termux/files/usr/bin/jdtls").exists()) {
            "/data/data/com.termux/files/usr/bin/jdtls"
        } else {
            File(localBinDir(), "jdtls/bin/jdtls").absolutePath
        }
        return LspConnectionConfig.Process(arrayOf("bash", "-c", jdtls))
    }
}
