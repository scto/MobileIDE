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

val MobileIDEIcons.Error: ImageVector
    get() {
        if (_Error != null) return _Error!!

        _Error =
            ImageVector.Builder(
                    name = "Error",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF000000))) {
                        moveTo(480f, 680f)
                        quadToRelative(17f, 0f, 28.5f, -11.5f)
                        reflectiveQuadTo(520f, 640f)
                        reflectiveQuadToRelative(-11.5f, -28.5f)
                        reflectiveQuadTo(480f, 600f)
                        reflectiveQuadToRelative(-28.5f, 11.5f)
                        reflectiveQuadTo(440f, 640f)
                        reflectiveQuadToRelative(11.5f, 28.5f)
                        reflectiveQuadTo(480f, 680f)
                        moveToRelative(-40f, -160f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(-240f)
                        horizontalLineToRelative(-80f)
                        close()
                        moveToRelative(40f, 360f)
                        quadToRelative(-83f, 0f, -156f, -31.5f)
                        reflectiveQuadTo(197f, 763f)
                        reflectiveQuadToRelative(-85.5f, -127f)
                        reflectiveQuadTo(80f, 480f)
                        reflectiveQuadToRelative(31.5f, -156f)
                        reflectiveQuadTo(197f, 197f)
                        reflectiveQuadToRelative(127f, -85.5f)
                        reflectiveQuadTo(480f, 80f)
                        reflectiveQuadToRelative(156f, 31.5f)
                        reflectiveQuadTo(763f, 197f)
                        reflectiveQuadToRelative(85.5f, 127f)
                        reflectiveQuadTo(880f, 480f)
                        reflectiveQuadToRelative(-31.5f, 156f)
                        reflectiveQuadTo(763f, 763f)
                        reflectiveQuadToRelative(-127f, 85.5f)
                        reflectiveQuadTo(480f, 880f)
                        moveToRelative(0f, -80f)
                        quadToRelative(134f, 0f, 227f, -93f)
                        reflectiveQuadToRelative(93f, -227f)
                        reflectiveQuadToRelative(-93f, -227f)
                        reflectiveQuadToRelative(-227f, -93f)
                        reflectiveQuadToRelative(-227f, 93f)
                        reflectiveQuadToRelative(-93f, 227f)
                        reflectiveQuadToRelative(93f, 227f)
                        reflectiveQuadToRelative(227f, 93f)
                        moveToRelative(0f, -320f)
                    }
                }
                .build()

        return _Error!!
    }

private var _Error: ImageVector? = null
