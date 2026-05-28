// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mobile.ide.core.resources.R
import com.mobile.ide.core.utils.*

object PermissionManager {
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun hasBasicStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                )
                .all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            true
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).all {
                permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun hasRequiredPermissions(context: Context): Boolean {
        return hasAllFilesAccess() || hasBasicStoragePermission(context)
    }

    fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent =
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                context.startActivity(intent)
                LogCatcher.i("PermissionManager", context.getString(R.string.perm_log_navigating))
            } catch (e: Exception) {
                LogCatcher.e("PermissionManager", context.getString(R.string.perm_log_error_open), e)
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intent)
            }
        }
    }

    @Composable
    fun rememberPermissionRequest(
        onPermissionGranted: () -> Unit = {},
        onPermissionDenied: () -> Unit = {},
    ): PermissionRequestState {
        val context = LocalContext.current
        var showRationale by remember { mutableStateOf(false) }

        val allFilesAccessLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                if (hasAllFilesAccess()) {
                    LogCatcher.i("PermissionManager", context.getString(R.string.perm_log_granted_all))
                    onPermissionGranted()
                } else {
                    LogCatcher.w("PermissionManager", context.getString(R.string.perm_log_denied_all))
                    onPermissionDenied()
                }
            }

        val basicPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    LogCatcher.i("PermissionManager", context.getString(R.string.perm_log_granted_basic))
                    onPermissionGranted()
                } else {
                    LogCatcher.w(
                        "PermissionManager",
                        context.getString(R.string.perm_log_denied_basic, permissions.toString()),
                    )
                    onPermissionDenied()
                    showRationale = true
                }
            }

        return remember(context) {
            PermissionRequestState(
                requestPermissions = {
                    LogCatcher.d("PermissionManager", context.getString(R.string.perm_log_start_req))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (hasAllFilesAccess()) {
                            onPermissionGranted()
                        } else {
                            try {
                                val intent =
                                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                allFilesAccessLauncher.launch(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                allFilesAccessLauncher.launch(intent)
                            }
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val permissions =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_IMAGES,
                                    Manifest.permission.READ_MEDIA_VIDEO,
                                    Manifest.permission.READ_MEDIA_AUDIO,
                                )
                            } else {
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                )
                            }
                        basicPermissionLauncher.launch(permissions)
                    } else {
                        onPermissionGranted()
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

    sealed class PermissionResult {
        object Granted : PermissionResult()

        object Denied : PermissionResult()

        data class Rationale(val message: String) : PermissionResult()
    }

    fun checkPermissionsWithResult(context: Context): PermissionResult {
        return if (hasRequiredPermissions(context)) {
            PermissionResult.Granted
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionResult.Rationale(context.getString(R.string.perm_rationale_all))
            } else {
                PermissionResult.Rationale(context.getString(R.string.perm_rationale_basic))
            }
        }
    }
}
