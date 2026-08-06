# Ziel
Migriere/refaktoriere das Verzeichnis 
`Xed-Editor/features/terminal/src/main/java/com/rk/lsp` 
nach 
`MobileIDE/features/terminal/src/main/java/com/scto/mobile/ide/features/terminal/lsp` 
inklusive vollständiger Package-Umbenennung, Anpassung aller Imports/Referenzen 
innerhalb von MobileIDE, und Sicherstellung, dass alle bereits migrierten Plugin-
Module (java-lsp, json-lsp, kotlin-lsp, kotlin-kmp-lsp, lua-lsp, python-lsp, 
typst-lsp, go-lsp, rust-lsp, zig-lsp, fsharp-lsp, prettier-lsp), die auf die darin 
enthaltenen Klassen (`LspRegistry`, `ScriptedLspServer`, `LspConnectionConfig` etc.) 
verweisen, weiterhin korrekt kompilieren und funktionieren.

# Kontext (verifizierter Ist-Zustand von ~/MobileIDE, Stand settings.gradle.kts/
status.md/PROGRESS.md/MIGRATION_STATUS.md)
- rootProject.name = "MobileIDE", Basis-Package: com.scto.mobile.ide
- `:features:terminal` ist im Gegensatz zu `:features:git` und `:core:commands` 
  BEREITS AKTIV in settings.gradle.kts eingebunden (`include(":features:terminal")`) 
  und laut PROGRESS.md (Prompt 15, "Consolidate Terminal Features Module", 
  2026-07-26) bereits vollständig konsolidiert (Session-Backend, ANSI/PTY-Emulator, 
  View-Rendering, TerminalScreen/-SettingsScreen, TerminalService, Sandbox-Assets).
- WICHTIGER BEFUND aus dem Zig-Plugin-Beispiel (`plugins/zig-lsp/app/src/main/java/
  io/kiquar/plugin/zig/ZigServer.kt` und `Main.kt`): Diese bereits vollständig nach 
  MobileIDE migrierten Plugins (laut MIGRATION_STATUS.md: "Vollständig", keine 
  offenen Punkte) referenzieren WEITERHIN die ALT-Packages `com.rk.lsp.LspRegistry`, 
  `com.rk.lsp.ScriptedLspServer`, `com.rk.lsp.LspConnectionConfig`, sowie zusätzlich 
  `com.rk.file.*`, `com.rk.icons.Icon`, `com.rk.runner.RunnerManager`, `com.rk.utils.
  getTempDir`, `com.rk.exec.isTerminalInstalled`, `com.rk.file.sandboxHomeDir`. Dies 
  liegt daran, dass Plugins gegen ein separates `sdk.jar` (Xed-Editor Extension SDK, 
  `compileOnly(files("libs/sdk.jar"))`) kompilieren, das zur Laufzeit durch die 
  tatsächliche Host-App-Implementierung (also MobileIDE selbst) bereitgestellt wird.
- Das bedeutet: Die Klassen `LspRegistry`, `ScriptedLspServer`, `LspConnectionConfig` 
  MÜSSEN entweder (a) unter ihrem ORIGINAL-Package `com.rk.lsp` in der Host-App 
  weiterhin auffindbar bleiben (Kompatibilitäts-Fassade), damit alle 12 bereits 
  "Vollständig" migrierten Plugins ohne Neu-Build weiter funktionieren, ODER 
  (b) das SDK/die Plugins müssen in einem SEPARATEN, nachfolgenden Schritt ebenfalls 
  auf den neuen Package-Pfad aktualisiert werden. Dieser Prompt behandelt AUSSCHLIESS-
  LICH die Migration der Kernklassen selbst – die Entscheidung, ob eine Kompatibilitäts-
  Fassade nötig ist, MUSS in Stufe 0 anhand des tatsächlichen Umfangs von `com.rk.lsp` 
  getroffen werden.
