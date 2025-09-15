package com.mobileide.svgtoavd.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SvgToAvdScreen(viewModel: SvgToAvdViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SVG to Vector Drawable", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.svgInput,
            onValueChange = { viewModel.onEvent(SvgToAvdEvent.SvgInputChanged(it)) },
            label = { Text("Paste SVG content here") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.onEvent(SvgToAvdEvent.ConvertClicked) },
            enabled = !state.isLoading && state.svgInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Convert")
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.avdOutput,
            onValueChange = { /* Output is read-only */ },
            label = { Text("Android Vector Drawable Output") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            readOnly = true,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
        )

        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
