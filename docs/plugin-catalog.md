# MobileIDE Plugin Catalog Documentation

This document lists all offline-installable LSP language servers and tool plugins available in MobileIDE.

## 1. Available Plugins Overview

| Plugin-ID | Name | Version | Category | File Extensions | SHA-256 (Short) |
|---|---|---|---|---|---|
| `com.koner.prettier` | Prettier | `1.0.0` | `LSP` | `` | `1458ced272...` |
| `com.koner.rust` | Rust LSP | `1.0.2` | `LSP` | `` | `80fd22b04e...` |
| `com.koner.typst` | Typst Support | `1.2.3` | `LSP` | `` | `441d569095...` |
| `com.scto.mobile.ide.bash_lsp` | Bash Language Support | `1.0.0` | `LSP` | `` | `4d2c57ddbe...` |
| `com.scto.mobile.ide.cpp_lsp` | C/C++ Language Support | `1.0.0` | `LSP` | `` | `68fa4c8c1e...` |
| `com.scto.mobile.ide.css_lsp` | CSS Language Support | `1.0.0` | `language` | `css, scss, less` | `7b6201c920...` |
| `com.scto.mobile.ide.emmet_lsp` | Emmet Language Support | `1.0.0` | `language` | `html, css, jsx, tsx` | `d1cf8ab9d2...` |
| `com.scto.mobile.ide.eslint_lsp` | ESLint Support | `1.0.0` | `language` | `js, jsx, ts, tsx` | `0863d1e48f...` |
| `com.scto.mobile.ide.html_lsp` | HTML Language Support | `1.0.0` | `language` | `html, htm` | `cbc46e7902...` |
| `com.scto.mobile.ide.java_lsp` | Java Language Support (JDTLS) | `1.0.0` | `LSP` | `` | `1f160cbde6...` |
| `com.scto.mobile.ide.json_lsp` | JSON Language Support | `1.0.0` | `LSP` | `` | `71c5c62698...` |
| `com.scto.mobile.ide.json_lsp` | Json LSP | `1.0.2` | `LSP` | `` | `826301eb34...` |
| `com.scto.mobile.ide.kotlin_lsp` | Kotlin JetBrains LSP | `0.2.1` | `LSP` | `` | `d90452ea6b...` |
| `com.scto.mobile.ide.kotlin_lsp` | Kotlin Language Support | `1.0.0` | `LSP` | `` | `6a57f7fb64...` |
| `com.scto.mobile.ide.markdown_lsp` | Markdown Language Support | `1.0.0` | `language` | `md, markdown` | `eb996da557...` |
| `com.scto.mobile.ide.plugin.fs` | F# Support | `0.1.0` | `LSP` | `` | `5c50b3ef41...` |
| `com.scto.mobile.ide.plugin.go` | Go Support | `0.2.0` | `LSP` | `` | `1f2873a9a7...` |
| `com.scto.mobile.ide.plugin.json` | JSON LSP | `1.0.2` | `LSP` | `` | `9ff1ff8f1a...` |
| `com.scto.mobile.ide.plugin.lua` | Lua-LSP | `1.0.2` | `LSP` | `` | `dc478015b8...` |
| `com.scto.mobile.ide.plugin.prettier` | Prettier | `1.0.0` | `LSP` | `` | `af384752e6...` |
| `com.scto.mobile.ide.plugin.python` | Python LSP | `1.0.3` | `LSP` | `` | `f918300735...` |
| `com.scto.mobile.ide.plugin.rust` | Rust LSP | `1.0.2` | `LSP` | `` | `e55e9e120d...` |
| `com.scto.mobile.ide.plugin.typst` | Typst Support | `1.2.3` | `LSP` | `` | `b3ff7cf76f...` |
| `com.scto.mobile.ide.plugin.zig` | Zig Support | `0.4.0` | `LSP` | `` | `5d4946334a...` |
| `com.scto.mobile.ide.plugins.java.lsp` | Java LSP | `1.1.0` | `LSP` | `` | `7290c507ae...` |
| `com.scto.mobile.ide.plugins.kotlin.kmplsp` | kmp-lsp | `0.1.2` | `LSP` | `` | `43caca3aff...` |
| `com.scto.mobile.ide.python_lsp` | Python Language Support | `1.0.0` | `LSP` | `` | `f744832996...` |
| `com.scto.mobile.ide.python_lsp` | Python LSP | `1.0.3` | `LSP` | `` | `4e8a3066e5...` |
| `com.scto.mobile.ide.toml_lsp` | TOML Language Support | `1.0.0` | `LSP` | `` | `dd92ccd0fe...` |
| `com.scto.mobile.ide.typescript_lsp` | TypeScript & JavaScript Language Support | `1.0.0` | `language` | `ts, tsx, js, jsx` | `df15998c59...` |
| `com.scto.mobile.ide.xml_lsp` | XML Language Support | `1.0.0` | `LSP` | `` | `b59a8ea5f5...` |
| `com.scto.mobile.ide.yaml_lsp` | YAML Language Support | `1.0.0` | `LSP` | `` | `c9473c14d8...` |
| `io.kiquar.plugin.fs` | F# Support | `0.1.0` | `LSP` | `` | `449812af1b...` |
| `io.kiquar.plugin.go` | Go Support | `0.2.0` | `LSP` | `` | `2f865e785a...` |
| `io.kiquar.plugin.zig` | Zig Support | `0.4.0` | `LSP` | `` | `cab2c1be28...` |

## 2. Manifest Schema Specification (`manifest.json`)
```json
{
  "id": "com.scto.mobile.ide.plugin.fs",
  "name": "F# Support",
  "version": "0.1.0",
  "category": "language",
  "description": "F# Language Server and Syntax Highlighting",
  "author": "com.scto.mobile.ide",
  "mainClass": "com.scto.mobile.ide.plugin.fs.Main",
  "fileExtensions": ["fs", "fsi", "fsx"]
}
```