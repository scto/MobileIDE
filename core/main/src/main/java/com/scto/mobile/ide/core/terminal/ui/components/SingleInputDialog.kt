package com.scto.mobile.ide.core.terminal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.scto.mobile.ide.core.terminal.resources.strings

@Composable
fun SingleInputDialog(
    title: String,
    inputLabel: String,
    inputValue: String,
    errorMessage: String? = null,
    confirmEnabled: Boolean = true,
    onInputValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onFinish: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onFinish() },
        title = { Text(text = title) },
        text = {
            Column {
                OutlinedTextField(
                    value = inputValue,
                    singleLine = true,
                    onValueChange = onInputValueChange,
                    label = { Text(inputLabel) },
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (confirmEnabled) {
                                onConfirm()
                                onFinish()
                            }
                        }
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    )
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onFinish()
                },
                enabled = confirmEnabled
            ) {
                Text(stringResource(strings.apply))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onFinish() }) { Text(stringResource(strings.cancel)) }
        },
    )
}
