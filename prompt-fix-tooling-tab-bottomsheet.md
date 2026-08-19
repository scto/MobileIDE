Strukturriere das Ausgabe-BottomSheet von MobileIDE (com.scto.mobile.ide) so um,
dass GENAU FÜNF klar getrennte Log-Tabs existieren und JEDE Log-/Terminal-Ausgabe
deterministisch in den richtigen Tab geroutet wird:

  1. INSTALL   – jegliche Installation (apt/pip/npm/curl-Installer, LSP-Server-
                 Installer, Plugin-Installationen, SDK-Setup)
  2. BUILD     – jegliche Build-Ausgabe (Gradle, APK-Builder, compile, createFinalZip)
  3. LSP       – LSP-Meldungen (connect, release, start, stop, errors, Status)
  4. DIAGNOSE  – Diagnose der in MobileIDE ENTWICKELTEN Applikation bezüglich
                 ihres Source Codes (publishDiagnostics, Problems, Compiler-Marker)
  5. IDE LOGS  – Logs, die MobileIDE selbst betreffen (LogCatcher, App-Interna,
                 Fehler/Stacktraces der IDE, Plugin-Lifecycle)

Kein Log darf mehr in einem falschen Tab landen. Bestehende Tabs (Terminal,
Problems, IDE, Build, LSP, Debug, Docs, AI) dürfen NICHT entfernt werden, wenn
sie eigene Funktionalität bieten – sie werden lediglich in die neue
Kanal-Struktur eingeordnet bzw. umbenannt, wo es dem neuen Modell entspricht.

══════════════════════════════════════════════════════════════════
KONTEXT / VERIFIZIERTER IST-ZUSTAND
══════════════════════════════════════════════════════════════════
- Modul :core:tooling:tooling-api      – Logging-Framework-Interfaces,
  GradleTaskDefinition (Loglevel/Channel? prüfen)
- Modul :core:tooling:tooling-impl     – Echtzeit-Logging-Panel + Gradle-Tasks-
  Panel mit Checklisten-UI (Tabs laut PROGRESS.md 2026-06-29: "Terminal Logs,
  Problems (Diagnostics), IDE Log, Build, LSP"; weitere Tabs: Debug (Prompt 10),
  Docs (Prompt 14), AI (Prompt 16))
- ToolingBottomSheet.kt                – zentraler BottomSheet-Host aller Tabs
- ResizablePanelLayout.kt (Zeile ~322) – Statusleiste mit "● LSP Fehler"-Indikator
- GradleLogLine.kt                     – Severity-Parsing (INFO/WARN/ERROR/TASK/SUCCESS)
- GradleTaskManager                    – streamt Logs via Flow<GradleLogLine>
- BuildHelper.kt                       – scannt Output-APKs, installiert via FileProvider
- KeystoreManagerDialog.kt             – GUI-Wizard im Build-Tab
- LogCatcher (core.common.utils)       – optionales IDE-Debug-Logging nach
  /sdcard/MobileIDEProjects/logs/
- launchTerminal / TerminalCommand     – TerminalCommand hat ein "id"-Feld
  (z. B. id="Typst installation" in TypstInstallationManager; ScriptedLspServer
  launchInstaller nutzt es für LSP-Installer)
- Install-Skripte: gopls-installer.sh, install-gopls.sh, java-lsp.sh,
  kmp-lsp-installer.sh, kotlin-lsp-installer.sh, rust-lsp.sh, typst-cli.sh,
  typst-lsp.sh, zig-installer.sh (apt install, curl-Downloads, npm/pip)
- LSP-Diagnostics: textDocument/publishDiagnostics → aktuell Problems/Diagnosis-
  Panel + inline squiggly underlines (Prompt 05); LspEditorExtensions.kt
  (Go-to-Def, Hover, Signature Help); LspRegistry.kt / ScriptedLspServer /
  EditorViewModel.kt (connect/Status)
- Screenshot-Befund: Terminal-Tab zeigt "Welcome to MobileIDE Terminal" +
  apt update/upgrade/install-Ausgaben – Installationen laufen im falschen Tab;
  Statusleiste zeigt "● LSP Fehler".

