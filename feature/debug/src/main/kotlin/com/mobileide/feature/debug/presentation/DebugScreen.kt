package com.mobileide.debug.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import com.mobileide.debug.data.LogMessage

@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val filteredLogs = state.logs.filter { it.message.contains(state.filter, ignoreCase = true) || it.tag.contains(state.filter, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = state.filter,
            onValueChange = { viewModel.onEvent(DebugEvent.FilterChanged(it)) },
            label = { Text("Filter logs...") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredLogs) { log ->
                LogMessageRow(log)
            }
        }
    }
}

@Composable
fun LogMessageRow(log: LogMessage) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "${log.tag}: ",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = log.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = when (log.priority) {
                android.util.Log.ERROR -> Color.Red
                android.util.Log.WARN -> Color(0xFFFFA500) // Orange
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
