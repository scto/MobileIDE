# Ziel
Migriere/refaktoriere den vollständigen Inhalt von 
`~/Xed-Editor/core/main/src/main/java/com/rk/lsp` nach 
`~/MobileIDE/core/lsp/src/main/java/com/scto/mobile/ide/lsp`, inklusive Package-
Umbenennung, Import-Anpassung und aller notwendigen Folgeänderungen in bereits 
migrierten Konsumenten dieses Codes innerhalb von MobileIDE.

# Kontext (verifizierter Ist-Zustand von ~/MobileIDE)
- Das Zielmodul `:core:lsp` ist BEREITS in `settings.gradle.kts` aktiv eingebunden 
  (`include(":core:lsp")`) – im Gegensatz zu `:features:git` und `:core:commands`, 
  die dort auskommentiert sind. Das bedeutet, dieses Modul existiert und kompiliert 
  vermutlich bereits in irgendeiner Form; die Migration MUSS also den vorhandenen 
  Modul-Zustand respektieren, nicht blind überschreiben.

- STARKER INDIZ für den benötigten Umfang: Die bereits erfolgreich nach MobileIDE 
  migrierten 12 Plugin-Module (siehe `MIGRATION_STATUS.md`, alle Status "Vollständig") 
  referenzieren in ihrem Kotlin-Code weiterhin explizit die Original-Xed-Editor-
  Pakete `com.rk.lsp.LspRegistry`, `com.rk.lsp.ScriptedLspServer` und 
  `com.rk.lsp.LspConnectionConfig` (siehe z. B. `plugins/zig-lsp/app/src/main/java/
  io/kiquar/plugin/zig/Main.kt` und `ZigServer.kt`: 
  `import com.rk.lsp.LspRegistry`, `import com.rk.lsp.ScriptedLspServer`, 
  `import com.rk.lsp.LspConnectionConfig`). Diese Klassen sind Bestandteil der 
  Original-Xed-Editor-Extension-SDK (`sdk.jar`, `compileOnly`) und werden von JEDEM 
  der 12 Plugins zur Registrierung ihres jeweiligen Sprachservers benötigt.

- WICHTIGE KONSEQUENZ: Da alle 12 Plugins weiterhin gegen `com.rk.lsp.*` compileOnly-
  kompilieren (über `sdk.jar`, nicht gegen MobileIDE-eigene Klassen), ist zu klären, 
  ob (a) MobileIDE für Plugin-Kompatibilität dauerhaft eine kompatible `com.rk.lsp`-
  API-Fassade bereitstellen muss, oder (b) die Plugins in einem SEPARATEN, hier NICHT 
  zu bearbeitenden Folgeschritt ebenfalls auf `com.scto.mobile.ide.lsp` umgestellt 
  werden müssen. Dieser Prompt behandelt AUSSCHLIESSLICH die Migration des Core-
  Moduls selbst; die Entscheidung über Plugin-Kompatibilität ist in Stufe 0 zu 
  dokumentieren, aber NICHT eigenmächtig umzusetzen.

- Bereits vorhandenes, unmittelbar verwandtes Modul `:extension-languages` (siehe 
  status.md 2026-07-04) enthält bereits eigene Server-Implementierungen 
  (`KotlinLspServer.kt`, `JavaLspServer.kt`, `BashLspServer.kt`, `XmlLspServer.kt`), 
  die vermutlich VON den zu migrierenden Basisklassen (`ScriptedLspServer`, 
  `LspConnectionConfig`) ABLEITEN bzw. diese direkt nutzen. Diese Dateien MÜSSEN 
  während der Migration auf korrekte Imports aus dem neuen `com.scto.mobile.ide.lsp`-
  Package geprüft und ggf. angepasst werden, da sie sonst nach der Migration nicht 
  mehr kompilieren.

- Bereits vorhandene Utility-Konvention: `com.scto.mobile.ide.core.common.utils.
  NavigationUtils.safeNavigate` sowie konsolidiertes Logging über `core.common.utils.
  LogCatcher`/`LogEntry`/`LogConfigRepository` (siehe Fixing.md) – bei eventuell im 
  LSP-Code vorhandenen UI-Navigations- oder Logging-Aufrufen entsprechend anpassen.

