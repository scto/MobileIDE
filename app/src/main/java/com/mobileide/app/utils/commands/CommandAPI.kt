package com.mobileide.app.utils.commands

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import com.mobileide.app.editor.Editor
import com.mobileide.app.ui.icons.AppIconType
import com.mobileide.app.viewmodel.IDEViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Context objects passed to each command at execution time
// ─────────────────────────────────────────────────────────────────────────────

/** Available to every command at all times — holds the root ViewModel. */
data class CommandContext(val vm: IDEViewModel)

/** Passed to [Command.action] — guaranteed to have a live Activity. */
data class ActionContext(val activity: Activity)

/** Passed to [EditorCommand.action] — active editor + tab are resolved. */
data class EditorActionContext(
    val activity: Activity,
    val editor: Editor,
    val tabIndex: Int,
)

// ─────────────────────────────────────────────────────────────────────────────
// Key combination  (for keybind display + hardware keyboard)
// ─────────────────────────────────────────────────────────────────────────────

data class KeyCombination(
    val keyCode: Int,
    val ctrl: Boolean  = false,
    val alt: Boolean   = false,
    val shift: Boolean = false,
) {
    fun getDisplayName(): String = buildString {
        if (ctrl)  append("Ctrl+")
        if (shift) append("Shift+")
        if (alt)   append("Alt+")
        append(android.view.KeyEvent.keyCodeToString(keyCode)
            .removePrefix("KEYCODE_")
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Base Command
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single executable action in the Command Palette.
 *
 * - [id]              — stable unique string, used for keybind storage.
 * - [getLabel]        — displayed name (localised where needed).
 * - [getIcon]         — icon rendered next to the label.
 * - [action]          — executes the command.
 * - [isEnabled]       — whether the item is tappable.
 * - [isSupported]     — whether the item is shown at all.
 * - [childCommands]   — if non-empty, tapping opens a sub-palette.
 * - [sectionId]       — groups commands with a divider.
 * - [defaultKeybinds] — hardware keyboard shortcut.
 */
abstract class Command(val ctx: CommandContext) {
    abstract val id: String
    open val prefix: String? = null

    abstract fun getLabel(): String
    abstract fun getIcon(): AppIconType

    abstract fun action(actionContext: ActionContext)

    open fun isEnabled(): Boolean  = true
    open fun isSupported(): Boolean = true

    open val preferText: Boolean               = false
    open val childCommands: List<Command>       = emptyList()
    open fun getChildSearchPlaceholder(): String? = null
    open val sectionId: Int                    = 0
    open val defaultKeybinds: KeyCombination?  = null

    /** Execute or open child sub-palette via the ViewModel. */
    fun performCommand(actionContext: ActionContext) {
        if (childCommands.isNotEmpty()) {
            ctx.vm.showCommandPalette(getChildSearchPlaceholder(), childCommands)
        } else {
            action(actionContext)
        }
    }

    override fun equals(other: Any?) = other is Command && id == other.id
    override fun hashCode()          = id.hashCode()
}

interface ToggleableCommand { fun isOn(): Boolean }

// ─────────────────────────────────────────────────────────────────────────────
// Global command  — no editor context required
// ─────────────────────────────────────────────────────────────────────────────

abstract class GlobalCommand(ctx: CommandContext) : Command(ctx)

// ─────────────────────────────────────────────────────────────────────────────
// Editor command  — only shown / enabled when an editor tab is active
// ─────────────────────────────────────────────────────────────────────────────

abstract class EditorCommand(ctx: CommandContext) : Command(ctx) {

    final override fun action(actionContext: ActionContext) {
        val tabIndex = ctx.vm.activeTabIndex.value
        val editor   = ctx.vm.activeEditorRef?.get() ?: return
        action(EditorActionContext(actionContext.activity, editor, tabIndex))
    }

    abstract fun action(editorActionContext: EditorActionContext)

    final override fun isSupported(): Boolean =
        ctx.vm.activeTab.value != null && isSupported(ctx.vm)

    final override fun isEnabled(): Boolean =
        ctx.vm.activeTab.value != null && isEnabled(ctx.vm)

    open fun isSupported(vm: IDEViewModel): Boolean = true
    open fun isEnabled(vm: IDEViewModel): Boolean    = true
}
