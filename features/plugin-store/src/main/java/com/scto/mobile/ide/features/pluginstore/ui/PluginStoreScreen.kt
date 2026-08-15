package com.scto.mobile.ide.features.pluginstore.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scto.mobile.ide.features.pluginstore.model.PluginStatus
import com.scto.mobile.ide.features.pluginstore.model.PluginType
import com.scto.mobile.ide.features.pluginstore.model.StorePluginItem
import com.scto.mobile.ide.features.pluginstore.viewmodel.CategoryFilter
import com.scto.mobile.ide.features.pluginstore.viewmodel.MainTab
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
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = uiState.mainTab == MainTab.DISCOVER,
                    onClick = { viewModel.setMainTab(MainTab.DISCOVER) },
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    label = { Text("Entdecken") }
                )
                NavigationBarItem(
                    selected = uiState.mainTab == MainTab.INSTALLED,
                    onClick = { viewModel.setMainTab(MainTab.INSTALLED) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.installedPlugins.isNotEmpty()) {
                                    Badge { Text("${uiState.installedPlugins.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.DownloadDone, contentDescription = null)
                        }
                    },
                    label = { Text("Installiert") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Sub-Filters
            SearchBarAndFilters(
                uiState = uiState,
                onSearchQueryChange = viewModel::setSearchQuery,
                onCategorySelect = viewModel::setSelectedCategory
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.mainTab) {
                    MainTab.DISCOVER -> DiscoverTabContent(
                        uiState = uiState,
                        onPluginClick = { viewModel.selectDetailPlugin(it) },
                        onInstall = { viewModel.installPlugin(it) },
                        onUpdate = { viewModel.updatePlugin(it) },
                        onUninstall = { viewModel.uninstallPlugin(it) },
                        onRefresh = { viewModel.loadPlugins(isRefresh = true) }
                    )

                    MainTab.INSTALLED -> InstalledTabContent(
                        uiState = uiState,
                        onPluginClick = { viewModel.selectDetailPlugin(it) },
                        onUpdate = { viewModel.updatePlugin(it) },
                        onUninstall = { viewModel.uninstallPlugin(it) },
                        onToggleEnable = { viewModel.togglePluginEnabled(it) },
                        onRefresh = { viewModel.loadPlugins(isRefresh = true) }
                    )
                }
            }
        }
    }

    // Detail Dialog
    uiState.selectedDetailPlugin?.let { detailPlugin ->
        PluginDetailDialog(
            plugin = detailPlugin,
            onDismiss = { viewModel.selectDetailPlugin(null) },
            onInstall = { viewModel.installPlugin(detailPlugin) },
            onUpdate = { viewModel.updatePlugin(detailPlugin) },
            onUninstall = { viewModel.uninstallPlugin(detailPlugin) },
            onToggleEnable = { viewModel.togglePluginEnabled(detailPlugin) }
        )
    }
}

