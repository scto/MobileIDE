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

package com.scto.mobile.ide.icons

import com.scto.mobile.ide.core.common.icons.Icon as CoreIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder

@Composable
fun MobileIDEIcon(
    icon: CoreIcon,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    when (icon) {
        is CoreIcon.ResourceIcon -> {
            Icon(
                painter = painterResource(icon.drawableRes),
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint,
            )
        }

        is CoreIcon.ExternalResourceIcon -> {
            val theme = LocalContext.current.theme

            Icon(
                imageVector = ImageVector.vectorResource(theme = theme, resId = icon.drawableRes, res = icon.resources),
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint,
            )
        }

        is CoreIcon.VectorIcon -> {
            Icon(imageVector = icon.vector, contentDescription = contentDescription, modifier = modifier, tint = tint)
        }

        is CoreIcon.SvgIcon -> {
            AsyncImage(
                model = icon.file,
                imageLoader = rememberSvgImageLoader(),
                contentDescription = contentDescription,
                modifier = modifier,
                colorFilter = ColorFilter.tint(tint),
            )
        }

        is CoreIcon.TextIcon -> {
            val textSize =
                when (icon.text.length) {
                    in 1..2 -> 14.sp
                    in 3..5 -> 12.sp
                    else -> 10.sp
                }

            Text(text = icon.text, fontFamily = FontFamily.Monospace, color = tint, fontSize = textSize, maxLines = 1)
        }
    }
}

@Composable
fun rememberSvgImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember { ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build() }
}
