package com.mobileide.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobileide.app.ui.screens.*
import com.mobileide.feature.settings.app.EditorSelectionScreen as FeatureEditorSelectionScreen
import com.mobileide.feature.settings.theme.ThemeScreen as FeatureThemeScreen
import com.mobileide.feature.settings.terminal.TerminalSettingsScreen as FeatureTerminalScreen
import com.mobileide.feature.settings.support.SupportScreen as FeatureSupportScreen
import com.mobileide.feature.settings.runners.RunnersScreen as FeatureRunnersScreen
import com.mobileide.feature.settings.lsp.LspScreen as FeatureLspScreen
import com.mobileide.feature.settings.language.LanguageScreen as FeatureLanguageScreen
import com.mobileide.feature.settings.keybinds.KeybindsScreen as FeatureKeybindsScreen
import com.mobileide.feature.settings.git.GitSettingsScreen as FeatureGitSettingsScreen
import com.mobileide.feature.settings.extension.ExtensionScreen as FeatureExtensionScreen
import com.mobileide.feature.settings.editor.EditorSettingsScreen as FeatureEditorSettingsScreen
import com.mobileide.feature.settings.debugOptions.DebugOptionsScreen as FeatureDebugScreen
import com.mobileide.feature.settings.about.AboutScreen as FeatureAboutScreen
import com.mobileide.feature.settings.SettingsScreen as FeatureSettingsScreen
import com.mobileide.feature.settings.app.EditorPreference
import com.mobileide.feature.settings.app.EditorSelectionScreen
import com.mobileide.feature.editor.FeatureEditorScreen
import com.mobileide.app.ui.screens.settings.SettingsScreen
import com.mobileide.app.ui.screens.settings.about.AboutScreen
import com.mobileide.app.ui.screens.settings.debugOptions.DebugOptionsScreen
import com.mobileide.app.ui.screens.settings.editor.EditorSettingsScreen as SettingsEditorScreen
import com.mobileide.app.ui.screens.settings.extension.ExtensionScreen
import com.mobileide.app.ui.screens.settings.git.GitSettingsScreen
import com.mobileide.app.ui.screens.settings.keybinds.KeybindsScreen
import com.mobileide.app.ui.screens.settings.language.LanguageScreen
import com.mobileide.app.ui.screens.settings.lsp.LspScreen
import com.mobileide.app.ui.screens.settings.runners.RunnersScreen
import com.mobileide.app.ui.screens.settings.support.SupportScreen
import com.mobileide.app.ui.screens.settings.terminal.TerminalSettingsScreen
import com.mobileide.app.ui.screens.settings.theme.ThemeScreen
import com.mobileide.app.utils.commands.CommandRegistry
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Let Compose draw behind system bars — required for transparent bars
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialise ThemePreferences BEFORE setContent so the first frame
        // can read monet/amoled/themeId synchronously without a coroutine.
        ThemePreferences.init(this)
        dynamicM3Theme.value = ThemePreferences.monet
        amoledM3Theme.value  = ThemePreferences.amoled

        setContent {
            MobileIdeTheme {
                // Dynamically adjust status-bar and navigation-bar icon colours
                // to match the current light/dark mode — this is the correct
                // way to do it in an edge-to-edge Compose app.
                val isDark = isSystemInDarkTheme()
                val insetsController = remember {
                    WindowInsetsControllerCompat(window, window.decorView)
                }
                SideEffect {
                    // true  → dark icons (use on light backgrounds)
                    // false → light icons (use on dark backgrounds)
                    insetsController.isAppearanceLightStatusBars     = !isDark
                    insetsController.isAppearanceLightNavigationBars = !isDark
                }

                MobileIDEApp()
            }
        }
    }
}

