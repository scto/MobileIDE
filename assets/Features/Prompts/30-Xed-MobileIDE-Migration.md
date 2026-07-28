## Antigravity-CLI Prompt: Xed-Editor → MobileIDE Migration & Terminal-Overhaul

Führe im Arbeitsverzeichnis ~/ folgende mehrteilige Migrations- und Refactoring-Aufgabe 
vollständig und eigenständig durch. Quelle: ~/Xed-Editor (Referenzprojekt). Ziel: ~/MobileIDE 
(Gradle-Multi-Modul-Monorepo, Kotlin DSL, Package-Root "com.scto.mobile.ide").

WICHTIG: Dies ist ein mehrphasiges Vorhaben. Nach JEDER Phase MUSST du "./gradlew build" 
ausführen, Fehler iterativ beheben, und einen Kurzstatus ausgeben, bevor du zur nächsten 
Phase übergehst. Erstelle vor Beginn einen Sicherungs-Branch:
  cd ~/MobileIDE && git checkout -b backup/before-xed-migration && git checkout main

=====================================================================
PHASE 1: MIGRATION ALLER FEATURE-SUBMODULE VON ~/Xed-Editor/features
=====================================================================

## Schritt 1.1: Bestandsaufnahme
- Liste alle Unterverzeichnisse in ~/Xed-Editor/features auf.
- Vergleiche sie mit den bereits existierenden Modulen in ~/MobileIDE/features 
  (aktuell laut settings.gradle.kts: :features:layout-preview, :features:proot, 
  :features:exec, :features:terminal, :features:lsp).
- Erstelle eine Mapping-Tabelle: Xed-Editor-Modul -> Zielpfad in MobileIDE/features, 
  inkl. Kennzeichnung ob es sich um NEUE Module handelt oder um Module, die mit 
  bereits vorhandenen MobileIDE-Feature-Modulen ZUSAMMENGEFÜHRT werden müssen 
  (Namenskollision).

## Schritt 1.2: Spezialfall Terminal-CLI-Umbenennung
- Finde ~/Xed-Editor/features/terminal/xed-cli.
- Benenne dieses Verzeichnis (inkl. aller enthaltenen Gradle-Modul-Referenzen, 
  Klassennamen, Manifest-Einträge und Skript-Dateinamen) um zu "mobileide-cli", 
  sodass es im Zielprojekt unter ~/MobileIDE/features/terminal/mobileide-cli landet.
- Falls Xed-Editor CLI-Kommandos wie "xed <command>" o.ä. definiert, ändere den 
  Binary-/Command-Namen konsistent auf "mobileide" (z. B. "mobileide open", 
  "mobileide --version").

## Schritt 1.3: Package- und Import-Migration
- Für JEDE migrierte Datei (.kt, .java, .xml, .json, .gradle.kts):
  - Ersetze alle Package-Deklarationen und Imports von Xed-Editor-Namensräumen 
    (typischerweise beginnend mit "com.rk.*" oder ähnlichem Xed-Editor-Root-Package -
    ermittle das exakte Root-Package durch Analyse der build.gradle.kts / 
    AndroidManifest.xml in ~/Xed-Editor) auf das MobileIDE-Schema 
    "com.scto.mobile.ide.features.<modulname>".
  - Passe alle relativen Modul-Referenzen in build.gradle.kts an 
    (z. B. project(":core:extension") -> passendes MobileIDE-Äquivalent, 
    Referenz auf com.rk.exec.* -> com.scto.mobile.ide.features.exec.*, etc. - 
    prüfe anhand von core/extension in MobileIDE, welche Interfaces/Facades 
    bereits existieren, siehe PROGRESS.md Eintrag "06-modularization-features-consolidation").

## Schritt 1.4: Dateinamens- und Text-Korrektur (xed/Xed → mobileide/MobileIDE)
- Durchsuche ALLE migrierten Dateien (Code, Ressourcen, Manifeste, Skripte, Strings, 
  Kommentare, Dateinamen selbst) nach den Mustern:
  "xed", "Xed", "XED", "Xed-Editor", "xed-editor" (in beliebigen Wort-Kontexten wie 
  Präfixen/Suffixen, z. B. "xed_something", "SomethingXed", "xed-cli")
