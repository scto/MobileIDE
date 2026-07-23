package com.scto.mobile.ide.settings.runners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import android.widget.Toast
import com.scto.mobile.ide.core.terminal.ui.components.InfoBlock
import com.scto.mobile.ide.core.terminal.ui.components.SettingsItem
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.core.terminal.resources.getString
import com.scto.mobile.ide.core.terminal.resources.R
import com.scto.mobile.ide.runner.RunnerManager
import com.scto.mobile.ide.runner.ShellBasedRunner
import com.scto.mobile.ide.runner.ShellBasedRunners
import com.scto.mobile.ide.core.common.utils.openDocs
import kotlinx.coroutines.launch

@Composable
fun RunnerSettings(modifier: Modifier = Modifier, navController: NavController) {
    var showDialog by remember { mutableStateOf(false) }
    var runnerName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var regexError by remember { mutableStateOf<String?>(null) }
    var isEditingExisting by remember { mutableStateOf<ShellBasedRunner?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val nameFocusRequester = remember { FocusRequester() }
    val regexFocusRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var regexFieldValue by remember { mutableStateOf(TextFieldValue("", selection = TextRange(runnerName.length))) }

    // Validation functions
    fun validateName() {
        nameError =
            when {
                runnerName.isBlank() -> {
                    R.string.name_empty_err.getString()
                }
                !runnerName.matches("^[a-zA-Z0-9_-]+$".toRegex()) -> {
                    R.string.invalid_runner_name.getString()
                }
                else -> null
            }
    }

    fun String.isValidRegex(): Boolean =
        try {
            Regex(this)
            true
        } catch (_: Exception) {
            false
        }

    fun validateRegex() {
        regexError =
            if (regexFieldValue.text.isValidRegex()) {
                null
            } else {
                R.string.invalid_regex.getString()
            }
    }

    fun resetDialogState() {
        runnerName = ""
        regexFieldValue = regexFieldValue.copy(text = "")
        nameError = null
        regexError = null
        isEditingExisting = null
    }

    LaunchedEffect(Unit) {
        isLoading = true
        ShellBasedRunners.indexRunners()
        isLoading = false
    }

    PreferenceLayout(
        label = stringResource(R.string.runners),
        fab = {
            FloatingActionButton(
                onClick = {
                    resetDialogState()
                    showDialog = true
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) {
        InfoBlock(
            icon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
            text = stringResource(R.string.info_runners),
        )

        PreferenceGroup(heading = stringResource(R.string.built_in)) {
            RunnerManager.builtinRunners.forEach { runner ->
                SettingsItem(
                    label = runner.label,
                    description = runner.description,
                    default = runner.isEnabled(),
                    sideEffect = { runner.setEnabled(it) },
                    onClick = runner.onConfigure,
                )
            }
        }

        if (RunnerManager.extensionRunners.isNotEmpty()) {
            PreferenceGroup(heading = stringResource(R.string.ext)) {
                RunnerManager.extensionRunners.forEach { runner ->
                    SettingsItem(
                        label = runner.label,
                        description = runner.description,
                        startWidget = null,
                        default = runner.isEnabled(),
                        sideEffect = { runner.setEnabled(it) },
                        onClick = runner.onConfigure,
                    )
                }
            }
        }

        PreferenceGroup(heading = stringResource(R.string.external)) {
            val scope = rememberCoroutineScope()
            if (isLoading) {
                SettingsItem(
                    modifier = Modifier,
                    label = stringResource(R.string.loading),
                    default = false,
                    sideEffect = {},
                    showSwitch = false,
                    startWidget = {},
                )
            } else {
                if (ShellBasedRunners.runners.isEmpty()) {
                    SettingsItem(
                        modifier = Modifier,
                        label = stringResource(R.string.no_runners),
                        default = false,
                        sideEffect = {},
                        showSwitch = false,
                        startWidget = {},
                    )
                } else {
                    ShellBasedRunners.runners.forEach { runner ->
                        SettingsItem(
                            modifier = Modifier,
                            label = runner.label,
                            description = null,
                            default = runner.isEnabled(),
                            onClick = {
                                Toast.makeText(context, R.string.tab_opened.getString(), Toast.LENGTH_SHORT).show()
                            },
                            sideEffect = { runner.setEnabled(it) },
                            endWidget = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    IconButton(
                                        onClick = {
                                            isEditingExisting = runner
                                            runnerName = runner.label
                                            regexFieldValue =
                                                regexFieldValue.copy(
                                                    text = runner.regex,
                                                    selection = TextRange(runner.regex.length),
                                                )
                                            showDialog = true
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                                    }

                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                ShellBasedRunners.deleteRunner(runner)
                                                // Refresh list
                                                isLoading = true
                                                ShellBasedRunners.indexRunners()
                                                isLoading = false
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    resetDialogState()
                },
                title = {
                    Text(stringResource(if (isEditingExisting != null) R.string.edit_runner else R.string.new_runner))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = runnerName,
                            onValueChange = {
                                runnerName = it
                                validateName()
                            },
                            label = { Text(stringResource(R.string.runner_name)) },
                            modifier = Modifier.focusRequester(nameFocusRequester),
                            isError = nameError != null,
                            supportingText =
                                if (nameError != null) {
                                    {
                                        Text(
                                            text = nameError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        )
                                    }
                                } else null,
                            trailingIcon =
                                if (nameError != null) {
                                    {
                                        Icon(
                                            Icons.Default.Info,
                                            stringResource(R.string.error),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                } else null,
                            enabled = isEditingExisting == null, // Disable name editing for existing runners
                            readOnly = isEditingExisting != null,
                        )

                        OutlinedTextField(
                            value = regexFieldValue,
                            onValueChange = {
                                regexFieldValue = it
                                validateRegex()
                            },
                            label = { Text(stringResource(R.string.runner_regex)) },
                            modifier = Modifier.focusRequester(regexFocusRequester),
                            isError = regexError != null,
                            supportingText =
                                if (regexError != null) {
                                    {
                                        Text(
                                            text = regexError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                } else null,
                            trailingIcon =
                                if (regexError != null) {
                                    {
                                        Icon(
                                            Icons.Default.Info,
                                            stringResource(R.string.error),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                } else null,
                        )

                        LaunchedEffect(showDialog) {
                            if (isEditingExisting == null) {
                                nameFocusRequester.requestFocus()
                            } else {
                                regexFocusRequester.requestFocus()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = nameError == null && regexError == null && runnerName.isNotBlank(),
                        onClick = {
                            // Check for duplicate names only when creating new runner
                            if (isEditingExisting == null) {
                                if (ShellBasedRunners.runners.any { it.label == runnerName }) {
                                    nameError = R.string.runner_name_exists.getString()
                                    return@TextButton
                                }
                            }

                            scope.launch {
                                if (isEditingExisting == null) {
                                    // Create new runner
                                    val runner = ShellBasedRunner(label = runnerName, regex = regexFieldValue.text)
                                    val created = ShellBasedRunners.newRunner(runner)
                                    if (created) {
                                        // Refresh list
                                        isLoading = true
                                        ShellBasedRunners.indexRunners()
                                        isLoading = false
                                        showDialog = false
                                        resetDialogState()
                                    } else {
                                        Toast.makeText(context, R.string.failed.getString(), Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Update existing runner
                                    val updatedRunner =
                                        ShellBasedRunner(
                                            label = runnerName, // Name remains the same
                                            regex = regexFieldValue.text,
                                        )
                                    ShellBasedRunners.deleteRunner(isEditingExisting!!)
                                    val updated = ShellBasedRunners.newRunner(updatedRunner)
                                    if (updated) {
                                        // Refresh list
                                        isLoading = true
                                        ShellBasedRunners.indexRunners()
                                        isLoading = false
                                        showDialog = false
                                        resetDialogState()
                                    } else {
                                        Toast.makeText(context, R.string.failed.getString(), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                    ) {
                        Text(stringResource(if (isEditingExisting != null) R.string.save else R.string.create))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            resetDialogState()
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}
