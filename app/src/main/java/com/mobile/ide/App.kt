// Copyright 2025 Thomas Schmid
package com.mobile.ide

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.mobile.ide.R
import com.mobile.ide.ui.editor.CodeEditScreen
import com.mobile.ide.ui.editor.viewmodel.EditorViewModel
import com.mobile.ide.ui.editor.components.TextMateInitializer
import com.mobile.ide.ui.preview.WebPreviewScreen
import com.mobile.ide.ui.projects.NewProjectScreen
import com.mobile.ide.ui.projects.ProjectListScreen
import com.mobile.ide.ui.projects.WorkspaceSelectionScreen
import com.mobile.ide.ui.settings.AboutScreen
import com.mobile.ide.ui.settings.SettingsScreen
import com.mobile.ide.ui.welcome.WelcomeScreen
import com.mobile.ide.core.projects.*
import com.mobile.ide.core.resources.Res
import com.mobile.ide.core.ui.components.*
import com.mobile.ide.core.ui.theme.*
import com.mobile.ide.core.utils.*

import kotlinx.coroutines.launch

@Composable
fun App(
    themeViewModel: ThemeViewModel,
    logConfigRepository: LogConfigRepository,
    logConfigState: LogConfigState
) {
    val navController = rememberNavController()
    val mainViewModel: EditorViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        mainViewModel.initializePermissions(context)
    }

    val startDestination = if (
        WorkspaceManager.getWorkspacePath(context) != WorkspaceManager.getDefaultPath(context)
    ) {
        "project_list"
    } else {
        "workspace_selection"
    }
    val themeState by themeViewModel.themeState.collectAsState()

    val predictiveEasing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
    val duration = 350 

    val enterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(duration, easing = predictiveEasing)
        ) + fadeIn(
            animationSpec = tween(duration, easing = predictiveEasing)
        )
    }

    val exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -(it * 0.3f).toInt() },
            animationSpec = tween(duration, easing = predictiveEasing)
        ) + fadeOut(
            targetAlpha = 0.7f,
            animationSpec = tween(duration, easing = predictiveEasing)
        )
    }

    val popEnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -(it * 0.3f).toInt() },
            animationSpec = tween(duration, easing = predictiveEasing)
        ) + fadeIn(
            initialAlpha = 0.7f,
            animationSpec = tween(duration, easing = predictiveEasing)
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(duration, easing = predictiveEasing)
        )
    }

    val popExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(duration, easing = predictiveEasing)
        ) + fadeOut(
            targetAlpha = 0f,
            animationSpec = tween(duration, easing = predictiveEasing)
        ) + scaleOut(
            targetScale = 1.1f,
            animationSpec = tween(duration, easing = predictiveEasing)
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { enterTransition() },
        exitTransition = { exitTransition() },
        popEnterTransition = { popEnterTransition() },
        popExitTransition = { popExitTransition() }
    ) {
        composable("workspace_selection") {
            WorkspaceSelectionScreen(navController = navController)
        }

        composable("project_list") {
            ProjectListScreen(navController = navController)
        }

        composable(
            route = "code_edit/{folderName}",
            arguments = listOf(navArgument("folderName") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName")
            if (folderName != null) {
                CodeEditScreen(folderName, navController, mainViewModel)
            }
        }

        composable(
            route = "preview/{folderName}",
            arguments = listOf(navArgument("folderName") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName")
            if (folderName != null) {
                WebPreviewScreen(folderName, navController, mainViewModel)
            }
        }

        composable("new_project") {
            NewProjectScreen(navController = navController)
        }

        composable("settings") {
            SettingsScreen(
                navController,
                themeState,
                logConfigState,
                onThemeChange = { mode, theme, color, isMonet, isCustom ->
                    themeViewModel.saveThemeConfig(mode, theme, color, isMonet, isCustom)
                },
                onLogConfigChange = { enabled, filePath ->
                    scope.launch { logConfigRepository.saveLogConfig(enabled, filePath) }
                }
            )
        }

        composable("about") {
            AboutScreen(navController = navController)
        }
    }
}
