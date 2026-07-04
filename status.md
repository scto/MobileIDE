# MobileIDE Change Log / Status

This file tracks the features, bug fixes, and improvements implemented by AI coding assistants.

## [2026-07-04] Extension Framework & CLI Enhancements

### 1. Extension Framework & Dynamic LSP Registries
*   **Command Registration API**: Fully implemented `CommandManager.kt` inside `:core:commands`, enabling dynamic extensions to register, unregister, and execute IDE commands with UI integration.
*   **Decoupled Terminal Launcher for LSP**: Fixed the `ScriptedLspServer` terminal execution `TODO` by introducing a `terminalLauncher` delegate. Hooked this delegate up inside `MainActivity` to execute LSP installer scripts directly within the MobileIDE terminal via `TerminalCommand`.
*   **Languages Extension Module (`:extension-languages`)**: Created a unified Kotlin/Java library module replacing the previous single Kotlin LSP extension stub. It registers four major language servers upon load:
    *   **Kotlin**: Custom Edge server wrapper.
    *   **Java**: Eclipse JDT Language Server (`jdtls`), resolved natively inside Termux.
    *   **Bash**: `bash-language-server` via npm.
    *   **XML**: Eclipse LemMinX XML Language Server via Java jar execution.
*   **Ubuntu Kotlin LSP Support**: Rewrote `kotlin.sh` to download the precompiled zip release of `kotlin-language-server` from GitHub and install it to `/opt` with a symlink to `/usr/local/bin` inside the container. This makes it fully compatible with Ubuntu (which lacks `kotlin-language-server` packages in default repos).
*   **Aligned Extension Servers to Container Paths**: Refactored the `isInstalled` and `getConnectionConfig` methods in `KotlinLspServer.kt`, `JavaLspServer.kt`, `BashLspServer.kt`, and `XmlLspServer.kt` to target correct container-relative paths (e.g. `/usr/local/bin` and `/root/.lsp`) instead of incorrect host-relative local paths, aligning the runtime connection configs with the installer scripts.
*   **Decoupled APK Builder PRoot integration**: Modified `ApkBuilder.kt` to accept an optional `configureProcessBuilder` lambda to allow `:app` to wrap the host process builder inside the sandbox container. Updated `CodeEditScreen.kt`'s `handleRunApk` to pass the PRoot command, resolving the `Cannot run program "bash"` crash when building and running projects via the UI play button.
*   **Corrected LSP Install Script Paths**: Fixed a path mismatch where the new language servers (`Kotlin`, `Java`, `Bash`, `XML`) expected their installer scripts to be named `install_*_lsp.sh` directly in `localBinDir()`. Updated them to point to `lsp/*.sh` inside `localBinDir()` matching the assets extraction directory structure.
*   **LSP Installer Scripts & Settings Clean-up**: Removed obsolete language server scripts (`css`, `html`, `json`, `typescript`, `python`, `eslint`, `emmet`, `markdown`) from `app/src/main/assets/terminal/lsp` and added native installer scripts for `java.sh` and `kotlin.sh`. Updated the container binary checking logic in `LspSettingsScreen.kt` to dynamically map only these four core languages (Java, Kotlin, Bash, XML).

### 2. Sandbox CLI & Version Selection Upgrades
*   **Dynamic SDK Version Selection**: Overhauled `idesetup` (in both `MobileIDE` and `mobileide-tools`) to download the `manifest.json` early, parse it using `jq`, and present the user with a dynamic selection menu of all available SDK version layers (such as `37.0.0`) on startup.
*   **Interactive CLI environment manager (`ideenv`)**: Completely rewrote the `ideenv` script to transition it from a raw file editor into a smart CLI utility. Added support for `set KEY=VALUE` (with directory and path warnings), `get KEY` (for scripting), `reset` (with confirmation), and `eval $(ideenv --export)` to inject variables immediately into the running terminal session without a restart.
*   **Dynamic CPU Architecture OpenJDK Detection**: Fixed `Missing required components: * OpenJDK 17 or OpenJDK 21` pre-build error on ARM64 and other CPU architectures. Modified `CodeEditScreen.kt` and `BuildSettingsScreen.kt` to dynamically look up openjdk directories under `usr/lib/jvm` matching any architecture suffix (e.g. `java-17-openjdk-arm64`), establishing robust detection and path mapping for all JVM builds.
*   **Bypassed Shared Storage Executable Restriction (`noexec`)**: Fixed `sh: ./gradlew: Permission denied` / `sh: ./gradlew: not found` errors when building via the play button or normal build. Replaced direct `./gradlew` executions with `bash ./gradlew` in `CodeEditScreen.kt` to run the script under the container's executable shell, completely bypassing the `noexec` flags imposed by Android on shared/emulated storage.
*   **Corrected idesetup openssh Package Name**: Fixed an issue where running `idesetup` inside a Debian/Ubuntu container crashed with `E: Unable to locate package openssh`. Adjusted the package selection to dynamically use `openssh-client openssh-server` on `apt` systems and `openssh` on `apk` (Alpine) systems.
*   **AAPT2 Compile Hotfix**: Resolved ARM/AARCH64 compilation issues by reactivating the local `aapt2FromMavenOverride` path in `gradle.properties`, enabling a successful full APK compile.
*   **Successful APK Compilation**: Compiled and outputted `MobileIDE-0.0.1-DEBUG-debug.apk` with all updated extensions, dynamic version menu, and smart CLI features.

