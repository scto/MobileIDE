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
 
package com.scto.mobile.ide.ui.terminal

import android.view.View
import android.widget.Button

import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeyButton
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysView

import com.termux.terminal.TerminalSession

// 🔥 Directly port rk terminal's implementation
class VirtualKeysListener(val session: TerminalSession) : VirtualKeysView.IVirtualKeysView {
    override fun onVirtualKeyButtonClick(
        view: View?,
        buttonInfo: VirtualKeyButton?,
        button: Button?,
    ) {
        val key = buttonInfo?.key ?: return
        val writeable: String =
            when (key) {
                "UP" -> "\u001B[A"
                "DOWN" -> "\u001B[B"
                "LEFT" -> "\u001B[D"
                "RIGHT" -> "\u001B[C"
                "ENTER" -> "\u000D"
                "PGUP" -> "\u001B[5~"
                "PGDN" -> "\u001B[6~"
                "TAB" -> "\u0009"
                "HOME" -> "\u001B[H"
                "END" -> "\u001B[F"
                "ESC" -> "\u001B"
                else -> key
            }

        session.write(writeable)
    }

    override fun performVirtualKeyButtonHapticFeedback(
        view: View?,
        buttonInfo: VirtualKeyButton?,
        button: Button?,
    ): Boolean {
        return false
    }
}