package com.scto.mobile.ide.commands





import androidx.compose.runtime.mutableStateListOf
import com.scto.mobile.ide.commands.editor.CopyCommand
import com.scto.mobile.ide.commands.editor.CutCommand
import com.scto.mobile.ide.commands.editor.DuplicateLineCommand
import com.scto.mobile.ide.commands.editor.EmulateKeyCommand
import com.scto.mobile.ide.commands.editor.JumpToLineCommand
import com.scto.mobile.ide.commands.editor.LowerCaseCommand
import com.scto.mobile.ide.commands.editor.PasteCommand
import com.scto.mobile.ide.commands.editor.RedoCommand
import com.scto.mobile.ide.commands.editor.RefreshCommand
import com.scto.mobile.ide.commands.editor.ReplaceCommand
import com.scto.mobile.ide.commands.editor.SaveAsCommand
import com.scto.mobile.ide.commands.editor.SaveCommand
import com.scto.mobile.ide.commands.editor.SearchCommand
import com.scto.mobile.ide.commands.editor.SelectAllCommand
import com.scto.mobile.ide.commands.editor.SelectWordCommand
import com.scto.mobile.ide.commands.editor.ShareCommand
import com.scto.mobile.ide.commands.editor.SortLinesAscendingCommand
import com.scto.mobile.ide.commands.editor.SortLinesDescendingCommand
import com.scto.mobile.ide.commands.editor.SyntaxHighlightingCommand
import com.scto.mobile.ide.commands.editor.ToggleReadOnlyCommand
import com.scto.mobile.ide.commands.editor.ToggleWordWrapCommand
import com.scto.mobile.ide.commands.editor.UndoCommand
import com.scto.mobile.ide.commands.editor.UpperCaseCommand
import com.scto.mobile.ide.commands.global.CommandPaletteCommand
import com.scto.mobile.ide.commands.global.DocumentationCommand
import com.scto.mobile.ide.commands.global.NewFileCommand
import com.scto.mobile.ide.commands.global.SaveAllCommand
import com.scto.mobile.ide.commands.global.SearchCodeCommand
import com.scto.mobile.ide.commands.global.SearchFileFolderCommand
import com.scto.mobile.ide.commands.global.SettingsCommand
import com.scto.mobile.ide.commands.lsp.FormatDocumentCommand
import com.scto.mobile.ide.commands.lsp.FormatDocumentLspCommand
import com.scto.mobile.ide.commands.lsp.FormatSelectionCommand
import com.scto.mobile.ide.commands.lsp.GoToDefinitionCommand
import com.scto.mobile.ide.commands.lsp.GoToReferencesCommand
import com.scto.mobile.ide.commands.lsp.RenameSymbolCommand
import com.scto.mobile.ide.extension.api.DisposableManager
import com.scto.mobile.ide.extension.api.Disposer
import com.scto.mobile.ide.extension.api.XedExtensionPoint











object CommandProvider {
    private val _commandList = mutableStateListOf<Command>()
    val commandList: List<Command>
        get() = _commandList

    lateinit var DocumentationCommand: DocumentationCommand
    lateinit var SettingsCommand: SettingsCommand
    lateinit var NewFileCommand: NewFileCommand
    lateinit var CommandPaletteCommand: CommandPaletteCommand
    lateinit var SearchFileFolderCommand: SearchFileFolderCommand
    lateinit var SearchCodeCommand: SearchCodeCommand
    lateinit var CutCommand: CutCommand
    lateinit var CopyCommand: CopyCommand
    lateinit var PasteCommand: PasteCommand
    lateinit var SelectAllCommand: SelectAllCommand
    lateinit var SelectWordCommand: SelectWordCommand
    lateinit var DuplicateLineCommand: DuplicateLineCommand
    lateinit var LowerCaseCommand: LowerCaseCommand
    lateinit var UpperCaseCommand: UpperCaseCommand
    lateinit var SaveCommand: SaveCommand
    lateinit var SaveAsCommand: SaveAsCommand
    lateinit var SaveAllCommand: SaveAllCommand
    lateinit var UndoCommand: UndoCommand
    lateinit var RedoCommand: RedoCommand
    lateinit var ToggleReadOnlyCommand: ToggleReadOnlyCommand
    lateinit var SearchCommand: SearchCommand
    lateinit var ReplaceCommand: ReplaceCommand
    lateinit var RefreshCommand: RefreshCommand
    lateinit var SyntaxHighlightingCommand: SyntaxHighlightingCommand
    lateinit var ToggleWordWrapCommand: ToggleWordWrapCommand
    lateinit var JumpToLineCommand: JumpToLineCommand
    lateinit var SortLinesAscendingCommand: SortLinesAscendingCommand
    lateinit var SortLinesDescendingCommand: SortLinesDescendingCommand
    lateinit var ShareCommand: ShareCommand
    lateinit var EmulateKeyCommand: EmulateKeyCommand
    lateinit var GoToDefinitionCommand: GoToDefinitionCommand
    lateinit var GoToReferencesCommand: GoToReferencesCommand
    lateinit var RenameSymbolCommand: RenameSymbolCommand
    lateinit var FormatDocumentCommand: FormatDocumentCommand
    lateinit var FormatDocumentLspCommand: FormatDocumentLspCommand
    lateinit var FormatSelectionCommand: FormatSelectionCommand

