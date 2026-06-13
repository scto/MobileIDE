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

package com.scto.mobile.ide.ui.terminal

import android.annotation.SuppressLint
import android.app.Application
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

// 🔥 Global VirtualKeysView reference, used by TerminalBackEnd to read Ctrl/Alt key states
var virtualKeysView: WeakReference<VirtualKeysView>? = null

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isEnvironmentReady by remember { mutableStateOf(false) }
    val isSystemDark = isSystemInDarkTheme()

    // === Initialization Logic ===
    LaunchedEffect(Unit) {
        if (application == null) application = context.applicationContext as Application
        withContext(Dispatchers.IO) { SetupWorker.prepareEnvironment(context) }
        isEnvironmentReady = true
        if (SessionManager.sessions.isEmpty()) {
            SessionManager.addNewSession(context)
        }
    }

    if (!isEnvironmentReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val currentSession = SessionManager.currentSession
    var terminalViewRef by remember { mutableStateOf<WeakReference<TerminalView>?>(null) }

    val buttonTextColor = if (isSystemDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    val buttonBgColor = if (isSystemDark) 0xFF21222C.toInt() else 0xFFE0E0E0.toInt()

    Scaffold(
        modifier =
            Modifier.fillMaxSize()
                // Only handle left/right safe areas, leave top and bottom to topBar/bottomBar
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),

        // === Top Area ===
        topBar = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding() // 1. Ensure status bar is avoided
            ) {
                // 1. Title bar
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

                // 2. Tabs + Add Button
                // 🔥 As per your request:
                // 1. Set a fixed height (45dp) to prevent taking too much space
                // 2. Remove CenterVertically, change to Bottom alignment, right against the divider
                Row(
                    modifier = Modifier.fillMaxWidth().height(45.dp),
                    verticalAlignment = Alignment.Bottom, // 🔥 Core modification: Bottom alignment
                ) {
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
                                    height = 3.dp, // Indicator slightly thicker for better clarity
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
                                    // Tab content
                                    Row(
                                        // 🔥 Remove vertical centering, let text fall naturally
                                        // Add slight padding to adjust left/right and bottom spacing
                                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                                    ) {
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

                    // Vertical divider
                    VerticalDivider(
                        modifier =
                            Modifier.padding(vertical = 10.dp) // Top and bottom whitespace
                                .height(20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    // Add button
                    // Because the Row is bottom-aligned, the button might sink to the bottom.
                    // Here we still center the button slightly for a better look, or just let it sink.
                    Box(
                        modifier =
                            Modifier.size(45.dp) // Width and height fill the Row
                                .clickable { coroutineScope.launch { SessionManager.addNewSession(context) } },
                        contentAlignment = Alignment.Center, // Button icon centered in the grid
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.terminal_new_session),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // 3. Bottom divider (Tabs are right above this line)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        },

        // === Bottom Virtual Keys ===
        bottomBar = {
            Surface(
                color = Color(buttonBgColor),
                modifier =
                    Modifier.fillMaxWidth().imePadding(), // Ensure it is pushed up by the IME (Input Method Editor)
            ) {
                val pagerState = rememberPagerState(pageCount = { 2 })
                HorizontalPager(state = pagerState, modifier = Modifier.height(75.dp), userScrollEnabled = true) { page
                    ->
                    when (page) {
                        0 -> {
                            AndroidView(
                                factory = { ctx ->
                                    VirtualKeysView(ctx, null).apply {
                                        // 🔥 Set global reference
                                        virtualKeysView = WeakReference(this)

                                        // 🔥 Use TerminalSession instead of TerminalView
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
                                                text = "" // Clear the input box, ready for next input
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
        // === Terminal Content Area ===
        // 🔥🔥🔥 The innerPadding here is crucial! It contains the height of the TopBar.
        // If padding is not added here, the TopBar will directly overlap the terminal.
        if (currentSession != null) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(innerPadding) // Padding passed from Scaffold must be applied
                        .background(Color(TerminalConfig.getBackgroundColor(isSystemDark)))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        TerminalView(ctx, null).apply {
                            terminalViewRef = WeakReference(this)
                            setTextSize(42)
                            setTypeface(TerminalFontManager.getTypeface(ctx))
                            keepScreenOn = true
                            isFocusable = true
                            isFocusableInTouchMode = true
                            attachSession(currentSession)
                            val client = TerminalBackEnd(this, ctx)
                            setTerminalViewClient(client)
                            currentSession.updateTerminalSessionClient(client)
                        }
                    },
                    update = { view ->
                        view.setTypeface(TerminalFontManager.getTypeface(context))
                        view.setBackgroundColor(TerminalConfig.getBackgroundColor(isSystemDark))
                        if (view.currentSession != currentSession) {
                            view.attachSession(currentSession)
                            val client = TerminalBackEnd(view, context)
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
