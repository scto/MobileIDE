# Go Extension

This extension adds Gp and GoPls (LSP)

### Installation

Install the extension through the Xed-Editor's extension marketplace, and you're ready to go! Alternatively, you can download the latest release ZIP file and install it via **Settings > Extensions > Install from storage**.

After install extension install kotlin and lsp in **Settings > Editor > Language servers > Kotlin > Install**

Check installed:
```bash
go --help
gopls --help
```

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

Or use files `./compileDebug` or `./compileRelease`