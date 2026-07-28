Führe im Projekt ~/MobileIDE folgende Aufgabe durch. Dies ist PHASE 2 des 
Xed-Editor-Migrationsvorhabens. 

VORAUSSETZUNG: Phase 1 (Migration der Xed-Editor Feature-Module, siehe 
"01-migrate-xed-features-to-mobileide.md") muss bereits abgeschlossen sein - 
prüfe dies, indem du verifizierst, dass in settings.gradle.kts die Zeilen 
":features:proot", ":features:exec", ":features:lsp" bereits mit 
"// TODO Phase 2: entfernen" auskommentiert sind. Falls NICHT, breche ab und 
weise darauf hin, dass zuerst Phase 1 ausgeführt werden muss.

Erstelle zu Beginn einen Sicherungs-Branch:
  git checkout -b backup/before-remove-legacy-modules && git checkout main

## Schritt 1: Analyse vor Löschung
- Prüfe projektweit, wer aktuell ":features:proot", ":features:exec", ":features:lsp" 
  als Dependency referenziert (grep über alle build.gradle.kts).
- Stelle sicher, dass die in Phase 1 migrierten Xed-Editor-Äquivalente (falls 
  Xed-Editor eigene proot/exec/lsp-Handling-Module besitzt) bereits als Ersatz 
  eingebunden sind, ODER dass die Funktionalität (ProotSandbox-Interface, 
  ShellUtils-Facade laut PROGRESS.md "06-modularization-features-consolidation") 
  bereits vollständig durch die neuen/verbliebenen Module abgedeckt ist.
- Falls NICHT vollständig abgedeckt: übernimm die fehlenden Klassen aus den alten 
  Modulen :features:proot / :features:exec / :features:lsp direkt in das jeweils 
  passende neu migrierte Modul (Package-Pfade anpassen), BEVOR du löschst.
- Dokumentiere diese Analyse in einer Übernahme-Liste (Datei -> übernommen nach).

## Schritt 2: Löschung
- Entferne die Verzeichnisse features/proot, features/exec, features/lsp vollständig:
    git rm -r features/proot features/exec features/lsp
- Entferne final die zuvor auskommentierten include-Zeilen aus settings.gradle.kts.
- Entferne alle verbliebenen Dependency-Referenzen auf diese drei Module aus 
  sämtlichen build.gradle.kts-Dateien im Projekt.

## Schritt 3: Build-Verifikation
- Führe "./gradlew build" aus, behebe iterativ alle Fehler, die durch die Löschung 
  entstehen (fehlende Imports, kaputte Interface-Implementierungen).

## Abschlussbericht
Gib aus:
- Bestätigung: proot/exec/lsp Verzeichnisse entfernt
- Liste ggf. übernommener Restfunktionalität (Datei -> Zielmodul)
- Bestätigung: "./gradlew build" erfolgreich
- Commit erstellen: "chore(phase2): remove legacy proot/exec/lsp modules"

WICHTIG: Warte auf explizite Bestätigung des Nutzers, bevor du mit 
"Phase 3: Terminal-Ersatz + Multi-Distro" fortfährst.