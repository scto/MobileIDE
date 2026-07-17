---
name: mobileide-extensions
description: MobileIDE extension system, language server extensions (extension-languages), dynamic command registration, and UI integration for extensions.
---

# MobileIDE Extensions Guide

## Overview
This skill covers the development of internal and external extensions for MobileIDE, typically found in `core:extension` and `extension-languages`.

## Architecture
- **Extension API**: The core interfaces for registering extensions, adding commands, and interacting with the IDE environment.
- **Language Extensions**: Specifically, the packaging of Java, Kotlin, Bash, XML, and other language servers into extensions.
- **CommandManager**: Handles the registration and execution of custom commands from extensions.

## Best Practices
1. **Isolation**: Extensions should interact with the IDE strictly through the defined Extension APIs.
2. **Lifecycle**: Ensure extensions correctly handle initialization, suspension, and termination events.
3. **Lazy Loading**: Avoid heavy initialization on startup to keep IDE boot times fast.
