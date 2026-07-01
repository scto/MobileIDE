# MobileIDE ![Stone Badge](https://stone.professorlee.work/api/stone/scto/MobileIDE)
![Version](https://img.shields.io/badge/version-0.3.2-blue?style=flat-square)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue?style=flat-square)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-green?style=flat-square)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-GPLv3-orange?style=flat-square)](LICENSE)


[ [**English**] | [German](README_DE.md) | [Project Status](status.md) ]

MobileIDE is a native Android Integrated Development Environment (IDE) for app development. Built entirely with Jetpack Compose, this project implements a complete workflow ranging from code editing to building APKs directly on your mobile device.

This is an experimental engineering project; its core architecture and code logic were collaboratively completed by multiple AI models (Claude, Gemini, DeepSeek).

## Screenshots

<div align="center">
  <img src="https://github.com/user-attachments/assets/2eac6ea4-25a1-4a02-b814-2925ffb2092e" width="45%" />
  <img src="https://github.com/user-attachments/assets/7999b42a-af56-4aea-b705-920e7e168844" width="45%" />
</div>

## Project Structure

The project has been refactored into a highly modular system consisting of the following key submodules:

*   `:app` - Main application entry point (UI screens, onboarding, welcome logic, project selection, setting screens, template extraction logic).
*   `:editor` - Code editor logic (based on sora-editor integration, handles open file tabs, and editor actions).
*   `:editor-lsp` - LSP (Language Server Protocol) integration and support for the editor.
*   `:language-treesitter` - Syntax highlighting and semantic parsing engine using TreeSitter for Java, Kotlin, XML, Log, and C++.
*   `:core:main` - Central core IDE module (handles main navigation, Terminal session view backend, theme configurations).
*   `:core:components` - Common UI widgets, Jetpack Compose preference components, and bottom sheet widgets.
*   `:core:resources` - Universal resources (icons, string translations, drawable assets).
*   `:core:terminal-emulator` - Terminal parser, ANSI escape code interpreter, PTY process launcher/runner.
*   `:core:terminal-view` - Core Android View widget rendering the terminal session matrix and capturing hardware key events.
*   `:core:apk-builder` - Custom APK compilation toolset (AAPT2 compiler, DX/D8 compilers, signing, zipalign, and packaging).
*   `:core:tooling:tooling-api` - Logging framework interfaces and Gradle task definition objects.
*   `:core:tooling:tooling-impl` - Categorized real-time logging panel (Terminal, Problems, IDE, Build, LSP logs) and Gradle tasks panel with checkbox list UI.

**Key Assets (`app/src/main/assets/`)**:
*   `textmate/`: TextMate grammars and configurations for syntax highlighting fallback.
*   `queries/`: TreeSitter query definitions.
*   `terminal/`: Embedded terminal setup files (`ideenv`, `idesetup`, `init.sh`, `setup.sh`), as well as built-in color schemes under `terminal/colorschemes/`.


## Features

*   **Syntax Highlighting**: Dual-engine architecture supporting both **TextMate** (providing robust styling for HTML, CSS, JavaScript, JSON, etc.) and **TreeSitter** (offering high-performance semantic parsing for Kotlin, Java, CPP, JSON, Log, and XML).
*   **Editor Engine Selection**: Preferences entry allowing users to switch between the classic TextMate engine and the TreeSitter (LSP) engine on the fly, with automatic fail-safe fallback.
*   **Optional Logging**: Built-in LogCatcher subsystem with a toggle in settings to enable or disable verbose debugging logs for compiler and editor operations.
*   **Project Management**: Full file system access permissions, supporting the creation and management of multi-file Web projects.
*   **Real-time Preview**: Integrated WebView preview environment supporting JavaScript interaction testing.
*   **Modern UI**: Written 100% in Kotlin and Jetpack Compose, supporting dynamic themes.
*   **Git Integration**: Built-in Git version control with a visual commit history graph, supporting Clone, Commit, Push, Pull, and Branch management. Automatically ignores sensitive files and build artifacts.

## Discussion

* QQ Group: [1050254184](https://qm.qq.com/q/tFXuqMQDlK)
* TG Group: [Android_For_MobileIDE](https://t.me/Android_For_MobileIDE)

## Contributors

<a href="https://github.com/scto/MobileIDE/graphs/contributors">
   <img src="https://contributors-img.web.app/image?repo=scto/MobileIDE" />
</a>

## License

```
MobileIDE - A powerful IDE for Android app development.
Copyright (C) 2025  scto  <tschmid35@.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```


[![Star History Chart](https://api.star-history.com/svg?repos=scto/MobileIDE&type=Date)](https://star-history.com/#scto/MobileIDE&Date)
