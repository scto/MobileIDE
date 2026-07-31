# Ziel
Migriere/refaktoriere ausgewählte Settings-Screens, Editor-Funktionen und Terminal-
Erweiterungen von ~/Xed-Editor nach ~/MobileIDE, vervollständige die Command-Palette-
Implementierung und stelle sicher, dass die Git-Funktionalität in der UI sichtbar ist.

# Kontext (verifizierter Ist-Zustand von ~/MobileIDE)
- Root-Package: com.scto.mobile.ide
- Relevante bestehende Module: :app, :editor, :editor-lsp, :language-treesitter, 
  :core:main, :core:components, :core:resources, :core:terminal-emulator, 
  :core:terminal-view, :core:terminal, :core:runner, :core:apk-builder, 
  :core:tooling:tooling-api, :core:tooling:tooling-impl, :core:tooling:tooling-server, 
  :core:extension, :core:lsp, :core:commands, :extension-languages, :core:common.
- Bereits vorhandene Settings-Screens laut PROGRESS.md/status.md: 
  TerminalSettingsScreen (Reset/Reinstall, Font Size 10-30sp, Farbschemata: default, 
  dracula, solarized_dark, nord, monokai), LspSettingsScreen (Java/Kotlin/Bash/XML 
  Status-Checks), BuildSettingsScreen (OpenJDK 17/21, Gradle, Android SDK, 
  Build-Tools, Platforms, CMake/NDK), EditorScreen-Settings (Font Size Slider, 
  LSP-Editor-Toggle TreeSitter/TextMate).
- Bereits vorhandene Command-Infrastruktur: `CommandManager`/`CommandProvider` in 
  :core:commands mit den Basisklassen `GlobalCommand` und `EditorCommand` (Beispiele 
  siehe Plugin `typst-lsp`: `TypstCompilePdfCommand`, `TypstUninstallCommand`, jeweils 
  mit `id`, `prefix`, `getLabel()`, `getIcon()`, `isSupported()`/`isEnabled()`, 
  `action()`).
