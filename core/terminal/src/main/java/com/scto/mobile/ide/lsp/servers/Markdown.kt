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

object Markdown : ScriptedLspServer() {
    override val id: String = "markdown"
    override val languageName: String = "Markdown"
    override val serverName = "vscode-markdown-language-server"
    override val supportedExtensions = BuiltinFileType.MARKDOWN.extensions
    override val icon = BuiltinFileType.MARKDOWN.icon

    override val installScript = localBinDir().child("lsp/markdown")
    override val installId = "Markdown language server"

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
