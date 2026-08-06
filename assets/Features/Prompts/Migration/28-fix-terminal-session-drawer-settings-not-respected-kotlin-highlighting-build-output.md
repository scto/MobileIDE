
# Ziel
Behebe fünf zusammenhängende Bugs in MobileIDE (com.scto.mobile.ide): Terminal-
Session-Drawer erweitert die Liste beim Erstellen einer neuen Session nicht sichtbar 
mit, Terminal-Theme-Einstellungen werden ignoriert (aktuell nur 5 statt gewünschter 
10 Themes), der konfigurierbare Terminal-Schließen-Modus (App schließen vs. neue 
Session erstellen) wird ignoriert, fehlerhafte Kotlin-Syntax-Highlighting-Farben in 
Kombination mit einem abstürzenden LSP-Installationsdialog und einem funktionslosen 
Terminal-Install-Toast, sowie eine unbrauchbare Build-Ausgabe ohne Angabe des 
tatsächlichen Projektpfads.

# Kontext (siehe Screenshot-Anhang)

## Screenshot – Editor mit Kotlin-Datei + LSP-Dialog + Toast
Zeigt `MainActivity.kt` im Editor mit:
- Einem Popup-Dialog "Kotlin Language Server" ("LSP-Server kotlin-language-server 
  für .kt-Dateien ist nicht installiert. Jetzt installieren?") mit "Installieren"-
  Button und Schließen-X.
- Import-Zeilen, `class MainActivity : Compon...`, `override fun onCreate(sav...`, 
  `enableEdgeToEdge()`, `setContent { AppTheme { Scaffold(modi... CounterAp... } } }` 
  – alle in erkennbar undifferenzierten/falschen Farben (siehe Detailproblem 4).
- Einem unteren Toast "kotlin-language-server ist nicht installiert." mit 
  "Installieren"-Link.

## Detailproblem 1 – Session-Drawer aktualisiert sich nicht
Beim Erstellen einer neuen Terminal-Session über das Plus-Icon im Session-Drawer 
wird die neu erstellte Session NICHT sichtbar zur Liste hinzugefügt – der Drawer 
zeigt weiterhin nur die ursprüngliche Session, obwohl im Hintergrund vermutlich 
eine zweite Session-Instanz tatsächlich erzeugt wird (oder gar nicht erzeugt wird – 
dies ist in Stufe 0 zu klären). Dies deutet auf einen fehlenden State-Update-/
Recomposition-Trigger im Session-Listen-ViewModel hin.

## Detailproblem 2 – Terminal-Themes werden ignoriert, nur 5 statt 10
In den Terminal-Einstellungen existiert eine Theme-Auswahl, die aktuell nur 5 
Themes anbietet, UND deren Auswahl beim tatsächlichen Terminal-Rendering NICHT 
angewendet/beachtet wird. Zusätzlich zur Bugfix-Aufgabe soll die Theme-Liste von 5 
auf 10 Themes erweitert werden.

## Detailproblem 3 – Terminal-Schließen-Verhalten wird ignoriert
In den Terminal-Einstellungen existiert ein Auswahl-Modus für das Verhalten beim 
Schließen der letzten/aktiven Terminal-Session, mit zwei Optionen: "App schließen" 
und "Neue Terminal-Session erstellen". Aktuell wird diese Einstellung beim 
tatsächlichen Schließen-Ereignis NICHT ausgewertet.

## Detailproblem 4 – Kotlin-Highlighting, LSP-Dialog-Absturz, Toast-Funktionslosigkeit
- Kotlin-Syntax-Highlighting-Farben sind komplett falsch/undifferenziert.
- Der Installationsdialog für den Kotlin-Language-Server schließt beim Bestätigen 
  ("Installieren") die gesamte App KOMMENTARLOS – kein Installationsvorgang findet 
  statt.
- Der untere Toast öffnet beim Antippen von "Installieren" NUR das Terminal, führt 
  aber KEINE tatsächliche Installation/Skript-Ausführung durch.

## Detailproblem 5 – Build-Ausgabe fehlt Projektpfad
Die Build-Ausgabe/Konsole (vermutlich das Panel, das beim Antippen des Play-/Run-
Icons in der Toolbar erscheint, siehe Toolbar-Icons oben rechts im Screenshot: 
Checkliste, Kommentar-Avatar, Play-Dreieck, Undo/Redo, Overflow-Menü) ist laut 
Nutzerangabe "ein Witz", weil sie den tatsächlichen absoluten Projektpfad NICHT 
anzeigt (z. B. fehlt eine Zeile wie `> Projektverzeichnis: /storage/emulated/0/...
/MyApp` oder `Working Directory: ...` zu Beginn der Build-Ausgabe). Dies erschwert 
Nutzern die Nachvollziehbarkeit, welches Projekt/Verzeichnis tatsächlich gebaut 
wird, insbesondere bei mehreren Projekten mit identischem Namen in unterschiedlichen 
Verzeichnissen.

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER. Nach jeder Stufe: Gesamtprojekt bauen 
(Gradle), Fehlerfreiheit bestätigen, Ergebnis-Report mit geänderten Dateien liefern, 
bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme (Pflicht vor jeder Code-Änderung)

0.1. Lokalisiere den Session-Drawer-Compose-Code und das zugehörige 
   `TerminalSessionViewModel`. Prüfe per Log/State-Analyse, ob beim Antippen des 
   Plus-Icons tatsächlich eine neue Session-Instanz im Backend erzeugt wird, aber 
   die UI-Liste nicht aktualisiert wird, ODER ob bereits die Session-Erzeugung 
   selbst fehlschlägt.

0.2. Lokalisiere die Terminal-Theme-Einstellungen (Settings-Screen + zugehöriges 
   Theme-Enum/-Liste mit aktuell 5 Einträgen) sowie die Stelle, an der das gewählte 
   Theme tatsächlich auf die Terminal-Rendering-Engine angewendet werden SOLLTE. 
   Identifiziere die Ursache der Nicht-Anwendung.

0.3. Lokalisiere die Terminal-Schließen-Verhalten-Einstellung sowie die Stelle im 
   Terminal-Session-Lifecycle-Code, die beim tatsächlichen Schließen-Ereignis 
   ausgelöst wird. Identifiziere, warum diese Einstellung dort nicht ausgelesen 
   wird.

0.4. Lokalisiere die Kotlin-Highlight-Query/Theme-Mapping-Stelle in 
   `:language-treesitter` bzw. `EditorViewModel.kt`, den Installationsdialog-
   Trigger für den Kotlin-Language-Server (Absturzursache via Stacktrace/LogCatcher 
   ermitteln), sowie den Toast-Trigger, der aktuell nur das Terminal öffnet ohne 
   Skript-Ausführung.

0.5. Lokalisiere die Build-Ausgabe-/Konsolen-Implementierung (vermutlich 
   `BuildOutputPanel.kt`/`BuildConsoleViewModel.kt`, ausgelöst über das Play-Icon in 
   der Editor-Toolbar). Analysiere den aktuellen Ausgabe-Header/-Präfix und 
   bestätige, dass der absolute Projektpfad tatsächlich fehlt, obwohl er dem 
   Build-Prozess (Gradle-Working-Directory) bekannt sein muss. Prüfe, ob der Pfad 
   im zugrundeliegenden Gradle-Task-Aufruf-Objekt bereits verfügbar ist und nur 
   nicht in die UI-Ausgabe geschrieben wird.

0.6. Liefere diese Bestandsaufnahme als eigenständigen Zwischen-Report mit den 
   exakten betroffenen Dateien und Ursachen zu allen fünf Detailproblemen, BEVOR 
   mit Stufe 1 fortgefahren wird.

---

## STUFE 1 – Session-Drawer-Liste korrekt aktualisieren

1.1. Behebe den in Stufe 0.1 identifizierten Fehler, sodass beim Antippen des 
   Plus-Icons eine neue Session tatsächlich erzeugt UND sofort sichtbar mit 
   korrekter Nummerierung ("2", "3", ...) in der Liste erscheint, mit 
   funktionierendem Löschen-Icon pro Eintrag.

1.2. Stelle sicher, dass die aktuell aktive Session weiterhin korrekt visuell 
   hervorgehoben bleibt, auch nach Hinzufügen weiterer Sessions, und dass ein 
   Tippen auf eine andere Session-Zeile korrekt zu dieser Session umschaltet.

1.3. Build- & Funktionsverifikation: Erstelle nacheinander 3 neue Sessions, 
   bestätige, dass alle sichtbar in der Liste erscheinen, umschaltbar und einzeln 
   löschbar sind.

---

## STUFE 2 – Terminal-Theme-Anwendung reparieren & auf 10 Themes erweitern

2.1. Behebe den in Stufe 0.2 identifizierten Fehler, sodass ein in den Settings 
   gewähltes Terminal-Theme SOFORT (ohne App-Neustart) auf alle offenen und neu 
   erstellten Terminal-Sessions angewendet wird (Hintergrundfarbe, Vordergrundfarbe, 
   ANSI-Farbpalette).

2.2. Erweitere die Theme-Liste von 5 auf 10 Themes. Ergänze 5 zusätzliche, 
   sinnvolle Terminal-Farbschemata (z. B. "Solarized Dark", "Solarized Light", 
   "Dracula", "Nord", "Gruvbox Dark" mit den jeweils offiziell bekannten Farbwerten), 
   passend zur bestehenden Theme-Datenstruktur.

2.3. Build- & Funktionsverifikation: Wähle nacheinander mehrere der 10 Themes in 
   den Settings aus, bestätige, dass sich das Terminal-Erscheinungsbild jedes Mal 
   sofort sichtbar entsprechend ändert.

---

## STUFE 3 – Terminal-Schließen-Verhalten korrekt auswerten

3.1. Behebe den in Stufe 0.3 identifizierten Fehler, sodass beim Schließen der 
   letzten aktiven Terminal-Session tatsächlich die in den Settings gewählte 
   Option ausgewertet wird:
   - Modus "App schließen": Die App wird beendet.
   - Modus "Neue Terminal-Session erstellen": Statt die App zu schließen, wird 
     automatisch eine neue Standard-Session erstellt und aktiviert.

3.2. Build- & Funktionsverifikation: Stelle beide Modi nacheinander ein, schließe 
   jeweils die letzte Session, bestätige das jeweils korrekte, unterschiedliche 
   Verhalten.

---

## STUFE 4 – Kotlin-Highlighting, LSP-Dialog-Absturz, Toast-Funktionslosigkeit

4.1. Behebe die Kotlin-Highlighting-Farbzuordnung in `EditorViewModel.kt`/der 
   TreeSitter-Kotlin-Query, sodass Import-Keywords, Deklarations-/Modifier-
   Keywords, Typnamen, Import-Pfade, Funktionsdeklarationen und Funktionsaufrufe 
   klar unterscheidbare, semantisch konsistente Farben erhalten.

4.2. Behebe die Absturzursache des Kotlin-LSP-Installationsdialogs, sodass 
   "Installieren" tatsächlich den Download/Installationsvorgang mit sichtbarem 
   Fortschritt anstößt, statt die App kommentarlos zu schließen. Ergänze zwingend 
   eine sichtbare Fehlermeldung (Snackbar/Dialog) für den Fehlschlag-Fall, STATT 
   eines stillen Absturzes.

4.3. Behebe den unteren Toast, sodass "Installieren" tatsächlich das zugehörige 
   Installations-Skript für `kotlin-language-server` im Terminal-Kontext ausführt, 
   mit anschließendem automatischem Status-Update in der Sprachserver-Liste nach 
   erfolgreicher Installation.

4.4. Build- & Funktionsverifikation: Öffne eine Kotlin-Datei ohne installierten 
   Sprachserver, bestätige sowohl über den oberen Dialog als auch über den unteren 
   Toast jeweils eine tatsächlich durchgeführte, sichtbare Installation ohne 
   App-Absturz, sowie korrektes, differenziertes Syntax-Highlighting danach.

---

## STUFE 5 – Build-Ausgabe um Projektpfad ergänzen

5.1. Ergänze die Build-Ausgabe/Konsole so, dass zu Beginn JEDES Build-Vorgangs 
   (Run/Build/Clean etc.) eine klar sichtbare Kopfzeile mit dem vollständigen, 
   absoluten Projektpfad ausgegeben wird, z. B.:
   ```
   > Projektverzeichnis: /storage/emulated/0/AndroidIDEProjects/MyApp
   > Gradle-Task: assembleDebug
   > Gestartet: 2026-08-05 17:29:03
   ```
   Formatierung konsistent mit dem restlichen Output-Stil (Farbkodierung für 
   Info-Zeilen, analog zu bestehenden Erfolgs-/Fehler-Farbmarkierungen im 
   Build-Panel).

5.2. Stelle sicher, dass der angezeigte Pfad IMMER der tatsächlich vom Build-Prozess 
   verwendete Working-Directory-Pfad ist (nicht hartkodiert, sondern dynamisch aus 
   dem tatsächlichen Gradle-Projekt-Root-Objekt gelesen), um Divergenzen bei 
   mehreren gleichnamigen Projekten in unterschiedlichen Verzeichnissen zu 
   verhindern.

5.3. Ergänze zusätzlich einen klickbaren/kopierbaren Pfad (z. B. Tippen auf die 
   Pfad-Zeile kopiert den Pfad in die Zwischenablage, mit kurzer Bestätigungs-
   Snackbar "Pfad kopiert"), analog zu bereits bestehenden Copy-to-Clipboard-
   Mustern in der App (falls vorhanden).

5.4. Build- & Funktionsverifikation: Starte einen Build-Vorgang, bestätige, dass 
   der korrekte, vollständige Projektpfad zu Beginn der Ausgabe sichtbar ist und 
   bei mehreren Test-Projekten mit identischem Namen in unterschiedlichen 
   Verzeichnissen korrekt jeweils den richtigen Pfad zeigt.

---

## STUFE 6 – Abschlussverifikation

6.1. Vollständiger Gradle-Build aller betroffenen Module.

6.2. Manuelle Durchklick-Bestätigung aller fünf behobenen Punkte:
   - Session-Drawer zeigt neu erstellte Sessions sofort sichtbar an.
   - Terminal-Theme-Auswahl (jetzt 10 Themes) wird sofort angewendet.
   - Terminal-Schließen-Verhalten (beide Modi) wird korrekt ausgewertet.
   - Kotlin-Highlighting ist korrekt differenziert, LSP-Dialog installiert 
     tatsächlich ohne Absturz, Toast führt tatsächliche Installation aus.
   - Build-Ausgabe zeigt zu Beginn immer den korrekten, vollständigen 
     Projektpfad.

6.3. Liefere einen finalen Gesamt-Report mit allen geänderten Dateien pro Stufe.

# Nicht-Ziele
- Keine Änderung der grundsätzlichen Terminal-Emulator-Engine (PTY-Handling, 
  Escape-Sequenz-Verarbeitung) jenseits der Theme-Anwendungslogik.
- Keine Änderung der Sandbox-/Ubuntu-Rootfs-Konfiguration.
- Keine Änderung an anderen Sprachservern (Java/Bash/XML) jenseits der reinen 
  Referenzprüfung zur Farbkonsistenz.
- Keine grundsätzliche Neugestaltung des Build-Panels jenseits der Ergänzung der 
  Projektpfad-Kopfzeile.

# Akzeptanzkriterien
- Neu erstellte Terminal-Sessions erscheinen sofort und korrekt nummeriert im 
  Session-Drawer.
- Terminal bietet 10 auswählbare Themes, jede Auswahl wird sofort visuell auf alle 
  Terminal-Sessions angewendet.
- Terminal-Schließen-Verhalten (App schließen / Neue Session erstellen) wird beim 
  tatsächlichen Schließen-Ereignis korrekt gemäß Settings-Auswahl ausgeführt.
- Kotlin-Code zeigt klar differenzierte Syntax-Highlighting-Farben.
- Kotlin-LSP-Installationsdialog installiert erfolgreich ohne App-Absturz.
- Terminal-Install-Toast führt tatsächlich die Sprachserver-Installation aus.
- Build-Ausgabe zeigt zu Beginn jedes Build-Vorgangs den korrekten, vollständigen, 
  absoluten Projektpfad an.
- Gesamtprojekt-Build ist am Ende fehlerfrei.
