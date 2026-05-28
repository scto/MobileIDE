// Copyright 2025 Thomas Schmid
package com.mobile.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

import com.mobile.ide.core.ui.components.*
import com.mobile.ide.core.ui.theme.*
import com.mobile.ide.core.utils.*
import com.mobile.ide.ui.editor.components.TextMateInitializer
import com.mobile.ide.ui.welcome.WelcomeScreen

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        initApp()
    }

    private fun initApp() {
        TextMateInitializer.initialize(this)

        setContent {
            val context = LocalContext.current
            val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(context))
            val themeState by themeViewModel.themeState.collectAsState()

            val logConfigRepository = remember { LogConfigRepository(context) }
            val logConfigState by logConfigRepository.logConfigFlow.collectAsState(initial = LogConfigState())

            LaunchedEffect(logConfigState) {
                if (logConfigState.isLoaded) {
                    LogCatcher.updateConfig(logConfigState)
                    LogCatcher.i(TAG, context.getString(R.string.log_config_updated, logConfigState.isLogEnabled))
                }
            }

            if (!themeState.isLoaded || !logConfigState.isLoaded) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            } else {
                MyComposeApplicationTheme(themeState = themeState) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                                        WelcomePreferences.setWelcomeCompleted(context)
                                        LogCatcher.i(TAG, context.getString(R.string.log_welcome_completed))
                                        showWelcomeScreen = false
                                    },
                                )
                            } else {
                                App(
                                    themeViewModel = themeViewModel,
                                    logConfigRepository = logConfigRepository,
                                    logConfigState = logConfigState,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
