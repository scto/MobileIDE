package com.scto.mobile.ide.settings.debugOptions





import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.components.RoundedValueSlider
import com.scto.mobile.ide.components.SettingsItem
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.dialogRes
import com.scto.mobile.ide.utils.toast
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@file:Suppress("ktlint:standard:filename")











private var flipperJob: Job? = null

@Suppress("ktlint:standard:function-naming")
@OptIn(DelicateCoroutinesApi::class)
@Composable
fun DeveloperOptions(modifier: Modifier = Modifier, navController: NavController) {
    val activity = LocalActivity.current

    val memoryUsage = remember { mutableStateOf("Unknown") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                delay(300.milliseconds)
                val runtime = Runtime.getRuntime()
                val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                memoryUsage.value = "$usedMem/${runtime.maxMemory() / (1024 * 1024)}MB"
            }
        }
    }

    PreferenceLayout(label = stringResource(com.scto.mobile.ide.core.main.R.string.debug_options)) {
        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.general)) {
            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.force_crash),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.force_crash_desc),
                showSwitch = false,
                default = false,
                sideEffect = {
                    dialogRes(
                        activity = activity,
                        title = com.scto.mobile.ide.core.main.R.string.force_crash.getString(),
                        msg = com.scto.mobile.ide.core.main.R.string.force_crash_confirm.getString(),
                        onCancel = {},
                        onOk = { Thread { throw HarmlessException("Force crash") }.start() },
                    )
                },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.memory_usage),
                description = memoryUsage.value,
                showSwitch = false,
                default = false,
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.strict_mode),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.strict_mode_desc),
                showSwitch = true,
                default = Settings.strict_mode,
                sideEffect = { Settings.strict_mode = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.anr_watchdog),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.anr_watchdog_desc),
                default = Settings.anr_watchdog,
                sideEffect = { Settings.anr_watchdog = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.desktop_mode),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.desktop_mode_desc),
                showSwitch = true,
                default = Settings.desktop_mode,
                sideEffect = { Settings.desktop_mode = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.theme_flipper),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.theme_flipper_desc),
                showSwitch = true,
                default = Settings.theme_flipper,
                sideEffect = {
                    Settings.theme_flipper = it
                    if (it) {
                        startThemeFlipperIfNotRunning()
                    }
                },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.reset_consent),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.reset_consent_desc),
                showSwitch = false,
                default = false,
                sideEffect = {
                    Settings.shown_disclaimer = false
                    toast(com.scto.mobile.ide.core.main.R.string.restart_required)
                },
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.logs)) {
            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.verbose_errors),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.verbose_errors_desc),
                showSwitch = true,
                default = Settings.verbose_error,
                sideEffect = { Settings.verbose_error = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.record_rpc_traffic),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.record_rpc_traffic_desc),
                showSwitch = true,
                default = Settings.record_rpc,
                sideEffect = { Settings.record_rpc = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.enable_logcat),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.enable_logcat_desc),
                showSwitch = true,
                default = Settings.enable_logcat,
                sideEffect = {
                    Settings.enable_logcat = it
                    if (it) {
                        LogcatService.start(application!!)
                    } else {
                        LogcatService.stop(application!!)
                    }
                },
            )

            RoundedValueSlider(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.lsp_log_limit),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.lsp_log_limit_desc),
                min = 1_000,
                max = 100_000,
                stepSize = 5_000,
                default = Settings.lsp_log_limit,
                onValueChanged = { Settings.lsp_log_limit = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.view_logs),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.view_app_logs),
                default = false,
                showSwitch = false,
                onClick = { navController.navigate(SettingsRoutes.AppLogs.route) },
            )
        }
    }
}

fun startThemeFlipperIfNotRunning() {
    if (flipperJob == null || flipperJob?.isActive?.not() == true) {
        flipperJob =
            GlobalScope.launch(Dispatchers.IO) {
                runCatching {
                    while (isActive && Settings.theme_flipper) {
                        delay(7000.milliseconds)

                        val mode =
                            if (Settings.theme_mode == AppCompatDelegate.MODE_NIGHT_NO) {
                                AppCompatDelegate.MODE_NIGHT_YES
                            } else {
                                AppCompatDelegate.MODE_NIGHT_NO
                            }

                        Settings.theme_mode = mode

                        withContext(Dispatchers.Main) { AppCompatDelegate.setDefaultNightMode(mode) }
                    }
                }
                    .onFailure { it.printStackTrace() }
            }
    }
}

class HarmlessException(msg: String) : Exception(msg)
