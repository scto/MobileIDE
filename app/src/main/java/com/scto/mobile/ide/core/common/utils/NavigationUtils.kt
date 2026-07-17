package com.scto.mobile.ide.core.common.utils

import androidx.navigation.NavController

/**
 * Extension function to safely navigate to a route only if it differs from the current destination.
 * This prevents crashes caused by rapid duplicate navigation attempts.
 */
fun NavController.safeNavigate(route: String) {
    val current = currentBackStackEntry?.destination?.route
    if (current != route) {
        navigate(route) {
            launchSingleTop = true
        }
    }
}
