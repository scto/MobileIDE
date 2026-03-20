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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mobileide.app.ui.theme.*
import java.io.File

// ══════════════════════════════════════════════════════════════════════════════
//  Breadcrumb Navigation Bar
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun BreadcrumbBar(
    file: File,
    projectRoot: File?,
    modifier: Modifier = Modifier
) {
    val segments = buildBreadcrumbs(file, projectRoot)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(IDESurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                Icon(Icons.Default.ChevronRight, null, Modifier.size(14.dp), tint = IDEOutline)
            }

            val isLast = index == segments.lastIndex
            val color = when {
                isLast && segment.endsWith(".kt")   -> SyntaxKeyword
                isLast && segment.endsWith(".java") -> SyntaxNumber
                isLast && segment.endsWith(".xml")  -> SyntaxType
                isLast                              -> IDEOnBackground
                else                                -> IDEOnSurface
            }

            Text(
                text = segment,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                    color = color
                )
            )
        }

        // Language tag
        val lang = file.extension.uppercase()
        if (lang.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = IDESurfaceVariant
            ) {
                Text(
                    lang,
                    fontSize = 9.sp,
                    color = IDEOnSurface,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun buildBreadcrumbs(file: File, projectRoot: File?): List<String> {
    if (projectRoot == null) return listOf(file.name)
    val relativePath = try {
        file.canonicalPath.removePrefix(projectRoot.canonicalPath).trimStart('/')
    } catch (_: Exception) { file.name }
    return relativePath.split("/").filter { it.isNotEmpty() }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Recent Files Panel (popup)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun RecentFilesPanel(
    recentFiles: List<String>,
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = modifier.width(280.dp),
            color = IDESurface,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, IDEOutline),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(IDESurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, null, Modifier.size(16.dp), tint = IDEPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Recent Files", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.weight(1f), color = IDEOnBackground)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(14.dp), tint = IDEOnSurface)
                    }
                }
                HorizontalDivider(color = IDEOutline)

                if (recentFiles.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        Text("No recent files", color = IDEOnSurface, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(recentFiles.take(15)) { path ->
                            val file = File(path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (file.exists()) { onOpenFile(file); onDismiss() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val extColor = when (file.extension.lowercase()) {
                                    "kt", "kts" -> SyntaxKeyword
                                    "java"      -> SyntaxNumber
                                    "xml"       -> SyntaxType
                                    "json"      -> SyntaxString
                                    else        -> IDEOnSurface
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = extColor.copy(alpha = 0.12f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            file.extension.uppercase().take(3).ifEmpty { "?" },
                                            fontSize = 8.sp, color = extColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        file.name,
                                        fontSize = 12.sp, color = IDEOnBackground,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    // Show parent folder
                                    Text(
                                        file.parentFile?.name ?: "",
                                        fontSize = 10.sp, color = IDEOnSurface,
                                        maxLines = 1
                                    )
                                }
                                if (!file.exists()) {
                                    Icon(Icons.Default.Error, null,
                                        Modifier.size(12.dp), tint = IDETertiary)
                                }
                            }
                            HorizontalDivider(color = IDEOutline.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Code Stats Overlay (shown when cursor is in editor)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun EditorStatusBar(
    fileName: String,
    lineCount: Int,
    cursorLine: Int,
    cursorCol: Int,
    isModified: Boolean,
    language: String,
    modifier: Modifier = Modifier
) {
    Surface(color = IDESurface, tonalElevation = 1.dp, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: file info
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (isModified) {
                    Icon(Icons.Default.Circle, null, Modifier.size(8.dp), tint = IDESecondary)
                }
                Text(language, fontSize = 10.sp, color = IDEOnSurface,
                    fontFamily = FontFamily.Monospace)
            }
            // Right: cursor position
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Ln $cursorLine, Col $cursorCol", fontSize = 10.sp, color = IDEOnSurface,
                    fontFamily = FontFamily.Monospace)
                Text("$lineCount lines", fontSize = 10.sp, color = IDEOutline,
                    fontFamily = FontFamily.Monospace)
            }
        }
    }
}