- Git-Feature ist laut README.md als abgeschlossen gelistet ("Git Integration: Built-
  in Git version control with a visual commit history graph, supporting Clone, 
  Commit, Push, Pull, and Branch management"), dessen konkreter Modul-/Screen-Standort 
  ist im vorliegenden Codeausschnitt jedoch NICHT sichtbar und muss in Stufe 0 
  lokalisiert werden.
- Quelle für die Migration: ~/Xed-Editor (öffentliches Android-Text-Editor-Projekt mit 
  Syntaxhervorhebung, LSP-Integration, eingebautem Terminal, Extensions, laut eigener 
  Beschreibung). Der exakte interne Aufbau (Package-Namen, Dateistruktur, Settings- 
  Screen-Klassennamen) ist NICHT Teil der vorliegenden Unterlagen und MUSS in Stufe 0 
  live im Quellverzeichnis ~/Xed-Editor ermittelt werden – keine Annahmen über Klassen-
  /Dateinamen treffen, die nicht durch tatsächliche Inspektion bestätigt sind.

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER (0 bis 5), nicht vermischt. Nach jeder 
Stufe: Gesamtprojekt bauen, Fehlerfreiheit bestätigen, Ergebnis-Report mit geänderten/
migrierten Dateien liefern, bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme (Pflicht vor jeder Code-Änderung)

0.1. Durchsuche ~/Xed-Editor vollständig und erstelle eine strukturierte Übersicht 
   (Dateipfad, Zweck, Package-Name) für:
   - Alle Settings-Screens zu: Editor, Terminal, Git, LSP, Extensions, Speech-Server 
     (Text-to-Speech / Speech-to-Text – exakten Feature-Namen im Xed-Editor-Code 
     verifizieren, da "Speachserver" im Auftrag ein Tippfehler sein könnte).
   - Die zugehörigen ViewModels/Repositories/Preference-Datenhalter, die diese 
     Settings-Screens mit Logik versorgen.
   - Alle Editor-Kernfunktionen (Datei editor/, z. B. Multi-Cursor, Suchen&Ersetzen, 
     Code-Folding, Minimap, Auto-Save, Diff-Ansicht, Split-Editor, Snippets, 
     Bracket-Matching, o. Ä. – vollständige Liste aus dem tatsächlichen Code, nicht 
     geraten).
   - Die Terminal-Implementierung inkl. aller Settings-Möglichkeiten (Themes, Fonts, 
     Schriftgröße, Session-Handling-Optionen).
   - Die Command-Palette-Implementierung (Befehlsliste, Registrierungsmechanismus, 
     UI-Darstellung).
   - Die Git-Integration-Screens/Logik.

0.2. Erstelle eine Gegenüberstellungstabelle "Xed-Editor-Feature → bereits vorhanden 
   in MobileIDE? (ja/teilweise/nein) → Ziel-Modul in MobileIDE". Nutze dafür den 
   oben aufgeführten Ist-Zustand von MobileIDE als Vergleichsbasis. Für Git 
   insbesondere: lokalisiere den TATSÄCHLICHEN Ort der bestehenden Git-Funktionalität 
   in MobileIDE (vermutlich :core:main oder ein noch nicht in den vorliegenden 
   Unterlagen aufgeführtes :core:git-Modul) durch Volltextsuche nach "git", "Jgit", 
   "org.eclipse.jgit" (siehe bereits vorhandene Dependency `jgit` im Version-Catalog 
   der Plugins) im MobileIDE-Quellbaum.

0.3. Liefere diese Bestandsaufnahme als eigenständigen Report, BEVOR mit Stufe 1 
   fortgefahren wird. Kennzeichne darin explizit alle Fälle, in denen der Xed-Editor-
   Quellcode von der Auftragsbeschreibung abweicht (z. B. falls "Speachserver" dort 
   nicht existiert oder anders heißt).

---

## STUFE 1 – Migration der Settings (Editor, Terminal, Git, LSP, Extensions, 
   Speech-Server) inkl. SettingsScreens

1.1. Für jeden der sechs Bereiche (Editor, Terminal, Git, LSP, Extensions, 
   Speech-Server) einzeln vorgehen:
   - Portiere die in Stufe 0 identifizierten Settings-Datenmodelle (Preference-Keys, 
     DataStore/SharedPreferences-Strukturen) aus Xed-Editor nach MobileIDE, unter 
     Beibehaltung bereits vorhandener MobileIDE-eigener Einstellungen im selben 
     Bereich (z. B. TerminalSettingsScreen: NICHT die bestehenden Farbschemata/
     Reset/Reinstall-Funktionen ersetzen, sondern um aus Xed-Editor fehlende Optionen 
     ERGÄNZEN).
   - Portiere die zugehörigen Compose-SettingsScreens strukturell (UI-Layout, 
     verwendete Preference-Komponenten aus :core:components), passe dabei package- 
     Deklarationen und alle imports von der Xed-Editor-Namensgebung auf 
     com.scto.mobile.ide.* (bzw. das jeweils sinnvolle Untermodul-Package) an.
   - Löse alle Abhängigkeiten zu Xed-Editor-spezifischen Utility-Klassen auf, indem 
     entweder (a) das MobileIDE-Äquivalent verwendet wird, falls vorhanden 
     (z. B. eigenes Dialog-Utility aus core.common.utils, siehe `dialog()`-Funktion 
     aus dem Typst-Plugin-Beispiel), oder (b) die Utility-Klasse mitmigriert und in 
     ein passendes MobileIDE-Modul (bevorzugt :core:common oder :core:components) 
     integriert wird.
   - Für "Git"-Settings speziell: Erstelle einen neuen GitSettingsScreen (z. B. unter 
     dem in Stufe 0.2 identifizierten Git-Zielmodul, oder neu unter :app/.../settings/ 
     falls Git bislang nur ohne eigenen Settings-Screen implementiert war) mit 
     mindestens folgenden Einstellungsmöglichkeiten:
       * Benutzername (globaler Git user.name)
       * E-Mail-Adresse (globaler Git user.email)
       * GitHub Personal Access Token (sicher gespeichert, siehe Punkt 1.2)
     Diese Werte müssen beim Ausführen von Commit/Push/Pull-Operationen der 
     bestehenden JGit-Integration tatsächlich verwendet werden (Anbindung an die 
     bestehende Git-Business-Logik prüfen und herstellen, falls diese aktuell 
     hartkodierte oder fehlende Credentials nutzt).

