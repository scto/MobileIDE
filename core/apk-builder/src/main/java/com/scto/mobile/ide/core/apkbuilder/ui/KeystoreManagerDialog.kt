package com.scto.mobile.ide.core.apkbuilder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.scto.mobile.ide.core.apkbuilder.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeystoreManagerDialog(
    projectPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var existingKeystores by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Keystores, 1 = Neu erstellen

    // Form fields
    var name by remember { mutableStateOf("release.jks") }
    var alias by remember { mutableStateOf("key0") }
    var storePass by remember { mutableStateOf("") }
    var keyPass by remember { mutableStateOf("") }
    var samePassword by remember { mutableStateOf(true) }
    var validityYears by remember { mutableStateOf("25") }
    var commonName by remember { mutableStateOf("MobileIDE Developer") }
    var orgUnit by remember { mutableStateOf("MobileIDE") }
    var org by remember { mutableStateOf("MobileIDE") }
    var locality by remember { mutableStateOf("Berlin") }
    var state by remember { mutableStateOf("Berlin") }
    var countryCode by remember { mutableStateOf("DE") }
    var saveOnDevice by remember { mutableStateOf(true) }

    var isPassVisible by remember { mutableStateOf(false) }
    var selectedDetails by remember { mutableStateOf<KeystoreDetailInfo?>(null) }
    var showInjectConfirm by remember { mutableStateOf<KeystoreSigningConfig?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refreshList() {
        existingKeystores = KeystoreManager.listKeystores(projectPath)
    }

    LaunchedEffect(projectPath) {
        refreshList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Android Keystore Signier-Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Vorhandene (${existingKeystores.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Neuer Keystore") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // TAB 0: List existing keystores
                    if (existingKeystores.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Keine Keystores im Projekt gefunden", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(existingKeystores) { ksFile ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(ksFile.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            ksFile.absolutePath,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val details = KeystoreManager.getKeystoreDetails(ksFile, storePass)
                                                    if (details != null) {
                                                        selectedDetails = details
                                                    } else {
                                                        statusMessage = "Keystore konnte nicht geöffnet werden (Passwort prüfen)."
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Details", style = MaterialTheme.typography.labelSmall)
                                            }

                                            Button(
                                                onClick = {
                                                    val config = KeystoreSigningConfig(
                                                        storeFilePath = ksFile.absolutePath,
                                                        storePassword = storePass,
                                                        keyAlias = alias,
                                                        keyPassword = if (samePassword) storePass else keyPass
                                                    )
                                                    KeystoreConfigStore.saveSigningConfig(context, projectPath, config)
                                                    if (!KeystoreGradleInjector.hasReleaseSigningConfig(projectPath)) {
                                                        showInjectConfirm = config
                                                    } else {
                                                        statusMessage = "Keystore als Release-Standard gesetzt & keystore.properties aktualisiert."
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Als Standard", style = MaterialTheme.typography.labelSmall)
                                            }

                                            IconButton(
                                                onClick = {
                                                    KeystoreManager.deleteKeystore(ksFile)
                                                    refreshList()
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: Create Form
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Keystore Dateiname (z. B. release.jks)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = alias,
                                onValueChange = { alias = it },
                                label = { Text("Key Alias (z. B. key0)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = storePass,
                                onValueChange = { storePass = it },
                                label = { Text("Keystore Passwort") },
                                singleLine = true,
                                visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isPassVisible = !isPassVisible }) {
                                        Icon(if (isPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = samePassword, onCheckedChange = { samePassword = it })
                                Text("Schlüssel-Passwort = Keystore-Passwort", style = MaterialTheme.typography.bodySmall)
                            }
                            if (!samePassword) {
                                OutlinedTextField(
                                    value = keyPass,
                                    onValueChange = { keyPass = it },
                                    label = { Text("Schlüssel Passwort") },
                                    singleLine = true,
                                    visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            OutlinedTextField(
                                value = validityYears,
                                onValueChange = { validityYears = it },
                                label = { Text("Gültigkeit (Jahre)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = commonName,
                                onValueChange = { commonName = it },
                                label = { Text("Name / Inhaber (CN)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = orgUnit,
                                    onValueChange = { orgUnit = it },
                                    label = { Text("Abteilung (OU)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = org,
                                    onValueChange = { org = it },
                                    label = { Text("Firma (O)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = locality,
                                    onValueChange = { locality = it },
                                    label = { Text("Stadt (L)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = countryCode,
                                    onValueChange = { countryCode = it },
                                    label = { Text("Land (C, DE/US)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = saveOnDevice, onCheckedChange = { saveOnDevice = it })
                                Text("Passwort sicher auf diesem Gerät speichern?", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val effKeyPass = if (samePassword) storePass else keyPass
                            val params = KeystoreManager.CreateParams(
                                name = name,
                                alias = alias,
                                storePassword = storePass,
                                keyPassword = effKeyPass,
                                validityYears = validityYears.toIntOrNull() ?: 25,
                                commonName = commonName,
                                organizationUnit = orgUnit,
                                organization = org,
                                locality = locality,
                                state = state,
                                countryCode = countryCode
                            )
                            coroutineScope.launch {
                                val result = KeystoreManager.createKeystore(projectPath, params)
                                if (result.isSuccess) {
                                    val file = result.getOrThrow()
                                    val config = KeystoreSigningConfig(
                                        storeFilePath = file.absolutePath,
                                        storePassword = storePass,
                                        keyAlias = alias,
                                        keyPassword = effKeyPass,
                                        isSaveCredentialsOnDevice = saveOnDevice
                                    )
                                    KeystoreConfigStore.saveSigningConfig(context, projectPath, config)
                                    statusMessage = "Keystore ${file.name} erfolgreich erstellt & als Release-Standard gesetzt!"
                                    refreshList()
                                    selectedTab = 0
                                } else {
                                    statusMessage = "Fehler: ${result.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        enabled = name.isNotBlank() && alias.isNotBlank() && storePass.isNotBlank() && commonName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Keystore Erstellen")
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Schließen") }
                }
            }
        }
    }

    // Keystore Details Dialog
    selectedDetails?.let { details ->
        AlertDialog(
            onDismissRequest = { selectedDetails = null },
            title = { Text("Keystore Details: ${details.file.name}") },
            text = {
                Column {
                    Text("Alias: ${details.firstAlias ?: "-"}", fontWeight = FontWeight.Bold)
                    Text("Gültig von: ${details.validFrom ?: "-"}")
                    Text("Gültig bis: ${details.validUntil ?: "-"}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SHA-256 Fingerprint:", fontWeight = FontWeight.Bold)
                    Text(
                        details.sha256Fingerprint ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDetails = null }) { Text("OK") }
            }
        )
    }

    // Gradle Inject Confirm Dialog
    showInjectConfirm?.let { config ->
        val isKotlinDsl = KeystoreGradleInjector.findAppGradleFile(projectPath)?.name?.endsWith(".kts") == true
        AlertDialog(
            onDismissRequest = { showInjectConfirm = null },
            title = { Text("Release SigningConfig eintragen?") },
            text = {
                Column {
                    Text("Das Projekt enthält noch keine release-Signierkonfiguration. Soll die Konfiguration automatisch in app/build.gradle.kts eingetragen werden?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        KeystoreGradleInjector.generateGradleSnippet(isKotlinDsl),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val success = KeystoreGradleInjector.injectSigningConfig(projectPath)
                    statusMessage = if (success) "app/build.gradle.kts erfolgreich aktualisiert (Backup erstellt)." else "Fehler beim Eintragen in build.gradle.kts."
                    showInjectConfirm = null
                }) { Text("Jetzt Eintragen") }
            },
            dismissButton = {
                TextButton(onClick = { showInjectConfirm = null }) { Text("Später") }
            }
        )
    }
}
