package com.mobileide.debug.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class LogMessage(val priority: Int, val tag: String, val message: String)

@Singleton
class LogRepository @Inject constructor() {
    private val _logs = MutableStateFlow<List<LogMessage>>(emptyList())
    private val logs: MutableList<LogMessage> = mutableListOf()

    fun getLogs(): Flow<List[LogMessage]> = _logs.asStateFlow()

    fun addLog(priority: Int, tag: String?, message: String) {
        val logMessage = LogMessage(priority, tag ?: "Default", message)
        logs.add(0, logMessage) // Add to the top
        if (logs.size > 500) { // Limit log size
            logs.removeLast()
        }
        _logs.value = logs.toList()
    }
}
