package com.mobileide.app.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.logger.LogMsg
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(vm: IDEViewModel) {
    val context    = LocalContext.current
    val allEntries by Logger.entries.collectAsState()

    var filterLevel  by remember { mutableStateOf<Logger.Level?>(null) }
    var filterText   by remember { mutableStateOf("") }
    var filterTag    by remember { mutableStateOf("") }
    var autoScroll   by remember { mutableStateOf(true) }
    var showFilter   by remember { mutableStateOf(false) }
    var showLvlMenu  by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filtered = remember(allEntries, filterLevel, filterText, filterTag) {
        allEntries.filter { msg ->
            (filterLevel == null || msg.level == filterLevel!!.name) &&
            (filterText.isEmpty() || msg.message.contains(filterText, ignoreCase = true)) &&
            (filterTag.isEmpty()  || msg.src.contains(filterTag, ignoreCase = true))
        }
    }

    LaunchedEffect(filtered.size) {
        if (autoScroll && filtered.isNotEmpty())
            listState.animateScrollToItem(filtered.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Debug Log", fontWeight = FontWeight.Bold)
                        Text(
                            "${filtered.size}/${allEntries.size}  •  ${Logger.summary}",
                            fontSize = 10.sp, color = IDEOnSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.DEV_SETTINGS) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // Auto-scroll
                    IconButton(onClick = { autoScroll = !autoScroll }) {
                        Icon(Icons.Default.VerticalAlignBottom, null,
                            tint = if (autoScroll) IDEPrimary else IDEOnSurface)
                    }
                    // Filter
                    IconButton(onClick = { showFilter = !showFilter }) {
                        Icon(Icons.Default.FilterList, null,
                            tint = if (showFilter || filterLevel != null ||
                                       filterText.isNotEmpty() || filterTag.isNotEmpty())
                                       IDEPrimary else IDEOnSurface)
                    }
                    // Share
                    IconButton(onClick = {
                        Logger.info(LogTag.SCREEN_DEV, "export log")
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, Logger.exportText())
                            putExtra(Intent.EXTRA_SUBJECT, "MobileIDE Debug Log")
                        }
                        context.startActivity(Intent.createChooser(i, "Share Log")
                            .also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }) { Icon(Icons.Default.Share, null, tint = IDEOnSurface) }
                    // Clear buffer
                    IconButton(onClick = { Logger.clear() }) {
                        Icon(Icons.Default.Delete, null, tint = IDEOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Filter panel ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = showFilter,
                enter = slideInVertically { -it } + fadeIn(),
                exit  = slideOutVertically { -it } + fadeOut()
            ) {
                Surface(color = IDESurfaceVariant) {
                    Column(modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        OutlinedTextField(
                            value = filterText, onValueChange = { filterText = it },
                            placeholder = { Text("Filter message…", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                            trailingIcon = if (filterText.isNotEmpty()) {{
                                IconButton(onClick = { filterText = "" }) {
                                    Icon(Icons.Default.Close, null, Modifier.size(14.dp))
                                }
                            }} else null,
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                                focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = filterTag, onValueChange = { filterTag = it },
                                placeholder = { Text("Tag…", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f), singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                                    focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground)
                            )
                            Box {
                                OutlinedButton(onClick = { showLvlMenu = true }) {
                                    Text(filterLevel?.name ?: "ALL", fontSize = 11.sp,
                                        color = filterLevel?.let { levelColor(it) } ?: IDEOnBackground)
                                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                                }
                                DropdownMenu(expanded = showLvlMenu,
                                    onDismissRequest = { showLvlMenu = false }) {
                                    DropdownMenuItem(text = { Text("ALL") },
                                        onClick = { filterLevel = null; showLvlMenu = false })
                                    Logger.Level.values().forEach { level ->
                                        DropdownMenuItem(
                                            text = { Text(level.name, color = levelColor(level)) },
                                            onClick = { filterLevel = level; showLvlMenu = false })
                                    }
                                }
                            }
                        }

                        // Level stat chips
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Logger.Level.values().forEach { level ->
                                val count = allEntries.count { it.level == level.name }
                                if (count > 0) {
                                    val col = levelColor(level)
                                    Surface(
                                        onClick = { filterLevel = if (filterLevel == level) null else level },
                                        shape  = RoundedCornerShape(10.dp),
                                        color  = if (filterLevel == level) col.copy(.2f) else IDESurfaceVariant,
                                        border = BorderStroke(1.dp, col.copy(.5f))
                                    ) {
                                        Text("${level.name.take(1)}:$count",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 10.sp, color = col, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = IDEOutline)

            // ── Entries ────────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BugReport, null, Modifier.size(52.dp), tint = IDEOutline)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (allEntries.isEmpty()) "No entries yet.\nEnable Debug Log in Dev Settings."
                            else "No entries match the filter.",
                            color = IDEOnSurface, textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    itemsIndexed(filtered,
                        key = { i, msg -> "${msg.timestamp}_${msg.src}_$i" }) { _, msg ->
                        LogMsgRow(msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogMsgRow(msg: LogMsg) {
    var expanded by remember { mutableStateOf(false) }
    val col  = msg.color
    val rowBg = when (msg.level) {
        Logger.Level.ERROR.name   -> IDETertiary.copy(alpha = 0.05f)
        Logger.Level.WARNING.name -> SyntaxNumber.copy(alpha = 0.04f)
        Logger.Level.SUCCESS.name -> IDESecondary.copy(alpha = 0.03f)
        else -> IDEBackground
    }

    Column(modifier = Modifier.fillMaxWidth().background(rowBg)
        .clickable { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top) {
            // Level badge
            Surface(shape = RoundedCornerShape(3.dp), color = col.copy(alpha = 0.18f)) {
                Text(msg.level.take(1), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, color = col))
            }
            Spacer(Modifier.width(5.dp))
            Text(msg.formattedTime,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = IDEOutline),
                modifier = Modifier.width(76.dp))
            Spacer(Modifier.width(4.dp))
            Text(msg.src,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, color = IDEPrimary),
                modifier = Modifier.width(96.dp), maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Text(msg.message.trim(),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    lineHeight = 15.sp, color = col.copy(alpha = 0.92f)),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                modifier = Modifier.weight(1f))
        }
        if (expanded) {
            Surface(color = IDESurface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text("thread: ${msg.thread}   time: ${msg.formattedTimeFull}",
                        style = TextStyle(fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp, color = IDEOnSurface))
                }
            }
        }
    }
}

private fun levelColor(level: Logger.Level): Color = when (level) {
    Logger.Level.INFO    -> LogMsg.COLOR_INFO
    Logger.Level.SUCCESS -> LogMsg.COLOR_SUCCESS
    Logger.Level.WARNING -> LogMsg.COLOR_WARNING
    Logger.Level.ERROR   -> LogMsg.COLOR_ERROR
}
