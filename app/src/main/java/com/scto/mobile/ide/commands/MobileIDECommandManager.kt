package com.scto.mobile.ide.commands

import com.scto.mobile.ide.ui.editor.viewmodel.EditorViewModel


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
