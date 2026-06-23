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

package com.scto.mobile.ide.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MobileIDEIcons.CreateNewFolder: ImageVector
    get() {
        if (_CreateNewFolder != null) return _CreateNewFolder!!

        _CreateNewFolder =
            ImageVector.Builder(
                    name = "CreateNewFolder",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF000000))) {
                        moveTo(20.0f, 6.0f)
                        horizontalLineToRelative(-8.0f)
                        lineToRelative(-2.0f, -2.0f)
                        lineTo(4.0f, 4.0f)
                        curveToRelative(-1.11f, 0.0f, -1.99f, 0.89f, -1.99f, 2.0f)
                        lineTo(2.0f, 18.0f)
                        curveToRelative(0.0f, 1.11f, 0.89f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(16.0f)
                        curveToRelative(1.11f, 0.0f, 2.0f, -0.89f, 2.0f, -2.0f)
                        lineTo(22.0f, 8.0f)
                        curveToRelative(0.0f, -1.11f, -0.89f, -2.0f, -2.0f, -2.0f)
                        close()
                        moveTo(20.0f, 18.0f)
                        lineTo(4.0f, 18.0f)
                        lineTo(4.0f, 6.0f)
                        horizontalLineToRelative(5.17f)
                        lineToRelative(2.0f, 2.0f)
                        lineTo(20.0f, 8.0f)
                        verticalLineToRelative(10.0f)
                        close()
                        moveTo(12.0f, 14.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(-2.0f)
                        close()
                    }
                }
                .build()

        return _CreateNewFolder!!
    }

private var _CreateNewFolder: ImageVector? = null
