Ermittle den EXAKTEN IST-ZUSTAND der Log-Kanal- und ToolingBottomSheet-Architektur
in MobileIDE (com.scto.mobile.ide), NACHDEM Commit c9561d5b bereits die Dateien
LogRouter.kt, ToolingLogManager.kt und ToolingBottomSheet.kt angefasst hat.

ZIEL: Ein verifizierter Stand-Bericht, der beantwortet, WELCHER Teil des geplanten
5-Tab-Zielmodells (INSTALL | BUILD | LSP | DIAGNOSE | IDE_LOGS) bereits umgesetzt
ist, WELCHER noch fehlt und WO der spätere Log-Tab-Umbau-Prompt nahtlos ansetzen
kann – OHNE bestehenden Code zu verändern (dies ist ein REINER LESE-/DIAGNOSE-
Prompt, KEINE Code-Änderung).

══════════════════════════════════════════════════════════════════
VERIFIZIERTER AUSGANGSPUNKT (aus gradle-path-fix2-report.md §2 + MobileIDE-Dump)
══════════════════════════════════════════════════════════════════
- Commit c9561d5bc61ae0d0529ff8710d7ab6ee0c6d1e7d (Branch main, gepusht) enthält
  U. a. folgende Dateien (Hinweis: der Commit mischt Pfad-Fix UND Log-Architektur):
  * core/common/src/main/java/com/scto/mobile/ide/core/common/utils/ProjectPaths.kt
  * app/src/main/java/com/scto/mobile/ide/ui/editor/CodeEditScreen.kt
  * app/src/main/java/com/scto/mobile/ide/ui/settings/SettingsScreen.kt
  * core/tooling/tooling-api/src/main/java/com/scto/mobile/ide/core/tooling/api/LogRouter.kt
  * core/tooling/tooling-api/src/main/java/com/scto/mobile/ide/core/tooling/api/ToolingLogManager.kt
  * core/tooling/tooling-impl/src/main/java/com/scto/mobile/ide/core/tooling/impl/ui/ToolingBottomSheet.kt
- PROGRESS.md (2026-06-29): "Categorized Logs" – Echtzeit-Routing in fünf Tabs
  im BottomSheet: *Terminal Logs*, *Problems (Diagnostics)*, *IDE Log*, *Build*, *LSP*.
- PROGRESS.md (2026-07-01): Terminal-Close-Verhalten "new_session"; TerminalBackEnd.kt
  mit decoupled Session-Close-Callback.
- PROGRESS.md (2026-07-26): Tabs 7–9 ergänzt: Debug (Prompt 10), Docs (Prompt 14),
  AI (Prompt 16); dazu Gradle Tasks Panel (Build-Tab).
- Module: :core:tooling:tooling-api (Interfaces) und :core:tooling:tooling-impl
  (Echtzeit-Panel + Gradle-Tasks-Panel).
- Verwandte Sender (aus Dump/Reports):
  * GradleLogLine.kt – Severity-Parsing (INFO/WARN/ERROR/TASK/SUCCESS),
  * GradleTaskManager(Impl) – streamt Logs via Flow<GradleLogLine>,
  * BuildHelper.kt / KeystoreManagerDialog.kt – Build-Tab-Zubehör,
  * TerminalCommand (mit id-Feld: z. B. "Typst installation", "zig.run") +
    launchTerminal,
  * ScriptedLspServer.launchInstaller / LspRegistry / EditorViewModel.kt (connect),
  * publishDiagnostics → Problems/Diagnosis-Panel (Prompt 05),
  * LogCatcher (core.common.utils) + LogConfigRepository + LogEntry,
  * SetupWorker / TerminalSetupUI / Downloader (Sandbox-Setup, RootFS),
  * ResizablePanelLayout.kt – Statusleiste "● LSP Fehler".
- Screenshot-Befund (log-BottomSheet): Terminal-Tab zeigt "Welcome to MobileIDE
  Terminal" + apt-Ausgaben; Statusleiste zeigt "● LSP Fehler" – Kategorisierung
  NICHT sauber erkennbar.

