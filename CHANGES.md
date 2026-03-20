# MobileIDE v5 — Änderungsprotokoll

> **Paket:** `com.mobileide.app`
> **compileSdk:** 36 · **minSdk:** 26 · **Kotlin:** 2.2.0 · **AGP:** 8.11.1
> **Sora Editor:** 0.23.4 · **Compose BOM:** 2025.05.01

---

## Übersicht

Dieses Dokument beschreibt alle Änderungen, die im Rahmen der Integration des
Material3-Themsystems, der neuen Editor-Engine und des erweiterten Asset-Pakets
in das MobileIDE-Projekt vorgenommen wurden. Die Änderungen sind nach
Themenbereich gegliedert.

---

## 1. Material3-Themsystem (`ui/theme/`)

Das ursprüngliche hardcodierte Catppuccin-Farbsystem wurde durch ein vollständiges,
austauschbares Material3-Themsystem ersetzt. Alle neuen Dateien liegen im Paket
`com.mobileide.app.ui.theme`.

### 1.1 Neue Dateien

| Datei | Beschreibung |
|---|---|
| `ThemePreferences.kt` | Synchroner SharedPreferences-Wrapper für `monet`, `amoled` und `themeId`. Wird in `MainActivity.onCreate()` initialisiert, bevor `setContent {}` aufgerufen wird, damit der erste Compose-Frame die Werte ohne Coroutine lesen kann. |
| `ThemeHolder.kt` | Datenklasse, die ein vollständig aufgelöstes Theme enthält: Material3 `ColorScheme` (light + dark), Sora-Editor-Farben, TextMate-Token-Farben und Terminal-Farben (vorbereitet für spätere Terminal-Unterstützung). |
| `ThemeConfig.kt` | JSON-deserialisierbare Konfiguration (`BaseColors`, `ThemePalette`, `ThemeConfig`). Unterstützt einfaches Map-Format und TextMate-Array-Format für `tokenColors`. |
| `EditorColor.kt` | Bildet JSON-Schlüssel per Reflection auf `EditorColorScheme`-Konstanten ab. Enthält `mapEditorColorScheme()` und `applyTo()`. |
| `PreBuiltThemes.kt` | Zwei eingebaute Themes: **Blueberry** (Standard, blau) und **Lime** (grün). Blueberry dient als Fallback für alle fehlenden Farbwerte in nutzerinitialisierten Themes. |
| `ThemeLoader.kt` | Installiert, speichert und lädt JSON-Theme-Dateien. Enthält `installM3ThemeFromFile()`, `updateM3Themes()`, `ThemeConfig.build()` und `ThemePalette.buildColorScheme()`. |
| `ThemePreferences.kt` | Synchroner Lese-/Schreibzugriff auf Theme-Einstellungen via SharedPreferences. |

### 1.2 Geänderte Dateien

**`Theme.kt`** — komplett neu geschrieben:
- Behält alle bisherigen `IDEBackground`, `IDEPrimary`, `SyntaxKeyword`, … Konstanten für Legacy-Screens.
- Fügt globale Zustände `currentM3Theme`, `dynamicM3Theme`, `amoledM3Theme` (alle `MutableState`) hinzu.
- Neue Composable `MobileIdeTheme` mit Unterstützung für Material You (API 31+), AMOLED-Modus und statische ThemeHolder-Schemata.
- `MobileIDETheme` bleibt als `@Deprecated`-Alias erhalten, damit bestehende Aufrufstellen kompilieren.
- `LocalThemeHolder` — `CompositionLocal` für Downstream-Zugriff auf Token- und Editor-Farben.
- Neue `ColorScheme`-Erweiterungen: `warningSurface`, `onWarningSurface`, `folderSurface`, `gitAdded`, `gitModified`, `gitDeleted`, `gitConflicted`.

**`ThemeManager.kt`** — `IDEColorPalette`, `Themes`-Objekt und `ActiveTheme`-Singleton unverändert erhalten.

### 1.3 Theme-JSON-Format

Ein nutzerdefiniertes Theme ist eine `.json`-Datei, die per `installM3ThemeFromFile()` installiert wird:

```json
{
  "id": "my-theme",
  "name": "My Theme",
  "targetVersion": 1,
  "inheritBase": true,
  "dark": {
    "baseColors": {
      "primary": "#89B4FA",
      "background": "#1E1E2E",
      "surface": "#181825"
    },
    "tokenColors": [
      { "scope": "keyword", "settings": { "foreground": "#CBA6F7" } }
    ]
  },
  "light": {
    "baseColors": { "primary": "#0969DA" }
  }
}
```

