package com.mobileide.terminal.presentation

data class TerminalState(
    val outputLines: List<String> = emptyList(),
    val workingDir: String = "/storage/emulated/0/",
    val isExecuting: Boolean = false
)

sealed interface TerminalEvent {
    data class ExecuteCommand(val command: String) : TerminalEvent
}
