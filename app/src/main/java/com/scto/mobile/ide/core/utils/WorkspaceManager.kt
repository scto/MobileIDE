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

package com.scto.mobile.ide.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object WorkspaceManager {
    private const val PREFS_NAME = "mobileide_prefs"
    private const val KEY_WORKSPACE_PATH = "workspace_path"
    private const val KEY_IS_CONFIGURED = "is_workspace_configured"

    fun cleanPath(context: Context, path: String): String {
        if (path.contains("/Android/data/")) {
            val parts = path.split("/Android/data/")
            if (parts.size == 2) {
                val subPath = parts[1]
                val remaining = subPath.substringAfter("/", "")
                return "${parts[0]}/Android/data/${context.packageName}${if (remaining.isNotEmpty()) "/$remaining" else ""}"
            }
        }
        return path.replace(".debug", "").replace(".release", "")
    }

    fun getDefaultPath(context: Context): String {
        val internalDir = File(context.filesDir, "MobileIDEProjects")
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        return internalDir.absolutePath
    }

    /** Get workspace directory (with automatic error correction) */
    fun getWorkspacePath(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPath = prefs.getString(KEY_WORKSPACE_PATH, null)

        // 1. If not saved before, return default
        if (savedPath.isNullOrBlank()) {
            return cleanPath(context, getDefaultPath(context))
        }

        val cleanedPath = cleanPath(context, savedPath)

        // Migrate legacy shared storage path which is incompatible with Gradle/PRoot executions
        if (cleanedPath.startsWith("/storage/emulated/0/MobileIDEProjects")) {
            val validPath = cleanPath(context, getDefaultPath(context))
            saveWorkspacePath(context, validPath)
            return validPath
        }

        // Check if path is in private Android/data directory
        if (cleanedPath.contains("/Android/data/")) {
            // Strip .debug or other build suffixes to see if it belongs to MobileIDE
            val basePackage = context.packageName.substringBefore(".debug").substringBefore(".release")
            if (!cleanedPath.contains(basePackage)) {
                android.util.Log.e(
                    "WorkspaceManager",
                    "Invalid path detected (package name mismatch): $cleanedPath, resetting to default",
                )
                val validPath = cleanPath(context, getDefaultPath(context))
                saveWorkspacePath(context, validPath)
                return validPath
            }
        }

        return cleanedPath
    }

    fun isWorkspaceConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_CONFIGURED, false)
    }

    fun getWorkspacePathFlow(context: Context): Flow<String> = callbackFlow {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_WORKSPACE_PATH) {
                    trySend(getWorkspacePath(context))
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getWorkspacePath(context))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun saveWorkspacePath(context: Context, path: String) {
        val cleanedPath = cleanPath(context, path)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_WORKSPACE_PATH, cleanedPath)
            putBoolean(KEY_IS_CONFIGURED, true)
        }
        ensurePathExists(context, cleanedPath)
    }

    fun ensurePathExists(context: Context, path: String): Boolean {
        val file = File(path)
        if (file.exists() && file.isDirectory) return true

        return try {
            file.mkdirs() || file.exists()
        } catch (e: Exception) {
            e.printStackTrace()
            file.exists()
        }
    }
}
