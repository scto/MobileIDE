package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ── LogCat Entry Model ─────────────────────────────────────────────────────────
data class LogEntry(
    val timestamp: String,
    val pid: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel(val letter: String, val color: Color) {
    VERBOSE("V", Color(0xFF9399B2)),
    DEBUG  ("D", Color(0xFF89DCEB)),
    INFO   ("I", Color(0xFFA6E3A1)),
    WARN   ("W", Color(0xFFFAB387)),
    ERROR  ("E", Color(0xFFF38BA8)),
    FATAL  ("F", Color(0xFFFF0000)),
    UNKNOWN("?", Color(0xFF6C7086));

    companion object {
        fun fromChar(c: Char) = values().firstOrNull { it.letter == c.uppercase() } ?: UNKNOWN
    }
}

// ── Parser ─────────────────────────────────────────────────────────────────────
object LogCatParser {
    // Format: "MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG: MSG"
    private val LOG_RE = Regex("""(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d+)\s+(\d+)\s+\d+\s+([VDIWEF])\s+([^:]+):\s*(.*)""")

    fun parse(raw: String): LogEntry? {
        val m = LOG_RE.matchEntire(raw.trim()) ?: return null
        val (ts, pid, lvl, tag, msg) = m.destructured
        return LogEntry(ts, pid, LogLevel.fromChar(lvl[0]), tag.trim(), msg)
    }

    fun parseFallback(raw: String): LogEntry =
        LogEntry("", "", LogLevel.UNKNOWN, "", raw)
}

// ── LogCat Screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogCatScreen(vm: IDEViewModel) {
    var entries     by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var filterText  by remember { mutableStateOf("") }
    var filterLevel by remember { mutableStateOf(LogLevel.VERBOSE) }
    var isRunning   by remember { mutableStateOf(false) }
    var autoScroll  by remember { mutableStateOf(true) }
    var showLevelMenu by remember { mutableStateOf(false) }
    val listState   = rememberLazyListState()

    // Simulated/real logcat reading via process
    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        try {
            val process = ProcessBuilder("sh", "-c",
                "logcat -v threadtime 2>&1 | head -1000")
                .redirectErrorStream(true)
                .start()
            val reader = process.inputStream.bufferedReader()
            while (isActive && isRunning) {
                val line = reader.readLine() ?: break
                val entry = LogCatParser.parse(line) ?: LogCatParser.parseFallback(line)
                entries = (entries + entry).takeLast(2000) // keep last 2000 lines
            }
        } catch (e: Exception) {
            entries = entries + LogEntry(
                "", "", LogLevel.ERROR, "LogCat",
                "Cannot read logcat directly. Use adb logcat via Terminal. Error: ${e.message}"
            )
            isRunning = false
        }
    }

    // Auto-scroll
    LaunchedEffect(entries.size) {
        if (autoScroll && entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    val filtered = remember(entries, filterText, filterLevel) {
        entries.filter { entry ->
            entry.level.ordinal >= filterLevel.ordinal &&
            (filterText.isEmpty() ||
             entry.tag.contains(filterText, ignoreCase = true) ||
             entry.message.contains(filterText, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LogCat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.EDITOR) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // Run/Stop
                    IconButton(onClick = { isRunning = !isRunning }) {
                        Icon(
                            if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            null,
                            tint = if (isRunning) IDETertiary else IDESecondary
                        )
                    }
                    // Auto-scroll
                    IconButton(onClick = { autoScroll = !autoScroll }) {
                        Icon(Icons.Default.VerticalAlignBottom, null,
                            tint = if (autoScroll) IDEPrimary else IDEOnSurface)
                    }
                    // Clear
                    IconButton(onClick = { entries = emptyList() }) {
                        Icon(Icons.Default.Delete, null, tint = IDEOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Filter bar
            Surface(color = IDESurfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Text filter
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Filter by tag or message…", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                            focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground,
                            cursorColor = IDEPrimary
                        )
                    )

                    // Level selector
                    Box {
                        Surface(
                            onClick = { showLevelMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            color = filterLevel.color.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, filterLevel.color)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(filterLevel.letter, color = filterLevel.color,
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp), tint = filterLevel.color)
                            }
                        }
                        DropdownMenu(expanded = showLevelMenu, onDismissRequest = { showLevelMenu = false }) {
                            LogLevel.values().filter { it != LogLevel.UNKNOWN }.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(level.letter, color = level.color,
                                                fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp),
                                                fontFamily = FontFamily.Monospace)
                                            Spacer(Modifier.width(8.dp))
                                            Text(level.name.lowercase().replaceFirstChar { it.uppercase() })
                                        }
                                    },
                                    onClick = { filterLevel = level; showLevelMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            // Stats bar
            Surface(color = IDESurface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${filtered.size} entries", style = MaterialTheme.typography.labelSmall, color = IDEOnSurface)
                    if (isRunning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(8.dp), color = IDESecondary, strokeWidth = 1.5.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("Live", style = MaterialTheme.typography.labelSmall, color = IDESecondary)
                        }
                    }
                    // Level counts
                    LogLevel.values().filter { it != LogLevel.UNKNOWN }.forEach { level ->
                        val count = entries.count { it.level == level }
                        if (count > 0) {
                            Text("${level.letter}:$count", style = MaterialTheme.typography.labelSmall, color = level.color)
                        }
                    }
                }
            }

            HorizontalDivider(color = IDEOutline)

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Terminal, null, Modifier.size(48.dp), tint = IDEOutline)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (entries.isEmpty()) "Press ▶ to start reading logs\nor run 'adb logcat' in Terminal"
                            else "No entries match the filter",
                            color = IDEOnSurface, fontSize = 13.sp
                        )
                        if (entries.isEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = {
                                vm.navigate(Screen.TERMINAL)
                                vm.runCommand("adb logcat -v time 2>&1 | head -200")
                            }) {
                                Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open in Terminal")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(filtered, key = { it.hashCode() }) { entry ->
                        LogEntryRow(entry, filterText)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry, highlight: String) {
    val bg = when (entry.level) {
        LogLevel.ERROR, LogLevel.FATAL -> IDETertiary.copy(alpha = 0.07f)
        LogLevel.WARN                  -> IDEPrimary.copy(alpha = 0.05f)
        else                           -> IDEBackground
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Level badge
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = entry.level.color.copy(alpha = 0.15f),
            modifier = Modifier.padding(top = 1.dp)
        ) {
            Text(
                entry.level.letter,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, color = entry.level.color)
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(entry.tag, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, color = IDEPrimary), maxLines = 1)
                if (entry.timestamp.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(entry.timestamp, style = TextStyle(fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp, color = IDEOutline))
                }
            }
            Text(entry.message,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    lineHeight = 16.sp, color = entry.level.color.copy(alpha = 0.9f)),
                overflow = TextOverflow.Visible
            )
        }
    }
}
