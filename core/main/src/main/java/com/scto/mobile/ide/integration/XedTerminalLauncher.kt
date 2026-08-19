package com.scto.mobile.ide.integration





import android.content.Context
import com.scto.mobile.ide.core.terminal.termux_exec.TermuxExec
import com.scto.mobile.ide.core.tooling.tooling_api.LogRouter











object XedTerminalLauncher {
    fun launch(
        context: Context,
        executable: String,
        args: Array<String>,
        sessionTitle: String = "LSP Installer",
        env: Map<String, String> = emptyMap()
    ) {
        val command = "$executable ${args.joinToString(" ")}"
        LogRouter.classify(command)
        TermuxExec.launchInternalTerminal(
            context = context,
            command = command,
            sessionTitle = sessionTitle
        )
    }
}
