Migriere die Kern-Bereiche von ~/Xed-Editor nach /MobileIDE (falls das MobileIDE-Repo
nicht exakt unter /MobileIDE liegt: realen Root-Pfad zu Beginn der Phase 0 ermitteln
und im Report dokumentieren). Ziel ist eine möglichst exakte Source-Code-Übereinstimmung
mit Xed-Editor, umbenannt auf MobileIDE-Packages und -Imports. JEDER Schritt endet mit
Build-Validierung, Unit-Tests, Commit UND Push. Kein Schritt darf ohne Push abgeschlossen
werden.

══════════════════════════════════════════════════════════════════
1. ZIELZUSTAND (Definition of Done)
══════════════════════════════════════════════════════════════════
1.1 core/main → MobileIDE :core:main (bzw. das in MobileIDE bestehende core-Modul,
     sofern umbenennbar – Entscheidung in Phase 3):
     - extension/, feature/, lsp/, search/ VOLLSTÄNDIG übernommen, Packages umbenannt
     - Alle transitiv benötigten Klassen (file, tabs, events, settings, resources,
       components, theme, icons, utils, commands, project, common, filetree, color,
       xed, App, DefaultScope, TerminalLauncher, CrashActivity …) sind über die
       Abhängigkeits-BFS (Phase 1) inkludiert – Kategorie TRANSITIV
