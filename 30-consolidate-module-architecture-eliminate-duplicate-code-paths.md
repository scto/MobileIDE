### Prompt-Datei: `30-consolidate-module-architecture-eliminate-duplicate-code-paths.md`

```
# Ziel
Konsolidiere die MobileIDE-Modularchitektur (com.scto.mobile.ide), die durch drei
überlappende Migrationswellen (Xed-Editor → WebIDE → MobileIDE) an mehreren Stellen
DUPLIZIERTE oder GEISTERHAFTE Code-Pfade für dieselbe Funktionalität enthält (Git,
LSP, Terminal-Submodule, Extensions). Dies ist die Ursache dafür, dass KI-Agenten bei
Bugfix-Prompts nicht konsistent den "richtigen" Ort patchen, weil pro Feature teils
zwei bis drei parallele Implementierungen gleichzeitig existieren.

WICHTIG: Dies ist ein reiner STRUKTUR-Prompt. Es werden KEINE neuen Features
gebaut. Ziel ist ausschließlich: pro Funktionsbereich existiert danach GENAU EIN
Modul an GENAU EINEM Ort, alle Referenzen sind korrekt, der Build ist grün.

# Verifizierter Ist-Zustand (aus settings.gradle.kts, migrate_xed.py, migrate_xed2.py,
# fix_xed.py, PROGRESS.md, MIGRATION_STATUS.md, README.md)

```

include(":app",":editor",":editor-lsp",":language-treesitter")

include(":core:main")
include(":core:components")
include(":core:resources")
include(":core:runner")
include(":core:apk-builder")
include(":core:tooling:tooling-api")
include(":core:tooling:tooling-impl")
include(":core:tooling:tooling-server")
include(":features:extensions")
include(":core:lsp")
include(":core:commands")
include(":extension-languages")          // Top-Level, KEIN :core/:features Präfix!
include(":plugins:java-lsp")             // ... + 11 weitere LSP-Plugins
include(":core:common")
include(":features:layout-preview")

// TODO Phase 2: entfernen
include(":features:terminal")

// include(":features:git")
// include(":features:runner")
// include(":features:terminal:mobileide-cli")
// include(":features:terminal:proot")
// include(":features:terminal:link2symlink")

include(":features:plugin-store")

