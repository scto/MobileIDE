Führe im Arbeitsverzeichnis ~/ folgende Migration durch. Quelle: ~/Xed-Editor. 
Ziel: ~/MobileIDE (Gradle-Multi-Modul-Monorepo, Kotlin DSL, Package-Root 
"com.scto.mobile.ide"). Dies ist PHASE 1 eines mehrteiligen Migrationsvorhabens.

Erstelle zu Beginn einen Sicherungs-Branch:
  cd ~/MobileIDE && git checkout -b backup/before-xed-migration-phase1 && git checkout main

## Schritt 1: Bestandsaufnahme
- Liste alle Unterverzeichnisse in ~/Xed-Editor/features auf.
- Vergleiche sie mit den bereits existierenden Modulen in ~/MobileIDE/features 
  (aktuell laut settings.gradle.kts: :features:layout-preview, :features:proot, 
  :features:exec, :features:terminal, :features:lsp).
- Erstelle eine Mapping-Tabelle: Xed-Editor-Modul -> Zielpfad in MobileIDE/features, 
  inkl. Kennzeichnung ob es sich um NEUE Module handelt oder um Module, die mit 
  bereits vorhandenen MobileIDE-Feature-Modulen ZUSAMMENGEFÜHRT werden müssen 
  (Namenskollision).

## Schritt 2: Spezialfall Terminal-CLI-Umbenennung
- Finde ~/Xed-Editor/features/terminal/xed-cli.
- Benenne dieses Verzeichnis (inkl. aller enthaltenen Gradle-Modul-Referenzen, 
  Klassennamen, Manifest-Einträge und Skript-Dateinamen) um zu "mobileide-cli", 
  sodass es im Zielprojekt unter ~/MobileIDE/features/terminal/mobileide-cli landet.
- Falls Xed-Editor CLI-Kommandos wie "xed <command>" o.ä. definiert, ändere den 
  Binary-/Command-Namen konsistent auf "mobileide" (z. B. "mobileide open", 
  "mobileide --version").

## Schritt 3: Package- und Import-Migration
- Für JEDE migrierte Datei (.kt, .java, .xml, .json, .gradle.kts):
  - Ersetze alle Package-Deklarationen und Imports von Xed-Editor-Namensräumen 
    (ermittle das exakte Root-Package durch Analyse der build.gradle.kts / 
    AndroidManifest.xml in ~/Xed-Editor) auf das MobileIDE-Schema 
    "com.scto.mobile.ide.features.<modulname>".
  - Passe alle relativen Modul-Referenzen in build.gradle.kts an, prüfe anhand von 
    core/extension in MobileIDE, welche Interfaces/Facades bereits existieren 
    (siehe PROGRESS.md Eintrag "06-modularization-features-consolidation").

## Schritt 4: Dateinamens- und Text-Korrektur (xed/Xed → mobileide/MobileIDE)
- Durchsuche ALLE migrierten Dateien (Code, Ressourcen, Manifeste, Skripte, Strings, 
  Kommentare, Dateinamen selbst) nach den Mustern:
  "xed", "Xed", "XED", "Xed-Editor", "xed-editor"
- Ersetze konsistent:
  - "xed" (klein, Variablen/Dateinamen) -> "mobileide"
  - "Xed" (Klassen-/Titel-Schreibweise) -> "MobileIDE"
  - "XED" (Konstanten/ENV-Variablen) -> "MOBILEIDE"
  - "Xed-Editor" / "xed-editor" -> "MobileIDE" bzw. "mobileide" je nach Kontext
- Benenne betroffene DATEINAMEN selbst entsprechend um, 
  z. B. "XedTerminalActivity.kt" -> "MobileIDETerminalActivity.kt".
- Ausnahme: Belasse Verweise, die explizit als Quellangabe/Attribution in 
  Lizenz-/README-Kommentaren auf Xed-Editor verweisen (GPLv3-Konformität).

## Schritt 5: settings.gradle.kts & build.gradle.kts Korrektur
- Kommentiere ":features:proot", ":features:exec", ":features:lsp" in 
  settings.gradle.kts mit "// TODO Phase 2: entfernen" aus (NICHT löschen - das 
  passiert erst in Phase 2 des Gesamtvorhabens).
- Füge include-Statements für alle NEU migrierten Feature-Module hinzu 
  (inkl. ":features:terminal:mobileide-cli").
- Prüfe alle build.gradle.kts-Dateien im GESAMTEN MobileIDE-Projekt auf Referenzen 
  zu migrierten/umbenannten Modulen und korrigiere die project(":...")-Pfade.
- Übertrage fehlende Dependency-Deklarationen aus Xed-Editor build.gradle.kts-Dateien 
  in gradle/libs.versions.toml von MobileIDE (Namenskollisionen bei Versions-Aliases 
  vermeiden, ggf. mit Suffix versehen).

## Schritt 6: Build-Verifikation
- Führe "./gradlew build" aus, behebe alle Compile-Fehler iterativ.
- Führe abschließend aus: 
  grep -rli "xed" --include="*.kt" --include="*.xml" --include="*.gradle.kts" . 
  | grep -v "LICENSE\|README"
  → Ergebnis sollte leer sein (Ausnahme: bewusst belassene Attributions-Kommentare).

## Abschlussbericht
Gib aus:
- Liste aller migrierten Feature-Module (Xed-Pfad -> MobileIDE-Pfad)
- Bestätigung mobileide-cli-Umbenennung erfolgreich
- Anzahl korrigierter xed/Xed-Textvorkommen
- Liste aller umbenannten Dateien
- Bestätigung: "./gradlew build" erfolgreich
- Commit erstellen: "feat(phase1): migrate Xed-Editor feature modules to MobileIDE"

WICHTIG: Bevor du mit "Phase 2: Entfernung von proot/exec/lsp" fortfährst, 
warte auf explizite Bestätigung des Nutzers, dass diese Phase 1 zufriedenstellend 
abgeschlossen ist.