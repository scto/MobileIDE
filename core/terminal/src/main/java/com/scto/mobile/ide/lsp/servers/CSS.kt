package com.scto.mobile.ide.lsp.servers

import android.content.Context
import com.scto.mobile.ide.exec.NpmUtils
import com.scto.mobile.ide.exec.isTerminalInstalled
import com.scto.mobile.ide.file.BuiltinFileType
import com.scto.mobile.ide.file.child
import com.scto.mobile.ide.file.localBinDir
import com.scto.mobile.ide.file.sandboxDir
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer

object CSS : ScriptedLspServer() {
    override val id: String = "css"
    override val languageName: String = "CSS"
    override val serverName = "vscode-css-language-server"
    override val supportedExtensions =
        BuiltinFileType.CSS.extensions + BuiltinFileType.SCSS.extensions + BuiltinFileType.LESS.extensions
    override val icon = BuiltinFileType.CSS.icon

    override val installScript = localBinDir().child("lsp/css")
    override val installId = "CSS language server"

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
