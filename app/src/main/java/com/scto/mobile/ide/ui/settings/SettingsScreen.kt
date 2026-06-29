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

package com.scto.mobile.ide.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.scto.mobile.ide.R
import com.scto.mobile.ide.core.utils.AppLanguageManager
import com.scto.mobile.ide.core.utils.AppLanguageOption
import com.scto.mobile.ide.core.utils.LogConfigState
import com.scto.mobile.ide.core.utils.ThemeState
import com.scto.mobile.ide.core.utils.WorkspaceManager
import com.scto.mobile.ide.safeNavigate
import com.scto.mobile.ide.ui.components.ColorPickerDialog
import com.scto.mobile.ide.ui.components.DirectorySelector
import com.scto.mobile.ide.ui.terminal.DistroManager
import com.scto.mobile.ide.ui.welcome.themeColors
import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AutoSaveOption(@StringRes val labelRes: Int, val interval: Long) {
    OFF(R.string.auto_save_off, 0L),
    SEC_30(R.string.auto_save_30_seconds, 30_000L),
    MIN_1(R.string.auto_save_1_minute, 60_000L),
    MIN_5(R.string.auto_save_5_minutes, 300_000L),
    MIN_10(R.string.auto_save_10_minutes, 600_000L),
}

fun Color.luminance(): Float {
    return 0.2126f * this.red + 0.7152f * this.green + 0.0722f * this.blue
}

