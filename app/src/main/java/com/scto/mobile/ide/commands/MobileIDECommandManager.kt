package com.scto.mobile.ide.commands

import com.rk.commands.Command
import com.rk.commands.CommandContext
import com.scto.mobile.ide.ui.editor.viewmodel.EditorViewModel

data class MobileIDECommandContext(val editorViewModel: EditorViewModel) : CommandContext

object MobileIDECommandManager {
    private val commands = mutableMapOf<String, Command>()

    fun registerCommand(command: Command) {
        commands[command.id] = command
    }

    fun getCommand(id: String): Command? = commands[id]

    fun getAllCommands(): List<Command> = commands.values.toList()

    suspend fun executeCommand(id: String, context: MobileIDECommandContext) {
        commands[id]?.execute(context)
    }
}
