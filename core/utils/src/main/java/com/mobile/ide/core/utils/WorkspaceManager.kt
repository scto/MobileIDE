// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

import com.mobile.ide.core.utils.*
import com.mobile.ide.core.resources.Res

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object WorkspaceManager {
    private const val PREFS_NAME = "mobileide_prefs"
    private const val KEY_WORKSPACE_PATH = "workspace_path"

    fun getDefaultPath(context: Context): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    }

    fun getWorkspacePath(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WORKSPACE_PATH, null) ?: getDefaultPath(context)
    }

    fun getWorkspacePathFlow(context: Context): Flow<String> = callbackFlow {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultPath = getDefaultPath(context)

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == KEY_WORKSPACE_PATH) {
                    val path = sharedPreferences.getString(key, null) ?: defaultPath
                    trySend(path)
                }
            }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString(KEY_WORKSPACE_PATH, null) ?: defaultPath)

        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun saveWorkspacePath(context: Context, path: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_WORKSPACE_PATH, path) }
    }
}