- Bereits etablierte Migrationsmuster aus `migrate_xed.py`/`migrate_xed2.py`/
  `fix_xed.py` (Root-Skripte): Package-Mapping `com.rk.` → `com.scto.mobile.ide.`, 
  Textersetzung `Xed`/`xed`/`XED` → `MobileIDE`/`mobileide`/`MOBILEIDE`, AUSSER in 
  `LICENSE`- und `README.md`-Dateien (dort bewusst NICHT ersetzen, zur Wahrung der 
  Original-Attribution). Dieses Muster ist für die LSP-Migration exakt fortzuführen 
  (Package `com.rk.lsp` → `com.scto.mobile.ide.lsp`).

- Lizenz-Header-Konvention (siehe build.gradle.kts, settings.gradle.kts): GPLv3, 
  Copyright scto <tschmid35@gmail.com> – bei neu erstellten/migrierten Dateien im 
  Zielmodul konsistent fortführen, sofern im Zielmodul `:core:lsp` bereits eine 
  Header-Konvention existiert.

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER. Nach jeder Stufe: Gesamtprojekt bauen 
(Gradle), Fehlerfreiheit bestätigen, Ergebnis-Report mit geänderten/migrierten 
Dateien liefern, bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme (Pflicht vor jeder Code-Änderung)

0.1. Öffne `~/Xed-Editor/core/main/src/main/java/com/rk/lsp` und liste ALLE darin 
   enthaltenen Dateien vollständig auf (Klassen, Interfaces, Objects, Enums), inkl. 
   kurzer Beschreibung ihres Zwecks. Besonders zu identifizieren: `LspRegistry`, 
   `ScriptedLspServer`, `LspConnectionConfig` (bereits durch Plugin-Code als 
   existent bestätigt), sowie alle weiteren Begleitklassen (z. B. LSP-Server-
   Basis-Interface, Verbindungs-Typen wie `Process`/`Socket`/`TCP`, Installer- 
   Hilfsklassen, Status-Enums wie `isInstalled`/`isUpdatable`).

0.2. Öffne `~/MobileIDE/core/lsp/src/main/java/com/scto/mobile/ide/lsp` (bzw. den 
   tatsächlichen Pfad, falls das Package abweichend organisiert ist) und dokumentiere 
   den AKTUELLEN Ist-Zustand: Ist das Verzeichnis leer? Enthält es bereits 
   Teilimplementierungen? Falls ja: welche Klassen existieren bereits, und wie 
   verhalten sie sich zu den in 0.1 identifizierten Xed-Editor-Äquivalenten 
   (identisch / teilweise / komplett abweichend)?

0.3. Durchsuche das gesamte MobileIDE-Repository (insbesondere `:extension-languages`, 
   `:plugins:*`) nach allen Vorkommen von `com.rk.lsp` (Import-Statements, 
   qualifizierte Referenzen) und erstelle eine vollständige Liste aller Dateien, die 
   nach der Migration von einem Import-Pfad-Wechsel betroffen wären. Kennzeichne 
   dabei explizit:
   - Dateien innerhalb bereits migrierter MobileIDE-Kernmodule (`:extension-languages` 
     etc.) → MÜSSEN in Stufe 2 mit angepasst werden.
   - Dateien innerhalb der 12 Plugin-Module (`:plugins:*`) → NICHT in diesem Prompt 
     anfassen (siehe Nicht-Ziele), da diese laut `MIGRATION_STATUS.md` bereits als 
     "Vollständig" migriert markiert sind und gegen die separate Plugin-SDK 
     (`sdk.jar`, `compileOnly`) kompilieren, nicht gegen das Core-Modul selbst.

0.4. Liefere diese Bestandsaufnahme als eigenständigen Zwischen-Report, BEVOR mit 
   Stufe 1 fortgefahren wird. Kläre darin explizit die in "Kontext" beschriebene 
   Frage, ob eine Kompatibilitätsfassade für die 12 Plugins notwendig erscheint, 
   OHNE sie in diesem Auftrag umzusetzen.

---

## STUFE 1 – Kernmigration des Package-Inhalts

