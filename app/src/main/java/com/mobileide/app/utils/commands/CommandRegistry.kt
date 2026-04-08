package com.mobileide.app.utils.commands

import androidx.compose.runtime.mutableStateListOf
import com.mobileide.app.utils.commands.editor.*
import com.mobileide.app.utils.commands.global.*
import com.mobileide.app.viewmodel.IDEViewModel

/**
 * Central registry of all [Command] instances.
 *
 * Initialise once per app lifecycle with [build], then access
 * [allCommands] from the Command Palette composable.
 *
 * External features can register additional commands via [register].
 */
object CommandRegistry {

    private val _builtIn   = mutableListOf<Command>()
    private val _extension = mutableStateListOf<Command>()

    /** All built-in + extension commands. */
    val allCommands: List<Command>
        get() = _builtIn + _extension

    // ── Typed references for direct call sites ────────────────────────────────
    lateinit var saveCommand:          SaveCommand          private set
    lateinit var saveAllCommand:       SaveAllCommand       private set
    lateinit var undoCommand:          UndoCommand          private set
    lateinit var redoCommand:          RedoCommand          private set
    lateinit var buildProjectCommand:  BuildProjectCommand  private set
    lateinit var commandPaletteCommand:CommandPaletteCommand private set

    // ── Init ──────────────────────────────────────────────────────────────────

    fun build(vm: IDEViewModel) {
        _builtIn.clear()
        val ctx = CommandContext(vm)

        // ── Editor section (sectionId = 0) ────────────────────────────────────
        fun <T : Command> reg(cmd: T): T { _builtIn.add(cmd); return cmd }

        reg(CopyCommand(ctx))
        reg(CutCommand(ctx))
        reg(PasteCommand(ctx))
        reg(SelectAllCommand(ctx))
        reg(SelectWordCommand(ctx))
        undoCommand  = reg(UndoCommand(ctx))
        redoCommand  = reg(RedoCommand(ctx))
        saveCommand  = reg(SaveCommand(ctx))
        saveAllCommand = reg(SaveAllCommand(ctx))
        reg(UpperCaseCommand(ctx))
        reg(LowerCaseCommand(ctx))
        reg(FormatDocumentCommand(ctx))
        reg(ToggleWordWrapCommand(ctx))
        reg(JumpToLineCommand(ctx))
        reg(ShareFileCommand(ctx))

        // ── Global section (sectionId = 1) ────────────────────────────────────
        fun <T : Command> g(cmd: T): T { _builtIn.add(cmd.also { it.also {} }); return cmd }
        commandPaletteCommand = g(CommandPaletteCommand(ctx))
        buildProjectCommand   = g(BuildProjectCommand(ctx))
        g(HomeCommand(ctx))
        g(TerminalCommand(ctx))
        g(GitCommand(ctx))
        g(ProjectSearchCommand(ctx))
        g(GradleTasksCommand(ctx))
        g(CloseTabCommand(ctx))
        g(EditorSettingsCommand(ctx))
        g(SettingsCommand(ctx))

        KeybindingsManager.loadKeybindings()
        KeybindingsManager.generateKeybindMap()
    }

    /** Register a runtime command (e.g. from a plugin or screen). */
    fun register(command: Command) {
        if (!_extension.contains(command)) _extension.add(command)
    }

    fun unregister(command: Command) = _extension.remove(command)

    fun getById(id: String): Command? = allCommands.firstOrNull { it.id == id }
}
