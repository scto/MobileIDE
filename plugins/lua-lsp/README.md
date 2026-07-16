# Lua-LSP Extension for Xed-Editor

`Lua-LSP` is a fast Lua Language Server (LSP) extension for **Xed-Editor (Karbon)**. It provides real-time diagnostics, autocompletion, formatting, and other IDE-like language features for Lua files.

This extension packages and runs [emmylua-analyzer-rust](https://github.com/UndeadScythes/emmylua-analyzer-rust)—a high-performance EmmyLua language server written in Rust—natively on Android.

---

## Features

- **Autocompletion & IntelliSense**: Snippets, variables, functions, and standard library completions.
- **Real-time Diagnostics**: Quick syntax checking using `emmylua_check`.
- **Code Formatting**: Built-in formatter powered by `luafmt`.
- **Native Performance**: Runs precompiled native binaries compiled for Android architectures with minimal overhead.
- **Multi-architecture Support**: Bundled with binaries for:
  - `arm64-v8a`
  - `armeabi-v7a`
  - `x86_64`

---

## How It Works

1. On load, the extension detects the system architecture (`Build.SUPPORTED_ABIS`).
2. It locates the corresponding native binary (`emmylua_ls`) in the extension's installation directory.
3. It registers the server with Xed-Editor's `LspRegistry` using the appropriate system linker (`linker` or `linker64`).

---

## Building the Extension

To build the extension, you need a standard Java/Android development environment.

### 1. Build commands

* **Debug Build**:
  ```bash
  ./compileDebug
  ```

* **Release Build**:
  ```bash
  ./compileRelease
  ```

### 2. Output Package

After a successful build, the packaged extension is created at:
```
output/Lua-LSP.zip
```

---

## Installation

1. Open **Xed-Editor** on your device.
2. Go to the **Extensions** configuration section.
3. Choose the option to install/load an extension from a file and select the compiled `Lua-LSP.zip`.

---

## Compatibility & Metadata

As configured in `manifest.json`:

- **Extension ID**: `com.rk.lua`
- **Supported App Versions**: Xed-Editor version `87` to `95`
- **Supported File Types**: `.lua`
- **Developer**: [RohitKushvaha01](https://github.com/RohitKushvaha01)
- **Repository**: [RohitKushvaha01/Lua-LSP](https://github.com/RohitKushvaha01/Lua-LSP)
