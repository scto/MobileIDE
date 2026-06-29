# MobileIDE Change Log / Status

This file tracks the features, bug fixes, and improvements implemented by AI coding assistants.

## [2026-06-13] Settings Panel Enhancements

Implemented crucial terminal, editor, build, and LSP configurations to complete the Settings screen.

### 1. Terminal Management
*   **Reset Terminal**: Added a reset feature that closes all active shell sessions, resets cached project environments, and spawns a fresh session.
*   **Reinstall Terminal**: Added a reinstall action that deletes the existing Alpine rootfs (`local/alpine` and `alpine.tar.gz`) and triggers a fresh environment extraction and initialization.

### 2. Editor Settings
*   **Font Size Adjustment**: Exposed a new Slider (ranging from 8sp to 32sp) to customize the editor font size. Changes are written dynamically to SharedPreferences under `editor_font_size` and autosaved.

### 3. Build Configuration Group
Created a dedicated **Build Config** section to manage Android and Java build dependencies inside the Alpine environment:
*   **OpenJDK Selection**: Supports installing and switching between **OpenJDK 17** and **OpenJDK 21**.
*   **Gradle Build System**: Check status and install the Gradle build system.
*   **Android SDK**: Setup the Android SDK Command Line Tools (`sdkmanager`).
*   **Build-Tools v35 / v36**: Parallel status check and individual installations for Android SDK Build-Tools.
*   **Platforms (API 34/35)**: Install android platforms via `sdkmanager`.
*   **CMake & NDK**: Manage native compilation components.
*   **Base Build Utilities**: Quick install for essential Alpine compiling packages (`build-base`, `bash`, `git`, `wget`, `curl`, `gcompat`).

### 4. LSP (Language Server Protocol) Status Group
Added a dedicated **LSP Status** card to verify language server availability:
*   **Java LSP**: Check/install `jdtls` and its JDK 17 dependencies.
*   **Kotlin LSP**: Check/install `kotlin-language-server` from edge testing repositories.
*   **TypeScript LSP**: Check/install Node/npm and `typescript-language-server`.
*   **Web LSPs**: Check/install HTML/CSS/JSON language servers (`vscode-langservers-extracted`).

## [2026-06-29] Editor Engine Selection & TreeSitter Syntax Highlighting Fixes

### 1. Editor Highlight Engine Selection
*   **Engine Preference Toggle**: Added list selection to `EditorScreen.kt` and `SettingsScreen.kt` to allow users to switch between the classic TextMate highlighter and the TreeSitter (LSP) highlighter.
*   **Fallback Logic**: If TreeSitter is preferred but unsupported for the target file extension, the editor gracefully falls back to the TextMate engine.

### 2. LogCatcher Configuration
*   **Logging Settings Control**: Integrated an optional LogCatcher logging control toggle. When enabled/disabled in the Settings panel, verbose log messages are written/suppressed dynamically inside the workspace directory (`/sdcard/MobileIDEProjects/logs/`).

### 3. TreeSitter XML Grammar Corrections
*   **AndroidIDE Compatibility Fix**: Resolved SCM parsing error (`Failed to create TreeSitter language for: xml - bad scm sources: error NodeType occurs in highlight range`) by migrating highlights queries to Yadav's custom AndroidIDE `tree-sitter-xml` node definition syntax (mapping `empty_element`, `tag_start`, `tag_end`, `xml_decl`, etc.).
*   **Theme Mapping**: Configured proper sora-editor theme color mapping in `EditorViewModel.kt` to bind the new AndroidIDE-specific XML query tags to CSS/HTML theme colors.

### 4. TreeSitter Kotlin Script (KTS) Query Fixes
*   **Grammar Alignment**: Replaced local Kotlin highlights schema with the official `fwcd/tree-sitter-kotlin` highlight schema configuration. This eliminated parser errors caused by obsolete or incorrect Kotlin AST nodes (like `multiline_lambda_parameter` and `receiver_type: (_)`).
*   **Immediate Highlight Color Application**: Fixed a timing bug where newly opened file tabs rendered monochrome text by caching the active Jetpack Compose `ColorScheme` and applying theme colors immediately when loading TreeSitter instances.