1.1. Kopiere/verschiebe jede Datei aus 
   `~/Xed-Editor/core/main/src/main/java/com/rk/lsp` nach 
   `~/MobileIDE/core/lsp/src/main/java/com/scto/mobile/ide/lsp`, unter Beibehaltung 
   der relativen Unterordnerstruktur (falls Unterpakete wie `com.rk.lsp.internal` 
   oder ähnliches existieren, entsprechend nach `com.scto.mobile.ide.lsp.internal` 
   spiegeln).

1.2. Passe in JEDER migrierten Datei die `package`-Deklaration von `com.rk.lsp...` 
   auf `com.scto.mobile.ide.lsp...` an.

1.3. Passe in JEDER migrierten Datei alle internen `import com.rk....`-Anweisungen 
   auf die entsprechenden `com.scto.mobile.ide....`-Äquivalente an (Package-Mapping-
   Muster wie in `migrate_xed.py`: `com.rk.` → `com.scto.mobile.ide.`), sofern die 
   referenzierten Klassen bereits an anderer Stelle in MobileIDE migriert vorliegen 
   (z. B. `com.rk.file.FileObject`, `com.rk.icons.Icon` – prüfe die tatsächlichen 
   Zielpfade dieser Abhängigkeiten in MobileIDE, falls vorhanden, ansonsten in Stufe 
   0.4 als offene Abhängigkeit vermerken).

1.4. Ersetze zusätzlich alle Textvorkommen von `Xed`/`xed`/`XED`/`Xed-Editor` in 
   Kommentaren, Log-Strings, Fehlermeldungen oder Dokumentations-Blöcken durch 
   `MobileIDE`/`mobileide`/`MOBILEIDE` (Ausnahme: falls eine `LICENSE`- oder 
   `README.md`-Datei im migrierten Verzeichnis vorhanden ist, dort NICHT ersetzen, 
   analog zu `fix_xed.py`/`migrate_xed.py`-Konvention).

1.5. Prüfe, ob in `~/MobileIDE/core/lsp/build.gradle.kts` alle von den migrierten 
   Klassen benötigten Abhängigkeiten (z. B. `lsp4j`, falls `LspConnectionConfig`/
   `ScriptedLspServer` auf LSP4J-Typen referenzieren, siehe Version-Catalog-Eintrag 
   `lsp4j = "1.0.0"` bzw. `org.eclipse.lsp4j:org.eclipse.lsp4j`) bereits vorhanden 
   sind; ergänze fehlende Dependencies konsistent zum bestehenden Root-Version-
   Catalog-Muster (`gradle/libs.versions.toml`).

1.6. Build-Verifikation ausschließlich für `:core:lsp` isoliert (z. B. 
   `./gradlew :core:lsp:compileDebugKotlin` bzw. äquivalent), BEVOR Konsumenten 
   angepasst werden.

---

## STUFE 2 – Anpassung bereits migrierter Konsumenten in MobileIDE

2.1. Öffne die in Stufe 0.3 identifizierten Dateien innerhalb von 
   `:extension-languages` (`KotlinLspServer.kt`, `JavaLspServer.kt`, 
   `BashLspServer.kt`, `XmlLspServer.kt`) und prüfe, ob diese tatsächlich von 
   `com.rk.lsp.ScriptedLspServer` erben bzw. `com.rk.lsp.LspConnectionConfig`/
   `com.rk.lsp.LspRegistry` referenzieren.

2.2. Falls ja: passe die betroffenen Import-Anweisungen auf 
   `com.scto.mobile.ide.lsp.ScriptedLspServer`/`LspConnectionConfig`/`LspRegistry` 
   an, sodass diese Klassen ab sofort aus dem neu migrierten `:core:lsp`-Modul 
   bezogen werden (dazu ggf. `:core:lsp` als Dependency in 
   `extension-languages/build.gradle.kts` ergänzen, falls noch nicht vorhanden).

2.3. Durchsuche zusätzlich `:core:main`, `:app` und weitere bereits aktive Module 
   (gemäß Stufe 0.3-Ergebnis) nach verbleibenden `com.rk.lsp`-Referenzen (z. B. 
   `terminalLauncher`-Delegate-Anbindung an `ScriptedLspServer`, siehe status.md 
   2026-07-04 "Decoupled Terminal Launcher for LSP" – diese Anbindung in 
   `MainActivity` MUSS ebenfalls auf den neuen Package-Pfad aktualisiert werden) und 
   korrigiere diese identisch.

