package com.scto.mobile.ide.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import androidx.navigation.NavController
import com.scto.mobile.ide.R
import com.scto.mobile.ide.ui.editor.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(navController: NavController, editorViewModel: EditorViewModel? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MobileIDE_Editor_Settings", Context.MODE_PRIVATE) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_editor_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
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
    }
}
