# kmp-lsp Extension

Adds Kotlin and Java LSP support using [kmp-lsp](https://github.com/Hessesian/kmp-lsp) — fast, low-memory LSP server written in Rust (~10MB).

## Installation

Install via Xed-Editor's extension marketplace or from a ZIP file (Settings > Extensions > Install from storage).

After installation, go to **Settings > Editor > Language servers > Kotlin > Install** to set up the LSP.

Verify:
```bash
kmp-lsp --help
```

## Usage

Basic LSP works immediately

```bash
# Index the project
kmp-lsp index --root . --verbose

# Extract library sources (Gradle projects)
kmp-lsp extract-sources

# For local JARs (app/libs/*.jar), create workspace.json:
# { "jarPaths": ["<WORKSPACE>/app/libs"] }

# Verify sources
kmp-lsp sources --root . --json
```

CLI commands:

```bash
kmp-lsp find MyClass
kmp-lsp refs MyClass
kmp-lsp hover src/Foo.kt 42 10
```

Build

```bash
./gradlew assembleDebug && ./gradlew :app:createFinalZip
# or release
./gradlew assembleRelease && ./gradlew :app:createFinalZip
```