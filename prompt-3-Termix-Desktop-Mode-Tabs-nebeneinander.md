Ergänze in MobileIDE (com.scto.mobile.ide) die aus Termix (TermOne Plus) bekannte
FUNKTION "Desktop Mode" für das Terminal: mehrfache Terminal-Sessions werden
NEBENEINANDER (vertikale Split-Ansicht) statt im Smartphone-Modus (gestapelt bzw.
nur eine sichtbare Session) angezeigt.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND (aus terminal-settings-diagnose.md + Fix-Report + Dump)
══════════════════════════════════════════════════════════════════
- Session-Verwaltung: SessionService (Single Source of Truth) hält sessions
  (LinkedHashMap<String, TerminalSession>) + sessionOrder (SnapshotStateList<String>);
  currentSession als State; getDisplayTitle(sessionId) liefert Anzeigenamen.
- TerminalScreen.kt rendert aktuell EINE aktive Session (currentSession);
  Drawer (LazyColumn) listet alle Sessions; Wechsel per Klick ersetzt die Ansicht.
- TerminalBackEnd.kt: decoupled Session-Close-Callback; CloseLastSessionBehavior
  (EXIT_APP=0 / NEW_SESSION=1) via terminal_close_behavior.
- UI: ToolingBottomSheet mit Log-Tabs (INSTALL/BUILD/LSP/DIAGNOSE/IDE_LOGS + Panels);
  ResizablePanelLayout für Status-/Panel-Höhe; CodeEditScreen mit BackHandler-Kette.
- Compose-basiert (Jetpack Compose Navigation).

══════════════════════════════════════════════════════════════════
PHASE A – DESIGN (KEINE Änderung, nur Konzept festlegen)
══════════════════════════════════════════════════════════════════
A1. Modus-Definition:
    - Smartphone-Modus (Default): wie heute – eine Session sichtbar, Drawer/Tabs
      zum Wechseln.
    - Desktop-Modus: 2+ Sessions NEBENEINANDER in Split-Panes (Row/Box-Layout mit
      festen Split-Rändern; Drag-Handler zum Anpassen der Breitenverhältnisse,
      analog Android Studio/VS Code Split Editor bzw. Termix Desktop Mode).
A2. Umschaltung: Wo (Einstellung "Terminal Layout: Smartphone | Desktop" im
    Settings-Bereich) und wie (Toggle im Terminal-Kontextmenü)?
    Empfohlene Persistenz: SharedPreferences terminal_layout_mode (values:
    "smartphone" | "desktop"), gelesen beim TerminalScreen-Aufbau.
A3. Festlegen der Desktop-Regeln:
    a) 1 Session → volle Breite (kein Split nötig),
    b) 2 Sessions → 50:50 (verschiebbar 25–75%),
    c) 3+ Sessions → wie Termix Desktop Mode: alle offenen Sessions nebeneinander
       (pro Session ein Pane, Breite 100/n %, horizontal scrollbar falls nötig),
    d) aktive Session bleibt sichtbar markiert (Rand/Fokus), Klick ins Pane
       aktiviert die Session,
    e) Schließen einer Pane → restliche Panes füllen den Raum neu auf,
    f) Jede Pane zeigt Session-Titel (getDisplayTitle) als schmale Kopfzeile
       (Index + Distro + Projekt, konsistent mit Drawer-Format aus
       terminal-settings-back-fix Phase B).
A4. Interaktion:
    - Jede Pane unabhängig fokussierbar (Tastatur/Input routed zur aktiven Pane),
    - Drag-Handler zwischen Panes (Breite anpassen),
    - Context-Aktionen je Pane (Schließen, Titel, Duplizieren optional),
    - In jedem Pane funktioniert das Terminfo-/PRoot-Tooling (ubuntu/alpine)
      unverändert.

══════════════════════════════════════════════════════════════════
PHASE B – IMPLEMENTIERUNG
══════════════════════════════════════════════════════════════════
B1. TerminalScreen.kt: Layout umbauen auf Modus-abhängige Darstellung:
    - Smartphone: bestehendes Verhalten (currentSession),
    - Desktop: TerminalDesktopSplit (neues Composable), das sessionOrder
      (SnapshotStateList) als Panes rendert; reaktive Aktualisierung bei
      createSession/terminateSession (gleiche Quelle wie Drawer).
B2. Neues Composable TerminalDesktopSplit.kt (:features:terminal):
    - Row mit Panes, Splitter (draggable), Kopfzeile je Pane,
    - Breiten in Fraction-State (pro Pane), rebalance bei open/close,
    - aktive Session aus SessionService.currentSession synchronisiert.
B3. Settings: In SettingsScreen.kt Eintrag "Terminal Layout" (Smartphone/Desktop)
    ergänzen; Preference terminal_layout_mode mit Default "smartphone".
B4. BackHandler-Integration: Im Desktop-Modus gilt dieselbe Back-Kette wie im
    Smartphone-Modus (Drawer/Panel/Session-Close-Verhalten) – keine neue
    Sonderbehandlung, die die App beendet.
B5. Regression: Drawer bleibt in beiden Modi funktionsfähig (Wechsel zwischen
    Sessions per Drawer UND per Pane-Klick).

══════════════════════════════════════════════════════════════════
PHASE C – VALIDIERUNG & BERICHT
══════════════════════════════════════════════════════════════════
C1. ./gradlew assembleDebug → BUILD SUCCESSFUL.
C2. On-Device-Test (Android ≥ 8; großes Display bevorzugt):
    a) 2 Sessions öffnen → Desktop-Mode aktivieren → 2 Panes nebeneinander,
    b) Drag-Splitter → Breitenverhältnis ändert sich (25–75%),
    c) 3. Session öffnen → 3 Panes (33/33/33), horizontal scrollbar,
    d) Pane-Klick fokussiert Session; Input geht an aktive Pane,
    e) Pane schließen → verbleibende Panes rebalancen,
    f) Zurück zu Smartphone-Modus → Darstellung wie vorher, Session erhalten,
    g) Back-Geste in Desktop-Modus → Pane schließen (bzw. Session-Close-Verhalten),
       NICHT App beenden,
    h) Ubuntu- und Alpine-Sessions funktionieren in beiden Modi.
C3. Report build/terminal-desktop-mode-report.md: Design-Entscheidungen (A1-A4),
    geänderte Dateien (Datei | Änderung | Test), Screenshots vorher/nachher,
    offene Punkte.
C4. Commit:
    "feat(terminal): add desktop mode with side-by-side session panes and
    adjustable splitter, synced with session registry"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Desktop-Mode ist eine ZUSÄTZLICHE Ansicht – der Smartphone-Modus darf sich NICHT
  ändern (keine Regression).
- Pane-Verhalten baut auf SessionService als einzige Quelle auf – KEINE zweite
  Session-Registry einführen.
- Falls der Splitter/Resize auf dem Zielgerät nicht sauber funktioniert: als
  offener Punkt dokumentieren, NICHT durch feste Breiten "kaschieren".
