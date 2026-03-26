package com.mobileide.feature.settings.language

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    Scaffold(
        topBar = { SettingsTopBar("Sprache", onBack = { onNavigate("SETTINGS") }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsSection("App-Sprache")
            SettingsGroup {
                SettingsItem("Systemstandard", "Folgt der Systemsprache")
            }
        }
    }
}