package com.mobileide.app.data

import java.io.File

data class Project(
    val name: String,
    val path: String,
    val type: ProjectType = ProjectType.KOTLIN_ANDROID,
    val createdAt: Long = System.currentTimeMillis()
) {
    val rootFile: File get() = File(path)
    val buildGradle: File get() = File(path, "app/build.gradle.kts")
    val srcDir: File get() = File(path, "app/src/main/java")
}

enum class ProjectType(val label: String) {
    KOTLIN_ANDROID("Android (Kotlin)"),
    JAVA_ANDROID("Android (Java)"),
    KOTLIN_PLAIN("Kotlin Script"),
    EMPTY("Empty")
}

data class EditorTab(
    val file: File,
    val content: String = "",
    val isModified: Boolean = false,
    val scrollOffset: Int = 0
) {
    val name: String get() = file.name
    val language: Language get() = Language.fromExtension(file.extension)
    val path: String get() = file.absolutePath
}

enum class Language(val ext: String, val label: String) {
    // Core Android / JVM
    KOTLIN      ("kt",    "Kotlin"),
    GRADLE      ("kts",   "Gradle KTS"),
    JAVA        ("java",  "Java"),
    XML         ("xml",   "XML"),
    // Data formats
    JSON        ("json",  "JSON"),
    YAML        ("yaml",  "YAML"),
    TOML        ("toml",  "TOML"),
    INI         ("ini",   "INI"),
    PROPERTIES  ("properties", "Properties"),
    // Web
    HTML        ("html",  "HTML"),
    HTMX        ("htmx",  "HTMX"),
    CSS         ("css",   "CSS"),
    SCSS        ("scss",  "SCSS"),
    LESS        ("less",  "Less"),
    JAVASCRIPT  ("js",    "JavaScript"),
    JSX         ("jsx",   "JSX"),
    TYPESCRIPT  ("ts",    "TypeScript"),
    TSX         ("tsx",   "TSX"),
    PHP         ("php",   "PHP"),
    // Systems
    C           ("c",     "C"),
    CPP         ("cpp",   "C++"),
    CSHARP      ("cs",    "C#"),
    RUST        ("rs",    "Rust"),
    GO          ("go",    "Go"),
    SWIFT       ("swift", "Swift"),
    // Scripting
    PYTHON      ("py",    "Python"),
    RUBY        ("rb",    "Ruby"),
    LUA         ("lua",   "Lua"),
    BASH        ("sh",    "Shell"),
    POWERSHELL  ("ps1",   "PowerShell"),
    BAT         ("bat",   "Batch"),
    // Markup / Docs
    MARKDOWN    ("md",    "Markdown"),
    LATEX       ("tex",   "LaTeX"),
    DIFF        ("diff",  "Diff"),
    LOG         ("log",   "Log"),
    // Niche / Special
    DART        ("dart",  "Dart"),
    SQL         ("sql",   "SQL"),
    GROOVY      ("groovy","Groovy"),
    SMALI       ("smali", "Smali"),
    ASM         ("asm",   "Assembly"),
    ZIG         ("zig",   "Zig"),
    NIM         ("nim",   "Nim"),
    PASCAL      ("pas",   "Pascal"),
    LISP        ("lisp",  "Lisp"),
    CMAKE       ("cmake", "CMake"),
    COQ         ("v",     "Coq"),
    IGNORE      ("gitignore", "Ignore"),
    // Fallback
    PLAIN       ("txt",   "Plain Text");

