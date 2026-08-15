package com.scto.mobile.ide.features.pluginstore.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scto.mobile.ide.features.pluginstore.model.PluginStatus
import com.scto.mobile.ide.features.pluginstore.model.PluginType
import com.scto.mobile.ide.features.pluginstore.model.StorePluginItem
import com.scto.mobile.ide.features.pluginstore.viewmodel.FilterTab
import com.scto.mobile.ide.features.pluginstore.viewmodel.PluginStoreUiState
import com.scto.mobile.ide.features.pluginstore.viewmodel.PluginStoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginStoreScreen(
    onBackPress: () -> Unit = {},
    viewModel: PluginStoreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Plugin & Extension Store",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Erweitere IDE & Language Server dynamisch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPlugins(isRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Filter Row
            SearchBarAndFilters(
                uiState = uiState,
                onSearchQueryChange = viewModel::setSearchQuery,
                onTabSelect = viewModel::setSelectedTab
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredPlugins.isEmpty()) {
                EmptyStoreState(
                    uiState = uiState,
                    onRefresh = { viewModel.loadPlugins(isRefresh = true) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredPlugins, key = { it.id }) { plugin ->
                        PluginItemCard(
                            plugin = plugin,
                            onInstall = { viewModel.installPlugin(plugin) },
                            onUpdate = { viewModel.updatePlugin(plugin) },
                            onUninstall = { viewModel.uninstallPlugin(plugin) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBarAndFilters(
    uiState: PluginStoreUiState,
    onSearchQueryChange: (String) -> Unit,
    onTabSelect: (FilterTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Plugins, LSP Server, Tags suchen...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Löschen")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterTabChip(
                    label = "Alle (${uiState.plugins.size})",
                    selected = uiState.selectedTab == FilterTab.ALL,
                    onClick = { onTabSelect(FilterTab.ALL) }
                )
            }
            item {
                FilterTabChip(
                    label = "LSP Server",
                    selected = uiState.selectedTab == FilterTab.LSP,
                    onClick = { onTabSelect(FilterTab.LSP) }
                )
            }
            item {
                FilterTabChip(
                    label = "Themes",
                    selected = uiState.selectedTab == FilterTab.THEMES,
                    onClick = { onTabSelect(FilterTab.THEMES) }
                )
            }
            item {
                val installedCount = uiState.plugins.count {
                    it.status == PluginStatus.INSTALLED || it.status == PluginStatus.UPDATE_AVAILABLE
                }
                FilterTabChip(
                    label = "Installiert ($installedCount)",
                    selected = uiState.selectedTab == FilterTab.INSTALLED,
                    onClick = { onTabSelect(FilterTab.INSTALLED) }
                )
            }
        }
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PluginItemCard(
    plugin: StorePluginItem,
    onInstall: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plugin Type Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = when (plugin.type) {
                                    PluginType.LSP -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                    PluginType.THEME -> listOf(Color(0xFFEC4899), Color(0xFFBE185D))
                                    PluginType.FORMATTER -> listOf(Color(0xFF10B981), Color(0xFF047857))
                                    else -> listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (plugin.type) {
                            PluginType.LSP -> Icons.Default.Code
                            PluginType.THEME -> Icons.Default.Palette
                            PluginType.FORMATTER -> Icons.Default.Build
                            else -> Icons.Default.Extension
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "v${plugin.version} • ${plugin.author.displayName.ifEmpty { "Community" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(plugin = plugin)
            }

            if (plugin.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (plugin.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    plugin.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = plugin.status == PluginStatus.DOWNLOADING) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    LinearProgressIndicator(
                        progress = { plugin.downloadProgress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Wird heruntergeladen & entpackt... ${(plugin.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (plugin.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Fehler: ${plugin.errorMessage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plugin.sizeFormatted.isNotEmpty()) {
                    Text(
                        text = plugin.sizeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                when (plugin.status) {
                    PluginStatus.NOT_INSTALLED -> {
                        Button(
                            onClick = onInstall,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Installieren")
                        }
                    }

                    PluginStatus.UPDATE_AVAILABLE -> {
                        Button(
                            onClick = onUpdate,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aktualisieren")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onUninstall,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Entfernen")
                        }
                    }

                    PluginStatus.INSTALLED -> {
                        OutlinedButton(
                            onClick = onUninstall,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deinstallieren")
                        }
                    }

                    PluginStatus.DOWNLOADING -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }

                    PluginStatus.ERROR -> {
                        Button(
                            onClick = onInstall,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Erneut versuchen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(plugin: StorePluginItem) {
    val (text, color) = when (plugin.status) {
        PluginStatus.INSTALLED -> "Installiert" to Color(0xFF10B981)
        PluginStatus.UPDATE_AVAILABLE -> "Update v${plugin.version}" to Color(0xFFF59E0B)
        PluginStatus.DOWNLOADING -> "Download..." to Color(0xFF3B82F6)
        PluginStatus.ERROR -> "Fehler" to MaterialTheme.colorScheme.error
        PluginStatus.NOT_INSTALLED -> "" to Color.Transparent
    }

    if (text.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun EmptyStoreState(
    uiState: PluginStoreUiState,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (uiState.searchQuery.isNotEmpty()) "Keine Plugins gefunden" else "Keine Plugins verfügbar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.errorMessage ?: "Versuche die Suche zu ändern oder den Katalog zu aktualisieren.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRefresh) {
                Text("Katalog neu laden")
            }
        }
    }
}
