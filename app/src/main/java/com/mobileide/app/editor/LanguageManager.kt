package com.mobileide.app.editor

import android.content.Context
import android.util.Log
import com.mobileide.app.ui.theme.amoledM3Theme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TAG = "LanguageManager"

object LanguageManager {

    private val grammarRegistryInitialized = CompletableDeferred<Unit>()
    private val languageCache = hashMapOf<String, TextMateLanguage>()

    suspend fun initGrammarRegistry(context: Context) {
        if (grammarRegistryInitialized.isCompleted) return
        withContext(Dispatchers.IO) {
            try {
                FileProviderRegistry.getInstance()
                    .addFileProvider(AssetsFileResolver(context.applicationContext.assets))
                GrammarRegistry.getInstance()
                    .loadGrammars(TEXTMATE_PREFIX + LANGUAGES_FILE)
                Log.i(TAG, "Grammar registry ready")
            } catch (e: Exception) {
                Log.e(TAG, "Grammar init failed: ${e.message}")
            } finally {
                grammarRegistryInitialized.complete(Unit)
            }
        }
    }

    suspend fun createLanguage(context: Context, textmateScope: String): TextMateLanguage {
        grammarRegistryInitialized.await()
        // Key: scope + amoled flag (amoled uses different base theme → different token colours)
        val key = "${textmateScope}_${amoledM3Theme.value}"
        return languageCache.getOrPut(key) {
            TextMateLanguage.create(textmateScope, true)
        }
    }

    fun createLanguageBlocking(context: Context, textmateScope: String): TextMateLanguage =
        runBlocking { createLanguage(context, textmateScope) }

    fun invalidateCache() = languageCache.clear()
}
