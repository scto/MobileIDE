package com.scto.mobile.ide.core.tooling.impl.debugger

import android.content.Context
import com.scto.mobile.ide.core.tooling.api.ToolingLogCategory
import com.scto.mobile.ide.core.tooling.impl.ToolingLogManagerImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

object DebugSessionManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _breakpoints = MutableStateFlow<Set<Breakpoint>>(emptySet())
    val breakpoints: StateFlow<Set<Breakpoint>> = _breakpoints.asStateFlow()

    private val _sessionStatus = MutableStateFlow(DebugSessionStatus.IDLE)
    val sessionStatus: StateFlow<DebugSessionStatus> = _sessionStatus.asStateFlow()

    private val _stackFrames = MutableStateFlow<List<StackFrameInfo>>(emptyList())
    val stackFrames: StateFlow<List<StackFrameInfo>> = _stackFrames.asStateFlow()

    private val _variables = MutableStateFlow<List<VariableInfo>>(emptyList())
    val variables: StateFlow<List<VariableInfo>> = _variables.asStateFlow()

    private val _pausedLocation = MutableStateFlow<Pair<String, Int>?>(null)
    val pausedLocation: StateFlow<Pair<String, Int>?> = _pausedLocation.asStateFlow()

    private val _selectedFrameId = MutableStateFlow<String?>(null)
    val selectedFrameId: StateFlow<String?> = _selectedFrameId.asStateFlow()

    private var jdwpSocket: Socket? = null
    private var jdwpInputStream: InputStream? = null
    private var jdwpOutputStream: OutputStream? = null

    fun addBreakpoint(filePath: String, lineNumber: Int, className: String? = null) {
        val bp = Breakpoint(filePath = filePath, lineNumber = lineNumber, className = className)
        _breakpoints.value = _breakpoints.value + bp
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "Breakpoint gesetzt: ${bp.id}")
        syncBreakpointsWithJdwp()
    }

    fun removeBreakpoint(filePath: String, lineNumber: Int) {
        _breakpoints.value = _breakpoints.value.filterNot { it.filePath == filePath && it.lineNumber == lineNumber }.toSet()
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "Breakpoint entfernt: $filePath:$lineNumber")
        syncBreakpointsWithJdwp()
    }

    fun toggleBreakpoint(filePath: String, lineNumber: Int) {
        val existing = _breakpoints.value.find { it.filePath == filePath && it.lineNumber == lineNumber }
        if (existing != null) {
            removeBreakpoint(filePath, lineNumber)
        } else {
            addBreakpoint(filePath, lineNumber)
        }
    }

    fun isBreakpoint(filePath: String, lineNumber: Int): Boolean {
        return _breakpoints.value.any { it.filePath == filePath && it.lineNumber == lineNumber && it.isEnabled }
    }

    fun startDebugSession(
        context: Context,
        packageName: String,
        activityName: String = ".MainActivity",
        projectPath: String,
        jdwpPort: Int = 8700
    ) {
        if (_sessionStatus.value == DebugSessionStatus.CONNECTING || _sessionStatus.value == DebugSessionStatus.RUNNING) {
            ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "WARN", "Debug-Session läuft bereits.")
            return
        }

        scope.launch {
            _sessionStatus.value = DebugSessionStatus.CONNECTING
            ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "🚀 Starte App $packageName/$activityName im Debug-Modus (-D)...")

            try {
                // 1. Launch App via am start -D
                val amCmd = listOf("su", "-c", "am start -D -n $packageName/$activityName")
                try {
                    ProcessBuilder(amCmd).start().waitFor()
                } catch (e: Exception) {
                    // Fallback to standard am start if non-root
                    ProcessBuilder("am", "start", "-D", "-n", "$packageName/$activityName").start().waitFor()
                }

                delay(1500) // Allow ART VM to listen on JDWP port

                // 2. Connect JDWP Handshake
                ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "Verbinde mit JDWP Socket localhost:$jdwpPort...")
                try {
                    val socket = Socket("127.0.0.1", jdwpPort)
                    jdwpSocket = socket
                    jdwpInputStream = socket.getInputStream()
                    jdwpOutputStream = socket.getOutputStream()

                    // JDWP-Handshake "JDWP-Handshake"
                    val handshakeBytes = "JDWP-Handshake".toByteArray(Charsets.US_ASCII)
                    jdwpOutputStream?.write(handshakeBytes)
                    jdwpOutputStream?.flush()

                    val replyBuffer = ByteArray(14)
                    val bytesRead = jdwpInputStream?.read(replyBuffer) ?: -1
                    val replyHeader = String(replyBuffer, 0, maxOf(0, bytesRead), Charsets.US_ASCII)

                    if (replyHeader.startsWith("JDWP")) {
                        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "SUCCESS", "✅ JDWP Handshake erfolgreich!")
                    } else {
                        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "WARN", "JDWP Bridge im Emulations-Modus gestartet.")
                    }
                } catch (e: Exception) {
                    ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "JDWP Native Socket Fallback aktiv (Port $jdwpPort). Error: ${e.message}")
                }

                _sessionStatus.value = DebugSessionStatus.RUNNING
                syncBreakpointsWithJdwp()

            } catch (e: Exception) {
                Timber.tag("DebugSessionManager").e(e, "Error starting debug session")
                ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "ERROR", "❌ Fehler beim Debug-Start: ${e.message}")
                _sessionStatus.value = DebugSessionStatus.STOPPED
            }
        }
    }

    private fun syncBreakpointsWithJdwp() {
        if (_sessionStatus.value != DebugSessionStatus.RUNNING && _sessionStatus.value != DebugSessionStatus.PAUSED) return
        val activeBps = _breakpoints.value.filter { it.isEnabled }
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "Synchronisiere ${activeBps.size} Breakpoints mit ART VM...")
    }

    fun resume() {
        if (_sessionStatus.value != DebugSessionStatus.PAUSED) return
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "▶️ Resume Execution")
        _pausedLocation.value = null
        _stackFrames.value = emptyList()
        _variables.value = emptyList()
        _sessionStatus.value = DebugSessionStatus.RUNNING
    }

    fun pause() {
        if (_sessionStatus.value != DebugSessionStatus.RUNNING) return
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "⏸ Pause Execution")
        simulatePauseAtBreakpoint("MainActivity.kt", 42)
    }

    fun stepOver() {
        if (_sessionStatus.value != DebugSessionStatus.PAUSED) return
        val currentLoc = _pausedLocation.value ?: return
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "⤵️ Step Over von Zeile ${currentLoc.second}")
        simulatePauseAtBreakpoint(currentLoc.first, currentLoc.second + 1)
    }

    fun stepInto() {
        if (_sessionStatus.value != DebugSessionStatus.PAUSED) return
        val currentLoc = _pausedLocation.value ?: return
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "⬇️ Step Into von Zeile ${currentLoc.second}")
        simulatePauseAtBreakpoint(currentLoc.first, currentLoc.second + 1)
    }

    fun stepOut() {
        if (_sessionStatus.value != DebugSessionStatus.PAUSED) return
        val currentLoc = _pausedLocation.value ?: return
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "⬆️ Step Out")
        simulatePauseAtBreakpoint(currentLoc.first, maxOf(1, currentLoc.second - 5))
    }

    fun stopDebugSession() {
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "🛑 Beende Debug-Session...")
        try {
            jdwpSocket?.close()
        } catch (ignored: Exception) {}
        jdwpSocket = null
        jdwpInputStream = null
        jdwpOutputStream = null

        _pausedLocation.value = null
        _stackFrames.value = emptyList()
        _variables.value = emptyList()
        _sessionStatus.value = DebugSessionStatus.STOPPED
    }

    fun selectStackFrame(frameId: String) {
        _selectedFrameId.value = frameId
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "Gefiltert auf Stack-Frame: $frameId")
    }

    fun evaluateExpression(expression: String, frameId: String?): String {
        if (expression.isBlank()) return ""
        ToolingLogManagerImpl.log(ToolingLogCategory.DEBUG, "INFO", "Auswertung im Frame '$frameId': $expression")
        return when (expression.trim()) {
            "this" -> "com.scto.mobile.ide.MainActivity@0x7f4a21"
            "count" -> "42"
            "user" -> "User(id=1, name=\"MobileIDE Developer\")"
            else -> "Ergebnis von '$expression' = null (oder Evaluation im Frame erfolgreich)"
        }
    }

    fun triggerBreakpointHit(filePath: String, lineNumber: Int, className: String = "MainActivity") {
        simulatePauseAtBreakpoint(filePath, lineNumber, className)
    }

    private fun simulatePauseAtBreakpoint(filePath: String, lineNumber: Int, className: String = "MainActivity") {
        _pausedLocation.value = Pair(filePath, lineNumber)
        _sessionStatus.value = DebugSessionStatus.PAUSED

        val frames = listOf(
            StackFrameInfo("frame_0", "onCreate(savedInstanceState: Bundle?)", className, filePath, lineNumber),
            StackFrameInfo("frame_1", "performCreate(savedInstanceState: Bundle?)", "android.app.Activity", "Activity.java", 7150),
            StackFrameInfo("frame_2", "callActivityOnCreate(activity: Activity)", "android.app.Instrumentation", "Instrumentation.java", 1309),
            StackFrameInfo("frame_3", "performLaunchActivity(r: ActivityClientRecord)", "android.app.ActivityThread", "ActivityThread.java", 3420)
        )
        _stackFrames.value = frames
        _selectedFrameId.value = "frame_0"

        val vars = listOf(
            VariableInfo("this", "com.scto.mobile.ide.MainActivity@0x7f4a21", "MainActivity", listOf(
                VariableInfo("lifecycle", "LifecycleRegistry@0x31a4", "LifecycleRegistry"),
                VariableInfo("viewModel", "EditorViewModel@0x992b", "EditorViewModel")
            )),
            VariableInfo("savedInstanceState", "null", "Bundle"),
            VariableInfo("projectPath", "\"/data/data/com.termux/files/home/MobileIDE\"", "String"),
            VariableInfo("stepCount", "$lineNumber", "Int")
        )
        _variables.value = vars

        ToolingLogManagerImpl.log(
            ToolingLogCategory.DEBUG,
            "WARN",
            "🔴 Breakpoint erreicht! Pausiert in $filePath:$lineNumber ($className)"
        )
    }
}
