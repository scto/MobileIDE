package com.mobileide.feature.editor

import androidx.compose.runtime.Stable

/** A file open in the feature editor. */
@Stable
data class FeatureEditorTab(
    val id: String,
    val fileName: String,
    val filePath: String?,
    val content: String = "",
    val language: FeatureEditorLanguage = FeatureEditorLanguage.PLAIN_TEXT,
    val isModified: Boolean = false,
    val cursorLine: Int = 0,
    val cursorColumn: Int = 0,
    val encoding: String = "UTF-8",
)

enum class FeatureEditorLanguage(
    val displayName: String,
    val fileExtensions: List<String>,
    val textmateScope: String?,
) {
    PLAIN_TEXT  ("Plain Text",  listOf("txt","text"),               null),
    KOTLIN      ("Kotlin",      listOf("kt","kts"),                 "source.kotlin"),
    JAVA        ("Java",        listOf("java"),                     "source.java"),
    PYTHON      ("Python",      listOf("py","pyw"),                 "source.python"),
    JAVASCRIPT  ("JavaScript",  listOf("js","mjs","cjs"),           "source.js"),
    TYPESCRIPT  ("TypeScript",  listOf("ts","tsx"),                 "source.ts"),
    HTML        ("HTML",        listOf("html","htm"),               "text.html.basic"),
    CSS         ("CSS",         listOf("css","scss","sass","less"), "source.css"),
    XML         ("XML",         listOf("xml","xsd","xsl"),          "text.xml"),
    JSON        ("JSON",        listOf("json","jsonc"),             "source.json"),
    MARKDOWN    ("Markdown",    listOf("md","markdown"),            "text.html.markdown"),
    BASH        ("Bash/Shell",  listOf("sh","bash","zsh","fish"),   "source.shell"),
    C           ("C",           listOf("c","h"),                    "source.c"),
    CPP         ("C++",         listOf("cpp","cc","cxx","hpp"),     "source.cpp"),
    CSHARP      ("C#",          listOf("cs"),                       "source.cs"),
    GO          ("Go",          listOf("go"),                       "source.go"),
    RUST        ("Rust",        listOf("rs"),                       "source.rust"),
    RUBY        ("Ruby",        listOf("rb","rake"),                "source.ruby"),
    PHP         ("PHP",         listOf("php","phtml"),              "source.php"),
    SWIFT       ("Swift",       listOf("swift"),                    "source.swift"),
    SQL         ("SQL",         listOf("sql"),                      "source.sql"),
    YAML        ("YAML",        listOf("yaml","yml"),               "source.yaml"),
    TOML        ("TOML",        listOf("toml"),                     "source.toml"),
    LUA         ("Lua",         listOf("lua"),                      "source.lua"),
    DART        ("Dart",        listOf("dart"),                     "source.dart"),
    GROOVY      ("Groovy",      listOf("groovy","gradle"),          "source.groovy"),
    PROPERTIES  ("Properties",  listOf("properties","env"),         "source.properties");

    companion object {
        fun fromFileName(name: String): FeatureEditorLanguage {
            val lower = name.lowercase()
            return when {
                lower == "dockerfile"           -> BASH
                lower.startsWith("makefile")    -> BASH
                lower == ".env" || lower.endsWith(".env") -> PROPERTIES
                else -> {
                    val ext = name.substringAfterLast('.', "")
                    values().firstOrNull { l -> l.fileExtensions.any { it == ext } }
                        ?: PLAIN_TEXT
                }
            }
        }
    }
}

/** Editor preference bundle for the feature editor. */
@Stable
data class FeatureEditorSettings(
    val fontSize: Float           = 14f,
    val fontPath: String          = "fonts/JetBrainsMono-Regular.ttf",
    val tabSize: Int              = 4,
    val wordWrap: Boolean         = false,
    val showLineNumbers: Boolean  = true,
    val highlightCurrentLine: Boolean = true,
    val autoComplete: Boolean     = true,
    val bracketAutoClose: Boolean = true,
    val autoIndent: Boolean       = true,
    val stickyScroll: Boolean     = false,
    val showWhitespace: Boolean   = false,
    val cursorAnimation: Boolean  = true,
    val lineSpacing: Float        = 1.2f,
    val formatOnSave: Boolean     = false,
    val trimTrailingWhitespace: Boolean = false,
    val insertFinalNewline: Boolean = true,
)

data class FeatureSearchOptions(
    val query: String = "",
    val replacement: String = "",
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val useRegex: Boolean = false,
    val wrapAround: Boolean = true,
)

data class FeatureSearchResult(
    val line: Int,
    val column: Int,
    val length: Int,
    val preview: String,
)

data class FeatureBookmark(
    val tabId: String,
    val line: Int,
    val label: String = "",
)
