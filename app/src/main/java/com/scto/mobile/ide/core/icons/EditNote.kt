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

val MobileIDEIcons.Edit_note: ImageVector
    get() {
        if (_Edit_note != null) return _Edit_note!!

        _Edit_note =
            ImageVector.Builder(
                    name = "Edit_note",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF000000))) {
                        moveTo(160f, 560f)
                        verticalLineToRelative(-80f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(80f)
                        close()
                        moveToRelative(0f, -160f)
                        verticalLineToRelative(-80f)
                        horizontalLineToRelative(440f)
                        verticalLineToRelative(80f)
                        close()
                        moveToRelative(0f, -160f)
                        verticalLineToRelative(-80f)
                        horizontalLineToRelative(440f)
                        verticalLineToRelative(80f)
                        close()
                        moveToRelative(360f, 560f)
                        verticalLineToRelative(-123f)
                        lineToRelative(221f, -220f)
                        quadToRelative(9f, -9f, 20f, -13f)
                        reflectiveQuadToRelative(22f, -4f)
                        quadToRelative(12f, 0f, 23f, 4.5f)
                        reflectiveQuadToRelative(20f, 13.5f)
                        lineToRelative(37f, 37f)
                        quadToRelative(8f, 9f, 12.5f, 20f)
                        reflectiveQuadToRelative(4.5f, 22f)
                        reflectiveQuadToRelative(-4f, 22.5f)
                        reflectiveQuadToRelative(-13f, 20.5f)
                        lineTo(643f, 800f)
                        close()
                        moveToRelative(300f, -263f)
                        lineToRelative(-37f, -37f)
                        close()
                        moveTo(580f, 740f)
                        horizontalLineToRelative(38f)
                        lineToRelative(121f, -122f)
                        lineToRelative(-18f, -19f)
                        lineToRelative(-19f, -18f)
                        lineToRelative(-122f, 121f)
                        close()
                        moveToRelative(141f, -141f)
                        lineToRelative(-19f, -18f)
                        lineToRelative(37f, 37f)
                        close()
                    }
                }
                .build()

        return _Edit_note!!
    }

private var _Edit_note: ImageVector? = null
