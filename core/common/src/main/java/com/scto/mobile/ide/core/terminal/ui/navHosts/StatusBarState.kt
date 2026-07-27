package com.scto.mobile.ide.core.terminal.ui.navHosts

import androidx.compose.runtime.mutableStateOf
import com.scto.mobile.ide.core.terminal.settings.Settings

var showStatusBar = mutableStateOf(Settings.statusBar)
var horizontal_statusBar = mutableStateOf(Settings.horizontal_statusBar)