1.2. Sicherheit für GitHub-Token: Speichere das Token NICHT im Klartext in 
   SharedPreferences. Nutze stattdessen Android Keystore-gestützte verschlüsselte 
   Speicherung (z. B. androidx.security.crypto EncryptedSharedPreferences, sofern 
   bereits eine vergleichbare Dependency im Projekt vorhanden ist – andernfalls diese 
   im Version-Catalog ergänzen). Zeige das Token im UI standardmäßig maskiert an 
   (Passwort-Feld-Stil mit Sichtbarkeits-Toggle).

1.3. Aktualisiere die Navigation (NavHost-Routen im :app-Modul) um die neuen/
   erweiterten Settings-Screens, konsistent mit der bestehenden Routen-Konvention 
   (siehe Fixing.md: Route-String-Muster "settings/terminal" etc.) – also z. B. 
   "settings/git", "settings/extensions", "settings/speech".

1.4. Build- & Funktionsverifikation: Gesamtprojekt bauen, alle sechs Settings-
   Screens müssen erreichbar sein und ihre Werte persistent speichern/laden.

---

## STUFE 2 – Migration fehlender Editor-Funktionen

2.1. Nutze die in Stufe 0.2 erstellte Gegenüberstellungstabelle: Bearbeite NUR die 
   dort als "nein" oder "teilweise" markierten Editor-Funktionen aus Xed-Editor.

2.2. Für jede fehlende Funktion einzeln:
   - Prüfe zuerst, ob die zugrundeliegende sora-editor-Bibliothek (bereits in 
     MobileIDE integriert laut :editor-Modul) die Funktion nativ unterstützt und in 
     MobileIDE bisher nur nicht verdrahtet/aktiviert wurde (in diesem Fall: nur 
     Verdrahtung ergänzen, keine Neuimplementierung).
   - Falls die Funktion in Xed-Editor als eigene Zusatzlogik über sora-editor hinaus 
     implementiert ist: portiere diese Logik unter Anpassung von package-
     Deklarationen/imports nach com.scto.mobile.ide, integriert in das :editor-Modul 
     bzw. :editor-lsp falls LSP-Bezug besteht.
   - Erweitere MobileIDE nötigenfalls um fehlende Abhängigkeiten (build.gradle.kts 
     des :editor-Moduls), ausschließlich Versionen konsistent zum bestehenden 
     Version-Catalog-Muster.
   - Binde neue Editor-Funktionen an bestehende UI-Einstiegspunkte an (Editor-Toolbar/ 
     Kontextmenü in CodeEditScreen.kt), konsistent mit dem bestehenden Look&Feel.

2.3. Build- & Funktionsverifikation nach Abschluss aller migrierten Funktionen.

---

## STUFE 3 – Terminal-Erweiterung: Settings, Session-Lifecycle, Back-Navigation, 
   State-Wiederherstellung

WICHTIG: Bearbeite dies im Zielmodul :core:terminal (+ :core:terminal-view, 
:core:terminal-emulator), NICHT in einem nicht-existierenden ":features:terminal".

3a. Fehlende Anzeige-/Personalisierungs-Optionen ergänzen:
   - Prüfe anhand von Stufe 0, welche Xed-Editor-Terminal-Einstellungen (Themes, 
     Fonts/Schriftart-Auswahl, Schriftgröße) in MobileIDEs TerminalSettingsScreen 
     NOCH FEHLEN (Schriftgröße 10-30sp und 5 Farbschemata existieren laut status.md 
     bereits – hier speziell die Ergänzung um freie Font-/Schriftart-Auswahl, sofern 
     in Xed-Editor vorhanden und in MobileIDE fehlend).
   - Ergänze diese fehlenden Optionen im bestehenden TerminalSettingsScreen, ohne die 
     bereits funktionierenden Optionen zu verändern oder zu duplizieren.

