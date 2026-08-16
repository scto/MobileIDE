# APK-Builder Path Resolution Fix & Kotlin-LSP Consistency Report

**Date:** 2026-08-16  
**Prompt Executed:** `~/MobileIDE/prompt-fix.md`  

---

## 1. Executive Summary & Root Cause Analysis

### Diagnosed Cause (Phase A)
- **Primary Root Cause:** The APK-Builder runner in [`CodeEditScreen.kt`](file:///data/data/com.termux/files/home/MobileIDE/app/src/main/java/com/scto/mobile/ide/ui/editor/CodeEditScreen.kt) constructed `targetDir` via string interpolation (`"$cleanPath/$folderName"`), which could leave trailing spaces or produce host paths like `/storage/emulated/0/MobileIDEProjects`. When executed inside the PRoot sandbox container without translating host paths to sandbox paths (`/sdcard/...`), Gradle was launched inside the parent directory (`/storage/emulated/0/MobileIDEProjects`) instead of the project directory (`/sdcard/MobileIDEProjects/MyApp`).
- **Active Distro Environment:** `MOBILEIDE_DISTRO` resolves to `ubuntu` (the primary/active distro). Alpine rootfs issues were skipped as per Phase A1/H3 rules.

---

## 2. Work & Fixes Implemented

### Phase B — Hardened `PathTranslator` & Assertions
- Updated [`PathTranslator.kt`](file:///data/data/com.termux/files/home/MobileIDE/core/apk-builder/src/main/java/com/scto/mobile/ide/core/apkbuilder/PathTranslator.kt):
  - Added strict assertion checks ensuring the last path segment (e.g. `MyApp`) is never stripped or altered during host ↔ sandbox translation.
  - Added unit test coverage in [`PathTranslatorTest.kt`](file:///data/data/com.termux/files/home/MobileIDE/core/apk-builder/src/test/java/com/scto/mobile/ide/core/apkbuilder/PathTranslatorTest.kt) verifying paths with spaces and segment preservation.

### Phase C & E — Process & Working Directory Fixes
- Updated [`CodeEditScreen.kt`](file:///data/data/com.termux/files/home/MobileIDE/app/src/main/java/com/scto/mobile/ide/ui/editor/CodeEditScreen.kt):
  - Explicitly wrapped target working directories with `PathTranslator.toSandboxPath(...)`.
  - Ensured `DistroManager.buildProotCommand` receives `workDir = cleanTargetDir` so PRoot spawns `sh` directly inside the translated sandbox project directory.

### Phase G — Kotlin-LSP Consistency Matrix
- Generated [`build/kotlin-lsp-consistency.tsv`](file:///data/data/com.termux/files/home/MobileIDE/build/kotlin-lsp-consistency.tsv) documenting the exact separation of:
  1. Built-in LSP (`:features:lsp`, `local/bin/lsp/kotlin.sh` → `kotlin-language-server`).
  2. Plugin `xed-kotlin` (`kotlin-lsp-installer.sh` → JetBrains `intellij-server`).
  3. Plugin `xed-kmp-lsp` (`kmp-lsp-installer.sh` → Rust `kmp-lsp`).

---

## 3. Consistency Matrix

| Source | Script | Target Dir | Binary | Version | File Extensions | LspRegistry ID | Conflict? |
|---|---|---|---|---|---|---|---|
| Built-in (`:features:lsp`) | `local/bin/lsp/kotlin.sh` | `/opt/kotlin-language-server` (`/usr/local/bin`) | `kotlin-language-server` | GitHub Release (Latest) | `.kt`, `.kts` | `kotlin_lsp` | None (Built-in Standard) |
| Plugin `xed-kotlin` | `kotlin-lsp-installer.sh` | `$HOME/.lsp/kotlin/bin/` | `intellij-server` | JetBrains 262.8190.0 | `.kt`, `.kts` | `xed_kotlin_lsp` | None (Catalog Plugin) |
| Plugin `xed-kmp-lsp` | `kmp-lsp-installer.sh` | `$HOME/.lsp/kmp-lsp/` | `kmp-lsp` + `kmp-jar-indexer` | v0.24.0 (Rust) | `.kt`, `.java`, `.swift` | `xed_kmp_lsp` | Decoupled / De-duplicated |

---

## 4. Verification Results

1. **Host Build Verification:**
   - Command: `./gradlew assembleDebug`
   - Result: `BUILD SUCCESSFUL in 1m 27s`
2. **Unit Tests Verification:**
   - Command: `./gradlew :core:apk-builder:testDebugUnitTest`
   - Result: `BUILD SUCCESSFUL in 1m 21s` (16 tests passed)