@Composable
fun MobileIDEApp() {
    val vm: IDEViewModel = viewModel()
    val screen by vm.currentScreen.collectAsState()

    // Initialise command registry once per ViewModel lifetime
    LaunchedEffect(vm) { CommandRegistry.build(vm) }

    // Command Palette overlay — shown on top of everything
    CommandPaletteScreen(vm)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                when (targetState) {
                    Screen.ONBOARDING      -> fadeIn()                         togetherWith fadeOut()
                    Screen.HOME            -> fadeIn()                         togetherWith fadeOut()
                    Screen.EDITOR          -> slideInHorizontally { it }       togetherWith slideOutHorizontally { -it }
                    Screen.TERMINAL        -> slideInVertically { it }         togetherWith slideOutVertically { -it }
                    Screen.SETTINGS        -> slideInHorizontally { -it }      togetherWith slideOutHorizontally { it }
                    Screen.EDITOR_SETTINGS -> slideInHorizontally { it }       togetherWith slideOutHorizontally { -it }

                    Screen.GIT             -> slideInVertically { -it }        togetherWith slideOutVertically { it }
                    Screen.LOGCAT          -> slideInHorizontally { it }       togetherWith slideOutHorizontally { -it }
                    Screen.DEPENDENCIES    -> slideInHorizontally { it }       togetherWith slideOutHorizontally { -it }
                    Screen.RUN_CONFIG      -> slideInVertically { it }         togetherWith slideOutVertically { -it }
                    Screen.PROJECT_SEARCH  -> slideInHorizontally { it }       togetherWith slideOutHorizontally { -it }
                    Screen.GRADLE_TASKS    -> slideInVertically { -it }        togetherWith slideOutVertically { it }
                    Screen.DIFF_VIEWER     -> slideInHorizontally { -it }      togetherWith slideOutHorizontally { it }
                    Screen.PROJECT_STATS   -> slideInVertically { it }         togetherWith slideOutVertically { -it }
                    Screen.TODO_PANEL      -> slideInVertically { -it }        togetherWith slideOutVertically { it }
                    Screen.PACKAGE_MANAGER -> slideInHorizontally { it }       togetherWith slideOutHorizontally { -it }
                    Screen.KEYBOARD_HELP   -> slideInHorizontally { -it }      togetherWith slideOutHorizontally { it }
                    Screen.SETUP_WIZARD    -> fadeIn()                         togetherWith fadeOut()
                    Screen.DEV_SETTINGS    -> slideInHorizontally { it }       togetherWith slideOutHorizontally { -it }
                    Screen.LOG_VIEWER      -> slideInVertically { it }         togetherWith slideOutVertically { -it }
                    Screen.APP_THEME             -> slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    Screen.FEATURE_EDITOR        -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_EDITOR_SELECT-> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_EDITOR   -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_THEME    -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_KEYBINDS -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_LANGUAGE -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_LSP      -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_RUNNERS  -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_GIT      -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_TERMINAL -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_EXTENSION-> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_DEBUG    -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_SUPPORT  -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                    Screen.SETTINGS_ABOUT    -> slideInHorizontally { it }  togetherWith slideOutHorizontally { -it }
                }
            },
            label = "nav"
        ) { s ->
            when (s) {
                Screen.ONBOARDING      -> OnboardingScreen(vm)
                Screen.HOME            -> HomeScreen(vm)
                Screen.EDITOR          -> EditorScreen(vm)
                Screen.TERMINAL        -> TerminalScreen(vm)
                Screen.SETTINGS        -> FeatureSettingsScreen(onBack = { vm.navigate(Screen.HOME) }, onNavigate = { route ->
                    when (route) {
                        "HOME"                  -> vm.navigate(Screen.HOME)
                        "SETTINGS"              -> vm.navigate(Screen.SETTINGS)
                        "SETTINGS_EDITOR"       -> vm.navigate(Screen.SETTINGS_EDITOR)
                        "SETTINGS_THEME"        -> vm.navigate(Screen.SETTINGS_THEME)
                        "SETTINGS_KEYBINDS"     -> vm.navigate(Screen.SETTINGS_KEYBINDS)
                        "SETTINGS_LANGUAGE"     -> vm.navigate(Screen.SETTINGS_LANGUAGE)
                        "SETTINGS_LSP"          -> vm.navigate(Screen.SETTINGS_LSP)
                        "SETTINGS_RUNNERS"      -> vm.navigate(Screen.SETTINGS_RUNNERS)
                        "SETTINGS_GIT"          -> vm.navigate(Screen.SETTINGS_GIT)
                        "SETTINGS_TERMINAL"     -> vm.navigate(Screen.SETTINGS_TERMINAL)
                        "SETTINGS_EXTENSION"    -> vm.navigate(Screen.SETTINGS_EXTENSION)
                        "SETTINGS_DEBUG"        -> vm.navigate(Screen.SETTINGS_DEBUG)
                        "SETTINGS_SUPPORT"      -> vm.navigate(Screen.SETTINGS_SUPPORT)
                        "SETTINGS_ABOUT"        -> vm.navigate(Screen.SETTINGS_ABOUT)
                        "SETTINGS_EDITOR_SELECT"-> vm.navigate(Screen.SETTINGS_EDITOR_SELECT)
                        "FEATURE_EDITOR"        -> vm.navigate(Screen.FEATURE_EDITOR)
                        else -> if (route.startsWith("setEditor:")) {
                            val pref = com.mobileide.feature.settings.app.EditorPreference.valueOf(
                                route.removePrefix("setEditor:"))
                            vm.setPreferredEditor(pref)
                        }
                    }
                })
                Screen.EDITOR_SETTINGS -> SettingsEditorScreen(vm)
                Screen.GIT             -> GitScreen(vm)
                Screen.LOGCAT          -> LogCatScreen(vm)
                Screen.DEPENDENCIES    -> DependencyScreen(vm)
                Screen.RUN_CONFIG      -> RunConfigScreen(vm)
                Screen.PROJECT_SEARCH  -> ProjectSearchScreen(vm)
                Screen.GRADLE_TASKS    -> GradleTasksScreen(vm)
                Screen.DIFF_VIEWER     -> DiffViewerScreen(vm)
                Screen.PROJECT_STATS   -> ProjectStatsScreen(vm)
                Screen.TODO_PANEL      -> TodoPanelScreen(vm)
                Screen.PACKAGE_MANAGER -> PackageManagerScreen(vm)
                Screen.KEYBOARD_HELP   -> KeyboardHelpScreen(vm)
                Screen.SETUP_WIZARD    -> SetupWizardScreen(vm)
                Screen.DEV_SETTINGS    -> DevSettingsScreen(vm)
                Screen.LOG_VIEWER      -> LogViewerScreen(vm)
                Screen.APP_THEME              -> AppThemeScreen(vm)
                Screen.FEATURE_EDITOR         -> {
                    val pref by vm.preferredEditor.collectAsState()
                    if (pref == EditorPreference.FEATURE) {
                        FeatureEditorScreen(onBack = { vm.navigate(Screen.HOME) })
                    } else {
                        EditorScreen(vm)
                    }
                }
                Screen.SETTINGS_EDITOR_SELECT -> FeatureEditorSelectionScreen(onBack = { vm.navigate(Screen.SETTINGS) }, onNavigate = { route ->
                    when (route) {
                        "HOME"                  -> vm.navigate(Screen.HOME)
                        "SETTINGS"              -> vm.navigate(Screen.SETTINGS)
                        "SETTINGS_EDITOR"       -> vm.navigate(Screen.SETTINGS_EDITOR)
                        "SETTINGS_THEME"        -> vm.navigate(Screen.SETTINGS_THEME)
                        "SETTINGS_KEYBINDS"     -> vm.navigate(Screen.SETTINGS_KEYBINDS)
                        "SETTINGS_LANGUAGE"     -> vm.navigate(Screen.SETTINGS_LANGUAGE)
                        "SETTINGS_LSP"          -> vm.navigate(Screen.SETTINGS_LSP)
                        "SETTINGS_RUNNERS"      -> vm.navigate(Screen.SETTINGS_RUNNERS)
                        "SETTINGS_GIT"          -> vm.navigate(Screen.SETTINGS_GIT)
                        "SETTINGS_TERMINAL"     -> vm.navigate(Screen.SETTINGS_TERMINAL)
                        "SETTINGS_EXTENSION"    -> vm.navigate(Screen.SETTINGS_EXTENSION)
                        "SETTINGS_DEBUG"        -> vm.navigate(Screen.SETTINGS_DEBUG)
                        "SETTINGS_SUPPORT"      -> vm.navigate(Screen.SETTINGS_SUPPORT)
                        "SETTINGS_ABOUT"        -> vm.navigate(Screen.SETTINGS_ABOUT)
                        "SETTINGS_EDITOR_SELECT"-> vm.navigate(Screen.SETTINGS_EDITOR_SELECT)
                        "FEATURE_EDITOR"        -> vm.navigate(Screen.FEATURE_EDITOR)
                        else -> if (route.startsWith("setEditor:")) {
                            val pref = com.mobileide.feature.settings.app.EditorPreference.valueOf(
                                route.removePrefix("setEditor:"))
                            vm.setPreferredEditor(pref)
                        }
                    }
                })
                Screen.SETTINGS_EDITOR   -> FeatureEditorSettingsScreen(onBack = { vm.navigate(Screen.SETTINGS) }, onNavigate = { route ->
                    when (route) {
                        "HOME"                  -> vm.navigate(Screen.HOME)
                        "SETTINGS"              -> vm.navigate(Screen.SETTINGS)
                        "SETTINGS_EDITOR"       -> vm.navigate(Screen.SETTINGS_EDITOR)
                        "SETTINGS_THEME"        -> vm.navigate(Screen.SETTINGS_THEME)
                        "SETTINGS_KEYBINDS"     -> vm.navigate(Screen.SETTINGS_KEYBINDS)
                        "SETTINGS_LANGUAGE"     -> vm.navigate(Screen.SETTINGS_LANGUAGE)
                        "SETTINGS_LSP"          -> vm.navigate(Screen.SETTINGS_LSP)
                        "SETTINGS_RUNNERS"      -> vm.navigate(Screen.SETTINGS_RUNNERS)
                        "SETTINGS_GIT"          -> vm.navigate(Screen.SETTINGS_GIT)
                        "SETTINGS_TERMINAL"     -> vm.navigate(Screen.SETTINGS_TERMINAL)
                        "SETTINGS_EXTENSION"    -> vm.navigate(Screen.SETTINGS_EXTENSION)
                        "SETTINGS_DEBUG"        -> vm.navigate(Screen.SETTINGS_DEBUG)
                        "SETTINGS_SUPPORT"      -> vm.navigate(Screen.SETTINGS_SUPPORT)
                        "SETTINGS_ABOUT"        -> vm.navigate(Screen.SETTINGS_ABOUT)
                        "SETTINGS_EDITOR_SELECT"-> vm.navigate(Screen.SETTINGS_EDITOR_SELECT)
                        "FEATURE_EDITOR"        -> vm.navigate(Screen.FEATURE_EDITOR)
                        else -> if (route.startsWith("setEditor:")) {
                            val pref = com.mobileide.feature.settings.app.EditorPreference.valueOf(
                                route.removePrefix("setEditor:"))
                            vm.setPreferredEditor(pref)
                        }
                    }
                })
                Screen.SETTINGS_THEME    -> FeatureThemeScreen(onBack = { vm.navigate(Screen.SETTINGS) }, onNavigate = { route ->
                    when (route) {
                        "HOME"                  -> vm.navigate(Screen.HOME)
                        "SETTINGS"              -> vm.navigate(Screen.SETTINGS)
                        "SETTINGS_EDITOR"       -> vm.navigate(Screen.SETTINGS_EDITOR)
                        "SETTINGS_THEME"        -> vm.navigate(Screen.SETTINGS_THEME)
                        "SETTINGS_KEYBINDS"     -> vm.navigate(Screen.SETTINGS_KEYBINDS)
                        "SETTINGS_LANGUAGE"     -> vm.navigate(Screen.SETTINGS_LANGUAGE)
                        "SETTINGS_LSP"          -> vm.navigate(Screen.SETTINGS_LSP)
                        "SETTINGS_RUNNERS"      -> vm.navigate(Screen.SETTINGS_RUNNERS)
                        "SETTINGS_GIT"          -> vm.navigate(Screen.SETTINGS_GIT)
                        "SETTINGS_TERMINAL"     -> vm.navigate(Screen.SETTINGS_TERMINAL)
                        "SETTINGS_EXTENSION"    -> vm.navigate(Screen.SETTINGS_EXTENSION)
                        "SETTINGS_DEBUG"        -> vm.navigate(Screen.SETTINGS_DEBUG)
                        "SETTINGS_SUPPORT"      -> vm.navigate(Screen.SETTINGS_SUPPORT)
                        "SETTINGS_ABOUT"        -> vm.navigate(Screen.SETTINGS_ABOUT)
                        "SETTINGS_EDITOR_SELECT"-> vm.navigate(Screen.SETTINGS_EDITOR_SELECT)
                        "FEATURE_EDITOR"        -> vm.navigate(Screen.FEATURE_EDITOR)
                        else -> if (route.startsWith("setEditor:")) {
                            val pref = com.mobileide.feature.settings.app.EditorPreference.valueOf(
                                route.removePrefix("setEditor:"))
                            vm.setPreferredEditor(pref)
                        }
                    }
                })
                Screen.SETTINGS_KEYBINDS -> FeatureKeybindsScreen(onBack = { vm.navigate(Screen.SETTINGS) }, onNavigate = { route ->
                    when (route) {
                        "HOME"                  -> vm.navigate(Screen.HOME)
                        "SETTINGS"              -> vm.navigate(Screen.SETTINGS)
                        "SETTINGS_EDITOR"       -> vm.navigate(Screen.SETTINGS_EDITOR)
                        "SETTINGS_THEME"        -> vm.navigate(Screen.SETTINGS_THEME)
                        "SETTINGS_KEYBINDS"     -> vm.navigate(Screen.SETTINGS_KEYBINDS)
                        "SETTINGS_LANGUAGE"     -> vm.navigate(Screen.SETTINGS_LANGUAGE)
                        "SETTINGS_LSP"          -> vm.navigate(Screen.SETTINGS_LSP)
                        "SETTINGS_RUNNERS"      -> vm.navigate(Screen.SETTINGS_RUNNERS)
                        "SETTINGS_GIT"          -> vm.navigate(Screen.SETTINGS_GIT)
                        "SETTINGS_TERMINAL"     -> vm.navigate(Screen.SETTINGS_TERMINAL)
                        "SETTINGS_EXTENSION"    -> vm.navigate(Screen.SETTINGS_EXTENSION)
                        "SETTINGS_DEBUG"        -> vm.navigate(Screen.SETTINGS_DEBUG)
                        "SETTINGS_SUPPORT"      -> vm.navigate(Screen.SETTINGS_SUPPORT)
                        "SETTINGS_ABOUT"        -> vm.navigate(Screen.SETTINGS_ABOUT)
                        "SETTINGS_EDITOR_SELECT"-> vm.navigate(Screen.SETTINGS_EDITOR_SELECT)
                        "FEATURE_EDITOR"        -> vm.navigate(Screen.FEATURE_EDITOR)
                        else -> if (route.startsWith("setEditor:")) {
                            val pref = com.mobileide.feature.settings.app.EditorPreference.valueOf(
                                route.removePrefix("setEditor:"))
                            vm.setPreferredEditor(pref)
                        }
                    }
                })
                Screen.SETTINGS_LANGUAGE -> FeatureLanguageScreen(onBack = { vm.navigate(Screen.SETTINGS) })
                Screen.SETTINGS_LSP      -> FeatureLspScreen(onBack = { vm.navigate(Screen.SETTINGS_EDITOR) })
                Screen.SETTINGS_RUNNERS  -> FeatureRunnersScreen(onBack = { vm.navigate(Screen.SETTINGS) })
                Screen.SETTINGS_GIT      -> FeatureGitSettingsScreen(onBack = { vm.navigate(Screen.SETTINGS) })
                Screen.SETTINGS_TERMINAL -> FeatureTerminalScreen(onBack = { vm.navigate(Screen.SETTINGS) })
                Screen.SETTINGS_EXTENSION-> FeatureExtensionScreen(onBack = { vm.navigate(Screen.SETTINGS) })
                Screen.SETTINGS_DEBUG    -> FeatureDebugScreen(onBack = { vm.navigate(Screen.SETTINGS) })
                Screen.SETTINGS_SUPPORT  -> FeatureSupportScreen(onBack = { vm.navigate(Screen.SETTINGS) })
                Screen.SETTINGS_ABOUT    -> FeatureAboutScreen(onBack = { vm.navigate(Screen.SETTINGS) })
            }
        }

        // Bottom nav only on Home and Settings
        if (screen == Screen.HOME || screen == Screen.SETTINGS) {
            NavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick  = { vm.navigate(Screen.HOME) },
                    icon     = { Icon(Icons.Default.Home, null) },
                    label    = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick  = { vm.navigate(Screen.TERMINAL) },
                    icon     = { Icon(Icons.Default.Terminal, null) },
                    label    = { Text("Terminal") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick  = { vm.navigate(Screen.SETUP_WIZARD) },
                    icon     = { Icon(Icons.Default.AutoFixHigh, null) },
                    label    = { Text("Setup") }
                )
                NavigationBarItem(
                    selected = screen == Screen.SETTINGS,
                    onClick  = { vm.navigate(Screen.SETTINGS) },
                    icon     = { Icon(Icons.Default.Settings, null) },
                    label    = { Text("Settings") }
                )
            }
        }
    }
}