══════════════════════════════════════════════════════════════════
PHASE A – KANALMODELL & BESTANDSAUFNAHME (keine Änderung)
══════════════════════════════════════════════════════════════════
A1. Lokalisiere ALLE Stellen, die aktuell Logs/Terminal-Ausgaben an das
    BottomSheet senden. Erstelle build/log-source-inventory.tsv:
      quelle (Datei/Zeile) | sender-art | aktueller tab | inhaltstyp
    Mindestens prüfen: GradleTaskManager (BUILD), ApkBuilder/BuildHelper
    (BUILD), ScriptedLspServer.launchInstaller + LspRegistry + EditorViewModel
    (LSP), publishDiagnostics-Handler (DIAGNOSE), LogCatcher (IDE),
    TerminalSession/TerminalBackEnd (Terminal-Ausgabe), SetupWorker/TerminalSetupUI
    (INSTALL, Sandbox-Setup), Downloader (INSTALL, RootFS-Download),
    PluginStoreManager/PluginManager (INSTALL, Plugin-Installation),
    KeystoreManagerDialog (BUILD), AiderBridgeService (AI), DebugSessionManager (DEBUG).
A2. Dokumentiere die aktuellen Tab-IDs/Konstanten in ToolingBottomSheet.kt
    (Enum/Sealed-Klasse? Strings?) und wie der Tab-Wechsel + Auto-Switch
    funktioniert (Badges, Fehlerzähler, Auto-Scroll).
A3. Befund-Report build/log-routing-befund.md: Welche Ausgabe landet aktuell
    WOHIN, und welche Fehlleitungen existieren (z. B. apt im Terminal-Tab,
    LSP-Status im IDE-Log, Gradle-Ausgabe im Terminal statt Build).

══════════════════════════════════════════════════════════════════
PHASE B – LOG-KANAL-MODELL EINFÜHREN (:core:tooling:tooling-api)
══════════════════════════════════════════════════════════════════
B1. Führe ein verbindliches Kanalmodell ein (neue Datei z. B.
    LogChannel.kt in :core:tooling:tooling-api):
    enum class LogChannel { INSTALL, BUILD, LSP, DIAGNOSE, IDE_LOGS }
    inkl. displayName (deutsch/en: "Install", "Build", "LSP", "Diagnose",
    "IDE Logs") und optionalem BadgeKey.
B2. Alle bestehenden Log-Emitter-Interfaces (LogEntry/LogLevel/LogEvent o. ä.)
    erweitern: Jeder LogEvent bekommt ein Pflichtfeld channel: LogChannel.
    Fallback: Ein Default-Channel (IDE_LOGS) für unklassifizierte Events,
    ABER: Kein Event darf stillschweigend im Default landen – jede Sende-Stelle
    muss einen expliziten Kanal setzen (Compile-Zeit-Erzwingung bevorzugt).
B3. Kompatibilität: Bestehende Aufrufer (GradleTaskManager, LogCatcher, …)
    migrieren, ohne deren Public-API unnötig zu brechen. Falls ein
    Alias/Overload nötig ist, als @Deprecated markieren mit Hinweis auf den
    neuen channel-Parameter.

══════════════════════════════════════════════════════════════════
PHASE C – ROUTING-REGELN FÜR TERMINAL-/PROZESS-AUSGABEN
══════════════════════════════════════════════════════════════════
C1. TerminalCommand erhält ein neues Feld channel: LogChannel = INSTALL
    (Default) bzw. die Aufrufer setzen es explizit:
    - launchTerminal mit TerminalCommand(id="*installation*"|"*Typst*"|"*LSP*"|
      "Install"|"Update"|"Uninstall", …) → INSTALL
    - ScriptedLspServer.launchInstaller → INSTALL
    - TerminalCommand(id="zig.run", exe=".../zig", …) → RUNNER/Anders? NICHT
      Teil dieses Prompts – als "Terminal" ohne Channel belassen oder IDE_LOGS,
      im Bericht dokumentieren.
