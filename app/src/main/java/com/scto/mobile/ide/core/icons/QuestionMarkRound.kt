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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val LucideCircleQuestionMark: ImageVector
    get() {
        if (_LucideCircleQuestionMark != null) return _LucideCircleQuestionMark!!

        _LucideCircleQuestionMark =
            ImageVector.Builder(
                    name = "circle-question-mark",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(
                        fill = SolidColor(Color.Transparent),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(22f, 12f)
                        arcTo(10f, 10f, 0f, false, true, 12f, 22f)
                        arcTo(10f, 10f, 0f, false, true, 2f, 12f)
                        arcTo(10f, 10f, 0f, false, true, 22f, 12f)
                        close()
                    }
                    path(
                        fill = SolidColor(Color.Transparent),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(9.09f, 9f)
                        arcToRelative(3f, 3f, 0f, false, true, 5.83f, 1f)
                        curveToRelative(0f, 2f, -3f, 3f, -3f, 3f)
                    }
                    path(
                        fill = SolidColor(Color.Transparent),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(12f, 17f)
                        horizontalLineToRelative(0.01f)
                    }
                }
                .build()

        return _LucideCircleQuestionMark!!
    }

private var _LucideCircleQuestionMark: ImageVector? = null