`inheritBase: true` → fehlende Farbwerte fallen auf Blueberry zurück.
`inheritBase: false` → nur die definierten Farben werden verwendet.

---

## 2. Neue Editor-Engine (`editor/`)

Das komplette Paket `com.mobileide.app.editor` wurde neu angelegt. Es ersetzt die
bisherigen lose gekoppelten Utilities in `utils/TextMateSetup.kt`.

### 2.1 Neue Dateien — Kern

| Datei | Beschreibung |
|---|---|
| `TextMateConstants.kt` | Asset-Pfade (`textmate/`, `textmate/black/`), Theme-Dateinamen (`darcula.json`, `quietlight.json`), `AVAILABLE_FONTS`-Liste, `DEFAULT_FONT_PATH`, Cache-Key-Funktion `buildEditorCacheKey()`. |
| `FontCache.kt` | In-Memory-`Typeface`-Cache. `loadFont()` für Vorabladen, `getFont()` für bedarfsgesteuerten Zugriff, `clear()` nach Font-Einstellungsänderung. |
| `LineEnding.kt` | Enum `LineEnding(LF, CRLF, CR)` mit Mapping auf Sora `LineSeparator`. |
| `LanguageManager.kt` | Verwaltet die TextMate-Grammatik-Registry (idempotent). `initGrammarRegistry()`, `createLanguage()` mit Scope-Cache, `invalidateCache()`. Cache-Key enthält AMOLED-Flag, damit Farbwechsel die Instanzen invalidiert. |
| `KeywordManager.kt` | Lädt `assets/textmate/keywords.json` einmalig per Coroutine. `getKeywords(scope)` liefert die Keyword-Liste für Soras Auto-Complete. Datei ist optional — fehlt sie, wird still degradiert. |
| `EditorThemeManager.kt` | **Zentrales Stück.** Liest Basis-Theme-JSON aus Assets, merged `currentM3Theme.value.dark/lightTokenColors` hinein (append bei `inheritBase=true`, replace bei `false`), erzeugt `TextMateColorScheme` und cached das Ergebnis. Drei Varianten: light (`quietlight.json`), dark (`darcula.json`), AMOLED (`black/darcula.json`). `invalidate()` leert den Cache. |
| `CodeHighlighter.kt` | Registriert einen globalen Markdown-Codeblock-Highlighter über Reflection-Guard (arbeitet ohne Fehler, wenn `MarkdownCodeHighlighterRegistry` nicht im Classpath ist). |

### 2.2 Neue Dateien — `Editor`-Klasse

**`Editor.kt`** — `CodeEditor`-Subklasse:

- `setThemeColors(...)` — setzt alle 16 Farb-Parameter des Sora-Editor-Chrome aus den Material3-ColorScheme-Werten. Wendet danach optional nutzerdefinierte `darkEditorColors` / `lightEditorColors` aus dem aktiven `currentM3Theme` an.
- `applyEditorSettings(settings)` — überträgt alle `EditorSettings`-Felder auf den Editor.
- `applyFont(context, fontPath, isAsset)` — lädt Schriftart über `FontCache`. `applyFontFromSettings(settings)` ist der bequeme Wrapper.
- `setLanguage(scope)` — erstellt TextMate-Sprache + lädt Keywords.
- `registerTextAction()` / `unregisterTextAction()` — API für benutzerdefinierte Text-Action-Buttons.
- `getSelectedText()` — gibt aktuell markierten Text zurück.
- Zweiphasige `updateColors()`: sofortiger synchroner Patch, danach asynchroner Neuaufbau des vollständigen TextMate-Schemas.

### 2.3 Neue Dateien — Intelligente Features (`editor/intelligent/`)

| Datei | Beschreibung |
|---|---|
| `IntelligentFeature.kt` | Abstract-Basisklasse mit Hooks: `handleInsertChar`, `handleDeleteChar`, `handleInsert`, `handleDelete`, `handleKeyEvent`, `handleKeyBindingEvent`, `isEnabled()`. `IntelligentFeatureRegistry` hält alle built-in + Extension-Features und dispatcht Events. |
| `BulletContinuation.kt` | Markdown: **Enter** in Blockquotes (`> `), ungeordneten Listen (`- `, `* `, `+ `, mit optionalen Checkboxen `[ ]`/`[x]`) und geordneten Listen (`1.`, `2)`) setzt das Listenmuster fort. Leere Listeneinträge werden beim Enter entfernt. **Tab/Shift+Tab** in Listenzeilen: einrücken/ausrücken. |
| `AutoCloseTag.kt` | HTML/HTMX/PHP: Tippt der Nutzer `>` nach einem öffnenden Tag, wird `</tagname>` eingefügt und der Cursor zwischen open und close Tag positioniert. Self-closing-Tags (`<br>`, `<img>`, …) werden übersprungen. Beim Tippen von `/` wird `>` ergänzt und ein Leerzeichen vor dem Slash sichergestellt. Wird nur außerhalb von Attribut-Strings ausgelöst. |

