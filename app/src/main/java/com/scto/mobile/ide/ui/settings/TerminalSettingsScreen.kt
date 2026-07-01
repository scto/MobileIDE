package com.scto.mobile.ide.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import androidx.navigation.NavController
import com.scto.mobile.ide.R
import com.scto.mobile.ide.ui.terminal.SetupWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val generalPrefs = remember { context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE) }

    var selectedDistro by remember { mutableStateOf(generalPrefs.getString("selected_distro", "ubuntu") ?: "ubuntu") }
    var isReinstalling by remember { mutableStateOf(false) }
    var reinstallDownloadedBytes by remember { mutableLongStateOf(0L) }
    var reinstallTotalBytes by remember { mutableLongStateOf(-1L) }
    var reinstallStatus by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val editorPrefs = remember { context.getSharedPreferences("MobileIDE_Editor_Settings", Context.MODE_PRIVATE) }
    var lspEnabled by remember { mutableStateOf(editorPrefs.getBoolean("editor_lsp_enabled", false)) }

    var fontSize by remember { mutableFloatStateOf(com.rk.settings.Settings.terminal_font_size.toFloat()) }
    var scrollbackLines by remember {
        mutableFloatStateOf(com.rk.settings.Settings.terminal_scrollback_lines.toFloat())
    }
    var closeBehavior by remember { mutableStateOf(com.rk.settings.Settings.terminal_close_behavior) }
    var colorscheme by remember { mutableStateOf(com.rk.settings.Settings.terminal_colorscheme) }

    LaunchedEffect(lspEnabled) { editorPrefs.edit { putBoolean("editor_lsp_enabled", lspEnabled) } }
    LaunchedEffect(fontSize) { com.rk.settings.Settings.terminal_font_size = fontSize.toInt() }
    LaunchedEffect(scrollbackLines) { com.rk.settings.Settings.terminal_scrollback_lines = scrollbackLines.toInt() }
    LaunchedEffect(closeBehavior) { com.rk.settings.Settings.terminal_close_behavior = closeBehavior }
    LaunchedEffect(colorscheme) { com.rk.settings.Settings.terminal_colorscheme = colorscheme }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_terminal_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
            TerminalSettingsItem(
                selectedDistro = selectedDistro,
                onDistroSelected = { distro ->
                    selectedDistro = distro
                    generalPrefs.edit { putString("selected_distro", distro) }
                    Toast.makeText(
                            context,
                            "Distribution auf $distro geändert. Bitte Terminal neu installieren.",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                },
                onReset = {
                    SetupWorker.resetTerminal(context)
                    Toast.makeText(context, R.string.toast_terminal_reset_success, Toast.LENGTH_SHORT).show()
                },
                isReinstalling = isReinstalling,
                reinstallDownloaded = reinstallDownloadedBytes,
                reinstallTotal = reinstallTotalBytes,
                reinstallStatus = reinstallStatus,
                onReinstall = {
                    isReinstalling = true
                    reinstallDownloadedBytes = 0L
                    reinstallTotalBytes = -1L
                    reinstallStatus = "Reinstallation wird gestartet..."
                    Toast.makeText(context, R.string.toast_terminal_reinstall_start, Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        SetupWorker.reinstallTerminal(
                            context = context,
                            onStatusChanged = { status -> reinstallStatus = status },
                            onProgress = { downloaded, total ->
                                reinstallDownloadedBytes = downloaded
                                reinstallTotalBytes = total
                            },
                        )
                        isReinstalling = false
                        Toast.makeText(context, R.string.toast_terminal_reinstall_success, Toast.LENGTH_SHORT).show()
                    }
                },
                lspEnabled = lspEnabled,
                onLspEnabledChange = { lspEnabled = it },
                fontSize = fontSize,
                onFontSizeChange = { fontSize = it },
                scrollbackLines = scrollbackLines,
                onScrollbackLinesChange = { scrollbackLines = it },
                closeBehavior = closeBehavior,
                onCloseBehaviorChange = { closeBehavior = it },
                colorscheme = colorscheme,
                onColorschemeChange = { colorscheme = it },
            )
        }
    }
}
