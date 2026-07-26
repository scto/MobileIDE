# Ziel
Implementiere eine bidirektionale Gradle-Tooling-Bridge zwischen der Compose-UI (EditorScreen) 
und dem im Terminal laufenden Gradle-Prozess, aufbauend auf den bestehenden Modulen 
:core:tooling:tooling-api, :core:tooling:tooling-impl und :core:tooling:tooling-server.

# Kontext
- Es existiert bereits ein Gradle-Task-Parser mit Checkbox-Liste (siehe PROGRESS.md, 2026-06-29 
  "Interactive Gradle Tasks Panel"). Analysiere zuerst den aktuellen Stand in 
  :core:tooling:tooling-impl und :core:tooling:tooling-server bevor du etwas neu implementierst.
- Terminal-Ausführung läuft über PRoot/Ubuntu-Sandbox (DistroManager.buildProotCommand).

# Anforderungen
1. GradleBridgeService (:core:tooling:tooling-server):
   - Führt `./gradlew tasks --all` (via bash-Wrapper wegen noexec) im aktuellen Projektverzeichnis aus.
   - Parst Task-Namen inkl. Gruppe/Beschreibung in eine strukturierte Liste (GradleTask(name, group, description)).
   - Cached das Ergebnis pro Projekt-Session, invalidiert bei Projektwechsel oder manuellem Refresh.
   - Stellt eine Methode `runTasks(tasks: List<String>, flags: List<String>): Flow<GradleLogLine>` bereit,
     die den Gradle-Prozess im Terminal-Kontext (PRoot) startet und die Ausgabe zeilenweise streamt.

2. GradleLogLine Datenmodell:
   - Felder: lineNumber: Int, rawText: String, level: LogLevel (INFO/WARN/ERROR/TASK/SUCCESS/DEFAULT),
     timestamp: Long.
   - Level-Erkennung per Regex auf typische Gradle-Ausgabemuster (`> Task :app:...`, `w:`, `e:`, 
     `BUILD SUCCESSFUL`, `BUILD FAILED`, `FAILURE:`).

3. UI – EditorScreen (:app):
   - Icon oben rechts in der TopAppBar ("Tasks"-Symbol, z. B. Icons.Outlined.Checklist).
   - Klick öffnet Dialog/BottomSheet mit Liste aller Tasks: Zeile = Checkbox links + Task-Name rechts,
     gruppiert nach Gradle-Group (z. B. "build", "verification").
   - Mehrfachauswahl möglich. Am unteren Rand: Button "Abbrechen" (links) und "OK" (rechts).
   - Klick auf "OK" ruft GradleBridgeService.runTasks() mit den ausgewählten Task-Namen auf und 
     schließt den Dialog, öffnet das Build-BottomSheet.

4. UI – Build BottomSheet (Tab "Build") in :core:tooling:tooling-impl:
   - Zeigt GradleLogLine-Einträge zeilenweise, mit führender Zeilennummer (rechtsbündig, gedimmt) 
     und farblich nach LogLevel (z. B. Rot=ERROR, Gelb=WARN, Grün=SUCCESS, Grau=DEFAULT, Blau=TASK).
   - Nutze LazyColumn mit Auto-Scroll-to-bottom (deaktivierbar durch manuelles Scrollen nach oben).
   - Wenn echtes Zeilen-Streaming in Echtzeit zu Performance-Problemen führt (Frame-Drops bei 
     hoher Log-Frequenz), implementiere Batching mit 100-200ms Debounce statt Echtzeit, aber 
     behalte Zeilenreihenfolge und -nummern exakt bei.
   - Monospace-Schriftart für exakte Gradle-Formatierung.

5. Flags-UI:
   - Über dem Log-Bereich im Build-Tab: ein aufklappbares Chip-Auswahlfeld mit den Standard-Flags:
     --info, --debug, --warn, --stacktrace, --scan, --offline, --refresh-dependencies, 
     --dry-run, --parallel, --continue.
   - Zusätzlich ein Freitextfeld für beliebige weitere Gradle-Flags/Properties (-P, -D).
   - Ausgewählte Flags werden bei jedem runTasks()-Aufruf mitgegeben.

6. Startup-Verhalten:
   - Bei Projektöffnung im EditorScreen wird automatisch (asynchron, nicht blockierend) 
     GradleBridgeService.tasks abgefragt und im ViewModel gecached, damit das Tasks-Icon 
     sofort eine befüllte Liste anzeigen kann.

# Nicht-Ziele
- Keine Änderung an der bestehenden Terminal-View-Session-Logik (:core:terminal-view) – 
  die Gradle-Bridge läuft als separater, headless Prozessaufruf über die Sandbox, nicht 
  im interaktiven Terminal-Tab selbst.

# Akzeptanzkriterien
- Tasks-Icon erscheint nur wenn ein Projekt mit build.gradle(.kts) geöffnet ist.
- Ausgewählte Tasks werden korrekt inkl. Flags an Gradle übergeben (verifizierbar per Logausgabe 
  des tatsächlichen Kommandos vor Ausführung).
- Build-Tab zeigt farbcodierte, zeilennummerierte Ausgabe, die bei einem echten Build 
  (z. B. `assembleDebug`) inhaltlich exakt der Gradle-Konsolenausgabe entspricht.
```