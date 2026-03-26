package com.mobileide.feature.settings.lsp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LspScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val servers = listOf("bash", "css", "html", "json", "markdown", "python", "typescript", "xml")
    Scaffold(
        topBar = { SettingsTopBar("Sprachserver", onBack = { onNavigate("SETTINGS_EDITOR") }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text("Sprachserver ermöglichen Funktionen wie Code-Vervollständigung, Gehe zu Definition und Umbenennen. Installiere sie über das Terminal.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            SettingsSection("Verfügbare Server")
            SettingsGroup {
                servers.forEach { name ->
                    SettingsNavItem(Icons.Default.Code, name.replaceFirstChar { it.uppercase() },
                        "assets/terminal/lsp/$name.sh") { }
                }
            }
        }
    }
}