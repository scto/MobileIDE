/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("ktlint:standard:filename")

package com.scto.mobile.ide.ui.settings.debugOptions

import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController

import com.rk.activities.settings.SettingsRoutes
import com.rk.components.SettingsItem
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.resources.getString
import com.rk.resources.strings
import com.rk.settings.Settings
import com.rk.utils.dialogRes
import com.rk.utils.toast

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                delay(300)
                val runtime = Runtime.getRuntime()
                val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                memoryUsage.value = "$usedMem/${runtime.maxMemory() / (1024 * 1024)}MB"
            }
        }
    }

    PreferenceLayout(label = stringResource(strings.debug_options)) {
        PreferenceGroup {
            SettingsItem(
                label = stringResource(strings.force_crash),
                description = stringResource(strings.force_crash_desc),
                showSwitch = false,
                default = false,
                sideEffect = {
                    dialogRes(
                        activity = activity,
                        title = strings.force_crash.getString(),
                        msg = strings.force_crash_confirm.getString(),
                        onCancel = {},
                        onOk = { Thread { throw HarmlessException("Force crash") }.start() },
                    )
                },
            )

            SettingsItem(
                label = stringResource(strings.memory_usage),
                description = memoryUsage.value,
                showSwitch = false,
                default = false,
            )

            SettingsItem(
                label = stringResource(strings.strict_mode),
                description = stringResource(strings.strict_mode_desc),
                showSwitch = true,
                default = Settings.strict_mode,
                sideEffect = { Settings.strict_mode = it },
            )

            SettingsItem(
                label = stringResource(strings.anr_watchdog),
                description = stringResource(strings.anr_watchdog_desc),
                default = Settings.anr_watchdog,
                sideEffect = { Settings.anr_watchdog = it },
            )

            SettingsItem(
                label = stringResource(strings.verbose_errors),
                description = stringResource(strings.verbose_errors_desc),
                showSwitch = true,
                default = Settings.verbose_error,
                sideEffect = { Settings.verbose_error = it },
            )

            SettingsItem(
                label = stringResource(strings.desktop_mode),
                description = stringResource(strings.desktop_mode_desc),
                showSwitch = true,
                default = Settings.desktop_mode,
                sideEffect = { Settings.desktop_mode = it },
            )

            SettingsItem(
                label = stringResource(strings.theme_flipper),
                description = stringResource(strings.theme_flipper_desc),
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
                label = stringResource(strings.reset_consent),
                description = stringResource(strings.reset_consent_desc),
                showSwitch = false,
                default = false,
                sideEffect = {
                    Settings.shown_disclaimer = false
                    toast(strings.restart_required)
                },
            )

            SettingsItem(
                label = stringResource(strings.view_logs),
                description = stringResource(strings.view_app_logs),
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
                            delay(7000)

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
