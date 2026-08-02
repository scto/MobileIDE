# Ziel
Vervollständige die bereits begonnene Migration von Xed-Editor-Funktionalität nach 
MobileIDE (com.scto.mobile.ide): fehlende Settings-Screens, fehlende Editor-Funktionen, 
Terminal-Erweiterungen (Theme/Font/Session-Lifecycle/Back-Navigation/State-Restore), 
vollständige Command-Palette-Commands, und mache die bereits vorhandene Git-Funktionalität 
in der App tatsächlich sichtbar.

# Kontext (verifizierter Ist-Zustand von ~/MobileIDE, Stand PROGRESS.md/status.md/
settings.gradle.kts)
- rootProject.name = "MobileIDE", Basis-Package: com.scto.mobile.ide
- Bereits abgeschlossene, im Repo aktive Migrationsarbeit von Xed-Editor 
  (siehe migrate_xed.py, migrate_xed2.py, fix_xed.py im Root): Terminal, Git, Runner 
  und Extensions-Code wurden bereits von com.rk.* nach com.scto.mobile.ide.features.* 
  umgezogen (Package-Mapping: com.rk.terminal → com.scto.mobile.ide.features.terminal, 
  com.rk.git → com.scto.mobile.ide.features.git, com.rk.runner → 
  com.scto.mobile.ide.features.runner, com.rk.extension → 
  com.scto.mobile.ide.features.extension).