C2. Klassifizierungs-Helper LogRouter.classify(command: TerminalCommand):
    a) exe enthält "gradle"/"gradlew" ODER args enthalten "assemble*",
       "build", "createFinalZip", "compile*" → BUILD
    b) exe/args enthalten "apt", "apt-get", "pkg", "apk", "pip", "pip3",
       "npm", "npx", "gem", "cargo install", "curl ... -L ... install",
       "setup", "idesetup", "sdkmanager", "installer", ".sh" mit --install/
       --update/--uninstall → INSTALL
    c) sonst → IDE_LOGS (bzw. Terminal ohne Kanal, siehe C1)
    REGEL: Die id-Whitelist (C1) hat VORRANG vor dem exe/args-Parsing.
C3. Alle LSP-Installationspfade (TypstInstallationManager, ZigServer,
    RustServer, Java/Kotlin/Bash/XML-Server aus :features:lsp, Plugin-LSPs)
    explizit auf channel=INSTALL setzen – NICHT aufs Parsing verlassen.

══════════════════════════════════════════════════════════════════
PHASE D – SENDER MIGRIEREN (Kanal zuweisen)
══════════════════════════════════════════════════════════════════
D1. BUILD: GradleTaskManager (Flow<GradleLogLine>), ApkBuilder-Output,
    BuildHelper/KeystoreManagerDialog → channel=BUILD.
    - GradleLogLine wird beim Emittieren zu LogEvent(channel=BUILD, …)
      gemappt; Severity bleibt erhalten.
D2. LSP: LspRegistry/ScriptedLspServer/EditorViewModel:
    - connect/connected/disconnected/start/stop/Fehler/Installations-Status →
      channel=LSP.
    - Der "● LSP Fehler"-Indikator (ResizablePanelLayout.kt) speist weiterhin
      die Statusleiste, ABER schreibt ZUSÄTZLICH eine LSP-Logzeile
      ("LSP connect fehlgeschlagen: <Ursache>") in den LSP-Tab, statt nur die
      Statusfarbe zu wechseln (behebt stille Fehler).
D3. DIAGNOSE: textDocument/publishDiagnostics → channel=DIAGNOSE.
    - Diagnostics werden als Einträge mit datei/zeile/severity/message an den
      Diagnose-Tab gesendet (Bestehendes Problems-Panel nutzen, nur in den
      DIAGNOSE-Kanal überführen). Tippen auf einen Eintrag öffnet die Datei
      und springt zur Zeile (bestehende EditorNavigation wiederverwenden).
D4. IDE LOGS: LogCatcher + alle App-internen Logs
    (Log.d/w/e aus :app, :core:*, :features:*) → channel=IDE_LOGS.
    - LogCatcher.send bekommt einen channel-Parameter (Default IDE_LOGS).
D5. INSTALL: SetupWorker/TerminalSetupUI (Sandbox-Setup), Downloader
    (RootFS-Download, Fortschritt), PluginStoreManager/PluginManager
    (Installieren/Deinstallieren), alle LSP-/CLI-Installer → channel=INSTALL.
    - Auch apt-Installationen, die der Nutzer manuell im Terminal startet,
    müssen im Install-Tab erscheinen (via TerminalCommand.channel, sofern die
    Ausgabe über launchTerminal läuft – dokumentieren, falls ein
    interaktives Terminal NICHT umleitbar ist und im Terminal-Tab bleiben
    muss; das dann als bewusste Ausnahme im Bericht markieren).

══════════════════════════════════════════════════════════════════
PHASE E – UI: TOOLINGBOTTOMSHEET AUF 5 KERNTABS UMBAUEN
══════════════════════════════════════════════════════════════════
E1. ToolingBottomSheet.kt: Neue Tab-Reihenfolge (feste Kern-Tabs):
       Install | Build | LSP | Diagnose | IDE Logs
    - Bestehende Zusatz-Tabs (Debug, Docs, AI, Gradle Tasks) bleiben erhalten
      und werden NACH den 5 Kerntabs einsortiert. Der alte "Terminal"-Tab
      entfällt als Log-Tab ODER wird zu einem reinen interaktiven
      Terminal-Tab (keine Log-Kategorie) – Entscheidung treffen und
      dokumentieren. Der alte "Problems"-Tab wird zum "Diagnose"-Tab.
    - Der alte "IDE"-Tab wird zu "IDE Logs".