- Ersetze konsistent unter Beachtung der jeweiligen Schreibweise:
  - "xed" (klein, im Wortkontext wie Variablen/Dateinamen) -> "mobileide"
  - "Xed" (Klassen-/Titel-Schreibweise) -> "MobileIDE"
  - "XED" (Konstanten/ENV-Variablen) -> "MOBILEIDE"
  - "Xed-Editor" / "xed-editor" -> "MobileIDE" bzw. "mobileide" je nach Kontext
- Benenne betroffene DATEINAMEN selbst entsprechend um (nicht nur den Inhalt), 
  z. B. "XedTerminalActivity.kt" -> "MobileIDETerminalActivity.kt".
- Ausnahme: Belasse Verweise, die explizit auf das externe Referenzprojekt 
  "Xed-Editor" als Quellangabe/Attribution in Lizenz-/README-Kommentaren verweisen 
  (z. B. "// originally adapted from Xed-Editor"), sofern dies aus 
  Lizenz-/Urheberrechtsgründen (siehe LICENSE, GPLv3) sinnvoll ist.

## Schritt 1.5: settings.gradle.kts & Root build.gradle.kts Korrektur
- Entferne aus der aktuellen include-Liste in settings.gradle.kts JEDEN Verweis auf 
  ":features:proot", ":features:exec", ":features:lsp" (werden in Phase 2 final entfernt, 
  hier zunächst nur vormerken/kommentieren mit "// TODO Phase 2: entfernen").
- Füge include-Statements für alle NEU migrierten Feature-Module aus Xed-Editor hinzu 
  (inkl. ":features:terminal:mobileide-cli" für das umbenannte CLI-Modul).
- Prüfe alle build.gradle.kts-Dateien im GESAMTEN MobileIDE-Projekt (app, editor, 
  editor-lsp, core:*, plugins:*, features:*) auf Referenzen zu den migrierten/
  umbenannten Modulen und korrigiere die project(":...")-Pfade entsprechend.
- Übertrage fehlende Dependency-Deklarationen (Versionen/Bibliotheken) aus den 
  Xed-Editor build.gradle.kts-Dateien in gradle/libs.versions.toml von MobileIDE, 
  falls dort noch nicht vorhanden (Namenskollisionen bei Versions-Aliases vermeiden, 
  ggf. mit Suffix versehen).

## Schritt 1.6: Build-Verifikation Phase 1
- Führe "./gradlew build" aus, behebe alle Compile-Fehler iterativ.
- Gib Kurzstatus aus: migrierte Module (Liste), umbenannte Dateien (Anzahl), 
  verbliebene "xed"-Treffer laut grep-Check (sollte 0 sein, Befehl: 
  grep -rli "xed" --include="*.kt" --include="*.xml" --include="*.gradle.kts" . 
  | grep -v "LICENSE\|README" ).

=====================================================================
PHASE 2: ENTFERNUNG DER ALTEN FEATURE-SUBMODULE proot, exec, lsp
=====================================================================

## Schritt 2.1: Analyse vor Löschung
- Prüfe projektweit, wer aktuell ":features:proot", ":features:exec", ":features:lsp" 
  als Dependency referenziert (grep über alle build.gradle.kts).
- Stelle sicher, dass die in Phase 1 migrierten Xed-Editor-Äquivalente 
  (falls Xed-Editor eigene proot/exec/lsp-Handling-Module besitzt) bereits als 
  Ersatz eingebunden sind, ODER dass die Funktionalität (ProotSandbox-Interface, 
  ShellUtils-Facade laut PROGRESS.md "06-modularization-features-consolidation") 
  bereits vollständig durch die neuen/verbliebenen Module abgedeckt ist.
- Falls NICHT vollständig abgedeckt: übernimm die fehlenden Klassen aus den 
  alten Modulen :features:proot / :features:exec / :features:lsp direkt in das 
  jeweils passende neu migrierte Modul (Package-Pfade anpassen), BEVOR du löschst.

