package com.scto.mobile.ide.settings.editor





import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.Command
import com.scto.mobile.ide.commands.CommandPalette
import com.scto.mobile.ide.commands.CommandProvider
import com.scto.mobile.ide.icons.Icon











@Composable
fun CommandSelectionDialog(
    commandIds: SnapshotStateList<String>,
    saveOrder: (SnapshotStateList<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val dialogCommands =
        CommandProvider.commandList.map { command ->
            val existingCommands = command.childCommands
            val patchedChildCommands =
                if (existingCommands.isEmpty()) {
                    emptyList()
                } else {
                    buildAddActions(command, commandIds, existingCommands, saveOrder)
                }

            val hasChildCommands = patchedChildCommands.isNotEmpty()
            command.copy(
                childCommands = patchedChildCommands,
                action = {
                    commandIds.add(command.id)
                    saveOrder(commandIds)
                },
                isSupported = { true },
                isEnabled = { !commandIds.contains(command.id) || hasChildCommands },
            )
        }

    CommandPalette(progress = 1f, commands = dialogCommands, lastUsedCommand = null) { onDismiss() }
}

private fun buildAddActions(
    command: Command,
    commandIds: SnapshotStateList<String>,
    existingCommands: List<Command>,
    saveOrder: (SnapshotStateList<String>) -> Unit,
): List<Command> = buildList {
    add(
        object : Command() {
            override val id: String = command.id

            override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.add_parent_command.getString()

            override fun action(context: ActionContext) {
                commandIds.add(command.id)
                saveOrder(commandIds)
            }

            override val sectionId: Int = 0

            override fun isEnabled(): Boolean = !commandIds.contains(command.id)

            override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.arrow_outward)
        }
    )
    addAll(
        existingCommands.map { command ->
            command.copy(
                action = {
                    commandIds.add(command.id)
                    saveOrder(commandIds)
                },
                isEnabled = { !commandIds.contains(command.id) },
                isSupported = { true },
                sectionId = command.sectionId + 1,
            )
        }
    )
}
