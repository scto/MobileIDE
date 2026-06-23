/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.ui.settings.keybinds

import android.view.KeyEvent

import com.rk.icons.Icon
import com.rk.resources.drawables
import com.rk.resources.getString
import com.rk.resources.strings

data object KeyUtils {
    fun getKeyDisplayName(keyCode: Int): String {
        when (keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.META_SHIFT_ON -> return strings.shift.getString()
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.META_CTRL_ON -> return strings.ctrl.getString()
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.META_ALT_ON -> return strings.alt.getString()
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.META_META_ON -> return "Meta"

            KeyEvent.KEYCODE_DPAD_DOWN -> return "Arrow Down"
            KeyEvent.KEYCODE_DPAD_UP -> return "Arrow Up"
            KeyEvent.KEYCODE_DPAD_LEFT -> return "Arrow Left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> return "Arrow Right"
            KeyEvent.KEYCODE_DEL -> return "Backspace"
            KeyEvent.KEYCODE_FORWARD_DEL -> return "Delete"
        }

        val keyName = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        return keyName.lowercase().split("_").joinToString(" ") { it[0].uppercase() + it.substring(1) }
    }

    fun getShortDisplayName(keyCode: Int): String {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> return "↓"
            KeyEvent.KEYCODE_DPAD_UP -> return "↑"
            KeyEvent.KEYCODE_DPAD_LEFT -> return "←"
            KeyEvent.KEYCODE_DPAD_RIGHT -> return "→"
            KeyEvent.KEYCODE_DEL -> return "⌫"
            KeyEvent.KEYCODE_FORWARD_DEL -> return "⌦"
            KeyEvent.KEYCODE_TAB -> return "⇥"
            KeyEvent.KEYCODE_ENTER -> return "↩"
            KeyEvent.KEYCODE_SPACE -> return "␣"
            KeyEvent.KEYCODE_MINUS -> return "-"
            KeyEvent.KEYCODE_EQUALS -> return "="
            KeyEvent.KEYCODE_SLASH -> return "/"
            KeyEvent.KEYCODE_BACKSLASH -> return "\\"
            KeyEvent.KEYCODE_PERIOD -> return "."
            KeyEvent.KEYCODE_COMMA -> return ","
            KeyEvent.KEYCODE_SEMICOLON -> return ";"
            KeyEvent.KEYCODE_APOSTROPHE -> return "'"
            KeyEvent.KEYCODE_GRAVE -> return "`"
            KeyEvent.KEYCODE_LEFT_BRACKET -> return "["
            KeyEvent.KEYCODE_RIGHT_BRACKET -> return "]"
        }

        val keyName = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        return keyName.lowercase().split("_").joinToString("") { it[0].uppercase() + it.substring(1) }
    }

    fun getKeyCodeFromChar(char: Char): Int {
        val keyName =
            when (char) {
                ' ' -> "SPACE"
                '\n' -> "ENTER"
                '-' -> "MINUS"
                '=' -> "EQUALS"
                '/' -> "SLASH"
                '\\' -> "BACKSLASH"
                '.' -> "PERIOD"
                ',' -> "COMMA"
                ';' -> "SEMICOLON"
                '\'' -> "APOSTROPHE"
                '`' -> "GRAVE"
                '[' -> "LEFT_BRACKET"
                ']' -> "RIGHT_BRACKET"
                else -> char.uppercase()
            }

        return KeyEvent.keyCodeFromString("KEYCODE_${keyName}")
    }

    fun getKeyIcon(keyCode: Int): Icon {
        return Icon.ResourceIcon(
            when (keyCode) {
                KeyEvent.KEYCODE_SHIFT_LEFT -> drawables.shift
                KeyEvent.KEYCODE_DPAD_DOWN -> drawables.chevron_down
                KeyEvent.KEYCODE_DPAD_UP -> drawables.chevron_up
                KeyEvent.KEYCODE_DPAD_LEFT -> drawables.chevron_left
                KeyEvent.KEYCODE_DPAD_RIGHT -> drawables.chevron_right
                KeyEvent.KEYCODE_DEL -> drawables.backspace
                KeyEvent.KEYCODE_FORWARD_DEL -> drawables.backspace_mirrored
                KeyEvent.KEYCODE_TAB -> drawables.kbd_tab
                else -> drawables.keyboard
            }
        )
    }

    fun isModifierKey(keyCode: Int): Boolean {
        return keyCode in
            listOf(
                KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.KEYCODE_CTRL_RIGHT,
                KeyEvent.META_CTRL_ON,
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.KEYCODE_SHIFT_RIGHT,
                KeyEvent.META_SHIFT_ON,
                KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.KEYCODE_ALT_RIGHT,
                KeyEvent.META_ALT_ON,
                KeyEvent.KEYCODE_META_LEFT,
                KeyEvent.KEYCODE_META_RIGHT,
                KeyEvent.META_META_ON,
            )
    }

    fun isModifierKey(event: KeyEvent): Boolean = isModifierKey(event.keyCode)

    fun isModifierKey(event: androidx.compose.ui.input.key.KeyEvent): Boolean = isModifierKey(event.nativeKeyEvent)
}
