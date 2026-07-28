## Antigravity-CLI Prompt (final, mit Review-Gate)

Führe im Projekt "MobileIDE" (Gradle-Multi-Modul-Monorepo, Kotlin DSL) folgende 
Refactoring-Aufgabe vollständig und eigenständig durch. Das Projekt nutzt 
settings.gradle.kts mit den bestehenden Includes ":core:layout-preview" und 
":features:layout-preview". Analysiere zunächst gründlich, bevor du Änderungen vornimmst.

WICHTIG: Dieses Prompt ist in zwei PHASEN unterteilt. Nach PHASE 1 MUSST du explizit 
anhalten, einen Review-Bericht ausgeben und auf eine manuelle Bestätigung des Nutzers 
warten, bevor du mit PHASE 2 (destruktive Löschung) fortfährst. Führe NIEMALS eigenständig 
Löschbefehle aus core/layout-preview aus, ohne diese Bestätigung erhalten zu haben.

=====================================================================
PHASE 1: ANALYSE, ZUSAMMENFÜHRUNG, KORREKTUR & VERIFIKATION
=====================================================================

## Ausgangslage
Zwei Module existieren parallel und sollen zu EINEM zusammengeführt werden:
- QUELLE (wird später aufgelöst): :core:layout-preview      → Verzeichnis: core/layout-preview
- ZIEL (bleibt bestehen):          :features:layout-preview  → Verzeichnis: features/layout-preview

## Schritt 1: Analyse
- Lies vollständig core/layout-preview/build.gradle.kts UND features/layout-preview/build.gradle.kts.
- Vergleiche Package-Namen (vermutlich "com.scto.mobile.ide.core.layoutpreview" vs. 
  "com.scto.mobile.ide.features.layoutpreview"), Klassen, AndroidManifest.xml, Ressourcen 
  (res/drawable, res/values, res/layout) und Dependencies beider Module.
- Führe projektweit "grep -rn ':core:layout-preview'" über ALLE build.gradle.kts-Dateien 
  aus, um jedes Modul zu finden, das aktuell von :core:layout-preview abhängt 
  (implementation(project(":core:layout-preview"))).
- Identifiziere in :core:layout-preview alle Klassen/Funktionen, die es in 
  :features:layout-preview NICHT gibt (echte Zusatz-Logik) sowie Duplikate (bereits 
  vorhandene, äquivalente Implementierungen wie ComposePreviewScanner.kt, 
  LayoutPreviewRenderer.kt, LayoutPreviewBottomSheet.kt).
- Finde die Editor-Klasse "CodeEditScreen.kt" (Modul :app oder :editor) inkl. der bereits 
  vorhandenen TopAppBar-Preview-Icon-Integration (laut PROGRESS.md 2026-07-26 bereits 
  teilweise umgesetzt) und prüfe, ob sie auf :core:layout-preview oder :features:layout-preview 
  verweist.
- Dokumentiere ALLE Funde in einer Liste (Datei -> Fundstelle -> Art der Referenz), 
  diese Liste wird für den Abschlussbericht am Ende von Phase 1 benötigt.

