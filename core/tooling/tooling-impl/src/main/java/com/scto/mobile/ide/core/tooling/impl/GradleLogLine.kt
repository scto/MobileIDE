package com.scto.mobile.ide.core.tooling.impl

enum class GradleLogLevel {
    INFO,
    WARN,
    ERROR,
    TASK,
    SUCCESS,
    DEFAULT
}

data class GradleLogLine(
    val lineNumber: Int,
    val rawText: String,
    val level: GradleLogLevel,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun parse(lineNumber: Int, rawText: String): GradleLogLine {
            val trimmed = rawText.trim()
            val level = when {
                trimmed.startsWith("> Task :") || trimmed.startsWith("Task :") -> GradleLogLevel.TASK
                trimmed.contains("BUILD SUCCESSFUL") -> GradleLogLevel.SUCCESS
                trimmed.startsWith("e:") || trimmed.contains("ERROR:") || trimmed.contains("FAILURE:") || trimmed.contains("FAILED") || trimmed.contains("Exception") -> GradleLogLevel.ERROR
                trimmed.startsWith("w:") || trimmed.contains("WARNING:") || trimmed.startsWith("WARN ") -> GradleLogLevel.WARN
                trimmed.startsWith("i:") || trimmed.contains("INFO:") -> GradleLogLevel.INFO
                else -> GradleLogLevel.DEFAULT
            }
            return GradleLogLine(lineNumber, rawText, level)
        }
    }
}
