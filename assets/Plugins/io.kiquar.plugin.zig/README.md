# Zig Extension

This extension adds **[Zig language](https://ziglang.org/)** and ZLS (Zig Language server)

## Installation

Install the extension through the Xed-Editor's extension marketplace, and you're ready to go! 

Alternatively, you can download the latest release ZIP file and install it via `Settings > Extensions > Install from storage`.

After install extension install zig and zls in Settings > Editor > Language servers > Zig > Install

![example](https://github.com/KiquarSL/xed-zig/blob/master/screenshots/main.png)

## Usage

**Create folder for project and go there**
```bash
mkdir test-project
cd test-project
```

**Init project**
```bash
zig init
```

**Build and Run project**:
```bash
zig build run
```


## Build plugin

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