/*
 * Copyright 2025 | Dmitri Chernysh | https://github.com/dmitriy-chernysh
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
package com.mobiledevpro.navigation.screen

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mobiledevpro.di.rememberNavViewModel
import com.mobiledevpro.navigation.Screen
import com.mobiledevpro.ui.compositionlocal.LocalAnalytics
import com.mobiledevpro.ui.ext.findActivity
import com.mobiledevpro.user.profile.di.featureUserProfileModule
import com.mobiledevpro.user.profile.view.ProfileScreen
import com.mobiledevpro.user.profile.view.vm.ProfileViewModel

fun NavGraphBuilder.userProfileScreen(onNavigateTo: (Screen) -> Unit) {
    composable(
        route = Screen.Profile.route
    ) {

        val context = LocalContext.current
        val analytics = LocalAnalytics.current

        LaunchedEffect(Unit) {
            analytics.trackScreen(
                "ProfileScreen",
                context.findActivity()
            )
        }

        val viewModel = rememberNavViewModel<ProfileViewModel>(
            modules = {
                listOf(featureUserProfileModule)
            }
        )

        ProfileScreen(
            state = viewModel.uiState,
            onDarkModeSwitched = { isDarkMode ->
                viewModel.onDarkModeSwitched(isDarkMode)
            },
            onNavigateToSubscription = {
                onNavigateTo(Screen.Subscription)
            }
        )
    }
}
