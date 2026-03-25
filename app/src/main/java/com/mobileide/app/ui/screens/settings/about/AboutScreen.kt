package com.mobileide.app.ui.screens.settings.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mobileide.app.ui.screens.settings.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(vm: IDEViewModel) {
    val context = LocalContext.current
    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = { SettingsTopBar("Über", onBack = { vm.navigate(Screen.SETTINGS) }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsSection("Entwickler")
            SettingsGroup {
                ListItem(
                    headlineContent = { Text("Thomas Schmid (scto)") },
                    supportingContent = { Text("GitHub-Profil anzeigen") },
                    leadingContent = {
                        Surface(shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Default.Person, null,
                                modifier = Modifier.padding(8.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }

            SettingsSection("Build-Informationen")
            SettingsGroup {
                SettingsItem("Version", "1.0")
                SettingsItem("Versionscode", "1")
                SettingsItem("Git-Commit-Hash", "—")
            }

            SettingsSection("Community")
            SettingsGroup {
                SettingsItem("GitHub-Sterne", "⭐ Auf GitHub einen Stern vergeben 😍")
                ListItem(
                    headlineContent = { Text("GitHub") },
                    supportingContent = { Text("Stern auf GitHub vergeben 😍") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                ListItem(
                    headlineContent = { Text("Telegram-Gruppe") },
                    supportingContent = { Text("Probleme, Funktionswünsche usw. melden") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                ListItem(
                    headlineContent = { Text("Discord Community") },
                    supportingContent = { Text("Probleme, Funktionswünsche usw. melden") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
    }
}
