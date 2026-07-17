package com.scto.mobile.ide.settings.terminal

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.scto.mobile.ide.core.terminal.resources.strings
import com.scto.mobile.ide.core.terminal.settings.Preference
import com.scto.mobile.ide.core.terminal.settings.Settings
import com.scto.mobile.ide.core.common.utils.isSystemInDarkTheme
import com.scto.mobile.ide.core.common.utils.openUrl
import com.scto.mobile.ide.core.terminal.ui.components.InfoBlock

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
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    DisposableEffect(Unit) { onDispose { keyboardController?.hide() } }

    var extraKeysText by remember { mutableStateOf(Settings.terminal_extra_keys) }

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
                    actions = {
                        IconButton(onClick = {
                            Preference.removeKey("terminal_extra_keys")
                            extraKeysText = DEFAULT_TERMINAL_EXTRA_KEYS
                            Settings.terminal_extra_keys = DEFAULT_TERMINAL_EXTRA_KEYS
                        }) {
                            Icon(Icons.Default.Refresh, stringResource(strings.reset))
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            InfoBlock(
                text = stringResource(strings.see_termux_extra_keys),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable {
                    val url = "https://wiki.termux.com/wiki/Touch_Keyboard#Extra_Keys_Row"
                    context.openUrl(url)
                }
            )

            TextField(
                value = extraKeysText,
                onValueChange = {
                    extraKeysText = it
                    Settings.terminal_extra_keys = it
                },
                modifier = Modifier.fillMaxSize().imePadding(),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )
        }
    }
}
