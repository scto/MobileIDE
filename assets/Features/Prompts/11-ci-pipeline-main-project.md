### Prompt-Datei: `11-ci-pipeline-main-project.md`

# Ziel
Erstelle eine automatisierte CI-Pipeline (GitHub Actions) für das MobileIDE-Hauptprojekt, 
analog zur bereits vorhandenen Pipeline in plugins/typst-lsp (.github/workflows/
plugin-build-test.yml), um Regressionen während der geplanten Modul-Konsolidierung 
(Prompt 06) und laufenden Weiterentwicklung frühzeitig zu erkennen.

# Kontext
- Für das Hauptprojekt (:app + alle :core:*, :editor*, :extension*-Module) existiert 
  laut aktueller Analyse noch keine CI-Pipeline, im Gegensatz zu mindestens einem Plugin.
- Aufgrund der PRoot/Termux-spezifischen Build-Anforderungen (aapt2-Override etc., 
  siehe Prompt 09) muss die CI-Umgebung ggf. reine JVM/Standard-Android-SDK-Umgebung 
  ohne PRoot-Spezifika nutzen (GitHub Actions Runner ist kein Termux/PRoot-Kontext) – 
  daher müssen sandbox-spezifische Properties für den CI-Build-Pfad deaktiviert/
  übersteuert werden.

# Anforderungen
1. Erstelle `.github/workflows/main-build-test.yml` mit folgenden Jobs:
   - `lint-and-assemble`: `./gradlew :app:lintDebug :app:assembleDebug` auf 
     ubuntu-latest Runner mit Standard-Android-SDK-Setup (setup-android-action).
   - `unit-tests`: `./gradlew testDebugUnitTest` für alle Module mit Test-Sourcesets, 
     Testergebnis-Report als Artifact hochladen.
   - `module-dependency-check`: Führt `./gradlew :app:dependencies` aus und prüft 
     (via einfachem Skript) auf zyklische Abhängigkeiten oder unerwünschte 
     Cross-Referenzen zwischen Feature-Modulen (relevant besonders während/nach Prompt 06).
   - `catalog-consistency` (falls Prompt 08 bereits umgesetzt): führt 
     `verifyCatalogConsistency` aus.

2. Override-Strategie für PRoot-spezifische gradle.properties-Einträge im CI-Kontext:
   - Erstelle ein separates `gradle-ci.properties`, das im CI-Workflow per 
     `--init-script` oder `ORG_GRADLE_PROJECT_*`-Umgebungsvariablen die 
     Termux-spezifischen Overrides (aapt2FromMavenOverride etc.) NICHT setzt, 
     sodass Standard-Maven-aapt2 verwendet wird.

3. Trigger: bei jedem Push auf `main`/`develop` und bei jedem Pull Request.

4. Status-Badge: Füge das Workflow-Status-Badge oben in die Haupt-README.md ein.

5. Cache-Optimierung: Gradle-Dependency-Cache und Konfigurations-Cache aktivieren 
   (`actions/cache` für `~/.gradle/caches` und `~/.gradle/wrapper`), um Build-Zeiten 
   auf akzeptablem Niveau zu halten (Ziel: < 10 Minuten für assemble + Unit-Tests).

# Akzeptanzkriterien
- Ein bewusst eingebauter Kompilierfehler in einem Pull-Request lässt den CI-Job 
  zuverlässig fehlschlagen, sichtbar direkt in der PR-Ansicht.
- Ein erfolgreicher Merge auf main zeigt einen grünen Badge-Status in der README.