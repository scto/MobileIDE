Lege die Review-Checkliste für die Xed-Migrations-Phasen als versionierte Datei
im MobileIDE-Repo ab, damit sie nach jeder Migrations-Phase abgehakt werden kann.

SCHRITTE:
1. Erstelle die Datei /MobileIDE/build/xed-migration-checklist.md mit folgendem
   Inhalt (Markdown, Checkboxen, KEINE Platzhalter, exakt dieser Text):

# Xed-Migrations-Review-Checkliste

## Übergreifend (bei JEDER Phase)
- [ ] `git log --oneline -3` zeigt den Phasen-Commit als obersten Eintrag
- [ ] `git push` war erfolgreich (kein „up to date" ohne echten Push)
- [ ] `./gradlew assembleDebug` → BUILD SUCCESSFUL (nur Phasen 0/1 sind read-only)
- [ ] `build/xed-migration-report.md` wurde um den Phasen-Abschnitt ergänzt
- [ ] Kein `com.rk.`-Import in neuen/migrierten Kotlin-Dateien (Ausnahmen dokumentiert)
- [ ] Keine MobileIDE-Funktion aus §1.6 regressiert (Terminal, Log-Tabs, APK-Builder)

## Phase 0 – Baseline
- [ ] Baseline-Tag + Branch `pre-xed-migration` existieren
- [ ] Vorher-Zustand dokumentiert (Module-Liste, Testanzahl, letzte Commits)
- [ ] Working Tree war sauber VOR dem Baseline-Commit

## Phase 1 – Inventar & BFS
- [ ] BFS-Datei `build/xed-bfs-dependencies.tsv` vollständig (Fixpunkt erreicht)
- [ ] Doppelprüfung durchgeführt (zwei unabhängige Läufe, Abweichungen dokumentiert)
- [ ] Jede ADAPTER-Klasse (MainActivity, MainViewModel, DrawerViewModel, SearchViewModel)
      als solche markiert – keine als PRIMÄR/TRANSITIV gelistet
- [ ] Package-Kollision core-extension vs. features-extension als EINE Entscheidung festgehalten

## Phase 2 – Toolchain
- [ ] Vorher/Nachher-Tabelle der Versionen im Report
- [ ] AGP/Kotlin-Entscheidung begründet (angleichen ODER MobileIDE-Versionen)
- [ ] NUR benötigte Katalog-Einträge ergänzt – kein kompletter Xed-Katalog kopiert

## Phase 3 – Modul-Scaffold
- [ ] `:core:main`, `:features:extensions`, `:editor`, `:editor-lsp`, `:language-textmate`
      in settings.gradle.kts registriert
- [ ] Keine Modulnamen-Kollision (alte Module existieren noch, eindeutige Namen)
- [ ] Dummy-Tests grün

## Phase 4 – core/main (größte Phase)
- [ ] Skript-basiert migriert (rsync + sed), nicht per Hand
- [ ] `grep -rn "com\.rk"` in `:core:main` → exakt 0 Treffer (Ausnahmen dokumentiert)
- [ ] Asset-Pfade korrekt: `assets/textmate/**` + `keywords.json` + LanguageManager-Konstanten
- [ ] strings/resources zusammengeführt OHNE MobileIDE-Keys zu überschreiben
- [ ] Neue Unit-Tests: LspPersistence, ExtensionManager-Validierung, KeywordManager, Search-Fallback
- [ ] ADAPTER-Stubs (`XedHost` etc.) kompilieren
- [ ] Zwischen-Tag `xed-phase4` gesetzt

## Phase 5 – features/extensions
- [ ] `com.rk.extension`-Reste in features/extensions → 0
- [ ] `MarkdownViewerTest.kt` migriert und grün
- [ ] ExtensionContext-Breaking-Change (alte .xed-Extensions) im Report dokumentiert
- [ ] build.gradle.kts-Dependencies zeigen auf richtige MobileIDE-Module

## Phase 6 – soraX-Editor
- [ ] `io.github.rosemoe.sora.*` unverändert (kein Package-Rename am Framework)
- [ ] `:editor`, `:editor-lsp`, `:language-textmate` bauen eigenständig
- [ ] Alte Module nur gegraut, noch NICHT gelöscht
- [ ] Kein JNI/Build-Logic-Bruch

## Phase 7 – Editor-Integration
- [ ] CodeEditScreen nutzt neuen Stack (sora + TextMate)
- [ ] PublishDiagnosticsEvent → DIAGNOSE-Kanal + Editor-Marker
- [ ] Suche ersetzt durch Xed-EditorSearchPanel/CodeSearchDialog
- [ ] Alter Editor-Code WIRKLICH gelöscht (nicht auskommentiert) – `:language-treesitter`
      + alte `:editor`-Quellen entfernt
- [ ] On-Device-Smoke: Highlighting, Suche, Farbschema, BackHandler
- [ ] grep nach alten Editor-Klassen → 0

## Phase 8 – LSP-Server-Matrix
- [ ] Server-Matrix vollständig (Bash, CSS, ESLint, Emmet, HTML, Markdown, TS, XML,
      Kotlin, Java, …) – jede Zeile mit Entscheidung a/b/c
- [ ] Installer-IDs in `LogRouter.classify` → INSTALL (LogRouter-Test erweitert)
- [ ] On-Device: LSP-Install → INSTALL-Tab, Diagnostics → DIAGNOSE-Tab
- [ ] Terminal-Regression negativ getestet (Session, Drawer, Desktop-Mode, Distro)

## Phase 9 – Plugins löschen
- [ ] Löschung in 3 atomaren Commits (Module → Manager/UI → Assets)
- [ ] `grep -rn "PluginStore\|PluginManager\|mobile.ide.plugin"` → 0 Treffer
- [ ] StoreScreen/DynamicRoutes als Ersatz eingebunden (Settings-Eintrag „Store")
- [ ] reconciliation-code-truth.tsv auf „removed" aktualisiert

## Phase 10 – Finale Validierung
- [ ] `./gradlew clean assembleDebug testDebugUnitTest` von Null an grün
- [ ] Alle Test-Suiten gelistet: apk-builder (16), tooling-api, core/main, features/extensions
- [ ] `scripts/reconcile_modules.sh` → Exit 0 + neuer „no com.rk"-Check integriert
- [ ] On-Device-Suite komplett (a–f aus Phase 10.4)
- [ ] Breaking-Change-Liste + PENDING-Punkte + Fallback-Anweisungen im Report
- [ ] Final-Commit gepusht

## Kritische Prüf-Punkte (häufigste Fehler)
1. `com.rk` nicht nur als Import, sondern auch als `package com.rk` und in
   Strings/Kommentaren/XML prüfen – Abschluss-Check über das GESAMTE Repo.
2. `MainActivity`/`MainViewModel`-Referenzen NIEMALS als TRANSITIV importieren –
   immer auf `XedHost` umschreiben (wichtigster Architektur-Wachpunkt).
3. `:language-treesitter`/`:features:LSP`/Plugins erst löschen, wenn der Ersatz
   bewiesen funktioniert (Phase 7/9) – das „Grauen" aus Phase 6.3 existiert dafür.

2. Commit + Push:
   git add build/xed-migration-checklist.md
   git commit -m "docs(migration): add phase review checklist for xed-core migration"
   git push origin <aktueller-branch>

ANFORDERUNGEN:
- Datei wird 1:1 mit obigem Inhalt erstellt (keine Kürzungen, keine Platzhalter)
- Build ist NICHT nötig (reine Doku-Datei)
- Commit-Message exakt wie oben
- Push-Verifikation: git log --oneline -3 nach dem Push
