package com.mobileide.app.logger

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Kotlin port of jkas.androidpe.logger.Logger  (author: JKas)
 *
 * Public API is 1:1 with the Java original:
 *   Logger.initFromZero()
 *   Logger.addLogListener { msg -> }
 *   Logger.removeLogListener(listener)
 *   Logger.info("tag", "msg")
 *   Logger.warn("tag", "msg")
 *   Logger.error("tag", "msg")
 *   Logger.success("tag", "msg")
 *
 * Kotlin additions (not in Java):
 *   Logger.enabled        – master switch (default false)
 *   Logger.entries        – StateFlow<List<LogMsg>> for Compose
 *   Logger.maxEntries     – ring-buffer cap
 *   Logger.clear()        – flush in-memory buffer
 *   Logger.exportText()   – dump to String
 *   Logger.summary        – counter summary string
 *   Logger.error(src, msg, throwable) – attach stack trace
 */
object Logger {

    // ── Counters — identical to Java ──────────────────────────────────────────
    @JvmField var warn    = 0
    @JvmField var info    = 0
    @JvmField var error   = 0
    @JvmField var success = 0

    // ── Listener list — identical to Java ────────────────────────────────────
    private val logListeners = mutableListOf<LogListener>()

    // ── Kotlin additions ──────────────────────────────────────────────────────
    /** Master switch. When false, log calls are forwarded to Logcat but not recorded. */
    @Volatile var enabled: Boolean = false

    /** Ring-buffer capacity for [entries]. */
    var maxEntries: Int = 2_000

    private val _entries = MutableStateFlow<List<LogMsg>>(emptyList())

    /**
     * Live stream of all captured [LogMsg] entries.
     * Collect this in any @Composable via `collectAsState()`.
     */
    val entries: StateFlow<List<LogMsg>> = _entries.asStateFlow()

    // ── initFromZero — identical to Java ─────────────────────────────────────
    fun initFromZero() {
        logListeners.clear()
        warn    = 0
        info    = 0
        error   = 0
        success = 0
        _entries.value = emptyList()
    }

    // ── Listener management — identical to Java ───────────────────────────────
    fun addLogListener(listener: LogListener) {
        if (!logListeners.contains(listener)) logListeners.add(listener)
    }

    fun removeLogListener(listener: LogListener) {
        logListeners.remove(listener)
    }

    // ── Internal dispatch — identical logic to Java ───────────────────────────
    private fun log(src: String, level: Level, vararg msg: String) {
        // Always bridge to Android Logcat
        bridgeLogcat(src, level, msg.joinToString("\n"))

        if (!enabled) return

        val logMsg = LogMsg(src, level.name, *msg)

        // Ring-buffered StateFlow
        _entries.update { current ->
            val next = current + logMsg
            if (next.size > maxEntries) next.drop(next.size - maxEntries) else next
        }

        // Dispatch — identical to Java
        for (listener in logListeners) listener.log(logMsg)
    }

    // ── Public API — identical to Java ───────────────────────────────────────
    @JvmStatic fun warn(src: String, vararg messages: String)    { warn++;    log(src, Level.WARNING, *messages) }
    @JvmStatic fun info(src: String, vararg messages: String)    { info++;    log(src, Level.INFO,    *messages) }
    @JvmStatic fun error(src: String, vararg messages: String)   { error++;   log(src, Level.ERROR,   *messages) }
    @JvmStatic fun success(src: String, vararg messages: String) { success++; log(src, Level.SUCCESS, *messages) }

    // ── Kotlin extras ─────────────────────────────────────────────────────────
    fun error(src: String, message: String, throwable: Throwable) {
        error++
        val lines = mutableListOf(message, throwable.toString())
        throwable.stackTrace.take(6).forEach { lines += "    at $it" }
        log(src, Level.ERROR, *lines.toTypedArray())
    }

    fun clear()              { _entries.value = emptyList() }
    val hasErrors: Boolean   get() = error > 0
    val summary: String      get() = "W:$warn  I:$info  E:$error  S:$success"

    fun exportText() = buildString {
        appendLine("=== MobileIDE Debug Log ===")
        appendLine("warn=$warn  info=$info  error=$error  success=$success")
        appendLine("buffer: ${_entries.value.size} entries  generated: ${java.util.Date()}")
        appendLine("=".repeat(60))
        _entries.value.forEach { appendLine(it.logcatLine) }
    }

    // ── Android bridge ────────────────────────────────────────────────────────
    private fun bridgeLogcat(src: String, level: Level, msg: String) = when (level) {
        Level.WARNING -> Log.w(src, msg)
        Level.ERROR   -> Log.e(src, msg)
        Level.INFO    -> Log.i(src, msg)
        Level.SUCCESS -> Log.d(src, "✓ $msg")
    }

    // ── Level enum — identical to Java ───────────────────────────────────────
    enum class Level { WARNING, ERROR, INFO, SUCCESS }

    // ── LogListener SAM — identical to Java ──────────────────────────────────
    fun interface LogListener {
        fun log(logMsg: LogMsg)
    }
}
