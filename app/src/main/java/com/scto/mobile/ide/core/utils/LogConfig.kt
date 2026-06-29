/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// LogConfigRepository.kt
package com.scto.mobile.ide.core.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mobileide_log_config")

data class LogEntry(val timestamp: Long, val level: String, val tag: String, val message: String)

data class LogConfigState(val isLogEnabled: Boolean = true, val logFilePath: String = "", val isLoaded: Boolean = false)

class LogConfigRepository(private val context: Context) {
    private object PreferencesKeys {
        val LOG_ENABLED = booleanPreferencesKey("log_enabled")
        val LOG_FILE_PATH = stringPreferencesKey("log_file_path")
    }

    /**
     * ✅ Core fix: Use combine to merge DataStore and WorkspaceManager flows This way, it recalculates here whether log
     * settings or the workspace directory are modified
     */
    val logConfigFlow: Flow<LogConfigState> =
        context.dataStore.data.combine(WorkspaceManager.getWorkspacePathFlow(context)) { preferences, workspacePath ->

            // 1. Get the dynamic workspace directory
            // workspacePath is passed in real-time from the Flow

            // 2. Build the default log directory: workspace/logs
            val defaultLogPath = File(workspacePath, "logs").absolutePath

            // 3. Determine the final path:
            // If the user manually specified a path in DataStore (savedPath is not empty), prioritize the manually
            // specified one
            // If not manually specified (null or empty), automatically follow the workspace directory
            val savedPath = preferences[PreferencesKeys.LOG_FILE_PATH]
            val finalPath = if (savedPath.isNullOrEmpty()) defaultLogPath else savedPath

            LogConfigState(
                isLogEnabled = preferences[PreferencesKeys.LOG_ENABLED] ?: true,
                logFilePath = finalPath,
                isLoaded = true,
            )
        }

    suspend fun saveLogConfig(isEnabled: Boolean, filePath: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOG_ENABLED] = isEnabled

            // Optional optimization: If the path saved by the user matches the current default path, save as
            // null/empty,
            // so that the log path can continue to automatically follow when modifying the workspace directory later.
            val currentWorkspace = WorkspaceManager.getWorkspacePath(context)
            val defaultPath = File(currentWorkspace, "logs").absolutePath

            if (filePath == defaultPath) {
                preferences.remove(PreferencesKeys.LOG_FILE_PATH)
            } else {
                preferences[PreferencesKeys.LOG_FILE_PATH] = filePath
            }
        }
    }

    suspend fun resetLogPath() {
        context.dataStore.edit { preferences -> preferences.remove(PreferencesKeys.LOG_FILE_PATH) }
    }
}

// LogCatcher remains unchanged ...
object LogCatcher {
    // ✅ After fixing, the type reference becomes the simpler LogConfigState
    private var logConfig: LogConfigState? = null

    @JvmStatic
    val isLoggingEnabled: Boolean
        get() = logConfig?.isLogEnabled ?: true

    @Volatile private var isInitialized = false
    @Volatile private var logFile: File? = null

    private val _logFlow = MutableSharedFlow<LogEntry>(extraBufferCapacity = 1000)
    val logFlow = _logFlow.asSharedFlow()

    // Used to store the history records of build logs
    private val _buildLogs = Collections.synchronizedList(ArrayList<LogEntry>())

    @JvmStatic
    fun getBuildLogs(): List<LogEntry> {
        synchronized(_buildLogs) {
            return ArrayList(_buildLogs)
        }
    }

    @JvmStatic
    fun clearBuildLogs() {
        _buildLogs.clear()
    }

    @JvmStatic
    fun updateConfig(config: LogConfigState) {
        val oldConfig = logConfig
        logConfig = config
        isInitialized = true

        if (config.isLogEnabled && config.logFilePath.isNotEmpty()) {
            if (
                oldConfig == null ||
                    !oldConfig.isLogEnabled ||
                    oldConfig.logFilePath != config.logFilePath ||
                    logFile == null
            ) {
                val logDir = File(config.logFilePath)
                if (!logDir.exists()) {
                    logDir.mkdirs()
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
        } else {
            logFile = null
        }

        i(
            "LogCatcher",
            "Log system configured - Enabled: ${config.isLogEnabled}, Path: ${config.logFilePath}, File: ${logFile?.name}",
        )
    }

    @JvmStatic
    fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
        emitLog("DEBUG", tag, message)
        writeToFile("DEBUG", tag, message)
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        android.util.Log.i(tag, message)
        emitLog("INFO", tag, message)
        writeToFile("INFO", tag, message)
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        android.util.Log.w(tag, message)
        emitLog("WARN", tag, message)
        writeToFile("WARN", tag, message)
    }

    @JvmStatic
    @JvmOverloads
    fun e(tag: String, message: String, exception: Throwable? = null) {
        android.util.Log.e(tag, message, exception)
        val msg = "$message${exception?.let { " - ${it.message}" } ?: ""}"
        emitLog("ERROR", tag, msg)
        writeToFile("ERROR", tag, msg)
    }

    private fun emitLog(level: String, tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)

        // If it is a build-related log, save it to the history
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
        val targetFile = logFile ?: return

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
