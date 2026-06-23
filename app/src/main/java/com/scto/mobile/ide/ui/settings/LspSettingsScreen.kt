package com.scto.mobile.ide.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
import com.scto.mobile.ide.R
import com.scto.mobile.ide.ui.terminal.SessionManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LspItem(val id: String, val name: String, val scriptName: String, var isInstalled: Boolean = false)

fun getLspBinaryPaths(id: String): List<String> {
    return when (id) {
        "bash" -> listOf("usr/bin/bash-language-server")
        "css" -> listOf("usr/bin/css-languageserver")
        "emmet" -> listOf("usr/bin/emmet-language-server")
        "eslint" -> listOf("usr/bin/vscode-eslint-language-server")
        "html" -> listOf("usr/bin/html-languageserver")
        "json" -> listOf("usr/bin/json-languageserver")
        "markdown" -> listOf("usr/bin/markdown-languageserver")
        "python" -> listOf("usr/bin/pyright")
        "typescript" -> listOf("usr/bin/typescript-language-server", "usr/local/bin/typescript-language-server")
        "xml" -> listOf("root/.lsp/lemminx/server.jar")
        else -> emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LspSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val generalPrefs = remember {
        context.getSharedPreferences("MobileIDE_Settings", android.content.Context.MODE_PRIVATE)
    }
    val selectedDistro = generalPrefs.getString("selected_distro", "ubuntu") ?: "ubuntu"

    var lspItems by remember { mutableStateOf<List<LspItem>>(emptyList()) }

    LaunchedEffect(refreshTrigger, selectedDistro) {
        withContext(Dispatchers.IO) {
            val prefixDir = context.filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$selectedDistro")

            val assetsList = context.assets.list("terminal/lsp") ?: emptyArray()
            val items =
                assetsList
                    .filter { it.endsWith(".sh") }
                    .map { fileName ->
                        val id = fileName.removeSuffix(".sh")
                        val displayName =
                            when (id) {
                                "css" -> "CSS"
                                "html" -> "HTML"
                                "json" -> "JSON"
                                "xml" -> "XML"
                                "eslint" -> "ESLint"
                                else -> id.replaceFirstChar { it.uppercase() }
                            } + " Language Server"

                        val testPaths = getLspBinaryPaths(id)
                        val isInstalled = testPaths.any { path -> File(distroDir, path).exists() }

                        LspItem(id = id, name = displayName, scriptName = fileName, isInstalled = isInstalled)
                    }
                    .sortedBy { it.name }

            withContext(Dispatchers.Main) { lspItems = items }
        }
    }

    fun launchLspTerminalJob(tabTitle: String, command: String) {
        SessionManager.addNewSession(context, initCommand = command, tabTitle = tabTitle)
        navController.navigate("terminal")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_lsp_servers_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(lspItems) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector =
                                        if (item.isInstalled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (item.isInstalled) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (item.isInstalled) "Installiert" else "Nicht installiert",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!item.isInstalled) {
                                Button(
                                    onClick = {
                                        launchLspTerminalJob(
                                            "Install ${item.id.uppercase()}",
                                            "logDir=\${MOBILEIDE_WORKSPACE:-\$HOME}/logs && mkdir -p \$logDir && bash \$LOCAL/bin/lsp/${item.scriptName} 2>&1 | tee \$logDir/lsp_install_${item.id}.log"
                                        )
                                    }
                                ) {
                                    Text("Installieren")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        launchLspTerminalJob(
                                            "Update ${item.id.uppercase()}",
                                            "logDir=\${MOBILEIDE_WORKSPACE:-\$HOME}/logs && mkdir -p \$logDir && bash \$LOCAL/bin/lsp/${item.scriptName} --update 2>&1 | tee \$logDir/lsp_install_${item.id}.log"
                                        )
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                ) {
                                    Text("Update")
                                }
                                Button(
                                    onClick = {
                                        launchLspTerminalJob(
                                            "Remove ${item.id.uppercase()}",
                                            "logDir=\${MOBILEIDE_WORKSPACE:-\$HOME}/logs && mkdir -p \$logDir && bash \$LOCAL/bin/lsp/${item.scriptName} --uninstall 2>&1 | tee \$logDir/lsp_install_${item.id}.log"
                                        )
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Text("Löschen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
