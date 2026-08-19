Setze die Log-Kanal-Architektur von MobileIDE (com.scto.mobile.ide) FINAL um.
Die 5-Kanal-Architektur ist im Code BEREITS VOLLSTÄNDIG implementiert, aber an
EINER kritischen Stelle NICHT verdrahtet. Dieser Prompt schließt genau diese Lücke
und konsolidiert das BottomSheet auf 5 Kerntabs.

ZIELZUSTAND:
  Der Terminal-/Log-BottomSheet zeigt GENAU FÜNF Kerntabs:
    1. INSTALL  – Installationen (apt/pip/npm/curl-Installer, LSP-Installer,
                  Typst/zig/rust-Server-Setup, Plugin-Installation, Sandbox-Setup)
    2. BUILD    – Gradle-/APK-Build-Ausgabe
    3. LSP      – LSP-Meldungen (connect/start/stop/Fehler/Status)
    4. DIAGNOSE – publishDiagnostics der ENTWICKELTEN App (datei:zeile, severity)
    5. IDE LOGS – MobileIDE-interne Logs (LogCatcher, App-Interna, Stacktraces)
  Bestehende Zusatz-Tabs (DEBUG, DOCS, AI, Gradle Tasks) bleiben als
  Funktions-Panels NACH den 5 Kerntabs erhalten. Der alte "Terminal"-Tab wird
  als Log-Kategorie ABGESCHAFFT (interaktive Terminals bleiben nutzbar, aber
  ihre Ausgabe wird kategorisiert).

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND (aus log-recon-befund.md, log-recon-inventory.md,
log-sender-matrix.tsv, MobileIDE-Dump, gradle-path-fix2-report.md §2)
══════════════════════════════════════════════════════════════════
- Die Architektur existiert BEREITS (aus Commit c9561d5b):
  * core/tooling/tooling-api/.../LogRouter.kt
    → object LogRouter mit classify(cmdShell, cmdArgs, cmdId): LogChannel
      (deterministische Heuristik: exe-Strings, args, id-Whitelist)
  * core/tooling/tooling-api/.../ToolingLogManager.kt
    → enum LogChannel (5 Kanäle: INSTALL, BUILD, LSP, DIAGNOSE, IDE_LOGS),
      enum ToolingLogCategory (inkl. DEBUG/DOCS/AI),
      SharedFlow<ToolingLogEntry>, ToolingLogManagerImpl
  * core/tooling/tooling-impl/.../ToolingBottomSheet.kt
    → iteriert aktuell über ToolingLogCategory.values() = 8 Tabs
- HAUPTLÜCKE (befund §3): LogRouter.classify() wird an der
  TERMINAL-COMMAND-AUSFÜHRUNG (launchTerminal-Runner) NICHT aufgerufen.
  Folge: Terminal-basierte Installer senden NICHT in den INSTALL-Tab.
  Beispiel (sender-matrix, Konflikt JA):
  * TypstInstallationManager.kt:134 – manageInstallation nutzt
    launchTerminal(TerminalCommand(id="Typst installation", exe="/bin/bash", …))
    → Ausgabe landet heute im Terminal-Fenster statt im INSTALL-Kanal.
  * Weitere betroffene Sender: zig-lsp.sh/rust-lsp.sh/gopls-installer.sh/
    kotlin-lsp-installer.sh/kmp-lsp-installer.sh/java-lsp.sh/typst-cli.sh/
    typst-lsp.sh (alle laufen via launchTerminal mit id="*installation*"-Mustern).
- Bereits korrekt geroutet (sender-matrix, KEIN Handlungsbedarf):
  * GradleTaskManagerImpl.kt:209 → BUILD ✅
  * EditorViewModel.kt:1603 (publishDiagnostics) → DIAGNOSE ✅
  * LogCatcher.kt:41/42/43/175 (Tag-Routing) → BUILD/INSTALL/LSP/IDE_LOGS ✅
  * ApkBuilder/BuildHelper/KeystoreManagerDialog → BUILD ✅
  * ScriptedLspServer/LspRegistry/EditorViewModel.connect → LSP ✅
- Statusleiste: ResizablePanelLayout.kt zeigt "● LSP Fehler" – aktuell wird
  NUR die Statusfarbe gewechselt, KEINE LSP-Logzeile geschrieben.
