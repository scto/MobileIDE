Räume die bestehende Log-, LSP- und Tooling-Struktur in MobileIDE (com.scto.mobile.ide)
weiter auf, ohne die bereits verifizierten Pfad-, Settings- und Desktop-Mode-Fixes
zu gefährden.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND
══════════════════════════════════════════════════════════════════
- Log-Tab-Umbau ist umgesetzt:
  - `LogRouter.classify()` ist verdrahtet
  - `ToolingBottomSheet` zeigt die Kerntabs
- LSP-/Plugin-Reconciliation-Tabellen existieren:
  - `plugin-package-mapping.tsv`
  - `kotlin-lsp-consistency.tsv`
  - `plugin-consolidation-inventory.tsv`
  - `lsp-script-inventory.tsv`
- Tooling-/Log-Struktur existiert bereits
- Es gab historisch mehrere parallele Stellen für Routing, Log-Capture und Tooling UI

══════════════════════════════════════════════════════════════════
PHASE A – IST-ZUSTAND AUFLISTEN
══════════════════════════════════════════════════════════════════
A1. Inventar aller aktuellen Log-/LSP-/Tooling-Einstiegspunkte
A2. Markiere:
    - bereits korrekt
    - teilweise redundant
    - potenziell veraltet
A3. Schreibe den Befund in:
    - `build/tooling-reconciliation-inventory.tsv`

══════════════════════════════════════════════════════════════════
PHASE B – AUFRÄUMEN
══════════════════════════════════════════════════════════════════
B1. Entferne oder vereinheitliche doppelte Routing- oder Statuspfade
B2. Stelle sicher, dass LSP-Status, Build-Logs, IDE-Logs und Install-Logs
    keine parallelen alternativen Wahrheiten haben
B3. Falls nötig, konsolidiere Hilfsklassen oder Manager-Objekte
B4. Achte darauf, dass LogCatcher, ToolingLogManager und LogRouter
    sauber getrennte Verantwortlichkeiten behalten

══════════════════════════════════════════════════════════════════
PHASE C – VALIDIERUNG
══════════════════════════════════════════════════════════════════
C1. `./gradlew assembleDebug`
C2. Relevante Unit-Tests
C3. On-device Smoke:
    - Build-Log erscheint im richtigen Tab
    - LSP-Fehler im LSP-Bereich
    - IDE-Logs in IDE-Logs
    - Install-Logs im INSTALL-Bereich
C4. Report:
    - `build/tooling-reconciliation-report.md`
D5. Commit:
    - `refactor(tooling): simplify log and LSP routing structure`

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Keine Regression in Build-, Install- oder LSP-Logging
- Keine neue doppelte Quelle
- LogCatcher bleibt als technische Log-Quelle erhalten
- Tooling UI darf nicht funktional schlechter werden
