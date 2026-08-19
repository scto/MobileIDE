Räume die bestehende Log-, LSP- und Tooling-Struktur in MobileIDE
(com.scto.mobile.ide) weiter auf, ohne die bereits verifizierten Funktionen
zu beschädigen.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND
══════════════════════════════════════════════════════════════════
Aus den Reports ist belegt:

- Log-Architektur:
  - `LogRouter`
  - `ToolingLogManager`
  - `ToolingBottomSheet`
  - `log-recon-*`
  - `log-tabs-report.md`
- Log-Kategorien und Sender-Matrix sind bereits analysiert
- LSP- und Plugin-Reconciliation existieren:
  - `kotlin-lsp-consistency.tsv`
  - `lsp-script-inventory.tsv`
  - `plugin-package-mapping.tsv`
  - `plugin-consolidation-inventory.tsv`
  - `plugin-consolidation-and-apkbuilder-report.md`
- Module-Konsolidierung ist bereits teilweise erledigt:
  - `module-consolidation-report.md`
  - `reconciliation-code-truth.tsv`

Ziel:
- bestehende Strukturen nicht doppeln
- klare Zuständigkeiten zwischen Log / LSP / Tooling / Plugin / Build schaffen
- keine neuen Widersprüche erzeugen

══════════════════════════════════════════════════════════════════
PHASE A – INVENTAR DER AKTUELLEN STRUKTUR
══════════════════════════════════════════════════════════════════
A1. Finde alle relevanten Log-/LSP-/Tooling-Komponenten:
    - LogRouter
    - ToolingLogManager
    - ToolingBottomSheet
    - GradleTaskManager / GradleTaskManagerImpl
    - ScriptedLspServer
    - LspRegistry
    - EditorViewModel
    - LogCatcher
    - PluginManager / PluginStore
    - BuildHelper / ApkBuilder

A2. Dokumentiere:
    - wer erzeugt Logs
    - wer routet Logs
    - wer zeigt Logs an
    - wer verarbeitet LSP
    - wer verarbeitet Build-Tasks
    - wer verarbeitet Plugin-Installationen

A3. Erstelle:
    `build/tooling-structure-inventory.tsv`

══════════════════════════════════════════════════════════════════
PHASE B – AUFRÄUMEN / NORMALISIEREN
══════════════════════════════════════════════════════════════════
B1. Entferne Dopplungen in Log- und Tooling-Namen
    - keine mehrfachen Begriffe für denselben Kanal
    - keine widersprüchlichen Kategorien

B2. Prüfe ToolingBottomSheet:
    - Tabs / Panels sollen konsistent zu den Kanälen passen
    - keine unnötigen Sonderfälle
    - kein Tab-Overload durch doppelte Zuständigkeiten

B3. Prüfe LSP-Fluss:
    - Start / Connect / Error / Stop
    - Diagnostics klar getrennt
    - Statusindikatoren sinnvoll gekoppelt

B4. Prüfe Build-Fluss:
    - Gradle-Tasks und APK-Builds sauber getrennt
    - keine versteckte doppelte Build-Auslösung

B5. Prüfe Plugin-Fluss:
    - Plugin-Installation und Plugin-Reporting klar
    - keine Vermischung mit Terminal-/Build-Logs

B6. Änderungen nur dort, wo tatsächlich Verwirrung oder Doppelung besteht.

══════════════════════════════════════════════════════════════════
PHASE C – VALIDIERUNG
══════════════════════════════════════════════════════════════════
C1. `./gradlew assembleDebug` → BUILD SUCCESSFUL

C2. Relevante Tests → grün

C3. On-Device-Smoke:
    - Build-Tab
    - LSP-Status
    - Log-Tab
    - Plugin-Installation
    - keine verlorenen Funktionen

C4. Report:
    `build/tooling-structure-cleanup-report.md`

C5. Commit:
    `refactor(tooling): normalize log, lsp, build, and plugin structure`

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN
══════════════════════════════════════════════════════════════════
- Keine bereits verifizierte Funktion beschädigen.
- Keine doppelte Kanalisierung einführen.
- Keine neue Tooling-Architektur ohne Not.
