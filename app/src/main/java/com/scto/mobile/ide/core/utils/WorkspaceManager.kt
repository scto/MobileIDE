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

    fun getDefaultPath(context: Context): String {
        val dir = context.getExternalFilesDir(null)
        return dir?.absolutePath ?: context.filesDir.absolutePath
    }

    /** Get workspace directory (with automatic error correction) */
    fun getWorkspacePath(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPath = prefs.getString(KEY_WORKSPACE_PATH, null)

        // 1. If not saved before, return default
        if (savedPath.isNullOrBlank()) {
            return getDefaultPath(context)
        }

        // 🔥🔥🔥 Fix 2: More robust path checking logic 🔥🔥🔥
        // The previous logic relied on absolute path string matching, which easily caused misjudgments due to the
        // difference between /sdcard and /storage/emulated/0
        // Current logic: As long as the path contains "Android/data", check if it contains "the current App's package
        // name"
        if (savedPath.contains("/Android/data/")) {
            val packageName = context.packageName
            // If the path does not even contain the package name, it means this path definitely belongs to other Apps
            // (or old package names). We have no permission and must reset it.
            if (!savedPath.contains(packageName)) {
                android.util.Log.e(
                    "WorkspaceManager",
                    "Invalid path detected (package name mismatch): $savedPath, resetting to default",
                )
                val validPath = getDefaultPath(context)
                saveWorkspacePath(context, validPath) // Automatically save the corrected path
                return validPath
            }
        }

        return savedPath
    }

    fun isWorkspaceConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // As long as this value is true, it means the user has clicked "Confirm and continue"
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_WORKSPACE_PATH, path)
            // ✅ Key: Set to true, indicating that the user has completed the initialization wizard
            putBoolean(KEY_IS_CONFIGURED, true)
        }
        ensurePathExists(context, path)
    }

    fun ensurePathExists(context: Context, path: String): Boolean {
        val file = File(path)
        if (file.exists() && file.isDirectory) return true

        try {
            if (path.contains(context.packageName)) {
                return file.mkdirs() || file.exists()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file.mkdirs() || file.exists()
    }
}