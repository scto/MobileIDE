/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

package io.github.rosemoe.sora.lang.styling.line

import io.github.rosemoe.sora.lang.styling.color.ConstColor
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor

/**
 * A breakpoint marker that replaces the line number with a colored circle.
 * When a breakpoint is set on a line, the line number will be replaced by a circle
 * with the specified color.
 *
 * @param line the line index (0-based)
 * @param color the color of the breakpoint circle
 * @param enabled whether the breakpoint is enabled (affects opacity)
 * @param verified whether the breakpoint has been verified by the debugger
 * @author TinaIDE
 */
data class LineBreakpoint(
    override var line: Int,
    val color: ResolvableColor = ConstColor(0xFFE53935.toInt()),  // Material Red 600
    val enabled: Boolean = true,
    val verified: Boolean = false
) : LineAnchorStyle(line)