## Schritt 2.2: Löschung
- Entferne die Verzeichnisse features/proot, features/exec, features/lsp vollständig:
    git rm -r features/proot features/exec features/lsp
- Entferne final die zuvor auskommentierten include-Zeilen aus settings.gradle.kts.
- Entferne alle verbliebenen Dependency-Referenzen auf diese drei Module aus 
  sämtlichen build.gradle.kts-Dateien im Projekt.

## Schritt 2.3: Build-Verifikation Phase 2
- Führe "./gradlew build" aus, behebe iterativ alle Fehler, die durch die Löschung 
  entstehen (fehlende Imports, kaputte Interface-Implementierungen).
- Gib Kurzstatus aus: Bestätigung, dass proot/exec/lsp entfernt sind und der Build läuft.

=====================================================================
PHASE 3: TERMINAL-ERSATZ DURCH XED-TERMINAL + MULTI-DISTRO-ERWEITERUNG
=====================================================================

## Schritt 3.1: Vollständiger Ersatz des Terminal-Moduls
- Das bestehende Terminal-Modul :features:terminal in MobileIDE (laut PROGRESS.md 
  "15-consolidate-terminal-features-module" bereits konsolidiert: Session-Backend, 
  PTY-Emulator, View-Rendering, TerminalScreen/-SettingsScreen, TerminalService, 
  Sandbox-Assets ideenv/idesetup/init.sh/setup.sh/colorschemes) wird KOMPLETT durch 
  das in Phase 1 migrierte Xed-Editor-Terminal ersetzt.
