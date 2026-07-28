# MobileIDE Plugin/Extension Development Guide

## 1. Einleitung
MobileIDE bietet ein erweiterbares Plugin-System, mit dem Entwickler neue Sprach-Server (LSP), Syntax-Highlighter, Runner, Tools und UI-Erweiterungen hinzufügen können. Plugins werden dynamisch geladen und nutzen eine zentral definierte `mainClass`, welche die Lebenszyklus-Methoden der Erweiterung implementiert (basierend auf den Schnittstellen unter `com.scto.mobile.ide.extension.*` bzw. `com.rk.extension.*`).

---

## 2. Voraussetzungen
Um Plugins für MobileIDE zu entwickeln, werden folgende Werkzeuge benötigt:
- **JDK 17+** (oder JDK 21)
- **Android SDK & Build-Tools** (API 34/35)
- **Gradle 8.x+**
- **MobileIDE SDK / Core Common Libraries** (`sdk.jar` oder Projekt-Abhängigkeiten)

---

## 3. Aufbau der `manifest.json`

Jedes Plugin **muss** im Hauptverzeichnis eine `manifest.json` enthalten. Die Felder gliedern sich wie folgt:

```json
{
  "id": "com.scto.mobile.ide.example_plugin",
  "name": "Example Plugin",
  "mainClass": "com.scto.mobile.ide.plugins.example.Main",
  "version": "1.0.0",
  "description": "Ein Beispiel-Plugin für MobileIDE",
  "author": {
    "displayName": "Entwickler Name",
    "github": "username"
  },
  "repository": "https://github.com/example/example-plugin",
  "license": "MIT",
  "tags": ["lsp", "example", "kotlin"],
  "minAppVersion": 1,
  "maxAppVersion": null
}
```

### Feld-Erklärung:
| Feld | Pflicht | Beschreibung |
|---|---|---|
| `id` | **Ja** | Eindeutige Paket-ID in umgekehrter Domain-Notation (z. B. `com.scto.mobile.ide.bash_lsp`). |
| `name` | **Ja** | Der in der MobileIDE UI angezeigte Name des Plugins. |
| `mainClass` | **Ja** | Vollständig qualifizierter Klassenname des Plugin-Einstiegspunkts. |
| `version` | **Ja** | Versionsnummer nach Semantic Versioning (`x.y.z`). |
| `description` | Nein | Kurze Beschreibung der Plugin-Funktionalität. |
| `author.displayName` | Nein | Name des Autors/Entwicklers. |
| `repository` | Nein | URL zum Quellcode-Repository. |
| `license` | Nein | Lizenzbezeichnung (z. B. `MIT`, `GPLv3`, `Apache-2.0`). |
| `tags` | Nein | Array von Stichwörtern zur Kategorisierung (z. B. `["lsp", "linter"]`). |

> [!IMPORTANT]
> **Pflichtfelder**: Fehlt eines der Felder `id`, `name`, `mainClass` oder `version` oder ist die `manifest.json` kein gültiges JSON, wird das Plugin beim Laden übersprungen.

---

## 4. Projektstruktur einer typischen Extension

```
/plugin-root
  ├── manifest.json
  ├── icon.png (optional)
  ├── build.gradle.kts (oder build.gradle)
  ├── settings.gradle.kts
  └── app/
      ├── src/main/java/com/scto/mobile/ide/plugins/example/
      │   ├── Main.kt
      │   └── ExampleServer.kt
      └── src/main/res/ (optional)
```

---

## 5. Schritt-für-Schritt-Anleitung

### Schritt 1: Plugin-Projekt anlegen
Erstelle einen neuen Ordner für dein Plugin (z. B. `plugins/my-plugin`).

### Schritt 2: `manifest.json` erstellen
Erstelle im Stammverzeichnis des Plugins die Datei `manifest.json` mit allen Pflichtfeldern (`id`, `name`, `mainClass`, `version`).

