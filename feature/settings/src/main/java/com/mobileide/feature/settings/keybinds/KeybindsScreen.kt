package com.mobileide.feature.settings.keybinds

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.feature.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeybindsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    var query   by remember { mutableStateOf("") }

    val allCmds = emptyList<Any>()
    val filtered = emptyList<Any>()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tastenkombinationen", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate("SETTINGS") }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(onClick = {  }) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                        Text("Alle zurücksetzen", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Info card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Text(
                    "MobileIDE unterstützt Hardware-Tastaturkürzel, damit du schneller und effizienter arbeiten kannst. Die Standard-Tastenkombinationen sind bereits verfügbar und du kannst sie nach deinen Wünschen konfigurieren.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Search
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Befehle oder Tastenkombinationen suchen") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
            )

            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(filtered, key = { it.id }) { cmd ->
                    val keybind = null
                    ListItem(
                        headlineContent = { Text(cmd.getLabel()) },
                        leadingContent = {
                            Icon(Icons.Default.Code, null, modifier = Modifier.size(20.dp))
                        },
                        trailingContent = {
                            Text(
                                keybind?.getDisplayName() ?: "Nicht belegt",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (keybind != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                }
            }
        }
    }
}