```

Zusätzlich per Dateisystem-Analyse bestätigt (`migrate_xed.py`/`migrate_xed2.py`):
- `features/git/` existiert AUF DER PLATTE, ist aber NICHT in `settings.gradle.kts`
  eingebunden (auskommentiert). `migrate_xed.py` verarbeitet dieses Verzeichnis aktiv
  (Package-Rename `com.rk.git` → `com.scto.mobile.ide.features.git`).
- `features/runner/` existiert analog auf der Platte, ebenfalls auskommentiert.
- `features/terminal/xed-cli/` wurde von `migrate_xed.py` bereits zu
  `features/terminal/mobileide-cli/` umbenannt – korrespondiert mit der
  auskommentierten Zeile `:features:terminal:mobileide-cli`.
- `features/extensions_migrated/` existiert als SEPARATES Verzeichnis neben dem
  aktiven `features/extensions/` (laut MIGRATION_STATUS.md "Vollständig" migriert) –
  vermutlich ein nie zusammengeführter Zwischenstand.
- Gemäß PROGRESS.md (Prompt 17, 2026-07-27) liegt die tatsächlich AKTIVE, in der App
  benutzte Git-Implementierung (`GitConfigDialog.kt`, `GitViewModel.kt`, `GitPanel.kt`,
  `GitConflictParser.kt`/`GitConflictManager.kt`/`GitConflictResolutionDialog.kt`) im
  `:app`-Modul (`app/src/main/java/com/scto/mobile/ide/...`), NICHT im auskommentierten
  `features/git/`. Das Verzeichnis `features/git/` ist somit eine reine Xed-
  Migrations-Leiche.
- LSP ist auf VIER Orte verteilt: `:core:lsp` (Low-Level-Protokoll), `:editor-lsp`
  (Editor-Bindung), `:extension-languages` (eingebautes Bundle für Kotlin/Java/Bash/
  XML, laut status.md 2026-07-04 mit "Custom Edge server wrapper" für Kotlin), UND
  `:plugins:kotlin-lsp` + `:plugins:java-lsp` (separat installierbare Plugin-Pakete
  für DIESELBEN Sprachen). Dies erklärt direkt den zuvor gefixten Kotlin-LSP-
  Installationsprüfungs-/Absturz-Bug: zwei parallele Registrierungspfade für
  dieselbe Sprache.
- `migrate_xed.py`, `migrate_xed2.py`, `fix_xed.py` liegen aktiv im Projekt-Root mit
  hartkodierten Absolutpfaden (`/data/data/com.termux/files/home/MobileIDE/...`).

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER. Nach JEDER Stufe: Gesamtprojekt bauen
(Gradle), Fehlerfreiheit bestätigen, Ergebnis-Report mit geänderten/verschobenen/
gelöschten Dateien liefern, bevor die nächste Stufe beginnt. Committe nach jeder
Stufe separat (falls Git-Zugriff besteht), damit jede Konsolidierungsstufe einzeln
zurückrollbar ist.

---

## STUFE 0 – Vollständige Bestandsaufnahme & Diff-Analyse (Pflicht, keine Code-Änderung)

0.1. Erstelle eine vollständige Dateibaum-Auflistung von `features/git/`,
   `features/runner/`, `features/extensions_migrated/`,
   `features/terminal/mobileide-cli/`, `features/terminal/proot/` (falls vorhanden),
   `features/terminal/link2symlink/` (falls vorhanden). Dokumentiere Dateianzahl,
   Package-Deklarationen (bereits migriert auf `com.scto.mobile.ide.*` oder noch auf
   `com.rk.*`?) und letzten inhaltlichen Änderungsstand pro Verzeichnis.

0.2. Vergleiche `features/git/` (Xed-Migrations-Leiche) MIT der tatsächlich aktiven
   Git-Implementierung in `:app` (Dateien wie `GitConfigDialog.kt`, `GitViewModel.kt`,
   `GitPanel.kt`, `GitConflictParser.kt`, `GitConflictManager.kt`,
   `GitConflictResolutionDialog.kt` – exakte Pfade via Grep ermitteln). Stelle fest:
   Ist `features/git/` komplett redundant (reiner alter Xed-Code, den man löschen
   kann), oder enthält es Funktionalität, die im `:app`-Code FEHLT und übernommen
   werden müsste (z. B. ältere JGit-Wrapper-Utilities)?

0.3. Vergleiche `features/runner/` MIT `:core:runner` UND `:core:apk-builder`.
   Stelle fest, ob `:core:runner` (Low-Level-Prozessausführung) und
   `:core:apk-builder` (AAPT2/D8/Signing) tatsächlich getrennte Verantwortlichkeiten
   haben, oder ob signifikante Logik-Überlappung besteht. Stelle fest, ob
   `features/runner/` reine Xed-Leiche ist oder UI-/State-Logik enthält, die in
   `:core:runner` fehlt.

0.4. Vergleiche `features/extensions_migrated/` MIT `features/extensions/`
   (Zieldateiweise Diff: identische Dateien, nur in `extensions/` vorhandene Dateien,
   nur in `extensions_migrated/` vorhandene Dateien).

0.5. Vergleiche die Kotlin-LSP-Installationsprüfung/Registrierung in
   `:extension-languages` (`KotlinLspServer.kt` o. ä.) MIT der Installationsprüfung/
   Registrierung in `:plugins:kotlin-lsp`. Dokumentiere exakt: Prüfen beide denselben
   Installationspfad? Registrieren beide sich in derselben zentralen LSP-Registry
   (welche Registry-Klasse genau)? Gleiches für Java (`:extension-languages` vs.
   `:plugins:java-lsp`).

0.6. Prüfe `features/terminal/mobileide-cli/`, `features/terminal/proot/`,
   `features/terminal/link2symlink/` gegen den bereits laut PROGRESS.md (Prompt 15,
   "vollständig konsolidiert") in `:features:terminal` vorhandenen Code. Stelle fest,
   ob diese drei Verzeichnisse bereits 1:1 in `:features:terminal` eingebettet sind
   (dann sind sie reine Leichen zum Löschen) oder ob sie noch nicht integrierte
   CLI-Helper/PRoot-Low-Level-Bindings/Symlink-Utilities enthalten, die aktuell
   fehlen.

0.7. Liefere einen vollständigen Bestandsaufnahme-Report mit einer klaren
   EMPFEHLUNG pro Punkt ("LÖSCHEN", "MERGEN NACH X", "ALS EIGENES MODUL AKTIVIEREN"),
   BEVOR mit Stufe 1 fortgefahren wird. Bei Unklarheiten (z. B. wenn Diffs
   uneindeutig sind) explizit nachfragen statt zu raten.

---

## STUFE 1 – Git-Feature konsolidieren

1.1. Basierend auf Stufe 0.2: Falls `features/git/` reine Leiche ist – lösche das
   Verzeichnis vollständig. Aktiviere `:features:git` NEU in `settings.gradle.kts`
   als LEERES Modul-Skelett (Standard `build.gradle.kts` für ein Compose-fähiges
   Android-Library-Modul, analog zu `:features:layout-preview`).

1.2. Verschiebe SÄMTLICHEN aktuell in `:app` liegenden Git-Code
   (`GitConfigDialog.kt`, `GitViewModel.kt`, `GitPanel.kt`, `GitToolbarCompact.kt`,
   `GitConflictParser.kt`, `GitConflictManager.kt`, `GitConflictResolutionDialog.kt`,
   sowie alle weiteren mit `Git*` benannten Dateien im `:app`-Modul) nach
   `:features:git`, mit korrektem Package `com.scto.mobile.ide.features.git`.

1.3. Passe alle Imports/Referenzen im `:app`-Modul an, die auf die verschobenen
   Git-Klassen zugreifen (Navigation-Graph, DI-Wiring, `CodeEditScreen.kt` Git-Menü-
   Einträge etc.). Füge `:features:git` als Dependency in `app/build.gradle.kts`
   hinzu.

1.4. Build- & Funktionsverifikation: Öffne den Git-Konfigurationsdialog, teste
   Commit/Push/Pull-Workflow, bestätige unveränderte Funktionalität nach der
   Verschiebung (reine Struktur-Migration, kein Verhaltens-Unterschied erwartet).

---

## STUFE 2 – Runner/Build-Feature konsolidieren

2.1. Basierend auf Stufe 0.3: Falls `features/runner/` reine Leiche ist (keine
   Logik, die nicht bereits in `:core:runner`/`:core:apk-builder` existiert) –
   lösche das Verzeichnis vollständig, entferne die auskommentierte Zeile
   ersatzlos aus `settings.gradle.kts`.

2.2. Falls `features/runner/` tatsächlich UI-/State-Logik enthält, die aktuell
   fehlt: Erstelle ein neues Modul `:features:build` (klarere Namensgebung als
   "runner", um Verwechslung mit `:core:runner` zu vermeiden), verschiebe die
   fehlende UI-/State-Logik dorthin, mit `:core:runner` als Low-Level-Dependency.

2.3. Build- & Funktionsverifikation: Teste den Build/Run-Workflow (Play-Button in
   `CodeEditScreen.kt`) nach der Konsolidierung, bestätige unveränderte Funktion.

---

## STUFE 3 – Terminal-Submodule bereinigen

3.1. Basierend auf Stufe 0.6: Für jedes der drei Verzeichnisse
   (`mobileide-cli`, `proot`, `link2symlink`) einzeln entscheiden:
   - Falls bereits 1:1 in `:features:terminal` eingebettet: Verzeichnis löschen,
     auskommentierte `include()`-Zeile ersatzlos entfernen.
   - Falls NICHT integrierte, aber weiterhin benötigte Funktionalität enthalten:
     als normale Kotlin-Dateien/Package innerhalb von `:features:terminal`
     einbetten (NICHT als eigenes Submodul – vermeidet weitere Modul-Fragmentierung
     für ein bereits laut PROGRESS.md konsolidiertes Feature).

3.2. Entferne den `// TODO Phase 2: entfernen`-Kommentar über `:features:terminal`
   aus `settings.gradle.kts` (bezieht sich nach dieser Bereinigung auf nichts mehr).

