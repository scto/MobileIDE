# Ziel
Vervollständige die bereits begonnene Migration von Xed-Editor-Funktionalität nach 
MobileIDE (com.scto.mobile.ide): fehlende Settings-Screens, fehlende Editor-Funktionen, 
Terminal-Erweiterungen (Theme/Font/Session-Lifecycle/Back-Navigation/State-Restore), 
vollständige Command-Palette-Commands, und mache die bereits vorhandene Git-Funktionalität 
in der App tatsächlich sichtbar.

# Kontext (verifizierter Ist-Zustand von ~/MobileIDE, Stand PROGRESS.md/status.md/
settings.gradle.kts/README.md/Fixing.md)
- rootProject.name = "MobileIDE", Basis-Package: com.scto.mobile.ide
- Aktive Module (settings.gradle.kts): :app, :editor, :editor-lsp, :language-treesitter, 
  :core:main, :core:components, :core:resources, :core:runner, :core:apk-builder, 
  :core:tooling:tooling-api, :core:tooling:tooling-impl, :core:tooling:tooling-server, 
  :features:extensions, :core:lsp, :extension-languages, :core:common, 
  :features:layout-preview, :features:terminal, :features:plugin-store, sowie 12 
  Plugin-Module unter :plugins:* (java-lsp, json-lsp, kotlin-lsp, kotlin-kmp-lsp, 
  lua-lsp, python-lsp, typst-lsp, go-lsp, rust-lsp, zig-lsp, fsharp-lsp, prettier-lsp).
- WICHTIGER BEFUND: In settings.gradle.kts sind folgende Zeilen AUSKOMMENTIERT und 
  MÜSSEN reaktiviert werden:
  `// include(":features:git")`
  `// include(":core:commands")`
  `// include(":features:runner")`
  `// include(":features:terminal:mobileide-cli")`
  `// include(":features:terminal:proot")`
  `// include(":features:terminal:link2symlink")`
  Von diesen ist für diesen Auftrag primär `:features:git` und `:core:commands` 
  relevant (Punkt 3 und 4 des Nutzerauftrags). Die physische Existenz von 
  `features/git` ist belegt durch `migrate_xed.py` (Zeile `os.path.join(base, 'git')`, 
  Package-Mapping `com.rk.git` → `com.scto.mobile.ide.features.git`).
