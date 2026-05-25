# WebIDE - KI-gestützter Web-Previewer für kollaborative Entwicklung

## 📖 Projektvorstellung

Erstellt mit Jetpack Compose. Das markanteste Merkmal dieses Projekts ist, dass es **vollständig von KI entwickelt** wurde, was das enorme Potenzial von KI in der Softwareentwicklung demonstriert.

## 🤖 KI-Entwicklung

Dieses Projekt ist das Ergebnis der Zusammenarbeit mehrerer KI-Modelle:

- **Claude**: Verantwortlich für die Erstellung des Begrüßungsbildschirms und des Theme-Systems
- **Gemini**: Entwickelte die Haupt-Benutzeroberfläche und die Dateibaum-Komponenten
- **DeepSeek**: Entwickelte zusammen mit Gemini die Kernfunktionen des Code-Editors

## 🛠️ Tech-Stack

- **Sprache**: Kotlin
- **UI-Framework**: Jetpack Compose
- **Zielplattform**: Android

## 📁 Projektstruktur

```text
app/src/main/java/com/web/webide/
├── core/           # Kern-Geschäftslogik
├── files/          # Dateiverwaltungsmodul
├── html/           # HTML-Verarbeitung
├── textmate/       # Unterstützung für Syntax-Hervorhebung
├── ui/             # Benutzeroberflächen-Schicht
│   ├── components/ # Wiederverwendbare Komponenten
│   ├── editor/     # Code-Editor
│   ├── preview/    # Echtzeit-Vorschau
│   ├── projects/   # Projektmanagement
│   ├── settings/   # Einstellungen
│   ├── theme/      # Theme-System
│   └── welcome/    # Begrüßungsbildschirm
├── App.kt          # Anwendungseinstiegspunkt
└── MainActivity.kt # Hauptaktivität (Main Activity)
