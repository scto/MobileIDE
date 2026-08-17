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

## Phase 1 – Inventar & BFS
- **Inventar:** 52 Quelldateien (.kt) in `core/main/{extension,feature,lsp,search}` + `features/extensions`.
- **Abhängigkeits-BFS:** In `build/xed-bfs-dependencies.tsv` erfasst und doppelgeprüft.
- **Paket-Mapping:** In `build/xed-package-mapping.tsv` dokumentiert (ausnahmslos `com.rk` -> `com.scto.mobile.ide`).
- **Adapter-Klassen:** `MainActivity` und `MainViewModel` werden konsequent auf den Adapter `XedHost` umgeleitet.

## Phase 2 – Toolchain & Versionskatalog
- **AGP/Kotlin-Entscheidung:** Wir verbleiben auf der stabilen MobileIDE-Toolchain (AGP 8.13.1, Kotlin 2.3.0, compileSdk 36) und passen die Modul-Build-Dateien selektiv an.
- **Katalog-Ergänzungen:** `kotlinx-serialization-json` (1.10.0) im Versionskatalog ergänzt; `lsp4j`, `monarch`, `regex-onig`, `semver`, `gson`, `okhttp` bereits vorhanden.
- **Build-Prüfung:** `./gradlew assembleDebug :core:apk-builder:testDebugUnitTest :core:tooling:tooling-api:test` erfolgreich und grün.