══════════════════════════════════════════════════════════════════
PHASE 1 – BESTANDSINVENTAR DER 3 KERN-DATEIEN (LESEN, NICHT ÄNDERN)
══════════════════════════════════════════════════════════════════
1.1. core/tooling/tooling-api/.../LogRouter.kt:
     a) Vollständigen Inhalt dokumentieren (Klassen, Funktionen, Signaturen),
     b) Existiert ein LogChannel/LogCategory-Enum oder String-Konstanten?
        Welche Werte genau (Terminal/Problems/IDE/Build/LSP …)?
     c) Existiert eine classify()-Funktion (TerminalCommand/Prozess → Kanal)?
        Falls ja: Welche Regeln implementiert sie (exe-Strings, args, id-Whitelist)?
     d) Ist LogRouter eine Klasse, ein Objekt oder reine Top-Level-Funktionen?
        Wird sie woanders referenziert (grep -rn "LogRouter")?
1.2. core/tooling/tooling-api/.../ToolingLogManager.kt:
     a) Inhalt dokumentieren; welche Zustände/Flows hält er
        (z. B. MutableStateFlow<List<LogEntry>>, Broadcasts pro Kanal)?
     b) Wie werden Log-Einträge EINGESPEIST (send(event), add(entry), …)?
        Wie werden sie KONSUMIERT (collectAsState, Flow, Callbacks)?
     c) Gibt es bereits Badge-/Fehlerzähler- oder Auto-Switch-Mechanik?
     d) Wie interagiert ToolingLogManager mit LogRouter (Aufrufrichtung)?
1.3. core/tooling/tooling-impl/.../ToolingBottomSheet.kt:
     a) Vollständige aktuelle Tab-Liste inkl. Reihenfolge und IDs/Enums
        (vermutlich: Terminal, Problems, IDE, Build, LSP, Debug, Docs, AI,
        Gradle Tasks o. ä.),
     b) Welche Tabs sind reine Log-Anzeigen, welche sind Funktions-Panels
        (Debug-Steuerung, Docs, AI-Chat, Gradle-Tasks-Checkliste)?
     c) Wie werden Tab-Badges/Fehlerzähler aktuell gepflegt?
     d) Existiert bereits der Kanal-Begriff "INSTALL", "DIAGNOSE" oder
        "IDE_LOGS" irgendwo (auch in Strings/Resources)?
1.4. Ergebnis als build/log-recon-inventory.md:
     Tabelle: Datei | Klassen/Symbole | Kanal-Konzept | Sender-Kopplung |
     bereits-umgesetzt (JA/TEILWEISE/NEIN) | Kommentar.

══════════════════════════════════════════════════════════════════
PHASE 2 – SENDER-BESTANDSAUFNAHME (wer sendet WOHIN heute?)
══════════════════════════════════════════════════════════════════
2.1. Für jeden bekannten Sender feststellen, an WELCHEN Tab/Kanal er heute
     tatsächlich sendet (NUR Lesen, Sender-Code-Zeilen + aktuelle Ziel-API):
     - GradleTaskManager / GradleTaskManagerImpl (BUILD-Log),
     - BuildHelper.kt / ApkBuilder.kt / KeystoreManagerDialog.kt,
     - TerminalCommand / launchTerminal (alle Aufrufer: TypstInstallationManager,
       ZigRunner, ScriptedLspServer.launchInstaller, SetupWorker, …),
     - ScriptedLspServer / LspRegistry / EditorViewModel.kt (LSP-Status/connect),
     - publishDiagnostics-Handler (Problems/Diagnose),
     - LogCatcher (+ LogConfigRepository/LogEntry),
     - ResizablePanelLayout.kt (Statusleisten-Indikator "● LSP Fehler").
