package com.mobileide.app.ui.components

import androidx.compose.animation.*
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
import com.mobileide.app.data.BuildError
import com.mobileide.app.data.ErrorSeverity
import com.mobileide.app.data.TerminalLine
import com.mobileide.app.data.LineType
import com.mobileide.app.ui.theme.*
import java.io.File

// ── Gradle output parser ───────────────────────────────────────────────────────
object BuildErrorParser {

    // Matches:  e: /path/to/File.kt: (42, 7): error message
    private val KOTLIN_ERROR = Regex(
        """([ew]):\s*(/.+?\.kt):\s*\((\d+),\s*(\d+)\):\s*(.+)"""
    )
    // Matches:  /path/to/File.java:42: error: message
    private val JAVA_ERROR = Regex(
        """(/.+?\.java):(\d+):\s*(error|warning):\s*(.+)"""
    )
    // Matches generic Gradle task failure
    private val TASK_ERROR = Regex("""FAILURE:\s*(.+)""")

    fun parse(lines: List<TerminalLine>): List<BuildError> {
        val errors = mutableListOf<BuildError>()
        lines.forEach { tl ->
            val text = tl.text

            KOTLIN_ERROR.find(text)?.let { m ->
                val (severity, file, line, col, msg) = m.destructured
                errors += BuildError(
                    file     = file,
                    line     = line.toIntOrNull() ?: 0,
                    column   = col.toIntOrNull() ?: 0,
                    message  = msg.trim(),
                    severity = if (severity == "e") ErrorSeverity.ERROR else ErrorSeverity.WARNING
                )
                return@forEach
            }

            JAVA_ERROR.find(text)?.let { m ->
                val (file, line, severity, msg) = m.destructured
                errors += BuildError(
                    file     = file,
                    line     = line.toIntOrNull() ?: 0,
                    column   = 0,
                    message  = msg.trim(),
                    severity = if (severity == "error") ErrorSeverity.ERROR else ErrorSeverity.WARNING
                )
                return@forEach
            }
        }
        return errors
    }

    fun buildSucceeded(lines: List<TerminalLine>): Boolean =
        lines.any { it.type == LineType.SUCCESS && it.text.contains("BUILD SUCCESSFUL") }

    fun buildFailed(lines: List<TerminalLine>): Boolean =
        lines.any { it.type == LineType.ERROR && it.text.contains("BUILD FAILED") }
}

// ── Build Status Banner ────────────────────────────────────────────────────────
@Composable
fun BuildStatusBanner(
    terminalLines: List<TerminalLine>,
    isBuilding: Boolean,
    modifier: Modifier = Modifier
) {
    val errors   = remember(terminalLines) { BuildErrorParser.parse(terminalLines) }
    val success  = remember(terminalLines) { BuildErrorParser.buildSucceeded(terminalLines) }
    val failed   = remember(terminalLines) { BuildErrorParser.buildFailed(terminalLines) }

    val errorCount   = errors.count { it.severity == ErrorSeverity.ERROR }
    val warningCount = errors.count { it.severity == ErrorSeverity.WARNING }

    AnimatedVisibility(
        visible = isBuilding || success || failed,
        modifier = modifier
    ) {
        val bg = when {
            isBuilding -> IDEPrimary.copy(alpha = 0.15f)
            success    -> IDESecondary.copy(alpha = 0.15f)
            else       -> IDETertiary.copy(alpha = 0.15f)
        }
        Surface(color = bg) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBuilding) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = IDEPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Building…", color = IDEPrimary, fontSize = 12.sp)
                } else if (success) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = IDESecondary)
                    Spacer(Modifier.width(6.dp))
                    Text("Build Successful", color = IDESecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.Error, null, Modifier.size(14.dp), tint = IDETertiary)
                    Spacer(Modifier.width(6.dp))
                    Text("Build Failed", color = IDETertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (errorCount > 0) {
                        Spacer(Modifier.width(12.dp))
                        ErrorChip("$errorCount error${if (errorCount > 1) "s" else ""}", IDETertiary)
                    }
                    if (warningCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        ErrorChip("$warningCount warning${if (warningCount > 1) "s" else ""}", IDEPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape  = RoundedCornerShape(8.dp),
        color  = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color)
    ) {
        Text(label, color = color, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

// ── Error Panel ────────────────────────────────────────────────────────────────
@Composable
fun BuildErrorPanel(
    errors: List<BuildError>,
    visible: Boolean,
    onErrorClick: (BuildError) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && errors.isNotEmpty(),
        enter = slideInVertically { it },
        exit  = slideOutVertically { it },
        modifier = modifier
    ) {
        Surface(color = IDESurface, shadowElevation = 8.dp) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BugReport, null, Modifier.size(16.dp), tint = IDETertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("Problems (${errors.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = IDEOnBackground, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = IDEOnSurface)
                    }
                }
                HorizontalDivider(color = IDEOutline)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(errors) { error ->
                        BuildErrorItem(error, onClick = { onErrorClick(error) })
                        HorizontalDivider(color = IDEOutline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildErrorItem(error: BuildError, onClick: () -> Unit) {
    val (icon, color) = when (error.severity) {
        ErrorSeverity.ERROR   -> Icons.Default.Error to IDETertiary
        ErrorSeverity.WARNING -> Icons.Default.Warning to IDEPrimary
        ErrorSeverity.INFO    -> Icons.Default.Info to IDESecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, Modifier.size(14.dp).padding(top = 2.dp), tint = color)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(error.message, fontSize = 12.sp, color = IDEOnBackground, maxLines = 2)
            Text(
                "${File(error.file).name}:${error.line}:${error.column}",
                fontSize = 10.sp, color = IDEOnSurface
            )
        }
    }
}