E2. Jeder Kerntab zeigt:
    - Badge mit Anzahl aktiver Einträge (z. B. Fehlerzähler rot),
    - Auto-Switch auf den Tab, wenn eine neue Fehlermeldung dort eintrifft
      (Konfigurierbar über bestehende Einstellungen, Default: nur bei ERROR),
    - Auto-Scroll ans Ende bei Live-Ausgabe, Stop-/Clear-Button pro Tab.
E3. TerminalCommand-Empfang: Wenn ein Terminal-Befehl mit channel=INSTALL
    läuft, öffnet/schaltet das BottomSheet automatisch auf den Install-Tab
    (sofern nicht vom Nutzer fixiert).
E4. Beim LSP-Statuswechsel (D2): Bei Fehler automatisch einen Hinweis-Badge
    auf dem LSP-Tab setzen; Klick öffnet den LSP-Tab mit der Fehlerzeile.
E5. ResizablePanelLayout.kt: "● LSP Fehler"-Indikator bleibt, aber zusätzlich
    Klick auf den Indikator öffnet den LSP-Tab (nicht nur das Panel).

══════════════════════════════════════════════════════════════════
PHASE F – VALIDIERUNG & TESTS
══════════════════════════════════════════════════════════════════
F1. ./gradlew assembleDebug → BUILD SUCCESSFUL.
F2. Unit-Tests für LogRouter.classify (Phase C2): Mindestfälle
    apt install → INSTALL; ./gradlew assembleRelease → BUILD;
    typst-lsp.sh --install → INSTALL; zig run → Terminal/IDE_LOGS;
    publishDiagnostics → DIAGNOSE; unbekannt → IDE_LOGS.
F3. Manuelle Tests auf dem Gerät:
    a) LSP installieren (z. B. Zig-ZLS) → Ausgabe erscheint NUR im
       Install-Tab, LSP-Status im LSP-Tab, KEIN Terminal-Tab-Eintrag.
    b) Projekt bauen (Play-Button/Gradle-Tasks) → Ausgabe NUR im Build-Tab.
    c) Kotlin-Datei mit Syntaxfehler öffnen → Diagnose-Eintrag erscheint
       im Diagnose-Tab (datei:zeile), squiggle weiterhin im Editor.
    d) Absichtlich fehlerhaften LSP-Start provozieren → Fehlerzeile im
       LSP-Tab + Badge + Klick öffnet LSP-Tab.
    e) LogCatcher einschalten → IDE-Logs im IDE-Logs-Tab.
    f) Regression: Debug/Docs/AI-Tabs weiterhin funktionsfähig.
F4. Erstelle build/log-tabs-report.md: Quelle → Kanal-Tabelle (Phase A3
    aktualisiert), Screenshot-Beschreibungen vorher/nachher, Abweichungen.
F5. Commits:
    "feat(logging): introduce LogChannel model (install/build/lsp/diagnose/ide)"
    "feat(ui): rebuild tooling bottom sheet tabs with channel routing"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase A MUSS vollständig sein (alle Sender erfasst), BEVOR Kanäle eingeführt
  werden. Fehlende Quelle → als offener Punkt listen, nicht raten.
- Phase B bricht ab, wenn ein bestehender Aufrufer nicht ohne API-Bruch
  migrierbar ist → Overload-Muster verwenden, nicht die alte API entfernen.
- Phase C: Ein TerminalCommand ohne klare Klassifizierung darf NIE einfach in
  einen beliebigen Tab wandern – Default ist IDE_LOGS mit Logzeile
  "unclassified terminal command: <exe>".
- Phase E: KEIN bestehender Feature-Tab (Debug/Docs/AI/Gradle-Tasks) darf
  durch den Umbau Funktionalität verlieren.
