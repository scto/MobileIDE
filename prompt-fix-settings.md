Behebe VIER zusammenhängende Fehler im Terminal- & Navigations-Bereich von MobileIDE
(com.scto.mobile.ide):

  1. TERMINAL-SETTINGS DISTRO-AUSWAHL FALSCH: "Linux Distribution" zeigt "Ubuntu" und
     "Debian". Es muss "Ubuntu" und "Alpine" sein (Debian existiert NICHT als Distro;
     einzige Rootfs sind ubuntu und alpine – belegt durch check_distro_rootfs.sh,
     das local/<distro>/{home,etc,usr,bin} prüft, und durch status.md 2026-06-13,
     das nur Alpine- und Ubuntu-Rootfs kennt).
  2. TERMINAL IGNORIERT TERMINAL-SETTINGS SESSION-EINSTELLUNG: Die gewählte
     Konfiguration (Distro, "Verhalten beim Schließen der letzten Sitzung"
     [new_session|exit_app], Schriftgröße, Scrollback Lines, Farbschema,
     LSP-Bash-Skripte) wird beim Start einer Session NICHT (vollständig) angewendet.
  3. ZURÜCK-WISCHEN SCHLIESST DIE APP: Die Android-Back-Geste (Predictive Back)
     beendet die App direkt, statt zuerst UI-Ebenen zu schließen
     (BottomSheet → Terminal → Editor → Navigation → App).
  4. TERMINAL-DRAWER ZEIGT NUR 1 SITZUNG: Der Terminal-Drawer (Sitzungsliste)
     zeigt IMMER NUR EINE Terminal-Sitzung an, selbst wenn eine zweite oder dritte
     Sitzung geöffnet wurde. Die Sitzungsliste muss alle aktiven Sessions anzeigen
     (mit Titel/Distro/Index), Umschalten zwischen ihnen muss möglich sein, und beim
     Schließen einer Session darf die Liste korrekt aktualisiert werden.

══════════════════════════════════════════════════════════════════
KONTEXT / VERIFIZIERTER IST-ZUSTAND (aus Projekt-Dumps)
══════════════════════════════════════════════════════════════════
- Terminals & Settings-UI liegen im Modul :features:terminal
  (features/terminal/src/main/...), Settings-Route: "settings/terminal"
  (TerminalRoutes.kt, TerminalSettingsScreen.kt).
- DistroManager.kt (in :features:terminal) baut den PRoot-Befehl
  (buildProotCommand) und setzt/übergibt MOBILEIDE_DISTRO + initCommand
  (PROGRESS.md 2026-07-04).
