package com.scto.mobile.ide.commands

import android.content.Context
import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import com.scto.mobile.ide.ui.editor.viewmodel.EditorViewModel
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File
import io.github.rosemoe.sora.lsp.editor.LspEditor

interface CommandContext

data class MobileIDECommandContext(val editorViewModel: EditorViewModel, val androidContext: Context) : CommandContext

interface Command {
    val id: String
    val title: String
    val description: String
    val icon: Any?

    suspend fun execute(context: CommandContext)
}

abstract class BaseCommand : Command {
    override val icon: Any? = null
    override val description: String = ""
    override val title: String = ""
}

abstract class EditorCommand : BaseCommand() {
    final override suspend fun execute(context: CommandContext) {
        val ideContext = context as? MobileIDECommandContext ?: return
        val vm = ideContext.editorViewModel
        if (vm.activeFileIndex in vm.openFiles.indices) {
            val currentTab = vm.openFiles[vm.activeFileIndex] as? CodeEditorState ?: return
            val editor = vm.editorInstances[currentTab.file.absolutePath] ?: return
            executeEditorCommand(ideContext, currentTab, editor)
        }
    }

    abstract suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor)
}

abstract class EditorFileCommand : EditorCommand() {
    final override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val file = tab.file
        executeFileCommand(context, tab, editor, file)
    }

    abstract suspend fun executeFileCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor, file: File)
}

abstract class LspCommand : EditorCommand() {
    final override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val lspEditor = tab.lspEditor ?: return
        executeLspCommand(context, tab, editor, lspEditor)
    }

    abstract suspend fun executeLspCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor, lspEditor: LspEditor)
}

abstract class GlobalCommand : BaseCommand()
