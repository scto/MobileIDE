<div align="center">

# MobileIDE

**A full-featured Android IDE that runs entirely on your Android device**

[![Android](https://img.shields.io/badge/Platform-Android%2026%2B-brightgreen?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.05-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Sora Editor](https://img.shields.io/badge/Sora%20Editor-0.23.4-orange)](https://github.com/Rosemoe/sora-editor)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

</div>

---

## What is MobileIDE?

MobileIDE is a fully self-contained Android development environment that runs directly on your phone or tablet — no PC required. It pairs the GPU-accelerated [Sora Editor](https://github.com/Rosemoe/sora-editor) with a TextMate grammar engine covering **47 programming languages**, a complete Material3 theme system, Termux integration for building and running code, and full Git support — all wrapped in a modern Jetpack Compose UI.

---

## Screenshots

> Coming soon

---

## Features

### Editor
- **Sora Editor** — GPU-accelerated code editor with hardware rendering
- **47 Language Grammars** — Kotlin, Java, C/C++, Rust, Go, Python, TypeScript, PHP, Dart, SQL and many more via TextMate
- **3 Editor Themes** — Darcula (dark), Quiet Light (light), AMOLED Darcula (pure black)
- **4 Fonts** — JetBrains Mono, Fira Code, Roboto, Default
- **Smart Editing** — Auto-close HTML/HTMX tags, Markdown bullet/list continuation
- **Keyword Autocomplete** — language-aware completion for all bundled grammars
- **Split Editor** — two files side by side
- **Search & Replace** — with regex support

### Theme System
- **Material3** — full light/dark/AMOLED support
- **Material You** — dynamic wallpaper colours on Android 12+
- **Installable Themes** — install custom `.json` theme files that override both the app UI and editor token colours simultaneously
- **Medium / High Contrast** — theme overlays for accessibility
- **2 Built-in Themes** — Blueberry and Lime

### Project & File Management
- Create projects from 8 built-in templates (Compose, XML, Empty, …)
- File tree with expand/collapse, rename, delete, copy path
- Breadcrumb navigation
- Project-wide search with regex
- Code outline (classes, functions, properties)
- TODO/FIXME/HACK scanner

### Git Integration
- Clone, push, pull, commit, branch, checkout
- Built-in Diff Viewer
- Git status colours in the file tree

### Build & Run
- Gradle task runner
- `./gradlew assembleDebug` via Termux with live output streaming
- Build error panel with clickable locations
- One-tap APK install via ADB
- Run configurations

### Tools
- LogCat viewer
- Structured in-app logger
- Termux package manager
- Project statistics (lines of code, file types)
- Snippet manager
- Setup wizard for first-time Termux environment configuration
- LSP server installation scripts (bash, CSS, HTML, JSON, TypeScript, Python, …)

---

## Getting Started

### Requirements

| Requirement | Version |
|---|---|
| Android | 8.0 (API 26) or higher |
| [Termux](https://f-droid.org/packages/com.termux/) | Latest (from F-Droid) |
| Storage permission | `MANAGE_EXTERNAL_STORAGE` |

### Build from Source

```bash
# 1. Clone the repository
git clone https://github.com/your-org/MobileIDE.git
cd MobileIDE

# 2. Build from the command line
./gradlew assembleDebug

# 3. Install on a connected device
./gradlew installDebug
```

> **Note:** The project targets `compileSdk 36`. Android Studio Ladybug or newer is recommended.

### First Launch

1. Grant the required permissions on the onboarding screen.
2. The **Setup Wizard** will install Java, Gradle and Git into Termux.
3. Create a project from `Home → +`.

---

## Project Structure

```
MobileIDE/
├── app/
│   ├── dokka/
│   │   └── module.md                     # Dokka module documentation entry page
│   └── src/main/
│       ├── assets/
│       │   ├── fonts/                    # JetBrains Mono, Fira Code, Roboto, Default
│       │   ├── terminal/                 # Termux shell scripts + LSP server installers
│       │   └── textmate/
│       │       ├── [45 language dirs]/   # Grammar + language-configuration per language
│       │       ├── darcula.json          # Dark base theme
│       │       ├── quietlight.json       # Light base theme
│       │       ├── black/darcula.json    # AMOLED base theme
│       │       ├── languages.json        # Sora grammar registry
│       │       └── keywords.json         # Autocomplete keywords per scope
│       ├── java/com/mobileide/app/
│       │   ├── AppConstants.kt
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   └── Models.kt             # Language (47), Project, EditorTab, …
│       │   ├── editor/
│       │   │   ├── Editor.kt             # CodeEditor subclass
│       │   │   ├── EditorThemeManager.kt # Builds + caches TextMateColorScheme
│       │   │   ├── FontCache.kt
│       │   │   ├── KeywordManager.kt
│       │   │   ├── LanguageManager.kt
│       │   │   ├── LineEnding.kt
│       │   │   ├── TextMateConstants.kt
│       │   │   └── intelligent/
│       │   │       ├── AutoCloseTag.kt
│       │   │       ├── BulletContinuation.kt
│       │   │       └── IntelligentFeature.kt
│       │   ├── logger/
│       │   ├── ui/
│       │   │   ├── components/           # 12 reusable Compose components
│       │   │   ├── screens/              # 20 application screens
│       │   │   └── theme/
│       │   │       ├── Theme.kt          # MobileIdeTheme + semantic extensions
│       │   │       ├── ThemeHolder.kt
│       │   │       ├── ThemeConfig.kt
│       │   │       ├── ThemeLoader.kt
│       │   │       ├── PreBuiltThemes.kt # Blueberry + Lime
│       │   │       ├── ThemePreferences.kt
│       │   │       └── EditorColor.kt
│       │   ├── utils/
│       │   │   ├── TextMateSetup.kt      # Public facade + SCOPE_MAP (47 entries)
│       │   │   ├── WorkspaceManager.kt   # DataStore persistence
│       │   │   ├── GitManager.kt
│       │   │   └── ProjectManager.kt
│       │   └── viewmodel/
│       │       └── IDEViewModel.kt       # Central ViewModel
│       └── res/
│           ├── values/
│           │   ├── colors.xml            # Full M3 Blueberry palette (3 contrast levels)
│           │   ├── themes.xml            # Base, Oled, Monet, Splash styles
│           │   └── theme_overlays.xml    # Medium + High Contrast overlays
│           └── xml/
│               ├── network_security_config.xml
│               ├── data_extraction_rules.xml
│               └── backup_rules.xml
├── gradle/
│   └── libs.versions.toml                # Version catalog
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── CHANGES.md                            # Detailed change log
```

---

## Architecture

MobileIDE follows a single-module MVVM architecture:

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│     Screens (Compose)    Components (Compose)    │
└──────────────────────────┬──────────────────────┘
                           │ StateFlow / collectAsState
┌──────────────────────────▼──────────────────────┐
│                ViewModel Layer                   │
│              IDEViewModel (SSOT)                 │
└──────┬────────────┬──────────────┬──────────────┘
       │            │              │
┌──────▼──────┐ ┌───▼──────┐ ┌────▼──────────────┐
│  Editor     │ │  Theme   │ │  Utils / Storage   │
│  Engine     │ │  System  │ │  WorkspaceManager  │
│  Sora +     │ │  M3 +    │ │  GitManager        │
│  TextMate   │ │  TextMate│ │  ProjectManager    │
└─────────────┘ └──────────┘ └───────────────────┘
```

### State Management

All UI state lives in `IDEViewModel` as `StateFlow`s. Screens collect these with
`collectAsState()` — there are no side-channel state objects outside the ViewModel.

### Theme Flow

```
ThemePreferences (SharedPreferences)
        │  read on first composition (synchronous)
        ▼
  currentM3Theme  ◄─── ThemeLoader (JSON install)
        │
        ├──► MobileIdeTheme → MaterialTheme (Compose UI colours)
        │
        └──► EditorThemeManager
                │  merges token colours into base TextMate JSON
                ▼
           TextMateColorScheme → Sora Editor
```

---

## Supported Languages

<details>
<summary>All 47 supported languages</summary>

| Language | Extension(s) | TextMate Scope |
|---|---|---|
| Kotlin | `.kt`, `.kts` | `source.kotlin` |
| Java | `.java` | `source.java` |
| XML | `.xml` | `text.xml` |
| JSON | `.json`, `.jsonc` | `source.json` |
| YAML | `.yaml`, `.yml` | `source.yaml` |
| TOML | `.toml` | `source.toml` |
| INI | `.ini` | `source.ini` |
| Properties | `.properties` | `source.properties` |
| HTML | `.html`, `.htm` | `text.html.basic` |
| HTMX | `.htmx` | `text.html.htmx` |
| CSS | `.css` | `source.css` |
| SCSS | `.scss`, `.sass` | `source.css.scss` |
| Less | `.less` | `source.css.less` |
| JavaScript | `.js`, `.mjs` | `source.js` |
| JSX | `.jsx` | `source.js.jsx` |
| TypeScript | `.ts`, `.mts` | `source.ts` |
| TSX | `.tsx` | `source.tsx` |
| PHP | `.php` | `text.html.php` |
| C | `.c`, `.h` | `source.c` |
| C++ | `.cpp`, `.cc`, `.hpp` | `source.cpp` |
| C# | `.cs` | `source.cs` |
| Rust | `.rs` | `source.rust` |
| Go | `.go` | `source.go` |
| Swift | `.swift` | `source.swift` |
| Python | `.py`, `.pyw` | `source.python` |
| Ruby | `.rb` | `source.ruby` |
| Lua | `.lua` | `source.lua` |
| Shell | `.sh`, `.bash`, `.zsh` | `source.shell` |
| PowerShell | `.ps1`, `.psm1` | `source.powershell` |
| Batch | `.bat`, `.cmd` | `source.batchfile` |
| Markdown | `.md`, `.markdown` | `text.html.markdown` |
| LaTeX | `.tex`, `.sty` | `text.tex.latex` |
| Diff | `.diff`, `.patch` | `source.diff` |
| Log | `.log` | `text.log` |
| Dart | `.dart` | `source.dart` |
| SQL | `.sql` | `source.sql` |
| Groovy | `.groovy`, `.gvy` | `source.groovy` |
| Smali | `.smali` | `source.smali` |
| Assembly | `.asm`, `.s` | `source.asm` |
| Zig | `.zig` | `source.zig` |
| Nim | `.nim` | `source.nim` |
| Pascal | `.pas`, `.pp` | `source.pascal` |
| Lisp | `.lisp`, `.cl`, `.el` | `source.lisp` |
| CMake | `.cmake` | `source.cmake` |
| Coq | `.v` | `source.coq` |
| Ignore files | `.gitignore`, … | `source.ignore` |
| Plain Text | `.txt` | `text.plain` |

</details>

---

## Custom Theme Format

```json
{
  "id": "my-dark-theme",
  "name": "My Dark Theme",
  "targetVersion": 1,
  "inheritBase": true,
  "dark": {
    "baseColors": {
      "primary":    "#89B4FA",
      "background": "#1E1E2E",
      "surface":    "#181825",
      "onSurface":  "#CDD6F4"
    },
    "tokenColors": [
      { "scope": "keyword", "settings": { "foreground": "#CBA6F7" } },
      { "scope": "string",  "settings": { "foreground": "#A6E3A1" } },
      { "scope": "comment", "settings": { "foreground": "#6C7086" } }
    ]
  },
  "light": {
    "baseColors": { "primary": "#0969DA" }
  }
}
```

`inheritBase: true` — missing colour slots fall back to the built-in **Blueberry** theme.  
`inheritBase: false` — only the explicitly defined colours are used.

---

## Documentation

API documentation is generated with [Dokka 2.1.0](https://kotlin.github.io/dokka/) and the
[html-mermaid-dokka-plugin 0.6.0](https://github.com/glureau/html-mermaid-dokka-plugin),
which renders ` ```mermaid ` blocks inside KDoc comments as inline SVG diagrams.

### Generate Docs

```bash
# HTML site  →  app/build/dokka/html/index.html
./gradlew :app:dokkaHtml

# GitHub-Flavoured Markdown  →  app/build/dokka/gfm/
./gradlew :app:dokkaGfm
```

### KDoc with Mermaid

```kotlin
/**
 * Loads the active TextMate colour scheme for the given dark-mode state.
 *
 * ```mermaid
 * sequenceDiagram
 *     participant C as Caller
 *     participant E as EditorThemeManager
 *     participant A as Assets
 *     C->>E: createColorScheme(isDark)
 *     E->>A: open darcula.json
 *     A-->>E: InputStream
 *     E->>E: merge user token colours
 *     E-->>C: TextMateColorScheme
 * ```
 *
 * @param context Android context used to open asset files.
 * @param isDark  `true` for dark / AMOLED mode.
 * @return A ready-to-use [TextMateColorScheme].
 */
suspend fun createColorScheme(context: Context, isDark: Boolean): TextMateColorScheme
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2.0 |
| UI | Jetpack Compose BOM 2025.05 · Material3 |
| Architecture | MVVM · AndroidViewModel · StateFlow |
| Editor | Sora Editor 0.23.4 |
| Syntax Highlighting | TextMate grammars (Eclipse TM4E) |
| Storage | DataStore Preferences 1.1.4 |
| Async | Kotlin Coroutines 1.10.2 |
| Image loading | Coil 2.7.0 |
| JSON | Gson 2.11.0 |
| Material colours | Material Components 1.12.0 |
| Documentation | Dokka 2.1.0 · html-mermaid-dokka-plugin 0.6.0 |
| Build | Gradle Version Catalog · AGP 8.11.1 |
| Min SDK | 26 (Android 8.0 Oreo) |
| Compile SDK | 36 |

---

## Contributing

Contributions are welcome. Please open an issue first to discuss what you would like to change.

```bash
# Build
./gradlew assembleDebug

# Generate documentation before submitting a PR
./gradlew :app:dokkaHtml
```

---

## License

```
MIT License — Copyright (c) 2025 MobileIDE Contributors
```

---

<div align="center">
Built with ❤️ in Kotlin · Powered by <a href="https://github.com/Rosemoe/sora-editor">Sora Editor</a>
</div>
