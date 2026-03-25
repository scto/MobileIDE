package com.mobileide.app.ui.screens.settings.git

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.app.ui.screens.settings.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitSettingsScreen(vm: IDEViewModel) {
    var colorFileNames  by remember { mutableStateOf(true) }
    var submodules      by remember { mutableStateOf(true) }
    var recursiveSubs   by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { SettingsTopBar("Git", onBack = { vm.navigate(Screen.SETTINGS) }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsSection("Allgemein")
            SettingsGroup {
                SettingsToggle("Dateinamen einfärben",
                    "Dateinamen je nach Git-Status farbig darstellen",
                    colorFileNames) { colorFileNames = it }
            }

            SettingsSection("Konto")
            SettingsGroup {
                SettingsItem("Zugangsdaten", "Zugangsdaten für den Zugriff auf Remote-Repositories")
                SettingsItem("Benutzerdaten", "Benutzerdaten, die in deinen Git-Commits erscheinen werden")
            }

            SettingsSection("Repository")
            SettingsGroup {
                SettingsToggle("Submodules",
                    "Submodules beim Klonen eines Repositories einbeziehen",
                    submodules) { submodules = it }
                SettingsToggle("Rekursive Submodules",
                    "Submodules und deren Abhängigkeiten rekursiv fetchen",
                    recursiveSubs) { recursiveSubs = it }
            }
        }
    }
}
