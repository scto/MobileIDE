package com.scto.mobile.ide.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.scto.mobile.ide.R
import com.scto.mobile.ide.utils.ThemeState
import com.scto.mobile.ide.ui.components.ColorPickerDialog
import com.scto.mobile.ide.ui.welcome.themeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    navController: NavController,
    currentThemeState: ThemeState,
    onThemeChange: (modeIndex: Int, themeIndex: Int, customColor: Color, isMonet: Boolean, isCustom: Boolean) -> Unit,
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_theme_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ThemeSettingsItem(
                currentThemeState = currentThemeState,
                onThemeChange = onThemeChange,
                onCustomColorClick = { showColorPicker = true },
            )
        }
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
