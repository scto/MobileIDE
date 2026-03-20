package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.AppConstants
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LoggerRes
import com.mobileide.app.logger.LogMsg
import com.mobileide.app.logger.LogTag
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevSettingsScreen(vm: IDEViewModel) {

    var debugEnabled by remember { mutableStateOf(Logger.enabled) }
    val entries      by Logger.entries.collectAsState()

    DisposableEffect(Unit) {
        val listener = object : LoggerRes.LogListener {
            override fun reloadResRef()    { Logger.info(LogTag.LOGGER_RES, "reloadResRef") }
            override fun onSaveRequested() { Logger.info(LogTag.LOGGER_RES, "onSaveRequested") }
        }
        LoggerRes.addLogListener(listener)
        onDispose { LoggerRes.removeLogListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Development", fontWeight = FontWeight.Bold)
                        Text("Debug & diagnostic tools", fontSize = 11.sp, color = IDEOnSurface)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.SETTINGS) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()
            .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            DevSection("Debug Logging")

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (debugEnabled) IDESecondary.copy(alpha = 0.07f) else IDESurface),
                border = BorderStroke(
                    if (debugEnabled) 1.5.dp else 1.dp,
                    if (debugEnabled) IDESecondary.copy(alpha = 0.4f) else IDEOutline)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(10.dp),
                        color = (if (debugEnabled) IDESecondary else IDEOutline).copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BugReport, null, Modifier.size(24.dp),
                                tint = if (debugEnabled) IDESecondary else IDEOutline)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Debug Log", fontWeight = FontWeight.SemiBold, color = IDEOnBackground)
                        Text(
                            if (debugEnabled) "Recording — ${entries.size} entries  •  ${Logger.summary}"
                            else "Off — calls still forwarded to Logcat",
                            fontSize = 12.sp, color = IDEOnSurface)
                    }
                    Switch(checked = debugEnabled,
                        onCheckedChange = { on ->
                            debugEnabled = on
                            Logger.enabled = on
                            if (on) Logger.info(LogTag.SCREEN_DEV, "Debug logging ENABLED by user")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = IDESecondary,
                            checkedTrackColor = IDESecondary.copy(alpha = 0.4f)))
                }
            }

            if (debugEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("WARN",    Logger.warn,    LogMsg.COLOR_WARNING, Modifier.weight(1f))
                    StatChip("INFO",    Logger.info,    LogMsg.COLOR_INFO,    Modifier.weight(1f))
                    StatChip("ERROR",   Logger.error,   LogMsg.COLOR_ERROR,   Modifier.weight(1f))
                    StatChip("SUCCESS", Logger.success, LogMsg.COLOR_SUCCESS, Modifier.weight(1f))
                }

                Card(onClick = { vm.navigate(Screen.LOG_VIEWER) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = IDESurface),
                    border   = BorderStroke(1.dp,
                        if (Logger.hasErrors) IDETertiary.copy(alpha = 0.4f) else IDEOutline)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Article, null, Modifier.size(22.dp),
                            tint = if (Logger.hasErrors) IDETertiary else IDEPrimary)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("View Debug Log", fontWeight = FontWeight.Medium, color = IDEOnBackground)
                            Text("${entries.size} entries in buffer", fontSize = 12.sp, color = IDEOnSurface)
                        }
                        if (Logger.hasErrors) {
                            Surface(shape = RoundedCornerShape(8.dp), color = IDETertiary.copy(alpha = 0.15f)) {
                                Text("${Logger.error} error${if (Logger.error != 1) "s" else ""}",
                                    fontSize = 11.sp, color = IDETertiary, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = IDEOutline)
                    }
                }

                OutlinedButton(onClick = {
                    Logger.initFromZero()
                    Logger.enabled = true
                    Logger.info(LogTag.SCREEN_DEV, "Logger reset by user")
                }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, IDEOutline)) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset Counters & Clear Buffer")
                }
            }

            DevSection("Logger Architecture")
            Card(colors = CardDefaults.cardColors(containerColor = IDESurface),
                border = BorderStroke(1.dp, IDEOutline)) {
                Column(modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(
                        Triple("Logger",          IDEPrimary,      "Global static — warn / info / error / success"),
                        Triple("LoggerRes",        IDESecondary,    "Resource event bus — reloadResRef / onSaveRequested"),
                        Triple("LoggerLayoutUI",   SyntaxAnnotation,"Per-tag WeakHashMap cache for UI components"),
                        Triple("LogMsg",           IDEOnSurface,    "Record: src · level · message · Compose color"),
                        Triple("LogTag",           IDEOnSurface,    "Compile-time tag constant strings"),
                    ).forEach { (cls, col, desc) ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("●", color = col, fontSize = 10.sp, modifier = Modifier.width(12.dp))
                            Column {
                                Text(cls, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = col, fontFamily = FontFamily.Monospace)
                                Text(desc, fontSize = 11.sp, color = IDEOnSurface)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Kotlin port of jkas.androidpe.logger by JKas",
                        fontSize = 10.sp, color = IDEOutline)
                }
            }

            DevSection("Build Info")
            listOf(
                "Package"     to AppConstants.APP_PACKAGE,
                "Version"     to AppConstants.APP_VERSION,
                "compileSdk"  to "36", "minSdk"      to "26",
                "Kotlin"      to "2.2.0", "AGP"       to "8.11.1",
                "Sora Editor" to "0.23.4",
            ).forEach { (l, v) -> DevInfoRow(l, v) }
        }
    }
}

@Composable private fun DevSection(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
        color = IDEPrimary, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable private fun StatChip(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)), modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(label, fontSize = 9.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable private fun DevInfoRow(label: String, value: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = IDESurface, shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = IDEOnSurface, fontSize = 12.sp, modifier = Modifier.width(100.dp))
            Text(value, color = IDEOnBackground, fontSize = 12.sp,
                fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        }
    }
}
