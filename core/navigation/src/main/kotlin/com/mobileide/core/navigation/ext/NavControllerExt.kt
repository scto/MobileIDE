/*
 * Copyright 2022 | Dmitri Chernysh | https://mobile-dev.pro
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.mobiledevpro.navigation.ext

import android.util.Log
import androidx.navigation.NavController
import com.mobiledevpro.navigation.Screen


fun NavController.navigateTo(
    screen: Screen
) {

    val currentRoute: String? = this.currentBackStackEntry?.destination?.route

    val route = screen.routePath?.let { routePath ->
        screen.route.replaceAfter("/", routePath)
    } ?: screen.route

    Log.d("navigation", "navigateTo: ${screen.route}")

    navigate(route) {

        // Pop up to the certain destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        screen.popUpToScreen?.let { screen ->
            Log.d("navigation", "pop up to: ${screen.route}")
            popUpTo(screen.route) {
                saveState = true
            }
        }
        // Avoid multiple copies of the same destination when
        // reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = screen.restoreState

        //Clearing back stack up to certain screen if required
        if (screen.clearBackStack && !currentRoute.isNullOrEmpty())
            popUpTo(currentRoute) {
                inclusive = true
            }
    }
}