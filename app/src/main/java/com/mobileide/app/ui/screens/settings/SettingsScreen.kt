package com.mobileide.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: IDEViewModel) {
    Scaffold(
        topBar = {
            SettingsTopBar(title = "Einstellungen", onBack = { vm.navigate(Screen.HOME) })
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

            // ── Editor selection ─────────────────────────────────────────────
            SettingsSection("Editor")
            SettingsGroup {
                SettingsNavItem(Icons.Default.Code,    "Editor auswählen",
                    "MobileIDE Editor oder Feature Editor",           { vm.navigate(Screen.SETTINGS_EDITOR_SELECT) })
                SettingsNavItem(Icons.Default.Tune,    "Editor",
                    "Aussehen, Verhalten, Schriftarten, Drawer",      { vm.navigate(Screen.SETTINGS_EDITOR) })
                SettingsNavItem(Icons.Default.Palette, "Themen",
                    "App-Farbthema, Material You, AMOLED",            { vm.navigate(Screen.SETTINGS_THEME) })
                SettingsNavItem(Icons.Default.Keyboard,"Tastenkombinationen",
                    "Hardware-Tastaturkürzel konfigurieren",           { vm.navigate(Screen.SETTINGS_KEYBINDS) })
            }

            // ── Code & Language ───────────────────────────────────────────────
            SettingsSection("Code & Sprache")
            SettingsGroup {
                SettingsNavItem(Icons.Default.Language, "Sprache",     "Sprach-Einstellungen",                       { vm.navigate(Screen.SETTINGS_LANGUAGE) })
                SettingsNavItem(Icons.Default.Hub,      "Sprachserver","LSP-Server installieren und konfigurieren",  { vm.navigate(Screen.SETTINGS_LSP) })
                SettingsNavItem(Icons.Default.PlayArrow,"Runner",      "Bash-Runner-Skripte verwalten",              { vm.navigate(Screen.SETTINGS_RUNNERS) })
            }

            // ── Tools ─────────────────────────────────────────────────────────
            SettingsSection("Tools")
            SettingsGroup {
                SettingsNavItem(Icons.Default.AccountTree,"Git",        "Zugangsdaten, Commits, Submodules",         { vm.navigate(Screen.SETTINGS_GIT) })
                SettingsNavItem(Icons.Default.Terminal,   "Terminal",   "Termux-Integration, Schriftgröße, Backup",  { vm.navigate(Screen.SETTINGS_TERMINAL) })
                SettingsNavItem(Icons.Default.Extension,  "Erweiterungen","Erweiterungen installieren und verwalten",{ vm.navigate(Screen.SETTINGS_EXTENSION) })
            }

            // ── App ───────────────────────────────────────────────────────────
            SettingsSection("App")
            SettingsGroup {
                SettingsNavItem(Icons.Default.BugReport, "Debug-Optionen","RAM, StrictMode, ANR, Logs",             { vm.navigate(Screen.SETTINGS_DEBUG) })
                SettingsNavItem(Icons.Default.Favorite,  "Unterstützung", "GitHub Sponsors · Buy Me a Coffee",       { vm.navigate(Screen.SETTINGS_SUPPORT) })
                SettingsNavItem(Icons.Default.Info,      "Über",          "Version, Entwickler, Community",           { vm.navigate(Screen.SETTINGS_ABOUT) })
            }
        }
    }
}
