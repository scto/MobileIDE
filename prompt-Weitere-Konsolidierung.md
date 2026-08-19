Konsolidiere die Terminal-/Settings-/Asset-Struktur von MobileIDE (com.scto.mobile.ide)
so, dass Doppeldeutigkeiten und parallele Wahrheiten endgültig verschwinden.

ZIEL:
- EINE eindeutige Settings-Quelle für Terminal-bezogene Einstellungen
- EINE eindeutige Owner-Struktur für Terminal-Assets
- EINE maschinenprüfbare Reconciliation gegen künftige Duplikate
- KEINE funktionale Regression

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND (aus den Attachments)
══════════════════════════════════════════════════════════════════
- `terminal-settings-back-fix-report.md`:
  - Drawer-Fix belegt: `TerminalScreen.kt:607`
  - SessionOrder wird in `TerminalService.kt:180-210` synchron gehalten
  - Git-Nachweis vorhanden
  - BackHandler-Ketten sind getrennt
- `terminal-desktop-mode-report.md`:
  - Desktop Mode existiert
  - `LayoutMode.DESKTOP` ist eingeführt
  - `TerminalDesktopSplit.kt` existiert
- `module-consolidation-report.md`:
  - `TerminalEnvironmentSelector.kt` wurde als Duplikat entfernt
  - Distro-Quelle vereinheitlicht via `TerminalEnvironmentOption.entries`
  - Terminal-Assets sind bereits auf `features:terminal/src/main/assets/terminal` konsolidiert
  - `scripts/reconcile_modules.sh` existiert
- `reconciliation-code-truth.tsv`:
  - dient als technische Wahrheitstabelle
- Offene Beobachtung aus den Reports:
  - Die Verwirrung zwischen `app/.../ui/settings/SettingsScreen.kt`
    und früheren Terminal-Settings-Wrappers soll vollständig eliminiert werden
  - Es darf keine zweiten Asset-Pfade für terminal-bezogene Ressourcen geben

══════════════════════════════════════════════════════════════════
PHASE A – INVENTAR UND RESTDUPLIKATE FINDEN
══════════════════════════════════════════════════════════════════
A1. Scanne das Projekt nach allen terminal-/settings-/asset-bezogenen Duplikaten:
    - Settings-Screens, Wrapper, Routes, Preferences
    - Distro-Listen / TerminalEnvironmentOption-Verwendungen
    - Asset-Pfade für terminal/, colorschemes/, lsp-scripts/, setup-assets/
    - doppelte Utilities oder parallele Konstanten
A2. Nutze `reconciliation-code-truth.tsv` als Basis und ergänze:
    - Datei
    - Symbol
    - Modul
    - aktueller Owner
    - empfohlener Owner
    - Konfliktstatus
A3. Schreibe den Befund in:
    - `build/consolidation-inventory.tsv`
    - `build/consolidation-befund.md`

══════════════════════════════════════════════════════════════════
PHASE B – SINGLE SOURCE OF TRUTH DURCHSETZEN
══════════════════════════════════════════════════════════════════
B1. Settings:
    - Eine zentrale Terminal-Settings-Komponente bleibt die Wahrheit
    - Alle Wrapper/Redirects auf die zentrale Komponente umbiegen oder entfernen
    - Keine zweite Terminal-Settings-UI mehr
B2. Distro:
    - Nur eine Distro-Quelle behalten
    - Alle String-Duplikate durch zentrale Enum/Quelle ersetzen
B3. Assets:
    - Terminal-Assets nur an einem einzigen Ort halten
    - Alle Referenzen auf diesen Owner-Pfad umbiegen
    - Alte Pfade entfernen
B4. Prüfe besonders:
    - SettingsScreen vs. TerminalSettingsScreen
    - TerminalEnvironmentSelector-Reste
    - assets/terminal-Duplikate
    - LSP-/colorscheme-/setup-Assets

══════════════════════════════════════════════════════════════════
PHASE C – RECONCILIATION-SKRIPT ABSCHLIESSEN
══════════════════════════════════════════════════════════════════
C1. Erweitere oder finalisiere `scripts/reconcile_modules.sh`, sodass es prüft:
    - nur eine Settings-Quelle für Terminal
    - nur eine Distro-Quelle
    - nur ein Terminal-Asset-Owner
    - keine alten Pfade in Kotlin-Referenzen
C2. Skript muss:
    - Termux-kompatibel sein
    - Exit-Code 0/1 liefern
    - klare Fundstellen ausgeben
C3. Falls sinnvoll, ergänze einen `--fix`-Modus nur für eindeutig sichere Umzüge

══════════════════════════════════════════════════════════════════
PHASE D – VALIDIERUNG
══════════════════════════════════════════════════════════════════
D1. `./gradlew assembleDebug`
D2. `./gradlew test`
D3. `scripts/reconcile_modules.sh`
D4. On-device Smoke:
    - Settings öffnen
    - Terminal-Assets/Farbschemata laden
    - Distro-Auswahl testen
    - Desktop Mode nicht beschädigen
D5. Report:
    - `build/module-consolidation-report.md`
    - vor/nach Tabelle
    - konkrete Files
    - offene Punkte
D6. Commit:
    - `refactor(modules): consolidate terminal settings and assets into single source of truth`

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Keine funktionale Änderung ohne klaren Grund
- Keine zweite Wahrheit für Settings, Distro oder Assets
- Keine Datei löschen, bevor alle Referenzen bekannt sind
- Desktop Mode darf durch die Konsolidierung nicht regressieren
