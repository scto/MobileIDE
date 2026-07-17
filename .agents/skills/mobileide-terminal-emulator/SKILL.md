---
name: mobileide-terminal-emulator
description: MobileIDE terminal emulator, Termux PRoot sandbox execution, PTY sessions, terminal view UI, and environment initialization.
---

# MobileIDE Terminal Emulator Guide

## Overview
This skill covers the development and maintenance of the built-in terminal emulator and PRoot sandboxed environment inside MobileIDE, focusing on `core:terminal`, `core:terminal-emulator`, and `core:terminal-view`.

## Architecture
- **TerminalView**: The Android view component responsible for rendering terminal output, handling scrolling, scaling, and touch events.
- **Terminal Emulator**: Parses ANSI escape sequences and manages the virtual terminal screen buffer.
- **PTY Management**: Handles spawning local shells or PRoot instances and reading/writing to the pseudo-terminal.
- **PRoot Sandbox**: The isolated Linux environment (e.g., Ubuntu/Debian) where commands like `git`, `gradle`, and `clang` are executed.

## Best Practices
1. **Performance**: Terminal rendering must be highly optimized to handle fast, continuous output (like a build log) without freezing the UI.
2. **Environment Variables**: Properly setup `HOME`, `PATH`, and other environment variables when spawning a shell inside PRoot to ensure tools function correctly.
3. **Color Schemes**: Support custom properties-based color schemes and ensure fallback contrast is adequate.
4. **Lifecycle**: Ensure PTY sessions are cleanly closed and processes terminated when a terminal tab is closed or the app is destroyed to prevent zombie processes.
