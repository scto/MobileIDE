package com.mobileide.feature.settings.runners

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnersScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    var htmlPreview      by remember { mutableStateOf(true) }
    var markdownPreview  by remember { mutableStateOf(true) }
    var universalRunner  by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { SettingsTopBar("Runner", onBack = { onNavigate("SETTINGS") }) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text("Runner sind kleine Bash-Skripte, die verwendet werden, um eine Datei oder ein Projekt auszuführen. Erstelle einen Runner und klicke darauf, um ihn im Editor zu öffnen.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }

            SettingsSection("Integriert")
            SettingsGroup {
                SettingsToggle("HTML-Vorschau",
                    "Einen lokalen HTTP-Server starten, um HTML-Dateien direkt in der App anzuzeigen",
                    htmlPreview) { htmlPreview = it }
                SettingsToggle("Markdown-Vorschau",
                    "Markdown-Dateien direkt in der App anzeigen",
                    markdownPreview) { markdownPreview = it }
                SettingsToggle("Universeller Runner",
                    "Dateityp automatisch erkennen und Skripte in verschiedenen Sprachen kompilieren/ausführen",
                    universalRunner) { universalRunner = it }
            }

            SettingsSection("Extern")
            SettingsGroup {
                SettingsItem("Keine Runner", "Noch keine externen Runner erstellt")
            }
        }
    }
}