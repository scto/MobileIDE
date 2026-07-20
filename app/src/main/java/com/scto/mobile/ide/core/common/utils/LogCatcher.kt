package com.scto.mobile.ide.core.common.utils

import android.content.Context
import com.scto.mobile.ide.utils.LogConfigState
import com.scto.mobile.ide.utils.LogEntry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

object LogCatcher {
    private var logConfig: LogConfigState? = null

    val isLoggingEnabled: Boolean
        get() = logConfig?.isLogEnabled ?: true

    @Volatile private var isInitialized = false
    @Volatile private var logFile: File? = null
    @Volatile private var cachedProject: String? = null
    private var contextRef: WeakReference<Context>? = null

    private val _logFlow = MutableSharedFlow<LogEntry>(extraBufferCapacity = 1000)
    val logFlow = _logFlow.asSharedFlow()

    // Used to store the history records of build logs
    private val _buildLogs = Collections.synchronizedList(ArrayList<LogEntry>())

    val currentLogFilePath: String?
        get() = getOrUpdateLogFile()?.absolutePath

    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
    }

    fun getBuildLogs(): List<LogEntry> {
        synchronized(_buildLogs) {
            return ArrayList(_buildLogs)
        }
    }

    fun clearBuildLogs() {
        _buildLogs.clear()
    }

    private fun getOrUpdateLogFile(): File? {
        val config = logConfig ?: return null
        if (!config.isLogEnabled || config.logFilePath.isEmpty()) return null

        val currentProj = com.scto.mobile.ide.ui.terminal.DistroManager.currentProject
        if (logFile == null || currentProj != cachedProject) {
            synchronized(this) {
                if (logFile == null || currentProj != cachedProject) {
                    cachedProject = currentProj
                    var logDir =
                        if (!currentProj.isNullOrBlank()) {
                            val projName = File(currentProj).name
                            File(config.logFilePath, projName)
                        } else {
                            File(config.logFilePath)
                        }

                    var success = true
                    try {
                        if (!logDir.exists()) {
                            success = logDir.mkdirs()
                        }
                        if (success) {
                            val testFile = File(logDir, ".test_write")
                            if (testFile.createNewFile()) {
                                testFile.delete()
                            } else {
                                success = false
                            }
                        }
                    } catch (e: Exception) {
                        success = false
                    }

                    if (!success) {
                        val context = contextRef?.get()
                        if (context != null) {
                            val fallbackDir = context.getExternalFilesDir("logs") ?: File(context.filesDir, "logs")
                            logDir = if (!currentProj.isNullOrBlank()) {
                                val projName = File(currentProj).name
                                File(fallbackDir, projName)
                            } else {
                                fallbackDir
                            }
                            logDir.mkdirs()
                        }
                    }

                    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val formattedDate = sdf.format(Date())
                    var file = File(logDir, "mobileide_$formattedDate.log")
                    var counter = 1
                    while (file.exists()) {
                        file = File(logDir, "mobileide_${formattedDate}_$counter.log")
                        counter++
                    }
                    logFile = file
                }
            }
        }
        return logFile
    }

    fun updateConfig(config: LogConfigState) {
        logConfig = config
        isInitialized = true
        synchronized(this) {
            logFile = null
            cachedProject = null
        }
        getOrUpdateLogFile()

        i(
            "LogCatcher",
            "Log system configured - Enabled: ${config.isLogEnabled}, Path: ${config.logFilePath}, File: ${logFile?.name}",
        )
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.d(tag, message, throwable)
        val msg = "$message${throwable?.let { " - ${it.message}" } ?: ""}"
        emitLog("DEBUG", tag, msg)
        writeToFile("DEBUG", tag, msg)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.i(tag, message, throwable)
        val msg = "$message${throwable?.let { " - ${it.message}" } ?: ""}"
        emitLog("INFO", tag, msg)
        writeToFile("INFO", tag, msg)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.w(tag, message, throwable)
        val msg = "$message${throwable?.let { " - ${it.message}" } ?: ""}"
        emitLog("WARN", tag, msg)
        writeToFile("WARN", tag, msg)
    }

    fun e(tag: String, message: String, exception: Throwable? = null) {
        android.util.Log.e(tag, message, exception)
        val msg = "$message${exception?.let { " - ${it.message}" } ?: ""}"
        emitLog("ERROR", tag, msg)
        writeToFile("ERROR", tag, msg)
    }

    private fun emitLog(level: String, tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)

        if (tag == "ApkBuilder" || tag == "Build") {
            _buildLogs.add(entry)
        }

        _logFlow.tryEmit(entry)

        try {
            com.scto.mobile.ide.core.tooling.impl.ToolingLogManagerImpl.handleLogEntry(level, tag, message)
        } catch (_: Exception) {}
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun writeToFile(level: String, tag: String, message: String) {
        val config = logConfig ?: return
        if (!config.isLogEnabled) return
        val targetFile = getOrUpdateLogFile() ?: return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logEntry = "[$timestamp] [$level] [$tag] $message\n"
                targetFile.appendText(logEntry)
            } catch (e: Exception) {
                android.util.Log.e("LogCatcher", "Failed to write to log file: ${e.message}")
            }
        }
    }
}
