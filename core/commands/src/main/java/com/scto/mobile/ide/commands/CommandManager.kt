package com.scto.mobile.ide.commands

object CommandManager {
    private val commands = mutableListOf<Command>()

    fun registerCommand(command: Command) {
        if (commands.none { it.id == command.id }) {
            commands.add(command)
        }
    }

    fun unregisterCommand(commandId: String) {
        commands.removeAll { it.id == commandId }
    }

    fun getCommands(): List<Command> {
        return commands.toList()
    }

    suspend fun executeCommand(id: String, context: CommandContext) {
        commands.find { it.id == id }?.execute(context)
    }
}