- TerminalCommand (core) hat ein id-Feld (z. B. "Typst installation", "zig.run").
- Settings-Screenshot: Terminal-Tab zeigt apt-Ausgaben ("Welcome to MobileIDE
  Terminal") – Beleg für die Fehlleitung.
- Tests: :core:apk-builder:testDebugUnitTest (16 grün), assembleDebug grün.

══════════════════════════════════════════════════════════════════
PHASE A – LogRouter.Classify() AN DER TERMINAL-AUSFÜHRUNG VERDRAHTEN
══════════════════════════════════════════════════════════════════
A1. Lokalisiere den launchTerminal-/Terminal-Runner (der die
    TerminalCommand-Instanzen ausführt, vermutlich im :features:terminal
    TerminalViewModel/TerminalRunner bzw. im Tooling-Modul). Prüfe, wo die
    ProcessBuilder-Output-Streams (stdout/stderr) gelesen und an die
    Terminal-UI geschickt werden.
A2. VERDRAHTUNG (Kernänderung dieses Prompts):
    a) Beim Ausführen EINES TerminalCommand: führe VOR dem Start aus:
         val channel = LogRouter.classify(cmd.exe, cmd.args, cmd.id)
    b) Sende JEDE Zeile stdout/stderr ZUSÄTZLICH an
       ToolingLogManager.send(channel, line, severity) – die Zeile bleibt
       parallel im interaktiven Terminal sichtbar (beide Anzeigen koexistieren).
    c) Kein Sonderfall: NICHT nur bei id="*installation*" routen, sondern
       IMMER über classify() (deterministisch), damit auch unbekannte
       Befehle sicher nach IDE_LOGS fallen statt ins Leere.
A3. INSTALL-Sender abdecken: Für alle LSP-/CLI-Installer (TypstInstallation-
    Manager, ZigServer, RustServer, Java/Kotlin/Bash/XML-Server, SetupWorker,
    Downloader, PluginManager) sicherstellen, dass classify() deren
    TerminalCommand-id bzw. exe/args korrekt als INSTALL erkennt.
    Falls classify() diese Fälle NICHT erkennt: Whitelist in LogRouter
    ergänzen (id-Präfixe "install", "setup", "Typst", exe-Whitelist
    "apt", "pip", "npm", "curl -L", "sh <installer>.sh" u. ä.) –
    ABER: jede Ergänzung mit Unit-Test belegen (siehe Phase E).
A4. TerminalCommand-Erweiterung (falls nötig): Falls das TerminalCommand
    kein Feld für die "interaktiv vs. log-only" Unterscheidung hat, prüfen,
    ob ein optionales Feld channelOverride sinnvoll ist (Default null →
    classify() entscheidet). NICHT zwingend – nur falls A3 es erfordert.

══════════════════════════════════════════════════════════════════
PHASE B – TOOLINGBOTTOMSHEET AUF 5 KERNTABS KONSOLIDIEREN
══════════════════════════════════════════════════════════════════
B1. Tab-Reihenfolge festlegen (ToolingBottomSheet.kt):
      [INSTALL] [BUILD] [LSP] [DIAGNOSE] [IDE LOGS]
      ── Trennlinie/Divider ──
      [DEBUG] [DOCS] [AI] [Gradle Tasks]
    - Die 5 Kerntabs binden an ToolingLogCategory der 5 LogChannel-Werte,
    - DEBUG/DOCS/AI/Gradle Tasks bleiben als Funktions-Panels erhalten
      (keine Funktionalität entfernen, nur einsortieren).
B2. Der alte "Terminal"-Tab: ENTFERNEN als Log-Kategorie. Prüfen, ob er
    zusätzlich als "interaktives Terminal" eigene Funktion hat:
    a) Falls ja: als eigenständiges Panel hinter den Kerntabs belassen
       (Titel "Terminal") OHNE Log-Charakter, NICHT im Kanal-Routing,
    b) Falls nein: komplett entfernen.
    Entscheidung dokumentieren.
B3. Badges/Fehlerzähler: Pro Kerntab Badge mit Anzahl aktiver Einträge;
    ERROR-Einträge heben den Zähler hervor (rot). Auto-Switch auf den Tab
    bei neuem ERROR (Default: nur ERROR, konfigurierbar).
B4. Auto-Scroll + pro-Tab Controls: Live-Ausgabe auto-schreibt ans Ende;
    je Tab ein Stop-/Clear-Button (bestehende Mechanik wiederverwenden,
    falls vorhanden).
B5. Konsistenz: ToolingLogCategory-Labels im UI deutsch/englisch anzeigen:
    "Install", "Build", "LSP", "Diagnose", "IDE Logs" (displayName in der
    Enum bereits vorhanden? prüfen, ggf. ergänzen).

══════════════════════════════════════════════════════════════════
PHASE C – "● LSP FEHLER"-INDIKATOR AN LSP-TAB KOPPELN
══════════════════════════════════════════════════════════════════
C1. ResizablePanelLayout.kt (Statusleiste): Der "● LSP Fehler"-Indikator
    speist weiterhin die Statusfarbe, schreibt aber ZUSÄTZLICH bei jedem
    LSP-Fehler eine strukturierte Zeile in den LSP-Kanal:
      [LSP ERROR] LSP connect fehlgeschlagen: <Ursache> (server: <name>)
    statt nur die Farbe zu wechseln (behebt stille Fehler).
