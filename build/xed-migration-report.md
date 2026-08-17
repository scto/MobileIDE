# Xed-Editor-Migration nach MobileIDE: Status & Protokoll

## Phase 0 – Baseline
- **Erstellungsdatum:** 2026-08-17
- **Root-Pfad MobileIDE:** `/data/data/com.termux/files/home/MobileIDE`
- **Root-Pfad Xed-Editor (Quelle):** `/data/data/com.termux/files/home/Xed-Editor`
- **Baseline-Tag:** `xed-migration-baseline-20260817`
- **Baseline-Branch:** `pre-xed-migration`
- **Baseline-Commit:** `a87b6951` (`refactor(modules): consolidate terminal settings and assets into single source of truth`)
- **Build-Status:** `BUILD SUCCESSFUL` (`./gradlew assembleDebug :core:apk-builder:testDebugUnitTest :core:tooling:tooling-api:test`)
- **Aktiver Unit-Test-Stand:** 16 Tests in `:core:apk-builder` grün, `:core:tooling:tooling-api` grün.

---
