package com.scto.mobile.ide.core.tooling.api

import kotlinx.coroutines.flow.SharedFlow

enum class ToolingLogCategory {
    TERMINAL_ERRORS,
    PROJECT_DIAGNOSIS,
    IDE_LOG,
    BUILD,
    LSP
}

data class ToolingLogEntry(
    val category: ToolingLogCategory,
    val timestamp: Long,
    val level: String,
    val message: String
)

interface ToolingLogManager {
    val logFlow: SharedFlow<ToolingLogEntry>
    fun log(category: ToolingLogCategory, level: String, message: String)
    fun getLogs(category: ToolingLogCategory): List<ToolingLogEntry>
    fun clearLogs(category: ToolingLogCategory)
}