C2. Klick auf den Indikator öffnet den LSP-Tab (nicht nur das Panel) –
    bestehende Tab-Switch-Mechanik von ToolingBottomSheet nutzen.
C3. Regression: ResizablePanelLayout-Statusanzeige (Größe, Layout) bleibt
    unverändert; nur die Kopplung an ToolingLogManager kommt hinzu.

══════════════════════════════════════════════════════════════════
PHASE D – KONFLIKTE AUS SENDER-MATRIX ABARBEITEN (Installations-Pfade)
══════════════════════════════════════════════════════════════════
D1. Für JEDE Konflikt-Zeile aus log-sender-matrix.tsv (sender → Terminal
    statt INSTALL) nach dem Umbau erneut prüfen:
    a) Startet der Sender ein TerminalCommand? → läuft ab jetzt über
       classify() (Phase A),
    b) Schreibt der Sender DIREKT in die Terminal-UI (ohne TerminalCommand)? →
       auf ToolingLogManager.send(channel=INSTALL, …) umstellen,
    c) Sender ohne klar erkennbaren Kanal → IDE_LOGS (Default) mit Logzeile
       "unclassified terminal command: <exe>".
D2. Plugin-Installationen (PluginStoreManager/PluginManager) und
    Sandbox-Setup (SetupWorker/TerminalSetupUI, Downloader RootFS-Fortschritt):
    auf INSTALL-Kanal sicherstellen.
D3. Debug-Store (DebugSessionManager) und AiderBridgeService (AI): bleiben in
    DEBUG/AI (Funktions-Panels), KEINE Kanal-Umleitung.

══════════════════════════════════════════════════════════════════
PHASE E – VALIDIERUNG & BERICHT
══════════════════════════════════════════════════════════════════
E1. Unit-Tests für LogRouter.classify (LogRouterTest ergänzen):
    a) apt install → INSTALL,
    b) ./gradlew assembleRelease → BUILD,
    c) bash typst-lsp.sh --install → INSTALL,
    d) bash zig.run / zig run → Terminal/IDE_LOGS (bewusste Ausnahme),
    e) publishDiagnostics → DIAGNOSE,
    f) unbekannt → IDE_LOGS,
    g) id="Typst installation" → INSTALL (id-Whitelist schlägt exe/args).
    ALLE neuen Tests grün, bestehende 16 Tests unverändert grün.
E2. ./gradlew assembleDebug → BUILD SUCCESSFUL (KEINE Compile-Fehler durch
    Tab-Umbau oder Verdrahtung).
E3. Manuelle Tests auf dem Gerät:
    a) Typst-LSP installieren → Ausgabe NUR im INSTALL-Tab, nichts im
       Terminal-Log-Bereich (interaktives Terminal zeigt weiterhin Output),
    b) Projekt bauen (Play-Button) → Ausgabe NUR im BUILD-Tab,
    c) Kotlin-Datei mit Syntaxfehler → Diagnose-Eintrag im DIAGNOSE-Tab
       (datei:zeile), Squiggle weiterhin im Editor,
    d) LSP-Start absichtlich fehlerhaft → Fehlerzeile im LSP-Tab + Badge +
       Klick auf Statusindikator öffnet LSP-Tab,
    e) LogCatcher-Toggle → IDE-Logs im IDE-LOGS-Tab,
    f) DEBUG/DOCS/AI/Gradle-Tasks-Panels weiterhin funktionsfähig,
    g) Interaktives Terminal: manuelles apt install bleibt tippbar und
       zeigt Ausgabe (Parallelität Terminal + INSTALL-Tab).
E4. Report build/log-tabs-report.md:
    - Vorher/Nachher-Tabelle (Datei | Änderung | Test),
    - Sender-Matrix-Abgleich (alle Konflikte gelöst? offene Punkte),
    - Entscheidung Terminal-Tab (B2) dokumentiert,
    - Screenshot-Beschreibung der neuen 5 Kerntabs.
E5. Commits:
    "feat(logging): wire LogRouter.classify into terminal command execution,
    route installers to INSTALL channel"
    "feat(ui): consolidate tooling bottom sheet into 5 core log tabs,
    keep debug/docs/ai/gradle-tasks panels"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- KEINE Änderung an bereits korrekt gerouteten Sendern (BUILD/DIAGNOSE/LSP/
  IDE_LOGS laut Sender-Matrix) – nur Verifikation.
- Falls classify() einen Fall NICHT erkennt, NICHT hartkodieren – Whitelist
  in LogRouter ergänzen und mit Unit-Test belegen.
- DEBUG/DOCS/AI/Gradle-Tasks-Panels dürfen durch den Umbau KEINE Funktion
  verlieren (Abbruch, falls ein Panel nicht mehr erreichbar ist).
- Terminal-Ausgaben bleiben im interaktiven Terminal sichtbar – das Routing
  in die Kanäle ist ZUSÄTZLICH, nicht exklusiv.