    fun buildCommands() =
        synchronized(this) {
            registerBuiltin(DocumentationCommand()) { DocumentationCommand = it }
            registerBuiltin(SettingsCommand()) { SettingsCommand = it }
            registerBuiltin(NewFileCommand()) { NewFileCommand = it }
            registerBuiltin(CommandPaletteCommand()) { CommandPaletteCommand = it }
            registerBuiltin(SearchFileFolderCommand()) { SearchFileFolderCommand = it }
            registerBuiltin(SearchCodeCommand()) { SearchCodeCommand = it }
            registerBuiltin(CutCommand()) { CutCommand = it }
            registerBuiltin(CopyCommand()) { CopyCommand = it }
            registerBuiltin(PasteCommand()) { PasteCommand = it }
            registerBuiltin(SelectAllCommand()) { SelectAllCommand = it }
            registerBuiltin(SelectWordCommand()) { SelectWordCommand = it }
            registerBuiltin(DuplicateLineCommand()) { DuplicateLineCommand = it }
            registerBuiltin(LowerCaseCommand()) { LowerCaseCommand = it }
            registerBuiltin(UpperCaseCommand()) { UpperCaseCommand = it }
            registerBuiltin(SaveCommand()) { SaveCommand = it }
            registerBuiltin(SaveAsCommand()) { SaveAsCommand = it }
            registerBuiltin(SaveAllCommand()) { SaveAllCommand = it }
            registerBuiltin(UndoCommand()) { UndoCommand = it }
            registerBuiltin(RedoCommand()) { RedoCommand = it }
            registerBuiltin(ToggleReadOnlyCommand()) { ToggleReadOnlyCommand = it }
            registerBuiltin(SearchCommand()) { SearchCommand = it }
            registerBuiltin(ReplaceCommand()) { ReplaceCommand = it }
            registerBuiltin(RefreshCommand()) { RefreshCommand = it }
            registerBuiltin(SyntaxHighlightingCommand()) { SyntaxHighlightingCommand = it }
            registerBuiltin(ToggleWordWrapCommand()) { ToggleWordWrapCommand = it }
            registerBuiltin(JumpToLineCommand()) { JumpToLineCommand = it }
            registerBuiltin(SortLinesAscendingCommand()) { SortLinesAscendingCommand = it }
            registerBuiltin(SortLinesDescendingCommand()) { SortLinesDescendingCommand = it }
            registerBuiltin(ShareCommand()) { ShareCommand = it }
            registerBuiltin(EmulateKeyCommand()) { EmulateKeyCommand = it }
            registerBuiltin(GoToDefinitionCommand()) { GoToDefinitionCommand = it }
            registerBuiltin(GoToReferencesCommand()) { GoToReferencesCommand = it }
            registerBuiltin(RenameSymbolCommand()) { RenameSymbolCommand = it }
            registerBuiltin(FormatDocumentCommand()) { FormatDocumentCommand = it }
            registerBuiltin(FormatDocumentLspCommand()) { FormatDocumentLspCommand = it }
            registerBuiltin(FormatSelectionCommand()) { FormatSelectionCommand = it }
        }

    private fun <T : Command> registerBuiltin(command: T, assign: (T) -> Unit) {
        if (_commandList.contains(command)) return
        assign(command)
        _commandList.add(command)
        KeybindingsManager.invalidate()
    }

    @XedExtensionPoint
    fun registerCommand(command: Command) {
        val index = _commandList.indexOf(command)
        if (index >= 0) {
            _commandList[index] = command
        } else {
            _commandList.add(command)
        }
        KeybindingsManager.invalidate()
    }

    @XedExtensionPoint
    fun unregisterCommand(command: Command) {
        _commandList.remove(command)
        KeybindingsManager.invalidate()
    }

    private val disposer =
        Disposer<Command> {
            unregisterCommand(it)
        }

    @XedExtensionPoint
    fun registerCommand(command: Command, dm: DisposableManager) {
        registerCommand(command)
        dm.register(command, disposer)
    }

    @XedExtensionPoint
    fun unregisterCommand(command: Command, dm: DisposableManager) {
        unregisterCommand(command)
        dm.unregister(command, disposer)
    }

    fun getForId(id: String): Command? = findRecursive(id, commandList)

    fun getParentCommand(command: Command): Command? = findParent(command, commandList)

    private fun findParent(target: Command, commands: List<Command>): Command? {
        for (parent in commands) {
            val children = parent.childCommands
            if (children.any { it.id == target.id }) return parent

            val match = findParent(target, children)
            if (match != null) return match
        }
        return null
    }

    private fun findRecursive(id: String, commands: List<Command>): Command? {
        for (command in commands) {
            if (command.id == id) return command
            val children = command.childCommands

            val match = findRecursive(id, children)
            if (match != null) return match
        }
        return null
    }
}
