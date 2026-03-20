package com.mobileide.app.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mobileide.app.ui.theme.*
import com.mobileide.app.utils.FileTemplate
import com.mobileide.app.utils.FileTemplates
import com.mobileide.app.utils.TemplateCategory

@Composable
fun FileTemplateDialog(
    targetDir: java.io.File,
    defaultPackage: String,
    onDismiss: () -> Unit,
    onCreate: (fileName: String, content: String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<FileTemplate?>(null) }
    var name by remember { mutableStateOf("") }
    var pkg  by remember { mutableStateOf(defaultPackage) }
    var selectedCat by remember { mutableStateOf<TemplateCategory?>(null) }

    val filtered = remember(selectedCat) {
        if (selectedCat == null) FileTemplates.templates
        else FileTemplates.byCategory(selectedCat!!)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = IDESurface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
        ) {
            if (selectedTemplate == null) {
                // ── Template Picker ──────────────────────────────────────────
                Column {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().background(IDESurfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = IDEPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("New File from Template", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, Modifier.size(18.dp), tint = IDEOnSurface)
                        }
                    }

                    // Category chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(selected = selectedCat == null, onClick = { selectedCat = null },
                            label = { Text("All", fontSize = 11.sp) })
                        TemplateCategory.values().forEach { cat ->
                            FilterChip(selected = selectedCat == cat,
                                onClick = { selectedCat = if (selectedCat == cat) null else cat },
                                label = { Text(cat.label, fontSize = 11.sp) })
                        }
                    }

                    HorizontalDivider(color = IDEOutline)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { template ->
                            TemplatePickerCard(template) {
                                selectedTemplate = template
                                name = template.name.split(" ").first() // default name
                            }
                        }
                    }
                }
            } else {
                // ── Configure Template ───────────────────────────────────────
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedTemplate = null }) {
                            Icon(Icons.Default.ArrowBack, null, tint = IDEOnBackground)
                        }
                        Text(selectedTemplate!!.name, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Configure", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (e.g. User, Product, Home)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                            focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pkg,
                        onValueChange = { pkg = it },
                        label = { Text("Package Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                            focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Preview file name
                    if (name.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = IDEBackground) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, null, Modifier.size(16.dp), tint = IDEPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text(selectedTemplate!!.fileName(name.trim().replaceFirstChar { it.uppercase() }),
                                    fontSize = 13.sp, color = IDEOnBackground)
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val cleanName = name.trim().replaceFirstChar { it.uppercase() }
                                if (cleanName.isNotEmpty()) {
                                    val content = selectedTemplate!!.generate(cleanName, pkg.trim())
                                    val fileName = selectedTemplate!!.fileName(cleanName)
                                    onCreate(fileName, content)
                                    onDismiss()
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create File")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatePickerCard(template: FileTemplate, onClick: () -> Unit) {
    val catColor = when (template.category) {
        TemplateCategory.ACTIVITY   -> IDEPrimary
        TemplateCategory.COMPOSE    -> IDESecondary
        TemplateCategory.VIEWMODEL  -> SyntaxAnnotation
        TemplateCategory.DATA       -> SyntaxType
        TemplateCategory.TESTING    -> SyntaxNumber
        TemplateCategory.UTIL       -> IDEOnSurface
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IDESurfaceVariant),
        border = BorderStroke(1.dp, IDEOutline)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = catColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.InsertDriveFile, null,
                    modifier = Modifier.padding(10.dp), tint = catColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(template.description, fontSize = 11.sp, color = IDEOnSurface)
                Surface(shape = RoundedCornerShape(4.dp), color = catColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(top = 4.dp)) {
                    Text(template.category.label, fontSize = 9.sp, color = catColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = IDEOutline)
        }
    }
}
