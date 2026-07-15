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

package com.scto.mobile.ide

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.rk.extension.extensionManager
import com.rk.extension.loader.loadAllExtensions
import com.rk.extension.manager.ExtensionManager
import com.scto.mobile.ide.core.utils.*
import com.scto.mobile.ide.ui.ThemeViewModel
import com.scto.mobile.ide.ui.ThemeViewModelFactory
import com.scto.mobile.ide.ui.editor.TextMateInitializer
import com.scto.mobile.ide.ui.theme.AppTheme
import com.scto.mobile.ide.ui.welcome.WelcomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    val fileManager = com.scto.mobile.ide.files.FileManager(this)

    companion object {
        private var activityRef = java.lang.ref.WeakReference<MainActivity?>(null)
        var instance: MainActivity?
            get() = activityRef.get()
            private set(value) {
                activityRef = java.lang.ref.WeakReference(value)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        getExternalFilesDir("logs")

        enableEdgeToEdge(
            statusBarStyle =
                androidx.activity.SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                androidx.activity.SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        window.isNavigationBarContrastEnforced = false

        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val crashFile = java.io.File(getExternalFilesDir(null), "crash.log")
                crashFile.appendText("\n\n--- CRASH AT ${java.util.Date()} ---\n")
                crashFile.appendText("Thread: ${thread.name}\n")
                val sw = java.io.StringWriter()
                exception.printStackTrace(java.io.PrintWriter(sw))
                crashFile.appendText(sw.toString())
            } catch (e: Exception) {
                // Ignore
            }
            defaultExceptionHandler?.uncaughtException(thread, exception)
        }

        init()
    }

    private fun init() {
        // Initialize base components
        TextMateInitializer.initialize(this)
        AppLanguageManager.initialize(this)

        // Initialize Extension Manager and load extensions
        extensionManager = ExtensionManager(application)

        // Setup a dummy extension for user testing
        val testExtDir = java.io.File(application.filesDir.parentFile, "local/extensions/com.rk.test_extension")
        if (!testExtDir.exists()) {
            testExtDir.mkdirs()
            val manifestJson =
                """
                {
                  "id": "com.rk.test_extension",
                  "name": "Test Extension",
                  "mainClass": "com.rk.test.TestExtension",
                  "version": "1.0.0",
                  "description": "This is a test extension to verify that the ExtensionSettingsScreen displays, toggles, and uninstalls local extensions correctly.",
                  "author": {
                    "displayName": "Developer"
                  },
                  "minAppVersion": 1,
                  "repository": "https://github.com/example/test-extension",
                  "license": "GPLv3"
                }
                """
                    .trimIndent()
            java.io.File(testExtDir, "manifest.json").writeText(manifestJson)
            java.io.File(testExtDir, "extension.apk").createNewFile()
        }

        // Install plugins bundled in assets/bundled_plugins/ (e.g. java_lsp, kotlin_lsp)
        // These are installed once on first run and updated when the asset version is newer.
        lifecycleScope.launch(Dispatchers.IO) {
            com.scto.mobile.ide.core.utils.BundledPluginLoader.install(application)
            extensionManager.indexLocalExtensions()
            extensionManager.loadAllExtensions()
            com.rk.lsp.LspRegistry.loadExternalServers(applicationContext)
        }

        com.rk.lsp.ScriptedLspServer.terminalLauncher =
            { activity: android.app.Activity, scriptFile: java.io.File, flags: List<String> ->
                com.scto.mobile.ide.exec.pendingCommand =
                    com.scto.mobile.ide.exec.TerminalCommand(
                        exe = "bash",
                        args = (listOf(scriptFile.absolutePath) + flags).toTypedArray(),
                        id = "lsp_installer_${scriptFile.name}",
                        workingDir = scriptFile.parentFile?.absolutePath,
                    )
                // Note: Since this requires navigating to the terminal screen, ideally we would broadcast an event or
                // navigate via NavController.
                // As a simple delegate, we just set the pending command which will be run when the terminal opens.
            }

        setContent {
            val activityContext = this@MainActivity
            val currentLanguageOption by AppLanguageManager.currentOption.collectAsState()
            val localizedContext =
                remember(activityContext, currentLanguageOption) {
                    AppLanguageManager.createLocalizedContext(activityContext, currentLanguageOption)
                }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides activityContext,
                androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides activityContext,
            ) {
                val context = LocalContext.current
                val navController = rememberNavController()
                val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(activityContext))
                val themeState by themeViewModel.themeState.collectAsState()

                // Log configuration
                val logConfigRepository = remember { LogConfigRepository(activityContext) }
                val logConfigState by logConfigRepository.logConfigFlow.collectAsState(initial = LogConfigState())

                // ✅ Core change 1: Change dependencies to logConfigState to re-initialize upon config changes
                LaunchedEffect(logConfigState) {
                    if (logConfigState.isLoaded) {
                        // ✅ Change: Call updateConfig to update configuration dynamically
                        LogCatcher.updateConfig(logConfigState)
                        LogCatcher.i(
                            "MainActivity",
                            "Log system configuration updated - Enabled: ${logConfigState.isLogEnabled}",
                        )
                    }
                }



                if (!themeState.isLoaded || !logConfigState.isLoaded) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                } else {
                    AppTheme(themeState = themeState) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            // Sync global night mode for Android Views (Editor, Terminal, etc.)
                            LaunchedEffect(themeState.selectedModeIndex) {
                                val mode =
                                    when (themeState.selectedModeIndex) {
                                        1 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                        2 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                        else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                    }
                                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
                            }

                            // ✅ Core change 2: Initialize state according to WelcomePreferences
                            var showWelcomeScreen by remember {
                                mutableStateOf(!WelcomePreferences.isWelcomeCompleted(context))
                            }

                            AnimatedContent(
                                targetState = showWelcomeScreen,
                                label = "ScreenTransition",
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(durationMillis = 500)) togetherWith
                                        fadeOut(animationSpec = tween(durationMillis = 500))
                                },
                            ) { isWelcomeTarget ->
                                if (isWelcomeTarget) {
                                    WelcomeScreen(
                                        themeViewModel = themeViewModel,
                                        onWelcomeFinished = {
                                            // ✅ Core change 3: Mark as completed at the end of the welcome flow
                                            WelcomePreferences.setWelcomeCompleted(context)
                                            LogCatcher.i("MainActivity", "Welcome flow completed, entering main app")
                                            showWelcomeScreen = false
                                        },
                                    )
                                } else {
                                    MainScreen(
                                        navController = navController,
                                        themeViewModel = themeViewModel,
                                        logConfigRepository = logConfigRepository,
                                        logConfigState = logConfigState,
                                    )
                                    com.scto.mobile.ide.ui.components.UpdateCheckerDialog(context = activityContext)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
