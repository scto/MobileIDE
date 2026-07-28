Kompiliere alle Extensions (Plugins) im aktuellen Projekt/Workspace.
Jedes Plugin besitzt eine manifest.json nach folgendem Schema:

{
  "id": "com.scto.mobile.ide.<plugin_name>",
  "name": "<Anzeigename>",
  "mainClass": "com.rk.extension.<package>.<ClassName>",
  "version": "x.y.z",
  "description": "<Beschreibung>",
  "author": { "displayName": "<Autor>" },
  "repository": "<Repo-URL>",
  "license": "<Lizenz>",
  "tags": ["<tag1>", "<tag2>"]
}

Aufgaben:

1. Durchsuche alle Unterordner nach manifest.json-Dateien, um alle
   verfügbaren Extensions/Plugins zu identifizieren.
2. Lies aus jeder manifest.json die Felder "id", "name" und "version" aus.
3. Baue jede Extension einzeln (Build-Skript im jeweiligen Plugin-Ordner
   ausführen, z. B. gradlew/npm/make je nach vorhandenem Build-System).
4. Verpacke das Build-Ergebnis:
   - Falls Android-Extension → als APK
   - Falls generisches/Non-Android-Plugin → als ZIP
     (inkl. der zugehörigen manifest.json im Archiv-Root)
5. Benenne die Ausgabedatei nach dem Schema:
   <id>-<version>.apk
   bzw.
   <id>-<version>.zip
   Beispiel: com.scto.mobile.ide.bash_lsp-1.0.0.zip
6. Verschiebe/Kopiere die fertige Datei in den Ordner:
   ~/MobileIDE/assets
   → Ordner inkl. übergeordneter Verzeichnisse anlegen, falls nicht vorhanden.
7. Überspringe Plugins mit fehlerhafter oder unvollständiger manifest.json
   (Pflichtfelder: id, name, mainClass, version) und liste sie im
   Abschlussbericht als "übersprungen" auf.

Erstelle zusätzlich eine ausführliche Entwickler-Anleitung als
Markdown-Dokument:

Dateiname: Plugins-Development.md
Zielordner: ~/MobileIDE/plugins
→ Ordner anlegen, falls nicht vorhanden.

Inhalt von Plugins-Development.md:

1. Titel: "MobileIDE Plugin/Extension Development Guide"
2. Einleitung: Zweck und Architektur des Plugin-Systems von MobileIDE
   (Bezug auf mainClass-basierte Extension-Registrierung, z. B.
   com.rk.extension.*)
3. Voraussetzungen (SDKs, Android-Tools, JDK-Version, Build-Tools)
4. Aufbau der manifest.json — Feld-für-Feld-Erklärung:
   - id (eindeutige Package-ID, umgekehrte Domain-Notation)
   - name (Anzeigename in der UI)
   - mainClass (Einstiegspunkt-Klasse, muss Extension-Interface implementieren)
   - version (SemVer x.y.z)
   - description
   - author.displayName
   - repository
   - license
   - tags (Kategorisierung, z. B. "lsp", "theme", "linter")
5. Projektstruktur einer typischen Extension:
   /plugin-root
     ├── manifest.json
     ├── src/.../<ClassName>.java (oder .kt)
     ├── build.gradle (oder entsprechendes Build-File)
     └── res/ (optional, Ressourcen)
6. Schritt-für-Schritt-Anleitung:
   a. Neues Plugin-Projekt anlegen
   b. manifest.json erstellen und Pflichtfelder ausfüllen
   c. mainClass implementieren (Extension-Interface/Basisklasse von
      com.rk.extension.* erweitern)
   d. Plugin lokal in MobileIDE testen (Sideloading via ~/MobileIDE/assets)
   e. Plugin bauen (konkrete Build-Befehle je Build-System)
   f. Ausgabe als APK oder ZIP verpacken (manifest.json muss im Archiv liegen)
   g. Plugin in ~/MobileIDE/assets ablegen und in MobileIDE registrieren/laden
7. Vollständiges Beispiel:
   - Beispiel-manifest.json für ein "Hello World"-Plugin
   - Beispiel-Klasse (mainClass-Implementierung) in Kotlin/Java
8. Best Practices:
   - Eindeutige, kollisionsfreie id-Vergabe
   - Semantic Versioning einhalten
   - Sinnvolle tags zur Auffindbarkeit
   - Lizenzangabe nicht vergessen
9. Troubleshooting häufiger Fehler (z. B. Plugin wird nicht erkannt,
   mainClass nicht gefunden, manifest.json ungültig)
10. Verweise auf Beispiel-Extension "Bash Language Support"
    (com.scto.mobile.ide.bash_lsp) als Referenzimplementierung

Nach Abschluss gib eine Zusammenfassung aus:
- Anzahl gefundener manifest.json-Dateien
- Anzahl erfolgreich kompilierter Extensions (mit id + Version)
- Liste aller erzeugten APK/ZIP-Dateien mit vollständigem Zielpfad
- Liste übersprungener/fehlgeschlagener Plugins mit Fehlerursache
- Bestätigung, dass Plugins-Development.md unter ~/MobileIDE/plugins erstellt wurde
```

**Kurze Anmerkung:** Ich habe angenommen, dass sich der `mainClass`-Pfad (`com.rk.extension.*`) direkt aus dem Beispiel ableiten lässt und alle Plugins ein gemeinsames Extension-Interface implementieren. Falls es noch weitere Pflichtfelder oder ein festes Build-Tool (z. B. immer Gradle) gibt, sag Bescheid – dann baue ich das noch konkreter mit exakten Befehlen ein.
