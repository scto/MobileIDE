### Prompt-Datei: `07-migrate-xed-editor-plugins.md`

# Ziel
Migriere alle in ~/Plugins vorhandenen Xed-Editor-Plugins auf die MobileIDE-Plugin-Struktur 
und Funktionalität, unter Beibehaltung aller Original-Features.

# Kontext
- MobileIDE hat bereits ein eigenes Extension-SDK (ExtensionAPI, ExtensionContext, 
  CommandProvider, LspRegistry, RunnerManager, FileTypeManager) sowie ein eigenes 
  manifest.json-Schema (siehe plugins/typst-lsp/schema/schema.json).
- Referenzimplementierungen liegen bereits vor: plugins/typst-lsp und plugins/python-lsp 
  sind BEREITS von Xed-Editor migrierte Plugins – nutze deren Struktur 
  (build.gradle.kts mit downloadLatestJar/createFinalZip/copyPluginToAssets-Tasks, 
  manifest.json, Main.kt als ExtensionAPI-Einstiegspunkt) als Vorlage für jedes weitere Plugin.

# Anforderungen
1. Für jedes Plugin in ~/Plugins:
   a) Analysiere die Original-Xed-Editor-Plugin-Struktur (welche Xed-Editor-SDK-Klassen 
      werden importiert: Commands, Runners, LSP-Server, FileTypes, Settings-UI?).
   b) Erstelle ein neues Verzeichnis plugins/<plugin-name> mit vollständigem Gradle-Subprojekt-
      Gerüst (build.gradle.kts, settings.gradle.kts, gradle/libs.versions.toml, gradlew, 
      manifest.json, schema/schema.json, compileDebug/compileRelease-Skripte) – 1:1 kopiert 
      aus dem Muster von plugins/typst-lsp, mit angepassten Metadaten (id, name, mainClass, 
      author, repository laut Original-Plugin-Attribution).
   c) Portiere den Kotlin/Java-Code:
      - Ersetze alle Xed-Editor-SDK-Package-Imports 1:1 durch die äquivalenten MobileIDE-SDK-
        Packages (z. B. `com.rk.xededitor.*` → `com.scto.mobile.ide.*`, exakte Mapping-Tabelle 
        vorab erstellen basierend auf einem Diff zwischen einem Xed-Editor-Original-Plugin 
        und dessen bereits migrierter MobileIDE-Version, falls ein Pendant existiert).
      - Falls eine Funktion im MobileIDE-SDK fehlt, die das Original-Plugin benötigt 
        (z. B. spezifische UI-Hooks), dokumentiere dies explizit als "SDK-Gap" in einer 
        MIGRATION_NOTES.md im Plugin-Ordner, statt die Funktionalität stillschweigend 
        wegzulassen.
   d) Übernimm README.md/CHANGELOG.md/LICENSE des Original-Plugins mit einem Hinweis-Absatz 
      analog zu plugins/typst-lsp/README.md ("Hinweis zur Herkunft: ... übernommen von ...").
   e) Registriere das neue Modul in der Root-settings.gradle.kts: `include(":plugins:<name>")`.

2. Erstelle nach Migration jedes Plugins einen Kompilier-Test:
   - `sh ./compileDebug` im jeweiligen Plugin-Verzeichnis muss ohne Fehler eine .zip 
     nach plugins/<name>/output/ erzeugen.

3. Erstelle abschließend eine Übersichtstabelle (MIGRATION_STATUS.md im Projekt-Root) mit 
   Spalten: Plugin-Name | Xed-Editor-Original-Repo | Migrationsstatus (Vollständig/Teilweise/
   SDK-Gap) | Offene Punkte.

# Akzeptanzkriterien
- Jedes migrierte Plugin lässt sich über die bestehende Plugin-ZIP-Installationsroute 
  ("Settings > Extensions > Install from storage") in MobileIDE laden und aktivieren.
- Kernfunktionalität jedes Plugins (z. B. Syntax-Highlighting, LSP-Verbindung, Commands, 
  Runner) funktioniert nach Migration nachweislich identisch zum Originalverhalten in Xed-Editor.