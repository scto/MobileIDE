package com.mobileide.feature.settings.keybinds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.SettingsTopBar

/**
 * Keybinds screen — shows keyboard shortcuts.
 * Command data is passed in by the app layer to keep this module app-independent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeybindsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    var query by remember { mutableStateOf("") }

    // Built-in shortcuts list (no dependency on CommandRegistry from :app)
    val shortcuts = remember {
        listOf(
            "Ctrl+S"       to "Speichern",
            "Ctrl+Z"       to "Rückgängig",
            "Ctrl+Y"       to "Wiederholen",
            "Ctrl+F"       to "Suchen",
            "Ctrl+H"       to "Ersetzen",
            "Ctrl+G"       to "Gehe zu Zeile",
            "Ctrl+W"       to "Tab schließen",
            "Ctrl+Tab"     to "Nächster Tab",
            "Ctrl+/"       to "Zeile auskommentieren",
            "Ctrl+D"       to "Zeile duplizieren",
            "Ctrl+L"       to "Zeile löschen",
            "Ctrl+A"       to "Alles auswählen",
            "Ctrl+C"       to "Kopieren",
            "Ctrl+X"       to "Ausschneiden",
            "Ctrl+V"       to "Einfügen",
            "Ctrl+Shift+F" to "Code formatieren",
            "Ctrl+Shift+S" to "Alle speichern",
            "Ctrl+B"       to "Build",
            "Ctrl+R"       to "Ausführen",
            "Ctrl+P"       to "Befehlspalette",
            "Ctrl+,"       to "Einstellungen",
            "Ctrl+`"       to "Terminal",
            "Alt+Left"     to "Zurück navigieren",
            "Alt+Right"    to "Vorwärts navigieren",
        )
    }

    val filtered = if (query.isBlank()) shortcuts
    else shortcuts.filter { (k, v) ->
        k.contains(query, ignoreCase = true) || v.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = { SettingsTopBar("Tastenkombinationen", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Suchen…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { (key, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = {
                            Surface(
                                color  = MaterialTheme.colorScheme.secondaryContainer,
                                shape  = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    key,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