3.3. Build- & Funktionsverifikation: Terminal-Sessions, PTY-Rendering, Sandbox-
   Setup-Flow weiterhin uneingeschränkt funktionsfähig.

---

## STUFE 4 – LSP-Architektur konsolidieren (kritischste Stufe)

4.1. Treffe die Architektur-Entscheidung EXPLIZIT und dokumentiere sie im
   Report: Kotlin und Java werden AUSSCHLIESSLICH über das eingebaute Bundle
   (`:extension-languages`, umzubenennen/zu verschieben nach `:features:lsp`, siehe
   4.2) bereitgestellt. Die separaten Plugin-Pakete `:plugins:kotlin-lsp` und
   `:plugins:java-lsp` werden aus `settings.gradle.kts` entfernt UND ihre
   Verzeichnisse aus `plugins/` gelöscht (Begründung: Vermeidung doppelter
   Installationsprüfungs-Pfade, die direkte Ursache der bereits behobenen Kotlin-
   LSP-Bugs war). Falls eine spätere externe Plugin-Distribution für Kotlin/Java
   gewünscht ist, muss dies über den in Stufe 4.4 vorbereiteten `:features:plugin-
   store`-Mechanismus erfolgen, NICHT über ein parallel eingebautes Bundle.

4.2. Erstelle ein neues Modul `:features:lsp`. Verschiebe dorthin:
   - Den kompletten Inhalt von `:extension-languages` (KotlinLspServer.kt,
     JavaLspServer.kt, BashLspServer.kt, XmlLspServer.kt, inkl. aller
     Installationsprüfungs-/Installationsskript-Anbindungslogik).
   - Den kompletten Inhalt von `:editor-lsp` (Editor-Hover/Diagnostics/Go-to-
     Definition-Bindung).
   Package-Namespace einheitlich auf `com.scto.mobile.ide.features.lsp` bringen.
   Entferne `:extension-languages` und `:editor-lsp` aus `settings.gradle.kts`.

