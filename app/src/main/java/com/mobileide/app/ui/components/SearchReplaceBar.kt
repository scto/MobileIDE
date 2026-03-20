package com.mobileide.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*

data class SearchState(
    val query: String = "",
    val replaceText: String = "",
    val caseSensitive: Boolean = false,
    val useRegex: Boolean = false,
    val matchCount: Int = 0,
    val currentMatch: Int = 0
)

@Composable
fun SearchReplaceBar(
    visible: Boolean,
    textContent: String,
    onClose: () -> Unit,
    onApplyReplace: (newContent: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var state      by remember { mutableStateOf(SearchState()) }
    var showReplace by remember { mutableStateOf(false) }
    val focusReq    = remember { FocusRequester() }

    // Recount matches whenever query or content changes
    val matches = remember(state.query, textContent, state.caseSensitive) {
        if (state.query.isEmpty()) emptyList()
        else {
            val flags = if (!state.caseSensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
            try {
                Regex(Regex.escape(state.query), flags).findAll(textContent).toList()
            } catch (e: Exception) { emptyList() }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            state = state.copy(matchCount = matches.size, currentMatch = 0)
            focusReq.requestFocus()
        }
    }

    LaunchedEffect(matches.size) {
        state = state.copy(matchCount = matches.size)
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit  = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(color = IDESurfaceVariant, tonalElevation = 4.dp) {
            Column(modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {

                // Search Row
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                    // Search field
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { state = state.copy(query = it) },
                        modifier = Modifier.weight(1f).focusRequester(focusReq),
                        placeholder = { Text("Search…", style = TextStyle(fontSize = 13.sp)) },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                            color = IDEOnBackground),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                Text(
                                    "${if (matches.isEmpty()) 0 else state.currentMatch + 1}/${matches.size}",
                                    style = TextStyle(fontSize = 11.sp, color = IDEOnSurface),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        },
                        colors = searchFieldColors(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (matches.isNotEmpty())
                                state = state.copy(currentMatch = (state.currentMatch + 1) % matches.size)
                        })
                    )

                    // Case sensitive toggle
                    IconToggleButton(
                        checked = state.caseSensitive,
                        onCheckedChange = { state = state.copy(caseSensitive = it) }
                    ) {
                        Text("Aa", style = TextStyle(fontSize = 12.sp,
                            color = if (state.caseSensitive) IDEPrimary else IDEOnSurface))
                    }

                    // Previous match
                    IconButton(
                        onClick = {
                            if (matches.isNotEmpty())
                                state = state.copy(currentMatch = (state.currentMatch - 1 + matches.size) % matches.size)
                        },
                        enabled = matches.isNotEmpty()
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, "Previous", Modifier.size(18.dp),
                            tint = if (matches.isNotEmpty()) IDEPrimary else IDEOutline)
                    }

                    // Next match
                    IconButton(
                        onClick = {
                            if (matches.isNotEmpty())
                                state = state.copy(currentMatch = (state.currentMatch + 1) % matches.size)
                        },
                        enabled = matches.isNotEmpty()
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "Next", Modifier.size(18.dp),
                            tint = if (matches.isNotEmpty()) IDEPrimary else IDEOutline)
                    }

                    // Toggle replace
                    IconButton(onClick = { showReplace = !showReplace }) {
                        Icon(Icons.Default.FindReplace, "Replace",
                            tint = if (showReplace) IDEPrimary else IDEOnSurface,
                            modifier = Modifier.size(18.dp))
                    }

                    // Close
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(18.dp), tint = IDEOnSurface)
                    }
                }

                // Replace Row
                AnimatedVisibility(visible = showReplace) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = state.replaceText,
                            onValueChange = { state = state.copy(replaceText = it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Replace with…", style = TextStyle(fontSize = 13.sp)) },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                                color = IDEOnBackground),
                            colors = searchFieldColors()
                        )
                        // Replace current
                        TextButton(
                            onClick = {
                                if (matches.isNotEmpty()) {
                                    val m = matches[state.currentMatch]
                                    val newContent = textContent.substring(0, m.range.first) +
                                            state.replaceText +
                                            textContent.substring(m.range.last + 1)
                                    onApplyReplace(newContent)
                                }
                            },
                            enabled = matches.isNotEmpty()
                        ) { Text("Replace", fontSize = 12.sp) }
                        // Replace all
                        TextButton(
                            onClick = {
                                if (state.query.isNotEmpty()) {
                                    val flags = if (!state.caseSensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
                                    val newContent = try {
                                        Regex(Regex.escape(state.query), flags)
                                            .replace(textContent, state.replaceText)
                                    } catch (e: Exception) { textContent }
                                    onApplyReplace(newContent)
                                }
                            },
                            enabled = state.query.isNotEmpty()
                        ) { Text("All", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun searchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = IDEPrimary,
    unfocusedBorderColor = IDEOutline,
    focusedContainerColor   = IDEBackground,
    unfocusedContainerColor = IDEBackground,
    cursorColor          = IDEPrimary
)
