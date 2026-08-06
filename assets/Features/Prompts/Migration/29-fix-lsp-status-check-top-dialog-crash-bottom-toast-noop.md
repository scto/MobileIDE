# Ziel
Behebe drei zusammenhängende Fehler im Kotlin-Language-Server-Installationsablauf 
von MobileIDE (com.scto.mobile.ide): fehlender/fehlerhafter Installations-Status-
Check (Dialog erscheint trotz bereits installiertem Plugin), Absturz der gesamten 
App beim Klick auf den oberen LSP-Installations-Dialog, sowie ein komplett 
funktionsloser unterer Toast (Klick löst keine Aktion aus).

# Kontext (siehe Video-Anhang)
Reproduktionsschritte im Video: Navigation im Dateibaum 
(MyApp > app > src > main > java > com > example > myapp > ui) → Öffnen von 
`MainActivity.kt` → sofortiges Erscheinen eines oberen Dialogs "Kotlin Language 
Server" ("LSP-Server kotlin-language-server für .kt-Dateien ist nicht installiert. 
Jetzt installieren?" mit Download-Button) → Klick auf diesen Dialog → SOFORTIGER 
KOMPLETTABSTURZ der App (Rücksprung zum Android-Homescreen, Notification-Leiste 
wird danach vom Nutzer geöffnet, vermutlich zur Suche nach einem Crash-Log/
Benachrichtigung). Zusätzlich existiert am unteren Bildschirmrand ein Toast für 
dieselbe Aktion, dessen Klick aktuell KEINE Wirkung zeigt (leerer/fehlender 
OnClickListener).

Laut Entwickler-Angabe ist das Kotlin-LSP-Plugin zum Zeitpunkt der Reproduktion 
bereits installiert – der Dialog/Toast dürfte in diesem Fall gar nicht erscheinen, 
was auf einen fehlenden oder fehlerhaften Installations-Status-Check hindeutet.

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER. Nach jeder Stufe: Gesamtprojekt bauen 
(Gradle), Fehlerfreiheit bestätigen, Ergebnis-Report mit geänderten Dateien liefern, 
bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme (Pflicht vor jeder Code-Änderung)

0.1. Lokalisiere die Stelle im Code, die beim Öffnen einer `.kt`-Datei die 
   LSP-Installationsprüfung auslöst (vermutlich in `CodeEditScreen.kt`/
   `EditorViewModel.kt`, getriggert durch Datei-Endungs-Erkennung). Identifiziere 
   die exakte Prüf-Logik: Wird der Installationsstatus überhaupt abgefragt? Falls 
   ja: gegen welche Quelle wird geprüft (Dateisystem-Existenzprüfung des LSP-
   Binaries, DataStore-Flag, Registry-Liste)? Warum liefert diese Prüfung ein 
   falsches Ergebnis, wenn das Plugin laut Entwickler bereits installiert ist 
   (z. B. falscher Pfad, veraltetes Cache-Flag, Race-Condition beim App-Start vor 
   vollständiger Initialisierung der Sprachserver-Registry)?

0.2. Lokalisiere die exakte Compose-Funktion/den Trigger des oberen Dialogs 
   ("Kotlin Language Server"-Overlay) und analysiere per Stacktrace/LogCatcher den 
   Absturz beim Klick. Identifiziere die exakte Exception-Ursache (z. B. Zugriff 
   auf einen ungültigen/null Context, ein fehlerhafter Intent-Aufruf ohne 
   Exception-Handling, ein Absturz in einer nicht abgefangenen Coroutine).

0.3. Lokalisiere die Extension-/Erweiterungseinstellungen-Screen-Route (Ziel-
   Navigation für den neuen Klick-Handler des Top-Dialogs) sowie – falls bereits 
   vorhanden – eine Vorstufe/Platzhalter für einen zukünftigen "Extension Store".

0.4. Lokalisiere den unteren Toast-Trigger und dokumentiere den aktuellen (fehlenden 
   oder leeren) OnClick-Handler, sowie die vorhandene Terminal-Öffnen-Funktion und 
   den Mechanismus zur Befehlsübergabe an eine Terminal-Session (analog zu 
   bestehenden Sandbox-Setup-Skript-Ausführungen).

0.5. Liefere diese Bestandsaufnahme als eigenständigen Zwischen-Report mit exakten 
   Dateipfaden und Ursachen zu allen drei Punkten, BEVOR mit Stufe 1 fortgefahren 
   wird.

---

## STUFE 1 – Zuverlässiger Installations-Status-Check

1.1. Implementiere/korrigiere eine zentrale, zuverlässige Prüf-Funktion 
   `isLspInstalled(languageId: String): Boolean`, die den tatsächlichen 
   Installationsstatus verlässlich anhand des Dateisystems (Existenz + 
   Ausführbarkeit des installierten LSP-Binaries/Skripts im erwarteten 
   Installationsverzeichnis) ermittelt, NICHT anhand eines möglicherweise 
   veralteten Cache-Flags allein.

1.2. Rufe diese Prüf-Funktion IMMER auf, bevor der obere Dialog ODER der untere 
   Toast angezeigt werden. Ist das Ergebnis `true` (bereits installiert), DÜRFEN 
   weder Dialog noch Toast erscheinen – stattdessen wird der LSP normal für die 
   geöffnete Datei aktiviert (Autocomplete, Diagnostics etc.).