- TerminalBackEnd.kt enthält den decoupled Session-Close-Callback
  (PROGRESS.md 2026-07-01: "decoupled session termination callbacks from the core
  terminal backend view-client to isolate :core:main dependencies from :app").
- Session-Erzeugung: PROGRESS.md 2026-07-03 – init.sh/setup.sh lösen den
  Projektpfad via MOBILEIDE_PROJECT_DIR auf; "Reset Terminal" schließt ALLE
  aktiven Shell-Sessions und erzeugt eine frische (status.md 2026-06-13).
- Sitzungs-Tabs/UI: prompt-script-fix.md dokumentiert eine Tab-Leiste
  "Termix | alpine | +" – die App unterstützt also mehrere Sessions, aber der
  DRAWER (Sitzungsliste) zeigt laut Fehlerbericht trotzdem nur 1 Sitzung.
- Terminal-Settings (verifiziert):
  * "Linux Distribution": Screenshot zeigt "Ubuntu [ausgewählt]" + "Debian" –
    MUSS zu ["Ubuntu", "Alpine"] korrigiert werden.
  * "Verhalten beim Schließen der letzten Sitzung": Default "new_session"
    (PROGRESS.md 2026-07-01), Preference-Schlüssel prüfen.
  * terminal_colorscheme (5 Themes: default, dracula, solarized_dark, nord,
    monokai; Assets features/terminal/src/main/assets/terminal/colorschemes/).
  * terminal_font_size (Slider 10-30sp, scaledDensity-Umrechnung),
    Scrollback Lines (aktuell 50000), settings_lsp_bash_scripts (Toggle).
- Back-Gesture/Navigation: Jetpack Compose Navigation; NavigationUtils.safeNavigate
  (app/src/main/java/com/scto/mobile/ide/core/common/utils/NavigationUtils.kt);
  CodeEditScreen.kt + ProjectListScreen.kt importieren safeNavigate. Kein zentraler
  BackHandler, der BottomSheet → Terminal → Editor → Navigation → App durchläuft.

══════════════════════════════════════════════════════════════════
PHASE A – DIAGNOSE (keine Änderung, nur Befund)
══════════════════════════════════════════════════════════════════
A1. DISTRO-AUSWAHL: Lokalisiere die Distro-Liste in TerminalSettingsScreen.kt
    (hartkodiert listOf("Ubuntu","Debian")? Enum/sealed class? Persistenz-
    Schlüssel?). Prüfe, ob die Auswahl als lowercase-Distro-ID ("ubuntu"/"alpine")
    gespeichert und beim Lesen auf gültige IDs normalisiert wird.
A2. SESSION-ERZEUGUNG: Lokalisiere den Session-Erzeugungspfad
    (TerminalScreen/TerminalBackEnd/DistroManager/ViewModel):
    a) Wo wird beim Start die Distro ermittelt (UI-Preference oder hartkodiert)?
    b) Wo wird MOBILEIDE_DISTRO gesetzt und an init-host/setup.sh übergeben?
    c) Welche Session-Settings (Font, Scrollback, Farbschema, close-behavior)
       werden gelesen/angewendet, welche NICHT?
A3. SESSION-LISTE / DRAWER (NEU – Bug 4):
    a) Finde den TerminalDrawer bzw. die Sitzungslisten-UI im :features:terminal
       (Composable/StateHolder/SessionAdapter). Prüfe, WELCHE Datenquelle die
       Liste speist: eine ObservableList/SnapshotStateList/Flow von Sessions oder
       nur einen einzelnen "currentSession"-State.
    b) Prüfe, ob beim Erzeugen einer 2./3. Session (Plus-Button " + ") die neue
       Session tatsächlich in die Liste eingefügt wird ODER ob die Liste nur den
       zuletzt aktiven Session-State rendert (häufigste Ursache: Drawer bindet an
       `activeSession` statt an `sessions`).
    c) Prüfe, ob Session-Erzeugung und Drawer auf DENSELBEN State-Holder zeigen
       (gleiche Instanz!) oder ob es zwei getrennte Session-Registrys gibt
       (z. B. TerminalService-Halter vs. UI-ViewModel) – Instance-Mismatch ist
       die zweithäufigste Ursache.
    d) Prüfe das Schließen: Wird eine geschlossene Session aus der Liste entfernt
       (Listener/Callback aus TerminalBackEnd.kt angebunden)?
A4. BACK-HANDLING: Gibt es BackHandler/OnBackPressedDispatcher in
    ToolingBottomSheet, CodeEditScreen, TerminalScreen, ProjectListScreen?
    Ist enableOnBackInvokedCallback im Manifest gesetzt?
A5. Erstelle build/terminal-settings-diagnose.md mit: Distro-Quelle, Session-
    Erzeugungspfad, Drawer-Datenquellen-Inventar (a-d aus A3), angewendete vs.
    ignorierte Settings, Back-Handler-Inventar.

══════════════════════════════════════════════════════════════════
PHASE B – DISTRO-AUSWAHL AUF UBUNTU/ALPINE KORRIGIEREN
══════════════════════════════════════════════════════════════════
B1. Ersetze "Debian" durch "Alpine" in der Distro-Auswahl
    (["Ubuntu", "Alpine"]). Persistierte Alt-Werte ("debian", "Debian", "Ubuntu")
    beim Lesen auf gültige IDs ("ubuntu"/"alpine") normalisieren.