@Composable
private fun SearchBarAndFilters(
    uiState: PluginStoreUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (CategoryFilter) -> Unit
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
            placeholder = {
                Text(
                    if (uiState.mainTab == MainTab.DISCOVER) "Plugins, Sprachen, Themes suchen..."
                    else "Installierte Plugins durchsuchen..."
                )
            },
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

        if (uiState.mainTab == MainTab.DISCOVER) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == CategoryFilter.ALL,
                        onClick = { onCategorySelect(CategoryFilter.ALL) },
                        label = { Text("Alle (${uiState.plugins.size})") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == CategoryFilter.LANGUAGES,
                        onClick = { onCategorySelect(CategoryFilter.LANGUAGES) },
                        label = { Text("Sprachen & LSP") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == CategoryFilter.THEMES,
                        onClick = { onCategorySelect(CategoryFilter.THEMES) },
                        label = { Text("Themes") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == CategoryFilter.FORMATTERS,
                        onClick = { onCategorySelect(CategoryFilter.FORMATTERS) },
                        label = { Text("Formatter") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == CategoryFilter.TOOLS,
                        onClick = { onCategorySelect(CategoryFilter.TOOLS) },
                        label = { Text("Tools & Debugger") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverTabContent(
    uiState: PluginStoreUiState,
    onPluginClick: (StorePluginItem) -> Unit,
    onInstall: (StorePluginItem) -> Unit,
    onUpdate: (StorePluginItem) -> Unit,
    onUninstall: (StorePluginItem) -> Unit,
    onRefresh: () -> Unit
) {
    if (uiState.discoverPlugins.isEmpty()) {
        EmptyStoreState(uiState = uiState, onRefresh = onRefresh)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.groupedDiscoverPlugins.forEach { (categoryHeader, itemsInGroup) ->
                item(key = categoryHeader) {
                    Text(
                        text = categoryHeader,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(itemsInGroup, key = { it.id }) { plugin ->
                    PluginItemCard(
                        plugin = plugin,
                        onPluginClick = { onPluginClick(plugin) },
                        onInstall = { onInstall(plugin) },
                        onUpdate = { onUpdate(plugin) },
                        onUninstall = { onUninstall(plugin) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledTabContent(
    uiState: PluginStoreUiState,
    onPluginClick: (StorePluginItem) -> Unit,
    onUpdate: (StorePluginItem) -> Unit,
    onUninstall: (StorePluginItem) -> Unit,
    onToggleEnable: (StorePluginItem) -> Unit,
    onRefresh: () -> Unit
) {
    val installedList = uiState.installedPlugins

    if (installedList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Keine Erweiterungen installiert",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Wechsele zum Tab 'Entdecken', um neue LSP-Server & Themes zu installieren.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(installedList, key = { it.id }) { plugin ->
                InstalledPluginCard(
                    plugin = plugin,
                    onPluginClick = { onPluginClick(plugin) },
                    onUpdate = { onUpdate(plugin) },
                    onUninstall = { onUninstall(plugin) },
                    onToggleEnable = { onToggleEnable(plugin) }
                )
            }
        }
    }
}

@Composable
private fun InstalledPluginCard(
    plugin: StorePluginItem,
    onPluginClick: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit,
    onToggleEnable: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPluginClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        text = "Installiert: v${plugin.installedVersion ?: plugin.version} • ${if (plugin.isEnabled) "Aktiviert" else "Deaktiviert"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (plugin.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = { onToggleEnable() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plugin.hasUpdate) {
                    Button(
                        onClick = onUpdate,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update auf v${plugin.version}")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

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
        }
    }
}

@Composable
private fun PluginItemCard(
    plugin: StorePluginItem,
    onPluginClick: () -> Unit,
    onInstall: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPluginClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            AnimatedVisibility(visible = plugin.status == PluginStatus.DOWNLOADING) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    LinearProgressIndicator(
                        progress = { plugin.downloadProgress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Wird heruntergeladen... ${(plugin.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (plugin.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plugin.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        Button(onClick = onInstall, shape = RoundedCornerShape(10.dp)) {
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
    if (!plugin.isCompatibleWithDevice) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        ) {
            Text(
                text = "Nicht kompatibel mit deinem Gerät",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        return
    }

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
private fun PluginDetailDialog(
    plugin: StorePluginItem,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit,
    onToggleEnable: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
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
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plugin.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "ID: ${plugin.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                DetailInfoRow(label = "Version", value = "v${plugin.version}")
                DetailInfoRow(label = "Autor", value = plugin.author.displayName.ifEmpty { "Community Maintainer" })
                if (plugin.sizeFormatted.isNotEmpty()) {
                    DetailInfoRow(label = "Größe", value = plugin.sizeFormatted)
                }
                DetailInfoRow(label = "Kategorie", value = plugin.type.name)

                if (plugin.dependencies.isNotEmpty()) {
                    DetailInfoRow(label = "Laufzeit-Abhängigkeiten", value = plugin.dependencies.joinToString(", "))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Beschreibung",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plugin.description.ifEmpty { "Keine Beschreibung verfügbar." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (plugin.status == PluginStatus.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { plugin.downloadProgress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Fortschritt: ${(plugin.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    when (plugin.status) {
                        PluginStatus.NOT_INSTALLED -> {
                            Button(
                                onClick = {
                                    onInstall()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Installieren")
                            }
                        }

                        PluginStatus.UPDATE_AVAILABLE -> {
                            Button(
                                onClick = {
                                    onUpdate()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aktualisieren")
                            }
                        }

                        PluginStatus.INSTALLED -> {
                            OutlinedButton(
                                onClick = {
                                    onUninstall()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deinstallieren")
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
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
                text = uiState.errorMessage ?: "Versuche die Suche zu ändern oder den Katalog neu zu laden.",
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
