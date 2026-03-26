package com.mobileide.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    Scaffold(
        topBar = {
            SettingsTopBar(title = "Einstellungen", onBack = { onBack() })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Editor ────────────────────────────────────────────────────────
            SettingsSection("Editor")
            SettingsGroup {
                SettingsNavItem(Icons.Default.Code,   "Editor",  "Aussehen, Verhalten, Schriftarten, Drawer",       { onNavigate("SETTINGS_EDITOR") })
                SettingsNavItem(Icons.Default.Palette,"Themen",  "App-Farbthema, Material You, AMOLED",              { onNavigate("SETTINGS_THEME") })
                SettingsNavItem(Icons.Default.Keyboard,"Tastenkombinationen", "Hardware-Tastaturkürzel konfigurieren", { onNavigate("SETTINGS_KEYBINDS") })
            }

            // ── Code & Language ───────────────────────────────────────────────
            SettingsSection("Code & Sprache")
            SettingsGroup {
                SettingsNavItem(Icons.Default.Language, "Sprache",     "Sprach-Einstellungen",                       { onNavigate("SETTINGS_LANGUAGE") })
                SettingsNavItem(Icons.Default.Hub,      "Sprachserver","LSP-Server installieren und konfigurieren",  { onNavigate("SETTINGS_LSP") })
                SettingsNavItem(Icons.Default.PlayArrow,"Runner",      "Bash-Runner-Skripte verwalten",              { onNavigate("SETTINGS_RUNNERS") })
            }

            // ── Tools ─────────────────────────────────────────────────────────
            SettingsSection("Tools")
            SettingsGroup {
                SettingsNavItem(Icons.Default.AccountTree,"Git",        "Zugangsdaten, Commits, Submodules",         { onNavigate("SETTINGS_GIT") })
                SettingsNavItem(Icons.Default.Terminal,   "Terminal",   "Termux-Integration, Schriftgröße, Backup",  { onNavigate("SETTINGS_TERMINAL") })
                SettingsNavItem(Icons.Default.Extension,  "Erweiterungen","Erweiterungen installieren und verwalten",{ onNavigate("SETTINGS_EXTENSION") })
            }

            // ── App ───────────────────────────────────────────────────────────
            SettingsSection("App")
            SettingsGroup {
                SettingsNavItem(Icons.Default.BugReport, "Debug-Optionen","RAM, StrictMode, ANR, Logs",             { onNavigate("SETTINGS_DEBUG") })
                SettingsNavItem(Icons.Default.Favorite,  "Unterstützung", "GitHub Sponsors · Buy Me a Coffee",       { onNavigate("SETTINGS_SUPPORT") })
                SettingsNavItem(Icons.Default.Info,      "Über",          "Version, Entwickler, Community",           { onNavigate("SETTINGS_ABOUT") })
            }
        }
    }
}