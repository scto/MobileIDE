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

import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.res.ResourcesCompat
import com.rk.utils.application
import com.rk.utils.loadSvg
import java.io.File

sealed class Icon {
    class ResourceIcon(@androidx.annotation.DrawableRes val drawableRes: Int) : Icon()

    class ExternalResourceIcon(@androidx.annotation.DrawableRes val drawableRes: Int, val resources: Resources) : Icon()

    class VectorIcon(val vector: ImageVector) : Icon()

    class SvgIcon(val file: File) : Icon()

    class TextIcon(val text: String) : Icon()

    fun toDrawable(): Drawable? {
        return when (this) {
            is ResourceIcon -> {
                ResourcesCompat.getDrawable(application!!.resources, drawableRes, null)
            }
            is ExternalResourceIcon -> {
                ResourcesCompat.getDrawable(resources, drawableRes, null)
            }
            is SvgIcon -> loadSvg(file.inputStream())
            else -> null
        }
    }
}