---

## 3. Überarbeitete Utility-Schicht (`utils/`)

### `TextMateSetup.kt` — Komplett neu

- **`SCOPE_MAP`** — vollständige Abbildung aller 47 `Language`-Enum-Werte auf TextMate-Scopes.
- **`EditorTheme`** — Enum `DARCULA` (dark) / `QUIETLIGHT` (light) mit `fromIsDark()` und `fromName()`.
- **`TextMateSetup`-Objekt** — schlanke öffentliche API:
  - `initialize(context)` — Grammatik + Keywords + Markdown-Highlighter.
  - `applyTheme(context, editor, isDark)` — delegiert an `EditorThemeManager`.
  - `applyLanguage(editor, language)` — delegiert an `LanguageManager`.
  - `configureEditor(editor, settings)` — überträgt alle Einstellungen.
  - `invalidateThemeCache()` — leert `EditorThemeManager`-Cache.
- **`EditorSettings`** — `fontPath: String` als neues Feld ergänzt.

### `WorkspaceManager.kt`

- Neuer DataStore-Schlüssel `K_ED_FONT_PATH` (`ed_font_path`).
- `WorkspaceState.editorFontPath` — Default `fonts/JetBrainsMono-Regular.ttf`.
- `saveEditorSettings()` persistiert `settings.fontPath`.

---

## 4. Erweiterte Sprachunterstützung (`data/Models.kt`)

`Language`-Enum von **11 auf 47 Einträge** erweitert:

| Kategorie | Sprachen |
|---|---|
| Android / JVM | Kotlin, Gradle KTS, Java, XML |
| Datenformate | JSON, YAML, TOML, INI, Properties |
| Web | HTML, HTMX, CSS, SCSS, Less, JavaScript, JSX, TypeScript, TSX, PHP |
| Systeme | C, C++, C#, Rust, Go, Swift |
| Scripting | Python, Ruby, Lua, Shell, PowerShell, Batch |
| Markup / Docs | Markdown, LaTeX, Diff, Log |
| Speziell | Dart, SQL, Groovy, Smali, ASM, Zig, Nim, Pascal, Lisp, CMake, Coq, Ignore |
| Fallback | Plain Text |

`fromExtension()` wurde entsprechend erweitert und deckt alle gängigen Dateiendungen ab (z.B. `mjs`, `cjs`, `tsx`, `ps1`, `gvy`, `el`, `gitignore`, …).

---

## 5. Neuer Screen: App-Theme-Picker (`AppThemeScreen.kt`)

Neuer Screen `Screen.APP_THEME` für die Material3-Themeauswahl:

- **Material You** — Toggle mit API-31-Prüfung. Aktiviert dynamische Wandfarben.
- **AMOLED-Modus** — Toggle für reinen schwarzen Hintergrund.
- **Theme-Karten** — alle `m3Themes` werden mit Farbvorschau-Swatches (light + dark ColorScheme) angezeigt.
- **Live-Vorschau** — Mini-Mockup des ausgewählten Themes mit simulierten Toolbar-, Farb- und Textzeilen.
- Navigation: `SettingsScreen` → `AppThemeScreen` über neuen „App Theme"-Eintrag.
- Speicherung über `vm.setM3Theme(id)`, `vm.setMonet(enabled)`, `vm.setAmoled(enabled)`.

---

## 6. Aktualisierter `EditorSettingsScreen`

- **Theme-Picker** ersetzt: statt `IDEColorPalette`-basiertem `ThemePickerCard` jetzt `SimpleThemeCard` für `EditorTheme.DARCULA` / `EditorTheme.QUIETLIGHT` mit Mini-Swatch.
- **Font-Picker** neu: `FontCard`-Liste mit allen vier verfügbaren Fonts.
- `selectedFont` State + Speicherung über `fontPath` in `EditorSettings`.

---

