/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysConstants
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysInfo
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysView
import com.termux.view.TerminalView

@Composable
fun TerminalScreen(navController: NavController) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()

    // Status für den Ladebildschirm
    var isEnvironmentReady by remember { mutableStateOf(false) }

    // Startet die Setup-Logik im Hintergrund
    LaunchedEffect(Unit) {
        SetupWorker.prepareEnvironment(context)
        isEnvironmentReady = true

        if (SessionManager.sessions.isEmpty()) {
            SessionManager.addNewSession(context)
        }
    }

    if (!isEnvironmentReady) {
        // LADEBILDSCHIRM
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Initialisiere MobileIDE...")
            }
        }
    } else {
        // TERMINAL BEREIT
        val currentSessionIndex = SessionManager.currentSessionIndex
        if (SessionManager.sessions.isNotEmpty() && currentSessionIndex in SessionManager.sessions.indices) {
            val sessionWrapper = SessionManager.sessions[currentSessionIndex]
            val currentSession = sessionWrapper.session

            Column(
                modifier = Modifier.fillMaxSize().background(Color(TerminalConfig.getBackgroundColor(isSystemDark)))
            ) {

                // 1. TERMINAL VIEW
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { ctx ->
                        TerminalView(ctx, null).apply {
                            setTextSize(42)
                            setTypeface(TerminalFontManager.getTypeface(ctx))
                            keepScreenOn = true
                            isFocusable = true
                            isFocusableInTouchMode = true
                            attachSession(currentSession)
                        }
                    },
                    update = { view ->
                        view.setTypeface(TerminalFontManager.getTypeface(context))
                        view.setBackgroundColor(TerminalConfig.getBackgroundColor(isSystemDark))
                        if (view.currentSession != currentSession) {
                            view.attachSession(currentSession)
                            view.onScreenUpdated()
                        }
                    },
                )

                // 2. VIRTUAL KEYS VIEW
                AndroidView(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    factory = { ctx ->
                        VirtualKeysView(ctx, null).apply {
                            // Hier wird der VirtualKeysListener angebunden
                            val listener = VirtualKeysListener(currentSession)
                            setVirtualKeysViewClient(listener)
                            // Keys aus deiner TerminalConfig laden
                            try {
                                val info =
                                    VirtualKeysInfo(
                                        TerminalConfig.VIRTUAL_KEYS_JSON,
                                        null,
                                        VirtualKeysConstants.CONTROL_CHARS_ALIASES,
                                    )
                                reload(info)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                )
            }
        }
    }
}
