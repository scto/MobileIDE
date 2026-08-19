Schließe die vier dokumentierten Lücken des Reports
~/MobileIDE/build/terminal-settings-back-fix-report.md NACHTRÄGLICH und beweise, dass
der 4-Bug-Fix (Distro-Auswahl, Session-Settings, Back-Geste, Drawer) in MobileIDE
(com.scto.mobile.ide) vollständig, nachvollziehbar und auf dem Gerät verifiziert ist.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND (aus terminal-settings-diagnose.md + Fix-Report)
══════════════════════════════════════════════════════════════════
- Fix-Report Phase B (Distro, konkret belegt):
  SettingsScreen.kt: listOf("ubuntu", "alpine") + Migration "debian" → "alpine"
  (TerminalEnvironmentSelector.kt:40-56 hat bereits ALPINE/UBUNTU/ANDROID).
- Fix-Report Phase D (BackHandler, konkret belegt):
  CodeEditScreen.kt: Kette showToolingBottomSheet → showPreviewBottomSheet →
  isOpenSearch → isOpenJump → drawerState.
- Fix-Report Phase C (Session-Settings): NUR "Verified" – kein Diff, keine Datei:Zeile.
- Fix-Report Phase E (Drawer): NUR "Verified" – sessionOrder/SSOT bestätigt, aber die
  von der Diagnose empfohlene eindeutige Formatierung "${index + 1} · Titel"
  (terminal-settings-diagnose.md, Bug 4) ist NICHT als implementiert belegt.
- Diagnose-Befund Bug 3 (Back): Inventar TerminalScreen.kt:504 (Drawer),
  ResizablePanelLayout.kt:156 (Panel), ProjectListScreen.kt:127 (Search) – die Kette
  in CodeEditScreen deckt diese drei Kontexte NICHT ab.
- KEIN Commit-Hash im Fix-Report (anders als c9561d5b / d24c7842).
- Build: assembleDebug → BUILD SUCCESSFUL in 15m 16s (626 Tasks).
- Settings-UI liegt in app/.../ui/settings/SettingsScreen.kt (NICHT :features:terminal),
  Persistenz via SharedPreferences: selected_distro, terminal_font_size,
  scrollback_lines, terminal_colorscheme, terminal_close_behavior.

══════════════════════════════════════════════════════════════════
PHASE A – GIT-NACHWEIS
══════════════════════════════════════════════════════════════════
A1. git -C ~/MobileIDE status + git log --oneline -10:
    a) Existiert ein Commit mit SettingsScreen.kt (Distro-Liste) + CodeEditScreen.kt
       (BackHandler-Kette)?
    b) Falls JA: Hash + Message + Branch notieren; Push-Status prüfen (branch -vv).
    c) Falls NEIN (uncommittet): diff gegen Working Tree prüfen (muss exakt dem
       Report entsprechen), dann committen:
       "fix(terminal-settings): replace debian with alpine in distro selector with
       legacy migration; add layered BackHandler in editor"
A2. Report-Abschnitt "Git-Nachweis" ergänzen (Hash, Branch, git show --stat).

══════════════════════════════════════════════════════════════════
PHASE B – BUG 4 DRAWER: EINDEUTIGE FORMATIERUNG BEWEISEN ODER IMPLEMENTIEREN
══════════════════════════════════════════════════════════════════
B1. TerminalScreen.kt (Drawer-LazyColumn, siehe Diagnose):
    Prüfe, ob der Eintrag aktuell so gerendert wird:
      Text(text = service.getDisplayTitle(session_id))
    a) Falls JA → Umsetzen auf eindeutige Anzeige:
       "${index + 1} · ${service.getDisplayTitle(session_id)}"
       (optional Zusatz: Projektname aus SessionWorkingDirectory,
       z. B. "1 · ubuntu · MyApp").
    b) Falls bereits geändert → als Diff belegen (Datei:Zeile, vorher/nachher).
B2. sessionOrder-Konsistenz prüfen: Wird bei createSession (Einfügen) und bei
    terminateSession (entkoppelter Binder) die SnapshotStateList sessionOrder
    zuverlässig aktualisiert? Falls createSession die Session NICHT sofort in
    sessionOrder aufnimmt (nur in die LinkedHashMap) → fixen (das wäre die
    eigentliche Ursache "Drawer zeigt nur 1 Sitzung").
B3. On-Device-Test: 3 Sessions öffnen (2× ubuntu, 1× alpine) → Drawer MUSS zeigen:
    "1 · ubuntu", "2 · ubuntu", "3 · alpine". Umschalten per Klick, Schließen per
    X aktualisiert die Liste. Screenshot-Datei build/screenshots/drawer-3-sessions.png.

