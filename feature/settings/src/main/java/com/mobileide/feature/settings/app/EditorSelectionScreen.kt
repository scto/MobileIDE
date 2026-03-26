package com.mobileide.feature.settings.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.SettingsSection
import com.mobileide.feature.settings.SettingsTopBar

/**
 * Settings screen to choose between:
 *  • MobileIDE Editor (built-in)
 *  • Feature Editor (:feature:editor — full Sora editor with tab bar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSelectionScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    var selected by remember { mutableStateOf(EditorPreference.MOBILEIDE) }

    Scaffold(
        topBar = {
            SettingsTopBar("Editor auswählen", onBack = { onNavigate("SETTINGS") })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsSection("Editor-Auswahl")

            Text(
                "Wähle, welcher Editor beim Öffnen einer Datei verwendet werden soll.",
                style  = MaterialTheme.typography.bodyMedium,
                color  = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            // ── MobileIDE Editor ──────────────────────────────────────────────
            EditorOptionCard(
                title       = "MobileIDE Editor",
                description = "Vollständig integrierter Editor mit Projekt-Navigation, Git, Build-System, " +
                              "IntelligentFeatures (Auto-Tag, Bullet-Continuation), Theme-System und DataStore-Persistenz.",
                icon        = Icons.Default.Code,
                isSelected  = selected == EditorPreference.MOBILEIDE,
                features    = listOf(
                    "TextMate-Syntaxhervorhebung",
                    "Intelligente Funktionen (Auto-Tag, Bullet)",
                    "Material3-Theme-Integration",
                    "DataStore-Persistenz",
                    "Projekt-Kontext (Build, Git, Terminal)",
                ),
                onClick     = { onNavigate("setEditor:MOBILEIDE") },
            )

            // ── Feature Editor ────────────────────────────────────────────────
            EditorOptionCard(
                title       = "Feature Editor",
                description = "Eigenständiger Code-Editor als separates Modul (:feature:editor). " +
                              "Multi-Tab, Suchen/Ersetzen, Sprachauswahl, Bookmarks, Quick-Toolbar mit Sonderzeichen.",
                icon        = Icons.Default.Terminal,
                isSelected  = selected == EditorPreference.FEATURE,
                features    = listOf(
                    "Multi-Tab-Verwaltung",
                    "Suchen & Ersetzen (Regex)",
                    "Vollständige Sprachauswahl (25+ Sprachen)",
                    "Lesezeichen",
                    "Sonderzeichen-Schnellleiste",
                ),
                onClick     = { onNavigate("setEditor:FEATURE") },
            )
        }
    }
}

@Composable
private fun EditorOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    features: List<String>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = MaterialTheme.shapes.large,
        border   = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
        colors   = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null,
                    tint     = if (isSelected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface)
                }
                RadioButton(
                    selected = isSelected,
                    onClick  = onClick,
                    colors   = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(description,
                style  = MaterialTheme.typography.bodySmall,
                color  = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                features.forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null,
                            modifier = Modifier.size(12.dp),
                            tint     = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(f, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Enum to store editor preference. */
enum class EditorPreference { MOBILEIDE, FEATURE }