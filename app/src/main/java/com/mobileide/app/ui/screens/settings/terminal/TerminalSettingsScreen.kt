package com.mobileide.app.ui.screens.settings.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mobileide.app.ui.screens.settings.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSettingsScreen(vm: IDEViewModel) {
    var failsafe       by remember { mutableStateOf(true) }
    var textSize       by remember { mutableStateOf(18f) }
    var seccomp        by remember { mutableStateOf(true) }
    var killSessions   by remember { mutableStateOf(false) }
    var projectAsHome  by remember { mutableStateOf(true) }
    var shareHomeDir   by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsTopBar("Terminal", onBack = { vm.navigate(Screen.SETTINGS) }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsGroup {
                SettingsToggle("Failsafe-Modus", "Terminal im Wartungsmodus starten", failsafe) { failsafe = it }
            }

            SettingsGroup {
                // Text size slider
                ListItem(
                    headlineContent = {
                        Row {
                            Text("Textgröße", modifier = Modifier.weight(1f))
                            Text(textSize.toInt().toString(),
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    supportingContent = {
                        Slider(value = textSize, onValueChange = { textSize = it },
                            valueRange = 8f..32f, steps = 23)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                SettingsToggle("SECCOMP",
                    "Fehler 'Funktion nicht implementiert' auf einigen Geräten beheben",
                    seccomp) { seccomp = it }
                SettingsItem("Sicherung", "Terminal-Backup")
                SettingsItem("Wiederherstellen", "Terminal-Backup wiederherstellen")
                SettingsItem("Deinstallieren", "Terminal deinstallieren")
            }

            SettingsGroup {
                SettingsToggle("Alle Sitzungen beenden",
                    "Alle Terminal-Sitzungen beim Schließen der App beenden",
                    killSessions) { killSessions = it }
                SettingsToggle("Projekt als Arbeitsverzeichnis verwenden",
                    "Projekt als Arbeitsverzeichnis im Terminal verwenden",
                    projectAsHome) { projectAsHome = it }
                SettingsToggle("Home-Verzeichnis freigeben",
                    "Home-Verzeichnis über die System-Dateiauswahl (SAF) für externe Apps zugänglich machen",
                    shareHomeDir) { shareHomeDir = it }
            }
        }
    }
}
