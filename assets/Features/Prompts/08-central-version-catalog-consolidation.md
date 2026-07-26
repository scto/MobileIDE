### Prompt-Datei: `08-central-version-catalog-consolidation.md`

# Ziel
Konsolidiere alle verstreuten gradle/libs.versions.toml-Dateien (Root-Projekt + jedes 
Plugin unter plugins/*) zu einem einzigen, zentral gepflegten Versionskatalog, um 
Versions-Drift zwischen AGP/Kotlin/R8/Compose-Versionen wie bereits am 17.07. aufgetreten 
dauerhaft zu verhindern.

# Kontext
- Jedes Plugin (plugins/typst-lsp, plugins/python-lsp, plugins/java-lsp, plugins/json-lsp, 
  plugins/kotlin-lsp, plugins/kotlin-kmp-lsp, plugins/lua-lsp) besitzt aktuell eine eigene, 
  eigenständige gradle/libs.versions.toml – dadurch mussten Versions-Fixes am 17.07. 
  mehrfach manuell in jedem Plugin nachgezogen werden.
- Plugins werden als eigenständige Gradle-Projekte gebaut (eigenes gradlew, eigene 
  settings.gradle.kts) und NICHT als Composite-Build ins Root-Projekt eingebunden – 
  das erschwert klassisches includeBuild-Sharing.

# Anforderungen
1. Erstelle im Projekt-Root eine maßgebliche, versionierte Referenzdatei:
   `gradle/shared-catalog/libs.versions.toml` mit allen gemeinsam genutzten Versionen 
   (Kotlin, AGP, Compose BOM, Compose Compiler, R8, lsp4j, Coroutines, AndroidX Core/
   Lifecycle/Activity, Test-Libraries).

2. Da Plugins als unabhängige Gradle-Builds außerhalb des Root-Composite-Builds liegen, 
   implementiere einen Sync-Mechanismus statt eines echten includeBuild-Sharings:
   - Erstelle ein Gradle-Task im Root-Projekt: `syncPluginCatalogs`, das die Referenzdatei 
     in jedes plugins/<name>/gradle/libs.versions.toml kopiert, dabei plugin-spezifische 
     Zusatzabhängigkeiten (die NICHT im shared-catalog stehen, z. B. lsp-server-spezifische 
     Libraries) aus der bestehenden Datei erhält (Merge, nicht Overwrite).
   - Implementiere den Merge robust: gemeinsame Keys werden aus shared-catalog übernommen 
     und überschreiben lokale Werte, plugin-eigene Zusatz-Keys bleiben unverändert erhalten.

3. Erstelle einen Verifikations-Task `verifyCatalogConsistency`, der bei jedem CI-Lauf 
   (oder manuell) prüft, ob alle Plugin-Kataloge mit dem shared-catalog für die 
   gemeinsamen Keys übereinstimmen, und mit klarer Fehlermeldung fehlschlägt, falls nicht 
   (verhindert stillschweigenden erneuten Drift).

4. Dokumentiere den Workflow in einer neuen VERSIONING.md: 
   "Version ändern → shared-catalog anpassen → syncPluginCatalogs ausführen → 
   verifyCatalogConsistency zur Bestätigung ausführen → Commit."

5. Integriere `verifyCatalogConsistency` optional als Pre-Commit-Hook-Empfehlung 
   (Dokumentation, kein Zwang) und/oder als Schritt in einer bestehenden/zukünftigen 
   CI-Pipeline (siehe Prompt 11-ci-pipeline).

# Akzeptanzkriterien
- Eine Versionsänderung (z. B. Kotlin-Update) erfordert nur eine Änderung in 
  gradle/shared-catalog/libs.versions.toml plus einen Task-Lauf, keine manuellen 
  Einzeländerungen mehr in 7 Plugin-Ordnern.
- `verifyCatalogConsistency` schlägt zuverlässig fehl, wenn jemand versehentlich direkt 
  in einer Plugin-libs.versions.toml eine geteilte Version manuell ändert, ohne den 
  shared-catalog zu aktualisieren.