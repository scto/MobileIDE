package com.scto.mobile.ide.settings.lsp





import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.icons.Error
import com.scto.mobile.ide.icons.XedIcons











@Composable
fun ProcessServerSection(dialogState: ExternalLspDialogState) {
    OutlinedTextField(
        value = dialogState.lspCommand,
        onValueChange = {
            dialogState.lspCommand = it
            dialogState.externalError = null

            if (dialogState.lspCommand.isBlank()) {
                dialogState.externalError = com.scto.mobile.ide.core.main.R.string.empty_command.getString()
            }
        },
        label = { Text(stringResource(com.scto.mobile.ide.core.main.R.string.command)) },
        singleLine = true,
        isError = dialogState.externalError != null,
        supportingText =
            if (dialogState.externalError != null) {
                {
                    Text(
                        text = dialogState.externalError!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else null,
        trailingIcon =
            if (dialogState.externalError != null) {
                { Icon(XedIcons.Error, stringResource(com.scto.mobile.ide.core.main.R.string.error), tint = MaterialTheme.colorScheme.error) }
            } else null,
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = dialogState.lspExtensions,
        onValueChange = { newValue -> dialogState.onExtensionsChange(newValue) },
        label = { Text(stringResource(com.scto.mobile.ide.core.main.R.string.file_ext_example)) },
        singleLine = true,
        isError = dialogState.extensionsError != null,
        supportingText =
            if (dialogState.extensionsError != null) {
                {
                    Text(
                        text = dialogState.extensionsError!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else null,
        trailingIcon =
            if (dialogState.extensionsError != null) {
                { Icon(XedIcons.Error, stringResource(com.scto.mobile.ide.core.main.R.string.error), tint = MaterialTheme.colorScheme.error) }
            } else null,
    )
}