## 7. `SoraCodeEditor.kt` — Compose-Wrapper

- Nutzt jetzt `Editor` (Subklasse) statt `CodeEditor` direkt.
- `setThemeColors()` wird aus dem aktuellen Material3-`ColorScheme` abgeleitet.
- Key- und KeyBinding-Events werden an `IntelligentFeatureRegistry` dispatcht.
- Font wird bei Init und bei Settings-Änderung über `editor.applyFont()` gesetzt.
- `MobileIDEColorScheme.PatchArgs` wurde entfernt — Farben kommen jetzt direkt aus Material3.

---

## 8. `MobileIDEColorScheme.kt` — Erweiterung

Neue Extension-Funktion `ComposeColorScheme.toPatchArgs(isDark)`:

Leitet `MobileIDEColorScheme.PatchArgs` vollständig aus einem Material3
`ColorScheme` ab, sodass der Sora-Editor-Chrome immer zum aktiven
Material3-Theme passt, unabhängig davon, welches Theme gerade aktiv ist.

---

## 9. `IDEViewModel.kt`

- `Screen.APP_THEME` zum `Screen`-Enum hinzugefügt.
- Neue Funktionen:
  - `setM3Theme(themeId)` — setzt `currentM3Theme`, persistiert `ThemePreferences.themeId`, invalidiert alle Editor-Caches.
  - `setMonet(enabled)` — setzt `dynamicM3Theme` und `ThemePreferences.monet`.
  - `setAmoled(enabled)` — setzt `amoledM3Theme` und `ThemePreferences.amoled`.
- `editorFontPath` wird aus `WorkspaceState` in `EditorSettings` übergeben.
- Stale `ThemeManager.invalidate()`-Aufrufe entfernt (werden jetzt durch `EditorThemeManager.invalidate()` abgedeckt).

---

## 10. `MainActivity.kt`

- `ThemePreferences.init(this)` wird vor `setContent {}` aufgerufen.
- `dynamicM3Theme.value` und `amoledM3Theme.value` werden aus `ThemePreferences` initialisiert.
- `MobileIdeTheme {}` statt `MobileIDETheme {}` als Root-Wrapper.
- `Screen.APP_THEME` → `AppThemeScreen(vm)` in der Navigations-`when`-Expression.

---

## 11. Ressourcen (`res/`)

### `values/colors.xml` — Neu

Vollständige Material3 Blueberry-Farbpalette mit drei Kontraststufen:
- **Standard** — 50 Farbwerte (`md_theme_primary` … `md_theme_surfaceContainerHighest`)
- **Medium Contrast** — vollständiger Satz mit Suffix `_mediumContrast`
- **High Contrast** — vollständiger Satz mit Suffix `_highContrast`
- Editor-Hilfswerte: `defaultSymbolInputBackgroundColor`, `defaultSymbolInputTextColor`, `bg`

### `values/themes.xml` — Ersetzt

Von `android:Theme.Material.Light.NoActionBar` auf vollständiges Material3-System migriert:

| Style-Name | Beschreibung |
|---|---|
| `Base.Theme.MobileIDE` | Basisstil: transparente Bars, forceDark=false |
| `Theme.MobileIDE` | Vollständige Blueberry Material3-Farbpalette |
| `Theme.MobileIDE.Oled` | Wie Standard, aber `background` und `surface` = schwarz |
| `Base.Theme.MobileIDE.Monet` | Material You (API 31+, `DynamicColors.Dark`) |
| `Theme.MobileIDE.Oled.Monet` | Material You + pure black |
| `Theme.MobileIDE.Splash` | Splash Screen, Hintergrund aus `md_theme_background` |

### `values/theme_overlays.xml` — Neu

- `ThemeOverlay.MobileIDE.MediumContrast`
- `ThemeOverlay.MobileIDE.HighContrast`

### `xml/network_security_config.xml` — Neu
Cleartext-Traffic erlaubt (notwendig für Termux-Integration und lokale LSP-Server).

### `xml/data_extraction_rules.xml` — Neu
Leere Cloud-Backup-Regeln (Android 12+).

### `xml/backup_rules.xml` — Neu
Legacy-Backup-Regeln (vor Android 12).

### `AndroidManifest.xml` — Erweitert

`<application>` erhält:
```xml
android:networkSecurityConfig="@xml/network_security_config"
android:dataExtractionRules="@xml/data_extraction_rules"
android:fullBackupContent="@xml/backup_rules"
```

---

## 12. Assets — Vollständiger Austausch

