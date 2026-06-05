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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object PermissionManager {
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    // Compatible with old code calls
    fun hasRequiredPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasAllFilesAccess()
        } else {
            hasBasicStoragePermission(context)
        }
    }

    /**
     * ✅ Smart determination: Whether the specified path requires system permission requests Private directory
     * (Android/data/...) -> Not required -> Return false Public directory (SDCard/...) -> Required -> Return true
     */
    fun isSystemPermissionRequiredForPath(context: Context, path: String): Boolean {
        // Get the root path of the private directory .../Android/data/package_name
        val appExternalDir = context.getExternalFilesDir(null)?.parentFile?.parentFile?.absolutePath

        // If retrieval fails, require permission by default for safety
        if (appExternalDir == null) return true

        // If the path is a subdirectory of a private directory, exempt directly
        if (path.startsWith(appExternalDir)) {
            return false
        }

        // Other directories are judged based on the version
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            !hasBasicStoragePermission(context)
        }
    }

    fun hasBasicStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            true
        } else {
            val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    @Composable
    fun rememberPermissionRequest(
        onPermissionGranted: () -> Unit = {},
        onPermissionDenied: () -> Unit = {},
    ): PermissionRequestState {
        val context = LocalContext.current
        var showRationale by remember { mutableStateOf(false) }

        val allFilesLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (hasAllFilesAccess()) onPermissionGranted() else onPermissionDenied()
            }

        val basicLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
                if (perms.values.all { it }) onPermissionGranted()
                else {
                    onPermissionDenied()
                    showRationale = true
                }
            }

        return remember(context) {
            PermissionRequestState(
                requestPermissions = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (hasAllFilesAccess()) {
                            onPermissionGranted()
                        } else {
                            try {
                                val intent =
                                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = "package:${context.packageName}".toUri()
                                    }
                                allFilesLauncher.launch(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                allFilesLauncher.launch(intent)
                            }
                        }
                    } else {
                        if (hasBasicStoragePermission(context)) {
                            onPermissionGranted()
                        } else {
                            basicLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                )
                            )
                        }
                    }
                },
                showRationale = showRationale,
                hasPermissions = { hasRequiredPermissions(context) },
            )
        }
    }

    data class PermissionRequestState(
        val requestPermissions: () -> Unit,
        val showRationale: Boolean,
        val hasPermissions: () -> Boolean,
    )
}
