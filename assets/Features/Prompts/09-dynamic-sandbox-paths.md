### Prompt-Datei: `09-dynamic-sandbox-paths.md`

# Ziel
Entferne alle hartkodierten Termux/PRoot-Pfade aus gradle.properties und anderen 
Konfigurationsdateien und ersetze sie durch dynamisch von ideenv/DistroManager 
generierte Werte, damit das Projekt auf unterschiedlichen Geräten/Installationspfaden 
portabel bleibt.

# Kontext
- Aktuell enthält die Root-gradle.properties feste Pfade wie 
  `android.aapt2FromMavenOverride=/data/data/com.termux/files/home/.androidide/aapt2`.
- ideenv/idesetup generieren bereits an anderer Stelle Umgebungsvariablen dynamisch 
  basierend auf der tatsächlichen Sandbox-Installation (siehe DistroManager, 
  JDK-Autodetektion aus PROGRESS.md).
- Das Problem betrifft primär: Build-Prozess innerhalb der App/PRoot-Sandbox selbst, 
  nicht die Entwicklung von MobileIDE auf einem Desktop-Rechner.

# Anforderungen
1. Analysiere alle Vorkommen von hartkodierten `/data/data/com.termux/` oder ähnlichen 
   absoluten Pfaden im gesamten Projekt (Root gradle.properties, Plugin-gradle.properties, 
   Skripte in idesetup/ideenv, ggf. AndroidManifest-Referenzen).

2. Erstelle eine zentrale Auflösungsroutine `SandboxPaths` (Kotlin-Objekt, sinnvoll in 
   :features:proot nach Konsolidierung, siehe Prompt 06) mit Properties wie:
   - `aapt2Path`, `javaHome`, `gradleUserHome`, `androidSdkRoot`
   - Werte werden zur Laufzeit ermittelt (z. B. via `context.filesDir`, 
     PackageManager-Query der eigenen Package-ID statt hartkodiertem "com.termux", 
     oder Auslesen einer von ideenv beim Sandbox-Setup geschriebenen Konfigurationsdatei 
     `~/.mobileide/env.properties`).

3. Ersetze in gradle.properties statische Pfade durch Platzhalter/Init-Script-Ansatz:
   - Nutze ein Gradle Init-Script (`~/.gradle/init.d/mobileide-paths.init.gradle.kts`), 
     das beim Sandbox-Setup (idesetup) dynamisch generiert wird und die tatsächlichen 
     Pfade zur Build-Zeit injiziert, statt sie statisch in gradle.properties zu verankern.
   - Alternative falls Init-Script-Ansatz nicht praktikabel: idesetup schreibt beim 
     Sandbox-Bootstrap eine gerätespezifische gradle.properties (Template + Ersetzung), 
     die NICHT ins Repository committed wird (zur .gitignore hinzufügen).

4. Füge einen Selbsttest hinzu: Beim App-Start (Sandbox-Initialisierung) wird geprüft, 
   ob die aufgelösten Pfade tatsächlich existieren (`File(path).exists()`), und bei 
   Fehlschlag eine klare Fehlermeldung mit Diagnosehinweis angezeigt statt eines 
   kryptischen Gradle-Fehlers erst beim Build-Versuch.

# Akzeptanzkriterien
- Ein Test-Build funktioniert auch, wenn die Sandbox unter einem abweichenden 
  Installationspfad läuft (z. B. anderes App-Package, anderer Root-Verzeichnis-Präfix).
- Keine verbleibenden hartkodierten `/data/data/com.termux/`-Pfade im Root- oder 
  Plugin-Projekt (verifizierbar per projektweiter Grep-Suche als CI-Check).