Der komplette `assets/`-Ordner wurde ersetzt.

### `assets/textmate/` — 47 Grammatiken

| Verzeichnis | Sprache | Scope |
|---|---|---|
| `kotlin/` | Kotlin | `source.kotlin` |
| `java/` | Java | `source.java` |
| `xml/` | XML | `text.xml` |
| `json/` | JSON | `source.json` |
| `yaml/` | YAML | `source.yaml` |
| `toml/` | TOML | `source.toml` |
| `ini/` | INI | `source.ini` |
| `properties/` | Properties | `source.properties` |
| `html/` | HTML | `text.html.basic` |
| `htmx/` | HTMX | `text.html.htmx` |
| `css/` | CSS | `source.css` |
| `scss/` | SCSS | `source.css.scss` |
| `less/` | Less | `source.css.less` |
| `javascript/` | JavaScript + JSX | `source.js` / `source.js.jsx` |
| `typescript/` | TypeScript + TSX | `source.ts` / `source.tsx` |
| `php/` | PHP | `text.html.php` |
| `cpp/` | C + C++ | `source.c` / `source.cpp` |
| `csharp/` | C# | `source.cs` |
| `rust/` | Rust | `source.rust` |
| `go/` | Go | `source.go` |
| `swift/` | Swift | `source.swift` |
| `python/` | Python | `source.python` |
| `ruby/` | Ruby | `source.ruby` |
| `lua/` | Lua | `source.lua` |
| `shellscript/` | Shell/Bash | `source.shell` |
| `powershell/` | PowerShell | `source.powershell` |
| `bat/` | Batch | `source.batchfile` |
| `markdown/` | Markdown | `text.html.markdown` |
| `latex/` | LaTeX | `text.tex.latex` |
| `diff/` | Diff/Patch | `source.diff` |
| `log/` | Log | `text.log` |
| `dart/` | Dart | `source.dart` |
| `sql/` | SQL | `source.sql` |
| `groovy/` | Groovy | `source.groovy` |
| `smali/` | Smali | `source.smali` |
| `asm/` | Assembly | `source.asm` |
| `zig/` | Zig | `source.zig` |
| `nim/` | Nim | `source.nim` |
| `pascal/` | Pascal | `source.pascal` |
| `lisp/` | Lisp | `source.lisp` |
| `cmake/` | CMake | `source.cmake` |
| `coq/` | Coq | `source.coq` |
| `ignore/` | .gitignore etc. | `source.ignore` |
| `text/` | Plain Text | `text.plain` |
| `black/darcula.json` | AMOLED-Darcula | — |

### Root-Theme-Dateien
- `darcula.json` — Dark-Basis-Theme
- `quietlight.json` — Light-Basis-Theme
- `languages.json` — Grammatik-Registry für Sora
- `keywords.json` — Keyword-Listen für 47 Sprachen
- `words.json` — Allgemeines Wörterbuch

### `assets/fonts/`

| Datei | Beschreibung |
|---|---|
| `JetBrainsMono-Regular.ttf` | Standard-Editorschrift (Default) |
| `JetBrainsMono-Medium.ttf` | Medium-Gewicht |
| `FiraCode-Regular.ttf` | Ligatur-Schrift |
| `FiraCode-Light/Medium/SemiBold/Bold.ttf` | Gewichtsvarianten |
| `Roboto-Regular.ttf` | Serifenlose System-Schrift |
| `Roboto-Medium.ttf` | Medium-Gewicht |
| `Default.ttf` | Systemfallback |

### `assets/terminal/`

Shell-Skripte für die Termux-Integration:

| Datei | Beschreibung |
|---|---|
| `init.sh` | Termux-Umgebungsinitialisierung |
| `setup.sh` | Erstkonfiguration (Java, Gradle, Git) |
| `sandbox.sh` | Isolierte Ausführungsumgebung |
| `universal_runner.sh` | Sprachübergreifender Code-Runner |
| `termux-x11.sh` | X11-Weiterleitung |
| `utils.sh` | Hilfsfunktionen |
| `lsp/bash.sh` | LSP-Server: bash-language-server |
| `lsp/css.sh` | LSP-Server: vscode-css-languageserver |
| `lsp/emmet.sh` | LSP-Server: emmet-ls |
| `lsp/eslint.sh` | LSP-Server: vscode-eslint-languageserver |
| `lsp/html.sh` | LSP-Server: vscode-html-languageserver |
| `lsp/json.sh` | LSP-Server: vscode-json-languageserver |
| `lsp/markdown.sh` | LSP-Server: unified-language-server |
| `lsp/python.sh` | LSP-Server: pylsp |
| `lsp/typescript.sh` | LSP-Server: typescript-language-server |
| `lsp/xml.sh` | LSP-Server: lemminx |

