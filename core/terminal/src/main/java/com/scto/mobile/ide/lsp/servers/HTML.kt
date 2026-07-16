package com.scto.mobile.ide.lsp.servers

import android.content.Context
import com.scto.mobile.ide.exec.NpmUtils
import com.scto.mobile.ide.exec.isTerminalInstalled
import com.scto.mobile.ide.core.common.files.BuiltinFileType
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.localBinDir
import com.scto.mobile.ide.core.common.files.sandboxDir
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer

object HTML : ScriptedLspServer() {
    override val id: String = "html"
    override val languageName: String = "HTML"
    override val serverName = "vscode-html-language-server"
    override val supportedExtensions = BuiltinFileType.HTML.extensions + BuiltinFileType.HTMX.extensions
    override val icon = BuiltinFileType.HTML.icon

    override val installScript = localBinDir().child("lsp/html")
    override val installId = "HTML language server"

    override suspend fun isInstalled(context: Context): Boolean {
        if (!isTerminalInstalled()) {
            return false
        }

        return sandboxDir().child("/usr/bin/$serverName").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean {
        return NpmUtils.hasUpdate("vscode-langservers-extracted")
    }

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("/usr/bin/node", "/usr/bin/$serverName", "--stdio"))
    }
}
