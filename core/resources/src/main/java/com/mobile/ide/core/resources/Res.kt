package com.mobile.ide.core.resources

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.DelicateCoroutinesApi

typealias drawables = R.drawable

typealias strings = R.string

typealias plurals = R.plurals

@OptIn(DelicateCoroutinesApi::class)
object Res {
    @JvmField var application: Application? = null
}

/** Holt einen String sicher. Falls die Application nicht initialisiert wurde, wird ein leerer String zurückgegeben. */
inline fun Int.getString(): String {
    val context = Res.application ?: return ""
    return ContextCompat.getString(context, this)
}

/** Holt einen String und füllt Platzhalter. Falls die Application fehlt, wird ein leerer String zurückgegeben. */
inline fun Int.getFilledString(vararg args: Any?): String {
    val context = Res.application ?: return ""
    return ContextCompat.getString(context, this).fillPlaceholders(*args)
}

/** Hilfsfunktion zum Füllen von Platzhaltern in Strings. */
inline fun String.fillPlaceholders(vararg args: Any?): String {
    return try {
        String.format(this, *args)
    } catch (e: Exception) {
        this
    }
}

/** Holt ein Drawable sicher. Falls der Context fehlt, wird null zurückgegeben. */
inline fun Int.getDrawable(): Drawable? {
    val context = Res.application ?: return null
    return ContextCompat.getDrawable(context, this)
}

/** Holt einen Plural-String sicher. Falls der Context fehlt, wird ein leerer String zurückgegeben. */
inline fun Int.getQuantityString(quantity: Int, vararg formatArgs: Any?): String {
    val context = Res.application ?: return ""
    return context.resources.getQuantityString(this, quantity, *formatArgs)
}
