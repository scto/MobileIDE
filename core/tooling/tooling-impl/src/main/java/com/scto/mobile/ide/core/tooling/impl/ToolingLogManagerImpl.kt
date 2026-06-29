package com.scto.mobile.ide.core.tooling.impl

import com.scto.mobile.ide.core.tooling.api.ToolingLogCategory
import com.scto.mobile.ide.core.tooling.api.ToolingLogEntry
import com.scto.mobile.ide.core.tooling.api.ToolingLogManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.CopyOnWriteArrayList

object ToolingLogManagerImpl : ToolingLogManager {
    private val _logFlow = MutableSharedFlow<ToolingLogEntry>(extraBufferCapacity = 100)
    override val logFlow: SharedFlow<ToolingLogEntry> = _logFlow.asSharedFlow()

    private val logsByCategory = HashMap<ToolingLogCategory, CopyOnWriteArrayList<ToolingLogEntry>>()

    init {
        for (category in ToolingLogCategory.values()) {
            logsByCategory[category] = CopyOnWriteArrayList()
        }
    }

    override fun log(category: ToolingLogCategory, level: String, message: String) {
        val entry = ToolingLogEntry(category, System.currentTimeMillis(), level, message)
        logsByCategory[category]?.add(entry)
        _logFlow.tryEmit(entry)
    }

    override fun getLogs(category: ToolingLogCategory): List<ToolingLogEntry> {
        return logsByCategory[category] ?: emptyList()
    }

    override fun clearLogs(category: ToolingLogCategory) {
        logsByCategory[category]?.clear()
    }

    @JvmStatic
    fun handleLogEntry(level: String, tag: String, message: String) {
        val category = when {
            tag == "Build" || tag == "ApkBuilder" -> ToolingLogCategory.BUILD
            tag.contains("LSP", ignoreCase = true) || tag.contains("LanguageServer", ignoreCase = true) -> ToolingLogCategory.LSP
            tag == "SetupWorker" || tag == "Terminal" || tag == "DistroManager" -> ToolingLogCategory.TERMINAL_ERRORS
            tag == "Diagnostics" || tag == "ProjectDiagnostics" -> ToolingLogCategory.PROJECT_DIAGNOSIS
            level == "ERROR" -> ToolingLogCategory.IDE_LOG
            else -> null
        }
        if (category != null) {
            log(category, level, message)
        }
    }
}
