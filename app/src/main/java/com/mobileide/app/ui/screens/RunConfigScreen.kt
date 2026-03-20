package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

data class RunConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val buildVariant: String = "debug",
    val module: String = ":app",
    val extraGradleArgs: String = "",
    val deployToDevice: Boolean = true,
    val jvmArgs: String = "-Xmx2g",
    val isDefault: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunConfigScreen(vm: IDEViewModel) {
    var configs by remember {
        mutableStateOf(
            listOf(
                RunConfig("debug-default", "Debug (default)", "debug", isDefault = true),
                RunConfig("release", "Release", "release"),
                RunConfig("debug-no-deploy", "Build only (no install)", "debug", deployToDevice = false),
            )
        )
    }
    var editingConfig by remember { mutableStateOf<RunConfig?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run Configurations", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.EDITOR) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showNewDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = IDESecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("CONFIGURATIONS", style = MaterialTheme.typography.labelSmall, color = IDEPrimary,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            items(configs, key = { it.id }) { config ->
                RunConfigCard(
                    config = config,
                    onRun = {
                        vm.navigate(Screen.TERMINAL)
                        val gradleTask = if (config.buildVariant == "release")
                            "assembleRelease" else "assembleDebug"
                        val deployCmd = if (config.deployToDevice)
                            " && adb install -r app/build/outputs/apk/${config.buildVariant}/app-${config.buildVariant}.apk"
                        else ""
                        val extraArgs = if (config.extraGradleArgs.isNotEmpty())
                            " ${config.extraGradleArgs}" else ""
                        vm.runCommand("cd '${vm.currentProject.value?.path}' && gradle ${config.module}:$gradleTask$extraArgs$deployCmd")
                    },
                    onEdit = { editingConfig = config },
                    onDelete = { if (!config.isDefault) configs = configs.filter { it.id != config.id } },
                    onSetDefault = {
                        configs = configs.map { it.copy(isDefault = it.id == config.id) }
                    }
                )
            }
        }
    }

    if (showNewDialog || editingConfig != null) {
        RunConfigDialog(
            initial = editingConfig,
            onDismiss = { showNewDialog = false; editingConfig = null },
            onSave = { config ->
                configs = if (editingConfig != null) {
                    configs.map { if (it.id == config.id) config else it }
                } else {
                    configs + config
                }
                showNewDialog = false; editingConfig = null
            }
        )
    }
}

@Composable
private fun RunConfigCard(
    config: RunConfig,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val variantColor = if (config.buildVariant == "release") IDETertiary else IDESecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isDefault) IDEPrimary.copy(alpha = 0.06f) else IDESurface
        ),
        border = BorderStroke(1.dp, if (config.isDefault) IDEPrimary.copy(alpha = 0.4f) else IDEOutline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (config.isDefault) {
                    Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = IDEPrimary)
                    Spacer(Modifier.width(6.dp))
                }
                Text(config.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(6.dp), color = variantColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, variantColor)) {
                    Text(config.buildVariant, fontSize = 10.sp, color = variantColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(Icons.Default.Extension, config.module)
                if (config.deployToDevice)
                    InfoChip(Icons.Default.PhoneAndroid, "install")
                if (config.extraGradleArgs.isNotEmpty())
                    InfoChip(Icons.Default.Settings, config.extraGradleArgs.take(20))
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onRun, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = IDESecondary)) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp), tint = IDEBackground)
                    Spacer(Modifier.width(4.dp))
                    Text("Run", color = IDEBackground, fontSize = 13.sp)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = IDEOnSurface)
                }
                if (!config.isDefault) {
                    IconButton(onClick = onSetDefault) {
                        Icon(Icons.Default.StarBorder, null, Modifier.size(18.dp), tint = IDEOnSurface)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = IDETertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(12.dp), tint = IDEOnSurface)
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 10.sp, color = IDEOnSurface, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun RunConfigDialog(
    initial: RunConfig?,
    onDismiss: () -> Unit,
    onSave: (RunConfig) -> Unit
) {
    var name    by remember { mutableStateOf(initial?.name ?: "") }
    var variant by remember { mutableStateOf(initial?.buildVariant ?: "debug") }
    var module  by remember { mutableStateOf(initial?.module ?: ":app") }
    var extra   by remember { mutableStateOf(initial?.extraGradleArgs ?: "") }
    var deploy  by remember { mutableStateOf(initial?.deployToDevice ?: true) }
    var jvm     by remember { mutableStateOf(initial?.jvmArgs ?: "-Xmx2g") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Run Configuration" else "Edit Configuration") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                field("Name", name) { name = it }
                Text("Build Variant", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("debug", "release").forEach { v ->
                        FilterChip(selected = variant == v, onClick = { variant = v },
                            label = { Text(v, fontSize = 12.sp) })
                    }
                }
                field("Module", module) { module = it }
                field("Extra Gradle Args (optional)", extra) { extra = it }
                field("JVM Args", jvm) { jvm = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Deploy to device after build", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Switch(deploy, { deploy = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(RunConfig(
                    id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                    name = name.ifBlank { "Config" },
                    buildVariant = variant, module = module,
                    extraGradleArgs = extra, deployToDevice = deploy,
                    jvmArgs = jvm, isDefault = initial?.isDefault ?: false
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
            focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground
        )
    )
}
