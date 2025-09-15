package com.mobileide.terminal.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(viewModel: TerminalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Ausgabe-Bereich
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .padding(8.dp)
        ) {
            items(state.outputLines) { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
            // Scrolle automatisch nach unten, wenn neue Zeilen hinzukommen
            LaunchedEffect(state.outputLines.size) {
                coroutineScope.launch {
                    if (state.outputLines.isNotEmpty()) {
                        listState.animateScrollToItem(state.outputLines.lastIndex)
                    }
                }
            }
        }

        // Eingabe-Bereich
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            label = { Text("Befehl eingeben...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                viewModel.onEvent(TerminalEvent.ExecuteCommand(inputText))
                inputText = ""
            })
        )
    }
}
