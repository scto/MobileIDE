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
package com.scto.mobile.ide.utils

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
import com.scto.mobile.ide.core.common.utils.WorkspaceManager
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


