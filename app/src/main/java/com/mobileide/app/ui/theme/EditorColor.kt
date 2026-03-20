package com.mobileide.app.ui.theme

import android.util.Log
import androidx.core.graphics.toColorInt
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.lang.reflect.Modifier

private const val TAG = "EditorColor"

/** Maps a single Sora [EditorColorScheme] constant to an ARGB int colour value. */
data class EditorColor(val key: Int, val color: Int)

/**
 * Cached reflection map: lowercase field name → EditorColorScheme int constant.
 * Built once on first access and never invalidated (field names are static).
 */
val EDITOR_COLOR_MAPPING: Map<String, Int> by lazy { buildEditorColorMapping() }

/** Parse a hex colour string to ARGB int, returning -1 on failure. */
private fun String.toColorIntSafe(): Int =
    runCatching {
        if (this == "0") return 0
        toColorInt()
    }.getOrElse { e ->
        Log.w(TAG, "Invalid editor colour '$this': ${e.message}")
        -1
    }

/**
 * Convert a raw JSON colour map (field name → #RRGGBB) to a list of [EditorColor]
 * entries that can be applied to a Sora [EditorColorScheme].
 *
 * Unknown keys and invalid colour strings are skipped with a WARN log entry.
 */
fun mapEditorColorScheme(rawScheme: Map<String, String>?): List<EditorColor> {
    if (rawScheme.isNullOrEmpty()) return emptyList()
    val result = mutableListOf<EditorColor>()
    rawScheme.forEach { (rawKey, hexColor) ->
        val key      = rawKey.lowercase().trim()
        val editorKey = EDITOR_COLOR_MAPPING[key]
        if (editorKey == null) { Log.w(TAG, "Unknown EditorColorScheme key: '$rawKey'"); return@forEach }
        val colorInt = hexColor.toColorIntSafe()
        if (colorInt != -1) result.add(EditorColor(editorKey, colorInt))
    }
    return result
}

/** Use reflection to build the lowercase field-name → int-value map from [EditorColorScheme]. */
fun buildEditorColorMapping(): Map<String, Int> =
    EditorColorScheme::class.java.declaredFields
        .filter { field ->
            Modifier.isPublic(field.modifiers) &&
            Modifier.isStatic(field.modifiers) &&
            Modifier.isFinal(field.modifiers) &&
            field.type == Int::class.javaPrimitiveType
        }
        .onEach { it.isAccessible = true }
        .associate { field -> field.name.lowercase() to (field.get(null) as Int) }

/** Apply this theme's editor-colour overrides to [scheme]. */
fun List<EditorColor>.applyTo(scheme: EditorColorScheme) {
    forEach { (key, color) -> scheme.setColor(key, color) }
}
