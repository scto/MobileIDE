# JSON LSP Extension for Xed-Editor (Karbon)

This extension provides high-performance, fully featured JSON editing experience on Xed-Editor

---

## Features

This extension integrates [light-json-lsp](light-json-lsp/README.md) to provide rich editor features:

* **High Performance**: Built on top of Tree-sitter for fast, incremental parsing and synchronous LSP transport.
* **JSON Schema Support**: Full support for JSON Schema drafts 4 through 2020-12.
* **Diagnostics**: Live syntax error checking, duplicate key detection, and JSONC (comments and trailing commas) tolerance.
* **Editor Integrations**:
  * **Code Completion**: Context-aware completions derived from JSON schemas.
  * **Hover Information**: Displays schema descriptions and documentation.
  * **Document Symbols**: Hierarchy view of the JSON structure.
  * **Formatting & Sorting**: CST-based code formatting and key sorting.
  * **Document Links**: Support for `$ref` resolution and clickable URLs.
  * **Color Highlights**: Hex color detection and presentation.
  * **Folding & Selection Ranges**: Precise structure-aware text selections and code folding.

---

## Project Structure

* **`app/`**: The Android project wrapper (written in Kotlin) that interfaces with Xed-Editor's Extension SDK.
  * **`com.rk.xededitor.json.Main`**: Entry point that detects system ABI and loads/registers the LSP server.
  * **`com.rk.xededitor.json.JsonServer`**: Manages the life cycle of the LSP process and handles path resolutions.
* **`light-json-lsp/`**: The Core LSP server written in Rust.
* **`bin/`**: Precompiled target-specific binaries for the Rust LSP.
  * `bin/arm64-v8a/light-json-lsp`
  * `bin/armeabi-v7a/light-json-lsp`
  * `bin/x86_64/light-json-lsp`
* **`manifest.json`**: Describes the extension details (ID, name, main class, version) for Xed-Editor.

---

## Getting Started

### 1. Requirements

* **Android SDK / NDK** (for building the Android wrapper)
* **Java JDK 21**
* **Rust Toolchain** (with `cargo-ndk` if compiling the LSP binary from source)

---

#### Build in Debug Mode:
```bash
./compileDebug
```

#### Build in Release Mode:
```bash
./compileRelease
```

The resulting zip package is outputted to:
```
output/Json LSP.zip
```

This project is licensed under the [MIT License](light-json-lsp/LICENSE).
