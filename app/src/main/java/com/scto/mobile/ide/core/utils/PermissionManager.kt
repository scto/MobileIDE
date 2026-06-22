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

    /** Checks if the app can request package installations. */
    fun hasInstallPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /** Checks if the WakeLock permission is granted. */
    fun hasWakeLockPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WAKE_LOCK) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Checks if the Foreground Service permission is granted. */
    fun hasForegroundServicePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** Checks if the Request Delete Packages permission is declared/granted. */
    fun hasDeletePackagesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_DELETE_PACKAGES) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** Composable helper to remember and trigger package install permission request. */
    @Composable
    fun rememberInstallPermissionRequest(onPermissionResult: (Boolean) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                onPermissionResult(hasInstallPermission(context))
            }

        return {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent =
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                try {
                    launcher.launch(intent)
                } catch (e: Exception) {
                    android.util.Log.e("PermissionManager", "Failed to launch ACTION_MANAGE_UNKNOWN_APP_SOURCES", e)
                }
            } else {
                onPermissionResult(true)
            }
        }
    }

    /** Checks if the app can post notifications (Android 13+). */
    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** Composable helper to remember and trigger push notifications permission request. */
    @Composable
    fun rememberPostNotificationsPermissionRequest(onPermissionResult: (Boolean) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
                onPermissionResult(granted)
            }

        return {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onPermissionResult(true)
            }
        }
    }

    /** Checks if notification access listener is enabled. */
    fun hasNotificationAccess(context: Context): Boolean {
        return try {
            val packageNames = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
            packageNames.contains(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    /** Composable helper to remember and trigger notification access settings page request. */
    @Composable
    fun rememberNotificationAccessRequest(onPermissionResult: (Boolean) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                onPermissionResult(hasNotificationAccess(context))
            }

        return {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            try {
                launcher.launch(intent)
            } catch (e: Exception) {
                android.util.Log.e("PermissionManager", "Failed to launch ACTION_NOTIFICATION_LISTENER_SETTINGS", e)
            }
        }
    }

    /** Checks if the app is ignoring battery optimizations. */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /** Composable helper to remember and trigger ignore battery optimizations request. */
    @Composable
    fun rememberIgnoreBatteryOptimizationsRequest(onPermissionResult: (Boolean) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                onPermissionResult(isIgnoringBatteryOptimizations(context))
            }

        return {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                try {
                    launcher.launch(intent)
                } catch (e: Exception) {
                    android.util.Log.e("PermissionManager", "Failed to launch ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", e)
                }
            } else {
                onPermissionResult(true)
            }
        }
    }
}
