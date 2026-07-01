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
    var aiEnabled by remember { mutableStateOf(prefs.getBoolean("editor_ai_enabled", true)) }
    var fontPath by remember { mutableStateOf(prefs.getString("editor_font_path", "") ?: "") }
    var editorType by remember { mutableStateOf(prefs.getString("editor_type", "treesitter") ?: "treesitter") }
    var customSymbols by remember {
        mutableStateOf(
            prefs.getString("editor_custom_symbols", "Tab,<,>,/,=,\",',!,?,;,:,{,},[,],(,),+,-,*,_,&,|") ?: ""
        )
    }

    var pinLineNumber by remember { mutableStateOf(prefs.getBoolean("editor_pin_line_number", false)) }
    var cursorAnimation by remember { mutableStateOf(prefs.getBoolean("editor_cursor_animation", true)) }
    var smoothScroll by remember { mutableStateOf(prefs.getBoolean("editor_smooth_scroll", true)) }
    var cursorBlink by remember { mutableIntStateOf(prefs.getInt("editor_cursor_blink", 500)) }
    var highlightCurrentLine by remember { mutableStateOf(prefs.getBoolean("editor_highlight_current_line", true)) }
    var highlightCurrentBlock by remember { mutableStateOf(prefs.getBoolean("editor_highlight_current_block", true)) }
    var autoCloseBrackets by remember { mutableStateOf(prefs.getBoolean("editor_auto_close_brackets", true)) }

    var previousEditorType by remember { mutableStateOf(editorType) }

    LaunchedEffect(
        fontSize,
        tabWidth,
        wordWrap,
        showInvisibles,
        codeFolding,
        showToolbar,
        showHistory,
        aiEnabled,
        fontPath,
        customSymbols,
        editorType,
        pinLineNumber,
        cursorAnimation,
        smoothScroll,
        cursorBlink,
        highlightCurrentLine,
        highlightCurrentBlock,
        autoCloseBrackets,
    ) {
        prefs.edit {
            putFloat("editor_font_size", fontSize)
            putInt("editor_tab_width", tabWidth)
            putBoolean("editor_word_wrap", wordWrap)
            putBoolean("editor_show_invisibles", showInvisibles)
            putBoolean("editor_code_folding", codeFolding)
            putBoolean("editor_show_toolbar", showToolbar)
            putBoolean("editor_show_history", showHistory)
            putBoolean("editor_ai_enabled", aiEnabled)
            putString("editor_font_path", fontPath)
            putString("editor_custom_symbols", customSymbols)
            putString("editor_type", editorType)
            putBoolean("editor_pin_line_number", pinLineNumber)
            putBoolean("editor_cursor_animation", cursorAnimation)
            putBoolean("editor_smooth_scroll", smoothScroll)
            putInt("editor_cursor_blink", cursorBlink)
            putBoolean("editor_highlight_current_line", highlightCurrentLine)
            putBoolean("editor_highlight_current_block", highlightCurrentBlock)
            putBoolean("editor_auto_close_brackets", autoCloseBrackets)
        }
        if (editorType != previousEditorType) {
            editorViewModel?.reloadAllEditors(context)
            previousEditorType = editorType
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
                isAiEnabled = aiEnabled,
                onIsAiEnabledChange = { aiEnabled = it },
                fontPath = fontPath,
                onFontPathChange = { fontPath = it },
                customSymbols = customSymbols,
                onCustomSymbolsChange = { customSymbols = it },
                editorType = editorType,
                onEditorTypeChange = { editorType = it },
                pinLineNumber = pinLineNumber,
                onPinLineNumberChange = { pinLineNumber = it },
                cursorAnimation = cursorAnimation,
                onCursorAnimationChange = { cursorAnimation = it },
                smoothScroll = smoothScroll,
                onSmoothScrollChange = { smoothScroll = it },
                cursorBlink = cursorBlink,
                onCursorBlinkChange = { cursorBlink = it },
                highlightCurrentLine = highlightCurrentLine,
                onHighlightCurrentLineChange = { highlightCurrentLine = it },
                highlightCurrentBlock = highlightCurrentBlock,
                onHighlightCurrentBlockChange = { highlightCurrentBlock = it },
                autoCloseBrackets = autoCloseBrackets,
                onAutoCloseBracketsChange = { autoCloseBrackets = it },
            )
        }
    }
}