- Bereits vorhandenes, ähnliches Migrations-Muster: `migrate_xed.py` (Root von 
  MobileIDE) migriert bereits `com.rk.terminal` → `com.scto.mobile.ide.features.
  terminal`, `com.rk.git` → `com.scto.mobile.ide.features.git`, `com.rk.runner` → 
  `com.scto.mobile.ide.features.runner`, `com.rk.extension` → `com.scto.mobile.ide.
  features.extension`, sowie generisch `com.rk.` → `com.scto.mobile.ide.` als Fallback. 
  Dieses Skript hat bisher explizit NICHT `com.rk.lsp` separat behandelt (kein 
  spezifisches Mapping dafür in `modify_contents()`), sodass der generische Fallback 
  `com.rk.` → `com.scto.mobile.ide.` griffe und `com.rk.lsp` zu `com.scto.mobile.ide.
  lsp` würde – NICHT zum vom Nutzer gewünschten Zielpfad `com.scto.mobile.ide.features.
  terminal.lsp`. Dieser Prompt weicht daher bewusst vom generischen migrate_xed.py-
  Muster ab und definiert das spezifischere Ziel-Package.
- Es existiert bereits ein separates `:core:lsp`-Modul in MobileIDE (aktiv in 
  settings.gradle.kts) – in Stufe 0 MUSS geprüft werden, ob dort BEREITS eine eigene, 
  unabhängige Implementierung von LSP-Registrierungslogik existiert, die mit der aus 
  Xed-Editor zu migrierenden `com.rk.lsp`-Logik kollidieren, überlappen oder sich 
  ergänzen könnte (Namenskollisionen bei Klassennamen wie `LspRegistry` vermeiden).
- Lizenz-/Copyright-Konvention: GPLv3, Copyright scto <tschmid35@gmail.com> für neu 
  erstellte/migrierte Dateien in MobileIDE (siehe LICENSE, build.gradle.kts-Header).

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER. Nach jeder Stufe: Modul-Build 
(mindestens `:features:terminal` sowie alle 12 `:plugins:*`-Module) durchführen, 
Fehlerfreiheit bestätigen, Ergebnis-Report liefern, bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme (Pflicht zuerst, keine Code-Änderung)

0.1. Prüfe im Quellverzeichnis `~/Xed-Editor/features/terminal/src/main/java/com/rk/
   lsp` den vollständigen tatsächlichen Inhalt: Liste ALLE darin enthaltenen Dateien, 
   Klassen, Interfaces und deren öffentliche API-Signaturen auf (insbesondere 
   `LspRegistry`, `ScriptedLspServer`, `LspConnectionConfig`, sowie jede weitere dort 
   vorhandene Klasse, die im vorliegenden Zig-Plugin-Beispiel nicht sichtbar war).

