package com.scto.mobile.ide.core.terminal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rk.components.compose.preferences.base.PreferenceTemplate

@Composable
fun ValueSlider(
    modifier: Modifier = Modifier,
    label: String,
    min: Int,
    max: Int,
    default: Int,
    onValueChanged: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(default.toFloat()) }

    PreferenceTemplate(
        modifier = modifier,
        title = {
            Text(text = label, fontWeight = FontWeight.Bold)
        },
        description = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onValueChanged(it.toInt())
                    },
                    valueRange = min.toFloat()..max.toFloat(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = sliderValue.toInt().toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        },
        applyPaddings = true
    )
}