- Lösche den gesamten bisherigen Quellcode-Inhalt von features/terminal/src 
  (außer dem neu migrierten mobileide-cli Submodul aus Schritt 1.2), UND lösche 
  alle bisherigen Terminal-Assets unter features/terminal/src/main/assets/terminal/*.
- Kopiere/verschiebe den kompletten Quellcode UND alle Assets 
  (inkl. Farbschemata, Setup-Skripte, Sandbox-Konfigurationsdateien) aus dem in 
  Phase 1 migrierten Xed-Editor-Terminal-Modul 1:1 in features/terminal/src.
- Stelle sicher, dass zentrale Navigationsrouten (laut PROGRESS.md: Route 
  "settings/terminal") und Cross-Modul-Interfaces (DistroManager.buildProotCommand, 
  ScriptedLspServer.terminalLauncher) im neuen Terminal-Code entsprechend 
  reimplementiert/portiert werden, damit alle abhängigen Module (:app, :plugins:*, 
  :core:lsp) weiterhin fehlerfrei kompilieren. Falls das Xed-Terminal andere 
  Konzepte nutzt, erstelle Adapter-/Facade-Klassen, die exakt diese Signaturen 
  nach außen bereitstellen.

## Schritt 3.2: Multi-Distro-Fähigkeiten implementieren
- Erweitere den migrierten Terminal-Quellcode (insbesondere die Klasse, die für 
  RootFS-Extraktion und PRoot-Start zuständig ist, vermutlich Äquivalent zu 
  "DistroManager") um echte Multi-Distro-Unterstützung:
  - Definiere ein Enum/Sealed-Class "SupportedDistro" mit initial den Werten 
    UBUNTU und ALPINE.
  - Implementiere pro Distro eine eigene Konfigurationsklasse (Download-URL des 
    RootFS-Archivs, Paketmanager-Typ [apt vs. apk], Standard-Pakete, Architektur-
    Mapping für aarch64/armv7/x86_64).
  - Passe das bestehende setup.sh (bzw. dessen migriertes Äquivalent) so an, dass 
    es einen Parameter/Env-Variable "MOBILEIDE_DISTRO" (Werte: "ubuntu" | "alpine") 
    entgegennimmt und je nach Wert die passende RootFS-URL herunterlädt und mit dem 
    korrekten Paketmanager (apt-get vs. apk) initialisiert.
  - Erstelle in der Terminal-Settings-UI (TerminalSettingsScreen-Äquivalent im neuen 
    Code) eine Auswahlmöglichkeit (Dropdown/Radio-Buttons) "Distribution wählen: 
    Ubuntu | Alpine", die vor der Erstinstallation angezeigt wird und den gewählten 
    Wert persistent in SharedPreferences/DataStore speichert.
  - Stelle sicher, dass ein Wechsel der Distro (z. B. über "Terminal neu 
    installieren") den alten RootFS-Ordner sauber entfernt und den neuen gemäß 
    gewählter Distro frisch extrahiert.

## Schritt 3.3: Assets für Multi-Distro erweitern
- Erweitere die migrierten Terminal-Assets um distro-spezifische Konfigurationsdateien:
  - features/terminal/src/main/assets/terminal/distros/ubuntu/ (falls noch nicht 
    vorhanden durch Migration: init.sh, setup.sh oder distro-spezifische Teile davon)
  - features/terminal/src/main/assets/terminal/distros/alpine/ (analog für Alpine, 
    inkl. angepasster Paketnamen wie "openssh" statt "openssh-client openssh-server")
  - Migriere/generalisiere die Farbschemata (colorschemes/*) so, dass sie 
    distro-unabhängig funktionieren (kein Duplizieren nötig, nur einmal vorhalten).
- Aktualisiere alle Skript-Referenzen (idesetup, ideenv-Äquivalente im neuen Code) 
  so, dass sie basierend auf der gewählten Distro den korrekten Unterordner unter 
  distros/ referenzieren.

## Schritt 3.4: Alte Terminal-Assets endgültig deaktivieren
- Bestätige per grep-Check, dass im GESAMTEN Projekt (app, alle Module) NIRGENDS 
  mehr auf die alten Terminal-Assets-Pfade referenziert wird, die vor Phase 3 
  existierten (alte Struktur laut PROGRESS.md-Eintrag 
  "15-consolidate-terminal-features-module").
- Stelle sicher, dass zukünftig AUSSCHLIESSLICH die neuen, aus Xed-Editor migrierten 
  und um Multi-Distro erweiterten Assets verwendet werden.

## Schritt 3.5: Build-Verifikation Phase 3
- Führe "./gradlew build" und speziell "./gradlew :features:terminal:build" aus.
- Behebe iterativ alle Fehler.
- Gib Kurzstatus aus: Bestätigung Terminal-Ersatz abgeschlossen, Multi-Distro 
  (Ubuntu/Alpine) implementiert, alte Assets entfernt.

=====================================================================
PHASE 4: FEHLENDE SOURCE-DATEIEN AUS ~/Xed-Editor NACHZIEHEN
=====================================================================

## Schritt 4.1: Vollständigkeits-Analyse
- Vergleiche systematisch die Funktionsumfänge zwischen ~/Xed-Editor (Gesamtprojekt, 
  nicht nur /features) und ~/MobileIDE.
- Identifiziere Klassen/Utility-Funktionen/Helper aus Xed-Editor (z. B. in Verzeichnissen 
  wie core, app, extension, common, utils), die von den migrierten Feature-Modulen 
  (Terminal, mobileide-cli, sowie den in Phase 1 migrierten neuen Feature-Modulen) 
  BENÖTIGT werden (Compile-Errors durch fehlende Klassen/Referenzen), aber in 
  MobileIDE noch nicht existieren.
- Erstelle eine Liste aller fehlenden, aber benötigten Dateien mit Quellpfad 
  (Xed-Editor) und geplantem Zielpfad (MobileIDE).

## Schritt 4.2: Migration der fehlenden Dateien
- Kopiere jede identifizierte fehlende Datei aus ~/Xed-Editor in den passenden 
  MobileIDE-Modulpfad (bevorzugt in ein bereits existierendes MobileIDE-Modul wie 
  :core:common, :core:extension, :core:main - falls keine passende Heimat existiert, 
  lege die Datei im am ehesten zugehörigen migrierten Feature-Modul ab).
- Wende auf JEDE dieser Dateien identisch die Regeln aus Phase 1, Schritt 1.3 
  (Package-/Import-Migration) und Schritt 1.4 (xed/Xed -> mobileide/MobileIDE 
  Textkorrektur) an.
- Löse dabei entstehende Namenskonflikte mit bereits vorhandenen MobileIDE-Klassen 
  gleichen Namens durch Analyse und sinnvolle Konsolidierung (bevorzuge die 
  vollständigere Implementierung, kommentiere übernommene Teile mit 
  "// merged from Xed-Editor").

## Schritt 4.3: Build-Verifikation Phase 4
- Führe "./gradlew build" aus, behebe iterativ alle verbliebenen Compile-Fehler, 
  bis das GESAMTE Projekt (app, editor, alle core:*, alle features:*, alle plugins:*) 
  fehlerfrei baut und die volle, aus Xed-Editor migrierte Funktionalität erhalten ist.
- Gib Kurzstatus aus: Liste der nachgezogenen Dateien, Bestätigung vollständiger Build.

=====================================================================
PHASE 5: PLUGIN STORE (GitHub Pages) + SettingsScreen-Integration
=====================================================================

## Schritt 5.1: Analyse der Xed-Editor Store-Fähigkeiten
- Untersuche in ~/Xed-Editor, wie die Extension-/Plugin-Marketplace-Funktionalität 
  implementiert ist (vermutlich ein Screen, der eine Liste von manifest.json-basierten 
  Erweiterungen von einer Remote-URL lädt, anzeigt, und "Install from URL/ZIP" anbietet - 
  MobileIDE nutzt bereits ein äquivalentes manifest.json + schema.json-Format für 
  Plugins, siehe plugins/*/manifest.json in diesem Projekt).