4.3. `:core:lsp` bleibt AUSSCHLIESSLICH als generischer, sprachunabhängiger
   Low-Level-Protokoll-Client (JSON-RPC-Client-Interfaces, lsp4j-Wrapper-Basis-
   klassen) bestehen. Entferne jegliche sprachspezifische Logik, die versehentlich
   in `:core:lsp` liegt, nach `:features:lsp`.

4.4. Ergänze in `:features:lsp` einen klar markierten Erweiterungspunkt
   (Interface `LanguageServerProvider`, analog zur bereits in Stufe 2 des vorherigen
   Bugfix-Prompts vorbereiteten `ExtensionActionHandler`-Abstraktion), über den
   zukünftig zusätzliche, über `:features:plugin-store` nachinstallierte Sprach-
   server registriert werden könnten, OHNE erneut einen parallelen, konkurrierenden
   Installationsprüfungs-Pfad zu erzeugen.

4.5. Build- & Funktionsverifikation: Öffne je eine Datei pro unterstützter Sprache
   (Kotlin, Java, Bash, XML), bestätige korrekte LSP-Aktivierung (Diagnostics,
   Hover, Go-to-Definition) ohne doppelte/widersprüchliche Installationsdialoge.

---

## STUFE 5 – Extensions-Migration final zusammenführen

5.1. Basierend auf Stufe 0.4: Merge alle in `features/extensions_migrated/`
   vorhandenen, in `features/extensions/` FEHLENDEN Dateien final in
   `features/extensions/`. Bei Konflikten (identischer Dateiname, unterschiedlicher
   Inhalt): den inhaltlich neueren/vollständigeren Stand behalten (anhand
   Datei-Zeitstempel oder Code-Vollständigkeit entscheiden, im Report begründen).

