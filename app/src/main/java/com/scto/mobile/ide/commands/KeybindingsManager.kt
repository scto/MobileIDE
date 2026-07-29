package com.scto.mobile.ide.commands

import android.view.KeyEvent
import io.github.rosemoe.sora.event.KeyBindingEvent

data class KeyCombination(
    val keyCode: Int,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
) {
    fun getDisplayName(): String = "KeyBind"
}

object KeybindingsManager {
    fun handleKeyBindingEvent(event: KeyBindingEvent): Boolean = false
    fun getCommandIdForKeybind(keyCombination: KeyCombination): String? = null
}