3b. Session-Schließen-Bestätigung:
   - Ergänze eine Prüfung/Bestätigungsabfrage, wenn eine Terminal-Session beendet 
     wird ODER wenn ein neues Terminal geöffnet wird, während eine aktive Session mit 
     laufendem Prozess existiert (z. B. laufender Build- oder Server-Prozess in der 
     Session). Zeige in diesem Fall einen Bestätigungsdialog ("Sitzung wird beendet – 
     fortfahren?"), analog zum bestehenden `dialog()`-Utility-Muster aus 
     core.common.utils.
   - Beachte die bestehende Einstellung "Default Close Last Session Behavior" 
     (aktuell: `new_session` statt `exit_app`, siehe status.md 2026-07-01) – die neue 
     Bestätigungsabfrage ergänzt diese bestehende Logik, ersetzt sie aber nicht.

3c. System-Zurück-Taste (globales App-Verhalten, nicht nur Terminal):
   - Implementiere global im :app-Modul (MainActivity bzw. zentraler NavHost-Handler), 
     dass der System-Zurück-Button NICHT die App beendet, sondern zum vorherigen 
     Screen im Navigations-Stack zurückspringt (Standard-NavController-Popverhalten), 
     AUSSER auf dem Haupt-/Start-Screen (MainScreen bzw. ProjectListScreen als 
     Root-Ziel).
   - Auf dem Haupt-Screen (Root der Navigation) angekommen: Zeige bei Betätigung der 
     Zurück-Taste einen Bestätigungsdialog "App beenden?" mit zusätzlicher Option/
     Checkbox oder zweitem Dialog-Schritt: "Geöffnete Dateien im Editor und den 
     aktuellen App-Zustand speichern?" (Ja/Nein), bevor die App tatsächlich beendet 
     wird (nur bei Bestätigung `finish()`/Prozessende auslösen).
   - Der zu speichernde "App-Zustand" umfasst mindestens: Liste aller aktuell 
     geöffneten Editor-Tabs (Dateipfade + Cursor-Position + Scroll-Position je Tab), 
     das zuletzt aktive Projekt-Verzeichnis, sowie – falls technisch sinnvoll trennbar 
     vom reinen Editor-Zustand – den Zustand offener Terminal-Sessions (mindestens 
     working directory je Session, kein vollständiges Prozess-Rehosting nötig, da 
     PRoot-Prozesse ohnehin beim App-Kill terminieren).
   - Persistiere diesen Zustand (z. B. als JSON via DataStore/eigene Datei im 
     App-internen Speicher), NICHT in flüchtigem SharedPreferences-Cache, damit er 
     App-Neustarts über mehrere Tage hinweg übersteht.

3d. Session-/State-Wiederherstellung beim App-Start:
   - Beim nächsten App-Start: Falls ein gespeicherter Zustand aus 3c. vorliegt, zeige 
     VOR dem normalen Start-Screen einen Dialog "Vorherige Sitzung wiederherstellen?" 
     mit den Optionen "Wiederherstellen" / "Neu beginnen" (bei "Neu beginnen" wird der 
     gespeicherte Zustand verworfen).
   - Bei "Wiederherstellen": Öffne automatisch alle zuvor gespeicherten Editor-Tabs 
     im gespeicherten Projekt, navigiere zur zuletzt aktiven Editor-Ansicht, und 
     stelle (soweit technisch möglich) die Terminal-Sessions mit demselben working 
     directory wieder her (neue Prozesse, aber gleicher Kontext).

3e. Build- & Funktionsverifikation: Manuelles Durchspielen des kompletten Zyklus – 
   App mit offenen Tabs/Terminal schließen (mit "Speichern"-Bestätigung), App neu 
   starten, "Wiederherstellen" wählen, Zustand muss korrekt reproduziert werden. 
   Zusätzlich: Zurück-Taste-Verhalten auf allen Screens außer Root muss zum 
   vorherigen Screen navigieren, nicht die App beenden.

---

## STUFE 4 – Vollständige Implementierung der Command-Palette-Commands

4.1. Nutze die bestehende Infrastruktur aus :core:commands (`CommandManager`, 
   `CommandProvider`, Basisklassen `GlobalCommand`/`EditorCommand`) – KEINE 
   Parallelstruktur aufbauen.

4.2. Erstelle eine vollständige Liste aller Befehle, die in einer produktiv 
   nutzbaren Command Palette einer Android-IDE erwartbar sind, und die aktuell in 
   MobileIDE noch NICHT als registrierte `GlobalCommand`/`EditorCommand`-Instanz 
   existieren (die bisher sichtbaren Beispiele sind ausschließlich Plugin-Befehle 
   aus typst-lsp – die IDE selbst hat laut vorliegendem Codeausschnitt noch KEINE 
   eigenen Kern-Commands registriert). Dazu zählen mindestens:
   - Datei: Neue Datei, Neuer Ordner, Datei speichern, Alle speichern, Datei 
     schließen, Alle schließen, Datei umbenennen, Datei löschen, Datei im 
     Dateimanager anzeigen.
   - Editor: Suchen, Suchen&Ersetzen, Gehe zu Zeile, Kommentar umschalten, Zeile 
     duplizieren, Zeile löschen, Nach oben/unten verschieben, Einrückung erhöhen/
     verringern, Groß-/Kleinschreibung umschalten, Zurück/Wiederholen (Undo/Redo), 
     Alles auswählen, Editor-Schriftgröße erhöhen/verringern, Zeilenumbruch 
     umschalten (Word Wrap).
   - Projekt/Build: Projekt bauen (Debug/Release), Projekt ausführen, Build abbrechen, 
     Gradle-Sync, Gradle-Cache leeren, Clean Build.
   - Terminal: Neues Terminal öffnen, Aktuelles Terminal schließen, Terminal fokus- 
     sieren/zurück zum Editor.
   - Git: Commit, Push, Pull, Branch wechseln, Status anzeigen, Diff anzeigen 
     (Anbindung an die in Stufe 0/1 lokalisierte/erweiterte Git-Funktionalität).
   - Ansicht: Theme wechseln (Hell/Dunkel/System), Seitenpanel ein-/ausblenden, 
     Terminal-Panel ein-/ausblenden, Zoom zurücksetzen.
   - Einstellungen: Direktsprung zu jedem der sechs in Stufe 1 behandelten Settings-
     Bereiche (Editor, Terminal, Git, LSP, Extensions, Speech-Server) als jeweils 
     eigener Command ("Go to Editor Settings" etc.).

4.3. Implementiere jeden Command als eigene Klasse nach dem bestehenden Muster 
   (siehe `TypstCompilePdfCommand`/`TypstUninstallCommand` als Vorlage: `id`, 
   `prefix`, `getLabel()` via String-Resource, `getIcon()`, ggf. `isSupported()`/
   `isEnabled()`, `action(context)`), organisiert in sinnvollen Paketen unterhalb 
   von com.scto.mobile.ide.commands.core.* (thematisch gruppiert nach Datei/Editor/
   Projekt/Terminal/Git/Ansicht/Einstellungen wie oben).

4.4. Registriere alle neuen Commands zentral beim App-Start (z. B. in MainActivity 
   oder einem dedizierten `CoreCommandsInitializer`), analog zur Registrierung von 
   Plugin-Commands via `CommandProvider.registerCommand(it)`.

4.5. Stelle sicher, dass die Command-Palette-UI (Suchfeld + Ergebnisliste, 
   vermutlich bereits als Grundgerüst vorhanden, da CommandManager laut status.md 
   "mit UI-Integration" implementiert wurde) tatsächlich ALLE registrierten Commands 
   durchsuchbar anzeigt, inkl. Icon, Label und ggf. Prefix-Gruppierung.

4.6. Build- & Funktionsverifikation: Öffne die Command Palette und bestätige, dass 
   jeder der oben gelisteten Commands auffindbar ist und beim Ausführen die 
   erwartete Aktion auslöst (keine No-Op-Platzhalter).

---

## STUFE 5 – Sichtbarkeit der Git-Funktionalität

5.1. Nutze das Ergebnis aus Stufe 0.2 (Lokalisierung der bestehenden Git-Logik). 
   Prüfe konkret, WARUM diese Funktionalität aktuell (laut Nutzerangabe) nicht 
   sichtbar/erreichbar in der UI ist – mögliche Ursachen: fehlender Navigations-
   Einstiegspunkt, ausgeblendetes/deaktiviertes UI-Element, fehlende Berechtigung, 
   oder eine noch unvollständige/nicht verdrahtete Compose-Screen-Implementierung.

5.2. Stelle sicher, dass ein Git-Bereich (z. B. als Tab im Seitenpanel/Filetree-
   Bereich, konsistent mit der bereits an anderer Stelle in diesem Projekt 
   besprochenen "Git-Sektion im Filetree-Sidepanel") tatsächlich sichtbar und über 
   die normale App-Navigation erreichbar ist, inkl. Anzeige von Status (Branch, 
   geänderte Dateien), sowie Zugriff auf Commit/Push/Pull/Branch-Aktionen und den in 
   Stufe 1.1 neu erstellten GitSettingsScreen (Username/E-Mail/Token).

5.3. Build- & Funktionsverifikation: Manuelles Durchklicken bestätigt, dass die 
   Git-Sektion sichtbar ist, den korrekten Status eines Test-Repositories anzeigt, 
   und die konfigurierten Credentials aus GitSettingsScreen bei einer Push-Operation 
   tatsächlich verwendet werden.

# Nicht-Ziele
- Keine Migration von Xed-Editor-Funktionen, die inhaltlich nicht zu den fünf 
  genannten Bereichen (Settings, Editor-Funktionen, Terminal-Erweiterung, Command 
  Palette, Git-Sichtbarkeit) gehören.
- Keine Änderung der bestehenden Terminal-Farbschemata/Reset-Reinstall-Logik über 
  reine Ergänzung hinaus.
- Keine Einführung eines neuen, von der bestehenden CommandManager-Infrastruktur 
  abweichenden Command-Systems.

# Akzeptanzkriterien
- Alle sechs Settings-Bereiche (Editor, Terminal, Git, LSP, Extensions, Speech-
  Server) sind über die App-Navigation erreichbar, korrekt lokalisiert im 
  com.scto.mobile.ide-Package, und persistieren ihre Werte zuverlässig.
- Git-Settings speichern Username, E-Mail und GitHub-Token sicher (verschlüsselt) 
  und diese werden nachweislich bei Commit/Push/Pull verwendet.
- Alle in Stufe 0 als fehlend identifizierten Editor-Funktionen aus Xed-Editor sind 
  in MobileIDE nutzbar.
- Terminal unterstützt zusätzliche Theme-/Font-Optionen aus Xed-Editor, fragt vor 
  Session-Schließung/Neuöffnung bei aktivem Prozess nach Bestätigung.
- System-Zurück-Taste navigiert auf allen Screens außer dem Haupt-Screen zum 
  vorherigen Screen; auf dem Haupt-Screen erscheint ein Beenden-Bestätigungsdialog 
  mit Zustands-Speicherungs-Option; beim nächsten App-Start wird die 
  Wiederherstellung angeboten und funktioniert nachweislich.
- Die Command Palette listet und führt alle in Stufe 4.2 aufgeführten Kern-Commands 
  korrekt aus.
- Die Git-Sektion ist sichtbar und vollständig funktional in der UI erreichbar.