2.2. Ergebnis als build/log-sender-matrix.tsv:
     sender | datei:zeile | ziel-tab-heute | inhaltstyp |
     zielkanal-laut-5-Tab-Modell (ISTALL/BUILD/LSP/DIAGNOSE/IDE_LOGS) |
     konflikt? (JA/NEIN).
     KONFLIKT = Sender leitet heute in einen anderen Tab als das 5-Tab-Ziel
     verlangt (z. B. apt-Ausgabe im Terminal-Tab statt INSTALL).

══════════════════════════════════════════════════════════════════
PHASE 3 – STAND-BEWERTUNG GEGEN DAS 5-TAB-ZIELMODELL
══════════════════════════════════════════════════════════════════
3.1. Bewerte für JEDES der fünf Ziel-Kanäle separat:
     INSTALL  → existiert Kanal/Log-Tab? Welche Sender laufen heute hinein?
     BUILD    → Gradle-Tab vorhanden? Vollständige Abdeckung (alle Gradle-Starts)?
     LSP      → LSP-Status-Tab vorhanden? Wo landen connect/start/stop/errors?
     DIAGNOSE → Existiert das Problems-Panel? Wie ist es an publishDiagnostics
                gekoppelt? Wird die APP-Diagnose (Source-Code) davon getrennt?
     IDE_LOGS → Existiert der IDE-Log-Tab? Werden LogCatcher-Ausgaben dorthin
                geroutet?
3.2. Feststellen, ob LogRouter/ToolingLogManager (aus c9561d5b) bereits ein
     Kanalmodell einführen, das das 5-Tab-Modell VORWEGNAHMT (z. B. durch
     Enum-Werte wie INSTALL/DIAGNOSE/IDE_LOGS) ODER ob sie nur das ALTE
     5-Tab-Modell (Terminal/Problems/IDE/Build/LSP) nachbilden.
3.3. Ergebnis als build/log-recon-befund.md mit:
     a) Kanal-Reifegrad-Matrix (5 Ziele × Status: VOLL/TEILWEISE/FEHLT),
     b) Liste der KONFLIKTE aus Phase 2 (Sender → falscher Tab),
     c) Liste der Dateien, die der spätere Log-Tab-Umbau-Prompt NICHT mehr
        anfassen muss (weil bereits fertig) vs. anfassen muss,
     d) Empfehlung: Soll der spätere Log-Tab-Prompt auf LogRouter/
        ToolingLogManager AUFBAUEN (weiterverwenden) oder sie ERSETZEN?
        (Begründung anhand der tatsächlichen API, NICHT anhand der Namen).

══════════════════════════════════════════════════════════════════
PHASE 4 – ABSCHLUSS
══════════════════════════════════════════════════════════════════
4.1. ./gradlew assembleDebug ausführen NUR zur Verifikation, dass der IST-Zustand
     kompilierbar ist (KEINE Code-Änderung, reiner Lese-Beweis).
4.2. KEIN Commit/Push (reine Diagnose; nichts zu committen).
4.3. build/log-recon-inventory.md, build/log-sender-matrix.tsv und
     build/log-recon-befund.md liegen bereit als Eingabe für den späteren
     Log-Tab-Umbau-Prompt (Install/Build/LSP/Diagnose/IDE Logs).

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Es dürfen KEINERLEI Änderungen an Kotlin-/XML-/Resource-Dateien vorgenommen
  werden – dieser Prompt ist strikt LESE-ONLY.
- Falls LogRouter.kt oder ToolingLogManager.kt im Working Tree NICHT existieren
  (trotz Commit-Nachweis): NICHT raten – den Befund "Datei fehlt im Working Tree,
  obwohl in c9561d5b enthalten (git show c9561d5b --stat verifizieren)" in den
  Report schreiben und als offenen Punkt markieren.
- Falls der Build fehlschlägt: Fehler EXAKT dokumentieren (Datei:Zeile + Message),
  KEINE automatischen Korrekturen vornehmen.
- Keine Bewertung, welche Tab-Umsetzung "besser" ist – nur Fakten sammeln,
  damit der Folge-Prompt eine belastbare Basis hat.
