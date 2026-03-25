package com.mobileide.app.ui.screens.settings.extension

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.app.ui.screens.settings.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionScreen(vm: IDEViewModel) {
    Scaffold(
        topBar = { SettingsTopBar("Erweiterungen", onBack = { vm.navigate(Screen.SETTINGS) }) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Aus Speicher installieren") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { /* TODO: file picker */ },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Info card
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text("Erweiterungen können verwendet werden, um das Verhalten und die Funktionen von MobileIDE zu verändern. Um zu erfahren, wie man eine Erweiterung erstellt, klicke hier.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }

            SettingsGroup {
                ListItem(
                    headlineContent = { Text("Keine Erweiterungen installiert.") },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
    }
}
