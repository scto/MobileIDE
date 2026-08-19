Sichere den bereits vorhandenen Desktop Mode für Terminal-Sessions in MobileIDE
(com.scto.mobile.ide) final ab, damit er auf echten Geräten stabil, konsistent und
ohne Layout-Regressions funktioniert.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND (aus dem Report-Set)
══════════════════════════════════════════════════════════════════
- Desktop Mode ist implementiert:
  - `LayoutMode.DESKTOP`
  - `TerminalDesktopSplit.kt`
  - Erweiterung in `TerminalScreen.kt`
- Session-Verwaltung basiert auf SSOT:
  - `SessionService`
  - `sessionOrder`
  - `currentSession`
- Drawer-/Session-Fix ist belegt:
  - `TerminalScreen.kt:607`
  - `sessionOrder` sauber synchronisiert
- Smartphone-Modus darf unverändert bleiben
- Terminal-Settings sind bereits aufgeräumt
- Modul-/Asset-Konsolidierung ist teilweise erfolgt

══════════════════════════════════════════════════════════════════
PHASE A – DESKTOP MODE REAL-WORLD VALIDIERUNG
══════════════════════════════════════════════════════════════════
A1. Prüfe die tatsächliche Desktop-Mode-Implementierung:
    - wie werden mehrere Sessions in Panes aufgeteilt?
    - wie wird die Breite verwaltet?
    - gibt es einen Splitter / Resize-Handler?
    - wie wird aktive Session markiert?
A2. Prüfe die Interaktion mit:
    - Drawer
    - BackHandler
    - Session-Close-Verhalten
    - Smartphone-Modus
A3. Dokumentiere den Ist-Stand in:
    - `build/desktop-mode-inventory.md`

══════════════════════════════════════════════════════════════════
PHASE B – DESKTOP MODE STABILISIEREN
══════════════════════════════════════════════════════════════════
B1. Stelle sicher, dass 2 Sessions wirklich nebeneinander erscheinen
B2. Stelle sicher, dass 3+ Sessions korrekt und nachvollziehbar dargestellt werden
B3. Stelle sicher, dass der Splitter auf großen Geräten sinnvoll nutzbar ist
B4. Stelle sicher, dass aktive Session und Fokus klar erkennbar sind
B5. Stelle sicher, dass das Schließen einer Pane die übrigen Panes korrekt neu ordnet
B6. Stelle sicher, dass Drawer und Desktop-Mode zusammen funktionieren
B7. Stelle sicher, dass Back im Desktop-Mode nicht die App unbeabsichtigt beendet

══════════════════════════════════════════════════════════════════
PHASE C – SETTINGS UND UX ABSICHERN
══════════════════════════════════════════════════════════════════
C1. Prüfe, ob der Modus über Settings steuerbar ist
C2. Prüfe, ob der Modus nach App-Neustart korrekt erhalten bleibt
C3. Prüfe, ob Smartphone-Modus identisch wie vorher funktioniert
C4. Prüfe, ob der Moduswechsel ohne Sessionverlust funktioniert

══════════════════════════════════════════════════════════════════
PHASE D – VALIDIERUNG & BERICHT
══════════════════════════════════════════════════════════════════
D1. `./gradlew assembleDebug`
D2. On-device Test:
    - 2 Sessions öffnen → nebeneinander
    - 3 Sessions öffnen → alle sichtbar
    - Splitter bewegen
    - Session schließen
    - Modus wechseln
    - Back-Verhalten prüfen
D3. Report:
    - `build/terminal-desktop-mode-report.md`
    - vorher/nachher
    - Screenshots
    - offene Punkte
D4. Commit:
    - `feat(terminal): harden desktop mode with verified split-pane behavior`

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Smartphone-Modus darf nicht regressieren
- Keine zweite Session-Registry einführen
- Kein Desktop-Mode ohne echte Pane-Interaktion
- Falls Splitter nicht stabil ist: offen dokumentieren, nicht kaschieren