- Identifiziere die relevanten UI-Screens, ViewModels, und Netzwerk-/Repository-Klassen 
  in Xed-Editor, die diese "Store"-Funktionalität abbilden.

## Schritt 5.2: Migration der Store-Funktionalität nach MobileIDE
- Migriere die identifizierten Store-Klassen (UI + Logik) nach MobileIDE, bevorzugt 
  in ein neues Modul ":features:plugin-store" (erstelle dieses Modul inkl. 
  build.gradle.kts, füge es in settings.gradle.kts hinzu).
- Wende die Migrationsregeln aus Phase 1 (Package-Migration, xed/Xed-Textkorrektur) an.
- Passe die Datenmodelle so an, dass sie exakt zum bereits vorhandenen MobileIDE 
  Plugin-Manifest-Schema passen (siehe plugins/*/manifest.json Struktur: id, name, 
  mainClass, version, description, author{displayName,github}, license, tags, 
  minAppVersion, maxAppVersion, repository, hasSettings - Referenz-Schema liegt 
  bereits im Projekt vor, z. B. unter plugins/zig-lsp/schema/schema.json).

## Schritt 5.3: Integration in den MobileIDE SettingsScreen
- Öffne den bestehenden SettingsScreen (im Modul :app, vermutlich 
  "SettingsScreen.kt" bzw. das zugehörige Navigation-Graph-Setup).
- Füge einen NEUEN, separaten Abschnitt/Kategorie-Eintrag "Plugin Store" / 
  "Erweiterungen" hinzu (analog zu bestehenden Kategorien wie "Terminal", "Editor", 
  "Build Config", "LSP Status" - siehe status.md für bestehende Sektionsstruktur).
- Dieser Abschnitt navigiert zum migrierten Plugin-Store-Screen aus 
  :features:plugin-store, der zukünftig verfügbare Plugins/Extensions von einer 
  konfigurierbaren Remote-URL (Standard-Platzhalter: GitHub Pages URL, siehe Schritt 
  5.4) abruft, als Liste mit Icon/Name/Beschreibung/Version/Tags anzeigt, und einen 
  "Installieren"-Button bereitstellt, der die ZIP via die bestehende 
  "Install from storage/URL"-Logik der Extension-Engine (:core:extension) einbindet.
- Zeige zusätzlich die bereits mitgelieferten Plugins (aus plugins/*/manifest.json 
  in diesem Projekt: java-lsp, json-lsp, kotlin-lsp, kotlin-kmp-lsp, lua-lsp, 
  python-lsp, typst-lsp, go-lsp, rust-lsp, zig-lsp, fsharp-lsp, prettier-lsp) 
  optional als "Bereits installiert / Bundled" Sektion im selben Screen.

