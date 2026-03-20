package com.mobileide.app.utils.commands

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.rosemoe.sora.event.KeyBindingEvent

/**
 * Persists and resolves keyboard shortcuts for [Command]s.
 *
 * Storage: SharedPreferences (`mobileide_keybinds`).
 * Format:  JSON array of [KeyAction].
 */
object KeybindingsManager {

    private const val PREFS_NAME = "mobileide_keybinds"
    private const val KEY_BINDS  = "custom_binds"
    private val gson             = Gson()

    private val customKeybinds = mutableListOf<KeyAction>()

    /** Live map: [KeyCombination] → commandId, used for O(1) dispatch. */
    val keybindMap = mutableMapOf<KeyCombination, String>()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Persistence ───────────────────────────────────────────────────────────

    fun saveKeybindings(context: Context) {
        prefs(context).edit().putString(KEY_BINDS, gson.toJson(customKeybinds)).apply()
    }

    fun loadKeybindings(context: Context? = null) {
        val ctx = context ?: return
        val json = prefs(ctx).getString(KEY_BINDS, "") ?: return
        if (json.isEmpty()) { generateKeybindMap(); return }
        runCatching {
            val type = object : TypeToken<List<KeyAction>>() {}
            customKeybinds.clear()
            customKeybinds.addAll(gson.fromJson(json, type))
        }
        generateKeybindMap()
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    fun setCustomKey(keyAction: KeyAction, context: Context) {
        customKeybinds.removeIf { it.commandId == keyAction.commandId }
        customKeybinds.add(keyAction)
        saveKeybindings(context)
        generateKeybindMap()
    }

    fun resetKey(commandId: String, context: Context) {
        customKeybinds.removeIf { it.commandId == commandId }
        saveKeybindings(context)
        generateKeybindMap()
    }

    fun resetAll(context: Context) {
        customKeybinds.clear()
        saveKeybindings(context)
        generateKeybindMap()
    }

    fun hasConflict(combo: KeyCombination, commandId: String) =
        keybindMap[combo]?.let { it != commandId } == true

    // ── Map generation ────────────────────────────────────────────────────────

    fun generateKeybindMap() {
        keybindMap.clear()
        // User overrides first
        customKeybinds.forEach { keybindMap[it.keyCombination] = it.commandId }
        val overridden = customKeybinds.map { it.commandId }.toSet()
        // Defaults for everything else
        CommandRegistry.allCommands.forEach { cmd ->
            if (cmd.id !in overridden) {
                cmd.defaultKeybinds?.let { keybindMap[it] = cmd.id }
            }
        }
    }

    fun getForCommand(commandId: String): KeyCombination? =
        keybindMap.entries.firstOrNull { it.value == commandId }?.key

    // ── Dispatch ──────────────────────────────────────────────────────────────

    /** Call from Activity.onKeyDown — returns true if consumed. */
    fun handleGlobalKey(event: android.view.KeyEvent, activity: android.app.Activity): Boolean {
        if (event.action != android.view.KeyEvent.ACTION_DOWN) return false
        val combo   = KeyCombination(event.keyCode, event.isCtrlPressed, event.isAltPressed, event.isShiftPressed)
        val cmd     = CommandRegistry.getById(keybindMap[combo] ?: return false) ?: return false
        if (!cmd.isSupported() || !cmd.isEnabled()) return false
        if (cmd is EditorCommand) return false   // editor events handled by Sora
        cmd.performCommand(ActionContext(activity))
        return true
    }

    /** Call from Sora KeyBindingEvent subscriber. */
    fun handleEditorKey(event: KeyBindingEvent, activity: android.app.Activity): Boolean {
        if (event.action != android.view.KeyEvent.ACTION_DOWN) return false
        val combo = KeyCombination(event.keyCode, event.isCtrlPressed, event.isAltPressed, event.isShiftPressed)
        val cmd   = CommandRegistry.getById(keybindMap[combo] ?: return false) ?: return false
        if (!cmd.isSupported() || !cmd.isEnabled()) return false
        if (cmd !is EditorCommand) return false
        cmd.performCommand(ActionContext(activity))
        return true
    }
}

data class KeyAction(val commandId: String, val keyCombination: KeyCombination)
