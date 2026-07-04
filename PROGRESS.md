# MobileIDE Progress History

This file tracks the timeline of all features, bug fixes, and refactoring efforts implemented in MobileIDE, ordered from newest to oldest.

---

## [2026-07-04]
### Language Server Extensions & Sandbox CLI Overhaul
*   **Renamed and Expanded Language Extension**: Migrated `:extension-kotlin-lsp` to `:extension-languages` and packaged support for Kotlin, Java, Bash, and XML language servers into a single library extension.
*   **Command Registration Infrastructure**: Introduced `CommandManager` to allow dynamic extensions to safely register and execute custom IDE Commands.
*   **Scripted LSP Terminal Execution**: Introduced a `terminalLauncher` delegate into `ScriptedLspServer` and implemented the host terminal routing inside `MainActivity` to allow LSPs to trigger their installer scripts in the interactive terminal.
*   **Dynamic SDK Version Setup**: Refactored `idesetup` to pull `manifest.json` early, parse it using `jq`, and prompt the user with a dynamic selection menu containing version layers like `37.0.0` based on the host architecture.
*   **Smart Environment CLI (`ideenv`)**: Replaced the baseline properties text editor `ideenv` with a smart environment controller supporting key settings (`set KEY=VALUE`), lookups (`get KEY`), session exports (`eval $(ideenv --export)`), and path verification warnings.
*   **LSP Installer Scripts & Settings Clean-up**: Purged all deprecated LSP installer scripts from `app/src/main/assets/terminal/lsp` and created new scripts for `java.sh` and `kotlin.sh`. Refactored `LspSettingsScreen.kt`'s path checker to target only the four supported servers (Java, Kotlin, Bash, XML).

## [2026-07-03]
### Terminal Stability & UX Fixes
*   **Foreground Notification Icon Crash**: Resolved a runtime crash (`Unable to start service`) in `TerminalService` caused by passing an Adaptive Icon to the Notification Builder on Android 8.0+. Replaced `R.mipmap.ic_launcher` with `R.drawable.ic_code`.
*   **Terminal Settings Navigation Fix**: Fixed a crash thrown by the Jetpack Compose Navigation component when trying to open terminal settings by correcting the hardcoded route string `terminal_settings` to `settings/terminal`.
*   **Terminal Initialization & Setup Flow**: Redesigned the container setup (`setup.sh` & `init.sh`) to natively resolve project paths using `MOBILEIDE_PROJECT_DIR` instead of nested bash execution commands. This prevents premature exit bugs and allows the startup script to pause gracefully after rendering the `idesetup` MOTD before dropping into the interactive shell.
*   **`.bashrc` Environment Consistency**: Implemented `.bashrc` autogeneration in `setup.sh` so that nested shells source `bash-completion` and properties natively.
*   **Gradle Home Export**: Modified `idesetup` to automatically insert `GRADLE_USER_HOME=$HOME/.gradle` and `AAPT2_HOME=/.mobileide` into `mobileide-environment.properties`.
*   **AAPT2 Template Override**: Configured the project extraction logic in `NewProjectScreen.kt` to append `android.aapt2FromMavenOverride=/.mobileide/aapt2` to the `gradle.properties` file of newly created projects, ensuring they use the correct native AAPT2 binaries.

---

## [2026-07-01]
### Theme Switching Fix
*   **Global Light/Dark Mode Sync**: Fixed an issue where the app (especially Android Views like `CodeEditorView` and `TerminalView`) remained dark when switching to the Light design. Synchronized `AppCompatDelegate` with Compose's `ThemeState` inside `MainActivity` to ensure the Activity configuration correctly resets to `uiMode = night=no`, allowing system color schemes and view backgrounds to correctly resolve to light variants.

