# MobileIDE - AI-Collaborated Android IDE

## 📖 Project Introduction

MobileIDE is a fully on-device Android IDE written in Kotlin and Jetpack Compose.
The project combines modern Android development tooling, syntax-aware editing,
TextMate grammars, APK signing support, project management, and integrated web previewing.

The IDE is designed to run completely on Android devices without external desktop tooling.

## 🤖 AI Development

This project was collaboratively developed using multiple AI systems:

- **Claude**: Architecture refinement, theme system, modularization
- **Gemini**: UI layer, project management flows, editor integrations
- **DeepSeek**: Core editor features and supporting infrastructure

## 🛠️ Tech Stack

- **Language**: Kotlin + Java
- **UI Framework**: Jetpack Compose
- **Architecture**: Modular Android Architecture
- **Editor Engine**: Sora Editor
- **Syntax Highlighting**: TextMate Grammars
- **Build System**: Gradle Kotlin DSL
- **APK Signing**: Embedded Android APK Signer
- **Target Platform**: Android

---

# 📁 Project Structure

```text
MobileIDE
├── app/                    # Main Android application module
│   ├── ui/                 # Screens and UI flows
│   ├── html/               # HTML analysis and autocomplete
│   ├── textmate/           # TextMate language integration
│   └── dokka/              # Dokka documentation
│
├── core/
│   ├── files/              # File abstraction and ZIP utilities
│   ├── projects/           # Project templates and workspace handling
│   ├── resources/          # Shared Android resources
│   ├── ui/                 # Shared Compose UI components and themes
│   └── utils/              # Logging, permissions, workspace utilities
│
├── signer/                 # Embedded APK signer implementation
│   ├── apksig/             # Android APK Signature Scheme implementation
│   └── mcal/               # Kotlin signing utilities and wrappers
│
├── webapp/                 # Lightweight Android Web IDE preview app
│
├── assets/
│   └── MobileIDE_Icons/    # Launcher icons and branding assets
│
├── gradle/                 # Gradle wrapper and version catalog
└── settings.gradle.kts     # Module configuration
```

---

# 🧩 Modules Overview

| Module | Description |
|---|---|
| `:app` | Main MobileIDE application |
| `:core:files` | File tree, ZIP support, file abstractions |
| `:core:projects` | Project templates and workspace helpers |
| `:core:resources` | Shared Android resources |
| `:core:ui` | Shared Compose components and theming |
| `:core:utils` | Logging, permissions, preferences |
| `:signer` | APK signing and keystore management |
| `:webapp` | Android-based web preview runtime |

# Dokka v2 + Mermaid Configuration

Included:

- Dokka v2 configuration
- Mermaid architecture diagrams
- Multi-module documentation setup
- MkDocs Mermaid support
- Markdown + HTML documentation support
