package com.scto.mobile.ide.core.terminal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rk.components.compose.preferences.base.PreferenceTemplate

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    label: String,
    description: String? = null,
    startWidget: (@Composable () -> Unit)? = null,
    default: Boolean? = null,
    showSwitch: Boolean = true,
    sideEffect: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    endWidget: (@Composable () -> Unit)? = null
) {
    var checked by remember { mutableStateOf(default ?: false) }
    val interactionSource = remember { MutableInteractionSource() }

    PreferenceTemplate(
        modifier = modifier.clickable(
            indication = ripple(),
            interactionSource = interactionSource,
            onClick = {
                if (onClick != null) {
                    onClick()
                } else if (default != null) {
                    checked = !checked
                    sideEffect?.invoke(checked)
                }
            }
        ),
        contentModifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp)
            .padding(start = if (startWidget == null) 16.dp else 8.dp),
        title = {
            ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) {
                Text(text = label)
            }
        },
        description = {
            if (description != null) {
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                ) {
                    Text(text = description)
                }
            }
        },
        startWidget = {
            if (startWidget != null) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    startWidget()
                }
            }
        },
        endWidget = {
            if (endWidget != null) {
                endWidget()
            } else if (default != null && showSwitch) {
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        sideEffect?.invoke(it)
                    }
                )
            }
        },
        enabled = true,
        applyPaddings = false
    )
}
