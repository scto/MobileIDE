# MobileIDE – KI-kollaborative Android-IDE

## 📖 Projekteinführung

MobileIDE ist eine vollständig auf dem Gerät laufende Android-IDE, geschrieben in Kotlin und Jetpack Compose. Das Projekt kombiniert moderne Android-Entwicklungswerkzeuge, syntaxbewusste Bearbeitung, TextMate-Grammatiken, Unterstützung für APK-Signierung, Projektmanagement und integrierte Web-Vorschau.

Die IDE ist so konzipiert, dass sie vollständig auf Android-Geräten ausgeführt werden kann, ohne dass externe Desktop-Tools erforderlich sind.

## 🤖 KI-Entwicklung

Dieses Projekt wurde gemeinschaftlich unter Verwendung mehrerer KI-Systeme entwickelt:

 * **Claude**: Architekturverfeinerung, Themendesign, Modularisierung
 * **Gemini**: UI-Ebene, Projektmanagement-Abläufe, Editor-Integrationen
 * **DeepSeek**: Kern-Editor-Funktionen und unterstützende Infrastruktur
 * 
## 🛠️ Technologie-Stack

 * **Sprache**: Kotlin + Java
 * **UI-Framework**: Jetpack Compose
 * **Architektur**: Modulare Android-Architektur
 * **Editor-Engine**: Sora Editor
 * **Syntax-Hervorhebung**: TextMate-Grammatiken
 * **Build-System**: Gradle Kotlin DSL
 * **APK-Signierung**: Eingebetteter Android APK Signer
 * **Zielplattform**: Android
 * 
# 📁 Projektstruktur

```text
MobileIDE
├── app/                    # Haupt-Android-Anwendungsmodul
│   ├── ui/                 # Bildschirme und UI-Abläufe
│   ├── html/               # HTML-Analyse und Autovervollständigung
│   ├── textmate/           # TextMate-Sprachintegration
│   └── dokka/              # Dokka-Dokumentation
│
├── core/
│   ├── files/              # Dateiabstraktion und ZIP-Dienstprogramme
│   ├── projects/           # Projektvorlagen und Workspace-Verwaltung
│   ├── resources/          # Gemeinsame Android-Ressourcen
│   ├── ui/                 # Gemeinsame Compose-UI-Komponenten und Themes
│   └── utils/              # Logging, Berechtigungen, Workspace-Tools
│
├── signer/                 # Implementierung der eingebetteten APK-Signierung
│   ├── apksig/             # Implementierung des Android APK Signature Scheme
│   └── mcal/               # Kotlin-Signierungs-Tools und Wrapper
│
├── webapp/                 # Leichtgewichtige Android Web-IDE-Vorschau-App
│
├── assets/
│   └── MobileIDE_Icons/    # App-Icons und Branding-Assets
│
├── gradle/                 # Gradle-Wrapper und Versionskatalog
└── settings.gradle.kts     # Modulkonfiguration
```

# 🧩 Modulübersicht

| Modul | Beschreibung |
| :--- | :--- |
| :app | Hauptanwendung der MobileIDE |
| :core:files | Dateibaum, ZIP-Unterstützung, Dateiabstraktionen |
| :core:projects | Projektvorlagen und Workspace-Hilfsprogramme |
| :core:resources | Gemeinsam genutzte Android-Ressourcen |
| :core:ui | Gemeinsam genutzte Compose-Komponenten und Theming |
| :core:utils | Logging, Berechtigungen, Einstellungen |
| :signer | APK-Signierung und Keystore-Verwaltung |
| :webapp | Android-basierte Web-Vorschau-Laufzeitumgebung |

# Dokka v2 + Mermaid-Konfiguration

Enthalten sind:

 * Dokka v2-Konfiguration
 * Mermaid-Architekturdiagramme
 * Dokumentations-Setup für mehrere Module
 * Unterstützung für MkDocs Mermaid
 * Unterstützung für Markdown + HTML-Dokumentation