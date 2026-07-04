package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// ─── Terminal color constants ─────────────────────────────────────────────────

private val BgTerminal      = Color(0xFF121212)
private val ColTimestamp    = Color(0xFF607D8B)   // Blue-grey
private val ColLineNumber   = Color(0xFF546E7A)   // Darker blue-grey
private val ColLevelInfo    = Color(0xFF66BB6A)   // Green
private val ColLevelWarn    = Color(0xFFFFCA28)   // Yellow
private val ColLevelError   = Color(0xFFEF5350)   // Red
private val ColLevelProgress= Color(0xFF29B6F6)   // Cyan
private val ColLevelDefault = Color(0xFFB0BEC5)   // Blue-grey (default text)
private val ColMessage      = Color(0xFFECEFF1)   // Near-white

private val tsFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

private fun levelColor(level: String): Color = when (level.uppercase()) {
    "ERROR"    -> ColLevelError
    "WARN"     -> ColLevelWarn
    "INFO"     -> ColLevelInfo
    "SUCCESS"  -> ColLevelInfo
    "PROGRESS" -> ColLevelProgress
    else       -> ColLevelDefault
}

/**
 * Dark-themed terminal log view rendered inside a [ModalBottomSheet].
 *
 * Each line is formatted exactly as:
 *   [lineNumber] - [HH:mm:ss.SSS] [[LEVEL]] message text
 *
 * - Level tag colour: ERROR=Red, WARN=Yellow, INFO/SUCCESS=Green, PROGRESS=Cyan
 * - Monospace font throughout
 * - [LaunchedEffect(logLines.size)] smoothly animates scroll to the last item
 * - Stable item keys via [itemsIndexed] + item ID
 *
 * @param logLines   Live log items from [GradleViewModel.logLines].
 * @param isBuilding True while a build is in progress (shows spinner).
 * @param onDismiss  Called when the sheet should close.
 * @param modifier   Optional outer modifier.
 */
@Composable
fun GradleLogBottomSheetContent(
    logLines:   List<LogDisplayItem>,
    isBuilding: Boolean,
    onDismiss:  () -> Unit,
    modifier:   Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Smooth auto-scroll to the latest log line whenever a new line arrives
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.animateScrollToItem(index = logLines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .background(BgTerminal)
            .padding(horizontal = 0.dp),
    ) {
        // ── Header bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = "Build Output",
                color      = Color(0xFF8A8A8A),
                fontSize   = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )

            if (isBuilding) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(16.dp),
                    color     = ColLevelInfo,
                    strokeWidth = 2.dp,
                )
            }
        }

        // ── Log lines ─────────────────────────────────────────────────────────
        LazyColumn(
            state          = listState,
            modifier       = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            itemsIndexed(
                items = logLines,
                // Stable key: prefer item.id (nanoTime-based, unique per item)
                key   = { _, item -> item.id },
            ) { index, item ->
                TerminalLogLine(
                    lineNumber = index + 1,
                    item       = item,
                )
            }
        }
    }
}

// ─── Single log line ─────────────────────────────────────────────────────────

@Composable
private fun TerminalLogLine(
    lineNumber: Int,
    item:       LogDisplayItem,
) {
    val formattedTs = remember(item.timestamp) {
        tsFormat.format(Date(item.timestamp))
    }

    val levelColor = levelColor(item.level)

    // Build the annotated string once, keyed by the item contents
    val annotatedLine = remember(lineNumber, item) {
        buildAnnotatedString {
            // Line number
            withStyle(SpanStyle(color = ColLineNumber, fontWeight = FontWeight.Normal)) {
                append(lineNumber.toString().padStart(4, ' '))
            }

            append(" - ")

            // Timestamp
            withStyle(SpanStyle(color = ColTimestamp)) {
                append(formattedTs)
            }

            append(" ")

            // Level badge [LEVEL]
            withStyle(
                SpanStyle(
                    color      = levelColor,
                    fontWeight = FontWeight.Bold,
                )
            ) {
                append("[")
                append(item.level.padEnd(8))
                append("]")
            }

            append(" ")

            // Message text
            withStyle(SpanStyle(color = ColMessage)) {
                append(item.text)
            }
        }
    }

    Text(
        text       = annotatedLine,
        fontSize   = 11.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 16.sp,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
    )
}