5.2. Lösche `features/extensions_migrated/` vollständig nach erfolgreichem Merge.

5.3. Build- & Funktionsverifikation: Plugin-Loader/Runtime-Funktionalität
   (`:features:extensions`) weiterhin uneingeschränkt funktionsfähig.

---

## STUFE 6 – Settings-Struktur konsolidieren

6.1. Ermittle, ob aktuell tatsächlich mehrere, unabhängige Settings-UI-Quellen
   (Terminal-Settings, Git-Settings, LSP-Settings, Editor-Settings, Build-Settings)
   direkt im `:app`-Modul verteilt liegen, OHNE zentrale Aggregations-Struktur.
   Prüfe insbesondere, ob dies die Ursache des in einem vorherigen Bugfix-Prompt
   beobachteten Git-Settings-Endlos-Wiederholungsbugs war (mehrere Settings-Quellen
   liefern parallel Inhalte in dieselbe Liste).

6.2. Falls bestätigt: Erstelle ein neues Modul `:features:settings` als zentralen
   Host-Screen, der die einzelnen, jeweils aus ihrem eigenen Feature-Modul
   stammenden Settings-Fragmente (Terminal-Settings aus `:features:terminal`,
   Git-Settings aus `:features:git`, LSP-Settings aus `:features:lsp`) über ein
   klares `SettingsSection`-Interface aggregiert, OHNE Duplizierung derselben
   Sektion.

6.3. Build- & Funktionsverifikation: Öffne den Git-Settings-Screen erneut, bestätige
   dass jede Sektion (General/Account/Repository) exakt EINMAL erscheint (Regressions-
   test für den bereits in einem vorherigen Prompt gemeldeten Wiederholungsbug).

---

## STUFE 7 – Migrations-Skripte archivieren

7.1. Verschiebe `migrate_xed.py`, `migrate_xed2.py`, `fix_xed.py` vom Projekt-Root
   nach `scripts/migrations/archive/`, NACHDEM verifiziert wurde, dass keine der in
   Stufe 1–5 verschobenen/gelöschten Verzeichnisse noch von diesen Skripten für eine
   künftige, versehentliche erneute Ausführung referenziert werden könnte (die
   Skripte enthalten hartkodierte, jetzt teils ungültige Pfade wie
   `features/git`, `features/runner`, `features/extensions_migrated`).

7.2. Ergänze am Kopf jeder archivierten Datei einen Kommentar-Block:
   ```python
   # ARCHIVIERT am [Datum] – einmalig ausgeführtes Xed→MobileIDE Migrationsskript.
   # NICHT erneut ausführen. Referenzierte Pfade sind nach der Modul-Konsolidierung
   # (Prompt 30) teilweise ungültig/gelöscht. Nur zu historischen Zwecken behalten.
   ```

---

## STUFE 8 – README/Dokumentation als Single Source of Truth wiederherstellen

8.1. Aktualisiere `README.md` UND `README_DE.md` (Projektstruktur-Abschnitt), sodass
   sie exakt den NACH dieser Konsolidierung tatsächlich in `settings.gradle.kts`
   vorhandenen Modulen entsprechen: `:features:git`, `:features:lsp` (ersetzt
   `:editor-lsp` + `:extension-languages`), ggf. `:features:build`,
   `:features:settings`. Entferne veraltete Erwähnungen von `:editor-lsp` als
   separatem Modul, falls in Stufe 4 zusammengeführt.

8.2. Aktualisiere `MIGRATION_STATUS.md` um einen neuen Abschnitt "Modul-
   Konsolidierung (Prompt 30)" mit einer Tabelle: Alter Zustand → Neuer Zustand →
   Status, analog zum bestehenden Tabellenformat für die Plugin-Migrationen.

8.3. Ergänze `PROGRESS.md` mit einem neuen datierten Eintrag, der diese
   Konsolidierungsstufe vollständig dokumentiert (analog zu den bestehenden
   Einträgen für Prompt 15, 17, 21 etc.).

---

## STUFE 9 – Abschlussverifikation