## Schritt 2: Zusammenführung (core → features)
- Verschiebe/merge den GESAMTEN Inhalt aus core/layout-preview/src/... nach 
  features/layout-preview/src/..., inklusive:
  - Kotlin/Java-Quelldateien (Package-Deklaration dabei auf 
    "com.scto.mobile.ide.features.layoutpreview" umschreiben, alle betroffenen 
    Import-Statements im gesamten Projekt entsprechend anpassen)
  - Ressourcen (res/*), sofern nicht bereits identisch in features/layout-preview vorhanden
  - AndroidManifest.xml-Einträge (Components, Permissions), sofern zusätzlich benötigt
- WICHTIG: Lösche core/layout-preview NICHT in diesem Schritt. Die Quelldateien bleiben 
  vorerst als Kopie/Referenz erhalten, bis Phase 2 freigegeben wird.
- Bei Namenskollisionen (gleiche Klassennamen mit unterschiedlichem Inhalt): 
  behalte die vollständigere/aktuellere Implementierung aus :features:layout-preview 
  als Basis, übernimm fehlende Funktionalität aus :core:layout-preview per Merge in 
  dieselbe Datei, und kommentiere übernommene Abschnitte mit 
  "// merged from :core:layout-preview".
- Bei kollidierenden Ressourcen-IDs: präfixe die aus core übernommenen mit "core_legacy_".
- Übertrage alle in core/layout-preview/build.gradle.kts deklarierten Dependencies, die 
  in features/layout-preview/build.gradle.kts noch fehlen, in die build.gradle.kts von 
  :features:layout-preview.

## Schritt 3: Projektweite Referenzkorrektur
- Ersetze in ALLEN build.gradle.kts-Dateien jedes Vorkommen von 
  'implementation(project(":core:layout-preview"))' (bzw. api(...), testImplementation(...) 
  in gleicher Form) durch 'implementation(project(":features:layout-preview"))' 
  (Sichtbarkeitstyp jeweils beibehalten).
- Kommentiere in settings.gradle.kts die Zeile 'include(":core:layout-preview")' vorerst 
  nur AUS (nicht löschen), z. B.:
  // include(":core:layout-preview") // TODO: entfernen nach Freigabe Phase 2
  damit das alte Modul im Build nicht mehr aktiv ist, aber im Bedarfsfall reaktivierbar bleibt.
- Korrigiere projektweit alle Kotlin-Import-Statements, die auf 
  "com.scto.mobile.ide.core.layoutpreview.*" verweisen, auf 
  "com.scto.mobile.ide.features.layoutpreview.*".
- Baue das Projekt mit "./gradlew build" und behebe iterativ ALLE Compile-Fehler, 
  die durch die Umstrukturierung entstehen (fehlende Imports, verschobene Klassen, 
  fehlerhafte Dependency-Graphen zwischen :app, :editor, :features:layout-preview etc.), 
  bis der Build fehlerfrei durchläuft.

## Schritt 4: Icon-Integration im Editor sicherstellen
- Öffne die CodeEditScreen.kt (Modul :app bzw. :editor).
- Stelle sicher, dass in der TopAppBar ein Icon/IconButton existiert, das die 
  Layout-Preview-Funktion öffnet:
  - Falls bereits vorhanden aber auf :core:layout-preview referenziert: korrigiere den 
    Import/Aufruf auf die entsprechende Klasse aus :features:layout-preview.
  - Falls nicht vorhanden: implementiere einen IconButton mit Icons.Outlined.Preview 
    (oder passendem Material Icon), Tooltip "Layout-Vorschau", der 
    "LayoutPreviewBottomSheet" aus :features:layout-preview öffnet und den 
    @Composable-Scan via "ComposePreviewScanner.kt" auf der aktuell geöffneten Datei anstößt.
  - Stelle sicher, dass in der build.gradle.kts des Moduls, das CodeEditScreen.kt enthält, 
    'implementation(project(":features:layout-preview"))' korrekt eingetragen ist.

## Schritt 5: Vollständige Build- und Funktionsverifikation
- Führe "./gradlew :features:layout-preview:build" sowie einen kompletten 
  "./gradlew build" aus.
- Stelle sicher, dass KEIN Modul im Projekt mehr aktiv ":core:layout-preview" referenziert 
  (finaler grep-Check über build.gradle.kts-Dateien muss leer sein; das auskommentierte 
  Include in settings.gradle.kts zählt dabei NICHT als aktive Referenz).
- Prüfe projektweit, dass keine verbliebenen Imports auf 
  "com.scto.mobile.ide.core.layoutpreview" mehr existieren.

## Schritt 6: STOPP – Review-Bericht (Ende Phase 1)
Gib exakt in dieser Struktur einen Bericht aus und HALTE DANACH AN:

--- REVIEW-CHECKLISTE VOR LÖSCHUNG ---
[ ] Strukturvergleich: Alle core-Dateien in Ziel übernommen? (Liste der verschobenen Dateien)
[ ] Package-Namen korrekt umgestellt? (Ja/Nein)
[ ] Ressourcen-Kollisionen gelöst? (Liste betroffener IDs, falls vorhanden)
[ ] settings.gradle.kts: include(":core:layout-preview") auskommentiert? (Ja/Nein + Zeilennummer)
[ ] grep-Check build.gradle.kts auf ":core:layout-preview" -> Ergebnis leer? (Ja/Nein)
[ ] grep-Check Imports auf "core.layoutpreview" -> Ergebnis leer? (Ja/Nein)
[ ] features/layout-preview/build.gradle.kts enthält übernommene Dependencies? (Liste)
[ ] CodeEditScreen.kt: Icon vorhanden, korrekt verdrahtet, Tooltip "Layout-Vorschau"? 
    (Datei + Zeilennummer angeben)
[ ] ./gradlew :features:layout-preview:build -> BUILD SUCCESSFUL? (Ja/Nein)
[ ] ./gradlew build (gesamt) -> BUILD SUCCESSFUL? (Ja/Nein)
[ ] Manueller Funktionstest empfohlen: Datei mit @Composable öffnen, Preview-Icon 
    antippen, Bottom Sheet prüft Rendering (< 10s laut PROGRESS.md), kein Crash

BITTE BESTÄTIGE EXPLIZIT MIT "PHASE 2 FREIGEBEN", DAMIT ICH DAS ALTE MODUL 
":core:layout-preview" ENDGÜLTIG LÖSCHEN KANN. Ohne diese Bestätigung führe ich 
KEINE weiteren Schritte aus.

=====================================================================
PHASE 2: LÖSCHUNG (NUR NACH EXPLIZITER BESTÄTIGUNG "PHASE 2 FREIGEBEN")
=====================================================================

## Schritt 7: Endgültige Löschung des Moduls :core:layout-preview
- Entferne die zuvor auskommentierte Zeile 'include(":core:layout-preview")' 
  jetzt vollständig und final aus settings.gradle.kts.
- Falls core/layout-preview als echtes Git-Submodul via .gitmodules eingebunden ist:
    git submodule deinit -f core/layout-preview
    git rm -f core/layout-preview
    rm -rf .git/modules/core/layout-preview
  Falls es sich lediglich um ein reguläres Verzeichnis im Monorepo handelt (kein 
  eigenständiges Submodul-Repo), lösche es stattdessen direkt:
    git rm -r core/layout-preview
- Führe abschließend erneut "./gradlew build" aus, um sicherzustellen, dass nach der 
  Löschung alles weiterhin fehlerfrei baut.

## Schritt 8: Commits
Committe in sinnvollen, atomaren Schritten:
1. "refactor: merge :core:layout-preview into :features:layout-preview"
2. "fix: update all module references from core:layout-preview to features:layout-preview"
3. "feat: ensure layout-preview icon integration in CodeEditScreen toolbar"
4. "chore: remove obsolete :core:layout-preview module and submodule"

## Schritt 9: Finaler Abschlussbericht
Gib eine strukturierte Zusammenfassung aus:
- Welche Dateien aus core/layout-preview wurden übernommen/gemergt (Liste)
- Welche Namens-/Ressourcenkonflikte wie gelöst wurden
- Welche build.gradle.kts-Dateien angepasst wurden (Liste mit betroffener Zeile)
- Bestätigung, dass settings.gradle.kts den Include für :core:layout-preview nicht mehr enthält
- Wo genau (Datei + Zeile) das Icon in CodeEditScreen.kt final verdrahtet ist
- Bestätigung: finaler Build erfolgreich, core/layout-preview vollständig entfernt
- Liste der erstellten Commits mit Hash

Führe alle Schritte innerhalb jeder Phase nacheinander aus, verifiziere nach jedem 
größeren Schritt den Build-Status per "./gradlew build", und halte STRIKT am Review-Gate 
zwischen Phase 1 und Phase 2 an.
