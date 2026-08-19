package com.scto.mobile.ide.lsp





import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.activities.main.MainViewModel
import com.scto.mobile.ide.activities.main.session.EditorManager
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.file.child
import com.scto.mobile.ide.file.sandboxDir
import com.scto.mobile.ide.file.sandboxHomeDir
import com.scto.mobile.ide.file.toFileObject
import com.scto.mobile.ide.file.toFileWrapper
import com.scto.mobile.ide.search.CodeItem
import com.scto.mobile.ide.search.utils.SnippetBuilder
import com.scto.mobile.ide.tabs.editor.EditorTab
import com.scto.mobile.ide.utils.toast
import io.github.rosemoe.sora.lsp.editor.LspEventManager
import io.github.rosemoe.sora.lsp.editor.getOption
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.document.applyEdits
import io.github.rosemoe.sora.lsp.events.format.fullFormatting
import io.github.rosemoe.sora.lsp.events.format.rangeFormatting
import io.github.rosemoe.sora.widget.component.TextActionItem
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.Range











/**
 * Workaround helper that fixes the URI path coming from LSP if they point to `/home`.
 *
 * Example: The LSP may return `file:///home/...` but Xed-Editor has to resolve the path to
 * `file:///data/user/0/com.scto.mobile.ide/local/sandbox/home/...`
 */
fun fixHomeLocation(context: Context, uri: String): String {
    val path = uri.toUri().path ?: return uri

    val fixedPath =
        when {
            path.startsWith("/home") -> {
                File(sandboxHomeDir(context), uri.toUri().path!!.removePrefix("/home/"))
            }
            path.startsWith("/usr") -> {
                File(sandboxDir(context).child("usr"), uri.toUri().path!!.removePrefix("/usr/"))
            }
            else -> null
        }

    return fixedPath?.let { Uri.fromFile(it).toString() } ?: uri
}

suspend fun EditorManager.jumpToPosition(file: FileObject, projectRoot: FileObject?, range: Range) {
    jumpToPosition(file, projectRoot, range.start.line, range.start.character, range.end.line, range.end.character)
}

fun goToDefinition(scope: CoroutineScope, context: Context, viewModel: MainViewModel, editorTab: EditorTab) {
    scope.launch(Dispatchers.Default) {
        runCatching {
            val baseLspConnector = editorTab.lspConnector!!
            val editorState = editorTab.editorState
            val editor = editorState.editor.get()!!

            val eitherDefinitions = baseLspConnector.requestDefinition(editor)
            val definitions = if (eitherDefinitions.isLeft) eitherDefinitions.left else eitherDefinitions.right

            if (definitions.isEmpty()) {
                toast(com.scto.mobile.ide.core.main.R.string.no_definitions_found)
                return@launch
            }

            // If only one definition exists, immediately view definition
            if (definitions.size == 1) {
                val range =
                    if (eitherDefinitions.isLeft) eitherDefinitions.left[0].range
                    else eitherDefinitions.right[0].targetSelectionRange
                var uriString =
                    if (eitherDefinitions.isLeft) eitherDefinitions.left[0].uri
                    else eitherDefinitions.right[0].targetUri
                uriString = fixHomeLocation(context, uriString)

                val uri = uriString.toUri()
                val targetFile = if (uri.scheme == null) File(uriString).toFileWrapper() else uri.toFileObject(true)

                scope.launch { viewModel.editorManager.jumpToPosition(targetFile, editorTab.projectRoot, range) }
                return@launch
            }

            // If multiple definitions exist, ask user which one to view
            withContext(Dispatchers.Main) {
                val snippetBuilder = SnippetBuilder(context)
                editorState.findingsItems =
                    List(definitions.size) { index ->
                        val range =
                            if (eitherDefinitions.isLeft) eitherDefinitions.left[index].range
                            else eitherDefinitions.right[index].targetSelectionRange
                        var uriString =
                            if (eitherDefinitions.isLeft) eitherDefinitions.left[index].uri
                            else eitherDefinitions.right[index].targetUri
                        uriString = fixHomeLocation(context, uriString)

                        val uri = uriString.toUri()
                        val targetFile =
                            if (uri.scheme == null) File(uriString).toFileWrapper() else uri.toFileObject(true)

                        val snippetResult = snippetBuilder.generateLspSnippet(viewModel, targetFile, range)
                        CodeItem(
                            snippet = snippetResult,
                            file = targetFile,
                            line = range.start.line,
                            column = range.start.character,
                            onClick = {
                                scope.launch {
                                    viewModel.editorManager.jumpToPosition(targetFile, editorTab.projectRoot, range)
                                }
                            },
                        )
                    }
            }
            editorState.findingsTitle = com.scto.mobile.ide.core.main.R.string.go_to_definition.getString()
            editorState.findingsDescription = com.scto.mobile.ide.core.main.R.string.go_to_definition_desc.getString()
            editorState.showFindingsDialog = true
        }
            .onFailure {
                it.printStackTrace()
                toast(com.scto.mobile.ide.core.main.R.string.find_definitions_error)
            }
    }
}

