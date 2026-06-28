package io.github.rosemoe.sora.editor.ts

import com.tom.rv2ide.treesitter.TSLanguage

/**
 * HTML language binding (custom native lib in jniLibs)
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
 * CSS language binding (custom native lib in jniLibs)
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
 * JavaScript language binding (custom native lib in jniLibs)
 */
class JavaScriptLanguage : TSLanguage("javascript", Companion.tree_sitter_javascript()) {
    companion object {
        init {
            System.loadLibrary("tree-sitter-javascript")
        }
        @JvmStatic external fun tree_sitter_javascript(): Long
    }
}