B2. Zentrale Distro-Quelle einführen (z. B. Distro-Enum in :features:terminal:
    UBUNTU("ubuntu","Ubuntu"), ALPINE("alpine","Alpine")) und in UI UND
    DistroManager nutzen – keine zweite String-Liste mehr.
B3. Report: alle Debian-Referenzen in UI/Strings/Descriptions der App auflisten
    und entfernen/ersetzen (Docs dürfen historisch bleiben).

══════════════════════════════════════════════════════════════════
PHASE C – TERMINAL ÜBERNIMMT SESSION-EINSTELLUNGEN BEIM START
══════════════════════════════════════════════════════════════════
C1. Beim Anlegen EINER NEUEN SESSION MÜSSEN gelesen + angewendet werden:
    a) Distro → MOBILEIDE_DISTRO; Rootfs-Integritätscheck VOR dem Start
       (local/<distro>/home existiert; sonst Setup/Extraktion anstoßen,
       NICHT mit kaputter Rootfs starten – behebt chdir("/home")/127-Bug),
    b) terminal_font_size → scaledDensity → TerminalView.setTextSize,
    c) Scrollback Lines → Emulator-Konfiguration,
    d) terminal_colorscheme → TerminalColors.COLOR_SCHEME.updateWith(props),
    e) settings_lsp_bash_scripts (wirkt auf LSP-Bash-Skript-Ausführung).
C2. Close-Verhalten "letzte Sitzung" exakt befolgen:
    - new_session → neue Terminal-Session öffnen (Default),
    - exit_app → App beenden NUR wenn explizit so konfiguriert.
C3. Regression: initCommand-Forwarding (LSP-Installer, 2026-07-04),
    MOBILEIDE_PROJECT_DIR-Auflösung (2026-07-03), PRoot-stat/vmstat-Sanitize
    dürfen nicht brechen.

══════════════════════════════════════════════════════════════════
PHASE D – ZURÜCK-WISCHEN: APP NICHT DIREKT SCHLIESSEN
══════════════════════════════════════════════════════════════════
D1. Zentrale Back-Handler-Kette (Compose BackHandler/OnBackPressedDispatcher):
    1. ToolingBottomSheet geöffnet? → schließen,
    2. Software-Keyboard offen? → schließen,
    3. Terminal-Screen/Session-Vorschau offen? → Session-Close-Setting anwenden
       (C2), NICHT sofort App beenden,
    4. CodeEditScreen im Back-Stack? → eine Navigationsebene zurück,
    5. ProjectListScreen/Settings? → zurück,
    6. Erst bei leerem Back-Stack → finish().
D2. In MainActivity registrieren, auch für Predictive Back (Android 13+,
    enableOnBackInvokedCallback). KEINE doppelten Handler,
    NavigationUtils.safeNavigate weiterverwenden.

══════════════════════════════════════════════════════════════════
PHASE E – TERMINAL-DRAWER: ALLE SITZUNGEN ANZEIGEN (NEU – Bug 4)
══════════════════════════════════════════════════════════════════
E1. Datenquelle konsolidieren: Der Drawer MUSS an eine beobachtbare, zentrale
    Sitzungsliste gebunden sein (SnapshotStateList<Session>/StateFlow<List<Session>>)
    – NICHT an einen einzelnen activeSession-State. Befund aus A3a umsetzen.
E2. Falls zwei getrennte Registrys existieren (Service-Halter vs. UI-ViewModel):
    auf EINE gemeinsame SessionRegistry zusammenführen ODER einen gemeinsamen
    Flow/StateLink zwischen TerminalService und Drawer legen (A3c). Der Drawer
    zeigt danach exakt die Sessions, die der TerminalService verwaltet.
