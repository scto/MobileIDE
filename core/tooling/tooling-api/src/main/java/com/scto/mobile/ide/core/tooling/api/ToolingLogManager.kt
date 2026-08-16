package com.scto.mobile.ide.core.tooling.api

import kotlinx.coroutines.flow.SharedFlow

/**
 * Standardized log channels for MobileIDE output bottom sheet.
 */
enum class LogChannel(val displayName: String) {
    INSTALL("Install"),
    BUILD("Build"),
    LSP("LSP"),
    DIAGNOSE("Diagnose"),
    IDE_LOGS("IDE Logs");

    companion object {
        fun fromCategory(category: ToolingLogCategory): LogChannel {
            return when (category) {
                ToolingLogCategory.INSTALL -> INSTALL
                ToolingLogCategory.BUILD -> BUILD
                ToolingLogCategory.LSP -> LSP
                ToolingLogCategory.DIAGNOSE, ToolingLogCategory.PROJECT_DIAGNOSIS -> DIAGNOSE
                ToolingLogCategory.IDE_LOGS, ToolingLogCategory.IDE_LOG, ToolingLogCategory.TERMINAL_ERRORS -> IDE_LOGS
                else -> IDE_LOGS
            }
        }
    }
}

enum class ToolingLogCategory(val displayName: String) {
    INSTALL("Install"),
    BUILD("Build"),
    LSP("LSP"),
    DIAGNOSE("Diagnose"),
    IDE_LOGS("IDE Logs"),
    AI("AI"),
    DEBUG("Debug"),
    DOCS("Docs"),
    PROJECT_DIAGNOSIS("Diagnosis"),
    IDE_LOG("IDE Log"),
    TERMINAL_ERRORS("Terminal Logs")
}

data class ToolingLogEntry(
    val category: ToolingLogCategory,
    val timestamp: Long,
    val level: String,
    val message: String,
    val channel: LogChannel = LogChannel.fromCategory(category)
)

interface ToolingLogManager {
    val logFlow: SharedFlow<ToolingLogEntry>
    fun log(category: ToolingLogCategory, level: String, message: String)
    fun log(channel: LogChannel, level: String, message: String) {
        val cat = when (channel) {
            LogChannel.INSTALL -> ToolingLogCategory.INSTALL
            LogChannel.BUILD -> ToolingLogCategory.BUILD
            LogChannel.LSP -> ToolingLogCategory.LSP
            LogChannel.DIAGNOSE -> ToolingLogCategory.DIAGNOSE
            LogChannel.IDE_LOGS -> ToolingLogCategory.IDE_LOGS
        }
        log(cat, level, message)
    }
    fun getLogs(category: ToolingLogCategory): List<ToolingLogEntry>
    fun clearLogs(category: ToolingLogCategory)
}
