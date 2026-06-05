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

package com.scto.mobile.ide.ui.editor.aicoding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

enum class DockSide {
    Right,
    Left,
}

@Stable
class AICodingState {
    var isExpanded by mutableStateOf(false)
    var dockSide by mutableStateOf(DockSide.Right)
    var windowWidth by mutableStateOf(300.dp)
    var windowHeight by mutableStateOf(400.dp)
    var windowOffset by mutableStateOf(Offset.Zero)
    var lastFloatingPosition by mutableStateOf<Offset?>(null)
    var isDragging by mutableStateOf(false)

    var isMaximized by mutableStateOf(false)
    internal var restoreWidth by mutableStateOf(300.dp)
    internal var restoreHeight by mutableStateOf(400.dp)
}

@Composable
fun rememberAICodingState(): AICodingState {
    return remember { AICodingState() }
}
