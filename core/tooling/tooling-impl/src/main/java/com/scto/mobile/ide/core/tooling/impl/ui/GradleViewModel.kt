package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scto.mobile.ide.core.tooling.api.GradleLogEvent
import com.scto.mobile.ide.core.tooling.api.IpcProtocol
import com.scto.mobile.ide.core.tooling.api.Resource
import com.scto.mobile.ide.core.tooling.impl.client.SocketToolingClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Display model for a single line in the terminal-style log output.
 */
data class LogDisplayItem(
    val id: Long = System.nanoTime(),
    val timestamp: Long,
    val level: String,          // "INFO", "WARN", "ERROR", "PROGRESS", "RESULT"
    val text: String,
    val isProgress: Boolean = false,
    val isResult: Boolean = false,
    val isSuccess: Boolean? = null,
)

/**
 * ViewModel for the Gradle tooling terminal UI.
 *
 * Exposes:
 *  - [availableTasks]  – list of task paths fetched from the server
 *  - [logLines]        – live stream of [LogDisplayItem]s (build output)
 *  - [isBuilding]      – true while a build is in progress
 *  - [tasksFetchState] – loading state for the task list
 */
class GradleViewModel : ViewModel() {

    private val client = SocketToolingClient()

    // ── Task list ─────────────────────────────────────────────────────────────

    private val _availableTasks = MutableStateFlow<List<String>>(emptyList())
    val availableTasks: StateFlow<List<String>> = _availableTasks.asStateFlow()

    private val _tasksFetchState = MutableStateFlow<Resource<List<String>>>(Resource.Loading)
    val tasksFetchState: StateFlow<Resource<List<String>>> = _tasksFetchState.asStateFlow()

    // ── Build log ─────────────────────────────────────────────────────────────

    private val _logLines = MutableStateFlow<List<LogDisplayItem>>(emptyList())
    val logLines: StateFlow<List<LogDisplayItem>> = _logLines.asStateFlow()

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()

    private var buildJob: Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches the list of available Gradle tasks for the project at [projectPath].
     */
    fun fetchTasks(projectPath: String) {
        viewModelScope.launch {
            client.fetchAvailableTasks(projectPath).collect { resource ->
                _tasksFetchState.value = resource
                if (resource is Resource.Success) {
                    _availableTasks.value = resource.data
                }
            }
        }
    }

    /**
     * Starts a Gradle build with the given [selectedTasks] in [projectPath].
     * Streams output into [logLines] and sets [isBuilding] accordingly.
     */
    fun onPlayClicked(projectPath: String, selectedTasks: List<String>) {
        if (_isBuilding.value) return
        buildJob?.cancel()

        // Reset log for new build
        _logLines.value = emptyList()
        _isBuilding.value = true

        // Header entry
        appendLog(
            LogDisplayItem(
                timestamp = System.currentTimeMillis(),
                level = IpcProtocol.LEVEL_INFO,
                text = "> Running: ${selectedTasks.joinToString(" ")}",
            )
        )

        buildJob = viewModelScope.launch {
            client.executeTasks(projectPath, selectedTasks).collect { event ->
                val item = event.toDisplayItem()
                appendLog(item)

                if (event is GradleLogEvent.Result) {
                    _isBuilding.value = false
                }
            }
        }
    }

    /**
     * Cancels a running build.
     */
    fun cancelBuild() {
        buildJob?.cancel()
        buildJob = null
        _isBuilding.value = false
        appendLog(
            LogDisplayItem(
                timestamp = System.currentTimeMillis(),
                level = IpcProtocol.LEVEL_WARN,
                text = "Build cancelled by user.",
            )
        )
    }

    /**
     * Clears the current log output.
     */
    fun clearLog() {
        _logLines.value = emptyList()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun appendLog(item: LogDisplayItem) {
        _logLines.value = _logLines.value + item
    }

    private fun GradleLogEvent.toDisplayItem(): LogDisplayItem = when (this) {
        is GradleLogEvent.Progress -> LogDisplayItem(
            timestamp = timestamp,
            level = "PROGRESS",
            text = message,
            isProgress = true,
        )
        is GradleLogEvent.Output -> LogDisplayItem(
            timestamp = timestamp,
            level = logLevel,
            text = text,
        )
        is GradleLogEvent.Result -> LogDisplayItem(
            timestamp = System.currentTimeMillis(),
            level = if (isSuccess) "SUCCESS" else IpcProtocol.LEVEL_ERROR,
            text = message,
            isResult = true,
            isSuccess = isSuccess,
        )
    }
}
