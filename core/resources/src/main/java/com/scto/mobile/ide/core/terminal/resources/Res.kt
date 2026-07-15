package com.scto.mobile.ide.core.terminal.resources

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

typealias drawables = R.drawable
typealias strings = R.string
typealias plurals = R.plurals

object Res{
    @JvmField
    var application:Application? = null
}

inline fun Int.getString():String{
    return ContextCompat.getString(Res.application!!, this)
}

inline fun Int.getDrawable():Drawable?{
    return ContextCompat.getDrawable(Res.application!!,this)
}

inline fun Int.getFilledString(vararg args: Any?): String {
    return this.getString().fillPlaceholders(*args)
}

inline fun String.fillPlaceholders(vararg args: Any?): String {
    return String.format(this, *args)
}

inline fun Int.getQuantityString(quantity: Int, vararg formatArgs: Any?): String {
    return Res.application!!.resources.getQuantityString(this, quantity, *formatArgs)
}