### Schritt 3: `mainClass` implementieren
Implementiere den Einstiegspunkt des Plugins. Die Klasse muss von MobileIDE aufrufbar sein:

```kotlin
package com.scto.mobile.ide.plugins.example

class Main {
    fun onEnable() {
        println("Beispiel-Plugin erfolgreich aktiviert!")
    }

    fun onDisable() {
        println("Beispiel-Plugin deaktiviert.")
    }
}
```

### Schritt 4: Plugin verpacken
Das Plugin kann als **ZIP** (für universelle/non-Android Plugins) oder als **APK** (für kompilierte Android-Erweiterungen) verpackt werden.

**Wichtig**: Die `manifest.json` **muss** im Stammverzeichnis des ZIP-Archivs bzw. im Asset-Ordner der APK liegen.

Namenskonvention für das Paket:
- `<id>-<version>.zip` (z. B. `com.scto.mobile.ide.bash_lsp-1.0.0.zip`)
- `<id>-<version>.apk` (z. B. `com.scto.mobile.ide.example-1.0.0.apk`)

### Schritt 5: Ablegen in `assets` & Sideloading
Kopiere/verschiebe das fertige Archiv in das `assets`-Verzeichnis von MobileIDE:
`~/MobileIDE/assets/<id>-<version>.zip`

MobileIDE erkennt das Paket beim Anwendungsstart automatisch und registriert die Extension.

---

## 6. Vollständiges Beispiel: Hello World Plugin

### `manifest.json`
```json
{
  "id": "com.scto.mobile.ide.helloworld",
  "name": "Hello World Extension",
  "mainClass": "com.scto.mobile.ide.plugins.helloworld.Main",
  "version": "1.0.0",
  "description": "Demonstriert die grundlegende Struktur einer MobileIDE Erweiterung.",
  "author": {
    "displayName": "MobileIDE Team"
  },
  "license": "MIT",
  "tags": ["example", "hello-world"]
}
```

### `Main.kt`
```kotlin
package com.scto.mobile.ide.plugins.helloworld

class Main {
    fun init() {
        // Plugin-Initialisierungslogik hier ausführen
        System.out.println("Hello World Extension loaded successfully!")
    }
}
```

---

## 7. Best Practices

- **Eindeutige ID-Vergabe**: Nutze stets deine eigene Domain in umgekehrter Notation (z. B. `com.meinedomain.plugin.feature`), um Namenskonflikte zu vermeiden.
- **Semantic Versioning**: Halte dich an `MAJOR.MINOR.PATCH` (z. B. `1.0.2`), damit MobileIDE Updates korrekt vergleichen kann.
- **Validierung**: Prüfe deine `manifest.json` stets mit einem JSON-Linter auf Syntaxfehler (keine Zeilenumbrüche in Strings, keine fehlenden Kommas).
- **Ressourcen**: Halte die Archivgröße klein und binde externe Binärdateien über Download-Skripte ein.

---

## 8. Troubleshooting & Häufige Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| **Plugin wird nicht geladen** | `manifest.json` fehlt oder hat ungültiges JSON-Format. | `manifest.json` im JSON-Validator prüfen (keine Steuerzeichen, Anführungszeichen prüfen). |
| **`ClassNotFoundException`** | Die in `mainClass` angegebene Klasse existiert im Paket nicht. | Den vollständigen Paketpfad in `mainClass` mit dem Quellcode abgleichen. |
| **`Missing Required Fields`** | Mindestens eines der Felder `id`, `name`, `mainClass` oder `version` fehlt. | Alle 4 Pflichtfelder in `manifest.json` eintragen. |

---

## 9. Referenzimplementierung
Als vollständiges Praxisbeispiel dient das Plugin **Bash Language Support**:
- **ID**: `com.scto.mobile.ide.bash_lsp`
- **Pfad**: `assets/bundled_plugins/com.scto.mobile.ide.bash_lsp`
