# Module MobileIDE

A full-featured Android IDE running entirely on-device via [AndroidIDE](https://androidide.com)
and [Termux](https://termux.dev). Written in Kotlin with Jetpack Compose, powered by
[Sora Editor](https://github.com/Rosemoe/sora-editor) for syntax highlighting and editing.

## Architecture

The application is structured in five layers:

```mermaid
graph TD
    UI["UI Layer<br/>Screens · Components"]
    VM["ViewModel Layer<br/>IDEViewModel · StateFlows"]
    EDITOR["Editor Engine<br/>Editor · ThemeManager · LanguageManager"]
    THEME["Theme System<br/>MobileIdeTheme · ThemeHolder · ThemeLoader"]
    DATA["Data / Utils<br/>WorkspaceManager · GitManager · ProjectManager"]

    UI --> VM
    VM --> EDITOR
    VM --> THEME
    VM --> DATA
    EDITOR --> THEME
```

## Package Overview

| Package | Responsibility |
|---|---|
| `com.mobile.ide` | `MainActivity`, `App`, `MyApplication` |
| `com.mobile.ide.html` | Domain models: `Project`, `Language`, `EditorTab` |
| `com.mobile.ide.textmate` | `Editor` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |

| `com.mobile.ide.ui.editor` | `Editor` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.ui.preview` | `Preview` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.ui.projects` | `Projects` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.ui.settings` | `Settings` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.ui.welcome` | `Welcome` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |

| `com.mobile.ide.core.build` | `Build` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.core.files` | `Files` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.core.projexrs` | `Projects` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.core.resources` | `Resources` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.core.ui.components` | `Ui` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |
| `com.mobile.ide.core.ui.theme` | `Projects` subclass, `EditorThemeManager`, `LanguageManager`, `KeywordManager`, `FontCache` |



| `com.mobile.ide.core` | Smart editing: `AutoCloseTag`, `BulletContinuation` |
| `com.mobileide.app.logger` | Structured in-app logger |
| `com.mobileide.app.ui.components` | Reusable Compose components |
| `com.mobileide.app.ui.screens` | All application screens |
| `com.mobileide.app.ui.theme` | Material3 theme system, `ThemeHolder`, `ThemeLoader` |
| `com.mobileide.app.utils` | `TextMateSetup`, `WorkspaceManager`, `GitManager`, `ProjectManager` |
| `com.mobileide.app.viewmodel` | `IDEViewModel`, `Screen` enum |

## Generating the Documentation

```bash
# Full HTML site (recommended)
./gradlew :app:dokkaHtml

# GitHub-Flavoured Markdown (for wikis)
./gradlew :app:dokkaGfm
```

Output location: `app/build/dokka/`
