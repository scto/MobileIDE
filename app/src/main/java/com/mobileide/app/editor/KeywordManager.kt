package com.mobileide.app.editor

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

private const val TAG = "KeywordManager"

/**
 * Loads `assets/textmate/keywords.json` once and provides per-scope keyword
 * lists for Sora's auto-complete engine.
 *
 * `keywords.json` format:
 * ```json
 * {
 *   "source.kotlin": ["fun", "val", "var", "class", "object", ...],
 *   "source.java":   ["public", "private", "class", "interface", ...]
 * }
 * ```
 *
 * If the file is absent (not yet bundled), [getKeywords] returns null and
 * auto-complete falls back to Sora's built-in completion.
 */
object KeywordManager {

    private val initDeferred = CompletableDeferred<Unit>()
    private var keywords: Map<String, List<String>> = emptyMap()

    /** Load keywords from assets. Call once from the app/init coroutine. */
    suspend fun initKeywordRegistry(context: Context) {
        if (initDeferred.isCompleted) return
        withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(TEXTMATE_PREFIX + KEYWORDS_FILE).use { stream ->
                    val type = object : TypeToken<Map<String, List<String>>>() {}
                    keywords = Gson().fromJson(InputStreamReader(stream), type)
                    Log.i(TAG, "Loaded keywords for ${keywords.size} scopes")
                }
            }.onFailure {
                // keywords.json is optional — missing file is not an error
                Log.w(TAG, "keywords.json not found, keyword completion disabled")
            }
            initDeferred.complete(Unit)
        }
    }

    /** Return the keyword list for [textmateScope], or null if unavailable. */
    suspend fun getKeywords(textmateScope: String): List<String>? {
        initDeferred.await()
        return keywords[textmateScope]
    }
}