- :features:terminal ist LAUT PROGRESS.md (Prompt 15, "Consolidate Terminal Features 
  Module", 2026-07-26) bereits vollständig konsolidiert: Session-Backend, ANSI/PTY-
  Emulator, View-Rendering, TerminalScreen + TerminalSettingsScreen, TerminalService, 
  Sandbox-Setup-Assets (ideenv, idesetup, init.sh, setup.sh) unter 
  `features/terminal/src/main/assets/terminal/`, inkl. 5 Farbschemata (default, 
  dracula, solarized_dark, nord, monokai) und Font-Size-Slider (10-30sp, siehe 
  status.md 2026-07-01 "Custom Terminal Color Schemes" + "Font Size scaling").
- Route-Konvention bereits etabliert: "settings/terminal" (siehe status.md 2026-07-03 
  Bugfix-Historie zu genau dieser Route).
- Bereits laut PROGRESS.md (2026-07-27, Prompt 17 "git-credentials-sidepanel-
  settings") implementiert: `GitConfigDialog.kt` (Username/E-Mail/Remote-URL 
  HTTPS+SSH/Personal Access Token/SSH-Key mit optionaler Passphrase, Live-
  Connectivity-Test via JGit `ls-remote`), `GitViewModel.kt`, `GitPanel.kt` 
  (inkl. automatischer Conflict-Detection-Banner laut Prompt 12), `GitToolbarCompact` 
  (Sidepanel-Settings-Launcher). Credentials werden in DataStore/SharedPreferences 
  persistiert. DIESE KOMPONENTEN MÜSSEN NICHT NEU ERSTELLT WERDEN – nur reaktiviert, 
  verdrahtet und sichtbar gemacht werden (siehe Stufe 1).
- Ebenfalls bereits implementiert laut PROGRESS.md Prompt 12 (2026-07-27): 
  `GitConflictParser.kt`, `GitConflictManager.kt`, `GitConflictResolutionDialog.kt` 
  (3-Way-Merge-Konfliktlösung, mobil-optimiert mit "Lokal übernehmen"/"Eingehend 
  übernehmen"/"Beide übernehmen").
- `:core:commands` mit `CommandManager.kt` ist laut status.md (2026-07-04, "Command 
  Registration API") bereits implementiert: "Fully implemented CommandManager.kt 
  inside :core:commands, enabling dynamic extensions to register, unregister, and 
  execute IDE commands with UI integration." Auch dies MUSS NICHT NEU GEBAUT, nur 
  reaktiviert werden.
- Bereits vorhandene Settings-Screens: TerminalSettingsScreen (Reset/Reinstall, 
  Farbschemata, Font Size), LspSettingsScreen (Java/Kotlin/Bash/XML Status-Checks, 
  siehe status.md 2026-07-04 "vier Kernsprachen"), BuildSettingsScreen (OpenJDK 17/21, 
  Gradle, Android SDK, Build-Tools v35/v36, Platforms API 34/35, CMake/NDK), 
  EditorScreen-Settings (Font Size Slider 8-32sp, LSP-Editor-Toggle TreeSitter/
  TextMate mit Fallback-Logik).
- NOCH NICHT vorhanden (in Stufe 0 zu verifizieren): dedizierter 
  ExtensionsSettingsScreen (Plugin-System existiert über `:features:extensions` und 
  `assets/bundled_plugins/<id>` laut Plugins-Development.md, aber ohne erkennbaren 
  eigenen Settings-Screen zur Verwaltung), dedizierter SpeechServerSettingsScreen 
  (Text-to-Speech/Speech-to-Text ist in KEINER der vorliegenden Feature-Listen 
  (README.md Features/TODO) erwähnt – Existenz in Xed-Editor selbst MUSS in Stufe 0 
  verifiziert werden, bevor irgendetwas gebaut wird).
- Zentrale Navigation-Utility: `com.scto.mobile.ide.core.common.utils.NavigationUtils.
  safeNavigate` (Extension-Function auf NavController, verhindert doppelte 
  Navigation/IllegalArgumentException, siehe Fixing.md) – bei ALLEN neuen 
  Navigations-Aufrufen zwingend verwenden.
- Bekanntes Architektur-Muster für Terminal-Session-Close: TerminalBackEnd.kt nutzt 
  bereits einen entkoppelten Callback-Mechanismus (siehe status.md 2026-07-01 
  "Modular decoupling"), um zirkuläre Abhängigkeiten zwischen :core:main und :app zu 
  vermeiden – bei Erweiterungen (Stufe 4) dieses Muster respektieren, nicht umbauen.
- Bereits vorhandene Tooling-Bottom-Sheet-Tabs (ToolingBottomSheet.kt) zur Orientierung 
  für weitere Tab-Ergänzungen: Terminal-Logs, Problems, IDE-Log, Build, LSP, AI (Prompt 
  16), Debug (Prompt 10), Docs (Prompt 14) – macht 8 Tabs total laut PROGRESS.md.
- Bereits vorhandener DebugSessionManager (Prompt 10, 2026-07-26): JDWP-Debugging mit 
  Breakpoints, Stack-Frames, Variable-Inspection, Execution-Controls (resume, pause, 
  stepOver, stepInto, stepOut, stop) – als Anbindungspunkt für Debug-Commands in 
  Stufe 5 nutzbar.
- README.md TODO-Liste (Stand aktuell) listet weiterhin offen: Interactive Visual 
  Debugger (TEILWEISE bereits durch Prompt 10 umgesetzt, TODO-Eintrag könnte veraltet 
  sein), LSP-Diagnostics-Overlays (TEILWEISE durch Prompt 05 umgesetzt), Gradle Sync/
  Dependency Downloader, GUI Keystore Wizard (TEILWEISE durch Prompt 03 umgesetzt), 
  Git Conflict Resolution Tool (BEREITS durch Prompt 12 umgesetzt trotz TODO-Eintrag – 
  README.md ist an dieser Stelle nicht mehr aktuell), Plugin Market. Explizit NICHT 
  gelistet: Command-Palette-Vollständigkeit oder System-Zurück-Taste-Verhalten – 
  diese sind echte, vom Nutzer korrekt identifizierte Lücken.
- Quelle für die eigentliche Feature-Migration ist ~/Xed-Editor (Original-Android-
  Editor-Projekt, com.rk.*-Package-Namespace laut migrate_xed.py-Mapping). Der exakte 
  interne Aufbau (konkrete Settings-Screen-Klassennamen, Editor-Zusatzfunktionen 
  jenseits von sora-editor, Terminal-Font-Auswahl-Mechanismus, Existenz eines Speech-
  Server-Features, vollständige Command-Palette-Liste) ist NICHT Teil der 
  vorliegenden MobileIDE-Unterlagen und MUSS in Stufe 0 live im Quellverzeichnis 
  ~/Xed-Editor ermittelt werden – keine Annahmen über Klassen-/Dateinamen treffen, 
  die nicht durch tatsächliche Inspektion bestätigt sind.
- Lizenz-Header-Konvention (siehe build.gradle.kts, settings.gradle.kts, LICENSE): 
  GPLv3, Copyright scto <tschmid35@gmail.com> – bei neu erstellten Dateien konsistent 
  fortführen, falls Header-Konvention im jeweiligen Modul bereits genutzt wird.

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER (0 bis 6), nicht vermischt. Nach jeder 
Stufe: Gesamtprojekt bauen (Gradle), Fehlerfreiheit bestätigen, Ergebnis-Report mit 
geänderten/migrierten Dateien liefern, bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme & Modul-Verifikation (Pflicht zuerst, keine Code-Änderung)

0.1. Öffne `features/git` im MobileIDE-Projektverzeichnis und prüfe den tatsächlichen 
   Inhalt: Ist bereits eine `build.gradle.kts` vorhanden? Ist die Package-Struktur 
   bereits vollständig auf `com.scto.mobile.ide.features.git` migriert (laut 
   migrate_xed.py-Skript sollte dies der Fall sein)? Sind `GitPanel.kt`, 
   `GitConfigDialog.kt`, `GitViewModel.kt`, `GitToolbarCompact`, 
   `GitConflictParser.kt`, `GitConflictManager.kt`, `GitConflictResolutionDialog.kt` 
   tatsächlich in diesem Modul enthalten, oder liegen sie noch an einem anderen Ort 
   (z. B. `:core:main`)?

0.2. Öffne `core/commands` und verifiziere, ob `CommandManager.kt`, `CommandProvider`, 
   sowie Basisklassen `GlobalCommand`/`EditorCommand` dort tatsächlich wie in 
   status.md (2026-07-04) beschrieben vorhanden sind, und ob bereits IRGENDWELCHE 
   Kern-IDE-Commands (nicht nur Plugin-Commands wie die Typst-Beispiele) registriert 
   sind.

0.3. Prüfe ~/Xed-Editor (Original-Quellprojekt) live und erstelle eine Gegenüber-
   stellungstabelle "Xed-Editor-Feature → Status in MobileIDE (bereits migriert laut 
   migrate_xed.py / teilweise / fehlt komplett)" für folgende Bereiche: 
   Editor-Settings, Terminal-Settings (insbesondere Font-FAMILY-Auswahl über die 
   bestehende Font-SIZE-Auswahl hinaus, sowie Theme-Auswahl über die 5 bestehenden 
   Farbschemata hinaus), Git-Settings (zusätzliche Optionen jenseits Username/E-Mail/
   Token/SSH-Key), LSP-Settings (zusätzliche Optionen jenseits der 4 Status-Checks), 
   Extensions-Settings, Speech-Server-Settings (Existenz in Xed-Editor ZUERST 
   verifizieren, da in KEINER MobileIDE-Feature-Liste erwähnt), sowie sämtliche 
   Editor-Kernfunktionen (Multi-Cursor, Suchen&Ersetzen, Code-Folding, Snippets, 
   Diff-View, Split-Editor, Bracket-Matching, Auto-Save etc.) und die vollständige 
   Command-Palette-Implementierung in Xed-Editor (Liste aller dort registrierten 
   Befehle).

0.4. Liefere diese Bestandsaufnahme als eigenständigen Zwischen-Report, BEVOR mit 
   Stufe 1 fortgefahren wird. Kennzeichne darin explizit alle Fälle, in denen der 
   Xed-Editor-Quellcode von der Auftragsbeschreibung abweicht (z. B. falls 
   "Speachserver" dort nicht existiert oder anders heißt), sowie alle Fälle, in denen 
   README.md-TODO-Einträge laut tatsächlichem Code bereits veraltet/erledigt sind.

---

## STUFE 1 – Git-Modul & Command-Modul reaktivieren und sichtbar machen

1.1. Entferne die Auskommentierung `// include(":features:git")` in 
   `settings.gradle.kts` und aktiviere das Modul.

1.2. Behebe alle beim ersten Build auftretenden Kompilierungsfehler in 
   `features/git` (fehlende Dependencies im modul-eigenen build.gradle.kts, fehlende 
   Version-Catalog-Einträge wie `jgit` – die Version `6.2.0.202206071550-r` ist 
   bereits projektweit im Plugin-Version-Catalog bekannt, muss aber im Root-
   Version-Catalog `gradle/libs.versions.toml` ggf. ergänzt werden).

1.3. Binde `:features:git` als Dependency in `app/build.gradle.kts` ein.

1.4. Stelle sicher, dass `GitPanel.kt` (bzw. das tatsächliche Haupt-UI-Composable des 
   Moduls) tatsächlich in die App-Navigation eingebunden ist: Prüfe, ob ein 
   Navigations-Einstiegspunkt existiert (z. B. im Filetree-Sidepanel gemäß Prompt 17 
   "sidepanel-settings", oder als eigener Tab im `ToolingBottomSheet` analog zu 
   Debug/Docs/AI-Tabs). Falls NavHost-Route oder UI-Trigger fehlt: ergänze diesen 
   (Route-Konvention: "git" oder "features/git" analog zu "settings/terminal"), unter 
   Verwendung von `NavigationUtils.safeNavigate`.

1.5. Reaktiviere ebenso `// include(":core:commands")` in settings.gradle.kts, behebe 
   Build-Fehler entsprechend, und binde das Modul in `app/build.gradle.kts` ein.

1.6. Build- & Funktionsverifikation: Git-Bereich ist über die App-Navigation 
   erreichbar, zeigt Repository-Status (Branch, geänderte Dateien) eines Test-
   Projekts, `GitConfigDialog` ermöglicht Eingabe/Speicherung von Username/E-Mail/
   Token, und ein Test-Commit/Push funktioniert mit diesen Credentials. Die bereits 
   implementierte Conflict-Resolution (Prompt 12) ist über die UI erreichbar.

---

## STUFE 2 – Fehlende Settings-Screens migrieren (Editor, Terminal-Ergänzungen, 
   Git-Ergänzungen, LSP-Ergänzungen, Extensions, Speech-Server)

WICHTIG: NICHT die bereits vorhandenen TerminalSettingsScreen/LspSettingsScreen/ 
BuildSettingsScreen/Editor-Settings/GitConfigDialog neu erstellen – nur ERGÄNZEN um 
das, was Stufe 0.3 als tatsächlich fehlend identifiziert hat.

2a. Editor-Settings-Ergänzung: Portiere aus Xed-Editor alle in Stufe 0.3 als fehlend 
   identifizierten Editor-Einstellungsoptionen (z. B. Tab-Größe, Auto-Indent, 
   Zeilennummern-Anzeige-Optionen, Whitespace-Anzeige, Soft-Keyboard-Verhalten) in 
   den bestehenden Editor-Settings-Bereich, unter Anpassung von Package-Namen/imports 
   auf com.scto.mobile.ide.

2b. Terminal-Settings-Ergänzung: Füge, sofern in Xed-Editor vorhanden und in 
   MobileIDE fehlend, eine freie Schriftart-Auswahl (Font-Family, nicht nur 
   Font-Size) zum bestehenden TerminalSettingsScreen hinzu, ohne bestehende 
   Farbschema-/Reset-Reinstall-Logik zu verändern.

2c. Git-Settings-Ergänzung: Prüfe, ob GitConfigDialog aus Xed-Editor-Sicht noch 
   fehlende Konfigurationsoptionen besitzt (z. B. Default-Branch-Name, Auto-Fetch-
   Intervall, Commit-Signierung via GPG) und ergänze diese im bestehenden 
   GitConfigDialog/GitViewModel, ohne Username/E-Mail/PAT/SSH-Key-Verwaltung zu 
   verändern.

2d. LSP-Settings-Ergänzung: Prüfe, ob Xed-Editor zusätzliche LSP-Konfigurations-
   optionen bietet (z. B. pro-Sprache Server-Pfad-Override, Log-Level, Timeout-Werte), 
   die im bestehenden LspSettingsScreen (aktuell Java/Kotlin/Bash/XML Status-Checks) 
   fehlen, und ergänze diese entsprechend.

2e. Extensions-Settings: Erstelle einen ExtensionsSettingsScreen (falls in Stufe 0 
   als fehlend bestätigt), der die bereits über `:features:extensions` bzw. das 
   Plugin-System (siehe Plugins-Development.md, manifest.json-Schema, 
   `assets/bundled_plugins/<id>`) verwalteten Erweiterungen auflistet, mit 
   Installieren/Deinstallieren/Aktivieren-Umschaltern, konsistent mit dem bestehenden 
   Plugin-Loading-Mechanismus.

2f. Speech-Server-Settings: NUR umsetzen, falls Stufe 0.3 bestätigt, dass Xed-Editor 
   tatsächlich eine Text-to-Speech/Speech-to-Text-Funktion besitzt. Falls bestätigt: 
   erstelle SpeechServerSettingsScreen mit den dort vorhandenen Konfigurationsoptionen 
   (Stimme, Sprache, Geschwindigkeit o. Ä.), inkl. Portierung der zugrundeliegenden 
   Service-Anbindung. Falls NICHT bestätigt: Report explizit vermerken und diesen 
   Punkt als "nicht anwendbar, da im Quellprojekt nicht vorhanden" markieren statt 
   etwas zu erfinden.

2g. Navigation: Registriere alle neuen/erweiterten Settings-Screens im NavHost mit 
   dem bestehenden Routen-Muster ("settings/extensions", "settings/speech" etc.), 
   Verwendung von `NavigationUtils.safeNavigate`.

2h. Build- & Funktionsverifikation nach jedem Einzel-Screen.

---

## STUFE 3 – Fehlende Editor-Kernfunktionen migrieren

3.1. Nutze die Gegenüberstellungstabelle aus Stufe 0.3: bearbeite ausschließlich als 
   "fehlt" markierte Kernfunktionen (jenseits reiner Settings, siehe Stufe 2a).

3.2. Für jede Funktion: prüfe zuerst, ob sora-editor (bereits Basis des :editor-
   Moduls) die Funktion nativ unterstützt und nur die Verdrahtung fehlt (bevorzugte 
   Lösung), sonst portiere die Zusatzlogik aus Xed-Editor unter Anpassung der 
   package-Deklarationen nach com.scto.mobile.ide.editor (bzw. passendes Untermodul, 
   ggf. :editor-lsp falls LSP-Bezug besteht).

3.3. Erweitere MobileIDE nötigenfalls um fehlende Abhängigkeiten (build.gradle.kts 
   des :editor-Moduls), ausschließlich Versionen konsistent zum bestehenden 
   Version-Catalog-Muster.

3.4. Binde neue Editor-Funktionen in die bestehende Editor-Toolbar/Kontextmenü in 
   `CodeEditScreen.kt` ein, konsistent mit dem bestehenden Look&Feel.

3.5. Build- & Funktionsverifikation.

---

## STUFE 4 – Terminal-Erweiterung: Session-Lifecycle, Zurück-Navigation, State-Restore

Arbeite ausschließlich im bereits konsolidierten Modul :features:terminal (für 4b/4c 
global im :app-Modul).

4a. Session-Schließen-Bestätigung: Ergänze eine Prüfung, ob beim Schließen der 
   aktuellen Terminal-Session oder beim Öffnen eines neuen Terminals ein aktiver 
   Prozess in der bisherigen Session läuft. Falls ja: zeige einen Bestätigungsdialog 
   ("Sitzung wird beendet – fortfahren?"). Dies ergänzt die bestehende "Default Close 
   Last Session Behavior" (aktuell `new_session`, siehe status.md 2026-07-01), 
   ersetzt sie nicht. Respektiere dabei das bestehende entkoppelte Callback-Muster von 
   `TerminalBackEnd.kt` (Vermeidung zirkulärer Abhängigkeiten zwischen :core:main und 
   :app).

4b. System-Zurück-Taste (global, im :app-Modul, nicht nur Terminal-spezifisch):
   - Implementiere in MainActivity/zentralem NavHost-Handler: Zurück-Taste navigiert 
     zum vorherigen Screen (Standard-NavController-Popverhalten) auf allen Screens 
     AUSSER dem Haupt-/Root-Screen (MainScreen/ProjectListScreen).
   - Auf dem Haupt-Screen: Zurück-Taste zeigt einen Bestätigungsdialog "App beenden?" 
     mit zusätzlicher Frage/Option "Geöffnete Dateien im Editor und aktuellen 
     App-Zustand speichern?" (Ja/Nein) vor tatsächlichem `finish()`.
   - Zu speichernder Zustand: alle offenen Editor-Tabs (Pfad, Cursor-/Scroll-
     Position), zuletzt aktives Projekt-Verzeichnis, working directories offener 
     Terminal-Sessions. Persistierung als JSON-Datei im App-internen Speicher (NICHT 
     flüchtiges SharedPreferences), damit Neustarts über mehrere Tage überstanden 
     werden.

4c. State-Wiederherstellung beim App-Start: Falls gespeicherter Zustand vorhanden, 
   zeige vor dem regulären Start-Screen einen Dialog "Vorherige Sitzung 
   wiederherstellen?" (Wiederherstellen/Neu beginnen; bei "Neu beginnen" wird der 
   gespeicherte Zustand verworfen). Bei "Wiederherstellen": öffne alle gespeicherten 
   Editor-Tabs im gespeicherten Projekt, navigiere zur zuletzt aktiven Ansicht, 
   stelle Terminal-Sessions mit denselben working directories wieder her (neue 
   Prozesse, gleicher Kontext – kein vollständiges Prozess-Rehosting nötig, da 
   PRoot-Prozesse beim App-Kill ohnehin terminieren).

