package com.mobileide.app.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * Unified icon representation — supports Material vector icons, drawable
 * resources, and single/short text glyphs (e.g. emoji or abbreviations).
 *
 * Render with [AppIcon].
 */
sealed class AppIconType {
    class VectorIcon(val vector: ImageVector)              : AppIconType()
    class DrawableIcon(@DrawableRes val res: Int)          : AppIconType()
    class TextIcon(val text: String)                       : AppIconType()
}

/**
 * Composable that renders any [AppIconType] with a unified API.
 */
@Composable
fun AppIcon(
    icon: AppIconType,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    when (icon) {
        is AppIconType.VectorIcon -> Icon(
            imageVector        = icon.vector,
            contentDescription = contentDescription,
            modifier           = modifier,
            tint               = tint,
        )
        is AppIconType.DrawableIcon -> Icon(
            painter            = painterResource(icon.res),
            contentDescription = contentDescription,
            modifier           = modifier,
            tint               = tint,
        )
        is AppIconType.TextIcon -> {
            val fontSize = when (icon.text.length) {
                in 1..2 -> 14.sp
                in 3..5 -> 12.sp
                else    -> 10.sp
            }
            Text(text = icon.text, fontFamily = FontFamily.Monospace,
                color = tint, fontSize = fontSize, maxLines = 1)
        }
    }
}