1.2 features/extensions → MobileIDE :features:extensions:
     - ExtensionFeature.kt, api/*, loader/*, manager/*, model/*, ui/*, settings/extension/*
       (StoreScreen, PackageDetail, PackageCard, MarkdownViewer, DependenciesDialog,
       ReviewsPage, SourceCodeSheet, packageActions …) inkl. MarkdownViewerTest.kt
1.3 Editor-Submodule ERSETZT:
     - MobileIDE :editor, :language-treesitter, :features:LSP werden ENTFERNT
     - Ersetzt durch Xed/soraX-Editor-Module:
         :editor            (io.github.rosemoe.sora.*, soraX-Fork editor/)
         :editor-lsp        (io.github.rosemoe.sora.lsp.*)
         :language-textmate (TextMateLanguage, TextMateColorScheme)
       übernommen aus soraX/editor + Xed-Editor :editor-lsp + :language-textmate
1.4 Editor-Integration:
     - MobileIDE-CodeEditScreen/EditorViewModel werden auf den neuen Editor-Stack
       umgestellt (Editor.kt-Wrapper com.scto.mobile.ide.editor.Editor, LanguageManager,
       Formatters, CodeHighlighter, XedColorScheme, IntelligentFeature)
     - Der ALTE MobileIDE-Editor-Code wird GELÖSCHT (nicht kommentiert)
1.5 Plugins:
     - ALLE MobileIDE-eigenen Plugins werden GELÖSCHT: PluginManager,
       PluginStoreManager, alle Plugin-Module (com.scto.mobile.ide.plugin.<language>:
       F#, Go, JSON, Lua, Prettier, Python, Rust, Typst, Zig …), Plugin-UI-Einträge,
       Plugin-Assets. Keine eigene Bastel-Lösung.
     - Ersetzt durch das Xed-Extension-System (features/extensions + core/main
       extension/) mit .xed-Paket-Installation, StoreScreen, DynamicRoutes,
       ExtensionAPI/ExtensionManager/ExtensionLoader, XedPackage.
1.6 ERHALTEN (keine Regression):
     - Terminal: SessionService, TerminalScreen, TerminalDesktopSplit (Desktop-Mode),
       TerminalBackEnd, Drawer, Terminal-Settings (Distro ubuntu/alpine)
     - Log-Architektur: LogRouter, ToolingLogManager, ToolingBottomSheet (5 Kerntabs
       INSTALL/BUILD/LSP/DIAGNOSE/IDE_LOGS), LogCatcher
     - APK-Builder: ApkBuilder, ProjectPaths, PathTranslator, GradleTaskManager,
       Gradle-Tasks-Panel
     - Settings-UI app/.../ui/settings, Reconciliation-Skript scripts/reconcile_modules.sh
1.7 KEIN `com.rk.`-Import und KEIN `com.rk.`-Package verbleibt im migrierten Code
     (Ausnahme: nur in Assets/TextMate-Grammatiken erlaubte IDs und Xed-Store-URLs)
1.8 Build + alle Unit-Tests grün; pro Phase ein Commit mit aussagekräftiger Message + Push

══════════════════════════════════════════════════════════════════
2. VERIFIZIERTER IST-ZUSTAND (Xed-Editor-Quelle, aus Dump)
══════════════════════════════════════════════════════════════════
XED-EDITOR (QUELLE):
- Toolchain: AGP 9.2.1, Kotlin 2.4.10, KSP 2.3.2, ktfmt 0.25.0, compileSdk 37,
  minSdk 26, Java 21, composeBom 2025.11.01, applicationId com.rk.xededitor
- Module: :app, :core:main, :core:components, :core:resources, :features:terminal
  (+ :features:terminal:xed-cli/:proot/:link2symlink), :features:extensions,
  :features:runner, :features:git, :editor, :editor-lsp, :language-textmate,
  plugin-sdk, benchmark, benchmark2, baselineprofile, soraX (app/editor/build-logic)
- core/main (namespace com.rk.xededitor, Pakete com.rk.*):
  * extension/api: XedExtensionPoint.kt, ExtensionScreen.kt, ExtensionActivity.kt,
    ExtensionActivityRegistry.kt, DynamicRoute.kt, IntentHandler.kt,
    IntentHandleRegistry.kt, DisposableManager.kt
  * extension/model: Manifest.kt (ExtensionManifest), Package.kt, PackageAuthor.kt,
    Review.kt, UpdatablePackage.kt, PackageCache.kt
  * extension: Constants.kt (EXTENSION_API_BASE/THEMES_API_BASE/ICONPACKS_API_BASE),
    ActivityProvider.kt, InstallState.kt, StoreManager.kt (manager/)
  * feature: Feature.kt, FeatureRegistry.kt, FeatureToggle.kt
  * lsp: LspRegistry.kt, LspServer.kt (ScriptedLspServer), LspServerInstance.kt,
    LspConnector.kt, LspPersistence.kt, LspConnectionConfig.kt, LspActions.kt,
    FileIconProvider.kt, servers/ExternalProcessServer.kt, ExternalSocketServer.kt
  * search: CodeSearchDirect.kt, CodeSearchIndexed.kt, CodeSearchStrategy.kt,
    FileSearchDirect.kt, FileSearchIndexed.kt, FileSearchStrategy.kt,
    SearchViewModel.kt, CodeSearchDialog.kt, FileSearchDialog.kt,
    EditorSearchPanel.kt, EditorSearch.kt, CodeItem.kt,
    index/ProjectIndexer.kt, index/IndexDatabase.kt, index/FileMeta.kt,
    utils/SnippetBuilder.kt, utils/SearchUtils.kt, utils/GlobExcluder.kt
  * assets/textmate/** (languages.json + ~50 Sprachen, keywords.json)
- features/extensions (namespace com.rk.extension, Paket com.rk.extension + com.rk.settings.extension):
  * ExtensionFeature.kt, api/{ExtensionAPI,ExtensionContext,ExtensionSettings,AppResources,Logger}.kt,
    loader/ExtensionLoader.kt, manager/{ExtensionManager,ExtensionAPIManager}.kt,
    model/{Extension,InstallResult}.kt, ui/XedInstallDialog.kt, ExtensionBridge.kt,
    ExtensionEvent.kt,
    settings/extension/{StoreScreen,PackageDetail,PackageCard,MarkdownViewer,
    DependenciesDialog,ExtensionActionButtons,ExtensionAuthorIcon,ExtensionDialogRenderer,
    ExtensionDialogState,ReviewsPage,SourceCodeSheet,packageActions}.kt
  * build.gradle.kts: deps auf :core:main, :core:components, :core:resources,
    :editor, :editor-lsp; Tests: MarkdownViewerTest.kt
- soraX/editor: io.github.rosemoe.sora.* (CodeEditor, event/*, lang/*, graphics/*,
  text/*, widget/*) + soraX/app als Referenz-App; Xed :editor-lsp (sora.lsp.*)
  + :language-textmate (TextMateLanguage, TextMateColorScheme, monarch)
- WICHTIGE TRANSITIVE ABHÄNGIGKEITEN (greifen aus den 4 Bereichen + extensions):
  com.rk.activities.main.{MainActivity,MainViewModel}, com.rk.activities.main.session.EditorManager,
  com.rk.tabs.editor.{EditorTab,CodeEditorState,CodeEditorCompose,EditorNotice},
  com.rk.file.{FileObject,FileWrapper,BuiltinFileType,FileTypeManager,FileOperations,child,sandboxDir,…},
  com.rk.events.{Events,AppEvent,LSPEvent,FileTreeEvent}, com.rk.settings.{Settings,Preference,SettingsRegistry,SettingsRoutes},
  com.rk.resources.{strings,drawables,getString,getFilledString,fillPlaceholders},
  com.rk.components.*, com.rk.theme.*, com.rk.icons.*, com.rk.utils.*,
  com.rk.commands.*, com.rk.project.*, com.rk.common.*, com.rk.filetree.*,
  com.rk.color.*, com.rk.xed.*, com.rk.App (iconPackManager/themeManager),
  com.rk.DefaultScope, com.rk.TerminalLauncher, com.rk.crashhandler.CrashActivity
- KRITISCHER KOPF: ExtensionContext.kt enthält „// DO NOT UPDATE PACKAGE NAME OTHERWISE
  EXTENSIONS WILL BREAK" → Package-Umbenennung bricht bereits kompilierte .xed-Erweiterungen.

MOBILEIDE (ZIEL, aus verifizierten Session-Reports):
- com.scto.mobile.ide; Module: :app, :core:common, :core:tooling:tooling-api,
  :core:tooling:tooling-impl, :features:terminal, :core:apk-builder, :editor,
  :language-treesitter, :features:LSP, Plugin-Module
- Log-Architektur verifiziert: LogRouter.classify (in TermuxExec.launchInternalTerminal
  verdrahtet, Commit d24c7842), ToolingLogManager, ToolingBottomSheet 5 Kerntabs,
  LogCatcher (Tag-Routing), Sender-Matrix
- Terminal verifiziert: SessionService (SSOT, sessionOrder/currentSession),
  TerminalScreen.kt:607 (Drawer "${index+1} · Titel"), TerminalDesktopSplit (Desktop-Mode,
  Commit 4eeab1a9), TerminalBackEnd, CloseLastSessionBehavior, Settings ubuntu/alpine
- APK-Builder verifiziert: ApkBuilder/ProjectPaths/PathTranslator (c9561d5b),
  GradleTaskManagerImpl, 16 Unit-Tests grün
- Plugins: com.scto.mobile.ide.plugin.<language> (reconciliation-code-truth.tsv: F#, Go,
  JSON, Lua, Prettier, Python, Rust, Typst, Zig, alle migriert, Versionen 0.1.0–1.2.3)
- Tests: :core:apk-builder:testDebugUnitTest (16), :core:tooling:tooling-api:test,
  assembleDebug grün
- Module-Konsolidierung: TerminalEnvironmentSelector entfernt, Assets single-owner
  (Commit bdf76e1d), scripts/reconcile_modules.sh

══════════════════════════════════════════════════════════════════
3. VERBINDLICHE PAKET-MAPPING-TABELLE (Phase 1 finalisiert)
══════════════════════════════════════════════════════════════════
  com.rk                          → com.scto.mobile.ide
  com.rk.xededitor.BuildConfig    → com.scto.mobile.ide.BuildConfig
  com.rk.extension                → com.scto.mobile.ide.extension
  com.rk.feature                  → com.scto.mobile.ide.feature
  com.rk.lsp                      → com.scto.mobile.ide.lsp
  com.rk.search                   → com.scto.mobile.ide.search
  com.rk.file                     → com.scto.mobile.ide.file
  com.rk.filetree                 → com.scto.mobile.ide.filetree
  com.rk.tabs                     → com.scto.mobile.ide.tabs
  com.rk.tabs.editor              → com.scto.mobile.ide.tabs.editor
  com.rk.tabs.base                → com.scto.mobile.ide.tabs.base
  com.rk.tabs.image               → com.scto.mobile.ide.tabs.image
  com.rk.events                   → com.scto.mobile.ide.events
  com.rk.settings                 → com.scto.mobile.ide.settings
  com.rk.settings.lsp             → com.scto.mobile.ide.settings.lsp
  com.rk.settings.extension       → com.scto.mobile.ide.settings.extension
  com.rk.settings.editor          → com.scto.mobile.ide.settings.editor
  com.rk.settings.language        → com.scto.mobile.ide.settings.language
  com.rk.resources                → com.scto.mobile.ide.resources
  com.rk.components               → com.scto.mobile.ide.components
  com.rk.theme                    → com.scto.mobile.ide.theme
  com.rk.icons                    → com.scto.mobile.ide.icons
  com.rk.utils                    → com.scto.mobile.ide.utils
  com.rk.editor                   → com.scto.mobile.ide.editor
  com.rk.commands                 → com.scto.mobile.ide.commands
  com.rk.project                  → com.scto.mobile.ide.project
  com.rk.common                   → com.scto.mobile.ide.common
  com.rk.color                    → com.scto.mobile.ide.color
  com.rk.xed                      → com.scto.mobile.ide.xed
  com.rk.activities.main          → com.scto.mobile.ide.ui.main (Adapter-Entscheidung!)
  com.rk.crashhandler             → com.scto.mobile.ide.crashhandler
  com.rk.git                      → NICHT migrieren (siehe 4C)
  com.rk.terminal / com.rk.exec   → NICHT migrieren (MobileIDE-Terminal bleibt)
  io.github.rosemoe.sora          → BLEIBT unverändert (Editor-Framework)
  WICHTIG: core/main-extension UND features/extensions teilen in Xed bewusst den
  Package-Raum com.rk.extension → wird auf EIN Paket com.scto.mobile.ide.extension
  gemappt (gleiches Package in zwei Modulen ist in Kotlin erlaubt und wird von Xed
  exakt so betrieben).

══════════════════════════════════════════════════════════════════
4. ÜBERNAHME-KATEGORIEN (analog Abhängigkeits-BFS)
══════════════════════════════════════════════════════════════════
4A PRIMÄR (verpflichtend, 1:1-Kopie mit Package-Rename):
   core/main extension/, feature/, lsp/, search/ + features/extensions/
4B TRANSITIV (BFS-erzwungen, mitübernehmen):
   Jede importierte Klasse ohne MobileIDE-Pendant (file, tabs, events, settings-Kern,
   resources, components, theme, icons, utils, commands, project, common, filetree,
   color, xed, App, DefaultScope, TerminalLauncher, CrashActivity …)
4C NICHT ÜBERNOMMEN:
   Xed :app, :features:git, :features:runner, :features:terminal, plugin-sdk,
   benchmark/*, baselineprofile, fastlane; MobileIDE-Terminal & APK-Builder & Log-System
   bleiben. Xed-`features:terminal/lsp/servers` (Bash, CSS, ESLint, Emmet, HTML,
   Markdown, TypeScript, XML) + assets/terminal/lsp/*.sh → werden NICHT blind kopiert,
   sondern NUR die MobileIDE-kompatiblen Fälle als ADAPTER übernommen (Konflikt mit
   MobileIDE-LSP-Installern – Phase 8 entscheidet pro Server).
4D ADAPTER (Xed-Code wird NICHT 1:1 übernommen, sondern auf MobileIDE-Äquivalente
    umgeschrieben):
   MainActivity.instance / MainViewModel → XedHost-Singleton in
   com.scto.mobile.ide.integration (editorTabs, currentTab, editorManager,
   openFile/jumpToPosition, snackbar, toast, errorDialog, fileTreeViewModel)
   EditorTab-LSP-Connect (connectLsp/applyHighlightingAndConnectLSP) → wird auf
   MobileIDE-EditorViewModel/CodeEditScreen-Adapter umgeschrieben UND an LogRouter/
   ToolingLogManager (Kanal LSP + INSTALL) angeschlossen
   TerminalLauncher.launch (ScriptedLspServer.install) → MobileIDE-Terminal-Runner
   (TermuxExec.launchInternalTerminal) + LogRouter.classify → INSTALL-Kanal

══════════════════════════════════════════════════════════════════
5. PHASENPLAN (JEDE Phase: CHECKPOINT mit Build + Tests + Commit + Push)
══════════════════════════════════════════════════════════════════
PHASE 0 – BASELINE & ABSICHERUNG (read-only)
0.1 git -C /MobileIDE status --porcelain; branch -vv; log --oneline -15
0.2 Vollbuild + ALLE Unit-Tests: ./gradlew assembleDebug testDebugUnitTest
    (Ergebnis dokumentieren: grün, Anzahl Tests)
0.3 Sicherung: git tag xed-migration-baseline-$(date +%Y%m%d) UND
    git branch pre-xed-migration (falls nicht vorhanden)
0.4 build/xed-migration-report.md anlegen (Startzustand, Module, Tests, Commits)
0.5 CHECKPOINT: commit+push „docs(migration): baseline snapshot pre-xed-migration"

PHASE 1 – INVENTAR & ABHÄNGIGKEITS-BFS (read-only, Doppelprüfung)
1.1 Echte Xed-Struktur lesen (NICHT aus Gedächtnis):
    ~/Xed-Editor/settings.gradle.kts, gradle/libs.versions.toml,
    core/main/build.gradle.kts, features/extensions/build.gradle.kts,
    soraX/editor/build.gradle.kts, soraX/build-logic/** , :editor-lsp & :language-textmate build.gradle.kts
1.2 Datei-Inventar der 4 Bereiche + features/extensions erzeugen:
    find ~/Xed-Editor/core/main/src/main/java/com/rk/{extension,feature,lsp,search} -name "*.kt" | wc -l
    find ~/Xed-Editor/features/extensions/src -name "*.kt"
    find ~/Xed-Editor/core/main/src/main/assets/textmate -type f | wc -l
    find ~/Xed-Editor/soraX/editor/src/main/java -name "*.java" -o -name "*.kt" | wc -l
1.3 ABHÄNGIGKEITS-BFS (KERN-VALIDIERUNG):
    a) Start = Dateien aus 1.2
    b) grep ALLE `import com.rk.*`, `import io.github.rosemoe.sora.*` je Datei
    c) Import fehlt in MobileIDE → TRANSITIV (4B), zur Start-Menge, rekursiv bis Fixpunkt
    d) Import hat MobileIDE-Pendant → ADAPTER (4D), Liste festhalten
    e) Ausgabe: build/xed-bfs-dependencies.tsv
       spalten: quelle-datei | import | kategorie | mobileide-pendant? | ziel-datei | phase
1.4 Package-Mapping final fixieren (Tabelle §3) in build/xed-package-mapping.tsv
1.5 DOPPELPRÜFUNG: Zwei unabhängige Sichten vergleichen
    (a) grep-BFS, (b) KScope/IDE-Index falls verfügbar ODER zweiter manueller Lauf.
    Abweichungen dokumentieren. Offene Punkte als PENDING markieren.
1.6 CHECKPOINT (nur Doku): commit+push „docs(migration): dependency BFS and package map"

PHASE 2 – TOOLCHAIN & VERSIONSKATALOG
2.1 libs.versions.toml (Xed) als Referenz; MobileIDE-Katalog erweitern NUR um benötigte
    Einträge: sora, sora-lsp, textmate (monarch, regex-lib), lsp4j 1.0.0,
    kotlinx-serialization, okhttp, gson, coil(+svg), semver, room (nur falls
    IndexDatabase benötigt), robolectric/truth/junit (nur für neue Tests)
2.2 Entscheidung dokumentieren: AGP/Kotlin-Version ANGLEICHEN (empfohlen, damit
    soraX-Module unverändert bauen: AGP 9.2.1, Kotlin 2.4.10, compileSdk 37,
    minSdk 26, Java 21) ODER auf MobileIDE-Versionen bleiben (dann müssen
    soraX-Build-Dateien angepasst werden). Kein stilles Upgrade – Report-Eintrag.
2.3 Vorher/nachher-Tabelle in build/xed-migration-report.md §2
2.4 ./gradlew assembleDebug → BUILD SUCCESSFUL
2.5 CHECKPOINT: commit+push „build: align version catalog for xed-core migration"

PHASE 3 – MODUL-SCAFFOLD (neue Modul-Pfade)
3.1 Registrieren in /MobileIDE/settings.gradle.kts:
    include(":core:main")        → MobileIDE core/main (bestehendes :core:common
                                    NICHT löschen; neues Modul anlegen, falls Pfad-Konflikt)
    include(":features:extensions")
    include(":editor")           → ersetzt MobileIDE :editor (soraX-Kopie)
    include(":editor-lsp")
    include(":language-textmate")
    → :language-treesitter und :features:LSP und Plugin-Module werden in Phase 6/7/9
      entfernt; bis dahin KOEXISTIEREN sie (kein Parallelbetrieb-Konflikt durch
      eindeutige Modulnamen)
3.2 Minimal-Build-Dateien je Modul anlegen (namespace com.scto.mobile.ide.*, deps auf
    :core:main bzw. sibling-Module), leere src-Verzeichnisse, Dummy-Test
3.3 ./gradlew :core:main:assembleDebug :features:extensions:assembleDebug → grün
3.4 CHECKPOINT: commit+push „build(migration): scaffold core/main and features/extensions modules"

PHASE 4 – core/main MIGRIEREN (extension/feature/lsp/search + TRANSITIV)
4.1 Kopieren+Umbenennen (SKRIPT, kein Hand-Drag):
    a) rsync -a ~/Xed-Editor/core/main/src/main/java/com/rk/ /MobileIDE/core/main/src/main/java/com/scto/mobile.ide/ (nur Kategorie PRIMÄR+TRANSITIV laut BFS-TSV)
    b) Verzeichnisse gemäß §3 umbenennen (find -type d -name rk → scto/mobile/ide)
    c) Inhalt: sed/Ersetzung `package com.rk` → `package com.scto.mobile.ide` und
       `import com.rk` → `import com.scto.mobile.ide` (mehrstufig, da rk→scto.mobile.ide
       zwei Pfad-Ebenen mehr erzeugt: mv + sed pro Datei)
    d) Alle Dateien, die ADAPTER-Klassen referenzieren (4D), werden VOM ÜBERNEHMEN
       AUSGENOMMEN und als Adapter-Stubs im MobileIDE-Integration-Paket angelegt
       (XedHost, XedTerminalLauncher, XedSnackbar…)
4.2 Assets kopieren: core/main/src/main/assets/textmate/** + keywords.json
    (Berechtigungen/Referenzen prüfen: LanguageManager initKeywordRegistry nutzt
    TEXTMATE_PREFIX + KEYWORDS_FILE – Konstante prüfen und ggf. anpassen)
4.3 Ressourcen: Xed resources/strings + drawables + values (transitiv benötigt) in
    :core:main übernehmen – KEINE MobileIDE-strings überschreiben, sondern zusammenführen
    (Duplikate: MobileIDE-Wert gewinnt für bereits existierende Keys, Doku der Entscheidung)
4.4 DOPPELPRÜFUNG com.rk-Reste:
    grep -rn "com\.rk" /MobileIDE/core/main/src/main/java --include="*.kt" → MUSS 0 sein
    (Ausnahme: Kommentar-Hinweise, Xed-URLs, BuildConfig-Konstanten dokumentieren)
4.5 Kompilieren: ./gradlew :core:main:compileDebugKotlin
    a) Fehler KATEGORISIEREN: fehlende TRANSITIV-Klasse → BFS erweitern; ADAPTER-Zugriff
       → XedHost-Stub vervollständigen; Asset-Pfad → Pfad anpassen
    b) Jede Fehlerklasse wird im Report §4 als Entscheidung dokumentiert
4.6 Unit-Tests für übernommene Pure-Logic:
    - LspPersistence (migrate/restore, Fallback bei leerem File)
    - SearchViewModel-Core (CodeSearchStrategies) bzw. mind. ein BFS-verifizierbarer
      Test; ProjectIndexer-Fallback (kein DB → Direktsuche)
    - ExtensionManager.validateExtensionDir (Fehlerfälle: fehlendes manifest.json,
      fehlende APK)
    - KeywordManager initKeywordRegistry
    Tests müssen grün sein; Xed-Vorlagen (falls vorhanden) 1:1 mit umbenennen
4.7 CHECKPOINT: ./gradlew :core:main:test assembleDebug → grün;
    commit+push „feat(migration): import xed core/main extension f
