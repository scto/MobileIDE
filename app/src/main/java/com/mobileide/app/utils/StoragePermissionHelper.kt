package com.mobileide.app.utils

import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Unified storage permission helper.
 *
 * Android permission model by API level:
 *
 *  API ≤ 29 (Android 9-)  → READ_ + WRITE_EXTERNAL_STORAGE (runtime permissions)
 *  API 30-32 (Android 11-12) → MANAGE_EXTERNAL_STORAGE via Settings intent
 *  API 33+  (Android 13+)  → MANAGE_EXTERNAL_STORAGE still works;
 *                             alternatively READ_MEDIA_IMAGES/VIDEO/AUDIO but
 *                             those don't cover arbitrary file access we need.
 *
 *  We always request MANAGE_EXTERNAL_STORAGE on API 30+ because we need to
 *  read/write project files anywhere on external storage.
 */
object StoragePermissionHelper {

    /** True if the app currently has full external storage access. */
    fun hasFullStorageAccess(context: Context): Boolean {
        Logger.info(LogTag.STORAGE, "check storage access")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns true if we need to show a rationale / request dialog.
     * On API 30+ this is always true when [hasFullStorageAccess] is false,
     * because we must open the Settings screen.
     */
    fun needsPermission(context: Context) = !hasFullStorageAccess(context)

    /**
     * Build the correct Intent to request storage permission.
     * - API 30+: opens the "Allow all files access" screen for this app.
     * - API ≤ 29: caller should use ActivityResultContracts.RequestMultiplePermissions.
     */
    fun buildManageStorageIntent(context: Context): Intent? {
        Logger.info(LogTag.STORAGE, "buildManageStorageIntent API=${android.os.Build.VERSION.SDK_INT}")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            } catch (_: Exception) {
                // Fallback to general manage-all-files screen
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else null   // handled via ActivityResultContracts on API ≤ 29
    }

    /** The legacy permissions needed on API ≤ 29. */
    val LEGACY_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Returns a human-readable description of the current permission state.
     */
    fun statusText(context: Context): String {
        return if (hasFullStorageAccess(context)) {
            "Granted ✓"
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                "Not granted — tap to open Settings"
            else
                "Not granted — tap to request"
        }
    }
}
