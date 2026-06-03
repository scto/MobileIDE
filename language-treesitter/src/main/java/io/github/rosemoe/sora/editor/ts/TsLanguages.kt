package io.github.rosemoe.sora.editor.ts

import com.itsaky.androidide.treesitter.TSLanguage

/**
 * HTML 语言绑定
 */
class HtmlLanguage : TSLanguage("html", Companion.tree_sitter_html()) {
    companion object {
        init {
            System.loadLibrary("tree-sitter-html")
        }
        @JvmStatic external fun tree_sitter_html(): Long
    }
}

/**
 * CSS 语言绑定
 */
class CssLanguage : TSLanguage("css", Companion.tree_sitter_css()) {
    companion object {
        init {
            System.loadLibrary("tree-sitter-css")
        }
        @JvmStatic external fun tree_sitter_css(): Long
    }
}

/**
 * JavaScript 语言绑定
 */
class JavaScriptLanguage : TSLanguage("javascript", Companion.tree_sitter_javascript()) {
    companion object {
        init {
            System.loadLibrary("tree-sitter-javascript")
        }
        @JvmStatic external fun tree_sitter_javascript(): Long
    }
}