- :features:terminal ist LAUT PROGRESS.md (Prompt 15, "Consolidate Terminal Features 
  Module") bereits vollständig konsolidiert: Session-Backend, ANSI/PTY-Emulator, 
  View-Rendering, TerminalScreen + TerminalSettingsScreen, TerminalService, Sandbox- 
  Setup-Assets (ideenv, idesetup, init.sh, setup.sh, 5 Farbschemata: default, dracula, 
  solarized_dark, nord, monokai). Font-Size-Slider (10-30sp) existiert bereits.
- WICHTIGER BEFUND: Ein Verzeichnis `features/git` existiert bereits physisch im 
  Projekt (Beleg: migrate_xed.py referenziert und bearbeitet 
  os.path.join(base, 'git')), das Modul ist aber in settings.gradle.kts AUSKOMMENTIERT: 
  `// include(":features:git")`. Ebenso ist `// include(":core:commands")` 
  auskommentiert, obwohl CommandManager laut status.md (2026-07-04) bereits 
  implementiert wurde ("Command Registration API: Fully implemented CommandManager.kt 
  inside :core:commands").
- Bereits laut PROGRESS.md (2026-07-27, Prompt 17 "git-credentials-sidepanel-settings") 
  implementiert: GitConfigDialog.kt (Username/E-Mail/PAT/SSH-Key-Verwaltung inkl. 
  Connectivity-Test via JGit ls-remote), GitViewModel.kt, GitPanel.kt, 
  GitToolbarCompact (Sidepanel-Settings-Launcher). Diese MÜSSEN NICHT neu erstellt 
  werden – nur reaktiviert/verdrahtet/sichtbar gemacht werden.
- Bereits vorhandene Settings-Screens: TerminalSettingsScreen, LspSettingsScreen 
  (Java/Kotlin/Bash/XML Status), BuildSettingsScreen (JDK/Gradle/SDK/Build-Tools/ 
  CMake/NDK), EditorScreen-eigene Settings (Font Size Slider, LSP-Editor-Toggle 
  TreeSitter/TextMate).
- NOCH NICHT vorhanden (zu prüfen/erstellen): dedizierter ExtensionsSettingsScreen, 
  dedizierter SpeechServerSettingsScreen (Text-to-Speech/Speech-to-Text ist im 
  aktuellen Feature-Stand NICHT gelistet – vor Umsetzung in Xed-Editor-Quelle 
  verifizieren, ob dieses Feature dort tatsächlich existiert).
- Weitere relevante Module: :app, :editor, :editor-lsp, :language-treesitter, 
  :core:main, :core:components, :core:resources, :core:runner, :core:apk-builder, 
  :core:tooling:tooling-api/-impl/-server, :features:extensions, :core:lsp, 
  :extension-languages, :core:common, :features:layout-preview, :features:plugin-store.
- Zentrale Navigation-Utility: `com.scto.mobile.ide.core.common.utils.NavigationUtils.
  safeNavigate` (siehe Fixing.md) – bei allen neuen Navigations-Aufrufen verwenden.
- Bekanntes Bug-Fix-Muster für Terminal-Session-Close: TerminalBackEnd.kt nutzt bereits 
  einen entkoppelten Callback-Mechanismus, um zirkuläre Abhängigkeiten zwischen 
  :core:main und :app zu vermeiden – bei Erweiterungen dieses Muster respektieren.
- README.md TODO-Liste bestätigt: Interactive Visual Debugger, LSP-Diagnostics-Overlays 
  (teils schon in Prompt 05 umgesetzt laut PROGRESS.md), Gradle Sync/Dependency 
  Downloader, GUI Keystore Wizard (teils in Prompt 03 umgesetzt), Git Conflict 
  Resolution Tool (bereits in Prompt 12 umgesetzt), Plugin Market – ABER kein 
  expliziter Hinweis auf fehlende Command-Palette-Vollständigkeit oder Zurück-Taste- 
  Verhalten, diese sind also echte Lücken, die dieser Prompt schließt.
- Quelle für die eigentliche Feature-Migration ist ~/Xed-Editor (öffentliches Android- 
  Text-Editor-Projekt). Der exakte interne Aufbau (Settings-Screen-Klassennamen, 
  konkrete Editor-Zusatzfunktionen, Terminal-Font-Auswahl-Mechanismus, Existenz eines 
  Speech-Server-Features) ist NICHT Teil der vorliegenden MobileIDE-Unterlagen und MUSS 
  in Stufe 0 live im Quellverzeichnis ~/Xed-Editor ermittelt werden – keine Annahmen 
  über Klassen-/Dateinamen treffen, die nicht durch tatsächliche Inspektion bestätigt 
  sind.

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER (0 bis 6), nicht vermischt. Nach jeder 
Stufe: Gesamtprojekt bauen (Gradle), Fehlerfreiheit bestätigen, Ergebnis-Report mit 
geänderten/migrierten Dateien liefern, bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme & Modul-Verifikation (Pflicht zuerst, keine Code-Änderung)

0.1. Öffne `features/git` im MobileIDE-Projektverzeichnis und prüfe den tatsächlichen 
   Inhalt (build.gradle.kts vorhanden? Package-Struktur bereits auf com.scto.mobile.
   ide.features.git migriert? Compose-Screens für Git-UI bereits vorhanden, z. B. 
   GitPanel.kt, GitConfigDialog.kt, GitViewModel.kt, GitToolbarCompact?).

0.2. Öffne `core/commands` (bzw. den tatsächlichen Pfad, sofern abweichend von 
   :core:commands) und verifiziere, ob `CommandManager.kt`, `CommandProvider`, 
   `GlobalCommand`/`EditorCommand`-Basisklassen dort bereits wie in status.md 
   beschrieben vorhanden sind.

0.3. Prüfe ~/Xed-Editor (Original-Quellprojekt) live und erstelle eine Gegenüber- 
   stellungstabelle "Xed-Editor-Feature → Status in MobileIDE (bereits migriert laut 
   migrate_xed.py / teilweise / fehlt komplett)" für folgende Bereiche: Editor-Settings, 
   Terminal-Settings (insbesondere Font-Auswahl über Schriftgröße hinaus, Theme- 
   Auswahl über die 5 bestehenden Farbschemata hinaus), Git-Settings, LSP-Settings, 
   Extensions-Settings, Speech-Server-Settings (Existenz in Xed-Editor zuerst 
   verifizieren, da NICHT in der MobileIDE-Feature-Liste erwähnt), sowie sämtliche 
   Editor-Kernfunktionen (Multi-Cursor, Suchen&Ersetzen, Code-Folding, Snippets, 
   Diff-View, Split-Editor, Bracket-Matching, Auto-Save etc.) und die vollständige 
   Command-Palette-Implementierung in Xed-Editor.

0.4. Liefere diese Bestandsaufnahme als eigenständigen Zwischen-Report, BEVOR mit 
   Stufe 1 fortgefahren wird. Kennzeichne darin explizit alle Fälle, in denen der 
   Xed-Editor-Quellcode von der Auftragsbeschreibung abweicht (z. B. falls 
   "Speachserver" dort nicht existiert oder anders heißt).

---

## STUFE 1 – Git-Modul reaktivieren & sichtbar machen

1.1. Entferne die Auskommentierung `// include(":features:git")` in 
   `settings.gradle.kts` und aktiviere das Modul.

1.2. Behebe alle beim ersten Build auftretenden Kompilierungsfehler in 
   `features/git` (fehlende Dependencies im build.gradle.kts des Moduls, fehlende 
   Version-Catalog-Einträge wie `jgit`, die laut Plugin-libs.versions.toml bereits 
   projektweit als Version bekannt sind – im Root-Version-Catalog ergänzen falls 
   nicht vorhanden).

1.3. Binde `:features:git` als Dependency in `app/build.gradle.kts` ein.

1.4. Stelle sicher, dass GitPanel.kt (oder das tatsächliche Haupt-UI-Composable des 
   Moduls) tatsächlich in die App-Navigation eingebunden ist: Prüfe, ob ein 
   Navigations-Einstiegspunkt (z. B. im Filetree-Sidepanel gemäß Prompt 17 
   "sidepanel-settings", oder als eigener Tab im ToolingBottomSheet analog zu 
   Debug/Docs/AI-Tabs) existiert. Falls NavHost-Route oder UI-Trigger fehlt: ergänze 
   diesen (Route-Konvention: "git" oder "features/git" analog zu "settings/terminal").

1.5. Reaktiviere ebenso `// include(":core:commands")` in settings.gradle.kts, behebe 
   Build-Fehler entsprechend, und binde das Modul in app/build.gradle.kts ein.

1.6. Build- & Funktionsverifikation: Git-Bereich ist über die App-Navigation 
   erreichbar, zeigt Repository-Status (Branch, geänderte Dateien) eines Test-Projekts, 
   GitConfigDialog ermöglicht Eingabe/Speicherung von Username/E-Mail/Token, und ein 
   Test-Commit/Push funktioniert mit diesen Credentials.

---

## STUFE 2 – Fehlende Settings-Screens migrieren (Editor, Terminal-Ergänzungen, 
   Git-Ergänzungen, LSP-Ergänzungen, Extensions, Speech-Server)

