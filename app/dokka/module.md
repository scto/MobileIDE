# Module MobileIDE

MobileIDE is a modular Android IDE built with Kotlin,
Jetpack Compose, TextMate grammars and Sora Editor.

---

# Architecture

```mermaid
graph TD

    APP["App Module"]
    CORE["Core Modules"]
    UI["Compose UI"]
    EDITOR["Sora Editor"]
    TM["TextMate"]
    SIGNER["APK Signer"]
    WEB["Web Preview"]

    APP --> CORE
    APP --> UI
    UI --> EDITOR
    EDITOR --> TM
    APP --> SIGNER
    APP --> WEB
```

---

# Multi-Module Structure

```mermaid
graph LR

    ROOT["MobileIDE"]

    APP["app"]
    FILES["core:files"]
    PROJECTS["core:projects"]
    RES["core:resources"]
    UI["core:ui"]
    UTILS["core:utils"]
    SIGNER["signer"]
    WEBAPP["webapp"]

    ROOT --> APP
    ROOT --> FILES
    ROOT --> PROJECTS
    ROOT --> RES
    ROOT --> UI
    ROOT --> UTILS
    ROOT --> SIGNER
    ROOT --> WEBAPP
```

---

# Documentation Generation

Generate Markdown:

```bash
./gradlew dokkaGfmMultiModule
```

Generate HTML:

```bash
./gradlew dokkaHtmlMultiModule
```
