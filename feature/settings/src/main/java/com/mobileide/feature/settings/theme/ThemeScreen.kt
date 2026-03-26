package com.mobileide.feature.settings.theme

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val context    = LocalContext.current
    val monet = false
    val amoled = false
    val current: Any? = null
    val allThemes = emptyList<Any>()

    // File picker for installing a theme from storage
    val themePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            kotlinx.coroutines.MainScope().launch {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val tmp = java.io.File(context.cacheDir, "theme_import.json")
                    context.contentResolver.openInputStream(u)?.use { it.copyTo(tmp.outputStream()) }
                    com.mobileide.app.ui.theme.installM3ThemeFromFile(context, tmp)
                    com.mobileide.app.ui.theme.updateM3Themes(context)
                    tmp.delete()
                }
            }
        }
    }

    Scaffold(
        topBar = { SettingsTopBar("Themen", onBack = { onNavigate("SETTINGS") }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Theme Settings ────────────────────────────────────────────────
            SettingsSection("Themeneinstellungen")
            SettingsGroup {
                SettingsItem("Thema-Modus", "App-Farbthema ändern")
                SettingsToggle("Schwarzes Thema", "Wendet ein rein schwarzes Thema an",
                    amoled) { /* setAmoled */ }
                SettingsToggle("Dynamische Farben", "Gerätethema in App verwenden",
                    monet) { /* setMonet */ }
            }

            // ── Theme List ────────────────────────────────────────────────────
            SettingsSection("Themen")
            SettingsGroup {
                allThemes.forEachIndexed { idx, theme ->
                    val isSelected = false
                    ListItem(
                        headlineContent = {
                            Text(theme.name, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp).clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    )
                            )
                        },
                        trailingContent = {
                            RadioButton(selected = isSelected,
                                onClick = { /* setM3Theme */ })
                        },
                        modifier = Modifier.clickable { /* setM3Theme */ },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    )
                    if (idx < allThemes.lastIndex)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                }

                // Add theme button
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                ListItem(
                    headlineContent = { Text("Thema hinzufügen") },
                    leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { themePicker.launch("application/json") },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }

            // ── Icon Packs ────────────────────────────────────────────────────
            SettingsSection("Icon-Pakete")
            SettingsGroup {
                ListItem(
                    headlineContent = { Text("Simple Icons (Standard)") },
                    trailingContent = { RadioButton(selected = true, onClick = {}) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                ListItem(
                    headlineContent = { Text("Icon-Paket hinzufügen") },
                    leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
    }
}