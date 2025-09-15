package com.mobileide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

import com.mobileide.debug.presentation.DebugScreen
import com.mobileide.editor.presentation.EditorScreen
import com.mobileide.explorer.presentation.ExplorerScreen
import com.mobileide.git.presentation.GitScreen
import com.mobileide.settings.presentation.SettingsScreen
import com.mobileide.termux.presentation.TermuxScreen

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "explorer", // Start with the Explorer
        modifier = modifier
    ) {
        composable("explorer") {
            ExplorerScreen(
                onOpenFile = { filePath ->
                    val encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("editor/$encodedPath")
                }
            )
        }

        composable(
            route = "editor/{filePath}",
            arguments = listOf(navArgument("filePath") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
             val encodedPath = backStackEntry.arguments?.getString("filePath")
             val filePath = encodedPath?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
            EditorScreen(filePath = filePath)
        }

        // Empty editor route
        composable("editor") {
            EditorScreen(filePath = null)
        }

        composable("git") {
            GitScreen()
        }
        
        composable("termux") {
            TermuxScreen()
        }

        composable("debug") {
            DebugScreen()
        }
        
        composable("settings") {
            SettingsScreen()
        }
    }
}
