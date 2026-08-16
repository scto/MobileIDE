package com.scto.mobile.ide.core.tooling.api

/**
 * LogRouter helper for deterministic command classification.
 */
object LogRouter {

    fun classify(cmdShell: String, cmdArgs: Array<String>, cmdId: String): LogChannel {
        // Priority 1: Check explicit id whitelist/patterns
        val idLower = cmdId.lowercase()
        if (idLower.contains("installation") || idLower.contains("installer") ||
            idLower.contains("typst") || idLower.contains("lsp") ||
            idLower.contains("install") || idLower.contains("update") || idLower.contains("uninstall")
        ) {
            return LogChannel.INSTALL
        }

        // Priority 2: Check executable and arguments heuristics
        val exeLower = cmdShell.lowercase()
        val argsJoined = cmdArgs.joinToString(" ").lowercase()

        if (exeLower.contains("gradle") || exeLower.contains("gradlew") ||
            argsJoined.contains("assemble") || argsJoined.contains("build") ||
            argsJoined.contains("createfinalzip") || argsJoined.contains("compile")
        ) {
            return LogChannel.BUILD
        }

        if (exeLower.contains("apt") || exeLower.contains("pkg") || exeLower.contains("apk") ||
            exeLower.contains("pip") || exeLower.contains("npm") || exeLower.contains("npx") ||
            exeLower.contains("cargo") || argsJoined.contains("install") ||
            argsJoined.contains("curl") || argsJoined.contains("setup")
        ) {
            return LogChannel.INSTALL
        }

        // Priority 3: Fallback channel
        return LogChannel.IDE_LOGS
    }
}
