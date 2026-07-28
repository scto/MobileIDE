# Xed-Editor Plugin Migration Status (`MIGRATION_STATUS.md`)

| Plugin-Name | Xed-Editor-Original-Repo | Migrationsstatus | Offene Punkte |
|---|---|---|---|
| `java-lsp` | `xed-java-lsp` | Vollständig | Keine |
| `json-lsp` | `JSON-LSP` | Vollständig | Keine |
| `kotlin-lsp` | `xed-kotlin` | Vollständig | Keine |
| `kotlin-kmp-lsp` | `xed-kmp-lsp` | Vollständig | Keine |
| `lua-lsp` | `Lua-LSP` | Vollständig | Keine |
| `python-lsp` | `Python-LSP` | Vollständig | Keine |
| `typst-lsp` | `xed-typst` | Vollständig | Keine |
| `go-lsp` | `xed-go` | Vollständig | Keine |
| `rust-lsp` | `xed-rust` | Vollständig | Keine |
| `zig-lsp` | `xed-zig` | Vollständig | Keine |
| `fsharp-lsp` | `xed-fsharp` | Vollständig | Keine |
| `prettier-lsp` | `xed-prettier` | Vollständig | Keine |

## Extension Module Restructuring (Prompt 21)

| Schritt | Beschreibung | Zielpfad | Status |
|---|---|---|---|
| **Schritt 1** | Modul `:core:extension` verschieben & refaktorieren | `~/MobileIDE/features/extensions` | Vollständig |
| **Schritt 2** | `Xed-Editor/features/extensions` Code & Event-Klassen zusammenführen | `~/MobileIDE/features/extensions` | Vollständig |
| **Schritt 3** | `Xed-Editor/core/main/.../com/rk/extension` Basis-APIs migrieren & Package `com.rk.extension` -> `com.scto.mobile.ide.features.extensions` aktualisieren | `~/MobileIDE/features/extensions` | Vollständig |