### Terminal UI & UX Refinement
*   **Scrollable Terminal Settings**: Added `verticalScroll` and `rememberScrollState` to `TerminalSettingsScreen` to ensure that all options are reachable on smaller screens.
*   **Terminal Font Size Scaling Fix**: Fixed microscopic text size bug in terminal by converting the font size setting (10-30sp, default 12) to physical pixels using the device display metrics (`scaledDensity`) before passing it to `TerminalView.setTextSize()`.
*   **Default Close Last Session Behavior**: Changed default behavior when closing the last terminal session from exiting the application (`exit_app`) to creating a new terminal session (`new_session`).
*   **Custom Terminal Color Schemes**:
    *   Added a new `terminal_colorscheme` preference to `Settings`.
    *   Created five built-in properties-based themes inside `app/src/main/assets/terminal/colorschemes/`: `default`, `dracula`, `solarized_dark`, `nord`, and `monokai`.
    *   Implemented dynamic theme application on the `TerminalView` using `TerminalColors.COLOR_SCHEME.updateWith(props)` and forcing emulator color reset on creation and updates.
    *   Exposed a scrollable horizontal filter chip selection row for Farbschemas inside the `TerminalSettingsItem` UI.
*   **Documentation overhaul**: Revised `README.md` and `README_DE.md` to reflect the newly modularized subproject layout of the repository. Created `PROGRESS.md` to track commit history by date.
*   **Gradle dependency and sandbox isolation fixes**: Handled finished terminal session close requests using a decoupled callback inside `TerminalBackEnd.kt` to avoid circular dependency compile errors between `:core:main` and `:app`.

---

## [2026-06-30]
### Initial Environment Boot & Sandboxed CLI Setup
*   **FGS Start Exception Fix**: Resolved `ForegroundServiceStartNotAllowedException` on Android 12+ by prompting the user for Post Notification permissions on the welcome/permissions screen before launching `TerminalService`.
*   **Tar Archive Decompression Resolution**: Fixed initial extraction failure of `tar.xz` packages by adding `xz` (Alpine) / `xz-utils` (Debian/Ubuntu) to the automated requirements check during setup.
*   **CURL Package Fallback**: Swapped `libcurl` with standard `curl` in Debian/Ubuntu setup packages to guarantee compatibility and prevent package installation blockages.
*   **Interactive Onboarding Flow**: Replaced raw shell invocation of `idesetup` during setup with an interactive setup notice screen, preventing headless TTY read errors when starting container environments.
*   **Setup Asset copy**: Added copying logic for `ideenv` and `idesetup` shell helper executables inside `setup.sh` to initialize Distro paths.

---

## [2026-06-29]
### Refactoring, Build Pre-checks, and Core Tooling Submodules
*   **Git and Gradle Sandbox Automations**: Removed redundant manual Gradle items from Settings, opting to auto-install `git` and `gradle` directly into the Ubuntu container environment.
*   **Gradle Permissive Builds**: Fixed shared storage permissions blockage (`Permission denied`) when calling `gradlew` by wrapper-script execution via bash shell and passing environment variables.
*   **Settings Screen Clean-up**: Separated LSP completion options into *"LSP Bash Scripts"* (Terminal Settings) and *"LSP Editor"* (Editor Settings) toggle switches with descriptive details.
*   **TreeSitter XML & KTS Syntax Highlight Fixes**: Resolved XML node syntax query errors, replaced outdated KTS highlights queries with official schemas, and resolved a tab rendering bug by immediate application of compose themes.
*   **LogCatcher Logging Control**: Added optional LogCatcher logging control toggles to enable or disable verbose runtime debugging.
*   **Modular Architecture Refactoring**: Moved build and logging logic into two new submodules `:core:tooling:tooling-api` and `:core:tooling:tooling-impl`.
*   **Categorized Logs sheet**: Created real-time log categorization routing under *Terminal*, *Problems*, *IDE*, *Build*, and *LSP* tabs.
*   **Interactive Gradle Tasks Panel**: Built a dynamic parser querying container Gradle tasks, listing them with checkbox selection, and executing them asynchronously inside the logs pane.

---

## [2026-06-28]
### Semantic Highlighting Engine Integration
*   **TreeSitter Engine Integration**: Added `:language-treesitter` module with support for Java, Kotlin, XML, and Log file types.
*   **Launcher Icon Assets**: Updated launcher icon resources across project configurations.
