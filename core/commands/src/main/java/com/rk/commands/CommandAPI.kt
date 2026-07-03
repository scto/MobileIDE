package com.rk.commands

import android.app.Activity

interface CommandContext

interface Command {
    val id: String
    val title: String
    val description: String
    val icon: Any?

    suspend fun execute(context: CommandContext)
}

abstract class BaseCommand : Command {
    override val icon: Any? = null
}
