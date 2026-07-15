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

package com.scto.mobile.ide

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.scto.mobile.ide.core.utils.LogConfigRepository
import com.scto.mobile.ide.core.utils.LogConfigState
import com.scto.mobile.ide.core.utils.WorkspaceManager
import com.scto.mobile.ide.ui.ThemeViewModel
import com.scto.mobile.ide.ui.editor.CodeEditScreen
import com.scto.mobile.ide.ui.editor.doc.JsInterfaceDocScreen
import com.scto.mobile.ide.ui.editor.viewmodel.EditorViewModel
import com.scto.mobile.ide.ui.projects.NewProjectScreen
import com.scto.mobile.ide.ui.projects.ProjectListScreen
import com.scto.mobile.ide.ui.projects.WorkspaceSelectionScreen
import com.scto.mobile.ide.ui.settings.AboutScreen
import com.scto.mobile.ide.ui.settings.BuildSettingsScreen
import com.scto.mobile.ide.ui.settings.EditorScreen
import com.scto.mobile.ide.ui.settings.LspSettingsScreen
import com.scto.mobile.ide.ui.settings.SettingsScreen
import com.scto.mobile.ide.ui.settings.ThemeSettingsScreen
import com.scto.mobile.ide.ui.terminal.TerminalScreen
import com.scto.mobile.ide.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navController: NavHostController,
    themeViewModel: ThemeViewModel,
    logConfigRepository: LogConfigRepository,
    logConfigState: LogConfigState,
) {
    val mainViewModel: EditorViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { mainViewModel.initializePermissions(context) }

    val startDestination =
        if (WorkspaceManager.getWorkspacePath(context) != WorkspaceManager.getDefaultPath(context)) {
            "project_list"
        } else {
            "workspace_selection"
        }

    val themeState by themeViewModel.themeState.collectAsState()

    // Optimized animation configuration
    // Use a more natural easing curve (similar to iOS smoothness)
    val predictiveEasing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
    val duration = 350 // Slightly shorten the duration to make the response faster

    // Enter transition (forward navigation)
    val enterTransition = {
        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(duration, easing = predictiveEasing)) +
            fadeIn(animationSpec = tween(duration, easing = predictiveEasing))
    }

    // Exit transition (when navigating forward)
    val exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -(it * 0.3f).toInt() },
            animationSpec = tween(duration, easing = predictiveEasing),
        ) + fadeOut(targetAlpha = 0.7f, animationSpec = tween(duration, easing = predictiveEasing))
    }

    // Pop enter transition (underlying page reappears on back)
    val popEnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -(it * 0.3f).toInt() },
            animationSpec = tween(duration, easing = predictiveEasing),
        ) +
            fadeIn(initialAlpha = 0.7f, animationSpec = tween(duration, easing = predictiveEasing)) +
            scaleIn(initialScale = 0.95f, animationSpec = tween(duration, easing = predictiveEasing))
    }

    // Pop exit transition (current page disappears on back)
    val popExitTransition = {
        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(duration, easing = predictiveEasing)) +
            fadeOut(targetAlpha = 0f, animationSpec = tween(duration, easing = predictiveEasing)) +
            scaleOut(targetScale = 1.1f, animationSpec = tween(duration, easing = predictiveEasing))
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,

        // Apply optimized transitions
        enterTransition = { enterTransition() },
        exitTransition = { exitTransition() },
        popEnterTransition = { popEnterTransition() },
        popExitTransition = { popExitTransition() },
    ) {
        composable("workspace_selection") { WorkspaceSelectionScreen(navController = navController) }

        composable("project_list") { ProjectListScreen(navController = navController) }

        composable(
            route = "code_edit/{folderName}",
            arguments = listOf(navArgument("folderName") { type = NavType.StringType }),
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName")
            if (folderName != null) {
                CodeEditScreen(folderName, navController, mainViewModel)
            }
        }

        composable("new_project") { NewProjectScreen(navController = navController) }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                currentThemeState = themeState,
                logConfigState = logConfigState,
                onThemeChange = { modeIndex, themeIndex, customColor, isMonet, isCustom ->
                    themeViewModel.saveThemeConfig(modeIndex, themeIndex, customColor, isMonet, isCustom)
                },
                onLogConfigChange = { enabled, path ->
                    scope.launch { logConfigRepository.saveLogConfig(enabled, path) }
                },
                editorViewModel = mainViewModel,
            )
        }
        composable("settings/editor") { EditorScreen(navController = navController, editorViewModel = mainViewModel) }
        composable("settings/build") { BuildSettingsScreen(navController = navController) }
        composable("settings/lsp") { LspSettingsScreen(navController = navController) }
        composable("settings/extensions") {
            com.scto.mobile.ide.ui.settings.ExtensionSettingsScreen(navController = navController)
        }
        composable("settings/theme") {
            ThemeSettingsScreen(
                navController = navController,
                currentThemeState = themeState,
                onThemeChange = { modeIndex, themeIndex, customColor, isMonet, isCustom ->
                    themeViewModel.saveThemeConfig(modeIndex, themeIndex, customColor, isMonet, isCustom)
                },
            )
        }

        composable("welcome") {
            WelcomeScreen(themeViewModel = themeViewModel, onWelcomeFinished = { navController.popBackStack() })
        }

        composable("js_interface_doc") { JsInterfaceDocScreen(navController) }

        composable("about") { AboutScreen(navController = navController) }

        composable("terminal") { TerminalScreen(navController = navController) }
    }
}

fun NavController.safeNavigate(route: String) {
    val current = currentBackStackEntry?.destination?.route
    if (current != route) {
        navigate(route) { launchSingleTop = true }
    }
}