WICHTIG: NICHT die bereits vorhandenen TerminalSettingsScreen/LspSettingsScreen/ 
BuildSettingsScreen/Editor-Settings/GitConfigDialog neu erstellen – nur ERGÄNZEN um 
das, was Stufe 0.3 als fehlend identifiziert hat.

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
   Intervall, Signierung von Commits via GPG) und ergänze diese im bestehenden 
   GitConfigDialog/GitViewModel, ohne Username/E-Mail/PAT/SSH-Key-Verwaltung zu 
   verändern.

2d. LSP-Settings-Ergänzung: Prüfe, ob Xed-Editor zusätzliche LSP-Konfigurations- 
   optionen bietet (z. B. pro-Sprache Server-Pfad-Override, Log-Level, Timeout-Werte), 
   die im bestehenden LspSettingsScreen (aktuell Java/Kotlin/Bash/XML Status-Checks) 
   fehlen, und ergänze diese entsprechend.

2e. Extensions-Settings: Erstelle einen ExtensionsSettingsScreen (falls in 
   Stufe 0 als fehlend bestätigt), der die bereits über `:features:extensions` bzw. 
   das Plugin-System (siehe Plugins-Development.md, manifest.json-Schema) verwalteten 
   Erweiterungen auflistet, mit Installieren/Deinstallieren/Aktivieren-Umschaltern, 
   konsistent mit dem bestehenden Plugin-Loading-Mechanismus 
   (`assets/bundled_plugins/<id>`).

2f. Speech-Server-Settings: NUR umsetzen, falls Stufe 0.3 bestätigt, dass Xed-Editor 
   tatsächlich eine Text-to-Speech/Speech-to-Text-Funktion besitzt. Falls bestätigt: 
   erstelle SpeechServerSettingsScreen mit den dort vorhandenen Konfigurationsoptionen 
   (Stimme, Sprache, Geschwindigkeit o. Ä.), inkl. Portierung der zugrundeliegenden 
   Service-Anbindung. Falls NICHT bestätigt: Report explizit vermerken und diesen 
   Punkt als "nicht anwendbar, da im Quellprojekt nicht vorhanden" markieren statt 
   etwas zu erfinden.

2g. Navigation: Registriere alle neuen/erweiterten Settings-Screens im NavHost mit dem 
   bestehenden Routen-Muster ("settings/extensions", "settings/speech", etc.), 
   Verwendung von `NavigationUtils.safeNavigate`.

2h. Build- & Funktionsverifikation nach jedem Einzel-Screen.

---

## STUFE 3 – Fehlende Editor-Kernfunktionen migrieren

