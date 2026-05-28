// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import com.mobile.ide.core.utils.*

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mobileide_log_config")

data class LogConfigState(val isLogEnabled: Boolean = true, val logFilePath: String = "", val isLoaded: Boolean = false)

class LogConfigRepository(private val context: Context) {
    private object PreferencesKeys {
        val LOG_ENABLED = booleanPreferencesKey("log_enabled")
        val LOG_FILE_PATH = stringPreferencesKey("log_file_path")
    }

    val logConfigFlow: Flow<LogConfigState> =
        context.dataStore.data.combine(WorkspaceManager.getWorkspacePathFlow(context)) { preferences, workspacePath ->
            val defaultLogPath = File(workspacePath, "logs").absolutePath
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

object LogCatcher {
    private var logConfig: LogConfigState? = null
    @Volatile private var isInitialized = false

    @JvmStatic
    fun updateConfig(config: LogConfigState) {
        logConfig = config
        isInitialized = true
        i("LogCatcher", "Log system configured - Enabled: ${config.isLogEnabled}, Path: ${config.logFilePath}")
    }

    @JvmStatic
    fun d(tag: String, message: String) {
        if (shouldLog()) {
            android.util.Log.d(tag, message)
            writeToFile("DEBUG", tag, message)
        }
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        if (shouldLog()) {
            android.util.Log.i(tag, message)
            writeToFile("INFO", tag, message)
        }
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        if (shouldLog()) {
            android.util.Log.w(tag, message)
            writeToFile("WARN", tag, message)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun e(tag: String, message: String, exception: Exception? = null) {
        android.util.Log.e(tag, message, exception)
        if (shouldLog()) {
            writeToFile("ERROR", tag, "$message${exception?.let { " - ${it.message}" } ?: ""}")
        }
    }

    @JvmStatic
    fun permission(tag: String, action: String, result: String) {
        if (shouldLog()) {
            val message = "Permission operation: $action - $result"
            android.util.Log.i("$tag-Permission", message)
            writeToFile("PERMISSION", tag, message)
        }
    }

    @JvmStatic
    fun fileOperation(tag: String, operation: String, filePath: String, result: String) {
        if (shouldLog()) {
            val message = "File operation: $operation - $filePath - $result"
            android.util.Log.i("$tag-File", message)
            writeToFile("FILE", tag, message)
        }
    }

    private fun shouldLog(): Boolean {
        return isInitialized && logConfig?.isLogEnabled == true
    }

    private fun writeToFile(level: String, tag: String, message: String) {
        val config = logConfig ?: return
        if (!config.isLogEnabled) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val logDir = File(config.logFilePath)
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                val logFile = File(logDir, "mobileide.log")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logEntry = "[$timestamp] [$level] [$tag] $message\n"
                logFile.appendText(logEntry)
            } catch (e: Exception) {
                android.util.Log.e("LogCatcher", "Failed to write to log file: ${e.message}")
            }
        }
    }
}
