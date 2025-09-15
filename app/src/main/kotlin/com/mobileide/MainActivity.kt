package com.mobileide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mobileide.ui.MainScreen
import com.mobileide.ui.PermissionWrapper
import com.mobileide.ui.theme.MobileIDETheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileIDETheme {
                PermissionWrapper {
                    // Die App entscheidet jetzt hier über den Startpunkt
                    MobileIdeApp()
                }
            }
        }
    }
}

@Composable
fun MobileIdeApp(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val hasProjectOpen = viewModel.hasProjectOpen.collectAsStateWithLifecycle(initialValue = null)

    // Zeige einen Ladebildschirm, bis wir wissen, ob ein Projekt offen ist
    if (hasProjectOpen.value == null) {
        // TODO: Loading Screen
        return
    }

    val startDestination = if (hasProjectOpen.value == true) "main_ide" else "project_picker"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("project_picker") {
            com.mobileide.projectpicker.presentation.ProjectPickerScreen(
                onProjectSelected = {
                    navController.navigate("main_ide") {
                        popUpTo("project_picker") { inclusive = true }
                    }
                },
                onNavigateToTemplates = {
                    // TODO: Navigation zu den Templates implementieren
                }
            )
        }
        composable("main_ide") {
            MainScreen()
        }
    }
}