## Schritt 5.4: Statischer Plugin-Store für GitHub Pages
- Erstelle im Repository-Root ein neues Verzeichnis "docs/" (Standard-Pfad für 
  GitHub Pages) mit folgender Struktur:
  - docs/index.html - Einfache statische Übersichtsseite (HTML/CSS, kein Framework 
    nötig), die alle verfügbaren Plugins als Karten anzeigt (Name, Beschreibung, 
    Version, Autor, Tags, Download-Button verlinkt auf die jeweilige ZIP-Release-URL).
  - docs/plugins.json - Zentrales Manifest-Verzeichnis: ein JSON-Array, das für 
    JEDES der 12 vorhandenen Plugins (java-lsp, json-lsp, etc.) einen Eintrag enthält, 
    zusammengestellt aus den jeweiligen plugins/*/manifest.json-Dateien (Felder: 
    id, name, version, description, author, license, tags, repository, 
    downloadUrl [Platzhalter-Format: 
    "https://github.com/<owner>/<repo>/releases/download/<tag>/<name>.zip"], 
    iconUrl [Platzhalter auf plugins/*/icon.png]).
  - docs/schema/plugins-index.schema.json - JSON-Schema zur Validierung von 
    plugins.json, analog zum bestehenden Einzelplugin-Schema 
    (plugins/*/schema/schema.json).
  - Erstelle ein Python- oder Kotlin-Skript "scripts/generate_plugin_store.py" 
    (analog zu scripts/package_all_plugins.py), das automatisiert durch alle 
    plugins/*/manifest.json-Dateien iteriert und daraus docs/plugins.json neu 
    generiert - so bleibt der Store bei neuen/aktualisierten Plugins wartbar.
- Erstelle eine GitHub Actions Workflow-Datei ".github/workflows/deploy-plugin-store.yml", 
  die bei Änderungen an plugins/**/manifest.json automatisch 
  "scripts/generate_plugin_store.py" ausführt und den docs/-Ordner auf den 
  "gh-pages"-Branch bzw. direkt via GitHub Pages Actions-Deployment veröffentlicht.
- Konfiguriere in :features:plugin-store die Standard-Remote-URL so, dass sie auf 
  "https://<github-username>.github.io/<repo-name>/plugins.json" zeigt 
  (als konfigurierbare Konstante, die der Nutzer später anpassen kann).

## Schritt 5.5: Build- und Funktionsverifikation Phase 5
- Führe "./gradlew :features:plugin-store:build" und den vollständigen 
  "./gradlew build" aus, behebe iterativ alle Fehler.
- Prüfe, dass der neue Settings-Abschnitt "Plugin Store" korrekt navigierbar ist 
  und (im UI-Code zumindest strukturell) eine Liste rendert, selbst wenn die 
  Remote-URL zur Laufzeit (ohne echtes GitHub Pages Deployment) noch keine Daten 
  liefert (Leerzustand/Error-State sauber behandeln).

=====================================================================
ABSCHLUSSBERICHT (nach ALLEN 5 Phasen)
=====================================================================
Gib eine strukturierte Gesamtzusammenfassung aus:
1. Phase 1: Liste aller migrierten Feature-Module, Bestätigung mobileide-cli-Umbenennung, 
   Anzahl korrigierter xed/Xed-Vorkommen.
2. Phase 2: Bestätigung Entfernung von proot/exec/lsp, Liste ggf. übernommener 
   Restfunktionalität.
3. Phase 3: Bestätigung Terminal-Komplettersatz, Liste der Multi-Distro-Dateien/Klassen, 
   unterstützte Distros (Ubuntu, Alpine).
4. Phase 4: Liste nachgezogener fehlender Source-Dateien aus Xed-Editor.
5. Phase 5: Pfad zum neuen :fea