4d. Build- & Funktionsverifikation: kompletten Zyklus manuell durchspielen (App mit 
   offenen Tabs/Terminal schließen inkl. Speichern-Bestätigung, Neustart, 
   "Wiederherstellen" wählen, Zustand muss korrekt reproduziert werden). Zurück-
   Tasten-Verhalten auf allen Screens außer Root muss zum vorherigen Screen 
   navigieren, nicht die App beenden.

---

## STUFE 5 – Vollständige Command-Palette-Commands

5.1. Nutze die in Stufe 0.2 verifizierte, bereits vorhandene CommandManager-
   Infrastruktur aus (jetzt reaktiviertem) :core:commands – KEINE Parallelstruktur 
   aufbauen.

5.2. Erstelle eine vollständige Liste aller Befehle, die in einer produktiv nutzbaren 
   Command Palette einer Android-IDE erwartbar sind, und die aktuell noch NICHT als 
   registrierte GlobalCommand/EditorCommand-Instanz existieren (Muster: bereits 
   vorhandene Plugin-Commands wie `TypstCompilePdfCommand`/`TypstUninstallCommand` mit 
   `id`, `prefix`, `getLabel()` via String-Resource, `getIcon()`, ggf. 
   `isSupported()`/`isEnabled()`, `action(context)`). Dazu zählen mindestens:
   - Datei: Neue Datei, Neuer Ordner, Speichern, Alle speichern, Schließen, 
     Alle schließen, Umbenennen, Löschen, im Dateimanager anzeigen.
   - Editor: Suchen, Suchen&Ersetzen, Gehe zu Zeile, Kommentar umschalten, Zeile 
     duplizieren/löschen/verschieben, Einrückung, Undo/Redo, Alles auswählen, 
     Schriftgröße ändern, Word Wrap umschalten.
   - Projekt/Build: Bauen (Debug/Release), Ausführen, Build abbrechen, Gradle-Sync, 
     Gradle-Cache leeren (Anbindung an `GradleCacheAnalyzer.kt` aus Prompt 13), 
     Clean Build.
   - Terminal: Neues Terminal, Terminal schließen, Fokus wechseln.
   - Git: Commit, Push, Pull, Branch wechseln, Status, Diff, Konflikt-Ansicht öffnen 
     (Anbindung an das jetzt in Stufe 1 aktivierte :feat
