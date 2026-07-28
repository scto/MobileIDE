Führe im Projekt ~/MobileIDE folgende Aufgabe durch. Dies ist PHASE 3 des 
Xed-Editor-Migrationsvorhabens.

VORAUSSETZUNG: Phase 1 muss abgeschlossen sein (das migrierte Xed-Editor-Terminal 
muss bereits unter ~/MobileIDE/features/terminal bzw. einem migrierten Zwischenpfad 
vorliegen, inkl. ":features:terminal:mobileide-cli"). Prüfe dies zu Beginn. 
Falls das migrierte Xed-Terminal nicht auffindbar ist, breche ab und weise auf 
die Notwendigkeit von Phase 1 hin. Phase 2 (Entfernung proot/exec/lsp) ist für 
diese Phase NICHT zwingend Voraussetzung, sollte aber idealerweise bereits erfolgt sein.

Erstelle zu Beginn einen Sicherungs-Branch:
  git checkout -b backup/before-terminal-replacement && git checkout main

## Schritt 1: Vollständiger Ersatz des Terminal-Moduls
- Das bestehende Terminal-Modul :features:terminal in MobileIDE (laut PROGRESS.md 
  "15-consolidate-terminal-features-module" bereits konsolidiert: Session-Backend, 
  PTY-Emulator, View-Rendering, TerminalScreen/-SettingsScreen, TerminalService, 
  Sandbox-Assets ideenv/idesetup/init.sh/setup.sh/colorschemes) wird KOMPLETT durch 
  das in Phase 1 migrierte Xed-Editor-Terminal ersetzt.
- Lösche den gesamten bisherigen Quellcode-Inhalt von features/terminal/src 
  (außer dem migrierten mobileide-cli Submodul), UND lösche alle bisherigen 
  Terminal-Assets unter features/terminal/src/main/assets/terminal/*.
- Kopiere/verschiebe den kompletten Quellcode UND alle Assets (inkl. Farbschemata, 
  Setup-Skripte, Sandbox-Konfigurationsdateien) aus dem migrierten 
  Xed-Editor-Terminal-Modul 1:1 in features/terminal/src.
- Stelle sicher, dass zentrale Navigationsrouten (Route "settings/terminal") und 
  Cross-Modul-Interfaces (DistroManager.buildProotCommand, 
  ScriptedLspServer.terminalLauncher) im neuen Terminal-Code entsprechend 
  reimplementiert/portiert werden, damit alle abhängigen Module (:app, :plugins:*, 
  :core:lsp) weiterhin fehlerfrei kompilieren. Erstelle bei Bedarf Adapter-/
  Facade-Klassen, die exakt diese Signaturen nach außen bereitstellen.

## Schritt 2: Multi-Distro-Fähigkeiten implementieren
- Erweitere den migrierten Terminal-Quellcode (insbesondere die Klasse, die für 
  RootFS-Extraktion und PRoot-Start zuständig ist, vermutlich Äquivalent zu 
  "DistroManager") um echte Multi-Distro-Unterstützung:
  - Definiere ein Enum/Sealed-Class "SupportedDistro" mit initial UBUNTU und ALPINE.
  - Implementiere pro Distro eine eigene Konfigurationsklasse (Download-URL des 
    RootFS-Archivs, Paketmanager-Typ [apt vs. apk], Standard-Pakete, 
    Architektur-Mapping für aarch64/armv7/x86_64).
  - Passe setup.sh (bzw. dessen migriertes Äquivalent) so an, dass es einen 
    Parameter/Env-Variable "MOBILEIDE_DISTRO" (Werte: "ubuntu" | "alpine") 
    entgegennimmt und je nach Wert die passende RootFS-URL herunterlädt und mit 
    dem korrekten Paketmanager (apt-get vs. apk) initialisiert.
  - Erstelle in der Terminal-Settings-UI eine Auswahlmöglichkeit 
    (Dropdown/Radio-Buttons) "Distribution wählen: Ubuntu | Alpine", die vor der 
    Erstinstallation angezeigt wird und den gewählten Wert persistent in 
    SharedPreferences/DataStore speichert.
  - Stelle sicher, dass ein Wechsel der Distro (z. B. über "Terminal neu 
    installieren") den alten RootFS-Ordner sauber entfernt und den neuen gemäß 
    gewählter Distro frisch extrahiert.

## Schritt 3: Assets für Multi-Distro erweitern
- Erweitere die migrierten Terminal-Assets um distro-spezifische Konfigurationsdateien:
  - features/terminal/src/main/assets/terminal/distros/ubuntu/
  - features/terminal/src/main/assets/terminal/distros/alpine/ 
    (inkl. angepasster Paketnamen wie "openssh" statt "openssh-client openssh-server")
  - Migriere/generalisiere die Farbschemata (colorschemes/*) so, dass sie 
    distro-unabhängig funktionieren (kein Duplizieren nötig).
- Aktualisiere alle Skript-Referenzen so, dass sie basierend auf der gewählten 
  Distro den korrekten Unterordner unter distros/ referenzieren.

## Schritt 4: Alte Terminal-Assets endgültig deaktivieren
- Bestätige per grep-Check, dass im GESAMTEN Projekt NIRGENDS mehr auf die alten 
  Terminal-Assets-Pfade referenziert wird, die vor dieser Phase existierten.
- Stelle sicher, dass zukünftig AUSSCHLIESSLICH die neuen, aus Xed-Editor migrierten 
  und um Multi-Distro erweiterten Assets verwendet werden.

## Schritt 5: Build-Verifikation
- Führe "./gradlew build" und speziell "./gradlew :features:terminal:build" aus.
- Behebe iterativ alle Fehler.

## Abschlussbericht
Gib aus:
- Bestätigung Terminal-Komplettersatz abgeschlossen
- Liste der Multi-Distro-Dateien/Klassen (DistroManager, SupportedDistro, etc.)
- Unterstützte Distros: Ubuntu, Alpine
- Bestätigung: alte Assets vollständig entfernt, "./gradlew build" erfolgreich
- Manueller Funktionstest empfohlen: Terminal öffnen, Ubuntu installieren, 
  Terminal neu installieren mit Alpine, Distro-Wechsel prüfen
- Commit erstellen: "feat(phase3): replace terminal with Xed terminal + multi-distro support"

WICHTIG: Warte auf explizite Bestätigung des Nutzers (idealerweise nach manuellem 
Test von Ubuntu UND Alpine), bevor mit "Phase 4: Backport fehlender Xed-Sources" 
fortgefahren wird.