E3. Verhalten der Sitzungsliste:
    a) " + " erzeugt eine neue Session → erscheint SOFORT als Eintrag im Drawer,
    b) Klick auf einen Eintrag → wechselt zu dieser Session (aktive Session wird
       markiert, Sitzungs-Ansicht wechselt),
    c) Schließen einer Session (X-Button/Swipe) → Eintrag wird entfernt,
       letzte Session-Verhalten aus C2 greift,
    d) Titel/Anzeige je Eintrag: Session-Index + Distro (z. B. "1 · ubuntu",
       "2 · ubuntu", "3 · alpine") – eindeutig unterscheidbar,
    e) Leerer Zustand: keine Sessions → Drawer zeigt Hinweis/Option "Neue
       Sitzung" statt kaputten Zustand.
E4. Session-Titel: Wenn möglich den vom init.sh gesetzten Projektnamen/cwd als
    Untertitel anzeigen (aus MOBILEIDE_PROJECT_DIR bzw. Session-Arbeitsverzeichnis)
    – mindestens Index + Distro, optional Projektname.
E5. TerminalBackEnd.kt: Der decoupled Close-Callback MUSS die Drawer-Liste
    benachrichtigen (entfernen) und der Session-Erzeuger MUSS die Liste
    benachrichtigen (einfügen + aktivieren). Prüfe, dass der Callback nicht nur
    den Prozess beendet, sondern auch den UI-State aktualisiert.

══════════════════════════════════════════════════════════════════
PHASE F – VALIDIERUNG & BERICHT
══════════════════════════════════════════════════════════════════
F1. ./gradlew assembleDebug → BUILD SUCCESSFUL.
F2. Manuelle Tests auf dem Gerät:
    a) Terminal-Einstellungen: Distro-Auswahl zeigt NUR "Ubuntu" und "Alpine".
    b) Distro auf "Alpine" → neue Session startet mit MOBILEIDE_DISTRO=alpine;
       Rootfs-Check greift bei unvollständiger local/alpine.
    c) Schriftgröße/Farbschema/Scrollback ändern → neue Session übernimmt sie.
    d) "Verhalten beim Schließen der letzten Sitzung": new_session → neue
       Sitzung; exit_app (nur wenn so gesetzt) → beendet.
    e) DRAWER (Bug 4): Öffne 3 Sitzungen ("1 · ubuntu", "2 · ubuntu",
       "3 · alpine"). Alle 3 müssen im Drawer erscheinen. Wechsel per Klick
       funktioniert. Schließe eine → Liste aktualisiert sich, aktive Session
       wechselt korrekt. Regression: 1 Sitzung → Schließen verhält sich gemäß C2.
    f) Back-Geste: BottomSheet schließt zuerst; App beendet erst bei leerem
       Back-Stack.
    g) Regression: LSP-Installer via initCommand, Projektordner via
       MOBILEIDE_PROJECT_DIR.
F3. Erstelle build/terminal-settings-back-fix-report.md mit: Befund (Phase A),
    Tabelle der geänderten Dateien (Datei | Änderung | Test), Drawer-Befund
    (A3a–d + Lösung aus E), Screenshot-Beschreibung vorher/nachher, offene Punkte.
F4. Commits:
    "fix(terminal-settings): replace Debian with Alpine in distro selector,
    apply session settings on session start"
    "fix(terminal): show all sessions in terminal drawer, sync with session
    registry, handle close via decoupled callback"
    "fix(navigation): layered BackHandler chain, never finish app while UI layers
    are open"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase A MUSS zuerst abgeschlossen sein (inkl. Drawer-Befund A3a–d); ohne
  Befund keine Änderung.
- Falls die Drawer-Datenquelle nicht eindeutig auffindbar ist, NICHT raten –
  als offener Punkt im Report markieren und die SessionRegistry-Struktur im
  :features:terminal dokumentieren.
- Die CSS-LSP/Node.js-Meldung (Plugin-Store) wird in diesem Prompt NICHT
  behoben (nur als Folge-Thema notieren).
- Keine Änderung an den Rootfs-Installationen selbst (ubuntu/alpine bleiben
  unangetastet) außer dem UI-/Env-Integritätscheck aus C1a.