3.1. Nutze die Gegenüberstellungstabelle aus Stufe 0.3: bearbeite ausschließlich als 
   "fehlt" markierte Kernfunktionen (jenseits reiner Settings, siehe Stufe 2a).

3.2. Für jede Funktion: prüfe zuerst, ob sora-editor (bereits Basis des :editor-Moduls) 
   die Funktion nativ unterstützt und nur die Verdrahtung fehlt (bevorzugte Lösung), 
   sonst portiere die Zusatzlogik aus Xed-Editor unter Anpassung der package- 
   Deklarationen nach com.scto.mobile.ide.editor (bzw. passendes Untermodul, ggf. 
   :editor-lsp falls LSP-Bezug besteht).

3.3. Erweitere MobileIDE nötigenfalls um fehlende Abhängigkeiten (build.gradle.kts 
   des :editor-Moduls), ausschließlich Versionen konsistent zum bestehenden 
   Version-Catalog-Muster.

3.4. Binde neue Editor-Funktionen in die bestehende Editor-Toolbar/Kontextmenü in 
   CodeEditScreen.kt ein, konsistent mit dem bestehenden Look&Feel.

3.5. Build- & Funktionsverifikation.

---

## STUFE 4 – Terminal-Erweiterung: Session-Lifecycle, Zurück-Navigation, State-Restore

Arbeite ausschließlich im bereits konsolidierten Modul :features:terminal 
(für 4b/4c global im :app-Modul).

4a. Session-Schließen-Bestätigung: Ergänze eine Prüfung, ob beim Schließen der 
   aktuellen Terminal-Session oder beim Öffnen eines neuen Terminals ein aktiver 
   Prozess in der bisherigen Session läuft. Falls ja: zeige einen Bestätigungsdialog 
   ("Sitzung wird beendet – fortfahren?"), analog zum bestehenden `dialog()`-Utility- 
   Muster aus core.common.utils. Dies ergänzt die bestehende "Default Close Last 
   Session Behavior" (aktuell `new_session`), ersetzt sie nicht. Respektiere dabei 
   das bestehende entkoppelte Callback-Muster von TerminalBackEnd.kt (Vermeidung 
   zirkulärer Abhängigkeiten zwischen :core:main und :app).

4b. System-Zurück-Taste (global, im :app-Modul, nicht nur Terminal-spezifisch):
   - Implementiere in MainActivity/zentralem NavHost-Handler: Zurück-Taste navigiert 
     zum vorherigen Screen (Standard-NavController-Popverhalten) auf allen Screens 
     AUSSER dem Haupt-/Root-Screen (MainScreen/ProjectListScreen).
   - Auf dem Haupt-Screen: Zurück-Taste zeigt einen Bestätigungsdialog "App beenden?" 
     mit zusätzlicher Frage/Option "Geöffnete Dateien im Editor und aktuellen 
     App-Zustand speichern?" (Ja/Nein) vor tatsächlichem `finish()`.
   - Zu speichernder Zustand: alle offenen Editor-Tabs (Pfad, Cursor-/Scroll-Position), 
     zuletzt aktives Projekt-Verzeichnis, working directories offener Terminal- 
     Sessions. Persistierung als JSON-Datei im App-internen Speicher (nicht 
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
   vorhandene Plugin-Commands wie TypstCompilePdfCommand/TypstUninstallCommand mit 
   `id`, `prefix`, `getLabel()` via String-Resource, `getIcon()`, ggf. 
   `isSupported()`/`isEnabled()`, `action(context)`). Dazu zählen mindestens:
   - Datei: Neue Datei, Neuer Ordner, Datei speichern, Alle speichern, Datei 
     schließen, Alle schließen, Datei umbenennen, Datei löschen, Datei im 
     Dateimanager anzeigen.
   - Editor: Suchen, Suchen&Ersetzen, Gehe zu Zeile, Kommentar umschalten, Zeile 
     duplizieren, Zeile löschen, Nach oben/unten verschieben, Einrückung erhöhen/
     verringern, Groß-/Kleinschreibung umschalten, Undo/Redo, Alles auswählen, 
     Editor-Schriftgröße erhöhen/verringern, Zeilenumbruch umschalten (Word Wrap).
   - Projekt/Build: Projekt bauen (Debug/Release), Projekt ausführen, Build abbrechen, 
     Gradle-Sync, Gradle-Cache leeren, Clean Build.
   - Terminal: Neues Terminal öffnen, Aktuelles Terminal schließen, Terminal fokus- 
     sieren/zurück zum Editor.
   - Git: Commit, Push, Pull, Branch wechseln, Status anzeigen, Diff anzeigen 
     (Anbindung an das jetzt in Stufe 1 aktivierte :features:git).
   - Debug: Anbindung an bestehenden DebugSessionManager (Prompt 10, laut PROGRESS.md 
     bereits implementiert) – Resume/Step Over/Step Into/Step Out/Stop als Commands.
   - Ansicht: Theme wechseln (Hell/Dunkel/System), Seitenpanel ein-/ausblenden, 
     Terminal-Panel ein-/ausblenden, Zoom zurücksetzen.
   - Einstellungen: Direktsprung zu jedem Settings-Bereich (Editor, Terminal, Git, 
     LSP, Extensions, ggf. Speech-Server) als jeweils eigener Command 
     ("Go to Editor Settings" etc.).

