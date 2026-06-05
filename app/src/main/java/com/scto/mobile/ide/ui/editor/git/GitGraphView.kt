/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.scto.mobile.ide.ui.editor.git

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.scto.mobile.ide.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun GitGraphListCompact(commits: List<GitCommitUI>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        itemsIndexed(commits) { index, commit ->
            // To draw continuous lines, we need to know the next commit's totalLanes
            // In Compose rendering, we only draw downward lines, so only the current commit data is required
            GitLogItemAligned(commit)
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun GitLogItemAligned(commit: GitCommitUI) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Color palette (used by the view layer for consistency)
    val laneColors = listOf(
        Color(0xFFFF5252), Color(0xFF40C4FF), Color(0xFFE040FB),
        Color(0xFF69F0AE), Color(0xFFFFAB40), Color(0xFFFFD740),
        Color(0xFF9E9E9E), Color(0xFF795548)
    )

    // Size constants
    val rowHeight = if (expanded) 80.dp else 40.dp // IDEA Default is relatively compact
    val graphWidth = 40.dp // Force a fixed left width to keep the right side aligned
    val laneW = 12.dp      // Lane spacing
    val dotR = 5.dp        // Node radius

    val surfaceColor = MaterialTheme.colorScheme.surface
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(if (expanded) highlightColor else Color.Transparent)
            .clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. Left graph area (fixed width) ---
        Box(
            modifier = Modifier
                .width(graphWidth)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.height / 2

                // Logic: assume the maximum lane count is totalLanes.
                // Note: this is only for visual continuity.
                val maxLaneIdx = max(commit.lane, commit.parentLanes.maxOrNull() ?: 0)
                
                // Limit the maximum rendered lanes to avoid drawing outside the screen
                val drawLimit = 4

                for (i in 0..minOf(maxLaneIdx + 1, drawLimit)) {
                    if (i != commit.lane) {
                        val x = (i * laneW.toPx()) + (laneW.toPx() / 2) + 6f
                        val color = laneColors[i % laneColors.size]

                        // Draw a continuous vertical line for passing branches
                        drawLine(
                            color = color,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // 2. Draw a Bezier line from the current node to its parent
                val myX = (commit.lane * laneW.toPx()) + (laneW.toPx() / 2) + 6f

                // Draw the upper segment first and connect it to the current node
                drawLine(
                    color = commit.color,
                    start = Offset(myX, 0f),
                    end = Offset(myX, centerY),
                    strokeWidth = 2.dp.toPx()
                )

                commit.parentLanes.forEach { pLane ->
                    if (pLane <= drawLimit) { // Only draw lines within the visible area
                        val pX = (pLane * laneW.toPx()) + (laneW.toPx() / 2) + 6f
                        val color = commit.color // Connect to the parent node using the node's own color

                        val path = Path().apply {
                            moveTo(myX, centerY)
                            if (pLane == commit.lane) {
                                lineTo(pX, size.height)
                            } else {
                                // Draw a curved line to the parent lane
                                cubicTo(
                                    myX, size.height * 0.9f,
                                    pX, centerY + (size.height - centerY) * 0.1f,
                                    pX, size.height
                                )
                            }
                        }
                        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
                    }
                }

                // 3. Draw node circle (top layer)
                if (commit.lane <= drawLimit) {
                    drawCircle(color = surfaceColor, radius = dotR.toPx() + 3f, center = Offset(myX, centerY))
                    drawCircle(color = commit.color, radius = dotR.toPx(), center = Offset(myX, centerY))
                }
            }
        }

        // --- 2. Right text area ---
        Column(
            modifier = Modifier
                .weight(1f) // Occupy remaining space
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // First row: [Tag] Message
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (commit.refs.isNotEmpty()) {
                    commit.refs.forEach { ref ->
                        GitRefChipNano(ref)
                        Spacer(Modifier.width(4.dp))
                    }
                }

                Text(
                    text = if (expanded) commit.fullMessage else commit.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    maxLines = if (expanded) 10 else 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f) // Allow text to adapt to available width
                )
            }

            // Second row: Author · Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 1.dp)
            ) {
                Text(
                    text = commit.author,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    text = getRelativeTimeShort(context, commit.time),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.weight(1f)) // Expand middle section

                // Show hash copy action only when expanded
                if (expanded) {
                    Icon(
                        Icons.Default.ContentCopy, null,
                        Modifier.size(12.dp).clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(context.getString(R.string.git_clipboard_hash_label), commit.hash)
                            clipboard.setPrimaryClip(clip)
                        },
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(4.dp))
                }

                Text(
                    text = commit.shortHash,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun GitRefChipNano(ref: GitRefUI) {
    // Minimalist style tag
    val bgColor = when(ref.type) {
        RefType.HEAD -> Color(0xFF607D8B)
        RefType.LOCAL_BRANCH -> Color(0xFF009688)
        RefType.REMOTE_BRANCH -> Color(0xFF673AB7)
        RefType.TAG -> Color(0xFFEF6C00)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(3.dp),
        modifier = Modifier.height(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 3.dp)) {
            Text(
                text = ref.name,
                fontSize = 9.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun getRelativeTimeShort(context: Context, timeMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timeMs
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> context.getString(R.string.git_time_now)
        minutes < 60 -> context.getString(R.string.git_time_minutes_short, minutes)
        hours < 24 -> context.getString(R.string.git_time_hours_short, hours)
        days < 30 -> context.getString(R.string.git_time_days_short, days)
        else -> DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(timeMs))
    }
}