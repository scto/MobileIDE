package com.mobileide.app.editor

import android.content.Context
import android.graphics.Typeface
import java.io.File

/**
 * Simple in-memory [Typeface] cache keyed by asset path or file path.
 * Avoids repeated disk reads when the editor is recreated across file switches.
 */
object FontCache {

    private val cache = mutableMapOf<String, Typeface>()

    /**
     * Pre-load a font into the cache without returning it.
     * Safe to call from a background thread.
     */
    fun loadFont(context: Context, path: String, isAsset: Boolean) {
        runCatching {
            val font = if (isAsset) {
                context.assets.open(path).close()          // verify it exists first
                Typeface.createFromAsset(context.assets, path)
            } else {
                val file = File(path)
                if (!file.exists()) return
                Typeface.createFromFile(file)
            }
            cache[path] = font
        }.onFailure { it.printStackTrace() }
    }

    /**
     * Return the [Typeface] for [path], loading it if not yet cached.
     * Returns null if the font cannot be loaded.
     */
    fun getFont(context: Context, path: String, isAsset: Boolean): Typeface? {
        cache[path]?.let { return it }
        return runCatching {
            val font = if (isAsset) {
                Typeface.createFromAsset(context.assets, path)
            } else {
                Typeface.createFromFile(File(path))
            }
            cache[path] = font
            font
        }.getOrNull()
    }

    /** Clear all cached fonts (e.g. after a font settings change). */
    fun clear() = cache.clear()
}
