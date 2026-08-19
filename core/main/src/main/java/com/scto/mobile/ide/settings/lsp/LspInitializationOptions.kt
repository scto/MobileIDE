package com.scto.mobile.ide.settings.lsp





import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.scto.mobile.ide.components.ResetButton
import com.scto.mobile.ide.editor.Editor
import com.scto.mobile.ide.file.BuiltinFileType
import com.scto.mobile.ide.lsp.LspServer
import com.scto.mobile.ide.settings.Preference
import com.scto.mobile.ide.tabs.editor.EditorErrorNotice
import com.scto.mobile.ide.theme.GitColorScheme
import com.scto.mobile.ide.utils.isSystemInDarkTheme
import io.github.rosemoe.sora.event.ContentChangeEvent
import java.lang.ref.WeakReference
import kotlinx.coroutines.launch











@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LspInitializationOptions(server: LspServer) {
    val scope = rememberCoroutineScope()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var editorRef = remember { WeakReference<Editor?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            editorRef.get()?.release()
            editorRef = WeakReference(null)
        }
    }

    val preferenceKey = "lsp_${server.id}_initialization_options"
    val defaultInitOptions = server.getInitializationOptions(null)
    val defaultInitJson =
        defaultInitOptions?.let {
            runCatching { Gson().toJson(it) }.getOrNull()
        } ?: "{}"

    var isJsonInvalid by remember { mutableStateOf(false) }

    fun Editor.validateJson(text: String) {
        runCatching { JsonParser.parseString(text) }
            .also {
                isJsonInvalid = it.isFailure
            }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { Text(stringResource(com.scto.mobile.ide.core.main.R.string.initialization_options)) },
                    actions = {
                        ResetButton {
                            Preference.removeKey(preferenceKey)
                            editorRef.get()?.setText(defaultInitJson)
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    ) { paddingValues ->
        val selectionColors = LocalTextSelectionColors.current
        val isDarkMode = isSystemInDarkTheme(context)
        val colorScheme = MaterialTheme.colorScheme
        val gitColorScheme = GitColorScheme.create()

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AnimatedVisibility(visible = isJsonInvalid) {
                EditorErrorNotice(text = stringResource(com.scto.mobile.ide.core.main.R.string.invalid_initialization_options))
            }

            AndroidView(
                modifier = Modifier.fillMaxSize().imePadding(),
                factory = { context ->
                    Editor(context).apply {
                        editorRef = WeakReference(this)

                        setTextSize(14f)
                        setText(Preference.getString(preferenceKey, defaultInitJson))
                        isWordwrap = false

                        validateJson(text.toString())

                        subscribeAlways(ContentChangeEvent::class.java) { event ->
                            val text = event.editor.text.toString()
                            Preference.setString(preferenceKey, text)

                            validateJson(text)
                        }

                        setThemeColors(
                            isDarkMode = isDarkMode,
                            selectionColors = selectionColors,
                            colorScheme = colorScheme,
                            gitColorScheme = gitColorScheme,
                        )

                        scope.launch { configureLanguage(BuiltinFileType.JSON.textmateScope!!) }
                    }
                },
            )
        }
    }
}