0.2. Prüfe, welche dieser Klassen tatsächlich innerhalb von Xed-Editor SPEZIFISCH 
   zum Terminal-Feature gehören (z. B. Terminal-basierte LSP-Installer-Skript-
   Ausführung, siehe `terminalLauncher`-Delegate laut status.md 2026-07-04 "Decoupled 
   Terminal Launcher for LSP") versus welche eigentlich allgemeine, terminal-
   UNABHÄNGIGE LSP-Infrastruktur darstellen (Registry-Pattern, Connection-Config-
   Definitionen), die eher in `:core:lsp` gehören würden. Dokumentiere diese 
   Einordnung explizit, auch wenn der Nutzerauftrag den kompletten Ordner nach 
   `:features:terminal:...lsp` verschieben möchte – weiche NICHT eigenmächtig vom 
   Auftrag ab, sondern liefere diese Einordnung nur als Hinweis im Report.

0.3. Öffne das bereits aktive `:core:lsp`-Modul in MobileIDE und liste dessen 
   vorhandene Klassen/Package-Struktur auf. Prüfe explizit auf Namenskollisionen mit 
   den in 0.1 gefundenen Klassen (gleicher Klassenname, unterschiedliches Package ist 
   grundsätzlich unkritisch, aber gleiche Verantwortlichkeit/Duplikat-Logik MUSS 
   gemeldet werden).

0.4. Prüfe, ob im bereits konsolidierten `:features:terminal`-Modul von MobileIDE 
   (Prompt 15) BEREITS ein `lsp`-Unterpaket oder Teile der zu migrierenden Logik 
   existieren (z. B. der bereits erwähnte `terminalLauncher`-Delegate-Mechanismus aus 
   status.md 2026-07-04, der laut Beschreibung in `ScriptedLspServer` und 
   `MainActivity` verdrahtet wurde – das würde bedeuten, dass Teile von `com.rk.lsp` 
   BEREITS an anderer Stelle in MobileIDE nachgebaut wurden, was Duplikate erzeugen 
   könnte).

0.5. Prüfe für ALLE 12 `:plugins:*`-Module (nicht nur zig-lsp), welche exakten 
   Klassen aus `com.rk.lsp` importiert werden (Volltextsuche nach `import com.rk.lsp` 
   in allen Plugin-Verzeichnissen), um den vollständigen Kompatibilitäts-Bedarf zu 
   ermitteln.

0.6. Liefere einen Befund-Report mit: vollständiger Klassenliste aus com.rk.lsp, 
   Einordnung Terminal-spezifisch vs. allgemein, Kollisionsprüfung mit :core:lsp, 
   bereits vorhandene Teil-Implementierung in :features:terminal, sowie vollständige 
   Liste aller Plugin-Abhängigkeiten auf com.rk.lsp – BEVOR mit Stufe 1 fortgefahren 
   wird.

---

## STUFE 2 – Migration des Verzeichnisses

2.1. Kopiere/verschiebe alle Dateien aus 
   `~/Xed-Editor/features/terminal/src/main/java/com/rk/lsp` 
   nach 
   `~/MobileIDE/features/terminal/src/main/java/com/scto/mobile/ide/features/
   terminal/lsp`, unter Erhalt der Dateinamen.

2.2. Passe in JEDER migrierten Datei die `package`-Deklaration von `com.rk.lsp` auf 
   `com.scto.mobile.ide.features.terminal.lsp` an.

2.3. Passe innerhalb der migrierten Dateien ALLE internen Imports an, die auf andere, 
   bereits an anderer Stelle in MobileIDE migrierte Xed-Editor-Packages verweisen 
   (z. B. `com.rk.file.*` → `com.scto.mobile.ide.features.terminal.file.*` bzw. den 
   tatsächlichen, in Stufe 0 durch Volltextsuche ermittelten Ziel-Pfad, falls `com.rk.
   file` bereits an anderer Stelle in MobileIDE migriert wurde; `com.rk.exec.*`, 
   `com.rk.utils.*`, `com.rk.icons.*` entsprechend behandeln). Nutze dabei denselben 
   Grad an Sorgfalt wie das bestehende `migrate_xed.py`-Skript, aber MIT dem 
   spezifischen Ziel-Package `com.scto.mobile.ide.features.terminal.lsp` statt dem 
   generischen Fallback.

2.4. Ergänze im `build.gradle.kts` von `:features:terminal` alle zusätzlich benötigten 
   Dependencies (z. B. `lsp4j`, siehe Version bereits im Root-Version-Catalog als 
   `lsp4j = "1.0.0"` verzeichnet), falls die migrierte LSP-Logik solche benötigt und 
   sie im Modul noch nicht vorhanden sind.

2.5. Stelle sicher, dass innerhalb von `:features:terminal` (z. B. `MainActivity`- 
   Anbindung, `TerminalBackEnd.kt`, oder wo auch immer laut Stufe 0.4 die bestehende 
   `terminalLauncher`-Delegate-Logik verdrahtet ist) die migrierten Klassen korrekt 
   referenziert und nicht doppelt implementiert werden – konsolidiere ggf. bereits 
   vorhandene Teil-Logik MIT der neu migrierten, statt beides parallel zu behalten.

## STUFE 3 – Sicherstellung der Plugin-Kompatibilität

3.1. Basierend auf der in Stufe 0.5 ermittelten Liste: Entscheide und dokumentiere, 
   ob eine Kompatibilitäts-Fassade unter dem alten Package-Pfad `com.rk.lsp` 
   (typealias oder dünne Wrapper-Klassen, die auf die neuen `com.scto.mobile.ide.
   features.terminal.lsp.*`-Implementierungen delegieren) notwendig ist, damit die 
   12 bereits als "Vollständig" migrierten Plugins (laut MIGRATION_STATUS.md) 
   weiterhin ohne Änderungen an ihrem eigenen Code funktionieren.

3.2. Falls eine Fassade als notwendig identifiziert wird: Implementiere sie minimal-
   invasiv (z. B. `typealias LspRegistry = com.scto.mobile.ide.features.terminal.
   lsp.LspRegistry` in einer kleinen Kompatibilitätsdatei), OHNE die eigentliche 
   Business-Logik zu duplizieren.

3.3. Falls stattdessen entschieden wird, die Plugins direkt auf den neuen Package-
   Pfad zu aktualisieren (Alternative zu 3.2): Aktualisiere in ALLEN betroffenen 
   Plugin-Dateien (siehe Liste aus Stufe 0.5) die Imports von `com.rk.lsp.*` auf 
   `com.scto.mobile.ide.features.terminal.lsp.*`. Dokumentiere im Report klar, welche 
   der beiden Strategien (3.2 oder 3.3) gewählt wurde und warum.

3.4. Aktualisiere ggf. `MIGRATION_STATUS.md`, falls durch Stufe 3.3 einzelne Plugins 
   erneut Code-Änderungen erfahren haben (Status bleibt "Vollständig", aber ggf. 
   Anmerkung ergänzen).

## STUFE 4 – Build- & Funktionsverifikation

4.1. Baue `:features:terminal` isoliert und bestätige Fehlerfreiheit.

4.2. Baue ALLE 12 `:plugins:*`-Module und bestätige, dass keines durch die Migration 
   einen Kompilierungsfehler erhält.

4.3. Baue das Gesamtprojekt (`:app` inkludiert) und bestätige Fehlerfreiheit.

4.4. Funktionstest: Installiere/aktiviere mindestens ein Plugin (z. B. `zig-lsp`) über 
   den bestehenden Plugin-Mechanismus und bestätige, dass `ZigServer` (bzw. das 
   jeweilige Server-Äquivalent) weiterhin korrekt bei `LspRegistry` registriert wird 
   und die Installations-/Verbindungs-Logik (`isInstalled`, `getConnectionConfig`) 
   fehlerfrei funktioniert.

4.5. Liefere einen finalen Report mit: Liste aller migrierten Dateien, gewählter 
   Kompatibilitätsstrategie (Fassade vs. direkte Plugin-Anpassung), sowie Ergebnis 
   der Build- und Funktionsverifikation.

# Nicht-Ziele
- Keine Änderung der Business-Logik innerhalb der migrierten LSP-Klassen selbst 
  (reine Package-Umbenennung/Pfad-Migration, keine Refaktorierung der internen 
  Funktionsweise).
- Keine Migration weiterer, in diesem Prompt nicht explizit genannter `com.rk.*`-
  Packages (z. B. `com.rk.file`, `com.rk.exec`) über das für die Kompilierbarkeit der 
  migrierten `lsp`-Klassen unbedingt notwendige Maß hinaus (das ist bereits an 
  anderer Stelle im Projekt behandelt bzw. behandelt worden, siehe migrate_xed.py).
- Keine Änderung an `:core:lsp`, außer zur reinen Kollisionsprüfung/Dokumentation 
  (Lesen, nicht Ändern), sofern in Stufe 0.3 keine zwingende Konsolidierung 
  identifiziert wird.

# Akzeptanzkriterien
- Alle Dateien aus `Xed-Editor/features/terminal/src/main/java/com/rk/lsp` liegen 
  nach der Migration unter `MobileIDE/features/terminal/src/main/java/com/scto.
  mobile.ide/features/terminal/lsp` mit korrekt angepasster Package-Deklaration.
- `:features:terminal` baut fehlerfrei mit der migrierten Logik.
- Alle 12 `:plugins:*`-Module bauen weiterhin fehlerfrei (entweder durch 
  Kompatibilitäts-Fassade oder durch direkte Anpassung der Plugin-Imports – 
  dokumentiert im finalen Report).
- Es existieren keine unentdeckten Namens- oder Verantwortlichkeits-Kollisionen 
  zwischen der migrierten Logik und dem bestehenden `:core:lsp`-Modul (bzw. diese 
  sind explizit dokumentiert und bewusst in Kauf genommen).
- Ein mindestens exemplarischer Plugin-Funktionstest (z. B. zig-lsp) bestätigt, dass 
  Registrierung und Verbindungsaufbau nach der Migration weiterhin funktionieren.
