/*
 * Copyright 2024 | Dmitri Chernysh | http://mobile-dev.pro
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

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mobiledevpro.di.rememberNavViewModel
import com.mobiledevpro.navigation.Screen
import com.mobiledevpro.people.profile.di.featurePeopleProfileModule
import com.mobiledevpro.people.profile.view.PeopleProfileScreen
import com.mobiledevpro.people.profile.view.args.PeopleProfileArgs
import com.mobiledevpro.people.profile.view.vm.PeopleProfileViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.peopleProfileScreen(
    transitionScope: SharedTransitionScope,
    onNavigateBack: () -> Unit,
    onNavigateTo: (Screen) -> Unit
) {
    composable(
        route = Screen.PeopleProfile.route,
        arguments = listOf(
            navArgument(PeopleProfileArgs.PEOPLE_PROFILE_ID_ARG) { type = NavType.IntType }
        )
    ) {

        val viewModel = rememberNavViewModel<PeopleProfileViewModel>(
            modules = { listOf(featurePeopleProfileModule) }
        )

        val context = LocalContext.current

        val openWebLink: (Uri) -> Unit = { uri ->
            Intent(Intent.ACTION_VIEW).apply {
                data = uri
            }.also { intent ->
                context.startActivity(intent)
            }
        }

        transitionScope.PeopleProfileScreen(
            viewModel.uiState,
            animatedVisibilityScope = this,
            onBackPressed = onNavigateBack,
            onOpenChatWith = {},
            onOpenSocialLink = openWebLink
        )
    }
}
