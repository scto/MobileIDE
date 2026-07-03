/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  Thomas Schmid  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.ui.terminal

import android.annotation.SuppressLint
import android.app.Application
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doOnTextChanged
import androidx.navigation.NavController
import com.rk.libcommons.application
import com.rk.terminal.ui.screens.terminal.TerminalBackEnd
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysConstants
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysInfo
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysView
import com.scto.mobile.ide.R
import com.scto.mobile.ide.ui.terminal.TerminalConfig.VIRTUAL_KEYS_JSON
import com.termux.view.TerminalView
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

var virtualKeysView: WeakReference<VirtualKeysView>? = null

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(navController: NavController) {
    val context = LocalContext.current
    var isEnvironmentReady by remember { mutableStateOf(false) }
    val isSystemDark = isSystemInDarkTheme()
    // Download progress state
    var downloadLabel by remember { mutableStateOf("") }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(-1L) }

    val prefs = remember { context.getSharedPreferences("MobileIDE_Settings", android.content.Context.MODE_PRIVATE) }
    val isFirstRun = remember { !prefs.getBoolean("first_run_distro_selected", false) }
    var showDistroDialog by rememberSaveable { mutableStateOf(isFirstRun) }
    var selectedDistroDialog by rememberSaveable {
        mutableStateOf(prefs.getString("selected_distro", "ubuntu") ?: "ubuntu")
    }

    if (showDistroDialog) {
        AlertDialog(
            onDismissRequest = { /* Nicht schließbar ohne Auswahl beim ersten Start */ },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            },
            title = {
                Text(
                    text = "Linux Distribution wählen",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Wähle die Linux-Distribution für dein Terminal aus:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    val distroOptions =
                        listOf(
                            Triple("ubuntu", "Ubuntu", "Einsteigerfreundlich, große Community"),
                            Triple("debian", "Debian", "Stabil & leichtgewichtig"),
                        )
                    distroOptions.forEach { (id, name, desc) ->
                        val isSelected = selectedDistroDialog == id
                        Surface(
                            modifier =
                                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    selectedDistroDialog = id
                                },
                            shape = MaterialTheme.shapes.medium,
                            color =
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedDistroDialog = id },
                                    colors =
                                        RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                            if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs
                            .edit()
                            .putString("selected_distro", selectedDistroDialog)
                            .putBoolean("first_run_distro_selected", true)
                            .apply()
                        showDistroDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("${selectedDistroDialog.replaceFirstChar { it.uppercase() }} verwenden")
                }
            },
        )
    }

    var setupError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        com.scto.mobile.ide.core.utils.LogCatcher.i(
            "TerminalScreen",
            "LaunchedEffect: initializing terminal environment...",
        )
        if (application == null) application = context.applicationContext as Application
        try {
            withContext(Dispatchers.IO) {
                SetupWorker.prepareEnvironment(
                    context = context,
                    onStatusChanged = { status ->
                        downloadLabel = status
                        com.scto.mobile.ide.core.utils.LogCatcher.i(
                            "TerminalScreen",
                            "prepareEnvironment status: $status",
                        )
                    },
                    onProgress = { downloaded, total ->
                        downloadedBytes = downloaded
                        totalBytes = total
                    },
                )
            }
            com.scto.mobile.ide.core.utils.LogCatcher.i(
                "TerminalScreen",
                "prepareEnvironment complete. environment is ready.",
            )
            isEnvironmentReady = true
            if (SessionManager.sessions.isEmpty()) {
                com.scto.mobile.ide.core.utils.LogCatcher.i(
                    "TerminalScreen",
                    "No active terminal sessions. Adding new session.",
                )
                SessionManager.addNewSession(context)
            }
        } catch (e: Exception) {
            setupError = e.message ?: "Ein unbekannter Fehler ist aufgetreten."
            com.scto.mobile.ide.core.utils.LogCatcher.e("TerminalScreen", "Setup failed", e)
        }
    }

    if (setupError != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Setup fehlgeschlagen", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(setupError!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { setupError = null }) { Text("Erneut versuchen") }
            }
        }
    } else if (!isEnvironmentReady) {
        DownloadProgressScreen(
            label = downloadLabel.ifBlank { "Vorbereitung…" },
            downloaded = downloadedBytes,
            total = totalBytes,
            archDesc = remember { Downloader.archDescription() },
        )
        return
    }

    val currentSession = SessionManager.currentSession
    var terminalViewRef by remember { mutableStateOf<WeakReference<TerminalView>?>(null) }

    val buttonTextColor = if (isSystemDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    val buttonBgColor = if (isSystemDark) 0xFF21222C.toInt() else 0xFFE0E0E0.toInt()

    Scaffold(
        modifier =
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.terminal_title),
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    windowInsets = WindowInsets(0.dp),
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                )

                Row(modifier = Modifier.fillMaxWidth().height(45.dp), verticalAlignment = Alignment.Bottom) {
                    if (SessionManager.sessions.isNotEmpty()) {
                        SecondaryScrollableTabRow(
                            selectedTabIndex = SessionManager.currentSessionIndex,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 0.dp,
                            divider = {},
                            modifier = Modifier.weight(1f),
                            indicator = {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(SessionManager.currentSessionIndex),
                                    height = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                        ) {
                            SessionManager.sessions.forEachIndexed { index, session ->
                                val isSelected = SessionManager.currentSessionIndex == index
                                Tab(
                                    selected = isSelected,
                                    onClick = { SessionManager.switchTo(index) },
                                    modifier = Modifier.fillMaxHeight(),
                                ) {
                                    Row(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color =
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.action_close),
                                                modifier =
                                                    Modifier.size(14.dp).clickable {
                                                        SessionManager.removeSession(session)
                                                    },
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    VerticalDivider(
                        modifier = Modifier.padding(vertical = 10.dp).height(20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Box(
                        modifier = Modifier.size(45.dp).clickable { SessionManager.addNewSession(context) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.terminal_new_session),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            Surface(color = Color(buttonBgColor), modifier = Modifier.fillMaxWidth().imePadding()) {
                val pagerState = rememberPagerState(pageCount = { 2 })
                HorizontalPager(state = pagerState, modifier = Modifier.height(75.dp), userScrollEnabled = true) { page
                    ->
                    when (page) {
                        0 -> {
                            AndroidView(
                                factory = { ctx ->
                                    VirtualKeysView(ctx, null).apply {
                                        virtualKeysView = WeakReference(this)
                                        virtualKeysViewClient = currentSession?.let { VirtualKeysListener(it) }
                                        setButtonTextAllCaps(true)
                                        reload(
                                            VirtualKeysInfo(
                                                VIRTUAL_KEYS_JSON,
                                                "",
                                                VirtualKeysConstants.CONTROL_CHARS_ALIASES,
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { view ->
                                    view.setButtonColors(
                                        buttonTextColor,
                                        0xFFf44336.toInt(),
                                        0x00000000,
                                        0xFF7F7F7F.toInt(),
                                    )
                                },
                            )
                        }
                        1 -> {
                            var text by rememberSaveable { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        EditText(ctx).apply {
                                            maxLines = 1
                                            isSingleLine = true
                                            imeOptions = EditorInfo.IME_ACTION_DONE
                                            background = null
                                            hint = context.getString(R.string.terminal_input_hint)
                                            setHintTextColor(
                                                if (isSystemDark) 0xFF888888.toInt() else 0xFFAAAAAA.toInt()
                                            )
                                            setTextColor(if (isSystemDark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
                                            doOnTextChanged { t, _, _, _ ->
                                                val inputChar = t.toString()
                                                if (inputChar.isNotEmpty()) {
                                                    val session = SessionManager.currentSession
                                                    session?.write(inputChar)
                                                }
                                                text = ""
                                            }
                                            setOnEditorActionListener { _, actionId, _ ->
                                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                                    val term = terminalViewRef?.get()
                                                    if (text.isEmpty())
                                                        term?.dispatchKeyEvent(
                                                            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                                                        )
                                                    else {
                                                        term?.mTermSession?.write(text)
                                                        setText("")
                                                    }
                                                    true
                                                } else false
                                            }
                                        }
                                    },
                                    update = {
                                        if (it.text.toString() != text) it.setText(text)
                                        it.setTextColor(if (isSystemDark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (currentSession != null) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(TerminalConfig.getBackgroundColor(isSystemDark)))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        TerminalView(ctx, null).apply {
                            terminalViewRef = WeakReference(this)
                            val fontSizePx =
                                (com.rk.settings.Settings.terminal_font_size *
                                        ctx.resources.displayMetrics.scaledDensity)
                                    .toInt()
                            setTextSize(fontSizePx)
                            setTypeface(TerminalFontManager.getTypeface(ctx))
                            keepScreenOn = true
                            isFocusable = true
                            isFocusableInTouchMode = true
                            attachSession(currentSession)
                            val client =
                                TerminalBackEnd(this, ctx) { finishedSession ->
                                    val wrapper = SessionManager.sessions.find { it.session == finishedSession }
                                    if (wrapper != null) {
                                        SessionManager.removeSession(wrapper)
                                    }
                                }
                            setTerminalViewClient(client)
                            currentSession.updateTerminalSessionClient(client)

                            val props = java.util.Properties()
                            try {
                                val scheme = com.rk.settings.Settings.terminal_colorscheme
                                ctx.assets.open("terminal/colorschemes/$scheme.properties").use { input ->
                                    props.load(input)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            com.termux.terminal.TerminalColors.COLOR_SCHEME.updateWith(props)
                            mEmulator?.mColors?.reset()
                        }
                    },
                    update = { view ->
                        view.setTypeface(TerminalFontManager.getTypeface(context))
                        val fontSizePx =
                            (com.rk.settings.Settings.terminal_font_size *
                                    context.resources.displayMetrics.scaledDensity)
                                .toInt()
                        view.setTextSize(fontSizePx)
                        view.setBackgroundColor(TerminalConfig.getBackgroundColor(isSystemDark))

                        val props = java.util.Properties()
                        try {
                            val scheme = com.rk.settings.Settings.terminal_colorscheme
                            context.assets.open("terminal/colorschemes/$scheme.properties").use { input ->
                                props.load(input)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        com.termux.terminal.TerminalColors.COLOR_SCHEME.updateWith(props)
                        view.mEmulator?.mColors?.reset()
                        view.onScreenUpdated()

                        if (view.currentSession != currentSession) {
                            view.attachSession(currentSession)
                            val client =
                                TerminalBackEnd(view, context) { finishedSession ->
                                    val wrapper = SessionManager.sessions.find { it.session == finishedSession }
                                    if (wrapper != null) {
                                        SessionManager.removeSession(wrapper)
                                    }
                                }
                            view.setTerminalViewClient(client)
                            currentSession.updateTerminalSessionClient(client)
                            view.onScreenUpdated()
                        }
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Download progress composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadProgressScreen(label: String, downloaded: Long, total: Long, archDesc: String) {
    val progressFraction =
        when {
            total > 0L -> (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            else -> -1f // indeterminate
        }

    fun Long.toMb() = "%.1f MB".format(this / 1_048_576.0)

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Terminal icon
            Icon(
                imageVector = Icons.Outlined.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )

            Text(
                text = "MobileIDE",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Progress bar
            if (progressFraction >= 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${downloaded.toMb()} / ${total.toMb()}  (${(progressFraction * 100).toInt()} %)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Architecture chip
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Arch: $archDesc",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
