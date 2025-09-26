package com.mobileide.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import com.mobileide.settings.presentation.AppTheme
import com.mobileide.settings.presentation.SettingsEvent
import com.mobileide.settings.presentation.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("App-Theme", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

        AppTheme.values().forEach { theme ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (state.settings.theme == theme),
                        onClick = { viewModel.onEvent(SettingsEvent.OnThemeChanged(theme)) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (state.settings.theme == theme),
                    onClick = { viewModel.onEvent(SettingsEvent.OnThemeChanged(theme)) }
                )
                Text(
                    text = theme.name.toLowerCase().capitalize(),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