    companion object {
        fun fromExtension(ext: String): Language = when (ext.lowercase()) {
            "kt"                             -> KOTLIN
            "kts", "gradle"                  -> GRADLE
            "java"                           -> JAVA
            "xml"                            -> XML
            "json", "jsonc"                  -> JSON
            "yaml", "yml"                    -> YAML
            "toml"                           -> TOML
            "ini"                            -> INI
            "properties"                     -> PROPERTIES
            "html", "htm", "xhtml"           -> HTML
            "htmx"                           -> HTMX
            "css"                            -> CSS
            "scss", "sass"                   -> SCSS
            "less"                           -> LESS
            "js", "mjs", "cjs"               -> JAVASCRIPT
            "jsx"                            -> JSX
            "ts", "mts", "cts"               -> TYPESCRIPT
            "tsx"                            -> TSX
            "php"                            -> PHP
            "c", "h"                         -> C
            "cpp", "cc", "cxx", "hpp", "hxx" -> CPP
            "cs"                             -> CSHARP
            "rs"                             -> RUST
            "go"                             -> GO
            "swift"                          -> SWIFT
            "py", "pyw"                      -> PYTHON
            "rb"                             -> RUBY
            "lua"                            -> LUA
            "sh", "bash", "zsh", "fish"      -> BASH
            "ps1", "psm1"                    -> POWERSHELL
            "bat", "cmd"                     -> BAT
            "md", "markdown"                 -> MARKDOWN
            "tex", "sty"                     -> LATEX
            "diff", "patch"                  -> DIFF
            "log"                            -> LOG
            "dart"                           -> DART
            "sql"                            -> SQL
            "groovy", "gvy", "gy"            -> GROOVY
            "smali"                          -> SMALI
            "asm", "s"                       -> ASM
            "zig"                            -> ZIG
            "nim"                            -> NIM
            "pas", "pp"                      -> PASCAL
            "lisp", "cl", "el"               -> LISP
            "cmake"                          -> CMAKE
            "v"                              -> COQ
            "gitignore", "ignore",
            "dockerignore", "npmignore"      -> IGNORE
            else                             -> PLAIN
        }
    }
}

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LineType { INPUT, OUTPUT, ERROR, SUCCESS, INFO }

data class BuildResult(
    val success: Boolean,
    val output: String,
    val errors: List<BuildError> = emptyList(),
    val duration: Long = 0L
)

data class BuildError(
    val file: String,
    val line: Int,
    val column: Int,
    val message: String,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

enum class ErrorSeverity { ERROR, WARNING, INFO }

data class FileNode(
    val file: File,
    val depth: Int = 0,
    val isExpanded: Boolean = false
) {
    val isDirectory: Boolean get() = file.isDirectory
    val name: String         get() = file.name
}

// Code outline symbol
data class OutlineSymbol(
    val name: String,
    val kind: SymbolKind,
    val line: Int,
    val detail: String = ""
)

enum class SymbolKind(val icon: String) {
    CLASS    ("class"),
    FUNCTION ("fun"),
    PROPERTY ("val"),
    VARIABLE ("var"),
    OBJECT   ("object"),
    INTERFACE("interface"),
    ENUM     ("enum"),
    COMPANION("companion")
}

// TODO item
data class TodoItem(
    val file: File,
    val line: Int,
    val text: String,
    val tag: TodoTag
)

enum class TodoTag(val label: String) {
    TODO ("TODO"),
    FIXME("FIXME"),
    HACK ("HACK"),
    NOTE ("NOTE"),
    BUG  ("BUG"),
    WARN ("WARN")
}

// Termux package
data class TermuxPackage(
    val name: String,
    val description: String,
    val version: String = "",
    val isInstalled: Boolean = false,
    val size: String = ""
)

// ── Project panel models ───────────────────────────────────────────────────────

enum class PanelTab { PROJECT, ANDROID, DATA }

enum class ExplorerViewMode(val label: String) {
    TREE_COMPONENT ("Tree Component"),
    PACKAGE        ("Package"),
    EXPLORER       ("Explorer"),
    MANAGE         ("Manage")
}

data class AndroidModule(
    val name: String,          // e.g. ":app"
    val path: String,
    val dependencies: List<String> = emptyList(),
    val isLibrary: Boolean = false
)

data class ProjectManifestInfo(
    val activities:  List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val services:    List<String> = emptyList(),
    val receivers:   List<String> = emptyList(),
    val providers:   List<String> = emptyList()
)

data class StringResource(val name: String, val value: String)
