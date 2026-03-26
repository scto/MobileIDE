package com.mobileide.feature.settings.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    fun openUrl(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Scaffold(
        topBar = { SettingsTopBar("Unterstützung", onBack = { onNavigate("SETTINGS") }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsGroup {
                ListItem(
                    headlineContent = { Text("GitHub Sponsors") },
                    supportingContent = { Text("Werde ein Sponsor und hilf mit, die Zukunft dieses Projekts zu gestalten") },
                    leadingContent = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                ListItem(
                    headlineContent = { Text("Buy Me a Coffee") },
                    supportingContent = { Text("Zeig etwas Liebe mit einem Kaffee – jede Tasse hilft! ☕") },
                    leadingContent = { Icon(Icons.Default.LocalCafe, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
    }
}