private data class FontPresetOption(val label: String, val file: String)

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    currentThemeState: ThemeState,
    logConfigState: LogConfigState,
    onThemeChange: (modeIndex: Int, themeIndex: Int, customColor: Color, isMonet: Boolean, isCustom: Boolean) -> Unit,
    onLogConfigChange: (enabled: Boolean, filePath: String) -> Unit,
    editorViewModel: com.scto.mobile.ide.ui.editor.viewmodel.EditorViewModel? = null,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MobileIDE_Editor_Settings", Context.MODE_PRIVATE) }
    val generalPrefs = remember { context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE) }

    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("editor_font_size", 14f)) }
    var tabWidth by remember { mutableIntStateOf(prefs.getInt("editor_tab_width", 4)) }
    var wordWrap by remember { mutableStateOf(prefs.getBoolean("editor_word_wrap", false)) }
    var showInvisibles by remember { mutableStateOf(prefs.getBoolean("editor_show_invisibles", false)) }
    var codeFolding by remember { mutableStateOf(prefs.getBoolean("editor_code_folding", true)) }
    var showToolbar by remember { mutableStateOf(prefs.getBoolean("editor_show_toolbar", true)) }
    var showHistory by remember { mutableStateOf(prefs.getBoolean("editor_show_history", true)) }
    var lspEnabled by remember { mutableStateOf(prefs.getBoolean("editor_lsp_enabled", false)) }
    var aiEnabled by remember { mutableStateOf(prefs.getBoolean("editor_ai_enabled", true)) }
    var fontPath by remember { mutableStateOf(prefs.getString("editor_font_path", "") ?: "") }
    var customSymbols by remember {
        mutableStateOf(
            prefs.getString("editor_custom_symbols", "Tab,<,>,/,=,\",',!,?,;,:,{,},[,],(,),+,-,*,_,&,|") ?: ""
        )
    }
    var autoSaveInterval by remember { mutableLongStateOf(generalPrefs.getLong("auto_save_interval", 0L)) }

    var selectedDistro by remember { mutableStateOf(generalPrefs.getString("selected_distro", "ubuntu") ?: "ubuntu") }

    var showAutoSaveDialog by remember { mutableStateOf(false) }
    var previousLspEnabled by remember { mutableStateOf(lspEnabled) }

    LaunchedEffect(
        fontSize,
        tabWidth,
        wordWrap,
        showInvisibles,
        codeFolding,
        showToolbar,
        showHistory,
        lspEnabled,
        aiEnabled,
        fontPath,
        customSymbols,
    ) {
        prefs.edit {
            putFloat("editor_font_size", fontSize)
            putInt("editor_tab_width", tabWidth)
            putBoolean("editor_word_wrap", wordWrap)
            putBoolean("editor_show_invisibles", showInvisibles)
            putBoolean("editor_code_folding", codeFolding)
            putBoolean("editor_show_toolbar", showToolbar)
            putBoolean("editor_show_history", showHistory)
            putBoolean("editor_lsp_enabled", lspEnabled)
            putBoolean("editor_ai_enabled", aiEnabled)
            putString("editor_font_path", fontPath)
            putString("editor_custom_symbols", customSymbols)
        }
        if (lspEnabled != previousLspEnabled) {
            editorViewModel?.reloadAllEditors(context)
            previousLspEnabled = lspEnabled
        }
    }

    var selectedWorkspace by remember { mutableStateOf(WorkspaceManager.getWorkspacePath(context)) }
    var showFileSelector by remember { mutableStateOf(false) }
    var showLogPathSelector by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguageOption by AppLanguageManager.currentOption.collectAsState()

    var refreshTrigger by remember { mutableIntStateOf(0) }

    var isJdk17Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isJdk21Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isGradleInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isAndroidSdkInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isBuildTools35Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isBuildTools36Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isPlatform34Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isPlatform35Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isCmakeInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isNdkInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isBaseUtilsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isJdtlsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isKotlinLsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isTsLsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isWebLsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }

    var activeInstallJobName by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Download progress for reinstall
    var reinstallDownloadedBytes by remember { mutableLongStateOf(0L) }
    var reinstallTotalBytes by remember { mutableLongStateOf(-1L) }
    var reinstallStatus by remember { mutableStateOf("") }
    var isReinstalling by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger, selectedDistro) {
        withContext(Dispatchers.IO) {
            val prefixDir = context.filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$selectedDistro")
            fun getDistroFile(path: String) = File(distroDir, path)

            isJdk17Installed =
                getDistroFile("usr/lib/jvm/java-17-openjdk/bin/java").exists() ||
                    getDistroFile("usr/lib/jvm/java-17-openjdk-amd64/bin/java").exists()
            isJdk21Installed =
                getDistroFile("usr/lib/jvm/java-21-openjdk/bin/java").exists() ||
                    getDistroFile("usr/lib/jvm/java-21-openjdk-amd64/bin/java").exists()
            isGradleInstalled = getDistroFile("usr/bin/gradle").exists()

            val hostSdk = File("/data/data/com.termux/files/home/android-sdk")
            val distroSdk = getDistroFile("root/android-sdk")
            isAndroidSdkInstalled = hostSdk.exists() || distroSdk.exists()

            isBuildTools35Installed =
                File(hostSdk, "build-tools/35.0.0").exists() ||
                    getDistroFile("root/android-sdk/build-tools/35.0.0").exists()
            isBuildTools36Installed =
                File(hostSdk, "build-tools/36.0.0").exists() ||
                    getDistroFile("root/android-sdk/build-tools/36.0.0").exists()

            isPlatform34Installed =
                File(hostSdk, "platforms/android-34").exists() ||
                    getDistroFile("root/android-sdk/platforms/android-34").exists()
            isPlatform35Installed =
                File(hostSdk, "platforms/android-35").exists() ||
                    getDistroFile("root/android-sdk/platforms/android-35").exists()

            isCmakeInstalled = getDistroFile("usr/bin/cmake").exists()
            isNdkInstalled =
                File(hostSdk, "ndk").exists() ||
                    getDistroFile("root/android-sdk/ndk").exists() ||
                    File(hostSdk, "ndk-bundle").exists()

            isBaseUtilsInstalled = getDistroFile("usr/bin/make").exists()

            isJdtlsInstalled = getDistroFile("usr/bin/jdtls").exists()
            isKotlinLsInstalled = getDistroFile("usr/bin/kotlin-language-server").exists()

            val tsFile1 = getDistroFile("usr/bin/typescript-language-server")
            val tsFile2 = getDistroFile("usr/local/bin/typescript-language-server")
            isTsLsInstalled = tsFile1.exists() || tsFile2.exists()

            val htmlFile1 = getDistroFile("usr/bin/vscode-html-language-server")
            val htmlFile2 = getDistroFile("usr/local/bin/vscode-html-language-server")
            isWebLsInstalled = htmlFile1.exists() || htmlFile2.exists()
        }
    }

    fun runInstall(jobName: String, command: String) {
        activeInstallJobName = jobName
        Toast.makeText(context, context.getString(R.string.toast_terminal_reinstall_start), Toast.LENGTH_SHORT).show()

        val fullCommand = DistroManager.buildProotCommand(context, arrayOf("sh", "-c", command))
        val env = DistroManager.getProotEnv(context)

        thread {
            try {
                val process =
                    ProcessBuilder(fullCommand)
                        .apply {
                            environment().putAll(env)
                            redirectErrorStream(true)
                        }
                        .start()
                process.waitFor()
                val success = process.exitValue() == 0

                (context as android.app.Activity).runOnUiThread {
                    activeInstallJobName = null
                    if (success) {
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_install_success, jobName),
                                Toast.LENGTH_LONG,
                            )
                            .show()
                    } else {
                        Toast.makeText(
                                context,
                                context.getString(
                                    R.string.toast_install_failed,
                                    jobName,
                                    "Exit code " + process.exitValue(),
                                ),
                                Toast.LENGTH_LONG,
                            )
                            .show()
                    }
                    refreshTrigger++
                }
            } catch (e: Exception) {
                (context as android.app.Activity).runOnUiThread {
                    activeInstallJobName = null
                    Toast.makeText(
                            context,
                            context.getString(
                                R.string.toast_install_failed,
                                jobName,
                                e.localizedMessage ?: "Unknown Error",
                            ),
                            Toast.LENGTH_LONG,
                        )
                        .show()
                    refreshTrigger++
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "theme_settings") {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = stringResource(R.string.settings_theme_summary),
                    onClick = { navController.navigate("settings/theme") },
                )
            }

            item(key = "editor_settings") {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.settings_editor_title),
                    subtitle =
                        stringResource(
                            R.string.settings_editor_summary,
                            tabWidth,
                            if (fontPath.isBlank()) stringResource(R.string.font_system_default)
                            else fontPath.substringAfterLast("/"),
                        ),
                    onClick = { navController.navigate("settings/editor") },
                )
            }

            item(key = "terminal_settings") {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Terminal,
                    title = stringResource(R.string.settings_terminal_title),
                    subtitle = stringResource(R.string.settings_terminal_summary, selectedDistro),
                    onClick = { navController.navigate("settings/terminal") },
                )
            }

            item(key = "build_settings") {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Build,
                    title = stringResource(R.string.settings_build_title),
                    subtitle = stringResource(R.string.settings_build_summary),
                    onClick = { navController.navigate("settings/build") },
                )
            }

            item(key = "lsp_settings") {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Memory,
                    title = stringResource(R.string.settings_lsp_servers_title),
                    subtitle = stringResource(R.string.settings_lsp_summary),
                    onClick = { navController.navigate("settings/lsp") },
                )
            }
            item {
                Text(
                    text = stringResource(R.string.settings_general),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            item {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(currentLanguageOption.labelRes),
                    onClick = { showLanguageDialog = true },
                )
            }
            item {
                val currentOption =
                    AutoSaveOption.entries.find { it.interval == autoSaveInterval } ?: AutoSaveOption.OFF
                val currentOptionLabel = stringResource(currentOption.labelRes)
                SimpleSettingsCard(
                    icon = Icons.Outlined.SaveAs,
                    title = stringResource(R.string.settings_auto_save_backup_title),
                    subtitle =
                        if (currentOption == AutoSaveOption.OFF) stringResource(R.string.status_disabled)
                        else stringResource(R.string.settings_auto_save_frequency, currentOptionLabel),
                    onClick = { showAutoSaveDialog = true },
                )
            }
            item {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Folder,
                    title = stringResource(R.string.settings_workspace_title),
                    subtitle = selectedWorkspace,
                    onClick = { showFileSelector = true },
                )
            }
            item {
                LogSettingsItem(
                    logConfigState = logConfigState,
                    onLogConfigChange = onLogConfigChange,
                    onPathClick = { showLogPathSelector = true },
                )
            }
            item {
                SimpleSettingsCard(
                    icon = Icons.Outlined.WavingHand,
                    title = stringResource(R.string.settings_welcome_title),
                    subtitle = stringResource(R.string.settings_welcome_subtitle),
                    onClick = { navController.safeNavigate("welcome") },
                )
            }
            item {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_title),
                    subtitle = stringResource(R.string.settings_about_subtitle),
                    onClick = { navController.safeNavigate("about") },
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showFileSelector) {
        DirectorySelector(
            initialPath = selectedWorkspace,
            onPathSelected = { path ->
                selectedWorkspace = path
                WorkspaceManager.saveWorkspacePath(context, path)
                showFileSelector = false
                Toast.makeText(context, context.getString(R.string.toast_workspace_updated), Toast.LENGTH_SHORT).show()
            },
            onDismissRequest = { showFileSelector = false },
        )
    }

    if (showLogPathSelector) {
        DirectorySelector(
            initialPath = logConfigState.logFilePath,
            onPathSelected = { path ->
                onLogConfigChange(logConfigState.isLogEnabled, path)
                showLogPathSelector = false
                Toast.makeText(context, context.getString(R.string.toast_log_path_updated), Toast.LENGTH_SHORT).show()
            },
            onDismissRequest = { showLogPathSelector = false },
        )
    }

    if (showAutoSaveDialog) {
        AutoSaveDialog(
            selectedInterval = autoSaveInterval,
            onDismiss = { showAutoSaveDialog = false },
            onOptionSelected = { option, toastMessage ->
                autoSaveInterval = option.interval
                generalPrefs.edit { putLong("auto_save_interval", option.interval) }
                showAutoSaveDialog = false
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.settings_language_dialog_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    AppLanguageOption.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        showLanguageDialog = false
                                        AppLanguageManager.updateLanguage(context, option)
                                    }
                                    .padding(vertical = 12.dp),
                        ) {
                            RadioButton(selected = option == currentLanguageOption, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(option.labelRes), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = currentThemeState.customColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                onThemeChange(currentThemeState.selectedModeIndex, themeColors.size, color, false, true)
                showColorPicker = false
            },
        )
    }
}

