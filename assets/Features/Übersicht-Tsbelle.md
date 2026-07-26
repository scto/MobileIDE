| # | Prompt-Dateiname | Kategorie | Priorität | Aufwand | Abhängig von (#) | Blockiert / relevant für (#) |
|---|---|---|---|---|---|---|
| 01 | `01-gradle-ide-terminal-bridge.md` | Core-Tooling | 🔴 Hoch | Mittel | – | 02, 09, 10, 11, 13 |
| 02 | `02-ui-build-apk-assembledebug.md` | Build/UI | 🔴 Hoch | Niedrig–Mittel | 01 | 03, 04, 10 |
| 03 | `03-android-keystore-signing-tool.md` | Build/Security | 🟠 Mittel-Hoch | Niedrig–Mittel | 02 | – |
| 04 | `04-compose-layout-preview-module.md` | Neues Feature | 🟠 Mittel | Hoch | 02, (06 empfohlen) | – |
| 05 | `05-full-lsp-treesitter-integration.md` | Editor/Core | 🟠 Mittel-Hoch | Sehr hoch | (06 empfohlen) | 10, 14 |
| 06 | `06-modularization-features-consolidation.md` | Refactoring | 🔴 Hoch (strukturell) | Mittel–Hoch | 01 (idealerweise vor 04/05) | 04, 05, 07, 10, 14 |
| 07 | `07-migrate-xed-editor-plugins.md` | Plugin-Ökosystem | 🟢 Mittel | Mittel, iterativ | 06 (empfohlen) | – |
| 08 | `08-central-version-catalog-consolidation.md` | Build-Infrastruktur | 🟢 Mittel | Niedrig–Mittel | – | 11 |
| 09 | `09-dynamic-sandbox-paths.md` | Build-Infrastruktur | 🟠 Mittel-Hoch | Mittel | – | 06, 11, 13 |
| 10 | `10-interactive-debugger-jdwp.md` | Neues Feature | 🟢 Niedrig-Mittel | Sehr hoch | 01, 02, (05, 06 empfohlen) | – |
| 11 | `11-ci-pipeline-main-project.md` | Qualitätssicherung | 🟠 Mittel-Hoch | Mittel | 08, 09 (empfohlen) | 06 (Absicherung) |
| 12 | `12-git-3way-merge-conflict-tool.md` | UI/Tooling | 🟢 Niedrig-Mittel | Mittel | – | – |
| 13 | `13-gradle-dependency-cache-manager.md` | UI/Tooling | 🟢 Niedrig | Niedrig–Mittel | 01, 09 | – |
| 14 | `14-inline-docs-hover-panel.md` | Editor-Feature | 🟢 Niedrig-Mittel | Mittel | 05, 06 | – |9