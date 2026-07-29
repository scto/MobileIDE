package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import com.scto.mobile.ide.commands.Command
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.file.FileTypeManager

class SyntaxHighlightingCommand : EditorCommand() {
    override val id: String = "editor.syntax_highlighting"

    override val title: String = "highlighting"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {}

    override val icon: Any? = null // Icon.ResourceIcon(drawables.edit_note)

    override val childCommands: List<Command> by lazy {
        FileTypeManager.allTypes()
            .filter { it.textmateScope != null }
            .map { fileType ->
                object : EditorCommand() {
                    override val id: String = "editor.syntax_highlighting.${fileType.name.lowercase()}"

                    override val title: String = fileType.title

                    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
                        tab.editorState.textmateScope = fileType.textmateScope!!
                    }

                    override val icon: Any? = null // fileType.getResolvedIcon()
                }
            }
    }

    override fun getChildSearchPlaceholder(): String = strings.select_language.getString()
}