@Composable
private fun AutoSaveDialog(
    selectedInterval: Long,
    onDismiss: () -> Unit,
    onOptionSelected: (AutoSaveOption, String) -> Unit,
) {
    val optionLabels =
        mapOf(
            AutoSaveOption.OFF to stringResource(R.string.auto_save_off),
            AutoSaveOption.SEC_30 to stringResource(R.string.auto_save_30_seconds),
            AutoSaveOption.MIN_1 to stringResource(R.string.auto_save_1_minute),
            AutoSaveOption.MIN_5 to stringResource(R.string.auto_save_5_minutes),
            AutoSaveOption.MIN_10 to stringResource(R.string.auto_save_10_minutes),
        )
    val optionToastMessages =
        mapOf(
            AutoSaveOption.OFF to stringResource(R.string.toast_auto_save_disabled),
            AutoSaveOption.SEC_30 to
                stringResource(R.string.toast_auto_save_set, optionLabels.getValue(AutoSaveOption.SEC_30)),
            AutoSaveOption.MIN_1 to
                stringResource(R.string.toast_auto_save_set, optionLabels.getValue(AutoSaveOption.MIN_1)),
            AutoSaveOption.MIN_5 to
                stringResource(R.string.toast_auto_save_set, optionLabels.getValue(AutoSaveOption.MIN_5)),
            AutoSaveOption.MIN_10 to
                stringResource(R.string.toast_auto_save_set, optionLabels.getValue(AutoSaveOption.MIN_10)),
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_auto_save_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_auto_save_dialog_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                AutoSaveOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { onOptionSelected(option, optionToastMessages.getValue(option)) }
                                .padding(vertical = 12.dp),
                    ) {
                        RadioButton(selected = option.interval == selectedInterval, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(text = optionLabels.getValue(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTypeDialog(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf("textmate", "treesitter")
    val optionLabels = mapOf(
        "textmate" to stringResource(R.string.editor_type_textmate),
        "treesitter" to stringResource(R.string.editor_type_treesitter)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_editor_type_title)) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selectedType,
                                onClick = { onTypeSelected(option) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selectedType, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(text = optionLabels.getValue(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsItem(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    tabWidth: Int,
    onTabWidthChange: (Int) -> Unit,
    wordWrap: Boolean,
    onWordWrapChange: (Boolean) -> Unit,
    showInvisibles: Boolean,
    onShowInvisiblesChange: (Boolean) -> Unit,
    codeFolding: Boolean,
    onCodeFoldingChange: (Boolean) -> Unit,
    showToolbar: Boolean,
    onShowToolbarChange: (Boolean) -> Unit,
    showHistory: Boolean,
    onShowHistoryChange: (Boolean) -> Unit,
    lspEnabled: Boolean,
    onLspEnabledChange: (Boolean) -> Unit,
    isAiEnabled: Boolean,
    onIsAiEnabledChange: (Boolean) -> Unit,
    fontPath: String,
    onFontPathChange: (String) -> Unit,
    customSymbols: String,
    onCustomSymbolsChange: (String) -> Unit,
    editorType: String,
    onEditorTypeChange: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val expandDuration = 200
    val textFadeDuration = 200
    val snappyEasing = LinearOutSlowInEasing

    var isFontDropdownExpanded by remember { mutableStateOf(false) }
    val fontPresetOptions =
        listOf(
            FontPresetOption(stringResource(R.string.font_default), ""),
            FontPresetOption(stringResource(R.string.font_jetbrains_mono), "ttf/JetBrainsMono-Regular.ttf"),
            FontPresetOption(stringResource(R.string.font_roboto_mono), "ttf/RobotoMono-Regular.ttf"),
            FontPresetOption(stringResource(R.string.font_source_code_pro), "ttf/SourceCodePro-Regular.ttf"),
            FontPresetOption(stringResource(R.string.font_comic_sans), "ttf/Comic-Sans-MS-Regular-2.ttf"),
        )

    var showEditorTypeDialog by remember { mutableStateOf(false) }

    if (showEditorTypeDialog) {
        EditorTypeDialog(
            selectedType = editorType,
            onTypeSelected = {
                onEditorTypeChange(it)
                showEditorTypeDialog = false
            },
            onDismiss = { showEditorTypeDialog = false }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier.animateContentSize(
                    animationSpec = tween(durationMillis = expandDuration, easing = snappyEasing)
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_editor_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    AnimatedVisibility(
                        visible = !expanded,
                        enter =
                            fadeIn(tween(textFadeDuration)) +
                                expandVertically(tween(textFadeDuration), expandFrom = Alignment.Top),
                        exit =
                            fadeOut(tween(textFadeDuration)) +
                                shrinkVertically(tween(textFadeDuration), shrinkTowards = Alignment.Top),
                    ) {
                        val displayFont =
                            if (fontPath.isBlank()) stringResource(R.string.font_system_default)
                            else fontPath.substringAfterLast("/")
                        Text(
                            text = stringResource(R.string.settings_editor_summary, tabWidth, displayFont),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val rotation by
                    animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "ArrowRotation",
                        animationSpec = tween(expandDuration),
                    )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation))
            }

            AnimatedVisibility(
                visible = expanded,
                enter =
                    fadeIn(tween(expandDuration)) +
                        expandVertically(
                            animationSpec = tween(expandDuration, easing = snappyEasing),
                            expandFrom = Alignment.Top,
                        ),
                exit =
                    fadeOut(tween(textFadeDuration)) +
                        shrinkVertically(
                            animationSpec = tween(textFadeDuration, easing = snappyEasing),
                            shrinkTowards = Alignment.Top,
                        ),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.settings_assistance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    CompactSwitchRow(stringResource(R.string.settings_ai_assistant), isAiEnabled, onIsAiEnabledChange)
                    CompactSwitchRow(stringResource(R.string.settings_lsp_completion), lspEnabled, onLspEnabledChange)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditorTypeDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_editor_type_title), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (editorType == "treesitter") stringResource(R.string.editor_type_treesitter) else stringResource(R.string.editor_type_textmate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        stringResource(R.string.settings_indent_width),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val options = listOf(2, 4, 8)
                        options.forEach { option ->
                            val isSelected = tabWidth == option
                            val containerColor by
                                animateColorAsState(
                                    targetValue =
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    animationSpec = tween(200),
                                    label = "ButtonContainer",
                                )
                            val contentColor by
                                animateColorAsState(
                                    targetValue =
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                    animationSpec = tween(200),
                                    label = "ButtonContent",
                                )

                            Surface(
                                onClick = { onTabWidthChange(option) },
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = containerColor,
                                contentColor = contentColor,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = stringResource(R.string.settings_spaces_format, option),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        stringResource(R.string.settings_editor_font),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = fontPath,
                            onValueChange = onFontPathChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.settings_input_hint)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isFontDropdownExpanded = !isFontDropdownExpanded }) {
                                    Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.settings_select_preset))
                                }
                            },
                        )

                        DropdownMenu(
                            expanded = isFontDropdownExpanded,
                            onDismissRequest = { isFontDropdownExpanded = false },
                            offset = DpOffset(0.dp, 0.dp),
                            modifier = Modifier.fillMaxWidth(0.9f),
                        ) {
                            fontPresetOptions.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(preset.label, style = MaterialTheme.typography.bodyLarge)
                                            if (preset.file.isNotEmpty()) {
                                                Text(
                                                    preset.file,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onFontPathChange(preset.file)
                                        isFontDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.settings_editor_font_size),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(R.string.settings_editor_font_size_val, fontSize),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = onFontSizeChange,
                        valueRange = 8f..32f,
                        steps = 24,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        stringResource(R.string.settings_behavior),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    CompactSwitchRow(stringResource(R.string.settings_show_toolbar), showToolbar, onShowToolbarChange)
                    CompactSwitchRow(
                        stringResource(R.string.settings_show_history_tabs),
                        showHistory,
                        onShowHistoryChange,
                    )
                    CompactSwitchRow(stringResource(R.string.settings_word_wrap), wordWrap, onWordWrapChange)
                    CompactSwitchRow(
                        stringResource(R.string.settings_show_whitespace),
                        showInvisibles,
                        onShowInvisiblesChange,
                    )
                    CompactSwitchRow(stringResource(R.string.settings_code_folding), codeFolding, onCodeFoldingChange)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                    Text(
                        stringResource(R.string.settings_custom_symbols),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customSymbols,
                        onValueChange = onCustomSymbolsChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        placeholder = { Text(stringResource(R.string.settings_symbols_placeholder)) },
                    )
                }
            }
        }
    }
}

@Composable
fun CompactSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun Modifier.scale(scale: Float) = this.graphicsLayer(scaleX = scale, scaleY = scale)

@Composable
fun ThemeSettingsItem(
    currentThemeState: ThemeState,
    onThemeChange: (Int, Int, Color, Boolean, Boolean) -> Unit,
    onCustomColorClick: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val expandDuration = 200
    val textFadeDuration = 200
    val snappyEasing = LinearOutSlowInEasing

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier.animateContentSize(
                    animationSpec = tween(durationMillis = expandDuration, easing = snappyEasing)
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    AnimatedVisibility(
                        visible = !expanded,
                        enter =
                            fadeIn(tween(textFadeDuration)) +
                                expandVertically(tween(textFadeDuration), expandFrom = Alignment.Top),
                        exit =
                            fadeOut(tween(textFadeDuration)) +
                                shrinkVertically(tween(textFadeDuration), shrinkTowards = Alignment.Top),
                    ) {
                        Text(
                            text =
                                if (currentThemeState.isMonetEnabled) stringResource(R.string.settings_dynamic_color)
                                else stringResource(R.string.settings_custom_appearance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                val rotation by
                    animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "ArrowRotation",
                        animationSpec = tween(expandDuration),
                    )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation))
            }

            AnimatedVisibility(
                visible = expanded,
                enter =
                    fadeIn(tween(expandDuration)) +
                        expandVertically(
                            animationSpec = tween(expandDuration, easing = snappyEasing),
                            expandFrom = Alignment.Top,
                        ),
                exit =
                    fadeOut(tween(textFadeDuration)) +
                        shrinkVertically(
                            animationSpec = tween(textFadeDuration, easing = snappyEasing),
                            shrinkTowards = Alignment.Top,
                        ),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_dynamic_color),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Switch(
                                checked = currentThemeState.isMonetEnabled,
                                onCheckedChange = {
                                    val newIsCustom = if (it) false else currentThemeState.isCustomTheme
                                    onThemeChange(
                                        currentThemeState.selectedModeIndex,
                                        currentThemeState.selectedThemeIndex,
                                        currentThemeState.customColor,
                                        it,
                                        newIsCustom,
                                    )
                                },
                            )
                        }
                    }

                    AnimatedVisibility(visible = !currentThemeState.isMonetEnabled) {
                        Column {
                            Text(
                                stringResource(R.string.settings_theme_color),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                            ) {
                                itemsIndexed(themeColors) { index, theme ->
                                    val isSelected =
                                        !currentThemeState.isCustomTheme &&
                                            currentThemeState.selectedThemeIndex == index
                                    ColorSelectionItem(
                                        color = theme.primaryColor,
                                        name = theme.name,
                                        isSelected = isSelected,
                                        onClick = {
                                            onThemeChange(
                                                currentThemeState.selectedModeIndex,
                                                index,
                                                currentThemeState.customColor,
                                                false,
                                                false,
                                            )
                                        },
                                    )
                                }
                                item {
                                    CustomColorButton(
                                        isSelected = currentThemeState.isCustomTheme,
                                        customColor = currentThemeState.customColor,
                                        onClick = onCustomColorClick,
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        stringResource(R.string.settings_display_mode),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val modes =
                            listOf(
                                stringResource(R.string.action_follow_system),
                                stringResource(R.string.action_light),
                                stringResource(R.string.action_dark),
                            )
                        modes.forEachIndexed { index, label ->
                            SmoothFilterChip(
                                selected = currentThemeState.selectedModeIndex == index,
                                label = label,
                                onClick = {
                                    onThemeChange(
                                        index,
                                        currentThemeState.selectedThemeIndex,
                                        currentThemeState.customColor,
                                        currentThemeState.isMonetEnabled,
                                        currentThemeState.isCustomTheme,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleSettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LogSettingsItem(
    logConfigState: LogConfigState,
    onLogConfigChange: (Boolean, String) -> Unit,
    onPathClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.BugReport, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_enable_log), style = MaterialTheme.typography.titleMedium)
                }
                Switch(
                    checked = logConfigState.isLogEnabled,
                    onCheckedChange = { onLogConfigChange(it, logConfigState.logFilePath) },
                )
            }
            AnimatedVisibility(visible = logConfigState.isLogEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onPathClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            logConfigState.logFilePath,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SmoothFilterChip(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val duration = 200
    val fastEasing = LinearEasing
    val colorAnimSpec = tween<Color>(durationMillis = duration, easing = fastEasing)
    val containerColor by
        animateColorAsState(
            if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            colorAnimSpec,
            "Container",
        )
    val borderColor by
        animateColorAsState(
            if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
            colorAnimSpec,
            "Border",
        )
    val contentColor by
        animateColorAsState(
            if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            colorAnimSpec,
            "Content",
        )

    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = CircleShape,
        color = containerColor,
        border = if (!selected) BorderStroke(1.dp, borderColor) else null,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(visible = selected) {
                Row {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = contentColor)
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
fun ColorSelectionItem(color: Color, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(48.dp)
                    .border(
                        if (isSelected) 3.dp else 0.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape,
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color),
        ) {
            if (isSelected)
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp),
                )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CustomColorButton(isSelected: Boolean, customColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(48.dp)
                    .border(
                        if (isSelected) 3.dp else 0.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape,
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (isSelected) {
                Box(Modifier.fillMaxSize().background(customColor))
                Icon(
                    Icons.Default.Edit,
                    null,
                    tint = if (customColor.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    Icons.Default.Add,
                    stringResource(R.string.settings_custom),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_custom),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSettingsItem(
    selectedDistro: String,
    onDistroSelected: (String) -> Unit,
    onReset: () -> Unit,
    onReinstall: () -> Unit,
    isReinstalling: Boolean = false,
    reinstallDownloaded: Long = 0L,
    reinstallTotal: Long = -1L,
    reinstallStatus: String = "",
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val expandDuration = 200
    val snappyEasing = LinearOutSlowInEasing

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier.animateContentSize(
                    animationSpec = tween(durationMillis = expandDuration, easing = snappyEasing)
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_terminal_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    if (!expanded) {
                        Text(
                            text = "Aktuell: ${selectedDistro.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val rotation by
                    animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "ArrowRotation",
                        animationSpec = tween(expandDuration),
                    )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation))
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(expandDuration)) + expandVertically(tween(expandDuration, easing = snappyEasing)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200, easing = snappyEasing)),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Linux Distribution",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val distros = listOf("ubuntu", "debian")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        distros.forEach { distro ->
                            FilterChip(
                                selected = selectedDistro == distro,
                                onClick = { onDistroSelected(distro) },
                                label = { Text(distro.replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_terminal_reset),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    stringResource(R.string.settings_terminal_reset_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Button(onClick = onReset, enabled = !isReinstalling) {
                                Text(stringResource(R.string.settings_terminal_reset))
                            }
                        }

                        // Reinstall row – shows download progress while active
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.settings_terminal_reinstall),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        stringResource(R.string.settings_terminal_reinstall_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Button(
                                    onClick = onReinstall,
                                    enabled = !isReinstalling,
                                    colors =
                                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) {
                                    if (isReinstalling) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onError,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Text(stringResource(R.string.settings_terminal_reinstall))
                                    }
                                }
                            }

                            // Download progress bar
                            if (isReinstalling) {
                                fun Long.toMb() = "%.1f MB".format(this / 1_048_576.0)
                                val fraction =
                                    if (reinstallTotal > 0L)
                                        (reinstallDownloaded.toFloat() / reinstallTotal.toFloat()).coerceIn(0f, 1f)
                                    else -1f

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text =
                                                if (
                                                    reinstallStatus == "Linux RootFS wird heruntergeladen..." &&
                                                        reinstallTotal > 0L
                                                ) {
                                                    "Download: ${reinstallDownloaded.toMb()} / ${reinstallTotal.toMb()}  (${(fraction * 100).toInt()} %)"
                                                } else {
                                                    reinstallStatus.ifBlank { "Download läuft\u2026" }
                                                },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        if (fraction >= 0f) {
                                            LinearProgressIndicator(
                                                progress = { fraction },
                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            )
                                        } else {
                                            LinearProgressIndicator(
                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuildSettingsItem(
    isJdk17Installed: Boolean,
    isJdk21Installed: Boolean,
    isGradleInstalled: Boolean,
    isAndroidSdkInstalled: Boolean,
    isBuildTools35Installed: Boolean,
    isBuildTools36Installed: Boolean,
    isPlatform34Installed: Boolean,
    isPlatform35Installed: Boolean,
    isCmakeInstalled: Boolean,
    isNdkInstalled: Boolean,
    isBaseUtilsInstalled: Boolean,
    onInstall: (String, String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val expandDuration = 200
    val snappyEasing = LinearOutSlowInEasing

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier.animateContentSize(
                    animationSpec = tween(durationMillis = expandDuration, easing = snappyEasing)
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_build_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    if (!expanded)
                        Text(
                            text = stringResource(R.string.settings_build_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                }
                val rotation by
                    animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "ArrowRotation",
                        animationSpec = tween(expandDuration),
                    )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation))
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(expandDuration)) + expandVertically(tween(expandDuration, easing = snappyEasing)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200, easing = snappyEasing)),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BuildToolRow(
                            name = stringResource(R.string.settings_build_jdk),
                            isInstalled = isJdk17Installed || isJdk21Installed,
                            infoText =
                                if (isJdk21Installed) "OpenJDK 21" else if (isJdk17Installed) "OpenJDK 17" else null,
                            onInstall = {},
                            customInstallButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onInstall(
                                                "OpenJDK 17",
                                                "apk add openjdk17 || apt install -y openjdk-17-jdk",
                                            )
                                        },
                                        enabled = !isJdk17Installed,
                                    ) {
                                        Text("JDK 17")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onInstall(
                                                "OpenJDK 21",
                                                "apk add openjdk21 || apt install -y openjdk-21-jdk",
                                            )
                                        },
                                        enabled = !isJdk21Installed,
                                    ) {
                                        Text("JDK 21")
                                    }
                                }
                            },
                        )

                        BuildToolRow(
                            name = stringResource(R.string.settings_build_gradle),
                            isInstalled = isGradleInstalled,
                            onInstall = { onInstall("Gradle", "apk add gradle || apt install -y gradle") },
                        )

                        BuildToolRow(
                            name = stringResource(R.string.settings_build_android_sdk),
                            isInstalled = isAndroidSdkInstalled,
                            onInstall = {
                                onInstall(
                                    "Android SDK",
                                    "mkdir -p /root/android-sdk && wget -O /tmp/sdk.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && unzip -o /tmp/sdk.zip -d /root/android-sdk && rm /tmp/sdk.zip",
                                )
                            },
                        )

                        BuildToolRow(
                            name = stringResource(R.string.settings_build_tools),
                            isInstalled = isBuildTools35Installed || isBuildTools36Installed,
                            infoText =
                                if (isBuildTools35Installed && isBuildTools36Installed) "v35 & v36"
                                else if (isBuildTools35Installed) "v35"
                                else if (isBuildTools36Installed) "v36" else null,
                            onInstall = {},
                            customInstallButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onInstall(
                                                "Build-Tools v35",
                                                "yes | /root/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/root/android-sdk \"build-tools;35.0.0\"",
                                            )
                                        },
                                        enabled = isAndroidSdkInstalled && !isBuildTools35Installed,
                                    ) {
                                        Text("v35")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onInstall(
                                                "Build-Tools v36",
                                                "yes | /root/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/root/android-sdk \"build-tools;36.0.0-rc1\"",
                                            )
                                        },
                                        enabled = isAndroidSdkInstalled && !isBuildTools36Installed,
                                    ) {
                                        Text("v36")
                                    }
                                }
                            },
                        )

                        BuildToolRow(
                            name = stringResource(R.string.settings_build_platforms),
                            isInstalled = isPlatform34Installed || isPlatform35Installed,
                            infoText =
                                if (isPlatform34Installed && isPlatform35Installed) "API 34 & 35"
                                else if (isPlatform34Installed) "API 34"
                                else if (isPlatform35Installed) "API 35" else null,
                            onInstall = {},
                            customInstallButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onInstall(
                                                "Platform API 34",
                                                "yes | /root/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/root/android-sdk \"platforms;android-34\"",
                                            )
                                        },
                                        enabled = isAndroidSdkInstalled && !isPlatform34Installed,
                                    ) {
                                        Text("API 34")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onInstall(
                                                "Platform API 35",
                                                "yes | /root/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/root/android-sdk \"platforms;android-35\"",
                                            )
                                        },
                                        enabled = isAndroidSdkInstalled && !isPlatform35Installed,
                                    ) {
                                        Text("API 35")
                                    }
                                }
                            },
                        )

                        BuildToolRow(
                            name = stringResource(R.string.settings_build_cmake_ndk),
                            isInstalled = isCmakeInstalled && isNdkInstalled,
                            infoText =
                                if (isCmakeInstalled && isNdkInstalled) "Both"
                                else if (isCmakeInstalled) "CMake Only" else if (isNdkInstalled) "NDK Only" else null,
                            onInstall = {},
                            customInstallButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onInstall("CMake", "apk add cmake || apt install -y cmake") },
                                        enabled = !isCmakeInstalled,
                                    ) {
                                        Text("CMake")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onInstall(
                                                "NDK",
                                                "yes | /root/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/root/android-sdk \"ndk-bundle\"",
                                            )
                                        },
                                        enabled = isAndroidSdkInstalled && !isNdkInstalled,
                                    ) {
                                        Text("NDK")
                                    }
                                }
                            },
                        )

                        BuildToolRow(
                            name = stringResource(R.string.settings_build_base_utils),
                            isInstalled = isBaseUtilsInstalled,
                            onInstall = {
                                onInstall(
                                    "Base Build Utils",
                                    "apk add build-base bash git wget curl gcompat || apt install -y build-essential bash git wget curl",
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LspSettingsItem(
    isJdtlsInstalled: Boolean,
    isKotlinLsInstalled: Boolean,
    isTsLsInstalled: Boolean,
    isWebLsInstalled: Boolean,
    onInstall: (String, String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val expandDuration = 200
    val snappyEasing = LinearOutSlowInEasing

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier.animateContentSize(
                    animationSpec = tween(durationMillis = expandDuration, easing = snappyEasing)
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_lsp_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    if (!expanded)
                        Text(
                            text = stringResource(R.string.settings_lsp_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                }
                val rotation by
                    animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "ArrowRotation",
                        animationSpec = tween(expandDuration),
                    )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation))
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(expandDuration)) + expandVertically(tween(expandDuration, easing = snappyEasing)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200, easing = snappyEasing)),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BuildToolRow(
                            name = stringResource(R.string.settings_lsp_java),
                            isInstalled = isJdtlsInstalled,
                            onInstall = {
                                onInstall(
                                    "Java LSP (jdtls)",
                                    "apk add openjdk17 jdtls || apt install -y openjdk-17-jdk jdtls",
                                )
                            },
                        )
                        BuildToolRow(
                            name = stringResource(R.string.settings_lsp_kotlin),
                            isInstalled = isKotlinLsInstalled,
                            onInstall = {
                                onInstall(
                                    "Kotlin LSP",
                                    "apk add -X http://dl-cdn.alpinelinux.org/alpine/edge/testing kotlin-language-server || apt install -y kotlin-language-server",
                                )
                            },
                        )
                        BuildToolRow(
                            name = stringResource(R.string.settings_lsp_typescript),
                            isInstalled = isTsLsInstalled,
                            onInstall = {
                                onInstall(
                                    "TypeScript LSP",
                                    "apk add nodejs npm || apt install -y nodejs npm && npm install -g typescript typescript-language-server",
                                )
                            },
                        )
                        BuildToolRow(
                            name = stringResource(R.string.settings_lsp_web),
                            isInstalled = isWebLsInstalled,
                            onInstall = {
                                onInstall(
                                    "Web LSPs",
                                    "apk add nodejs npm || apt install -y nodejs npm && npm install -g vscode-langservers-extracted",
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildToolRow(
    name: String,
    isInstalled: Boolean,
    infoText: String? = null,
    onInstall: () -> Unit,
    customInstallButton: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = if (isInstalled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isInstalled) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text =
                        if (isInstalled) infoText ?: stringResource(R.string.status_installed)
                        else stringResource(R.string.status_not_installed),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isInstalled) Color(0xFF4CAF50) else Color(0xFFF44336),
                )
            }
        }
        if (customInstallButton != null) {
            customInstallButton()
        } else {
            Button(
                onClick = onInstall,
                enabled = !isInstalled,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Text(stringResource(R.string.action_install))
            }
        }
    }
}
