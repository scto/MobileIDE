package com.mobileide.app.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeScreen(vm: IDEViewModel) {

    // Current state — read from global mutable state so changes hot-swap immediately
    val activeTheme    by remember { currentM3Theme }
    val monetEnabled   by remember { dynamicM3Theme }
    val amoledEnabled  by remember { amoledM3Theme }

    var selectedId     by remember { mutableStateOf(activeTheme?.id ?: "blueberry-default") }
    var monet          by remember { mutableStateOf(monetEnabled) }
    var amoled         by remember { mutableStateOf(amoledEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Theme", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.SETTINGS) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        vm.setM3Theme(selectedId)
                        vm.setMonet(monet)
                        vm.setAmoled(amoled)
                        vm.navigate(Screen.SETTINGS)
                    }) {
                        Text("Apply", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── System integration ────────────────────────────────────────────
            SectionLabel("System")

            // Material You (API 31+)
            ToggleCard(
                icon     = Icons.Default.AutoAwesome,
                title    = "Material You",
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    "Use your wallpaper colours (Android 12+)"
                else
                    "Requires Android 12 or higher",
                checked  = monet,
                enabled  = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onToggle = { monet = it }
            )

            // AMOLED
            ToggleCard(
                icon     = Icons.Default.DarkMode,
                title    = "AMOLED / Pure Black",
                subtitle = "Replace backgrounds with pure black to save OLED power",
                checked  = amoled,
                onToggle = { amoled = it }
            )

            // ── Built-in themes ───────────────────────────────────────────────
            SectionLabel("Built-in Themes")

            m3Themes.forEach { holder ->
                ThemeCard(
                    holder   = holder,
                    selected = selectedId == holder.id,
                    onClick  = { selectedId = holder.id }
                )
            }

            // ── Preview ───────────────────────────────────────────────────────
            SectionLabel("Active Preview")

            val previewHolder = m3Themes.find { it.id == selectedId } ?: blueberry
            ThemePreviewCard(previewHolder)

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun ToggleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked  = checked,
                onCheckedChange = onToggle,
                enabled  = enabled,
                colors   = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
private fun ThemeCard(
    holder: ThemeHolder,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected)
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    else
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = border
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colour swatches — dark scheme colours shown
            val scheme = holder.darkScheme
            val swatches = listOf(
                scheme.background,
                scheme.surface,
                scheme.primary,
                scheme.secondary,
                scheme.tertiary,
                scheme.error,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                swatches.forEach { color ->
                    Box(
                        Modifier
                            .size(22.dp, 18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text       = holder.name,
                modifier   = Modifier.weight(1f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Icon(
                imageVector = if (selected) Icons.Default.RadioButtonChecked
                              else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(holder: ThemeHolder) {
    val light = holder.lightScheme
    val dark  = holder.darkScheme

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("\"${holder.name}\"",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Dark mode mini-preview
                SchemeBlock("Dark", dark)
                // Light mode mini-preview
                SchemeBlock("Light", light)
            }
        }
    }
}

@Composable
private fun RowScope.SchemeBlock(label: String, scheme: ColorScheme) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.background)
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Simulated top bar
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(scheme.surface)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.error)
                        .forEach { color ->
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                }
                // Simulated text lines
                listOf(1f, 0.7f, 0.5f).forEach { widthFraction ->
                    Box(
                        Modifier
                            .fillMaxWidth(widthFraction)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(scheme.onBackground.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