fun goToReferences(scope: CoroutineScope, context: Context, viewModel: MainViewModel, editorTab: EditorTab) {
    scope.launch(Dispatchers.Default) {
        runCatching {
            val baseLspConnector = editorTab.lspConnector!!
            val editorState = editorTab.editorState
            val editor = editorState.editor.get()!!

            val references = baseLspConnector.requestReferences(editor)

            if (references.isEmpty()) {
                toast(com.scto.mobile.ide.core.main.R.string.no_references_found)
                return@launch
            }

            // If only one reference exists, immediately view reference
            if (references.size == 1) {
                val range = references[0]!!.range
                var uriString = references[0]!!.uri
                uriString = fixHomeLocation(context, uriString)

                val uri = uriString.toUri()
                val targetFile = if (uri.scheme == null) File(uriString).toFileWrapper() else uri.toFileObject(true)

                scope.launch { viewModel.editorManager.jumpToPosition(targetFile, editorTab.projectRoot, range) }
                return@launch
            }

            // If multiple references exist, ask user which one to view
            withContext(Dispatchers.Main) {
                val snippetBuilder = SnippetBuilder(context)
                editorState.findingsItems = references.mapNotNull { reference ->
                    val range = reference?.range ?: return@mapNotNull null
                    var uriString = reference.uri
                    uriString = fixHomeLocation(context, uriString)

                    val uri = uriString.toUri()
                    val targetFile = if (uri.scheme == null) File(uriString).toFileWrapper() else uri.toFileObject(true)

                    val snippetResult = snippetBuilder.generateLspSnippet(viewModel, targetFile, range)
                    CodeItem(
                        snippet = snippetResult,
                        file = targetFile,
                        line = range.start.line,
                        column = range.start.character,
                        onClick = {
                            scope.launch {
                                viewModel.editorManager.jumpToPosition(targetFile, editorTab.projectRoot, range)
                            }
                        },
                    )
                }
            }
            editorState.findingsTitle = com.scto.mobile.ide.core.main.R.string.go_to_references.getString()
            editorState.findingsDescription = com.scto.mobile.ide.core.main.R.string.go_to_references_desc.getString()
            editorState.showFindingsDialog = true
        }
            .onFailure {
                it.printStackTrace()
                toast(com.scto.mobile.ide.core.main.R.string.find_references_error)
            }
    }
}

fun renameSymbol(scope: CoroutineScope, editorTab: EditorTab) {
    scope.launch(Dispatchers.Default) {
        runCatching {
            var currentName = ""

            val file = editorTab.file!!
            val baseLspConnector = editorTab.lspConnector!!
            val editorState = editorTab.editorState
            val editor = editorState.editor.get()!!

            if (baseLspConnector.isPrepareRenameSymbolSupported()) {
                val prepareRename = baseLspConnector.requestPrepareRenameSymbol(editor)

                if (prepareRename == null) {
                    toast(com.scto.mobile.ide.core.main.R.string.cannot_rename_symbol)
                    return@launch
                }

                if (prepareRename.isFirst && prepareRename.first!!.start.line == prepareRename.first!!.end.line) {
                    currentName =
                        editor.text
                            .getLineString(prepareRename.first!!.start.line)
                            .substring(prepareRename.first!!.start.character, prepareRename.first!!.end.character)
                }

                if (prepareRename.isSecond) {
                    currentName = prepareRename.second!!.placeholder
                }

                if (prepareRename.isThird && editor.cursor.range.start.line == editor.cursor.range.end.line) {
                    currentName =
                        editor.text
                            .getLineString(editor.cursor.range.start.line)
                            .substring(editor.cursor.range.start.column, editor.cursor.range.end.column)
                }
            }

            editorState.renameValue = currentName
            editorState.showRenameDialog = true
            editorState.renameConfirm = { newName ->
                scope.launch(Dispatchers.Default) {
                    runCatching {
                        val workspaceEdit = baseLspConnector.requestRenameSymbol(editor, newName)

                        // TODO: Handle documentChanges too
                        val changes = workspaceEdit.changes

                        // Edits only supported in currently opened file
                        // TODO: Support edits in other files
                        if (changes.size > 1) {
                            toast(com.scto.mobile.ide.core.main.R.string.rename_symbol_multiple_files)
                            return@launch
                        }

                        val edits = changes[file.toUri().toString()]!!
                        baseLspConnector.getEventManager()!!.emitAsync(EventType.applyEdits) {
                            put("edits", edits)
                            put(editor.text)
                        }
                    }
                        .onFailure {
                            it.printStackTrace()
                            toast(com.scto.mobile.ide.core.main.R.string.rename_symbol_error)
                        }
                }
            }
        }
            .onFailure {
                it.printStackTrace()
                toast(com.scto.mobile.ide.core.main.R.string.rename_symbol_error)
            }
    }
}

