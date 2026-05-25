// Copyright 2025 Thomas Schmid
package com.mobile.ide.ui.welcome

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import com.mobile.ide.R
// Importe aktualisieren:
import com.mobile.ide.core.resources.Res
import com.mobile.ide.core.utils.*
import com.mobile.ide.core.ui.components.ColorPickerDialog
import com.mobile.ide.core.ui.theme.*

@Composable
fun WelcomeScreen(
    themeViewModel: ThemeViewModel,
    onWelcomeFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val themeState by themeViewModel.themeState.collectAsState()

    var currentStep by remember { mutableStateOf(WelcomeStep.INTRO) }

    var storageGranted by remember { mutableStateOf(false) }
    var installGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true 
            }
        )
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(themeState.customColor) }
    var selectedModeIndex by remember { mutableStateOf(themeState.selectedModeIndex) }
    var selectedThemeIndex by remember {
        mutableStateOf(if (themeState.isCustomTheme) themeColors.size else themeState.selectedThemeIndex)
    }
    var isMonetEnabled by remember { mutableStateOf(themeState.isMonetEnabled) }

    val permissionState = PermissionManager.rememberPermissionRequest(
        onPermissionGranted = {
            storageGranted = true
            LogCatcher.i("WelcomeScreen", "Callback: Permission granted, UI should update")
        },
        onPermissionDenied = {
        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                storageGranted = permissionState.hasPermissions()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    installGranted = context.packageManager.canRequestPackageInstalls()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "contentTransition"
                ) { step ->
                    when (step) {
                        WelcomeStep.INTRO -> IntroContent()

                        WelcomeStep.PERMISSIONS -> PermissionsContent(
                            storageGranted = storageGranted,
                            installGranted = installGranted,
                            onRequestStoragePermission = {
                                permissionState.requestPermissions()
                            },
                            onRequestInstallPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    if (!context.packageManager.canRequestPackageInstalls()) {
                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                        intent.setData(Uri.parse("package:" + context.packageName))
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )

                        WelcomeStep.THEME_SETUP -> ThemeSetupContent(
                            selectedModeIndex = selectedModeIndex,
                            selectedThemeIndex = selectedThemeIndex,
                            isMonetEnabled = isMonetEnabled,
                            onMonetToggle = { enabled ->
                                isMonetEnabled = enabled
                                themeViewModel.saveThemeConfig(selectedModeIndex, selectedThemeIndex, customColor, enabled, selectedThemeIndex == themeColors.size)
                            },
                            onModeSelected = { index ->
                                selectedModeIndex = index
                                themeViewModel.saveThemeConfig(index, selectedThemeIndex, customColor, isMonetEnabled, selectedThemeIndex == themeColors.size)
                            },
                            onThemeSelected = { index ->
                                selectedThemeIndex = index
                                themeViewModel.saveThemeConfig(selectedModeIndex, index, customColor, false, index == themeColors.size)
                            },
                            onCustomColorClick = {
                                selectedThemeIndex = themeColors.size
                                showColorPicker = true
                            }
                        )
                    }
                }
            }

            BottomNavigation(
                currentStep = currentStep,
                onBack = {
                    currentStep = when (currentStep) {
                        WelcomeStep.PERMISSIONS -> WelcomeStep.INTRO
                        WelcomeStep.THEME_SETUP -> WelcomeStep.PERMISSIONS
                        else -> currentStep
                    }
                },
                onNext = {
                    when (currentStep) {
                        WelcomeStep.INTRO -> currentStep = WelcomeStep.PERMISSIONS
                        WelcomeStep.PERMISSIONS -> currentStep = WelcomeStep.THEME_SETUP
                        WelcomeStep.THEME_SETUP -> {
                            themeViewModel.saveThemeConfig(selectedModeIndex, selectedThemeIndex, customColor, isMonetEnabled, selectedThemeIndex == themeColors.size)
                            onWelcomeFinished()
                        }
                    }
                }
            )
        }

        if (showColorPicker) {
            ColorPickerDialog(
                initialColor = customColor, 
                onDismiss = { showColorPicker = false },
                onColorSelected = { color ->
                    customColor = color
                    showColorPicker = false
                    selectedThemeIndex = themeColors.size
                    themeViewModel.saveThemeConfig(
                        selectedModeIndex,
                        themeColors.size,
                        color,
                        false, 
                        true   
                    )
                }
            )
        }
    }
}

@Composable
private fun IntroContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.app_name), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.welcome_subtitle), fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))
        FeatureItem(Icons.Default.Speed, stringResource(R.string.welcome_feat1_title), stringResource(R.string.welcome_feat1_desc))
        Spacer(Modifier.height(20.dp))
        FeatureItem(Icons.Default.Palette, stringResource(R.string.welcome_feat2_title), stringResource(R.string.welcome_feat2_desc))
        Spacer(Modifier.height(20.dp))
        FeatureItem(Icons.Default.Edit, stringResource(R.string.welcome_feat3_title), stringResource(R.string.welcome_feat3_desc))
    }
}

@Composable
private fun PermissionsContent(
    storageGranted: Boolean,
    installGranted: Boolean,
    onRequestStoragePermission: () -> Unit,
    onRequestInstallPermission: () -> Unit
) {
    val storageTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) stringResource(R.string.welcome_perm_storage_title_11) else stringResource(R.string.welcome_perm_storage_title)
    val storageDesc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) stringResource(R.string.welcome_perm_storage_desc_11) else stringResource(R.string.welcome_perm_storage_desc)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()), 
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.welcome_perms_title), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.welcome_perms_desc), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(48.dp))

        PermissionCard(
            Icons.Default.Folder,
            storageTitle,
            storageDesc,
            storageGranted,
            onRequestStoragePermission
        )

        Spacer(Modifier.height(16.dp))

        PermissionCard(
            Icons.Default.Download, 
            stringResource(R.string.welcome_perm_install_title),
            stringResource(R.string.welcome_perm_install_desc),
            installGranted,
            onRequestInstallPermission
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSetupContent(
    selectedModeIndex: Int,
    selectedThemeIndex: Int,
    isMonetEnabled: Boolean,
    onMonetToggle: (Boolean) -> Unit,
    onModeSelected: (Int) -> Unit,
    onThemeSelected: (Int) -> Unit,
    onCustomColorClick: () -> Unit
) {
    val modeOptions = listOf(stringResource(R.string.settings_mode_system), stringResource(R.string.settings_mode_light), stringResource(R.string.settings_mode_dark))
    val colorOptions = themeColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = stringResource(R.string.theme_select_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_theme_subtitle),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    modeOptions.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedModeIndex == index,
                            onClick = { onModeSelected(index) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modeOptions.size),
                            icon = {}
                        ) {
                            Text(label)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_dynamic_color), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.settings_extract_colors),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isMonetEnabled,
                            onCheckedChange = onMonetToggle
                        )
                    }
                }

                AnimatedVisibility(!isMonetEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(Modifier.width(16.dp))

                        colorOptions.forEachIndexed { index, theme ->
                            ThemePreviewCard(
                                theme = theme,
                                isSelected = selectedThemeIndex == index,
                                onClick = { onThemeSelected(index) }
                            )
                        }

                        CustomThemeCard(
                            isSelected = selectedThemeIndex == themeColors.size,
                            onClick = onCustomColorClick
                        )

                        Spacer(Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}