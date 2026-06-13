# MobileIDE Change Log / Status

This file tracks the features, bug fixes, and improvements implemented by AI coding assistants.

## [2026-06-13] Terminal UX, Permissions & Background FGS Enhancements

We implemented significant improvements to the terminal environment, onboarding permission flow, and background service execution:

### 1. Terminal Enhancements & Customizations (init.sh / .bashrc)
*   **Aesthetics**: Added a `clear` command right before the welcome message is displayed.
*   **Custom Branding**: Renamed MOTD (Message of the Day) text from "ReTerminal" to "MobileIDETerminal".
*   **Bash History Navigation**: Added prefix-based history scrolling using the up and down arrow keys via `/root/.inputrc` configuration (`history-search-backward` and `history-search-forward`).
*   **Colorized Output**: Enabled terminal color aliases for `ls`, `grep`, `egrep`, and `fgrep` inside `/root/.bashrc`, along with a green colorized command prompt.
*   **Autocompletion**: Integrated and applied `bash-completion` package during initialization.
*   **Android Environment Variables**: Exported `ANDROID_HOME`, `ANDROID_ROOT`, and `PROJECTS` environment variables globally.
*   **Android Toolchain Integration**: Added Android SDK, build-tools (33.0.2), platform-tools, platforms (android-33), cmake (3.22.1), and ndk (27.0.12077973) automatically to the setup and execution `PATH`.
*   **Cross-Compilation Toolchain**: Pre-configured `clang`, `llvm`, `make`, `gcc`, `g++`, `binutils`, and `musl-dev`/`libc-dev` in the environment to build arm, aarch64, and x86/x86-64 binaries.
*   **PaX/grsecurity MPROTECT Fix**: Automatically applies `paxctl -m` on all executable files inside `/opt/android-sdk` and JVM directories, bypassing Android's executable memory restrictions (`Failed to mark memory page as executable`). Used interpreted JVM mode `-Xint` as a secondary fallback.
*   **MOTD Upgrade Help**: Displayed helpful instructions inside the MOTD on how to upgrade SDK components using `sdkmanager` and system utilities using `apk`.

### 2. Onboarding & Welcome Screen Permissions
*   **Runtime Notification Permission**: Extended the onboarding Welcome screen with the `POST_NOTIFICATIONS` runtime permission card (API 33+) to allow showing foreground notification statuses.
*   **All Files Access**: Updated translations and description to clearly highlight requesting "Manage External Storage" (`MANAGE_EXTERNAL_STORAGE`) rather than generic storage permissions on Android 11+.

### 3. Foreground Service & WakeLock (`StatusService`)
*   **Background FGS**: Introduced `StatusService` (FGS type `specialUse`), which starts when the application runs and notification permission is granted.
*   **Persistent Notification**: Displays an ongoing notification containing the app name, active status, and WakeLock status.
*   **Action Buttons**: Includes a left action button "Exit" to gracefully terminate the application process, and a right action button to toggle the system "WakeLock" ("Acquire WakeLock" / "Release WakeLock") to prevent the CPU from entering deep sleep.

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
