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
import com.scto.mobile.ide.core.utils.LogCatcher
import com.scto.mobile.ide.core.utils.LogConfigState
import com.scto.mobile.ide.core.utils.ThemeState
import com.scto.mobile.ide.core.utils.WorkspaceManager
import com.scto.mobile.ide.safeNavigate
import com.scto.mobile.ide.ui.components.ColorPickerDialog
import com.scto.mobile.ide.ui.components.DirectorySelector
import com.scto.mobile.ide.ui.terminal.AlpineManager
import com.scto.mobile.ide.ui.terminal.SetupWorker
import com.scto.mobile.ide.ui.welcome.themeColors
import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Auto-save option enumeration
enum class AutoSaveOption(@StringRes val labelRes: Int, val interval: Long) {
    OFF(R.string.auto_save_off, 0L),
    SEC_30(R.string.auto_save_30_seconds, 30_000L),
    MIN_1(R.string.auto_save_1_minute, 60_000L),
    MIN_5(R.string.auto_save_5_minutes, 300_000L),
    MIN_10(R.string.auto_save_10_minutes, 600_000L),
}

// Extension function to resolve luminance error
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
    var showAutoSaveDialog by remember { mutableStateOf(false) }
    // Save the previous LSP state to detect changes
    var previousLspEnabled by remember { mutableStateOf(lspEnabled) }

    // Auto save
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

        // Detect LSP state changes and reload all editors
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

    // States for Build Group
    var isAndroidSdk33Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isAndroidSdk34Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isAndroidSdk35Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isAndroidSdk36Installed by remember(refreshTrigger) { mutableStateOf(false) }

    var isBuildTools33Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isBuildTools34Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isBuildTools35Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isBuildTools36Installed by remember(refreshTrigger) { mutableStateOf(false) }

    var isPlatform33Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isPlatform34Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isPlatform35Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isPlatform36Installed by remember(refreshTrigger) { mutableStateOf(false) }

    var isCmake318Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isCmake320Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isCmake325Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isCmake431Installed by remember(refreshTrigger) { mutableStateOf(false) }

    var isNdk26Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isNdk27Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isNdk28Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isNdk29Installed by remember(refreshTrigger) { mutableStateOf(false) }

    var isJdk17Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isJdk21Installed by remember(refreshTrigger) { mutableStateOf(false) }
    var isJdk25Installed by remember(refreshTrigger) { mutableStateOf(false) }

    var isGradleInstalled by remember(refreshTrigger) { mutableStateOf(false) }

    // States for LSP Group
    var isJdtlsInstalled by remember(refreshTrigger) { mutableStateOf(false) }
    var isKotlinLsInstalled by remember(refreshTrigger) { mutableStateOf(false) }
    var isTsLsInstalled by remember(refreshTrigger) { mutableStateOf(false) }
    var isWebLsInstalled by remember(refreshTrigger) { mutableStateOf(false) }

    // Dialog flags
    var showAndroidDevDialog by remember { mutableStateOf(false) }
    var showGradleDialog by remember { mutableStateOf(false) }
    var showJavaDialog by remember { mutableStateOf(false) }
    var showLspDialog by remember { mutableStateOf(false) }

    // Loading / Installation State
    var activeInstallJobName by remember { mutableStateOf<String?>(null) }
    var activeInstallProgress by remember { mutableFloatStateOf(0f) }
    var activeInstallElapsedTime by remember { mutableStateOf("00:00") }
    var activeInstallRemainingTime by remember { mutableStateOf("--:--") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        withContext(Dispatchers.IO) {
            val prefixDir = context.filesDir.parentFile!!
            val alpineDir = File(prefixDir, "local/alpine")
            fun getAlpineFile(path: String) = File(alpineDir, path)

            val hostSdk = File("/data/data/com.termux/files/home/android-sdk")
            val alpineSdk = getAlpineFile("root/android-sdk")
            val alpineSdkOpt = getAlpineFile("opt/android-sdk")

            fun checkSdkSubPathExists(subPath: String): Boolean {
                return File(hostSdk, subPath).exists() ||
                       File(alpineSdk, subPath).exists() ||
                       File(alpineSdkOpt, subPath).exists()
            }

            fun checkBuildToolsInstalled(major: Int): Boolean {
                val searchDirs = listOf(
                    File(hostSdk, "build-tools"),
                    File(alpineSdk, "build-tools"),
                    File(alpineSdkOpt, "build-tools")
                )
                for (dir in searchDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        val matches = dir.listFiles { f -> f.isDirectory && f.name.startsWith("$major.") }
                        if (!matches.isNullOrEmpty()) return true
                    }
                }
                return false
            }

            fun checkCmakeInstalled(versionPrefix: String): Boolean {
                val searchDirs = listOf(
                    File(hostSdk, "cmake"),
                    File(alpineSdk, "cmake"),
                    File(alpineSdkOpt, "cmake")
                )
                for (dir in searchDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        val matches = dir.listFiles { f -> f.isDirectory && f.name.startsWith(versionPrefix) }
                        if (!matches.isNullOrEmpty()) return true
                    }
                }
                return false
            }

            fun checkNdkInstalled(major: Int): Boolean {
                val searchDirs = listOf(
                    File(hostSdk, "ndk"),
                    File(alpineSdk, "ndk"),
                    File(alpineSdkOpt, "ndk"),
                    File(hostSdk, "ndk-bundle"),
                    File(alpineSdk, "ndk-bundle"),
                    File(alpineSdkOpt, "ndk-bundle")
                )
                for (dir in searchDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        val matches = dir.listFiles { f -> f.isDirectory && f.name.startsWith("$major.") }
                        if (!matches.isNullOrEmpty()) return true
                    }
                }
                return false
            }

            fun getPlatformToolsVersion(): String? {
                val searchDirs = listOf(
                    File(hostSdk, "platform-tools"),
                    File(alpineSdk, "platform-tools"),
                    File(alpineSdkOpt, "platform-tools")
                )
                for (dir in searchDirs) {
                    val prop = File(dir, "source.properties")
                    if (prop.exists()) {
                        try {
                            val v = prop.readLines().find { it.startsWith("Pkg.Revision=") }?.substringAfter("Pkg.Revision=")
                            if (v != null) return v
                        } catch (e: Exception) {}
                    }
                }
                return null
            }

            isAndroidSdk33Installed = checkSdkSubPathExists("platforms/android-33")
            isAndroidSdk34Installed = checkSdkSubPathExists("platforms/android-34")
            isAndroidSdk35Installed = checkSdkSubPathExists("platforms/android-35")
            isAndroidSdk36Installed = checkSdkSubPathExists("platforms/android-36")

            isBuildTools33Installed = checkBuildToolsInstalled(33)
            isBuildTools34Installed = checkBuildToolsInstalled(34)
            isBuildTools35Installed = checkBuildToolsInstalled(35)
            isBuildTools36Installed = checkBuildToolsInstalled(36)

            val ptVer = getPlatformToolsVersion()
            isPlatform33Installed = ptVer?.startsWith("33") == true
            isPlatform34Installed = ptVer?.startsWith("34") == true
            isPlatform35Installed = ptVer?.startsWith("35") == true
            isPlatform36Installed = ptVer?.startsWith("36") == true

            isCmake318Installed = checkCmakeInstalled("3.18")
            isCmake320Installed = checkCmakeInstalled("3.20")
            isCmake325Installed = checkCmakeInstalled("3.25")
            isCmake431Installed = checkCmakeInstalled("4.3")

            isNdk26Installed = checkNdkInstalled(26)
            isNdk27Installed = checkNdkInstalled(27)
            isNdk28Installed = checkNdkInstalled(28)
            isNdk29Installed = checkNdkInstalled(29)

            isJdk17Installed = getAlpineFile("usr/lib/jvm/java-17-openjdk/bin/java").exists()
            isJdk21Installed = getAlpineFile("usr/lib/jvm/java-21-openjdk/bin/java").exists()
            isJdk25Installed = getAlpineFile("usr/lib/jvm/java-25-openjdk/bin/java").exists()

            isGradleInstalled = getAlpineFile("usr/bin/gradle").exists() || getAlpineFile("usr/local/bin/gradle").exists()

            // LSPs
            isJdtlsInstalled = getAlpineFile("usr/bin/jdtls").exists()
            isKotlinLsInstalled = getAlpineFile("usr/bin/kotlin-language-server").exists()

            val tsFile1 = getAlpineFile("usr/bin/typescript-language-server")
            val tsFile2 = getAlpineFile("usr/local/bin/typescript-language-server")
            isTsLsInstalled = tsFile1.exists() || tsFile2.exists()

            val htmlFile1 = getAlpineFile("usr/bin/vscode-html-language-server")
            val htmlFile2 = getAlpineFile("usr/local/bin/vscode-html-language-server")
            isWebLsInstalled = htmlFile1.exists() || htmlFile2.exists()
        }
    }

    fun runInstall(jobName: String, command: String) {
        activeInstallJobName = jobName
        activeInstallProgress = 0f
        activeInstallElapsedTime = "00:00"
        activeInstallRemainingTime = "--:--"
        Toast.makeText(context, context.getString(R.string.toast_terminal_reinstall_start), Toast.LENGTH_SHORT).show()
        LogCatcher.i("SettingsScreen", "Starting installation of: $jobName")

        val expectedDurationSec =
            when (jobName) {
                "Terminal environment" -> 60
                "OpenJDK 17",
                "OpenJDK 21" -> 90
                "Gradle" -> 45
                "Android SDK" -> 120
                "Build-Tools v35",
                "Build-Tools v36" -> 80
                "Platform API 34",
                "Platform API 35" -> 100
                "CMake" -> 30
                "NDK" -> 180
                "Base Build Utils" -> 65
                else -> 60
            }

        val startTime = System.currentTimeMillis()
        var jobFinished = false

        // Timer thread to calculate elapsed time and remaining time
        thread {
            while (!jobFinished) {
                Thread.sleep(1000)
                if (jobFinished) break
                val elapsedMs = System.currentTimeMillis() - startTime
                val elapsedSec = (elapsedMs / 1000).toInt()
                val progress = (elapsedSec.toFloat() / expectedDurationSec.toFloat()).coerceAtMost(0.99f)
                val remainingSec = (expectedDurationSec - elapsedSec).coerceAtLeast(1)

                val elapsedMinStr = String.format("%02d", elapsedSec / 60)
                val elapsedSecStr = String.format("%02d", elapsedSec % 60)
                val remainingMinStr = String.format("%02d", remainingSec / 60)
                val remainingSecStr = String.format("%02d", remainingSec % 60)

                (context as android.app.Activity).runOnUiThread {
                    activeInstallProgress = progress
                    activeInstallElapsedTime = "$elapsedMinStr:$elapsedSecStr"
                    activeInstallRemainingTime = "$remainingMinStr:$remainingSecStr"
                }
            }
        }

        thread {
            try {
                if (command == "reinstall_terminal") {
                    // Reinstall Terminal directly using SetupWorker (runs suspend function)
                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        SetupWorker.reinstallTerminal(context)
                    }
                    (context as android.app.Activity).runOnUiThread {
                        jobFinished = true
                        activeInstallJobName = null
                        LogCatcher.i("SettingsScreen", "Terminal environment reinstall success")
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_terminal_reinstall_success),
                                Toast.LENGTH_LONG,
                            )
                            .show()
                        refreshTrigger++
                    }
                } else {
                    val fullCommand =
                        com.scto.mobile.ide.ui.terminal.AlpineManager.buildProotCommand(
                            context,
                            arrayOf("sh", "-c", command),
                        )
                    val env = com.scto.mobile.ide.ui.terminal.AlpineManager.getProotEnv(context)
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
                        jobFinished = true
                        activeInstallJobName = null
                        if (success) {
                            LogCatcher.i("SettingsScreen", "Install success: $jobName")
                            Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_install_success, jobName),
                                    Toast.LENGTH_LONG,
                                )
                                .show()
                        } else {
                            LogCatcher.w(
                                "SettingsScreen",
                                "Install failed: $jobName (Exit code ${process.exitValue()})",
                            )
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
                }
            } catch (e: Exception) {
                (context as android.app.Activity).runOnUiThread {
                    jobFinished = true
                    activeInstallJobName = null
                    LogCatcher.e("SettingsScreen", "Install error for $jobName", e)
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
                ThemeSettingsItem(
                    currentThemeState = currentThemeState,
                    onThemeChange = onThemeChange,
                    onCustomColorClick = { showColorPicker = true },
                )
            }

            item(key = "editor_settings") {
                EditorSettingsItem(
                    fontSize = fontSize,
                    onFontSizeChange = { fontSize = it },
                    tabWidth = tabWidth,
                    onTabWidthChange = { tabWidth = it },
                    wordWrap = wordWrap,
                    onWordWrapChange = { wordWrap = it },
                    showInvisibles = showInvisibles,
                    onShowInvisiblesChange = { showInvisibles = it },
                    codeFolding = codeFolding,
                    onCodeFoldingChange = { codeFolding = it },
                    showToolbar = showToolbar,
                    onShowToolbarChange = { showToolbar = it },
                    showHistory = showHistory,
                    onShowHistoryChange = { showHistory = it },
                    lspEnabled = lspEnabled,
                    onLspEnabledChange = { lspEnabled = it },
                    isAiEnabled = aiEnabled,
                    onIsAiEnabledChange = { aiEnabled = it },
                    fontPath = fontPath,
                    onFontPathChange = { fontPath = it },
                    customSymbols = customSymbols,
                    onCustomSymbolsChange = { customSymbols = it },
                )
            }

            item(key = "terminal_settings") {
                TerminalSettingsItem(
                    onReset = {
                        coroutineScope.launch {
                            SetupWorker.resetTerminal(context)
                            Toast.makeText(context, R.string.toast_terminal_reset_success, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReinstall = { runInstall("Terminal environment", "reinstall_terminal") },
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_build_run_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }

            item(key = "android_dev_settings") {
                SimpleSettingsCard(
                    icon = Icons.Default.Android,
                    title = stringResource(R.string.settings_android_dev_title),
                    subtitle = stringResource(R.string.settings_android_dev_summary),
                    onClick = { showAndroidDevDialog = true }
                )
            }

            item(key = "gradle_settings") {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Build,
                    title = stringResource(R.string.settings_gradle_group_title),
                    subtitle = stringResource(R.string.settings_gradle_group_summary),
                    onClick = { showGradleDialog = true }
                )
            }

            item(key = "java_settings") {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.settings_java_group_title),
                    subtitle = stringResource(R.string.settings_java_group_summary),
                    onClick = { showJavaDialog = true }
                )
            }

            item(key = "lsp_settings_group") {
                SimpleSettingsCard(
                    icon = Icons.Outlined.Dns,
                    title = stringResource(R.string.settings_lsp_group_title),
                    subtitle = stringResource(R.string.settings_lsp_group_summary),
                    onClick = { showLspDialog = true }
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
            // 🔥 New: Auto-save settings entry
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
                    icon = Icons.Outlined.SaveAs, // Ensure this icon exists; fallback to Icons.Default.Save if needed
                    title = stringResource(R.string.settings_auto_save_backup_title),
                    subtitle =
                        if (currentOption == AutoSaveOption.OFF) {
                            stringResource(R.string.status_disabled)
                        } else {
                            stringResource(R.string.settings_auto_save_frequency, currentOptionLabel)
                        },
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

    // Dialogs
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

    if (showAndroidDevDialog) {
        AndroidDevDialog(
            onDismiss = { showAndroidDevDialog = false },
            isSdk33 = isAndroidSdk33Installed,
            isSdk34 = isAndroidSdk34Installed,
            isSdk35 = isAndroidSdk35Installed,
            isSdk36 = isAndroidSdk36Installed,
            isBt33 = isBuildTools33Installed,
            isBt34 = isBuildTools34Installed,
            isBt35 = isBuildTools35Installed,
            isBt36 = isBuildTools36Installed,
            isPl33 = isPlatform33Installed,
            isPl34 = isPlatform34Installed,
            isPl35 = isPlatform35Installed,
            isPl36 = isPlatform36Installed,
            isCm318 = isCmake318Installed,
            isCm320 = isCmake320Installed,
            isCm325 = isCmake325Installed,
            isCm431 = isCmake431Installed,
            isNdk26 = isNdk26Installed,
            isNdk27 = isNdk27Installed,
            isNdk28 = isNdk28Installed,
            isNdk29 = isNdk29Installed,
            onInstall = { name, cmd -> runInstall(name, cmd) }
        )
    }

    if (showGradleDialog) {
        GradleDialog(
            onDismiss = { showGradleDialog = false },
            isGradleInstalled = isGradleInstalled,
            prefs = generalPrefs,
            onInstall = { name, cmd -> runInstall(name, cmd) },
            onClearCache = {
                clearGradleCache(context) { success, msg ->
                    (context as android.app.Activity).runOnUiThread {
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.settings_clear_cache_success), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.settings_clear_cache_failed, msg), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    if (showJavaDialog) {
        JavaDialog(
            onDismiss = { showJavaDialog = false },
            isJdk17 = isJdk17Installed,
            isJdk21 = isJdk21Installed,
            isJdk25 = isJdk25Installed,
            onInstall = { name, cmd -> runInstall(name, cmd) }
        )
    }

    if (showLspDialog) {
        LspDialog(
            onDismiss = { showLspDialog = false },
            isJdtls = isJdtlsInstalled,
            isKotlinLs = isKotlinLsInstalled,
            isTsLs = isTsLsInstalled,
            isWebLs = isWebLsInstalled,
            onInstall = { name, cmd -> runInstall(name, cmd) }
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

    if (activeInstallJobName != null) {
        AlertDialog(
            onDismissRequest = {}, // Force non-cancellable
            title = {
                Text(
                    text = "Installing " + activeInstallJobName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Elapsed Time: $activeInstallElapsedTime", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Estimated Remaining: $activeInstallRemainingTime",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(progress = { activeInstallProgress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = String.format("%.0f%%", activeInstallProgress * 100),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            },
            confirmButton = {}, // No dismiss button to ensure safe install completion
        )
    }
}

// ================= Editor Settings Component (Refactored & Optimized) =================
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
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
                            if (fontPath.isBlank()) {
                                stringResource(R.string.font_system_default)
                            } else {
                                fontPath.substringAfterLast("/")
                            }
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
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
            }

            // Expanded Content
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
                    Spacer(modifier = Modifier.height(24.dp))

                    // === 1. Indentation Settings (Segmented Style) ===
                    Text(
                        stringResource(R.string.settings_indent_width),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // Spacing between buttons
                    ) {
                        val options = listOf(2, 4, 8)
                        options.forEach { option ->
                            val isSelected = tabWidth == option

                            // Color animation: Use primary color when selected, SurfaceContainerHigh when unselected
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
                                modifier =
                                    Modifier.weight(1f) // Three buttons divide the width equally
                                        .height(32.dp), // [Key] Reduce height for a more refined look
                                shape = RoundedCornerShape(4.dp), // [Key] 4dp small rounded corners, robust style
                                color = containerColor,
                                contentColor = contentColor,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = stringResource(R.string.settings_spaces_format, option),
                                        style = MaterialTheme.typography.labelMedium, // Use smaller font size
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // === 2. Font Settings (Combo Box mode) ===
                    Text(
                        stringResource(R.string.settings_editor_font),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Box container used to position DropdownMenu
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = fontPath,
                            onValueChange = onFontPathChange, // Allow direct input
                            modifier =
                                Modifier.fillMaxWidth(), // Don't use menuAnchor to prevent click on input field from
                            // triggering Menu
                            label = { Text(stringResource(R.string.settings_input_hint)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isFontDropdownExpanded = !isFontDropdownExpanded }) {
                                    Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.settings_select_preset))
                                }
                            },
                        )

                        // Dropdown menu
                        DropdownMenu(
                            expanded = isFontDropdownExpanded,
                            onDismissRequest = { isFontDropdownExpanded = false },
                            offset = DpOffset(0.dp, 0.dp),
                            modifier = Modifier.fillMaxWidth(0.9f), // Slightly adjust width to fit
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

                    // === 3. Behavior Switches ===
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

                    // === 4. Symbol Bar ===
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

// Helper
fun Modifier.scale(scale: Float) = this.graphicsLayer(scaleX = scale, scaleY = scale)

// ... Other components like ThemeSettingsItem, LogSettingsItem, etc., remain unchanged (refer to previously provided
// full code) ...
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
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
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
                                if (currentThemeState.isMonetEnabled) {
                                    stringResource(R.string.settings_dynamic_color)
                                } else {
                                    stringResource(R.string.settings_custom_appearance)
                                },
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
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
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
                                    // Mutually exclusive logic: When Monet is enabled, force close CustomTheme
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

@Composable
fun TerminalSettingsItem(onReset: () -> Unit, onReinstall: () -> Unit) {
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
                Icon(
                    imageVector = Icons.Outlined.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_terminal_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    if (!expanded) {
                        Text(
                            text = stringResource(R.string.settings_terminal_summary),
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
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
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
                            Button(onClick = onReset) { Text(stringResource(R.string.settings_terminal_reset)) }
                        }

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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text(stringResource(R.string.settings_terminal_reinstall))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AndroidDevDialog(
    onDismiss: () -> Unit,
    isSdk33: Boolean, isSdk34: Boolean, isSdk35: Boolean, isSdk36: Boolean,
    isBt33: Boolean, isBt34: Boolean, isBt35: Boolean, isBt36: Boolean,
    isPl33: Boolean, isPl34: Boolean, isPl35: Boolean, isPl36: Boolean,
    isCm318: Boolean, isCm320: Boolean, isCm325: Boolean, isCm431: Boolean,
    isNdk26: Boolean, isNdk27: Boolean, isNdk28: Boolean, isNdk29: Boolean,
    onInstall: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Android-SDK", "Build-Tools", "Platform", "CMake", "NDK")
    val selectedItems = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Android Development") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> {
                            val items = listOf(
                                "Android-SDK 33" to isSdk33,
                                "Android-SDK 34" to isSdk34,
                                "Android-SDK 35" to isSdk35,
                                "Android-SDK 36" to isSdk36
                            )
                            VersionListTab(
                                items = items,
                                typeKey = "sdk",
                                selectedItems = selectedItems,
                                onInstall = { label, version -> onInstall(label, "bash /root/scripts/android_sdk.sh install $version") },
                                onUninstall = { label, version -> onInstall(label, "bash /root/scripts/android_sdk.sh uninstall $version") }
                            )
                        }
                        1 -> {
                            val items = listOf(
                                "Build-Tools 33" to isBt33,
                                "Build-Tools 34" to isBt34,
                                "Build-Tools 35" to isBt35,
                                "Build-Tools 36" to isBt36
                            )
                            VersionListTab(
                                items = items,
                                typeKey = "bt",
                                selectedItems = selectedItems,
                                onInstall = { label, version -> onInstall(label, "bash /root/scripts/build_tools.sh install $version") },
                                onUninstall = { label, version -> onInstall(label, "bash /root/scripts/build_tools.sh uninstall $version") }
                            )
                        }
                        2 -> {
                            val items = listOf(
                                "Platform 33" to isPl33,
                                "Platform 34" to isPl34,
                                "Platform 35" to isPl35,
                                "Platform 36" to isPl36
                            )
                            VersionListTab(
                                items = items,
                                typeKey = "pl",
                                selectedItems = selectedItems,
                                onInstall = { label, version -> onInstall(label, "bash /root/scripts/platforms.sh install $version") },
                                onUninstall = { label, version -> onInstall(label, "bash /root/scripts/platforms.sh uninstall $version") }
                            )
                        }
                        3 -> {
                            val items = listOf(
                                "CMake 3.18" to isCm318,
                                "CMake 3.20" to isCm320,
                                "CMake 3.25" to isCm325,
                                "CMake 4.3.1" to isCm431
                            )
                            VersionListTab(
                                items = items,
                                typeKey = "cmake",
                                selectedItems = selectedItems,
                                onInstall = { label, version -> onInstall(label, "bash /root/scripts/cmake.sh install $version") },
                                onUninstall = { label, version -> onInstall(label, "bash /root/scripts/cmake.sh uninstall $version") }
                            )
                        }
                        4 -> {
                            val items = listOf(
                                "NDK 26" to isNdk26,
                                "NDK 27" to isNdk27,
                                "NDK 28" to isNdk28,
                                "NDK 29" to isNdk29
                            )
                            VersionListTab(
                                items = items,
                                typeKey = "ndk",
                                selectedItems = selectedItems,
                                onInstall = { label, version -> onInstall(label, "bash /root/scripts/ndk.sh install $version") },
                                onUninstall = { label, version -> onInstall(label, "bash /root/scripts/ndk.sh uninstall $version") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

@Composable
fun VersionListTab(
    items: List<Pair<String, Boolean>>,
    typeKey: String,
    selectedItems: MutableMap<String, Boolean>,
    onInstall: (String, String) -> Unit,
    onUninstall: (String, String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items.size) { i ->
            val (label, installed) = items[i]
            val versionStr = label.substringAfterLast(" ")
            val key = "${typeKey}_${versionStr}"

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedItems[key] ?: false,
                    onCheckedChange = { selectedItems[key] = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = if (installed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (installed) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (installed) "Installiert" else "Nicht installiert",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (installed) Color(0xFF4CAF50) else Color(0xFFF44336),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onInstall(label, versionStr) },
                        enabled = !installed
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Install", tint = if (!installed) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                    IconButton(
                        onClick = { onUninstall(label, versionStr) },
                        enabled = installed
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = if (installed) MaterialTheme.colorScheme.error else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun GradleDialog(
    onDismiss: () -> Unit,
    isGradleInstalled: Boolean,
    prefs: android.content.SharedPreferences,
    onInstall: (String, String) -> Unit,
    onClearCache: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Gradle", "Optionen", "Cache löschen")
    val options = listOf("--info", "--debug", "--stacktrace", "--scan", "--build-cache", "--offline")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gradle Konfiguration") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                alignment = Alignment.CenterHorizontally
                            ) {
                                Text("Gradle Build System", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = if (isGradleInstalled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isGradleInstalled) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = if (isGradleInstalled) "Installiert" else "Nicht installiert",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isGradleInstalled) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Button(
                                        onClick = { onInstall("Gradle", "bash /root/scripts/gradle.sh install") },
                                        enabled = !isGradleInstalled
                                    ) {
                                        Text("Installieren")
                                    }
                                    Button(
                                        onClick = { onInstall("Gradle", "bash /root/scripts/gradle.sh uninstall") },
                                        enabled = isGradleInstalled,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Deinstallieren")
                                    }
                                }
                            }
                        }
                        1 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(options.size) { idx ->
                                    val opt = options[idx]
                                    val key = "gradle_option_${opt.replace("-", "_")}"
                                    var checked by remember { mutableStateOf(prefs.getBoolean(key, false)) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            checked = !checked
                                            prefs.edit().putBoolean(key, checked).apply()
                                        }.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = {
                                                checked = it
                                                prefs.edit().putBoolean(key, it).apply()
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(opt, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                alignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Gradle Cache löschen",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Löscht alle zwischengespeicherten Abhängigkeiten und Build-Dateien von Gradle.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onClearCache,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Gradle Cache löschen")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

@Composable
fun JavaDialog(
    onDismiss: () -> Unit,
    isJdk17: Boolean, isJdk21: Boolean, isJdk25: Boolean,
    onInstall: (String, String) -> Unit
) {
    val items = listOf(
        "OpenJDK 17" to isJdk17,
        "OpenJDK 21" to isJdk21,
        "OpenJDK 25" to isJdk25
    )
    val selectedItems = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Java JDKs verwalten") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                items(items.size) { i ->
                    val (label, installed) = items[i]
                    val versionStr = label.substringAfter("OpenJDK ")
                    val key = "jdk_${versionStr}"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedItems[key] ?: false,
                            onCheckedChange = { selectedItems[key] = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (installed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (installed) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = if (installed) "Installiert" else "Nicht installiert",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (installed) Color(0xFF4CAF50) else Color(0xFFF44336),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onInstall(label, "bash /root/scripts/java.sh install $versionStr") },
                                enabled = !installed
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Install", tint = if (!installed) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                            IconButton(
                                onClick = { onInstall(label, "bash /root/scripts/java.sh uninstall $versionStr") },
                                enabled = installed
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = if (installed) MaterialTheme.colorScheme.error else Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

@Composable
fun LspDialog(
    onDismiss: () -> Unit,
    isJdtls: Boolean, isKotlinLs: Boolean, isTsLs: Boolean, isWebLs: Boolean,
    onInstall: (String, String) -> Unit
) {
    val items = listOf(
        "Java LSP (jdtls)" to (isJdtls to "jdtls"),
        "Kotlin LSP" to (isKotlinLs to "kotlin"),
        "TypeScript LSP" to (isTsLs to "typescript"),
        "Web LSP" to (isWebLs to "web")
    )
    val selectedItems = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LSP Server verwalten") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                items(items.size) { i ->
                    val (label, statePair) = items[i]
                    val (installed, typeKey) = statePair
                    val key = "lsp_${typeKey}"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedItems[key] ?: false,
                            onCheckedChange = { selectedItems[key] = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (installed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (installed) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = if (installed) "Installiert" else "Nicht installiert",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (installed) Color(0xFF4CAF50) else Color(0xFFF44336),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onInstall(label, "bash /root/scripts/lsp/lsp.sh install $typeKey") },
                                enabled = !installed
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Install", tint = if (!installed) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                            IconButton(
                                onClick = { onInstall(label, "bash /root/scripts/lsp/lsp.sh uninstall $typeKey") },
                                enabled = installed
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = if (installed) MaterialTheme.colorScheme.error else Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

fun clearGradleCache(context: Context, onResult: (Boolean, String) -> Unit) {
    thread {
        try {
            val prefixDir = context.filesDir.parentFile!!
            val alpineGradleCache = File(prefixDir, "local/alpine/root/.gradle/caches")
            val hostGradleCache = File("/data/data/com.termux/files/home/.gradle/caches")

            if (alpineGradleCache.exists()) {
                alpineGradleCache.deleteRecursively()
            }
            if (hostGradleCache.exists()) {
                hostGradleCache.deleteRecursively()
            }

            val cmd = arrayOf("sh", "-c", "bash /root/scripts/gradle.sh clear-cache")
            val fullCommand = com.scto.mobile.ide.ui.terminal.AlpineManager.buildProotCommand(context, cmd)
            val env = com.scto.mobile.ide.ui.terminal.AlpineManager.getProotEnv(context)
            val process = ProcessBuilder(fullCommand).apply {
                environment().putAll(env)
            }.start()
            process.waitFor()

            onResult(true, "Caches gelöscht.")
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Fehler")
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
                        if (isInstalled) {
                            infoText ?: stringResource(R.string.status_installed)
                        } else {
                            stringResource(R.string.status_not_installed)
                        },
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
