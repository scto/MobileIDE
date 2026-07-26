### Prompt-Datei: `06-modularization-features-consolidation.md`

# Ziel
Konsolidiere Terminal-, Exec-, PRoot- und LSP-bezogenen Code aus :app und den bestehenden 
:core:*-Modulen in klar abgegrenzte neue Module: :features:terminal, :features:exec, 
:features:proot, :features:lsp.

# Kontext / WICHTIG
- Dies ist eine reine Refactoring-Aufgabe mit hohem Risiko für Breaking Changes. 
  Vor Beginn: vollständige Abhängigkeitsanalyse (Gradle Module Graph) erstellen und dokumentieren, 
  welche bestehenden Module (:core:terminal, :core:terminal-emulator, :core:terminal-view, 
  :core:runner, :core:lsp, :core:extension, :extension-languages) welche Klassen enthalten.
- Gehe Datei für Datei vor, NICHT alles auf einmal verschieben. Nach jedem Teilschritt: 
  vollständiger Gradle-Build (`./gradlew build`) zur Verifikation.

# Anforderungen
1. Erstelle Zielmodule in settings.gradle.kts:
   include(":features:terminal")
   include(":features:exec")
   include(":features:proot")
   include(":features:lsp")

2. :features:proot
   - Verschiebe alle Klassen rund um DistroManager, PRoot-Command-Building, Sandbox-Setup-
     Skript-Handling (Referenzen aus setup.sh/init.sh/idesetup Ladelogik) hierher.
   - Definiere ein klares public API-Interface (z. B. ProotSandbox) statt konkrete Klassen 
     direkt zu exportieren, damit :app nur gegen Interfaces programmiert.

3. :features:exec
   - Verschiebe alle `com.scto.mobile.ide.exec.*`-Klassen (ShellUtils, TerminalCommand, 
     ubuntuProcess, launchTerminal, isTerminalInstalled – referenziert in den Plugin-Sourcen 
     wie TypstInstallationManager.kt) hierher.
   - Dieses Modul hat eine Abhängigkeit zu :features:proot (für Ubuntu-Prozessausführung), 
     aber NICHT umgekehrt.

4. :features:terminal
   - Konsolidiere :core:terminal, :core:terminal-emulator, :core:terminal-view zu einem 
     Feature-Modul ODER behalte die interne Dreiteilung als Sub-Packages innerhalb des 
     einen Moduls (entscheide basierend auf tatsächlicher Kopplungsstärke – falls terminal-
     emulator/view auch von anderen Features unabhängig genutzt werden, als separate 
     interne source-sets belassen, aber alles unter einem Gradle-Modul :features:terminal 
     zusammenführen wie vom Nutzer gewünscht).
   - Migriere die komplette Terminal-UI aus :app (TerminalScreen, TerminalSettingsScreen, 
     TerminalService) in dieses Modul; :app referenziert nur noch Navigation-Einstiegspunkte.

5. :features:lsp
   - Konsolidiere :core:lsp (Registry/Interfaces) und die LSP-Editor-Integration aus 
     :editor-lsp in dieses eine Modul. :extension-languages bleibt als eigenständiges, 
     abhängiges Modul (konkrete Server-Implementierungen), referenziert aber :features:lsp 
     statt :core:lsp.

6. Nach jeder Verschiebung:
   - Aktualisiere alle Imports projektweit (inkl. aller plugins/*-Subprojekte, die z. B. 
     `com.scto.mobile.ide.exec.ShellUtils` oder `com.scto.mobile.ide.lsp.LspRegistry` 
     importieren – siehe TypstServer.kt, TypstInstallationManager.kt als Beispiele für 
     betroffene externe Referenzen über das SDK/sdk.jar).
   - Prüfe explizit, ob sich das Extension-SDK (sdk.jar, das für Plugins gebaut wird) durch 
     die Paketverschiebung ändert – falls ja, muss die SDK-Build-Pipeline (siehe 
     downloadLatestJar-Task in Plugin-build.gradle.kts) synchron aktualisiert werden, 
     sonst brechen ALLE bestehenden Plugins.

# Akzeptanzkriterien
- `./gradlew build` läuft für Root-Projekt und für mindestens ein Plugin (z. B. typst-lsp) 
  fehlerfrei durch.
- Keine zyklischen Modul-Abhängigkeiten (`./gradlew :app:dependencies` zeigt saubere DAG).
- Terminal-, Build- und LSP-Funktionalität in der App bleibt nach der Migration 
  funktional unverändert (manuelle Regressionsprüfung: Terminal öffnen, Datei mit LSP 
  bearbeiten, Build ausführen).