## [2026-07-03] Terminal Stability & Initialization Flow

### 1. Terminal Startup Fixes
*   **Foreground Service Crash**: Fixed an `IllegalArgumentException` causing the `TerminalService` to crash on launch by replacing the Adaptive Icon (`R.mipmap.ic_launcher`) with a standard vector drawable (`R.drawable.ic_code`) for the foreground notification.
*   **Settings Navigation Crash**: Corrected a route mismatch in `TerminalScreen` where clicking the settings icon attempted to navigate to `terminal_settings` instead of `settings/terminal`, which crashed the app.

### 2. Terminal Setup UX Overhaul
*   **Environment Initialization**: Revised the container startup sequence (`init.sh` and `setup.sh`). The environment now completely initializes (e.g. running `apt update`) in the background, prints the MOTD with the `idesetup` hint, and pauses (`Press any key to continue...`) before gracefully transitioning into an interactive bash prompt (`root@localhost:/#`).
*   **Premature Exit Bug**: Fixed a bug where passing a bash command to automatically CD into the project directory caused the outer shell to exit immediately after execution. Native directory resolution now uses the `MOBILEIDE_PROJECT_DIR` environment variable to ensure the interactive session persists.
*   **`.bashrc` Autogeneration**: `setup.sh` now generates a `.bashrc` file in the container's `/root` directory. This ensures that manually spawning nested `bash` instances still automatically sources `mobileide-environment.properties` and enables `bash-completion`.
*   **Gradle Workspace Configuration**: Updated `idesetup` to automatically export `GRADLE_USER_HOME=$HOME/.gradle` and `AAPT2_HOME=/.mobileide` into the environment properties alongside `JAVA_HOME`.
*   **AAPT2 Maven Override Injection**: Modified the project generation logic in `NewProjectScreen.kt` to automatically append `android.aapt2FromMavenOverride=/.mobileide/aapt2` to `gradle.properties` when creating new projects from templates.

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

### 5. APK Compilation & Gradlew Permissions Resolution
*   **PRoot Compile Environment**: Resolved execute permission (`Permission denied`) and environment (`JAVA_HOME set to an invalid directory`) errors when running `gradlew` from shared storage. The build process now executes inside the Alpine PRoot container using `DistroManager.buildProotCommand` and correct PRoot environment variables.
*   **Pre-Build Verification**: Added automatic checks before launching the build process to verify the installation of OpenJDK 17/21, Gradle, Android SDK, and optionally CMake/NDK if C/C++ files are present, logging detailed instructions on failure.
*   **Settings Panel Refactoring**: Moved the general LSP Completion toggle to `TerminalSettings` as *"LSP Bash Scripts"* (`settings_lsp_bash_scripts`).
*   **LSP Editor Toggle**: Added a new *"LSP-Editor"* switch to `EditorSettings`, which defaults to `on` (TreeSitter) and provides a description explaining the difference between TreeSitter semantic highlighting and classic TextMate.
*   **Core Tooling Submodules (API & IMPL)**: Created `:core:tooling:tooling-api` and `:core:tooling:tooling-impl` submodules to handle logging and build tools.
*   **Categorized Logs**: Implemented real-time routing of app logs into five dedicated tabs inside the bottom sheet panel: *Terminal Logs*, *Problems (Diagnostics)*, *IDE Log*, *Build*, and *LSP*.
*   **Interactive Gradle Tasks Panel**: Queried Gradle tasks dynamically inside the container and listed them with checkboxes inside the *Build / Tasks* bottom panel. Enabled task execution using a Play icon button, streaming output directly to the Build logs in real-time.

## [2026-06-30] Sandboxed CLI & FGS Permission Fixes
*   **Foreground Service Permission Onboarding**: Prevented `ForegroundServiceStartNotAllowedException` crashes on Android 12+ by dynamically prompting the user for Post Notification permissions during onboarding.
*   **CLI Decompression Compatibility**: Resolved extraction errors for `tar.xz` archives by packaging `xz-utils` (Debian/Ubuntu) / `xz` (Alpine) into container requirements.
*   **Setup Script Integration**: Automatically copied `ideenv` and `idesetup` shell executables into Alpine host bin paths to enable workspace command-line tooling.

## [2026-07-01] Terminal Usability & Custom Styling
*   **Theme Switching Fix**: Resolved an issue where the app layout (including Android Views like the CodeEditor and Terminal) remained dark despite switching to the Light design mode by synchronizing `AppCompatDelegate`'s default night mode dynamically inside `MainActivity` using `LaunchedEffect`.
*   **Terminal Settings Scrollability**: Fixed a UI layout bug where settings were unreachable on smaller screens by wrapping the `TerminalSettingsScreen` Column in a scrollable container.
*   **Density-Independent Font Size scaling**: Fixed microscopic terminal text sizes by scaling the font size preference (10-30sp) into physical pixels using device `scaledDensity` display metrics before setting the view text size.
*   **Default session close behavior**: Swapped default terminal close action to `"new_session"`, automatically opening a new shell terminal instead of exiting the app when the last tab is closed.
*   **Modular decoupling**: Decoupled session termination callbacks from the core terminal backend view-client to isolate `:core:main` dependencies from `:app`, preventing compile circular references.
*   **Custom Terminal Color Schemes**: Added a new `terminal_colorscheme` setting and created five built-in properties-based themes: `default`, `dracula`, `solarized_dark`, `nord`, and `monokai`. Updated the terminal rendering views to apply these color properties dynamically.
