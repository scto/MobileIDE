# MobileIDE Plugin Store Schema Specification

## Overview
MobileIDE supports a dynamic Plugin & Extension Store allowing users to discover, download, install, update, and remove Language Server Protocol (LSP) servers, themes, formatters, and developer tools without rebuilding the core app APK.

## Schema Definition (`plugins-catalog.json`)

The catalog file root structure:

```json
{
  "catalogVersion": 1,
  "lastUpdated": "2026-08-15T21:00:00Z",
  "plugins": [
    {
      "id": "lsp-rust",
      "name": "Rust Language Support",
      "version": "1.2.0",
      "category": "language",
      "description": "Rust language server (rust-analyzer) integration",
      "author": "KonerDev",
      "minAppVersion": "0.0.1",
      "arch": ["arm64-v8a", "armeabi-v7a", "x86_64"],
      "downloadUrl": "https://raw.githubusercontent.com/scto/MobileIDE-Plugins/main/plugins/lsp-rust-1.2.0.zip",
      "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "sizeBytes": 15420100,
      "entryScript": "lsp/rust.sh",
      "dependencies": ["runtime-node"],
      "fileExtensions": [".rs"],
      "icon": "icon.png"
    }
  ]
}
```

## Field Specification

| Field | Required | Type | Description |
|---|---|---|---|
| `id` | **Yes** | String | Unique plugin identifier (e.g. `lsp-rust`, `theme-dracula`). |
| `name` | **Yes** | String | Human-readable name displayed in the store UI. |
| `version` | **Yes** | String | Semantic version string (`X.Y.Z`). |
| `category` | **Yes** | String | Category type: `"language"`, `"theme"`, `"formatter"`, `"debugger"`, or `"tool"`. |
| `description` | No | String | Brief summary of the extension capabilities. |
| `author` | No | String | Author or maintainer name. |
| `minAppVersion` | No | String | Minimum supported MobileIDE app version. |
| `arch` | No | Array[String] | Supported CPU architectures (e.g. `["arm64-v8a", "x86_64"]`). |
| `downloadUrl` | **Yes** | String | Direct HTTPS or asset URL to download the plugin zip package. |
| `sha256` | No | String | SHA-256 checksum for binary integrity verification. |
| `sizeBytes` | No | Number | File size in bytes. |
| `entryScript` | No | String | Relative executable path inside container (e.g. `lsp/rust.sh`). |
| `dependencies` | No | Array[String] | Prerequisite runtimes (`"runtime-node"`, `"runtime-java"`, `"runtime-python"`). |
| `fileExtensions` | No | Array[String] | File extension bindings (e.g. `[".rs"]`). |
| `icon` | No | String | Relative path or URL to icon image. |
