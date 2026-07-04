package com.scto.mobile.ide.core.tooling.api

/**
 * Represents a single event emitted by a Gradle build execution.
 */
sealed class GradleLogEvent {

    /** A progress notification (task started, configuration phase, etc.) */
    data class Progress(
        val timestamp: Long = System.currentTimeMillis(),
        val message: String,
    ) : GradleLogEvent()

    /** A standard or error log line from the build output. */
    data class Output(
        val timestamp: Long = System.currentTimeMillis(),
        val logLevel: String,   // "INFO", "WARN", "ERROR", "DEBUG", "QUIET"
        val text: String,
    ) : GradleLogEvent()

    /** Final result of the build. Terminates the event stream. */
    data class Result(
        val isSuccess: Boolean,
        val message: String,
    ) : GradleLogEvent()
}
