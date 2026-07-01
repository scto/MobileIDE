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

package com.scto.mobile.ide.ui.welcome

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scto.mobile.ide.R
import com.scto.mobile.ide.core.utils.PermissionManager
import com.scto.mobile.ide.ui.ThemeViewModel
import com.scto.mobile.ide.ui.components.ColorPickerDialog
import com.scto.mobile.ide.ui.components.MobileIDE_Icon
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(themeViewModel: ThemeViewModel, onWelcomeFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val themeState by themeViewModel.themeState.collectAsState()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    var storageGranted by remember { mutableStateOf(PermissionManager.hasRequiredPermissions(context)) }
    var installGranted by remember { mutableStateOf(PermissionManager.hasInstallPermission(context)) }
    var postNotificationsGranted by remember {
        mutableStateOf(PermissionManager.hasPostNotificationsPermission(context))
    }
    var notificationAccessGranted by remember { mutableStateOf(PermissionManager.hasNotificationAccess(context)) }
    var batteryOptimizationIgnored by remember {
        mutableStateOf(PermissionManager.isIgnoringBatteryOptimizations(context))
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(themeState.customColor) }
    var selectedModeIndex by remember { mutableIntStateOf(themeState.selectedModeIndex) }
    var selectedThemeIndex by remember {
        mutableIntStateOf(if (themeState.isCustomTheme) themeColors.size else themeState.selectedThemeIndex)
    }
    var isMonetEnabled by remember { mutableStateOf(themeState.isMonetEnabled) }

    val systemDark = isSystemInDarkTheme()
    val isDarkTheme =
        remember(selectedModeIndex, systemDark) {
            when (selectedModeIndex) {
                1 -> false
                2 -> true
                else -> systemDark
            }
        }

    // --- Logic fixes in WelcomeScreen.kt ---

    // 1. Calculate current preview theme data
    val currentPreviewTheme: ThemeColor? =
        remember(selectedThemeIndex, customColor, isMonetEnabled, isDarkTheme) {
            if (isMonetEnabled) {
                null
            } else if (selectedThemeIndex < themeColors.size) {
                themeColors[selectedThemeIndex]
            } else {
                // [Custom mode]
                // 1. Determine background color: Dark uses slightly bright pure black, Light uses slightly gray pure
                // white
                val bgDark = Color(0xFF121212)
                val bgLight = Color(0xFFF8F9FA) // Slightly grayish to avoid glaring

                // 2. Key point: Stuff customColor into Primary and Accent
                // This way the light blobs in WelcomeBackground can read your custom color!
                val customSpecDark =
                    ThemeColorSpec(
                        background = bgDark,
                        surface = Color(0xFF1E1E1E),
                        primary = customColor,
                        accent =
                            customColor, // Make both light blobs your custom color, or make the second blob slightly
                        // lighter
                    )
                val customSpecLight =
                    ThemeColorSpec(
                        background = bgLight,
                        surface = Color.White,
                        primary = customColor,
                        accent = customColor,
                    )

                ThemeColor("Custom", customSpecDark, customSpecLight)
            }
        }

    // 2. Calculate target background color (used for text inverse color calculation)
    val targetBg =
        if (isMonetEnabled) {
            if (isDarkTheme) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLowest
        } else if (selectedThemeIndex < themeColors.size) {
            val theme = themeColors[selectedThemeIndex]
            if (isDarkTheme) theme.dark.background else theme.light.background
        } else {
            // Custom color mode: If custom is selected, background uses color generated by Theme.kt logic
            MaterialTheme.colorScheme.background
        }

    val animatedBgColor by animateColorAsState(targetBg, tween(600), label = "bg_color")

    // 3. Smart text color: Increase threshold to avoid text turning white on light gray background
    val contentColor by
        animateColorAsState(
            if (animatedBgColor.luminance() > 0.45f) Color.Black else Color.White,
            tween(600),
            label = "content_color",
        )

    val permissionState =
        PermissionManager.rememberPermissionRequest(
            onPermissionGranted = { storageGranted = true },
            onPermissionDenied = {},
        )
    val requestInstallPermission =
        PermissionManager.rememberInstallPermissionRequest { granted -> installGranted = granted }
    val requestPostNotificationsPermission =
        PermissionManager.rememberPostNotificationsPermissionRequest { granted -> postNotificationsGranted = granted }
    val requestNotificationAccess =
        PermissionManager.rememberNotificationAccessRequest { granted -> notificationAccessGranted = granted }
    val requestBatteryOptimizationIgnore =
        PermissionManager.rememberIgnoreBatteryOptimizationsRequest { granted -> batteryOptimizationIgnored = granted }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                storageGranted = permissionState.hasPermissions()
                installGranted = PermissionManager.hasInstallPermission(context)
                postNotificationsGranted = PermissionManager.hasPostNotificationsPermission(context)
                notificationAccessGranted = PermissionManager.hasNotificationAccess(context)
                batteryOptimizationIgnored = PermissionManager.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                // Calculate the highlight color of the bottom navigation bar
                val activeColor =
                    when {
                        isMonetEnabled -> MaterialTheme.colorScheme.primary
                        // If it is custom mode (index == size), use customColor directly
                        selectedThemeIndex == themeColors.size -> customColor
                        // Otherwise use theme color
                        else ->
                            if (isDarkTheme) themeColors[selectedThemeIndex].dark.primary
                            else themeColors[selectedThemeIndex].light.primary
                    }

                WelcomeBottomBar(
                    pagerState = pagerState,
                    activeColor = activeColor,
                    isLastPage = pagerState.currentPage == 2,
                    onBack = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    onNext = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            themeViewModel.saveThemeConfig(
                                selectedModeIndex,
                                selectedThemeIndex,
                                customColor,
                                isMonetEnabled,
                                selectedThemeIndex == themeColors.size,
                            )
                            onWelcomeFinished()
                        }
                    },
                )
            },
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background layer
                WelcomeBackground(
                    currentTheme = currentPreviewTheme,
                    isDarkTheme = isDarkTheme,
                    monetPrimary = MaterialTheme.colorScheme.primary,
                    monetTertiary = MaterialTheme.colorScheme.tertiary,
                )

                // Content layer
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(paddingValues)) { page ->
                    when (page) {
                        0 -> IntroContent()
                        1 ->
                            PermissionsContent(
                                storageGranted = storageGranted,
                                installGranted = installGranted,
                                postNotificationsGranted = postNotificationsGranted,
                                notificationAccessGranted = notificationAccessGranted,
                                batteryOptimizationIgnored = batteryOptimizationIgnored,
                                onRequestStoragePermission = { permissionState.requestPermissions() },
                                onRequestInstallPermission = requestInstallPermission,
                                onRequestPostNotificationsPermission = requestPostNotificationsPermission,
                                onRequestNotificationAccess = requestNotificationAccess,
                                onRequestBatteryOptimizationIgnore = requestBatteryOptimizationIgnore,
                            )

                        2 ->
                            ThemeSetupContent(
                                selectedModeIndex = selectedModeIndex,
                                selectedThemeIndex = selectedThemeIndex,
                                isMonetEnabled = isMonetEnabled,
                                isDarkTheme = isDarkTheme, // Pass current mode
                                onMonetToggle = { isMonetEnabled = it },
                                onModeSelected = { selectedModeIndex = it },
                                onThemeSelected = { selectedThemeIndex = it },
                                onCustomColorClick = {
                                    selectedThemeIndex = themeColors.size
                                    showColorPicker = true
                                },
                            )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = customColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                customColor = color
                showColorPicker = false
                selectedThemeIndex = themeColors.size
            },
        )
    }
}

