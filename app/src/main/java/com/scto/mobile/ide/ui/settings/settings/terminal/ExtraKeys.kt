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

package com.scto.mobile.ide.ui.settings.terminal

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView

import com.rk.components.ResetButton
import com.rk.editor.Editor
import com.rk.file.BuiltinFileType
import com.rk.resources.drawables
import com.rk.resources.strings
import com.rk.settings.Preference
import com.rk.settings.Settings
import com.rk.tabs.editor.EditorNotice
import com.rk.utils.isSystemInDarkTheme
import com.rk.utils.openUrl

import io.github.rosemoe.sora.event.ContentChangeEvent

import java.lang.ref.WeakReference

import kotlinx.coroutines.launch

const val DEFAULT_TERMINAL_EXTRA_KEYS =
    ("[" +
        "\n  [" +
        "\n    \"ESC\"," +
        "\n    {" +
        "\n      \"key\": \"/\"," +
        "\n      \"popup\": \"\\\\\"" +
        "\n    }," +
        "\n    {" +
        "\n      \"key\": \"-\"," +
        "\n      \"popup\": \"|\"" +
        "\n    }," +
        "\n    \"HOME\"," +
        "\n    \"UP\"," +
        "\n    \"END\"," +
        "\n    \"PGUP\"" +
        "\n  ]," +
        "\n  [" +
        "\n    \"TAB\"," +
        "\n    \"CTRL\"," +
        "\n    \"ALT\"," +
        "\n    \"LEFT\"," +
        "\n    \"DOWN\"," +
        "\n    \"RIGHT\"," +
        "\n    \"PGDN\"" +
        "\n  ]" +
        "\n]")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalExtraKeys() {
    val scope = rememberCoroutineScope()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var editorRef = remember { WeakReference<Editor?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            editorRef.get()?.release()
            editorRef = WeakReference(null)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { Text(stringResource(strings.change_extra_keys)) },
                    actions = { ResetButton { resetFiles(editorRef.get()) } },
                )
                HorizontalDivider()
            }
        }
    ) { paddingValues ->
        val selectionColors = LocalTextSelectionColors.current
        val isDarkMode = isSystemInDarkTheme(context)
        val colorScheme = MaterialTheme.colorScheme

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            EditorNotice(
                text = stringResource(strings.see_termux_extra_keys),
                actionButton = {
                    IconButton(
                        onClick = {
                            val url = "https://wiki.termux.com/wiki/Touch_Keyboard#Extra_Keys_Row"
                            context.openUrl(url)
                        }
                    ) {
                        Icon(
                            painter = painterResource(drawables.open_in_new),
                            contentDescription = stringResource(strings.open),
                        )
                    }
                },
            )

            AndroidView(
                modifier = Modifier.fillMaxSize().imePadding(),
                factory = { context ->
                    Editor(context).apply {
                        editorRef = WeakReference(this)

                        setTextSize(10f)
                        setText(Settings.terminal_extra_keys)
                        isWordwrap = false

                        subscribeAlways(ContentChangeEvent::class.java) {
                            Settings.terminal_extra_keys = it.editor.text.toString()
                        }

                        setThemeColors(
                            isDarkMode = isDarkMode,
                            selectionColors = selectionColors,
                            colorScheme = colorScheme,
                        )

                        scope.launch { configureLanguage(BuiltinFileType.JSON.textmateScope!!) }
                    }
                },
            )
        }
    }
}

/** Reset order of commands and symbols to default */
private fun resetFiles(editor: Editor?) {
    Preference.removeKey("terminal_extra_keys")
    editor?.setText(DEFAULT_TERMINAL_EXTRA_KEYS)
}
