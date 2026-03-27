package com.mobileide.feature.settings.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.SettingsSection
import com.mobileide.feature.settings.SettingsTopBar

/**
 * Theme screen — purely declarative, no dependency on :app's theme globals.
 * The active theme name is passed in; changes are signalled via [onNavigate].
 *
 * Routing conventions:
 *   onNavigate("setTheme:<name>")   — request theme change
 *   onNavigate("setMonet:<true|false>") — toggle Monet/Material You
 *   onNavigate("setAmoled:<true|false>") — toggle AMOLED mode
 *   onNavigate("importTheme")       — launch theme file picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    activeThemeName: String = "Default",
    monetEnabled: Boolean = false,
    amoledEnabled: Boolean = false,
) {
    var monet  by remember { mutableStateOf(monetEnabled) }
    var amoled by remember { mutableStateOf(amoledEnabled) }

    // Built-in theme catalogue — no reference to app's theme system
    data class ThemeEntry(val id: String, val displayName: String, val isDark: Boolean)
    val themes = listOf(
        ThemeEntry("Default",          "MobileIDE Default",    true),
        ThemeEntry("Blueberry",        "Blueberry",            true),
        ThemeEntry("TokyoNight",       "Tokyo Night",          true),
        ThemeEntry("CatppuccinMocha",  "Catppuccin Mocha",     true),
        ThemeEntry("Dracula",          "Dracula",              true),
        ThemeEntry("OneDarkPro",       "One Dark Pro",         true),
        ThemeEntry("Nord",             "Nord",                 true),
        ThemeEntry("Gruvbox",          "Gruvbox Dark",         true),
        ThemeEntry("SolarizedLight",   "Solarized Light",      false),
        ThemeEntry("GithubLight",      "GitHub Light",         false),
    )

    Scaffold(
        topBar = { SettingsTopBar("Themen", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Material You ──────────────────────────────────────────────────
            item {
                SettingsSection("Material You")
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        monet = !monet
                        onNavigate("setMonet:$monet")
                    }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Material You / Monet", style = MaterialTheme.typography.bodyMedium)
                        Text("Farben von deinem Hintergrundbild ableiten",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(monet, { monet = it; onNavigate("setMonet:$it") })
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        amoled = !amoled
                        onNavigate("setAmoled:$amoled")
                    }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AMOLED-Schwarz", style = MaterialTheme.typography.bodyMedium)
                        Text("Reines Schwarz für OLED-Displays",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(amoled, { amoled = it; onNavigate("setAmoled:$it") })
                }
            }

            // ── Themes ────────────────────────────────────────────────────────
            item {
                SettingsSection("Eingebaute Themen")
            }

            items(themes) { theme ->
                ListItem(
                    headlineContent = { Text(theme.displayName) },
                    supportingContent = {
                        Text(if (theme.isDark) "Dunkel" else "Hell",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingContent = {
                        Icon(
                            if (theme.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (activeThemeName == theme.id) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        if (activeThemeName == theme.id)
                            Icon(Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { onNavigate("setTheme:${theme.id}") },
                )
                HorizontalDivider()
            }

            // ── Import ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSection("Thema importieren")
                OutlinedButton(
                    onClick = { onNavigate("importTheme") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("JSON-Thema importieren")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
