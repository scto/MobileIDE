Hier ist die **Review-Checkliste** zur Xed-Migrations-Prompt — aufgeteilt nach Phasen, damit du nach jedem Commit+Push gezielt prüfen kannst.

---

# Review-Checkliste: Xed-Editor-Migration nach MobileIDE

## Übergreifend (bei JEDER Phase)

- [ ] `git log --oneline -3` zeigt den Phasen-Commit als obersten Eintrag
- [ ] `git push` war erfolgreich (kein „up to date" ohne echten Push)
- [ ] `./gradlew assembleDebug` → BUILD SUCCESSFUL (nur Phasen 0/1 sind read-only)
- [ ] `build/xed-migration-report.md` wurde um den Phasen-Abschnitt ergänzt
- [ ] Kein `com.rk.`-Import in **neuen/migrierten** Kotlin-Dateien (Ausnahmen dokumentiert)
- [ ] Keine MobileIDE-Funktion aus §1.6 regressiert (Terminal, Log-Tabs, APK-Builder)

---

## Phase 0 – Baseline

- [ ] Baseline-Tag + Branch `pre-xed-migration` existieren
- [ ] Vorher-Zustand dokumentiert: Module-Liste, Testanzahl, letzte Commits
- [ ] Working Tree war sauber VOR dem Baseline-Commit (sonst sind Fremdänderungen drin)

## Phase 1 – Inventar & BFS

- [ ] BFS-Datei `build/xed-bfs-dependencies.tsv` existiert und ist **vollständig** (Fixpunkt erreicht, nicht abgebrochen)
- [ ] Statistik plausibel: TRANSITIV-Anteil erwartbar hoch? (ja, wenn file/tabs/events/settings eingezogen werden)
- [ ] **Doppelprüfung durchgeführt** (zwei unabhängige Läufe, Abweichungen dokumentiert)
- [ ] Jede ADAPTER-Klasse (MainActivity, MainViewModel, DrawerViewModel, SearchViewModel) ist als solche markiert — keine davon darf als PRIMÄR/TRANSITIV gelistet sein
- [ ] Package-Kollision core-extension vs. features-extension ist als **eine** Entscheidung festgehalten

## Phase 2 – Toolchain

- [ ] Vorher/Nachher-Tabelle der Versionen im Report
- [ ] AGP/Kotlin-Entscheidung begründet (angleichen ODER MobileIDE-Versionen mit Build-Anpassung)
- [ ] NUR benötigte Katalog-Einträge ergänzt — kein kompletter Xed-Katalog kopiert
- [ ] Build grün OHNE dass alte Module betroffen sind

## Phase 3 – Modul-Scaffold

- [ ] `:core:main`, `:features:extensions`, `:editor`, `:editor-lsp`, `:language-textmate` in `settings.gradle.kts`
- [ ] Kein Modulname kollidiert (alte Module existieren noch, aber eindeutige Namen)
- [ ] Dummy-Tests grün (beweist, dass die Modul-Plumbing funktioniert)

## Phase 4 – core/main (größte Phase!)

- [ ] **Skript-basiert** migriert (rsync + sed), nicht per Hand — sonst sind Dateien unvollständig
- [ ] `grep -rn "com\.rk"` in `:core:main` → **exakt 0 Treffer** (außer dokumentierten URLs/Kommentaren)
- [ ] Asset-Pfade korrekt: `assets/textmate/**` + `keywords.json` vorhanden, `LanguageManager`-Konstanten passen
- [ ] strings/resources zusammengeführt OHNE MobileIDE-Keys zu überschreiben (Konfliktlog vorhanden)
- [ ] Neue Unit-Tests vorhanden: LspPersistence, ExtensionManager-Validierung, KeywordManager, Search-Fallback
- [ ] ADAPTER-Stubs (`XedHost` etc.) existieren und kompilieren
- [ ] **Empfehlung:** Phase 4 als Zwischen-Tag `xed-phase4` taggen (größte Rollback-Distanz)

## Phase 5 – features/extensions

- [ ] `com.rk.extension`-Reste in features/extensions → 0
- [ ] `MarkdownViewerTest.kt` migriert und grün
- [ ] ExtensionContext-Entscheidung (Breaking-Change für alte `.xed`-Extensions) im Report dokumentiert
- [ ] build.gradle.kts-Dependencies auf die richtigen MobileIDE-Module zeigen

## Phase 6 – soraX-Editor

- [ ] `io.github.rosemoe.sora.*` unverändert (kein Package-Rename am Framework!)
- [ ] `:editor`, `:editor-lsp`, `:language-textmate` bauen eigenständig
- [ ] Alte Module nur **gegraut** (aus Build ausgeschlossen), noch NICHT gelöscht
- [ ] Kein JNI/Build-Logic-Bruch durch Toolchain-Anpassung

## Phase 7 – Editor-Integration

- [ ] CodeEditScreen nutzt den neuen Editor-Stack (sora + TextMate)
- [ ] PublishDiagnosticsEvent → DIAGNOSE-Kanal + Editor-Marker
- [ ] Suche ersetzt durch Xed-EditorSearchPanel/CodeSearchDialog
- [ ] **Alter Editor-CODE wirklich gelöscht** (nicht auskommentiert!) — `:language-treesitter` + alte `:editor`-Quellen entfernt
- [ ] On-Device-Smoke: Highlighting, Suche, Farbschema, BackHandler
- [ ] `grep` nach alten Editor-Klassen → 0

## Phase 8 – LSP-Server-Matrix

- [ ] Server-Matrix vollständig: pro Server (Bash, CSS, ESLint, Emmet, HTML, Markdown, TS, XML, Kotlin, Java, …) eine Zeile mit Entscheidung a/b/c
- [ ] Installer-IDs in `LogRouter.classify` → INSTALL (LogRouter-Test erweitert)
- [ ] On-Device: LSP-Install erscheint im INSTALL-Tab, Diagnostics im DIAGNOSE-Tab
- [ ] Terminal-Regression negativ getestet (Session, Drawer, Desktop-Mode, Distro)

## Phase 9 – Plugins löschen

- [ ] Löschung in **3 atomaren Commits** (Module → Manager/UI → Assets)
- [ ] `grep -rn "PluginStore\|PluginManager\|mobile.ide.plugin"` → **0 Treffer**
- [ ] StoreScreen/DynamicRoutes als Ersatz eingebunden (Settings-Eintrag „Store")
- [ ] reconciliation-code-truth.tsv auf „removed" aktualisiert

## Phase 10 – Finale Validierung

- [ ] `./gradlew clean assembleDebug testDebugUnitTest` von Null an grün
- [ ] Alle Test-Suiten gelistet: apk-builder (16), tooling-api, core/main, features/extensions
- [ ] `scripts/reconcile_modules.sh` → Exit 0 + neuer „no com.rk"-Check integriert
- [ ] On-Device-Suite komplett (a)–f) aus Phase 10.4)
- [ ] Breaking-Change-Liste + PENDING-Punkte + Fallback-Anweisungen im Report
- [ ] Final-Commit gepusht, Tag `xed-migration-complete` optional

