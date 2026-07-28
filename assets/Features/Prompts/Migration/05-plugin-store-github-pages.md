Führe im Projekt ~/MobileIDE folgende Aufgabe durch. Dies ist PHASE 5 (letzte Phase) 
des Xed-Editor-Migrationsvorhabens. Quelle für Store-Konzept: ~/Xed-Editor.

VORAUSSETZUNG: Idealerweise sind Phasen 1-4 bereits abgeschlossen, für diese Phase 
jedoch nicht zwingend erforderlich, da sie funktional unabhängig ist (nutzt primär 
das bestehende :core:extension-Modul und das vorhandene Plugin-Manifest-Format 
unter plugins/*/manifest.json). Prüfe zu Beginn, dass plugins/*/manifest.json 
Dateien im Projekt vorhanden sind (aktuell 12 Plugins: java-lsp, json-lsp, 
kotlin-lsp, kotlin-kmp-lsp, lua-lsp, python-lsp, typst-lsp, go-lsp, rust-lsp, 
zig-lsp, fsharp-lsp, prettier-lsp).

Erstelle zu Beginn einen Sicherungs-Branch:
  git checkout -b backup/before-plugin-store && git checkout main

## Schritt 1: Analyse der Xed-Editor Store-Fähigkeiten
- Untersuche in ~/Xed-Editor, wie die Extension-/Plugin-Marketplace-Funktionalität 
  implementiert ist (vermutlich ein Screen, der eine Liste von manifest.json-
  basierten Erweiterungen von einer Remote-URL lädt, anzeigt, und "Install from 
  URL/ZIP" anbietet).
- Identifiziere die relevanten UI-Screens, ViewModels, und Netzwerk-/Repository-
  Klassen in Xed-Editor, die diese "Store"-Funktionalität abbilden.

## Schritt 2: Migration der Store-Funktionalität nach MobileIDE
- Migriere die identifizierten Store-Klassen (UI + Logik) nach MobileIDE, in ein 
  neues Modul ":features:plugin-store" (erstelle dieses Modul inkl. 
  build.gradle.kts, füge es in settings.gradle.kts hinzu).
- Wende die Migrationsregeln an: Package-Migration auf 
  "com.scto.mobile.ide.features.pluginstore", Textkorrektur aller 
  "xed"/"Xed"-Vorkommen zu "mobileide"/"MobileIDE".
- Passe die Datenmodelle so an, dass sie exakt zum bereits vorhandenen MobileIDE 
  Plugin-Manifest-Schema passen (siehe plugins/*/manifest.json Struktur: id, name, 
  mainClass, version, description, author{displayName,github}, license, tags, 
  minAppVersion, maxAppVersion, repository, hasSettings - Referenz-Schema z. B. 
  unter plugins/zig-lsp/schema/schema.json).

## Schritt 3: Integration in den MobileIDE SettingsScreen
- Öffne den bestehenden SettingsScreen (im Modul :app).
- Füge einen NEUEN, separaten Abschnitt/Kategorie-Eintrag "Plugin Store" / 
  "Erweiterungen" hinzu (analog zu bestehenden Kategorien wie "Terminal", 
  "Editor", "Build Config", "LSP Status").
- Dieser Abschnitt navigiert zum migrierten Plugin-Store-Screen aus 
  :features:plugin-store, der zukünftig verfügbare Plugins/Extensions von einer 
  konfigurierbaren Remote-URL (Standard-Platzhalter: GitHub Pages URL) abruft, 
  als Liste mit Icon/Name/Beschreibung/Version/Tags anzeigt, und einen 
  "Installieren"-Button bereitstellt, der die ZIP via die bestehende "Install 
  from storage/URL"-Logik der Extension-Engine (:core:extension) einbindet.
- Zeige zusätzlich die bereits mitgelieferten 12 Plugins optional als 
  "Bereits installiert / Bundled" Sektion im selben Screen.

## Schritt 4: Statischer Plugin-Store für GitHub Pages
- Erstelle im Repository-Root ein neues Verzeichnis "docs/" mit:
  - docs/index.html - statische Übersichtsseite (HTML/CSS), die alle verfügbaren 
    Plugins als Karten anzeigt (Name, Beschreibung, Version, Autor, Tags, 
    Download-Button verlinkt auf jeweilige ZIP-Release-URL).
  - docs/plugins.json - zentrales Manifest-Verzeichnis: JSON-Array mit einem 
    Eintrag pro Plugin (aus plugins/*/manifest.json zusammengestellt): id, name, 
    version, description, author, license, tags, repository, downloadUrl 
    (Platzhalter-Format: "https://github.com/<owner>/<repo>/releases/download/
    <tag>/<name>.zip"), iconUrl (Platzhalter auf plugins/*/icon.png).
  - docs/schema/plugins-index.schema.json - JSON-Schema zur Validierung von 
    plugins.json, analog zum bestehenden Einzelplugin-Schema.
  - scripts/generate_plugin_store.py - Skript (analog zu 
    scripts/package_all_plugins.py), das automatisiert durch alle 
    plugins/*/manifest.json-Dateien iteriert und daraus docs/plugins.json neu 
    generiert.
- Erstelle ".github/workflows/deploy-plugin-store.yml", die bei Änderungen an 
  plugins/**/manifest.json automatisch scripts/generate_plugin_store.py ausführt 
  und den docs/-Ordner via GitHub Pages Actions-Deployment veröffentlicht.
- Konfiguriere in :features:plugin-store die Standard-Remote-URL so, dass sie auf 
  "https://<github-username>.github.io/<repo-name>/plugins.json" zeigt 
  (als konfigurierbare Konstante).

## Schritt 5: Build- und Funktionsverifikation
- Führe "./gradlew :features:plugin-store:build" und den vollständigen 
  "./gradlew build" aus, behebe iterativ alle Fehler.
- Prüfe, dass der neue Settings-Abschnitt "Plugin Store" korrekt navigierbar ist 
  und eine Liste rendert, selbst wenn die Remote-URL zur Laufzeit (ohne echtes 
  GitHub Pages Deployment) noch keine Daten liefert (Leerzustand/Error-State 
  sauber behandeln).

## Abschlussbericht
Gib aus:
- Pfad zum neuen :features:plugin-store Modul
- Pfad zum SettingsScreen-Eintrag (Datei + Zeile)
- Pfad zu docs/plugins.json und docs/index.html
- Bestätigung: GitHub Actions Workflow erstellt
- Bestätigung: "./gradlew build" erfolgreich
- Commit erstellen: "feat(phase5): add plugin store module and GitHub Pages publishing"

Dies ist die letzte Phase des Migrationsvorhabens - gib abschließend eine 
Gesamtbestätigung aus, dass alle 5 Phasen (sofern vom Nutzer nacheinander 
ausgeführt) das Migrationsziel erfolgreich abgeschlossen haben.