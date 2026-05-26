// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mobile.ide.R
import com.mobile.ide.core.ui.theme.themeColors
import com.mobile.ide.core.utils.LogCatcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (Int, Int, Color, Boolean) -> Unit,
    initialModeIndex: Int = 0,
    initialThemeIndex: Int = 0,
    initialCustomColor: Color = Color(0xFF6750A4),
    initialIsCustom: Boolean = false,
) {
    val originMode = remember { initialModeIndex }
    val originTheme = remember { initialThemeIndex }
    val originColor = remember { initialCustomColor }
    val originIsCustom = remember { initialIsCustom }

    var selectedModeIndex by remember { mutableIntStateOf(initialModeIndex) }
    var selectedThemeIndex by remember { mutableIntStateOf(initialThemeIndex) }
    var showColorPicker by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(initialCustomColor) }

    fun applyThemeNow(mode: Int = selectedModeIndex, themeIdx: Int = selectedThemeIndex, color: Color = customColor) {
        val isCustom = themeIdx == themeColors.size
        onThemeSelected(mode, themeIdx, color, isCustom)
        LogCatcher.d("ThemeDebug_Preview", "Real-time preview: Mode=$mode, Theme=$themeIdx, Color=${color.value}")
    }

    Dialog(
        onDismissRequest = {
            onThemeSelected(originMode, originTheme, originColor, originIsCustom)
            onDismiss()
        }
    ) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.theme_select_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    stringResource(R.string.theme_mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                val modeOptions =
                    listOf(
                        stringResource(R.string.settings_mode_system),
                        stringResource(R.string.settings_mode_light),
                        stringResource(R.string.settings_mode_dark),
                    )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modeOptions.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedModeIndex == index,
                            onClick = {
                                selectedModeIndex = index
                                applyThemeNow(mode = index)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modeOptions.size),
                            icon = {},
                        ) {
                            Text(label)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    stringResource(R.string.theme_color),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    themeColors.forEachIndexed { index, theme ->
                        com.mobile.ide.ui.welcome.ThemePreviewCard(
                            theme = theme,
                            isSelected = selectedThemeIndex == index,
                            onClick = {
                                selectedThemeIndex = index
                                applyThemeNow(themeIdx = index)
                            },
                        )
                    }
                    com.mobile.ide.ui.welcome.CustomThemeCard(
                        isSelected = selectedThemeIndex == themeColors.size,
                        onClick = {
                            selectedThemeIndex = themeColors.size
                            applyThemeNow(themeIdx = themeColors.size)
                            showColorPicker = true
                        },
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            onThemeSelected(originMode, originTheme, originColor, originIsCustom)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.theme_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = { onDismiss() }) { Text(stringResource(R.string.theme_done)) }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = customColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                customColor = color
                showColorPicker = false
                selectedThemeIndex = themeColors.size
                applyThemeNow(themeIdx = themeColors.size, color = color)
            },
        )
    }
}
