package com.scto.mobile.ide

import android.app.Application
import android.content.Intent
import com.scto.mobile.ide.lsp.LspRegistry
import com.scto.mobile.ide.activities.terminal.Terminal
import com.scto.mobile.ide.exec.pendingCommand
import com.scto.mobile.ide.feature.Feature
import com.scto.mobile.ide.lsp.servers.Bash
import com.scto.mobile.ide.lsp.servers.CSS
import com.scto.mobile.ide.lsp.servers.ESLint
import com.scto.mobile.ide.lsp.servers.Emmet
import com.scto.mobile.ide.lsp.servers.HTML
import com.scto.mobile.ide.lsp.servers.Markdown
import com.scto.mobile.ide.lsp.servers.TypeScript
import com.scto.mobile.ide.lsp.servers.XML
import com.scto.mobile.ide.runner.RunnerManager
import com.scto.mobile.ide.runner.runners.UniversalRunner
import com.scto.mobile.ide.core.common.utils.toast

class TerminalFeature : Feature {
    override fun init(application: Application) {
        // Register UniversalRunner dynamically
        RunnerManager.registerRunner(UniversalRunner)

        // Register TerminalLauncher handler
        TerminalLauncher.handler = { activity, sandbox, exe, args, id, terminatePreviousSession, workingDir, env ->
            pendingCommand =
                com.scto.mobile.ide.exec.TerminalCommand(
                    sandbox = sandbox,
                    exe = exe,
                    args = args,
                    id = id,
                    terminatePreviousSession = terminatePreviousSession,
                    workingDir = workingDir,
                    env = env ?: emptyArray(),
                )
            try {
                val intent = Intent(activity, Terminal::class.java)
                activity.startActivity(intent)
            } catch (e: Exception) {
                toast("Terminal feature is not available in this build")
            }
        }

        // Register built-in LSP servers
        LspRegistry.registerServer(Bash)
        LspRegistry.registerServer(CSS)
        LspRegistry.registerServer(ESLint)
        LspRegistry.registerServer(Emmet)
        LspRegistry.registerServer(HTML)
        LspRegistry.registerServer(Markdown)
        LspRegistry.registerServer(TypeScript)
        LspRegistry.registerServer(XML)
    }
}
