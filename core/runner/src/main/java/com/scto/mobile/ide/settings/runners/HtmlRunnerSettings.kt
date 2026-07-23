package com.scto.mobile.ide.settings.runners

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scto.mobile.ide.core.terminal.ui.components.SettingsItem
import com.scto.mobile.ide.core.terminal.ui.components.SingleInputDialog
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.core.terminal.resources.getString
import com.scto.mobile.ide.core.terminal.resources.R
import com.scto.mobile.ide.core.terminal.settings.Settings

@Composable
fun HtmlRunnerSettings(modifier: Modifier = Modifier) {
    var showPortDialog by remember { mutableStateOf(false) }
    var portValue by remember { mutableStateOf(Settings.http_server_port.toString()) }
    var portError by remember { mutableStateOf<String?>(null) }

    PreferenceLayout(label = stringResource(R.string.html_preview)) {
        PreferenceGroup {
            SettingsItem(
                label = stringResource(R.string.launch_in_browser),
                description = stringResource(R.string.launch_in_browser_desc),
                default = Settings.launch_in_browser,
                sideEffect = { Settings.launch_in_browser = it },
            )

            SettingsItem(
                label = stringResource(R.string.inject_eruda),
                description = stringResource(R.string.inject_eruda_desc),
                default = Settings.inject_eruda,
                sideEffect = { Settings.inject_eruda = it },
            )

            SettingsItem(
                label = stringResource(R.string.server_port),
                description = stringResource(R.string.server_port_desc),
                default = false,
                showSwitch = false,
                onClick = { showPortDialog = true },
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showPortDialog) {
        SingleInputDialog(
            title = stringResource(R.string.server_port),
            inputLabel = stringResource(R.string.server_port),
            inputValue = portValue,
            errorMessage = portError,
            confirmEnabled = portValue.isNotBlank(),
            onInputValueChange = {
                portValue = it
                portError = null
                val portInt = portValue.toIntOrNull()
                if (portValue.isBlank() || portInt == null || portInt !in 0..65535) {
                    portError = R.string.invalid_port.getString()
                }
            },
            onConfirm = { Settings.http_server_port = portValue.toInt() },
            onFinish = {
                portValue = Settings.http_server_port.toString()
                portError = null
                showPortDialog = false
            },
        )
    }
}