// --- Page 1: Intro ---
@Composable
private fun IntroContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(250.dp)) { MobileIDE_Icon() }
            // Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.app_name),
                style =
                    MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.welcome_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current.copy(alpha = 0.8f),
            )
        }
    }
}

// --- Page 2: Permissions ---
@Composable
private fun PermissionsContent(
    storageGranted: Boolean,
    installGranted: Boolean,
    postNotificationsGranted: Boolean,
    notificationAccessGranted: Boolean,
    batteryOptimizationIgnored: Boolean,
    onRequestStoragePermission: () -> Unit,
    onRequestInstallPermission: () -> Unit,
    onRequestPostNotificationsPermission: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestBatteryOptimizationIgnore: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.welcome_permissions_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.welcome_permissions_description),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = 0.8f),
        )

        Spacer(Modifier.height(24.dp))

        PermissionCard(
            Icons.Default.Folder,
            stringResource(R.string.welcome_permission_storage_title),
            stringResource(R.string.welcome_permission_storage_description),
            storageGranted,
            onRequestStoragePermission,
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            Icons.Default.Download,
            stringResource(R.string.welcome_permission_install_title),
            stringResource(R.string.welcome_permission_install_description),
            installGranted,
            onRequestInstallPermission,
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            Icons.Default.Notifications,
            stringResource(R.string.welcome_permission_push_notifications_title),
            stringResource(R.string.welcome_permission_push_notifications_description),
            postNotificationsGranted,
            onRequestPostNotificationsPermission,
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            Icons.Default.Visibility,
            stringResource(R.string.welcome_permission_read_notifications_title),
            stringResource(R.string.welcome_permission_read_notifications_description),
            notificationAccessGranted,
            onRequestNotificationAccess,
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            Icons.Default.BatteryAlert,
            stringResource(R.string.welcome_permission_battery_optimization_title),
            stringResource(R.string.welcome_permission_battery_optimization_description),
            batteryOptimizationIgnored,
            onRequestBatteryOptimizationIgnore,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSetupContent(
    selectedModeIndex: Int,
    selectedThemeIndex: Int,
    isMonetEnabled: Boolean,
    isDarkTheme: Boolean,
    onMonetToggle: (Boolean) -> Unit,
    onModeSelected: (Int) -> Unit,
    onThemeSelected: (Int) -> Unit,
    onCustomColorClick: () -> Unit,
) {
    val modeOptions =
        listOf(
            stringResource(R.string.action_follow_system),
            stringResource(R.string.action_light),
            stringResource(R.string.action_dark),
        )

    // 1. Remove padding from parent container, keep only vertical scrolling
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        // 2. Add padding individually to internal elements
        Text(
            stringResource(R.string.welcome_appearance_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 24.dp), // <--- Add here
        )
        Spacer(Modifier.height(32.dp))

        // Mode selection button
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp) // <--- Add here
        ) {
            modeOptions.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedModeIndex == index,
                    onClick = { onModeSelected(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modeOptions.size),
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = LocalContentColor.current,
                        ),
                ) {
                    Text(label)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.welcome_dynamic_color)) },
                trailingContent = { Switch(checked = isMonetEnabled, onCheckedChange = onMonetToggle) },
                colors =
                    ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = LocalContentColor.current,
                        trailingIconColor = LocalContentColor.current,
                    ),
                modifier =
                    Modifier.padding(horizontal = 8.dp), // ListItem comes with some padding, just adjust slightly here
            )
        }

        // 3. Theme list: Switch to LazyRow to fix truncation issue
        AnimatedVisibility(visible = !isMonetEnabled) {
            Column {
                Spacer(Modifier.height(24.dp))

                // Use LazyRow instead of Row + Scroll
                LazyRow(
                    // Key point: contentPadding allows content to scroll to the edge of the screen, but has indentation
                    // at start
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Render preset themes
                    itemsIndexed(themeColors) { index, theme ->
                        ThemePreviewCard(
                            theme = theme,
                            isSelected = selectedThemeIndex == index,
                            isDarkTheme = isDarkTheme,
                            onClick = { onThemeSelected(index) },
                        )
                    }

                    // Render custom button
                    item {
                        CustomThemeCard(
                            isSelected = selectedThemeIndex == themeColors.size,
                            onClick = onCustomColorClick,
                        )
                    }
                }
            }
        }
    }
}
