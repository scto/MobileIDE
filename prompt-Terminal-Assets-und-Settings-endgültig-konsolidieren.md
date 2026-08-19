Konsolidiere in MobileIDE (com.scto.mobile.ide) die Terminal-Settings- und
Terminal-Asset-Struktur ENDGÜLTIG, damit Verwirrungen wie
`app/ui/settings/SettingsScreen.kt` vs. `:features:terminal/TerminalSettingsScreen.kt`
nicht mehr auftreten und alle Terminal-Assets nur noch an genau EINEM Ort liegen.

Ziel:
- EINE klare Source of Truth für Terminal-Settings
- EINE klare Source of Truth für Terminal-Assets
- KEINE doppelten oder widersprüchlichen Distro-Quellen
- KEINE parallelen Asset-Pfade für Terminal, LSP, Colorschemes, Setup-Skripte
- Danach ein Reconciliation-Skript, das diese Regeln dauerhaft prüft

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND
══════════════════════════════════════════════════════════════════
Aus den Reports und TSVs ist bereits belegt:

- `terminal-settings-back-fix-report.md`
  - Git-Commit `304d7abc`
  - Drawer-Fix mit `TerminalScreen.kt:607`
  - BackHandler-Ketten für Editor / Terminal / Panel / ProjectList
  - Settings-Pfade:
    - `terminal_font_size`
    - `scrollback_lines`
    - `terminal_colorscheme`
    - `terminal_close_behavior`
    - `selected_distro`

- `terminal-desktop-mode-report.md`
  - Git-Commit `4eeab1a9`
  - `LayoutMode.DESKTOP`
  - `TerminalDesktopSplit.kt`
  - `Settings.kt` enthält Desktop-Mode

- `module-consolidation-report.md`
  - Git-Commit `bdf76e1d`
  - `TerminalEnvironmentSelector.kt` nur noch in `features:terminal`
  - Distro-Quelle zentral über `TerminalEnvironmentOption.entries`
  - Terminal-Assets nur noch in `features:terminal/src/main/assets/terminal`
  - Reconciliation-Skript `scripts/reconcile_modules.sh`

- `module-consolidation-inventory.tsv`
- `reconciliation-code-truth.tsv`

Wichtig:
- Der vorherige Zustand war tatsächlich verwirrend.
- Die Konsolidierung ist bereits teilweise erfolgt, soll jetzt aber vollständig und dauerhaft abgesichert werden.

══════════════════════════════════════════════════════════════════
PHASE A – INVENTAR DER WAHREN QUELLEN
══════════════════════════════════════════════════════════════════
A1. Finde alle Terminal-Settings-bezogenen UIs und Komponenten:
    - `app/src/main/java/.../ui/settings/SettingsScreen.kt`
    - `features/terminal/.../TerminalSettingsScreen.kt`
    - weitere Settings-Wrapper / Routes / Screens
    Ziel: exakt feststellen, welche Komponente aktiv ist und welche nur Wrapper sind.

A2. Finde alle Distro-Quellen:
    - `listOf("ubuntu", "alpine")`
    - `TerminalEnvironmentOption.entries`
    - `DistroManager`
    - `TerminalEnvironmentSelector`
    Ziel: eine einzige Quelle für Distro-Werte.

A3. Finde alle Terminal-Assets:
    - `*/src/main/assets/terminal/**`
    - colorschemes
    - LSP-Skripte
    - setup/init/rootfs-bezogene Dateien
    Ziel: exakt einen Asset-Halter bestimmen.

A4. Erstelle/aktualisiere:
    - `build/consolidation-source-inventory.tsv`
    mit Spalten:
      `bereich | datei | status | source-of-truth? | kommentar`

══════════════════════════════════════════════════════════════════
PHASE B – KONSOLIDIERUNG DER SETTINGS
══════════════════════════════════════════════════════════════════
B1. Entscheide und dokumentiere EINE zentrale Settings-UI:
    - Bevorzugt die bereits aktive `app/.../ui/settings/SettingsScreen.kt`
    - Terminal-bezogene Routen/Wrapper aus `features:terminal` sollen nur noch
      auf diese zentrale UI zeigen oder entfernt werden.

