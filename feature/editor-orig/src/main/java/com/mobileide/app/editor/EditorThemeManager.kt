package com.mobileide.app.editor

import android.content.Context
import android.util.Log
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.mobileide.app.ui.theme.amoledM3Theme
import com.mobileide.app.ui.theme.currentM3Theme
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

private const val TAG = "EditorThemeManager"

// ── Selection colour (captured from Compose) ──────────────────────────────────
private var selectionColor: Color? = null

@Suppress("ComposableNaming")
@Composable
fun preloadSelectionColor() {
    selectionColor = LocalTextSelectionColors.current.backgroundColor
}
fun getSelectionColor(): Color? = selectionColor

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Builds and caches [TextMateColorScheme] instances for [Editor].
 *
 * **Three theme variants are available:**
 * - Light  → `textmate/quietlight.json`
 * - Dark   → `textmate/darcula.json`
 * - AMOLED → `textmate/black/darcula.json`
 *
 * User-installed Material3 themes can overlay additional token colours via
 * [currentM3Theme].dark/lightTokenColors.
 *
 * Call [invalidate] after any theme change to flush the cache.
 */
object EditorThemeManager {

    private val mutex = Mutex()
    private val schemeCache = hashMapOf<String, TextMateColorScheme>()

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun createColorScheme(context: Context, isDark: Boolean): TextMateColorScheme =
        mutex.withLock {
            val isAmoled = amoledM3Theme.value
            val cacheKey = buildEditorCacheKey(isDark, isAmoled)
            schemeCache[cacheKey]?.let { return@withLock it }

            val assetPath = when {
                isAmoled -> BASE_AMOLED_THEME
                isDark   -> BASE_DARK_THEME
                else     -> BASE_LIGHT_THEME
            }

            val model = buildThemeModel(context, assetPath, isDark)
            ThemeRegistry.getInstance().loadTheme(model)
            val scheme = TextMateColorScheme.create(model)
            schemeCache[cacheKey] = scheme
            Log.i(TAG, "Created scheme [isDark=$isDark amoled=$isAmoled]")
            scheme
        }

    fun createColorSchemeBlocking(context: Context, isDark: Boolean): TextMateColorScheme =
        runBlocking { createColorScheme(context, isDark) }

    fun invalidate() {
        schemeCache.clear()
        Log.i(TAG, "Cache invalidated")
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private suspend fun buildThemeModel(
        context: Context,
        assetPath: String,
        isDark: Boolean,
    ): ThemeModel = withContext(Dispatchers.IO) {
        context.assets.open(assetPath).use { stream ->
            InputStreamReader(stream).use { reader ->
                val jsonRoot  = JsonParser.parseReader(reader).asJsonObject
                val userTheme = currentM3Theme.value
                val tokens    = if (isDark) userTheme?.darkTokenColors else userTheme?.lightTokenColors

                val arrayKey = when {
                    jsonRoot.has("settings")    -> "settings"
                    jsonRoot.has("tokenColors") -> "tokenColors"
                    else                        -> null
                }

                userTheme?.let { jsonRoot.add("name", JsonPrimitive(it.name)) }

                if (tokens != null && !tokens.isEmpty) {
                    if (arrayKey != null) {
                        if (userTheme!!.inheritBase) {
                            jsonRoot[arrayKey].asJsonArray.addAll(tokens)
                        } else {
                            jsonRoot.remove(arrayKey)
                            jsonRoot.add(arrayKey, tokens)
                        }
                    } else {
                        jsonRoot.add("tokenColors", tokens)
                    }
                }

                val bytes    = jsonRoot.toString().toByteArray(Charsets.UTF_8)
                val baseName = assetPath.substringAfterLast('/')
                ThemeModel(IThemeSource.fromInputStream(ByteArrayInputStream(bytes), baseName, null))
            }
        }
    }
}
