package com.scto.mobile.ide.ui.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.lsp.ExternalLspServer
import com.rk.lsp.LspRegistry
import com.rk.lsp.LspServer
import com.scto.mobile.ide.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LspSettingsUiItem(
    val server: LspServer,
    val isInstalled: Boolean,
    val isUpdatable: Boolean,
    val isEnabled: Boolean,
)

@Composable
fun LspLogoBadge(languageName: String) {
    val (text, color) =
        when (languageName.lowercase()) {
            "html" -> "HTML" to Color(0xFFE44D26)
            "css" -> "CSS" to Color(0xFF264DE4)
            "typescript",
            "javascript",
            "js",
            "ts" -> "TS" to Color(0xFF3178C6)
            "emmet" -> "EM" to Color(0xFF7E57C2)
            "bash",
            "shell",
            "sh" -> "SH" to Color(0xFF4CAF50)
            "xml" -> "XML" to Color(0xFF8D6E63)
            "java" -> "JAVA" to Color(0xFF5382A1)
            "kotlin" -> "KT" to Color(0xFF7F52FF)
            "c++",
            "cpp",
            "c" -> "C++" to Color(0xFF00599C)
            "python" -> "PY" to Color(0xFF3776AB)
            "json" -> "JSON" to Color(0xFF008080)
            "yaml",
            "yml" -> "YML" to Color(0xFF78909C)
            "toml" -> "TOML" to Color(0xFF8D6E63)
            else -> "LSP" to MaterialTheme.colorScheme.primary
        }

    Box(
        modifier = Modifier.size(42.dp).background(color.copy(alpha = 0.15f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LspSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var lspItems by remember { mutableStateOf<List<LspSettingsUiItem>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val lspSettingsPrefs = remember { context.getSharedPreferences("MobileIDE_Lsp_Settings", Context.MODE_PRIVATE) }

    LaunchedEffect(refreshTrigger) {
        withContext(Dispatchers.IO) {
            val allServers = LspRegistry.extensionServers + LspRegistry.externalServers
            val items =
                allServers
                    .map { server ->
                        val isInstalled = server.isInstalled(context)
                        val isUpdatable = server.isUpdatable(context)
                        val isEnabled = lspSettingsPrefs.getBoolean("lsp_enabled_${server.id}", true)
                        LspSettingsUiItem(
                            server = server,
                            isInstalled = isInstalled,
                            isUpdatable = isUpdatable,
                            isEnabled = isEnabled,
                        )
                    }
                    .sortedBy { it.server.serverName }

            withContext(Dispatchers.Main) { lspItems = items }
        }
    }

    val integratedIds = setOf("html_lsp", "emmet_lsp", "css_lsp", "typescript_lsp", "bash_lsp", "xml_lsp")
    val integratedServers = lspItems.filter { it.server.id in integratedIds }
    val extensionServers = lspItems.filter { it.server.id !in integratedIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sprachserver verwalten", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("+ Externer LSP") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Info Card at the top
            item {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text =
                                "Sprachserver sind separate Prozesse, die intelligente Funktionen, wie Code-Vervollständigung, Fehlerhervorhebung und Inline-Dokumentation bereitstellen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Integrated Header
            if (integratedServers.isNotEmpty()) {
                item {
                    Text(
                        text = "Integriert",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            integratedServers.forEachIndexed { index, item ->
                                LspServerRow(
                                    item = item,
                                    activity = activity,
                                    onToggleEnable = { enabled ->
                                        lspSettingsPrefs
                                            .edit()
                                            .putBoolean("lsp_enabled_${item.server.id}", enabled)
                                            .apply()
                                        refreshTrigger++
                                    },
                                    onRefresh = { refreshTrigger++ },
                                )
                                if (index < integratedServers.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Extensions Header
            if (extensionServers.isNotEmpty()) {
                item {
                    Text(
                        text = "Erweiterungen",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            extensionServers.forEachIndexed { index, item ->
                                LspServerRow(
                                    item = item,
                                    activity = activity,
                                    onToggleEnable = { enabled ->
                                        lspSettingsPrefs
                                            .edit()
                                            .putBoolean("lsp_enabled_${item.server.id}", enabled)
                                            .apply()
                                        refreshTrigger++
                                    },
                                    onRefresh = { refreshTrigger++ },
                                )
                                if (index < extensionServers.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExternalLspDialog(
            onDismiss = { showAddDialog = false },
            onSave = { server ->
                LspRegistry.addExternalServer(context, server)
                showAddDialog = false
                refreshTrigger++
            },
        )
    }
}

@Composable
fun LspServerRow(
    item: LspSettingsUiItem,
    activity: Activity?,
    onToggleEnable: (Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LspLogoBadge(languageName = item.server.languageName)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.server.languageName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val binaryText =
                when (item.server.id) {
                    "xml_lsp" -> "lemminx"
                    "html_lsp" -> "vscode-html-language-server"
                    "css_lsp" -> "vscode-css-language-server"
                    "emmet_lsp" -> "emmet-language-server"
                    "typescript_lsp" -> "typescript-language-server"
                    else -> item.server.serverName
                }
            Text(
                text = binaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (item.isInstalled) "Installiert" else "Nicht installiert",
                style = MaterialTheme.typography.labelSmall,
                color = if (item.isInstalled) Color(0xFF4CAF50) else Color(0xFFF44336),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (activity != null) {
                if (!item.isInstalled) {
                    IconButton(
                        onClick = {
                            item.server.install(activity)
                            onRefresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Installieren",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    if (item.isUpdatable) {
                        IconButton(
                            onClick = {
                                item.server.update(activity)
                                onRefresh()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Aktualisieren",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    if (item.server.canBeUninstalled && item.server !is ExternalLspServer) {
                        IconButton(
                            onClick = {
                                item.server.uninstall(activity)
                                onRefresh()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Deinstallieren",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (item.server is ExternalLspServer) {
                        IconButton(
                            onClick = {
                                LspRegistry.removeExternalServer(activity, item.server)
                                onRefresh()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Deinstallieren",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Switch(checked = item.isEnabled, onCheckedChange = onToggleEnable, enabled = item.isInstalled)
        }
    }
}

@Composable
fun AddExternalLspDialog(onDismiss: () -> Unit, onSave: (LspServer) -> Unit) {
    var serverName by remember { mutableStateOf("") }
    var languageName by remember { mutableStateOf("") }
    var extensions by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Externer LSP hinzufügen", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Server Name") },
                    placeholder = { Text("e.g. rust-analyzer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = languageName,
                    onValueChange = { languageName = it },
                    label = { Text("Sprache") },
                    placeholder = { Text("e.g. Rust") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = extensions,
                    onValueChange = { extensions = it },
                    label = { Text("Dateiendungen (Komma-separiert)") },
                    placeholder = { Text("e.g. rs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Befehl (z.B. rust-analyzer)") },
                    placeholder = { Text("e.g. rust-analyzer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (serverName.isNotBlank() && languageName.isNotBlank() && command.isNotBlank()) {
                        val parsedExts = extensions.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                        val cmdList = command.split(" ").filter { it.isNotEmpty() }
                        val serverId = "external_" + serverName.replace(" ", "_").lowercase()
                        val newServer =
                            ExternalLspServer(
                                id = serverId,
                                languageName = languageName,
                                serverName = serverName,
                                supportedExtensions = parsedExts,
                                command = cmdList,
                            )
                        onSave(newServer)
                    }
                },
                enabled = serverName.isNotBlank() && languageName.isNotBlank() && command.isNotBlank(),
            ) {
                Text("Speichern")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
