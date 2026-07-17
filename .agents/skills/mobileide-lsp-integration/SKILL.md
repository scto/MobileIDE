---
name: mobileide-lsp-integration
description: MobileIDE Language Server Protocol (LSP) client configuration, server management, language extension implementations, and editor-lsp glue code.
---

# MobileIDE LSP Integration Guide

## Overview
This skill covers the integration of Language Servers into MobileIDE, focusing on the `core:lsp` and `editor-lsp` submodules.

## Architecture
- **LspClient**: Handles the standard LSP JSON-RPC communication.
- **Server Lifecycle**: Manages starting, stopping, and restarting background language server processes via PRoot or natively.
- **Editor-LSP**: Connects the `Rosemoe` editor engine to the LSP client for features like diagnostics, completions, and go-to-definition.

## Best Practices
1. **Asynchronous Communication**: All LSP requests should be non-blocking.
2. **Sandbox Environment**: Ensure LSP binaries are executed with correct permissions and environment variables inside the local PRoot container environment.
3. **Crash Recovery**: Implement robust error handling if the language server process dies unexpectedly.
