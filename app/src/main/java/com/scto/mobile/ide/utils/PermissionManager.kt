package com.scto.mobile.ide.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionManager {
    // Check if required permissions (storage, etc.) are granted
    fun hasRequiredPermissions(context: Context): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        return permissions.all { hasPermission(context, it) }
    }

    fun hasInstallPermission(context: Context): Boolean =
        hasPermission(context, android.Manifest.permission.REQUEST_INSTALL_PACKAGES)

    fun hasPostNotificationsPermission(context: Context): Boolean =
        hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)

    fun hasNotificationAccess(context: Context): Boolean {
        // Placeholder: real implementation would check NotificationListenerService
        return true
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        // Placeholder: real implementation would query PowerManager
        return true
    }

    fun rememberPermissionRequest(tag: String, onResult: (Boolean) -> Unit) {
        // Stub: UI layer will handle showing permission dialog and call onResult
    }

    fun rememberInstallPermissionRequest(onResult: (Boolean) -> Unit) {
        // Stub
    }

    fun rememberPostNotificationsPermissionRequest(onResult: (Boolean) -> Unit) {
        // Stub
    }

    fun rememberNotificationAccessRequest(onResult: (Boolean) -> Unit) {
        // Stub
    }

    fun rememberIgnoreBatteryOptimizationsRequest(onResult: (Boolean) -> Unit) {
        // Stub
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
