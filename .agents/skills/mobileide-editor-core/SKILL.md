---
name: mobileide-editor-core
description: MobileIDE editor core components, Rosemoe editor integration, text rendering, syntax highlighting (Monarch/Tree-sitter), and UI themes.
---

# MobileIDE Editor Core Guide

## Overview
This skill covers the development and maintenance of the core text editor components within MobileIDE, primarily centered around the `editor` submodule.

## Architecture
- **Rosemoe Editor Engine**: The primary text rendering and editing component.
- **Syntax Highlighting**: Uses Tree-sitter for robust parsing and Monarch for regex-based fallbacks.
- **CodeEditScreen**: The main compose wrapper for the editor UI.

## Best Practices
1. **Performance**: Avoid heavy operations on the main thread when dealing with large text buffers.
2. **Keybindings**: Ensure hardware keyboard support works seamlessly with soft keyboard inputs.
3. **Themes**: Respect the global light/dark theme settings and terminal color schemes.
4. **Scrolling & Touch**: Handle nested scrolling properly within Compose layouts.
