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
import com.scto.mobile.ide.core.terminal.AlpineDocumentProvider
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.activities.settings.settingsNavController
import com.scto.mobile.ide.core.terminal.ui.components.NextScreenCard
import com.scto.mobile.ide.core.terminal.ui.components.SettingsItem
import com.scto.mobile.ide.core.terminal.ui.components.ValueSlider
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceTemplate
import com.scto.mobile.ide.components.compose.preferences.switch.PreferenceSwitch
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.createFileIfNot
import com.scto.mobile.ide.core.common.files.localBinDir
import com.scto.mobile.ide.core.common.files.localDir
import com.scto.mobile.ide.core.common.files.localLibDir
import com.scto.mobile.ide.core.common.files.sandboxDir
import com.scto.mobile.ide.core.common.files.FileObject
import com.scto.mobile.ide.core.common.files.FileManager
import com.scto.mobile.ide.core.common.files.toFileObject
import com.scto.mobile.ide.core.terminal.resources.getString
import com.scto.mobile.ide.core.terminal.resources.strings
import com.scto.mobile.ide.core.terminal.settings.Settings
import com.scto.mobile.ide.terminal.terminalView
import com.scto.mobile.ide.core.common.utils.LoadingPopup
import com.scto.mobile.ide.core.common.utils.dialogRes
import com.scto.mobile.ide.core.common.utils.dpToPx
import com.scto.mobile.ide.core.common.utils.getTempDir
import com.scto.mobile.ide.core.common.utils.toast
import com.scto.mobile.ide.core.terminal.core.BuildConfig
import com.termux.terminal.TerminalEmulator
import android.content.Context
import androidx.activity.ComponentActivity
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
            if (BuildConfig.DEBUG) {
                SettingsItem(
                    label = stringResource(strings.failsafe_mode),
                    description = stringResource(strings.failsafe_mode_desc),
                    default = !Settings.sandbox,
                    sideEffect = { Settings.sandbox = !it },
                )
            }

            SettingsItem(
                label = "SECCOMP",
                description = stringResource(strings.seccomp_desc),
                default = Settings.seccomp,
                sideEffect = { Settings.seccomp = it },
            )

            NextScreenCard(
                label = stringResource(strings.terminal_health),
                description = stringResource(strings.terminal_health_desc),
                navController = overrideNavController ?: settingsNavController.get(),
                route = SettingsRoutes.TerminalCheck.route,
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
                route = SettingsRoutes.TerminalFontScreen.route,
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
                    val fileManager = getHostFileManager(context)

                    fileManager?.createNewFile(
                        mimeType = "application/octet-stream",
                        title = "terminal-backup.tar.gz",
                    ) { fileObject: FileObject? ->
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
                route = SettingsRoutes.TerminalExtraKeys.route,
            )

            SettingsItem(
                label = stringResource(strings.clipboard_keybindings),
                description = stringResource(strings.clipboard_keybindings_desc),
                default = Settings.terminal_clipboard_keybindings,
                sideEffect = { Settings.terminal_clipboard_keybindings = it },
            )

            ValueSlider(
                label = stringResource(strings.scrollback_buffer),
                min = TerminalEmulator.TERMINAL_TRANSCRIPT_ROWS_MIN,
                max = TerminalEmulator.TERMINAL_TRANSCRIPT_ROWS_MAX,
                default = Settings.terminal_scrollback_lines,
                onValueChanged = {
                    Settings.terminal_scrollback_lines = it
                    toast(strings.restart_required)
                },
            )

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
                                AlpineDocumentProvider.setDocumentProviderEnabled(context, true)
                                exposeHomeDirState = true
                            },
                        )
                    } else {
                        Settings.expose_home_dir = false
                        exposeHomeDirState = false
                        AlpineDocumentProvider.setDocumentProviderEnabled(context, false)
                    }
                },
                label = stringResource(strings.expose_saf),
                description = stringResource(strings.expose_saf_desc),
            )
        }
    }
}

private fun getHostFileManager(context: Context): FileManager? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is ComponentActivity) {
            break
        }
        ctx = ctx.baseContext
    }
    return try {
        val method = ctx.javaClass.getMethod("getFileManager")
        method.invoke(ctx) as? FileManager
    } catch (e: Exception) {
        try {
            val field = ctx.javaClass.getDeclaredField("fileManager")
            field.isAccessible = true
            field.get(ctx) as? FileManager
        } catch (ex: Exception) {
            null
        }
    }
}
