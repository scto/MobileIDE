package com.mobileide.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI primitives used across all settings sub-screens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsSection(title: String) {
    Text(
        text       = title,
        color      = MaterialTheme.colorScheme.primary,
        style      = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Tappable navigation row — the entire row area is clickable. */
@Composable
fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent   = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        leadingContent    = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent   = {
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        // clickable on the full row — this is the fix
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/** Toggle row */
@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent   = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        trailingContent   = {
            Switch(checked, onCheckedChange)
        },
        modifier = Modifier.fillMaxWidth(),
        colors   = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/** Info row — optionally tappable */
@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    val mod = if (onClick != null)
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    else
        Modifier.fillMaxWidth()

    ListItem(
        headlineContent   = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        modifier = mod,
        colors   = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/** Rounded card that groups related settings */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border   = BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column { content() }
    }
}

/** Standard settings TopAppBar */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title           = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon  = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions,
        colors  = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface),
    )
}
