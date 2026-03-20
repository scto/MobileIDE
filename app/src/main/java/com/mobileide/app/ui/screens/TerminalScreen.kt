package com.mobileide.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.data.LineType
import com.mobileide.app.data.TerminalLine
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(vm: IDEViewModel) {
    val lines         by vm.terminalLines.collectAsState()
    val project       by vm.currentProject.collectAsState()
    val isBuilding    by vm.isBuilding.collectAsState()
    val history       by vm.commandHistory.collectAsState()
    val listState     = rememberLazyListState()

    var command       by remember { mutableStateOf(TextFieldValue("")) }
    var historyIndex  by remember { mutableStateOf(-1) }
    var showQuickCmds by remember { mutableStateOf(false) }
    var autoScroll    by remember { mutableStateOf(true) }
    val focusReq      = remember { FocusRequester() }

    LaunchedEffect(lines.size) {
        if (autoScroll && lines.isNotEmpty())
            listState.animateScrollToItem(lines.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Terminal", fontWeight = FontWeight.Bold)
                        if (isBuilding) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                Modifier.size(14.dp), color = IDEPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("Building…", fontSize = 12.sp, color = IDEPrimary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.navigate(if (project != null) Screen.EDITOR else Screen.HOME)
                    }) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { autoScroll = !autoScroll }) {
                        Icon(Icons.Default.VerticalAlignBottom, null,
                            tint = if (autoScroll) IDEPrimary else IDEOnSurface)
                    }
                    IconButton(onClick = { showQuickCmds = !showQuickCmds }) {
                        Icon(Icons.Default.MoreVert, null, tint = IDEOnBackground)
                    }
                    IconButton(onClick = { vm.clearTerminal() }) {
                        Icon(Icons.Default.Delete, null, tint = IDEOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Quick commands panel
            AnimatedVisibility(
                visible = showQuickCmds,
                enter   = slideInVertically { -it } + fadeIn(),
                exit    = slideOutVertically { -it } + fadeOut()
            ) {
                Surface(color = IDESurfaceVariant) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Quick Commands",
                            style = MaterialTheme.typography.labelSmall, color = IDEPrimary)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            project?.let { p ->
                                QuickChip("▶ Build", IDESecondary) {
                                    vm.buildProject(); showQuickCmds = false
                                }
                                QuickChip("🧹 Clean", IDEPrimary) {
                                    vm.cleanProject(); showQuickCmds = false
                                }
                                QuickChip("📋 Tasks") {
                                    vm.runCommand("cd '${p.path}' && gradle tasks --all 2>&1 | head -80")
                                    showQuickCmds = false
                                }
                            }
                            QuickChip("☕ Env") {
                                vm.checkEnvironment(); showQuickCmds = false
                            }
                            QuickChip("📦 Packages") {
                                vm.runCommand("pkg list-installed 2>/dev/null | head -30")
                                showQuickCmds = false
                            }
                            QuickChip("💾 Disk") {
                                vm.runCommand("df -h 2>/dev/null | head -5")
                                showQuickCmds = false
                            }
                            project?.let { p ->
                                QuickChip("📡 Git status") {
                                    vm.runCommand("git -C '${p.path}' status 2>&1")
                                    showQuickCmds = false
                                }
                            }
                        }

                        if (vm.termux.isTermuxInstalled()) {
                            Button(
                                onClick = { vm.termux.openTermux(); showQuickCmds = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = IDESecondary)
                            ) {
                                Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp), tint = IDEBackground)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Termux App", color = IDEBackground)
                            }
                        }

                        OutlinedButton(
                            onClick = { vm.navigate(Screen.SETUP_WIZARD); showQuickCmds = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Build, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Setup Wizard")
                        }
                    }
                }
            }

            HorizontalDivider(color = IDEOutline)

            // Terminal output
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(lines) { line -> TerminalLineItem(line) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$ ", color = IDESecondary,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))
                    }
                }
            }

            HorizontalDivider(color = IDEOutline)

            // History hint
            if (history.isNotEmpty() && historyIndex >= 0) {
                Surface(color = IDESurfaceVariant) {
                    Text(
                        "History ${historyIndex + 1}/${history.size}  ↑↓ navigate",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp, color = IDEOnSurface)
                    )
                }
            }

            // Input row
            Surface(color = IDESurface) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$ ", color = IDESecondary,
                        style = TextStyle(fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold))

                    TextField(
                        value = command,
                        onValueChange = { command = it; historyIndex = -1 },
                        modifier = Modifier.weight(1f).focusRequester(focusReq),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp, color = IDEOnBackground),
                        placeholder = {
                            Text("enter command…",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp), color = IDEOutline)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor             = IDEPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (command.text.isNotBlank()) {
                                vm.runCommand(command.text); command = TextFieldValue(""); historyIndex = -1
                            }
                        })
                    )

                    // History up
                    IconButton(
                        onClick = {
                            if (history.isNotEmpty()) {
                                historyIndex = minOf(historyIndex + 1, history.lastIndex)
                                command = TextFieldValue(history[historyIndex])
                            }
                        },
                        modifier = Modifier.size(36.dp), enabled = history.isNotEmpty()
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(18.dp),
                            tint = if (history.isNotEmpty()) IDEOnSurface else IDEOutline)
                    }

                    // History down
                    IconButton(
                        onClick = {
                            if (historyIndex > 0) {
                                historyIndex--; command = TextFieldValue(history[historyIndex])
                            } else {
                                historyIndex = -1; command = TextFieldValue("")
                            }
                        },
                        modifier = Modifier.size(36.dp), enabled = historyIndex >= 0
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp),
                            tint = if (historyIndex >= 0) IDEOnSurface else IDEOutline)
                    }

                    // Send
                    IconButton(onClick = {
                        if (command.text.isNotBlank()) {
                            vm.runCommand(command.text); command = TextFieldValue(""); historyIndex = -1
                        }
                    }) { Icon(Icons.Default.Send, null, tint = IDEPrimary) }
                }
            }
        }
    }
}

@Composable
private fun TerminalLineItem(line: TerminalLine) {
    val color = when (line.type) {
        LineType.INPUT   -> IDESecondary
        LineType.ERROR   -> IDETertiary
        LineType.SUCCESS -> IDESecondary
        LineType.INFO    -> IDEPrimary
        LineType.OUTPUT  -> IDEOnBackground
    }
    Text(line.text, color = color,
        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp))
}

@Composable
private fun QuickChip(label: String, color: Color = IDEPrimary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape  = RoundedCornerShape(16.dp),
        color  = IDESurfaceVariant,
        border = BorderStroke(1.dp, color)
    ) {
        Text(label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = color)
    }
}
