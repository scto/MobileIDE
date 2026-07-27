package com.scto.mobile.ide.core.tooling.impl.debugger

data class Breakpoint(
    val filePath: String,
    val lineNumber: Int,
    val className: String? = null,
    val isEnabled: Boolean = true
) {
    val id: String
        get() = "$filePath:$lineNumber"
}

data class StackFrameInfo(
    val id: String,
    val methodName: String,
    val className: String,
    val fileName: String,
    val lineNumber: Int
)

data class VariableInfo(
    val name: String,
    val value: String,
    val type: String = "Object",
    val children: List<VariableInfo> = emptyList(),
    val isExpanded: Boolean = false
)

enum class DebugSessionStatus(val displayName: String) {
    IDLE("Inaktiv"),
    CONNECTING("Verbinde mit JDWP..."),
    RUNNING("App läuft..."),
    PAUSED("Pausiert an Breakpoint"),
    STOPPED("Debug-Session beendet")
}
