package com.scto.mobile.ide.icons





import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.res.ResourcesCompat
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.loadSvg
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
