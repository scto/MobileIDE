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

object Bash : ScriptedLspServer() {
    override val id: String = "bash"
    override val languageName: String = "Bash"
    override val serverName = "bash-language-server"
    override val supportedExtensions = BuiltinFileType.SHELL.extensions
    override val icon = BuiltinFileType.SHELL.icon

    override val installScript = localBinDir().child("lsp/bash")
    override val installId = "Bash language server"

    override suspend fun isInstalled(context: Context): Boolean {
        if (!isTerminalInstalled()) {
            return false
        }

        return sandboxDir().child("/usr/bin/$serverName").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean {
        return NpmUtils.hasUpdate(serverName)
    }

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("/usr/bin/node", "/usr/bin/$serverName", "start"))
    }
}
