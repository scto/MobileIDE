package com.mobile.ide.core.resources

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

typealias drawables = R.drawable

typealias strings = R.string

object Res {
    @JvmField var application: Application? = null
}

/**
 * Holt einen String sicher. Falls die Application nicht initialisiert wurde, wird eine leere Zeichenkette zurückgegeben
 * oder eine Exception geworfen.
 */
inline fun Int.getString(): String {
    val context = Res.application ?: return ""
    return ContextCompat.getString(context, this)
}

/**
 * Holt ein Drawable sicher. Falls der Context fehlt, wird null zurückgegeben, anstatt eine NullPointerException zu
 * provozieren.
 */
inline fun Int.getDrawable(): Drawable? {
    val context = Res.application ?: return null
    return ContextCompat.getDrawable(context, this)
}
