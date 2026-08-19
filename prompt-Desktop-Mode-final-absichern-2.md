Sichere den bereits implementierten Terminal-Desktop-Mode in MobileIDE
(com.scto.mobile.ide) final ab, damit die Neben-einander-Ansicht stabil,
reaktiv und auf echten Geräten zuverlässig nutzbar ist.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND
══════════════════════════════════════════════════════════════════
Aus `terminal-desktop-mode-report.md` ist belegt:

- Commit `4eeab1a9`
- `LayoutMode.DESKTOP`
- `features/terminal/.../settings/Settings.kt`
- `TerminalDesktopSplit.kt`
- `TerminalScreen.kt` schaltet bei Desktop-Modus auf Split-Ansicht um
- SSOT läuft über `SessionService`
- Build erfolgreich

Terminal-Session-Fakten:
- `sessionOrder` als `SnapshotStateList<String>`
- `currentSession` als aktive Session
- Drawer und Desktop-Mode sollen dieselbe Session-Quelle verwenden

Ziel dieses Prompts:
- Desktop-Mode nicht neu erfinden
- sondern on-device absichern, splitten, testen und dokumentieren

══════════════════════════════════════════════════════════════════
PHASE A – IST-ZUSTAND PRÜFEN
══════════════════════════════════════════════════════════════════
A1. Verifiziere, ob der Desktop-Modus bereits vollständig auf `SessionService`
    basiert und keine zweite Session-Registry besitzt.

A2. Prüfe, ob `TerminalDesktopSplit.kt`:
    - alle offenen Sessions rendert
    - sessionOrder korrekt beobachtet
    - currentSession synchronisiert
    - Pane-Wechsel sauber behandelt

A3. Prüfe, ob die Settings-Option für Layout Mode
    - persistiert wird
    - beim Start korrekt geladen wird
    - Smartphone/Desktop wirklich umschaltet

A4. Erstelle ein kurzes Inventar:
    `build/desktop-mode-inventory.tsv`

══════════════════════════════════════════════════════════════════
PHASE B – STABILISIERUNG
══════════════════════════════════════════════════════════════════
B1. Stelle sicher, dass folgende Regeln gelten:
    - Smartphone-Modus bleibt unverändert
    - Desktop-Modus zeigt Sessions nebeneinander
    - keine Session verschwindet beim Split
    - Öffnen/Schließen rebalanciert die Ansicht
    - aktive Session bleibt markiert

B2. Prüfe Back-Verhalten:
    - Back im Desktop-Modus darf nicht die App sofort beenden
    - Back sollte Pane-/Session-Handling respektieren
    - Session-Close-Behavior bleibt mit den existierenden Terminal-Settings kompatibel

B3. Prüfe Rotation / Größenänderung:
    - Split-Layout bleibt stabil
    - Breitenverhältnisse gehen nicht verloren
    - keine ungewollten Overflows

B4. Wenn nötig, kleine Korrekturen nur dort vornehmen, wo der Desktop-Modus
    real instabil ist.

══════════════════════════════════════════════════════════════════
PHASE C – VALIDIERUNG
══════════════════════════════════════════════════════════════════
C1. `./gradlew assembleDebug` → BUILD SUCCESSFUL

C2. On-Device-Test:
    - 2 Sessions öffnen
    - Desktop-Modus aktivieren
    - beide Sessions nebeneinander sichtbar
    - eine Session aktivieren
    - eine Session schließen
    - Layout rebalanced korrekt
    - zurück in Smartphone-Modus wechseln
    - Sessions bleiben erhalten

C3. Wenn möglich:
    - 3 Sessions testen
    - Pane-Rotation bzw. Split-Sizes prüfen

C4. Report:
    `build/terminal-desktop-mode-absicherung-report.md`
    mit:
    - geprüften Stellen
    - eventuellen Korrekturen
    - On-Device-Ergebnis
    - Screenshots oder Screenshot-Beschreibung

C5. Commit:
    `fix(terminal): stabilize desktop mode side-by-side session panes`

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN
══════════════════════════════════════════════════════════════════
- Keine Änderung am Smartphone-Modus.
- Keine zweite Session-Registry einführen.
- Wenn der Desktop-Mode bereits korrekt ist, nur absichern und dokumentieren.
- Keine neue Terminal-Architektur hinzufügen.
