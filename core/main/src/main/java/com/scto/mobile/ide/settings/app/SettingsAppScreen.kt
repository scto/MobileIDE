package com.scto.mobile.ide.settings.app





import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.scto.mobile.ide.activities.settings.SettingsActivity
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.components.BasicToggle
import com.scto.mobile.ide.components.NextScreenCard
import com.scto.mobile.ide.components.SettingsItem
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.feature.FeatureRegistry
import com.scto.mobile.ide.file.toFileObject
import com.scto.mobile.ide.icons.XedIcon
import com.scto.mobile.ide.settings.Preference
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.settings.editor.refreshEditors
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext











@Composable
fun SettingsAppScreen(activity: SettingsActivity, navController: NavController) {
    PreferenceLayout(label = stringResource(id = com.scto.mobile.ide.core.main.R.string.app), backArrowVisible = true) {
        val scope = rememberCoroutineScope()
        val gson = remember { GsonBuilder().setPrettyPrinting().create() }

        PreferenceGroup {
            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.lang),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.lang_desc),
                showSwitch = false,
                default = false,
                endWidget = {
                    Icon(
                        modifier = Modifier.padding(16.dp),
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                sideEffect = { navController.navigate(SettingsRoutes.LanguageScreen.route) },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.check_for_updates),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.check_for_updates_desc),
                default = Settings.check_for_update,
                sideEffect = { Settings.check_for_update = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.fullscreen),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.fullscreen_desc),
                default = Settings.fullscreen,
                sideEffect = { Settings.fullscreen = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.smart_toolbar),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.smart_toolbar_desc),
                default = Settings.smart_toolbar,
                sideEffect = { Settings.smart_toolbar = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.confirm_exit_dialog),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.confirm_exit_dialog_desc),
                default = Settings.confirm_exit,
                sideEffect = { Settings.confirm_exit = it },
            )

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                var hasManageExternalStorageDeclared by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val app = application ?: return@LaunchedEffect
                    val pm = app.packageManager

                    val pkgInfo =
                        pm.getPackageInfo(
                            app.packageName,
                            PackageManager.GET_PERMISSIONS,
                        )

                    hasManageExternalStorageDeclared =
                        pkgInfo.requestedPermissions?.any {
                            it == android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
                        } ?: false
                }

                SettingsItem(
                    label = stringResource(com.scto.mobile.ide.core.main.R.string.manage_storage),
                    description = stringResource(com.scto.mobile.ide.core.main.R.string.manage_storage_desc),
                    showSwitch = false,
                    isEnabled = hasManageExternalStorageDeclared,
                    default = false,
                    endWidget = {
                        Icon(
                            modifier = Modifier.padding(16.dp),
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    sideEffect = {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = "package:${activity.packageName}".toUri()
                        activity.startActivity(intent)
                    },
                )
            }

            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.manage_app_font),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.manage_app_font),
                route = SettingsRoutes.AppFontScreen,
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.feature_toggles)) {
            FeatureRegistry.toggles.forEach { toggle ->
                BasicToggle(
                    label = toggle.name,
                    checked = toggle.state.value,
                    onSwitch = { checked ->
                        if (toggle.onSwitch != null) {
                            toggle.onSwitch.invoke(activity, checked) { ok ->
                                toggle.setEnable(ok)
                            }
                        } else {
                            toggle.setEnable(checked)
                        }
                    },
                    startWidget = {
                        XedIcon(
                            icon = toggle.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    },
                )
            }
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.backup)) {
            SettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.backup),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.settings_backup_desc),
                showSwitch = false,
                default = false,
                sideEffect = {
                    activity.fileManager.createNewFile(
                        "application/json",
                        "xed-settings.json",
                    ) { fileObject ->
                        if (fileObject == null) return@createNewFile
                        scope.launch(Dispatchers.IO) {
                            try {
                                val json = gson.toJson(Preference.getAll())
                                fileObject.getOutputStream(false).use { outputStream ->
                                    outputStream.write(json.toByteArray())
                                }
                                toast(com.scto.mobile.ide.core.main.R.string.export_successful)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                toast(com.scto.mobile.ide.core.main.R.string.export_failed)
                            }
                        }
                    }
                },
            )
            SettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.restore),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.settings_restore_desc),
                showSwitch = false,
                default = false,
                sideEffect = {
                    activity.fileManager.requestOpenFile("application/json") { uri ->
                        if (uri == null) return@requestOpenFile
                        scope.launch(Dispatchers.IO) {
                            try {
                                val type = object : TypeToken<Map<String, Any>>() {}.type
                                val content = uri.toFileObject(true).readText()
                                val json: Map<String, Any> = gson.fromJson(content, type)

                                Preference.clearData()
                                json.forEach { (key, value) ->
                                    val expectedType = Preference.preferenceTypes[key]

                                    val fixedValue =
                                        when (expectedType) {
                                            Float::class -> (value as Number).toFloat()
                                            Int::class -> (value as Number).toInt()
                                            Long::class -> (value as Number).toLong()
                                            Boolean::class -> value as Boolean
                                            String::class -> value as String
                                            else -> value
                                        }

                                    Preference.put(key, fixedValue)
                                }

                                // Update theme in the UI if the setting changed
                                withContext(Dispatchers.Main) {
                                    AppCompatDelegate.setDefaultNightMode(Settings.theme_mode)
                                    refreshEditors()
                                }

                                toast(com.scto.mobile.ide.core.main.R.string.import_successful)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                toast(com.scto.mobile.ide.core.main.R.string.import_failed)
                            }
                        }
                    }
                },
            )
        }
    }
}