B2. Entferne oder entkopple doppelte Settings-Screens:
    - keine zweite terminal-spezifische Settings-UI als aktive Wahrheit
    - falls ein Wrapper für Routing nötig ist, dann nur als Delegation ohne eigene Logik

B3. Vereinheitliche die Distro-Werte:
    - überall `TerminalEnvironmentOption.entries`
    - keine separaten String-Listen mehr
    - keine harten `"ubuntu"`, `"alpine"`-Listen in mehreren Dateien

B4. Stelle sicher, dass die Settings beim Session-Start gelesen werden:
    - `selected_distro`
    - `terminal_font_size`
    - `scrollback_lines`
    - `terminal_colorscheme`
    - `terminal_close_behavior`

B5. Wenn ältere Fallbacks existieren:
    - `debian` nur als Migrationswert behandeln
    - danach nicht mehr als aktive Auswahl anzeigen

══════════════════════════════════════════════════════════════════
PHASE C – TERMINAL-ASSETS AUF EINEN HALTER REDUZIEREN
══════════════════════════════════════════════════════════════════
C1. Entscheide EINEN einzigen Asset-Halter:
    - bevorzugt der bereits verifizierte Halter aus `module-consolidation-report.md`
    - alle Terminal-Assets nur dort belassen

C2. Assets, die konsolidiert werden müssen:
    - `colorschemes/`
    - LSP-Skripte
    - Setup-Skripte
    - Rootfs-/Hilfsdateien
    - evtl. zusätzliche Terminal-Assets aus anderen Modulen

C3. Alle Referenzen auf den neuen Pfad umstellen:
    - ColorSchemeManager
    - ScriptedLspServer
    - DistroManager
    - SetupWorker
    - init.sh / setup.sh / andere Launcher
    - alle weiteren Code-Stellen aus den TSV-Inventaren

C4. Alte Asset-Pfade entfernen, wenn keine Referenzen mehr existieren.

C5. Keine funktionalen Änderungen am Verhalten:
    - nur Struktur konsolidieren
    - keine Logik für Terminal, LSP, Colorscheme oder Setup verändern

══════════════════════════════════════════════════════════════════
PHASE D – RECONCILIATION-SKRIPT ERWEITERN
══════════════════════════════════════════════════════════════════
D1. Aktualisiere `scripts/reconcile_modules.sh`, sodass es zusätzlich prüft:
    - nur eine Settings-UI mit Terminal-Bezug
    - nur eine Distro-Quelle
    - nur ein Terminal-Asset-Root
    - keine Referenzen auf alte Asset-Pfade

D2. Skript soll Exit 0 liefern, wenn alles konsistent ist.

D3. Ausgabe soll als TSV oder klar strukturierte Zeilen erfolgen:
    - Datei
    - Fundstelle
    - Regelverletzung
    - empfohlene Aktion

D4. Wenn ein `--fix`-Modus existiert oder ergänzt wird:
    - nur eindeutig automatisierbare Umzüge erlauben
    - niemals Dateien unkontrolliert löschen
    - bei Unsicherheit abbrechen

══════════════════════════════════════════════════════════════════
PHASE E – VALIDIERUNG
══════════════════════════════════════════════════════════════════
E1. `./gradlew assembleDebug` → BUILD SUCCESSFUL

E2. `./gradlew test` bzw. relevante Modul-Tests → grün

E3. `bash scripts/reconcile_modules.sh` → Exit 0

E4. On-Device-Smoke-Test:
    - Terminal-Settings öffnen
    - Distro-Auswahl prüfen
    - Farben/Colorscheme wechseln
    - LSP-Installer starten
    - prüfen, dass alle Assets und Settings korrekt funktionieren

E5. Report:
    `build/module-settings-assets-consolidation-report.md`
    mit:
    - Inventar
    - Entscheidungen
    - umgesetzte Änderungen
    - Tests
    - offene Punkte

E6. Commit:
    `refactor(modules): consolidate terminal settings and assets into single source of truth`

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN
══════════════════════════════════════════════════════════════════
- Keine parallelen Settings-Wahrheiten behalten.
- Keine parallelen Terminal-Asset-Halter behalten.
- Wenn eine Referenz auf einen alten Asset-Pfad nicht eindeutig gefunden wird,
  nicht raten, sondern als offenen Punkt dokumentieren.
- Keine funktionalen Nebenänderungen.
