Führe im Projekt ~/MobileIDE folgende Aufgabe durch. Dies ist PHASE 4 des 
Xed-Editor-Migrationsvorhabens. Quelle: ~/Xed-Editor (Gesamtprojekt, nicht nur 
/features). Ziel: ~/MobileIDE.

VORAUSSETZUNG: Phasen 1-3 (Feature-Migration, Legacy-Entfernung, Terminal-Ersatz 
inkl. Multi-Distro) sollten bereits abgeschlossen sein, damit hier vollständig 
geprüft werden kann, welche Xed-Editor-Klassen noch fehlen. Prüfe zu Beginn, ob 
features/terminal bereits das migrierte Xed-Terminal enthält (Enum "SupportedDistro" 
sollte vorhanden sein) - falls nicht, weise auf die Notwendigkeit vorheriger Phasen hin, 
fahre aber dennoch mit der Analyse fort, sofern der Nutzer dies bestätigt.

Erstelle zu Beginn einen Sicherungs-Branch:
  git checkout -b backup/before-source-backport && git checkout main

## Schritt 1: Vollständigkeits-Analyse
- Vergleiche systematisch die Funktionsumfänge zwischen ~/Xed-Editor (Gesamtprojekt) 
  und ~/MobileIDE.
- Identifiziere Klassen/Utility-Funktionen/Helper aus Xed-Editor (z. B. in 
  Verzeichnissen wie core, app, extension, common, utils), die von den migrierten 
  Feature-Modulen (Terminal, mobileide-cli, sowie den in Phase 1 migrierten Feature-
  Modulen) BENÖTIGT werden (Compile-Errors durch fehlende Klassen/Referenzen), aber 
  in MobileIDE noch nicht existieren.
- Erstelle eine Liste aller fehlenden, aber benötigten Dateien mit Quellpfad 
  (Xed-Editor) und geplantem Zielpfad (MobileIDE).

## Schritt 2: Migration der fehlenden Dateien
- Kopiere jede identifizierte fehlende Datei aus ~/Xed-Editor in den passenden 
  MobileIDE-Modulpfad (bevorzugt in ein bereits existierendes MobileIDE-Modul wie 
  :core:common, :core:extension, :core:main - falls keine passende Heimat existiert, 
  lege die Datei im am ehesten zugehörigen migrierten Feature-Modul ab).
- Wende auf JEDE dieser Dateien die Migrationsregeln an:
  - Package-/Import-Migration auf "com.scto.mobile.ide.*"-Schema
  - Textkorrektur aller "xed"/"Xed"/"XED"/"Xed-Editor"-Vorkommen zu 
    "mobileide"/"MobileIDE"/"MOBILEIDE" (inkl. Dateinamen selbst), außer bei 
    bewussten Lizenz-Attributionen
- Löse dabei entstehende Namenskonflikte mit bereits vorhandenen MobileIDE-Klassen 
  gleichen Namens durch Analyse und sinnvolle Konsolidierung (bevorzuge die 
  vollständigere Implementierung, kommentiere übernommene Teile mit 
  "// merged from Xed-Editor").

## Schritt 3: Build-Verifikation
- Führe "./gradlew build" aus, behebe iterativ alle verbliebenen Compile-Fehler, 
  bis das GESAMTE Projekt (app, editor, alle core:*, alle features:*, alle 
  plugins:*) fehlerfrei baut und die volle, aus Xed-Editor migrierte 
  Funktionalität erhalten ist.

## Abschlussbericht
Gib aus:
- Liste aller nachgezogenen Dateien (Xed-Pfad -> MobileIDE-Pfad)
- Liste gelöster Namenskonflikte
- Bestätigung: "./gradlew build" für das GESAMTE Projekt erfolgreich
- Commit erstellen: "feat(phase4): backport missing Xed-Editor source files"

WICHTIG: Warte auf explizite Bestätigung des Nutzers, bevor mit 
"Phase 5: Plugin Store + GitHub Pages" fortgefahren wird.