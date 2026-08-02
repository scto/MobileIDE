package com.scto.mobile.ide.commands

import com.scto.mobile.ide.commands.editor.*
import com.scto.mobile.ide.commands.global.*
import com.scto.mobile.ide.commands.lsp.*

object CoreCommandsInitializer {
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true

        // Editor commands
        MobileIDECommandManager.registerCommand(CopyCommand())
        MobileIDECommandManager.registerCommand(CutCommand())
        MobileIDECommandManager.registerCommand(DuplicateLineCommand())
        MobileIDECommandManager.registerCommand(EmulateKeyCommand())
        MobileIDECommandManager.registerCommand(JumpToLineCommand())
        MobileIDECommandManager.registerCommand(LowerCaseCommand())
        MobileIDECommandManager.registerCommand(PasteCommand())
        MobileIDECommandManager.registerCommand(RedoCommand())
        MobileIDECommandManager.registerCommand(RefreshCommand())
        MobileIDECommandManager.registerCommand(ReplaceCommand())
        MobileIDECommandManager.registerCommand(SaveCommand())
        MobileIDECommandManager.registerCommand(SaveAsCommand())
        MobileIDECommandManager.registerCommand(SearchCommand())
        MobileIDECommandManager.registerCommand(SelectAllCommand())
        MobileIDECommandManager.registerCommand(SelectWordCommand())
        MobileIDECommandManager.registerCommand(ShareCommand())
        MobileIDECommandManager.registerCommand(SortLinesAscendingCommand())
        MobileIDECommandManager.registerCommand(SortLinesDescendingCommand())
        MobileIDECommandManager.registerCommand(SyntaxHighlightingCommand())
        MobileIDECommandManager.registerCommand(ToggleReadOnlyCommand())
        MobileIDECommandManager.registerCommand(ToggleWordWrapCommand())
        MobileIDECommandManager.registerCommand(UndoCommand())
        MobileIDECommandManager.registerCommand(UpperCaseCommand())

        // Global commands
        MobileIDECommandManager.registerCommand(CommandPaletteCommand())
        MobileIDECommandManager.registerCommand(DocumentationCommand())
        MobileIDECommandManager.registerCommand(NewFileCommand())
        MobileIDECommandManager.registerCommand(SaveAllCommand())
        MobileIDECommandManager.registerCommand(SearchCodeCommand())
        MobileIDECommandManager.registerCommand(SearchFileFolderCommand())
        MobileIDECommandManager.registerCommand(SettingsCommand())

        // LSP commands
        MobileIDECommandManager.registerCommand(FormatDocumentCommand())
        MobileIDECommandManager.registerCommand(FormatSelectionCommand())
        MobileIDECommandManager.registerCommand(GoToDefinitionCommand())
        MobileIDECommandManager.registerCommand(GoToReferencesCommand())
        MobileIDECommandManager.registerCommand(RenameSymbolCommand())
    }
}
