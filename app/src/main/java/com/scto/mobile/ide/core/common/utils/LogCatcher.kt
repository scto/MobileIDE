package com.scto.mobile.ide.core.common.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal stub implementation of LogCatcher used throughout the project.
 * The real implementation provides richer logging and log collection; this stub
 * satisfies compilation and retains basic logging functionality.
 */
object LogCatcher {
    // Simple in‑memory log store for build logs
    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val logFlow = _logFlow.asSharedFlow()

    private val loggingEnabled = AtomicBoolean(true)

    // ----- Configuration -----
    fun updateConfig(any: Any) {
        // No‑op stub – the real implementation would adjust log settings.
    }

    // ----- Logging helpers -----
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log("INFO", tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log("DEBUG", tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log("WARN", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        val formatted = "[$level] $tag: $message"
        if (loggingEnabled.get()) {
            println(formatted)
            _logFlow.tryEmit(formatted)
            throwable?.printStackTrace()
        }
    }

    // ----- Build‑log helpers -----
    val isLoggingEnabled: Boolean
        get() = loggingEnabled.get()

    fun getBuildLogs(): List<String> = _logFlow.replayCache.toList()

    fun clearBuildLogs() {
        // MutableSharedFlow does not support clearing; this stub is a no‑op.
    }
}