---

## Kritische Prüf-Punkte (die drei häufigsten Fehler)

1. **com.rk-Reste übersehen:** Nicht nur `import com.rk` prüfen, sondern auch `package com.rk` **und** `com.rk` in Strings/Kommentaren/XML. Ein `grep -rn "com\.rk"` über das ganze Repo (nicht nur die migrierten Module) ist der Abschluss-Check.

2. **ADAPTER-Klassen falsch einsortiert:** `MainActivity.instance`-Zugriffe dürfen **nie** als TRANSITIV importiert werden (das würde die gesamte MobileIDE-Activity-Architektur zerstören). Alles, was `MainActivity`/`MainViewModel` referenziert, MUSS auf `XedHost` umgeschrieben werden — das ist der wichtigste Architektur-Wachpunkt überhaupt.

3. **Löschung zu früh:** `:language-treesitter`/`:features:LSP`/Plugins erst löschen, wenn der Ersatz **bewiesen** funktioniert (Phase 7/9). Das „Grauen" in Phase 6.3 existiert genau dafür — lass dich nicht verleiten, früher zu löschen.

---

**Empfehlung für die Umsetzung:** Häng dir die Checkliste als `build/xed-migration-checklist.md` ins Repo (eigener kleiner Commit vor Phase 1), damit sie versioniert ist und du nach jeder Phase abhaken kannst. Soll ich dir den kleinen Prompt dafür gleich mitliefern?
