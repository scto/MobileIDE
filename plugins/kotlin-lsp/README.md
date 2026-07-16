# Kotlin Extension

This extension adds Kotlin and LSP by JetBrains

**WARNING**: It unstable extension. LSP can using a lot of memory and can slow working!

### Installation

Install the extension through the MobileIDE's extension marketplace, and you're ready to go! Alternatively, you can download the latest release ZIP file and install it via Settings > Extensions > Install from storage.

Check installed:
```bash
kotlin --help
intellij-server --help
```

## Usage

More likely lsp from extension no working, use instruction below for using lsp.

1. Go in **Settins > Editor > LSP manager > Kotlin** (intellij-server) and disable it.
2. Add external lsp with button in down. Address: `localhost`, Port: `8081`, extension: `.kt`
3. Before start lsp run command in terminal and wait when lsp will done: `intellij-server --socket localhost:8081`
4. Open gradle/maven project and open your Kotlin files.

## Using for build plugins

If you write plugin for MobileIDE with it extension, more likely lsp not started bacause task "createFinalZip". For fix you can move content to `doFirst`, but then extension cannot build. If you know how fix it, please do pull request.

## Build

Debug build:
```bash
./gradlew assembleDebug
./gradlew :app:createFinalZip
```

Release build:
```bash
./gradlew assembleRelease
./gradlew :app:createFinalZip
```