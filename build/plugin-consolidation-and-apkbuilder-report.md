# MobileIDE Consolidation & APK-Builder Path Fix Report

## Executive Summary
This report documents the completion of the prompt `prompt-Plugin-Konsolidierung-APK-Builder-Pfad-Fix.md` divided into two independent sub-projects:

1. **Part 1: Plugin Consolidation & Namespace Standardization**
   - Consolidated single source of truth for all 9 active MobileIDE plugin modules inside `~/MobileIDE/plugins/`.
   - Completed full package namespace migration to `com.scto.mobile.ide.plugin.*`.
   - Corrected `copyPluginToAssets` build task target path to `assets/Plugins/LSP/`.
   - Deleted legacy `app/src/main/assets/bundled_plugins/` directory.

2. **Part 2: APK-Builder Host-to-Sandbox Path Translation**
   - Implemented `PathTranslator` in `:core:apk-builder` to seamlessly translate `/storage/emulated/0/...` host paths to `/sdcard/...` PRoot sandbox paths.
   - Added two-stage path validation logic in `ApkBuilder.kt` with explicit logging (`HOST_EXISTS`, `SANDBOX_EXISTS`).
   - Added explicit PRoot bindings (`-b /storage/emulated/0:/sdcard`) in `DistroManager.kt`.
   - Verified via unit test `PathTranslatorTest` (`BUILD SUCCESSFUL`).

---

## Part 1: Plugin Consolidation Overview

| Modul | Single Source of Truth | Alter Package-Name | Neuer Package-Name | Version | Task Target | Status |
|---|---|---|---|---|---|---|
| `fsharp-lsp` | `plugins/fsharp-lsp` | `io.kiquar.plugin.fs` | `com.scto.mobile.ide.plugin.fs` | `0.1.0` | `assets/Plugins/LSP` | ✅ OK |
| `go-lsp` | `plugins/go-lsp` | `io.kiquar.plugin.go` | `com.scto.mobile.ide.plugin.go` | `0.2.0` | `assets/Plugins/LSP` | ✅ OK |
| `json-lsp` | `plugins/json-lsp` | `com.scto.mobile.ide.plugin.json` | `com.scto.mobile.ide.plugin.json` | `1.0.2` | `assets/Plugins/LSP` | ✅ OK |
| `lua-lsp` | `plugins/lua-lsp` | `com.scto.mobile.ide.plugin.lua` | `com.scto.mobile.ide.plugin.lua` | `1.0.2` | `assets/Plugins/LSP` | ✅ OK |
| `prettier-lsp` | `plugins/prettier-lsp` | `com.koner.prettier` | `com.scto.mobile.ide.plugin.prettier` | `1.0.0` | `assets/Plugins/LSP` | ✅ OK |
| `python-lsp` | `plugins/python-lsp` | `com.scto.mobile.ide.plugin.python` | `com.scto.mobile.ide.plugin.python` | `1.0.3` | `assets/Plugins/LSP` | ✅ OK |
| `rust-lsp` | `plugins/rust-lsp` | `com.koner.rust` | `com.scto.mobile.ide.plugin.rust` | `1.0.2` | `assets/Plugins/LSP` | ✅ OK |
| `typst-lsp` | `plugins/typst-lsp` | `com.koner.typst` | `com.scto.mobile.ide.plugin.typst` | `1.2.3` | `assets/Plugins/LSP` | ✅ OK |
| `zig-lsp` | `plugins/zig-lsp` | `io.kiquar.plugin.zig` | `com.scto.mobile.ide.plugin.zig` | `0.4.0` | `assets/Plugins/LSP` | ✅ OK |

---

## Part 2: APK-Builder Host ↔ Sandbox Path Translation

| Komponente | Diagnose | Lösung | Test-Ergebnis |
|---|---|---|---|
| **Path Translation** | Host Path `/storage/emulated/0/...` was inaccessible inside PRoot sandbox without translation. | Created `PathTranslator.toSandboxPath()` & `toHostPath()` | `PathTranslatorTest` ✅ Passed |
| **ApkBuilder Validation** | `projectDir.exists()` failed when checking host path inside container context. | Two-stage check checking both Host and Sandbox paths with detailed diagnostics. | Unit Tests & Build ✅ OK |
| **PRoot Bindings** | Container mounts missing explicit `/storage/emulated/0` mount alias. | Added `-b /storage/emulated/0:/sdcard` in `DistroManager.kt`. | Build & Container ✅ Bound |

---

## Reconciliation (2026-08-16)

The codebase truth has been reconciled against all generated TSV tables and documentation reports per `prompt-fix-2.md`:

### 1. Code-Wahrheit-Tabelle (`reconciliation-code-truth.tsv`)
- All 9 plugin modules in `~/MobileIDE/plugins/` have been verified.
- 0 legacy imports remain in active Kotlin code (`import com.rk.*`, `com.koner.*`, `io.kiquar.*`, `androdev.*`).
- `namespace` and `applicationId` across all 9 plugins strictly enforce the `com.scto.mobile.ide.plugin.<name>` naming convention (Resolving inconsistency in `json-lsp`, `lua-lsp`, `python-lsp`).

### 2. Resolved Inconsistencies
1. **Migration Status Reconciled:** TSV files updated to mark all 9 plugins as `MIGRIERT` with target package `com.scto.mobile.ide.plugin.*`.
2. **Package Conventions Unified:** Standardized on `com.scto.mobile.ide.plugin.<name>` (singular).
3. **`json-lsp` & `python-lsp` Unified:** Fixed leftover demo / plural namespaces in `app/build.gradle.kts` files to `com.scto.mobile.ide.plugin.json` and `com.scto.mobile.ide.plugin.python`.
4. **`rootProject.name` Standardized:** Standardized to `"MobileIDE Plugin: <Name>"` across all 9 `settings.gradle.kts` files.
5. **LSP Script Inventory Corrected:** Cleaned up `lsp-script-inventory.tsv` removing false positive `sdk.jar` entries and correcting file sizes and installation scripts.

---

## Summary Statement
Plugins consolidated to a single source of truth, all `com.scto.mobile.ide.plugin.*` packages fully consistent, assets cleanly located in `assets/Plugins/LSP/`, APK-Builder correctly builds projects from PRoot sandbox using translated `/sdcard/` paths.
