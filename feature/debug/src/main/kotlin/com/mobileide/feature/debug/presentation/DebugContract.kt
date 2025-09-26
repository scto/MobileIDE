package com.mobileide.debug.presentation

import com.mobileide.debug.data.LogMessage

data class DebugState(
    val logs: List[LogMessage] = emptyList(),
    val filter: String = ""
)

sealed interface DebugEvent {
    data class FilterChanged(val filter: String) : DebugEvent
}