2.4. Build-Verifikation des Gesamtprojekts (alle betroffenen Module: `:core:lsp`, 
   `:extension-languages`, `:core:main`, `:app`).

---

## STUFE 3 – Abschlussverifikation

3.1. Führe eine projektweite Suche nach verbleibenden Vorkommen von `com.rk.lsp` 
   AUSSERHALB der 12 Plugin-Module (`:plugins:*`, die laut Nicht-Ziele unangetastet 
   bleiben) durch und bestätige, dass keine übersehen wurden.

3.2. Vollständiger Gradle-Build des Gesamtprojekts, Fehlerfreiheit bestätigen.

3.3. Manuelle Funktionsverifikation: Starte MobileIDE, öffne die LSP-Settings 
   (`LspSettingsScreen`) und bestätige, dass die vier Kern-Sprachserver (Java, 
   Kotlin, Bash, XML) weiterhin korrekt Status/Install/Update-Aktionen ausführen 
   können (dies bestätigt, dass die Migration die Laufzeit-Funktionalität von 
   `ScriptedLspServer`/`LspConnectionConfig` nicht gebrochen hat).

3.4. Liefere einen finalen Report mit allen migrierten/geänderten Dateien, sowie der 
   in Stufe 0.4 dokumentierten offenen Empfehlung zur Plugin-Kompatibilitätsfrage.

# Nicht-Ziele
- Keine Änderung an den 12 bereits migrierten Plugin-Modulen (`:plugins:java-lsp`, 
  `:plugins:json-lsp`, `:plugins:kotlin-lsp`, `:plugins:kotlin-kmp-lsp`, 
  `:plugins:lua-lsp`, `:plugins:python-lsp`, `:plugins:typst-lsp`, `:plugins:go-lsp`, 
  `:plugins:rust-lsp`, `:plugins:zig-lsp`, `:plugins:fsharp-lsp`, 
  `:plugins:prettier-lsp`) – diese bleiben laut `MIGRATION_STATUS.md` unverändert auf 
  "Vollständig" und referenzieren weiterhin ihre eigene, separate `sdk.jar`-
  Abhängigkeit gegen `com.rk.lsp`. Eine etwaige spätere Anpassung dieser Plugins auf 
  `com.scto.mobile.ide.lsp` ist ausdrücklich NICHT Teil dieses Auftrags.
- Keine Entscheidung/Implementierung einer Kompatibilitätsfassade zwischen 
  `com.rk.lsp` (Plugin-SDK-Erwartung) und `com.scto.mobile.ide.lsp` (migriertes 
  Core-Modul) – diese Frage ist in Stufe 0.4 nur zu DOKUMENTIEREN, nicht zu lösen.
- Keine Änderung der Funktionslogik/des Verhaltens der migrierten Klassen – reine 
  Package-/Import-/Text-Migration, keine Refaktorierung der Geschäftslogik.
- Keine Änderung an `:extension-languages`-eigener Business-Logik jenseits der reinen 
  Import-Pfad-Korrektur.

# Akzeptanzkriterien
- Der vollständige Inhalt von `~/Xed-Editor/core/main/src/main/java/com/rk/lsp` liegt 
  identisch (strukturell und funktional) unter 
  `~/MobileIDE/core/lsp/src/main/java/com/scto/mobile/ide/lsp` vor, mit korrektem 
  Package-Namen und korrigierten internen Imports.
- `:core:lsp` kompiliert isoliert fehlerfrei.
- Alle bereits migrierten MobileIDE-Kernmodule (`:extension-languages`, `:core:main`, 
  `:app`), die zuvor gegen `com.rk.lsp` kompiliert haben, kompilieren nach der 
  Migration fehlerfrei gegen `com.scto.mobile.ide.lsp`.
- Die 12 Plugin-Module bleiben unverändert und weiterhin als "Vollständig" migriert.
- Die vier LSP-Kern-Sprachserver (Java, Kotlin, Bash, XML) funktionieren nach der 
  Migration nachweislich unverändert in der App-UI.
- Ein Gesamtprojekt-Build ist am Ende fehlerfrei.
