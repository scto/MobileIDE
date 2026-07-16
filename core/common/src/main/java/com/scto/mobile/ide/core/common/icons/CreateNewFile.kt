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

package com.scto.mobile.ide.core.common.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MobileIDEIcons.CreateNewFile: ImageVector
    get() {
        if (_CreateNewFile != null) return _CreateNewFile!!

        _CreateNewFile =
            ImageVector.Builder(
                    name = "CreateNewFolder",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF000000))) {
                        moveTo(13.0f, 11.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(3.0f)
                        lineTo(8.0f, 14.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(3.0f)
                        verticalLineToRelative(3.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(-3.0f)
                        horizontalLineToRelative(3.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineToRelative(-3.0f)
                        close()
                        moveTo(14.0f, 2.0f)
                        lineTo(6.0f, 2.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        verticalLineToRelative(16.0f)
                        curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
                        lineTo(18.0f, 22.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        lineTo(20.0f, 8.0f)
                        lineToRelative(-6.0f, -6.0f)
                        close()
                        moveTo(18.0f, 20.0f)
                        lineTo(6.0f, 20.0f)
                        lineTo(6.0f, 4.0f)
                        horizontalLineToRelative(7.0f)
                        verticalLineToRelative(5.0f)
                        horizontalLineToRelative(5.0f)
                        verticalLineToRelative(11.0f)
                        close()
                    }
                }
                .build()

        return _CreateNewFile!!
    }

private var _CreateNewFile: ImageVector? = null
