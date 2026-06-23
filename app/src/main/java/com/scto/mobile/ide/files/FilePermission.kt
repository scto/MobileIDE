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

package com.scto.mobile.ide.files

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.rk.resources.getString
import com.rk.resources.strings
import com.rk.settings.Settings
import com.scto.mobile.ide.core.utils.dialogRes
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object FilePermission {
    private const val REQUEST_CODE_STORAGE_PERMISSIONS = 1259

    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        scope: CoroutineScope,
        activity: Activity,
    ) {
        // check permission for old devices
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // permission denied ask again

                scope.launch { verifyStoragePermission(activity) }
            }
        }
    }

    private var dialogRef = WeakReference<AlertDialog?>(null)

    fun verifyStoragePermission(activity: Activity) {
        dialogRef.get()?.apply {
            if (isShowing) {
                dismiss()
            }
        }
        dialogRef = WeakReference(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Settings.ignore_storage_permission = false
            }
        }
        if (Settings.ignore_storage_permission) {
            return
        }
        var shouldAsk = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                shouldAsk = true
            }
        } else {
            if (
                ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED
            ) {
                shouldAsk = true
            }
        }
        if (shouldAsk) {
            dialogRes(
                activity = activity,
                title = strings.manage_storage.getString(),
                msg = strings.manage_storage_reason.getString(),
                cancelRes = strings.ignore,
                okRes = strings.ok,
                onOk = {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                        val intent = Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = "package:${activity.packageName}".toUri()
                        activity.startActivity(intent)
                    } else {
                        // below 11
                        // Request permissions
                        val perms =
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            )
                        ActivityCompat.requestPermissions(activity, perms, REQUEST_CODE_STORAGE_PERMISSIONS)
                    }
                },
                onCancel = { Settings.ignore_storage_permission = true },
                cancelable = false,
            )
        }
    }
}
