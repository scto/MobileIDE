/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.core.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object PermissionHelper {

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
                    android.util.Log.e("PermissionHelper", "Failed to launch ACTION_MANAGE_UNKNOWN_APP_SOURCES", e)
                }
            } else {
                onPermissionResult(true)
            }
        }
    }
}
