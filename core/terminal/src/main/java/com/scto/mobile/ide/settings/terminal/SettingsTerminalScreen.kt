package com.scto.mobile.ide.settings.terminal

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.scto.mobile.ide.DocumentProvider
import com.scto.mobile.ide.activities.main.MainActivity
import com.scto.mobile.ide.activities.settings.SettingsActivity
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.activities.settings.settingsNavController
import com.scto.mobile.ide.components.NextScreenCard
import com.scto.mobile.ide.components.SettingsItem
import com.scto.mobile.ide.components.ValueSlider
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceTemplate
import com.scto.mobile.ide.components.compose.preferences.switch.PreferenceSwitch
import com.scto.mobile.ide.feature.FeatureRegistry
import com.scto.mobile.ide.file.child
import com.scto.mobile.ide.file.createFileIfNot
import com.scto.mobile.ide.file.localBinDir
import com.scto.mobile.ide.file.localDir
import com.scto.mobile.ide.file.localLibDir
import com.scto.mobile.ide.file.sandboxDir
import com.scto.mobile.ide.file.toFileObject
import com.scto.mobile.ide.resources.getString
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.terminal.terminalView
import com.scto.mobile.ide.utils.LoadingPopup
import com.scto.mobile.ide.utils.dialogRes
import com.scto.mobile.ide.utils.dpToPx
import com.scto.mobile.ide.utils.getTempDir
import com.scto.mobile.ide.utils.toast
import com.termux.terminal.TerminalEmulator
import java.io.File
import java.io.FileOutputStream
import java.lang.Runtime.getRuntime
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TerminalCursorStyle(val value: String, val stringRes: Int) {
    BLOCK("block", strings.block),
    BAR("bar", strings.bar),
    UNDERLINE("underline", strings.underline);

    companion object {
        fun fromString(value: String): TerminalCursorStyle {
            return entries.firstOrNull { it.value == value } ?: BLOCK
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun SettingsTerminalScreen(overrideNavController: NavController? = null) {
    PreferenceLayout(label = stringResource(id = strings.terminal), backArrowVisible = true) {
        val context = LocalContext.current
        val activity = LocalActivity.current as? AppCompatActivity

        PreferenceGroup(heading = stringResource(strings.advanced)) {
            if (FeatureRegistry.isEnabled("debug_mode")) {
                SettingsItem(
                    label = stringResource(strings.failsafe_mode),
                    description = stringResource(strings.failsafe_mode_desc),
                    default = !Settings.sandbox,
                    sideEffect = { Settings.sandbox = !it },
                )
            }

            var showSeccompDialog by remember { mutableStateOf(false) }
            var seccompMode by remember { mutableStateOf(Settings.seccomp_mode) }

            SettingsItem(
                label = "SECCOMP",
                description = stringResource(strings.seccomp_desc),
                default = false,
                showSwitch = false,
                onClick = { showSeccompDialog = true },
            )

            if (showSeccompDialog) {
                var tempSeccompMode by remember { mutableStateOf(seccompMode) }
                AlertDialog(
                    onDismissRequest = { showSeccompDialog = false },
                    title = { Text("SECCOMP") },
                    text = {
                        Column {
                            listOf(
                                    "unspecified" to strings.seccomp_unspecified,
                                    "no" to strings.seccomp_no_seccomp,
                                    "yes" to strings.seccomp_seccomp,
                                )
                                .forEach { (mode, stringRes) ->
                                    PreferenceTemplate(
                                        modifier =
                                            Modifier.clip(MaterialTheme.shapes.large).clickable {
                                                tempSeccompMode = mode
                                            },
                                        title = { Text(stringResource(stringRes)) },
                                        startWidget = {
                                            RadioButton(selected = tempSeccompMode == mode, onClick = null)
                                        },
                                    )
                                }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSeccompDialog = false
                                Settings.seccomp_mode = tempSeccompMode
                                seccompMode = tempSeccompMode
                            }
                        ) {
                            Text(stringResource(strings.apply))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSeccompDialog = false }) { Text(stringResource(strings.cancel)) }
                    },
                )
            }

            NextScreenCard(
                label = stringResource(strings.terminal_health),
                description = stringResource(strings.terminal_health_desc),
                navController = overrideNavController ?: settingsNavController.get(),
                route = SettingsRoutes.TerminalCheck,
            )
        }

        var showCursorStyleDialog by remember { mutableStateOf(false) }
        var cursorStyleValue by remember {
            mutableStateOf(TerminalCursorStyle.fromString(Settings.terminal_cursor_style))
        }

        PreferenceGroup(heading = stringResource(strings.appearance)) {
            ValueSlider(
                label = stringResource(strings.text_size),
                min = 10,
                max = 20,
                default = Settings.terminal_font_size,
                onValueChanged = {
                    Settings.terminal_font_size = it
                    terminalView.get()?.setTextSize(dpToPx(it.toFloat(), context))
                },
            )

            NextScreenCard(
                label = stringResource(strings.manage_terminal_font),
                description = stringResource(strings.manage_terminal_font),
                navController = overrideNavController ?: settingsNavController.get(),
                route = SettingsRoutes.TerminalFontScreen,
            )

            SettingsItem(
                label = stringResource(strings.cursor_style),
                description = stringResource(strings.cursor_style_desc),
                default = false,
                showSwitch = false,
                sideEffect = { showCursorStyleDialog = true },
            )
        }

        if (showCursorStyleDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCursorStyleDialog = false
                    cursorStyleValue = TerminalCursorStyle.fromString(Settings.terminal_cursor_style)
                },
                title = { Text(stringResource(strings.cursor_style)) },
                text = {
                    Column {
                        TerminalCursorStyle.entries.forEach {
                            PreferenceTemplate(
                                modifier =
                                    Modifier.clip(MaterialTheme.shapes.large).clickable { cursorStyleValue = it },
                                title = { Text(stringResource(it.stringRes)) },
                                startWidget = { RadioButton(selected = cursorStyleValue == it, onClick = null) },
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCursorStyleDialog = false
                            Settings.terminal_cursor_style = cursorStyleValue.value
                        }
                    ) {
                        Text(stringResource(strings.apply))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCursorStyleDialog = false
                            cursorStyleValue = TerminalCursorStyle.fromString(Settings.terminal_cursor_style)
                        }
                    ) {
                        Text(stringResource(strings.cancel))
                    }
                },
            )
        }

        PreferenceGroup(heading = stringResource(strings.user_data)) {
            val restore =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    if (uri == null) {
                        return@rememberLauncherForActivityResult
                    }

                    val loading = LoadingPopup(activity, null)
                    loading.show()

                    GlobalScope.launch(Dispatchers.IO) {
                        val fileObject = uri.toFileObject(expectedIsFile = true)

                        val tempFile = getTempDir().child("terminal-backup.tar.gz")

                        try {
                            fileObject.getInputStream().use { inputStream ->
                                FileOutputStream(tempFile).use { outputStream -> inputStream.copyTo(outputStream) }
                            }

                            sandboxDir().deleteRecursively()
                            sandboxDir().mkdirs()

                            val result =
                                getRuntime().exec("tar -xf ${tempFile.absolutePath} -C ${sandboxDir()}").waitFor()
                            withContext(Dispatchers.Main) {
                                loading.hide()
                                if (result == 0) {
                                    toast(strings.success)
                                } else {
                                    toast(strings.failed)
                                }
                            }

                            localDir().child(".terminal_setup_ok_DO_NOT_REMOVE").createFileIfNot()
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                loading.hide()
                                toast("Error: ${e.message}")
                            }
                        }
                    }
                }

            SettingsItem(
                label = stringResource(strings.backup),
                description = stringResource(strings.terminal_backup),
                showSwitch = false,
                default = false,
                sideEffect = {
                    val fileManager =
                        if (SettingsActivity.instance != null) {
                            SettingsActivity.instance!!.fileManager
                        } else {
                            MainActivity.instance!!.fileManager
                        }

                    fileManager.createNewFile(
                        mimeType = "application/octet-stream",
                        title = "terminal-backup.tar.gz",
                    ) { fileObject ->
                        GlobalScope.launch(Dispatchers.IO) {
                            if (fileObject != null) {
                                val targetFile = getTempDir().child("terminal-backup.tar.gz")

                                val loading = LoadingPopup(activity, null)
                                loading.show()

                                try {
                                    val sandboxDir = sandboxDir().absolutePath
                                    val targetPath = targetFile.absolutePath

                                    val processBuilder =
                                        ProcessBuilder(
                                                "tar",
                                                "-czf",
                                                targetPath,
                                                ".",
                                                "--exclude=dev",
                                                "--exclude=sys",
                                                "--exclude=proc",
                                                "--exclude=system",
                                                "--exclude=apex",
                                                "--exclude=vendor",
                                                "--exclude=data",
                                                "--exclude=home",
                                                "--exclude=root",
                                                "--exclude=var/cache",
                                                "--exclude=var/tmp",
                                                "--exclude=lost+found",
                                                "--exclude=storage",
                                                "--exclude=system_ext",
                                                "--exclude=tmp",
                                                "--exclude=vendor",
                                                "--exclude=sdcard",
                                                "--exclude=storage",
                                            )
                                            .apply {
                                                directory(File(sandboxDir))
                                                redirectErrorStream(true)
                                            }

                                    processBuilder.start().waitFor()

                                    loading.hide()

                                    targetFile.inputStream().use { inputStream ->
                                        fileObject.getOutPutStream(false).use { outputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        loading.hide()
                                        toast("Error: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                },
            )

            SettingsItem(
                label = stringResource(strings.restore),
                description = stringResource(strings.restore_terminal),
                showSwitch = false,
                default = false,
                sideEffect = { restore.launch("application/gzip") },
            )

            SettingsItem(
                label = stringResource(strings.uninstall),
                default = false,
                description = stringResource(strings.uninstall_terminal),
                showSwitch = false,
                sideEffect = {
                    dialogRes(
                        activity = activity,
                        title = strings.attention.getString(),
                        msg = strings.uninstall_terminal_warning.getString(),
                        onCancel = {},
                        okRes = strings.delete,
                        onOk = {
                            GlobalScope.launch(Dispatchers.IO) {
                                val loading = LoadingPopup(activity, null)
                                loading.show()
                                runCatching {
                                    localBinDir().deleteRecursively()
                                    localLibDir().deleteRecursively()
                                    sandboxDir().deleteRecursively()
                                    localDir().child(".terminal_setup_ok_DO_NOT_REMOVE").delete()
                                }
                                loading.hide()
                            }
                        },
                    )
                },
            )
        }

        PreferenceGroup(heading = stringResource(strings.other)) {
            NextScreenCard(
                label = stringResource(strings.change_extra_keys),
                description = stringResource(strings.change_extra_keys_desc),
                navController = overrideNavController ?: settingsNavController.get(),
                route = SettingsRoutes.TerminalExtraKeys,
            )

            SettingsItem(
                label = stringResource(strings.clipboard_keybindings),
                description = stringResource(strings.clipboard_keybindings_desc),
                default = Settings.terminal_clipboard_keybindings,
                sideEffect = { Settings.terminal_clipboard_keybindings = it },
            )

            ValueSlider(
                label = stringResource(strings.scrollback_buffer),
                description = stringResource(strings.scrollback_buffer_desc),
                min = TerminalEmulator.TERMINAL_TRANSCRIPT_ROWS_MIN,
                max = TerminalEmulator.TERMINAL_TRANSCRIPT_ROWS_MAX,
                default = Settings.terminal_scrollback_buffer,
                useSteps = false,
            ) {
                Settings.terminal_scrollback_buffer = it
                toast(strings.restart_required)
            }

            SettingsItem(
                label = stringResource(strings.terminate_all_sessions),
                description = stringResource(strings.terminate_all_sessions_desc),
                default = Settings.terminate_sessions_on_exit,
                sideEffect = { Settings.terminate_sessions_on_exit = it },
            )

            SettingsItem(
                label = stringResource(strings.project_as_wk),
                description = stringResource(strings.project_as_wk_desc),
                default = Settings.project_as_pwd,
                sideEffect = { Settings.project_as_pwd = it },
            )

            var exposeHomeDirState by remember { mutableStateOf(Settings.expose_home_dir) }
            PreferenceSwitch(
                checked = exposeHomeDirState,
                onCheckedChange = {
                    if (it) {
                        dialogRes(
                            activity = activity,
                            title = strings.attention.getString(),
                            msg = strings.saf_expose_warning.getString(),
                            okRes = strings.continue_action,
                            onCancel = {},
                            onOk = {
                                Settings.expose_home_dir = true
                                DocumentProvider.setDocumentProviderEnabled(context, true)
                                exposeHomeDirState = true
                            },
                        )
                    } else {
                        Settings.expose_home_dir = false
                        exposeHomeDirState = false
                        DocumentProvider.setDocumentProviderEnabled(context, false)
                    }
                },
                label = stringResource(strings.expose_saf),
                description = stringResource(strings.expose_saf_desc),
            )
        }
    }
}