---

## 13. `app/build.gradle.kts`

- `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")` hinzugefügt.
- `isCoreLibraryDesugaringEnabled = true` in `compileOptions`.
- `"com.google.code.gson:gson:2.11.0"` — für Theme-JSON-Parsing.
- `"com.google.android.material:material:1.12.0"` — für `MaterialColors.harmonizeWithPrimary()`.

---

## 14. Projektstruktur nach den Änderungen

```
app/src/main/
├── assets/
│   ├── fonts/                    (10 Schriftarten)
│   ├── terminal/                 (11 Shell-Skripte + LSP-Server)
│   └── textmate/
│       ├── [45 Sprachordner]/
│       ├── darcula.json          (Dark-Basis-Theme)
│       ├── quietlight.json       (Light-Basis-Theme)
│       ├── black/darcula.json    (AMOLED-Basis-Theme)
│       ├── languages.json
│       ├── keywords.json
│       └── words.json
├── java/com/mobileide/app/
│   ├── data/
│   │   └── Models.kt             (Language-Enum: 47 Einträge)
│   ├── editor/
│   │   ├── CodeHighlighter.kt    (NEU)
│   │   ├── Editor.kt             (NEU — CodeEditor-Subklasse)
│   │   ├── EditorThemeManager.kt (NEU)
│   │   ├── FontCache.kt          (NEU)
│   │   ├── KeywordManager.kt     (NEU)
│   │   ├── LanguageManager.kt    (NEU)
│   │   ├── LineEnding.kt         (NEU)
│   │   ├── TextMateConstants.kt  (NEU)
│   │   └── intelligent/
│   │       ├── AutoCloseTag.kt        (NEU)
│   │       ├── BulletContinuation.kt  (NEU)
│   │       └── IntelligentFeature.kt  (NEU)
│   ├── ui/
│   │   ├── components/
│   │   │   └── SoraCodeEditor.kt (aktualisiert)
│   │   ├── screens/
│   │   │   ├── AppThemeScreen.kt      (NEU)
│   │   │   ├── EditorSettingsScreen.kt (aktualisiert)
│   │   │   └── SettingsScreen.kt      (aktualisiert)
│   │   └── theme/
│   │       ├── EditorColor.kt    (NEU)
│   │       ├── PreBuiltThemes.kt (NEU)
│   │       ├── Theme.kt          (aktualisiert)
│   │       ├── ThemeConfig.kt    (NEU)
│   │       ├── ThemeHolder.kt    (NEU)
│   │       ├── ThemeLoader.kt    (NEU)
│   │       ├── ThemeManager.kt   (aktualisiert)
│   │       ├── ThemePreferences.kt (NEU)
│   │       └── Typography.kt
│   ├── utils/
│   │   ├── MobileIDEColorScheme.kt (erweitert: toPatchArgs())
│   │   ├── TextMateSetup.kt        (komplett neu)
│   │   └── WorkspaceManager.kt     (editorFontPath ergänzt)
│   ├── viewmodel/
│   │   └── IDEViewModel.kt     (setM3Theme, setMonet, setAmoled, APP_THEME)
│   └── MainActivity.kt         (ThemePreferences.init, MobileIdeTheme)
└── res/
    ├── values/
    │   ├── colors.xml           (NEU — vollständige M3-Palette)
    │   ├── themes.xml           (aktualisiert — M3-Themsystem)
    │   └── theme_overlays.xml   (NEU — Medium/High-Contrast)
    └── xml/
        ├── network_security_config.xml (NEU)
        ├── data_extraction_rules.xml   (NEU)
        └── backup_rules.xml            (NEU)
```

---

## 15. Statistiken

| Metrik | Vorher | Nachher |
|---|---|---|
| Kotlin-Quelldateien | 66 | 84 |
| Asset-Dateien gesamt | 27 | 125 |
| Unterstützte Sprachen | 11 | 47 |
| Verfügbare Schriftarten | 0 | 10 |
| Eingebaute M3-Themes | 0 | 2 (Blueberry, Lime) |
| Intelligente Editor-Features | 0 | 2 (AutoCloseTag, BulletContinuation) |
| Terminal-Skripte | 0 | 11 |
| LSP-Server-Installationsskripte | 0 | 10 |