5.3. Implementiere jeden Command als eigene Klasse nach dem bestehenden Muster, 
   organisiert in sinnvollen Paketen unterhalb von com.scto.mobile.ide.commands.core.* 
   (thematisch gruppiert nach Datei/Editor/Projekt/Terminal/Git/Debug/Ansicht/
   Einstellungen wie oben).

5.4. Registriere alle neuen Commands zentral beim App-Start (z. B. in MainActivity 
   oder einem dedizierten `CoreCommandsInitializer`), analog zur Registrierung von 
   Plugin-Commands via `CommandProvider.registerCommand(it)`.

5.5. Stelle sicher, dass die Command-Palette-UI (laut status.md bereits "mit 
   UI-Integration" vorhanden) tatsächlich ALLE registrierten Commands durchsuchbar 
   anzeigt, inkl. Icon, Label und ggf. Prefix-Gruppierung.

5.6. Build- & Funktionsverifikation: Öffne die Command Palette und bestätige, dass 
   jeder der oben gelisteten Commands auffindbar ist und beim Ausführen die 
   erwartete Aktion auslöst (keine No-Op-Platzhalter).

---

## STUFE 6 – Abschlussverifikation (Gesamtprojekt)

6.1. Führe einen vollständigen Gradle-Build aller reaktivierten/erweiterten Module 
   durch und bestätige Fehlerfreiheit.

6.2. Manuelle Durchklick-Bestätigung des gesamten Funktionsumfangs:
   - Git-Bereich sichtbar, Status korrekt, Commit/Push/Pull mit konfigurierten 
     Credentials funktioniert.
   - Alle sechs Settings-Bereiche (Editor, Terminal, Git, LSP, Extensions, ggf. 
     Speech-Server) erreichbar und persistieren Werte korrekt.
   - Alle migrierten Editor-Kernfunktionen nutzbar.
   - Terminal fragt vor Session-Schließung/Neuöffnung bei aktivem Prozess nach.
   - Zurück-Taste-Verhalten korrekt (Zurück zu vorigem Screen, Beenden-Dialog nur 
     auf Root-Screen, State-Speicherung funktioniert).
   - App-Start bietet Session-Wiederherstellung an und stellt Zustand korrekt wieder 
     her.
   - Command Palette listet und führt alle Commands aus Stufe 5.2 korrekt aus.

6.3. Liefere einen finalen Gesamt-Report mit allen betroffenen Dateien pro Stufe.

# Nicht-Ziele
- Keine Neuerstellung bereits vorhandener Git-UI-Komponenten (GitConfigDialog, 
  GitViewModel, GitPanel, GitToolbarCompact) – nur Reaktivierung/Sichtbarmachung 
  und punktuelle Ergänzung (Stufe 2c).
- Keine Neuerstellung von CommandManager – nur Reaktivierung des Moduls und 
  Command-Ergänzung.
- Keine Speech-Server-Umsetzung, falls in Xed-Editor nicht tatsächlich vorhanden.
- Keine Änderung an der bestehenden Terminal-Konsolidierung (Session-Backend/PTY/View) 
  jenseits der in Stufe 2b und Stufe 4 explizit beschriebenen Ergänzungen.
- Keine Migration von Xed-Editor-Funktionen, die inhaltlich nicht zu den behandelten 
  Bereichen (Settings, Editor-Funktionen, Terminal-Erweiterung, Command Palette, 
  Git-Sichtbarkeit) gehören.
- Keine Business-Logik-Änderung an ApkBuilder/ApkInstaller/Gradle-Bridge jenseits 
  dessen, was zur Aktivierung von :features:git und :core:commands notwendig is