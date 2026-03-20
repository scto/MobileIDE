package com.mobileide.app.logger

/**
 * Kotlin port of jkas.androidpe.logger.LoggerRes  (author: JKas)
 *
 * Resource/theme event bus — separate from the main [Logger].
 * Signals: [reloadResRef] (theme changed) and [onSaveRequested] (save triggered).
 */
object LoggerRes {

    // ── Listener list — identical to Java ────────────────────────────────────
    private val logListeners = mutableListOf<LogListener>()

    // ── initFromZero — identical to Java ─────────────────────────────────────
    fun initFromZero() = logListeners.clear()

    // ── Listener management — identical to Java ───────────────────────────────
    fun addLogListener(listener: LogListener) = logListeners.add(listener)
    fun removeLogListener(listener: LogListener) = logListeners.remove(listener)

    // ── Dispatch — identical to Java ─────────────────────────────────────────
    fun onSaveRequested() = logListeners.forEach { it.onSaveRequested() }
    fun reloadResRef()    = logListeners.forEach { it.reloadResRef() }

    // ── Interface — identical to Java ─────────────────────────────────────────
    interface LogListener {
        fun reloadResRef()
        fun onSaveRequested() { /* default no-op — matches Java default method */ }
    }
}
