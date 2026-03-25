package com.mobileide.app.editor

import android.content.Context
import android.util.Log
import com.mobileide.app.data.Language
import com.mobileide.app.ui.theme.ActiveTheme
import com.mobileide.app.utils.TextMateSetup

private const val TAG = "CodeHighlighter"

/**
 * Registers a global Markdown code-block syntax highlighter.
 *
 * When Sora encounters a fenced code block in a Markdown file it calls the
 * registered factory with the language name (e.g. "kotlin", "python").
 * This factory maps the name to a TextMate scope, then returns a
 * pre-built language + colour-scheme pair.
 *
 * **Usage:** call [registerMarkdownCodeHighlighter] once, right after the
 * grammar registry has been initialised in [LanguageManager].
 *
 * Note: This depends on the `sora-language-textmate` artefact exposing
 * `MarkdownCodeHighlighterRegistry`.  If the version in use does not include
 * that API yet, the call is wrapped in a runCatching so the rest of the
 * editor still functions.
 */
object CodeHighlighter {

    fun registerMarkdownCodeHighlighter(context: Context) {
        runCatching {
            // Sora ≥ 0.23.x exposes this via the LSP module.
            // Reflection guard: only wire up if the class is present on the classpath.
            val registryClass = Class.forName(
                "io.github.rosemoe.sora.lsp.editor.text.MarkdownCodeHighlighterRegistry"
            )
            val withHighlighterMethod = registryClass.getDeclaredMethod(
                "withEditorHighlighter",
                Function1::class.java
            )
            val globalField = registryClass.getDeclaredField("global")
            globalField.isAccessible = true
            val registry = globalField.get(null)

            val isDark = ActiveTheme.get().background.red < 0.5f

            val factory = factory@{ languageName: String ->
                val scope = Language.fromExtension(languageName).toTextmateScope()
                    ?: return@factory null

                val language = LanguageManager.createLanguageBlocking(context, scope)
                val scheme   = EditorThemeManager.createColorSchemeBlocking(context, isDark)
                Pair(language, scheme)
            }

            withHighlighterMethod.invoke(registry, factory)
            Log.i(TAG, "Markdown code highlighter registered")
        }.onFailure {
            Log.w(TAG, "MarkdownCodeHighlighterRegistry not available (${it.message}); skipping")
        }
    }
}

/** Map a [Language] to its primary TextMate scope string via the shared [TextMateSetup.SCOPE_MAP]. */
private fun Language.toTextmateScope(): String? = TextMateSetup.SCOPE_MAP[this]