1.3. Stelle sicher, dass diese Prüfung robust gegen Race-Conditions beim App-Start 
   ist (z. B. Warten auf vollständige Initialisierung der Sprachserver-Registry via 
   Coroutine/Flow, bevor die Prüfung ausgeführt wird, statt eines möglicherweise 
   noch leeren In-Memory-Zustands).

1.4. Build- & Funktionsverifikation: Öffne eine `.kt`-Datei bei bereits 
   installiertem Kotlin-LSP – weder Dialog noch Toast dürfen erscheinen. Deinstalliere 
   den LSP testweise, öffne die Datei erneut – Dialog und Toast müssen korrekt 
   erscheinen.

---

## STUFE 2 – Absturz beim Klick auf den oberen Dialog beheben & neue Aktion implementieren

2.1. Behebe die in Stufe 0.2 identifizierte Absturzursache vollständig (korrekten 
   Context verwenden, Intent-Aufruf mit Try-Catch/Ergebnis-Prüfung absichern, 
   Coroutine-Exceptions über einen zentralen `CoroutineExceptionHandler` abfangen 
   statt sie unbehandelt propagieren zu lassen).

2.2. Ändere die Klick-Aktion des oberen Dialogs so, dass sie NICHT mehr den 
   bisherigen (fehlerhaften) Installationspfad direkt auslöst, sondern zur 
   Extension-Settings-Screen-Route navigiert (`NavigationUtils.safeNavigate` unter 
   Verwendung bestehender Navigations-Konventionen).

2.3. Implementiere diese Navigation über eine austauschbare Abstraktion (z. B. 
   `ExtensionActionHandler`-Interface mit aktueller Implementierung 
   `NavigateToExtensionSettings`), sodass sie zu einem späteren Zeitpunkt ohne 
   Breaking Change durch eine Navigation zu einem zukünftigen "Extension Store" 
   ersetzt werden kann (Kommentar/TODO im Code ergänzen, der genau dies dokumentiert).

2.4. Build- & Funktionsverifikation: Löse den Dialog aus (LSP testweise nicht 
   installiert), klicke ihn an – App darf NICHT abstürzen, sondern muss zur 
   Extension-Settings-Seite navigieren.

---

## STUFE 3 – Unteren Toast funktional machen

3.1. Implementiere den fehlenden OnClick-Handler des unteren Toasts, sodass ein 
   Klick darauf:
   a) das integrierte Terminal der App öffnet (bestehende Terminal-Öffnen-Funktion 
      nutzen),
   b) UNMITTELBAR danach den tatsächlichen Installationsbefehl/das zugehörige 
      Installationsskript für `kotlin-language-server` an die geöffnete Terminal-
      Session übergibt und ausführt (Befehlsübergabe-Mechanismus analog zu 
      bestehenden Sandbox-Setup-Skript-Ausführungen, NICHT nur Terminal öffnen ohne 
      Befehl).

3.2. Stelle sicher, dass nach erfolgreichem Abschluss der Installation im Terminal 
   der Installationsstatus (Stufe 1.1) automatisch neu ausgewertet wird und der LSP 
   danach ohne weiteren Nutzer-Eingriff für offene `.kt`-Dateien aktiv wird.

3.3. Build- & Funktionsverifikation: Klicke bei nicht installiertem LSP auf den 
   unteren Toast – Terminal öffnet sich UND der Installationsbefehl wird sichtbar 
   ausgeführt (stdout/stderr im Terminal sichtbar), anschließend automatische 
   Status-Aktualisierung.

---

## STUFE 4 – Abschlussverifikation

4.1. Vollständiger Gradle-Build.

4.2. Manuelle Durchklick-Bestätigung aller drei behobenen Punkte:
   - Kein Dialog/Toast bei bereits installiertem LSP.
   - Klick auf oberen Dialog führt zu Extension-Settings statt Absturz.
   - Klick auf unteren Toast öffnet Terminal UND startet tatsächliche Installation.

4.3. Liefere einen finalen Gesamt-Report mit allen geänderten Dateien pro Stufe, 
   inkl. Nennung der exakten behobenen Exception-Klasse aus Stufe 2.1.

# Nicht-Ziele
- Keine Implementierung des zukünftigen "Extension Store" selbst – nur 
  architektonische Vorbereitung (austauschbare Handler-Abstraktion) dafür.
- Keine Änderung der grundsätzlichen LSP-Server-Download-/Installationslogik 
  jenseits der Skript-Ausführungs-Anbindung im Terminal.
- Keine Änderung an anderen Sprachservern (Java/Bash/XML) jenseits der Wieder-
  verwendung derselben Status-Check-Funktion.

# Akzeptanzkriterien
- Bei bereits installiertem Kotlin-LSP erscheinen weder oberer Dialog noch unterer 
  Toast beim Öffnen einer `.kt`-Datei.
- Klick auf den oberen Dialog führt zuverlässig zur Extension-Settings-Seite, ohne 
  App-Absturz.
- Klick auf den unteren Toast öffnet das Terminal und startet nachweislich den 
  Installationsprozess des Kotlin-Language-Servers.
- Nach erfolgreicher Installation wird der Status automatisch aktualisiert, ohne 
  dass der Nutzer die Datei erneut öffnen oder die App neu starten muss.
- Gesamtprojekt-Build ist am Ende fehlerfrei.
