package com.mobileide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mobileide.sidepanel.presentation.SidePanelScreen
import com.mobileide.terminal.presentation.TerminalScreen
import com.mobileide.ui.navigation.AppNavigation
import timber.log.Timber
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var terminalHeight by remember { mutableStateOf(200.dp) }
    val density = LocalDensity.current

    Scaffold(
        topBar = {
             TopAppBar(
                 title = { Text("MobileIDE") },
                 navigationIcon = {
                     IconButton(onClick = { /* TODO: Drawer öffnen */ Timber.i("Menu clicked") }) {
                         Icon(Icons.Filled.Menu, contentDescription = "Menu")
                     }
                 }
             )
        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(modifier = Modifier.weight(1f)) {
                SidePanelScreen(
                    onFileSelected = { filePath ->
                        val encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
                        navController.navigate("editor/$encodedPath") {
                            // Verhindert, dass der Editor-Screen mehrfach auf dem Backstack landet
                            launchSingleTop = true
                        }
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    AppNavigation(navController = navController, modifier = Modifier.fillMaxSize())
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            val newHeight = terminalHeight - (dragAmount / density.density).dp
                            if (newHeight > 50.dp) {
                                terminalHeight = newHeight
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Divider(modifier = Modifier.width(40.dp).padding(vertical = 8.dp))
            }

            Box(modifier = Modifier
                .fillMaxWidth()
                .height(terminalHeight)) {
                TerminalScreen()
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Explorer", "explorer", Icons.Default.Folder),
        BottomNavItem("Git", "git", Icons.Default.Code),
        BottomNavItem("Termux", "termux", Icons.Default.Terminal),
        BottomNavItem("Debug", "debug", Icons.Default.BugReport)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val title: String, val route: String, val icon: ImageVector)