fun applyFormattingOptions(eventManager: LspEventManager, editorTab: EditorTab) {
    val editor = editorTab.editorState.editor.get() ?: return
    val formattingOptions = eventManager.getOption<FormattingOptions>()!!
    formattingOptions.tabSize = editor.tabWidth
    formattingOptions.isInsertSpaces = !editor.editorLanguage.useTab()
    formattingOptions.isInsertFinalNewline = editor.insertFinalNewline
    formattingOptions.isTrimTrailingWhitespace = editor.trimTrailingWhitespace
}

suspend fun formatDocumentSuspend(editorTab: EditorTab) {
    runCatching {
        val baseLspConnector = editorTab.lspConnector!!
        val editorState = editorTab.editorState
        val editor = editorState.editor.get()!!
        val eventManager = baseLspConnector.getEventManager()!!

        applyFormattingOptions(eventManager, editorTab)

        eventManager.emitAsync(EventType.fullFormatting, editor.text)
    }
        .onFailure {
            it.printStackTrace()
            toast(com.scto.mobile.ide.core.main.R.string.format_document_error)
        }
}

fun formatDocumentRange(scope: CoroutineScope, editorTab: EditorTab) {
    scope.launch(Dispatchers.Default) {
        runCatching {
            val baseLspConnector = editorTab.lspConnector!!
            val editorState = editorTab.editorState
            val editor = editorState.editor.get()!!
            val eventManager = baseLspConnector.getEventManager()!!

            applyFormattingOptions(eventManager, editorTab)

            eventManager.emitAsync(EventType.rangeFormatting) {
                put("text", editor.text)
                put("range", editor.cursor.range)
            }
        }
            .onFailure {
                it.printStackTrace()
                toast(com.scto.mobile.ide.core.main.R.string.format_selection_error)
            }
    }
}

/** Returns a list of registerable LSP text actions. */
fun createLspTextActions(
    scope: CoroutineScope,
    context: Context,
    viewModel: MainViewModel,
    editorTab: EditorTab,
): List<TextActionItem> {
    fun isUrlSelected(): Boolean {
        return editorTab.editorState.editor.get()?.isUrlSelected() == true
    }

    val goToDefinition =
        TextActionItem(
            titleRes = com.scto.mobile.ide.core.main.R.string.go_to_definition,
            iconRes = com.scto.mobile.ide.core.main.R.drawable.jump_to_element,
            shouldShow = { _ -> !isUrlSelected() && editorTab.lspConnector?.isGoToDefinitionSupported() == true },
        ) { _ ->
            goToDefinition(scope, context, viewModel, editorTab)
        }

    val goToReferences =
        TextActionItem(
            titleRes = com.scto.mobile.ide.core.main.R.string.go_to_references,
            iconRes = com.scto.mobile.ide.core.main.R.drawable.manage_search,
            shouldShow = { _ -> !isUrlSelected() && editorTab.lspConnector?.isGoToReferencesSupported() == true },
        ) { _ ->
            goToReferences(scope, context, viewModel, editorTab)
        }

    val renameSymbol =
        TextActionItem(
            titleRes = com.scto.mobile.ide.core.main.R.string.rename_symbol,
            iconRes = com.scto.mobile.ide.core.main.R.drawable.edit_note,
            shouldShow = { editor ->
                !isUrlSelected() && editor.isEditable && editorTab.lspConnector?.isRenameSymbolSupported() == true
            },
        ) { _ ->
            renameSymbol(scope, editorTab)
        }

    return listOf(goToDefinition, goToReferences, renameSymbol)
}