══════════════════════════════════════════════════════════════════
PHASE C – BUG 2 SESSION-SETTINGS: DIFF-BEWEIS STATT "VERIFIED"
══════════════════════════════════════════════════════════════════
C1. Zeige für jede Einstellung den konkreten Codepfad MIT Datei:Zeile auf:
    - terminal_font_size → Settings.kt → scaledDensity → TerminalView.setTextSize
    - scrollback_lines → Emulator-Konfiguration (wo genau?)
    - terminal_colorscheme → ColorSchemeManager → TerminalView
    - terminal_close_behavior → CloseLastSessionBehavior (EXIT_APP=0 / NEW_SESSION=1)
    - selected_distro → DistroManager.getDistroName → MOBILEIDE_DISTRO/init-host
C2. WICHTIG: Prüfe, ob die Settings tatsächlich BEIM SESSION-START gelesen werden
    (nicht erst beim Erstellen des Settings-Screens). Falls eine Einstellung nur
    beim Einstellen (SettingsScreen) gilt, aber beim Session-Start nicht gelesen
    wird → Bug ist NICHT behoben → fixen (Datei:Zeile + vorher/nachher).
C3. Test: Schriftgröße 16→24 ändern, neue Session öffnen → Schrift ist sofort
    größer. FarbSchema ändern → neue Session zeigt neues Schema. Ergebnis im Report.

══════════════════════════════════════════════════════════════════
PHASE D – BUG 3 BACK-KETTE UM TERMINAL-/PANEL-KONTEXTE ERGÄNZEN
══════════════════════════════════════════════════════════════════
D1. Die bestehende Kette (CodeEditScreen.kt) ist NUR der Editor-Kontext. Ergänze
    eine konsistente Back-Reihenfolge an den in der Diagnose gefundenen Stellen:
    - TerminalScreen.kt:504: BackHandler(drawerState.isOpen) → drawer schließen;
      zusätzlich bei geschlossenem Drawer: wenn letzte Session offen →
      CloseLastSessionBehavior anwenden (new_session → neue Session, exit_app →
      finish), sonst eine Ebene zurück,
    - ResizablePanelLayout.kt:156: Panel minimieren vor App-Finish,
    - ProjectListScreen.kt:127: Suche schließen vor Navigation.
    Ziel: Back schließt IMMER zuerst UI-Ebenen; App beendet nur bei leerem Backstack.
D2. Sicherstellen, dass BackHandler NICHT doppelt/konkurrierend registriert sind
    (Compose BackHandler-Priority). Test: Back im Terminal mit offenem Drawer →
    Drawer schließt; Back auf Editor-Hauptansicht → zurück zu ProjectList.

══════════════════════════════════════════════════════════════════
PHASE E – VALIDIERUNG & REPORT
══════════════════════════════════════════════════════════════════
E1. ./gradlew assembleDebug → BUILD SUCCESSFUL.
E2. On-Device-Suite:
    a) Settings: Distro-Liste = NUR "Ubuntu" + "Alpine" (kein Debian),
    b) Distro "Alpine" → neue Session startet mit MOBILEIDE_DISTRO=alpine,
    c) 3 Sessions → Drawer zeigt 1/2/3 mit unterscheidbaren Titeln,
    d) Back-Kette in allen 4 Kontexten (Editor, Terminal+Drawer, Panel, ProjectList),
    e) close_last_session_behavior: NEW_SESSION → neue Session, EXIT_APP → beendet,
    f) Regression: LSP-Installer (initCommand) + MOBILEIDE_PROJECT_DIR-Auflösung.
E3. Report terminal-settings-back-fix-report.md um die Abschnitte Git-Nachweis (A2),
    Drawer-Diff (B), Session-Settings-Diff (C), Back-Kette-Erweiterung (D) ergänzen;
    Screenshot-Beschreibung vorher/nachher (Distro-Liste, Drawer 3 Sessions, Back).
E4. Commits:
    "fix(terminal-drawer): unique session titles with index+distro, ensure
    sessionOrder updates on create/terminate"
    "fix(navigation): extend layered BackHandler to terminal/panel/project contexts"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase B/C/D: Falls eine Einstellung/das Drawer-Verhalten bereits korrekt ist,
  KEIN künstlicher Fix – als "bereits korrekt, belegt durch <datei>:<zeile>"
  dokumentieren.
- Falls Session-Settings beim Start NICHT gelesen werden → ehrlich als ungelösten
  Bug ausweisen (nicht als "verified" umschreiben).
- KEIN erfundener Commit-Hash: Falls nichts committet wurde, wird der Fix committet
  (A1c) – der Hash entsteht aus dem tatsächlichen Commit.