9.1. Vollständiger Gradle-Build ALLER Module (`./gradlew build` bzw. äquivalent),
   inkl. Ausführung von `verifyCatalogConsistency` (bereits in `build.gradle.kts`
   vorhanden).

9.2. Grep-basierte Restprüfung: Suche projektweit nach `com.rk.` (alte Xed-Package-
   Präfixe), die evtl. in den verschobenen/gemergten Dateien übersehen wurden.
   Suche nach verbleibenden Referenzen auf gelöschte Pfade (`features/git` außerhalb
   von `:features:git`, `extension-languages`, `editor-lsp`).

9.3. Manuelle Durchklick-Bestätigung: Git-Workflow, LSP-Aktivierung (alle 4
   Sprachen), Terminal-Sessions, Settings-Screens (Terminal/Git/LSP), Extensions-
   Laden – alle unverändert funktionsfähig nach der reinen Struktur-Migration.

9.4. Liefere einen finalen Gesamt-Report mit:
   - Tabelle aller verschobenen/umbenannten/gelöschten Verzeichnisse (Alt → Neu)
   - Finaler, vollständiger Inhalt des neuen `settings.gradle.kts`-`include()`-Blocks
   - Bestätigung, dass jedes der ursprünglich 10 identifizierten Doppelspurigkeits-
     Probleme (Terminal-Submodule, LSP-Vierfachverteilung, Git/Runner-Geister,
     Extensions-Zwischenstand, Migrations-Skript-Altlast, README-Drift,
     Settings-Fragmentierung) einzeln als GELÖST markiert ist.

# Nicht-Ziele
- Keine Änderung der grundsätzlichen fachlichen Logik/des Verhaltens von Git-,
  LSP-, Terminal- oder Extension-Funktionen – ausschließlich Verschieben,
  Umbenennen, Zusammenführen von Code an den korrekten Modul-Ort.
- Keine Implementierung neuer Features (Plugin-Store-Marktplatz-UI bleibt Stub).
- Keine Änderung der Versionsverwaltung/CI-Pipeline außer ggf. Anpassung von
  Modul-Referenzen in `main-build-test.yml`, falls dort explizite Modulnamen
  (`:extension-languages`, `:editor-lsp`) referenziert werden.

# Akzeptanzkriterien
- `settings.gradle.kts` enthält für Git, LSP, Terminal, Extensions jeweils GENAU
  EIN aktives Modul, keine auskommentierten Geister-Includes mehr.
- Keine Verzeichnisse `features/git`, `features/runner`, `features/extensions_
  migrated`, `features/terminal/mobileide-cli|proot|link2symlink` existieren mehr
  außerhalb ihrer konsolidierten Ziel-Module.
- `:extension-languages` und `:editor-lsp` existieren nicht mehr als separate
  Module – vollständig in `:features:lsp` zusammengeführt.
- `:plugins:kotlin-lsp` und `:plugins:java-lsp` sind entfernt (Kotlin/Java nur noch
  über `:features:lsp` bereitgestellt).
- `migrate_xed.py`, `migrate_xed2.py`, `fix_xed.py` liegen archiviert unter
  `scripts/migrations/archive/`, nicht mehr im Projekt-Root.
- `README.md`/`README_DE.md`/`MIGRATION_STATUS.md`/`PROGRESS.md` spiegeln exakt den
  neuen, konsolidierten Ist-Zustand wider.
- Gesamtprojekt-Build ist am Ende fehlerfrei, alle manuellen Funktionsverifikationen
  aus Stufe 9.3 sind erfolgreich.
```

**Hinweis:** Dies ist bewusst der aufwändigste Prompt bisher (9 Stufen statt der üblichen 4–5), weil eine fehlerhafte Modul-Konsolidierung selbst neue, schwer auffindbare Bugs erzeugen kann. Lass ihn NICHT in einem einzelnen Agenten-Durchlauf ohne Zwischen-Reports laufen – die Stufe-0-Bestandsaufnahme (Diff-Analyse) ist hier besonders kritisch, da sie entscheidet, ob z. B. `features/git/` sicher gelöscht werden kann oder noch wertvolle Logik enthält.
