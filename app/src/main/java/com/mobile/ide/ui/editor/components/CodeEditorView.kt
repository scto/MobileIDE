// Copyright 2025 Thomas Schmid
package com.mobile.ide.ui.editor.components

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobile.ide.R
import com.mobile.ide.ui.ThemeViewModel
import com.mobile.ide.ui.ThemeViewModelFactory
import com.mobile.ide.ui.editor.viewmodel.CodeEditorState
import com.mobile.ide.ui.editor.viewmodel.EditorViewModel
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.launch
import org.eclipse.tm4e.core.registry.IThemeSource

@Composable
fun CodeEditorView(modifier: Modifier = Modifier, state: CodeEditorState, viewModel: EditorViewModel) {
    val context = LocalContext.current
    var isEditorReady by remember { mutableStateOf(false) }

    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(context))
    val themeState by themeViewModel.themeState.collectAsState()

    val systemDark = isSystemInDarkTheme()
    val isDark =
        when (themeState.selectedModeIndex) {
            0 -> systemDark
            1 -> false
            2 -> true
            else -> systemDark
        }

    val seedColor =
        if (themeState.isCustomTheme) {
            themeState.customColor
        } else {
            MaterialTheme.colorScheme.primary
        }

    LaunchedEffect(seedColor, isDark, isEditorReady) {
        if (isEditorReady) {
            viewModel.updateEditorTheme(seedColor, isDark)
        }
    }

    val editor = remember(state.file.absolutePath) { viewModel.getOrCreateEditor(context, state) }

    LaunchedEffect(state.file.absolutePath) {
        if (!TextMateInitializer.isReady()) {
            TextMateInitializer.initialize(context) {
                isEditorReady = true
                viewModel.updateEditorTheme(seedColor, isDark)
            }
        } else {
            isEditorReady = true
            viewModel.updateEditorTheme(seedColor, isDark)
        }
    }

    DisposableEffect(state.file.absolutePath) { onDispose {} }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isEditorReady) {
            AndroidView(
                factory = { factoryContext ->
                    (editor.parent as? ViewGroup)?.removeView(editor)
                    editor
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    if (view.text.toString() != state.content) {
                        val cursor = view.cursor
                        val cursorLine = cursor.leftLine
                        val cursorColumn = cursor.leftColumn
                        view.setText(state.content)
                        try {
                            val lineCount = view.text.lineCount
                            val targetLine = cursorLine.coerceIn(0, lineCount - 1)
                            val lineLength = view.text.getColumnCount(targetLine)
                            val targetColumn = cursorColumn.coerceIn(0, lineLength)
                            view.setSelection(targetLine, targetColumn)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    view.isEnabled = true
                    view.visibility = android.view.View.VISIBLE
                    view.requestLayout()
                },
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.msg_initializing_editor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

object TextMateInitializer {
    private var isInitialized = false
    private var isInitializing = false
    private val callbacks = mutableListOf<() -> Unit>()

    @Synchronized
    fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
        if (isInitialized) {
            onComplete?.invoke()
            return
        }
        if (isInitializing) {
            onComplete?.let { callbacks.add(it) }
            return
        }
        isInitializing = true
        onComplete?.let { callbacks.add(it) }

        kotlinx.coroutines.GlobalScope.launch {
            try {
                val appContext = context.applicationContext
                val assetsFileResolver = AssetsFileResolver(appContext.assets)
                FileProviderRegistry.getInstance().addFileProvider(assetsFileResolver)

                val themeRegistry = ThemeRegistry.getInstance()
                val themeName = "quietlight"
                val themePath = "textmate/$themeName.json"

                FileProviderRegistry.getInstance().tryGetInputStream(themePath)?.use { inputStream ->
                    themeRegistry.loadTheme(
                        ThemeModel(IThemeSource.fromInputStream(inputStream, themePath, null), themeName)
                    )
                    themeRegistry.setTheme(themeName)
                }

                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

                synchronized(this) {
                    isInitialized = true
                    isInitializing = false
                    callbacks.forEach { it.invoke() }
                    callbacks.clear()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                synchronized(this) {
                    isInitializing = false
                    callbacks.clear()
                }
            }
        }
    }

    fun isReady() = isInitialized

    fun preloadCommonLanguages(context: Context) {
        if (!isInitialized && !isInitializing) {
            initialize(context)
        }
    }
}
