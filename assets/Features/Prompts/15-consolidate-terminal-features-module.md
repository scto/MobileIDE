### Prompt-Datei: `15-consolidate-terminal-features-module.md`

# Ziel
Führe die gesamte Terminal-Funktionalität, die aktuell über die Module :core:main 
(Terminal-Backend-Anteil), :core:terminal, :core:terminal-emulator, :core:terminal-view 
sowie das UI-Package app/src/main/java/com/scto/mobile/ide/ui/terminal verteilt ist, 
in ein einziges, neues Feature-Modul :features:terminal zusammen – inklusive aller 
zugehörigen Assets aus app/src/main/assets/terminal/ (ideenv, idesetup, init.sh, setup.sh, 
sowie terminal/colorschemes/{default,dracula,solarized_dark,nord,monokai}).

# Kontext / WICHTIG vor Beginn
- Bestätigte aktuelle Modul-Registrierung in settings.gradle.kts:
  include(":core:main")
  include(":core:terminal-emulator")
  include(":core:terminal-view")
  include(":core:terminal")
  → Es gibt bereits vier separate Anlaufstellen für Terminal-Code. :features:terminal 
  existiert noch NICHT und muss neu angelegt werden.
- :core:main ist laut README/README_DE NICHT ausschließlich ein Terminal-Modul, sondern 
  übernimmt zusätzlich zentrale App-Navigation und Theme-Konfiguration 
  ("Zentrales Kern-IDE-Modul: Hauptnavigation, Terminal-Sitzungsverwaltung-Backend, 
  Design- und Theme-Konfigurationen"). 
  → Migriere aus :core:main NUR den Terminal-Backend-Anteil (TerminalBackEnd.kt und 
  verwandte Session-Callback-Klassen). Navigation- und Theme-Code bleibt in :core:main.
- Bestätigte UI-Quelle: app/src/main/java/com/scto/mobile/ide/ui/terminal 
  → Dieses gesamte Package (TerminalScreen.kt, TerminalSettingsScreen.kt, 
  TerminalService und alle weiteren Dateien in diesem Ordner) wird vollständig nach 
  :features:terminal verschoben.
- Bekanntes historisches Entkopplungsmuster beachten (PROGRESS.md, 2026-07-01): 
  "Modulare Entkopplung: Session-Terminierungs-Callbacks wurden vom Terminal-Backend-
  View-Client entkoppelt, um :core:main-Abhängigkeiten von :app zu isolieren und 
  zirkuläre Compile-Referenzen zu verhindern." 
  → Dieses Callback/Interface-Delegationsmuster MUSS bei der neuen Modulgrenze 
  :app ↔ :features:terminal konsequent fortgeführt werden (KEINE direkte 
  :features:terminal → :app-Abhängigkeit, nur die umgekehrte Richtung ist erlaubt).
- Bekannte Cross-Modul-Nutzung, die nach der Migration weiterhin funktionieren muss:
  1. `DistroManager.buildProotCommand` wird laut PROGRESS.md (2026-07-04) in 
     CodeEditScreen.kt (:app) genutzt, um den APK-Build im PRoot-Container laufen zu 
     lassen ("Decoupled APK Builder PRoot integration").
  2. `ScriptedLspServer.terminalLauncher`-Delegate wird in MainActivity.kt (:app) 
     gehookt, um LSP-Installer-Skripte im Terminal auszuführen ("Decoupled Terminal 
     Launcher for LSP", PROGRESS.md 2026-07-04).
  3. Route `settings/terminal` (NICHT `terminal_settings` – siehe Bugfix vom 2026-07-03, 
     wo genau diese Verwechslung zu einem Navigation-Crash führte) wird von :app aus 
     zur Terminal-Settings-Ansicht navigiert.
  4. TerminalService wird im AndroidManifest von :app registriert und nutzt 
     R.drawable.ic_code als Foreground-Notification-Icon (Fix vom 2026-07-03 wegen 
     ForegroundServiceStartNotAllowedException bei Adaptive Icons).

# Vorgehen (schrittweise, mit Build-Verifikation nach jedem Schritt)

## Schritt 1: Bestandsaufnahme
- Erstelle eine temporäre Datei TERMINAL_MIGRATION_INVENTORY.md mit:
  - Vollständiger Dateiliste in :core:main, markiert als "Terminal-relevant" 
    (z. B. TerminalBackEnd.kt) vs. "Navigation/Theme-relevant" (bleibt in :core:main).
  - Vollständiger Dateiliste in :core:terminal, :core:terminal-emulator, 
    :core:terminal-view.
  - Vollständiger Dateiliste in app/src/main/java/com/scto/mobile/ide/ui/terminal.
  - Liste aller Cross-Modul-Imports (wer importiert was aus diesen Paketen, 
    insbesondere :core:apk-builder, MainActivity.kt, CodeEditScreen.kt).

## Schritt 2: Neues Modul anlegen
- Erstelle :features:terminal als neues Android-Library-Modul (build.gradle.kts, 
  AndroidManifest.xml minimal).
- Ergänze in settings.gradle.kts: `include(":features:terminal")`
- Übernimm alle terminal-relevanten Gradle-Dependencies aus :core:terminal, 
  :core:terminal-emulator, :core:terminal-view sowie die vom UI-Package benötigten 
  Compose-/Service-/Notification-Dependencies aus :app in die neue build.gradle.kts.

## Schritt 3: Core-Terminal-Module migrieren
- Verschiebe den vollständigen Inhalt von :core:terminal, :core:terminal-emulator, 
  :core:terminal-view als Sub-Packages nach :features:terminal, z. B.:
  - com.scto.mobile.ide.features.terminal.emulator (ANSI/PTY-Parser)
  - com.scto.mobile.ide.features.terminal.view (Android View Widget)
  - com.scto.mobile.ide.features.terminal.session (Session-Verwaltung aus :core:terminal)
- Entferne anschließend `include(":core:terminal")`, `include(":core:terminal-emulator")`, 
  `include(":core:terminal-view")` aus settings.gradle.kts.
- Build-Check: `./gradlew :features:terminal:build` – Importfehler beheben.

## Schritt 4: Terminal-Backend-Anteil aus :core:main extrahieren
- Verschiebe ausschließlich die in Schritt 1 als "Terminal-relevant" markierten Klassen 
  (TerminalBackEnd.kt, Session-Callback-Interfaces) aus :core:main nach 
  :features:terminal, Package z. B. com.scto.mobile.ide.features.terminal.backend.
- Navigation- und Theme-Code bleibt unverändert in :core:main.
- Falls :core:main (Navigation) das Terminal-Backend referenzieren muss: definiere ein 
  minimales Interface/Contract in :core:main oder :core:common, das :features:terminal 
  implementiert. :core:main darf NICHT von :features:terminal abhängen.
- Build-Check: `./gradlew :core:main:build :features:terminal:build`.

## Schritt 5: App-UI-Package vollständig migrieren
- Verschiebe das komplette Package app/src/main/java/com/scto/mobile/ide/ui/terminal 
  (inkl. TerminalScreen.kt, TerminalSettingsScreen.kt, TerminalService und aller 
  weiteren enthaltenen Dateien) 1:1 nach 
  features/terminal/src/main/java/com/scto/mobile/ide/features/terminal/ui/.
- :app referenziert diese Screens fortan nur noch über Navigation-Routen und ein 
  von :features:terminal exportiertes Composable/NavGraph-Fragment 
  (Pattern: `fun NavGraphBuilder.terminalGraph(navController: NavController)`, 
  eingehängt in den zentralen NavHost von :app).
- WICHTIG: Route-Konstante für Terminal-Settings (`"settings/terminal"`) zentral in 
  :features:terminal definieren und von :app importieren – NICHT erneut als 
  String-Literal duplizieren, um den Bug vom 2026-07-03 nicht zu wiederholen.
- Aktualisiere den TerminalService-Eintrag im AndroidManifest von :app: Service-Klasse 
  liegt jetzt in :features:terminal, wird aber weiterhin mit vollqualifiziertem 
  Klassennamen im :app-Manifest deklariert (`<service android:name="com.scto.mobile.ide.features.terminal.ui.TerminalService" .../>`).
- Prüfe, dass R.drawable.ic_code (Notification-Icon-Fix) weiterhin korrekt aufgelöst 
  wird – ggf. über :core:resources referenzieren statt lokaler :app-Ressource.

## Schritt 6: Assets migrieren
- Verschiebe den kompletten Ordner app/src/main/assets/terminal/ nach 
  features/terminal/src/main/assets/terminal/ (inkl. ideenv, idesetup, init.sh, setup.sh, 
  terminal/colorschemes/{default,dracula,solarized_dark,nord,monokai}).
- Prüfe alle `context.assets.open("terminal/...")`-Zugriffe (z. B. Farbschema-Ladelogik 
  aus dem Fix vom 2026-07-01 "Custom Terminal Color Schemes", Setup-Extraktionslogik) – 
  diese Zugriffe funktionieren nur korrekt, wenn der zugreifende Code ebenfalls nach 
  :features:terminal migriert wurde (Schritt 3-5 müssen also VOR diesem Schritt 
  abgeschlossen sein).
- Falls :core:apk-builder oder andere Module ebenfalls auf denselben Assets-Pfad 
  zugreifen (z. B. für AAPT2-Setup-Referenzen): klären, ob eine Duplizierung nötig ist 
  oder ob :core:resources als gemeinsamer Assets-Container sinnvoller wäre – 
  Entscheidung in TERMINAL_MIGRATION_INVENTORY.md dokumentieren.

## Schritt 7: Cross-Modul-Abhängigkeiten von außen sicherstellen
- DistroManager.buildProotCommand: :app (CodeEditScreen.kt) muss diese Funktion weiterhin 
  aufrufen können – jetzt als Abhängigkeit auf :features:terminal statt :core:main. 
  Aktualisiere Import und Gradle-Dependency von :app.
- ScriptedLspServer.terminalLauncher-Delegate: Stelle sicher, dass der Hook in 
  MainActivity.kt nach der Migration weiterhin auf die korrekte Klasse in 
  :features:terminal verweist.
- Prüfe alle Plugin-Referenzen (z. B. plugins/typst-lsp nutzt 
  `com.scto.mobile.ide.exec.ShellUtils`/`launchTerminal`/`TerminalCommand` über das 
  SDK/sdk.jar) – falls diese Klassen NICHT Teil dieser Migration sind (sie gehören zu 
  einem separaten :exec-Bereich), stelle sicher, dass keine versehentliche Verschiebung 
  stattfindet, die das Extension-SDK bricht. Falls doch betroffen: SDK-Build-Pipeline 
  (downloadLatestJar-Task) synchron aktualisieren, sonst brechen ALLE Plugins.

## Schritt 8: Build- und Funktionsverifikation
- `./gradlew build` für das Gesamtprojekt muss fehlerfrei durchlaufen.
- Manuelle Regressionsprüfung (keine Ausnahmen erlaubt):
  1. Terminal öffnen, neue Session starten, einen Befehl ausführen.
  2. Terminal-Einstellungen öffnen über Route `settings/terminal`, Farbschema wechseln 
     (Dracula/Nord/Monokai/Solarized/Default).
  3. Foreground-Notification erscheint beim Terminal-Start korrekt ohne Crash 
     (Regressionstest für 2026-07-03-Fix).
  4. APK bauen über den Play-Button in CodeEditScreen – DistroManager.buildProotCommand 
     muss weiterhin funktionieren.
  5. Eine LSP-Installation (z. B. Kotlin-LSP) über terminalLauncher-Delegate anstoßen.
  6. Letzte Terminal-Session schließen – Standardverhalten "neue Session öffnen" 
     (statt App-Exit, siehe Fix vom 2026-07-01) muss weiterhin greifen.

## Schritt 9: Aufräumen & Dokumentation
- Entferne `include(":core:terminal")`, `include(":core:terminal-emulator")`, 
  `include(":core:terminal-view")` final aus settings.gradle.kts (falls in Schritt 3 
  noch nicht geschehen).
- Lösche TERMINAL_MIGRATION_INVENTORY.md.
- Aktualisiere README.md und README_DE.md: Entferne die Einträge für 
  `:core:terminal-emulator`, `:core:terminal-view` sowie den Terminal-Backend-Hinweis 
  bei `:core:main`, füge stattdessen hinzu:
  "`:features:terminal` – Vollständig konsolidiertes Terminal-Feature: Session-Backend, 
  ANSI/PTY-Emulator, View-Rendering, Terminal-Screen- und Settings-UI, TerminalService, 
  Sandbox-Setup-Assets (ideenv, idesetup, init.sh, setup.sh, Farbschemata)."
- Ergänze in PROGRESS.md einen neuen datierten Abschnitt, der diese 
  Konsolidierungsmaßnahme im bestehenden Stil der Datei dokumentiert.

# Nicht-Ziele
- Keine Verschiebung von DistroManager selbst nach :features:proot in diesem Schritt 
  (das betrifft eine separate, spätere PRoot-Konsolidierung).
- Keine Verschiebung von ShellUtils/exec-bezogenem Code (das betrifft eine separate 
  :features:exec-Konsolidierung).
- Keine funktionalen Änderungen am Terminal-Verhalten selbst – reines Struktur-Refactoring.

# Akzeptanzkriterien
- `./gradlew build` läuft fehlerfrei durch, keine zyklischen Modul-Abhängigkeiten 
  (`./gradlew :app:dependencies` zeigt saubere DAG, :features:terminal hat keine 
  Abhängigkeit auf :app).
- Alle 6 Regressionstests aus Schritt 8 sind erfolgreich.
- Keine verbleibenden Referenzen auf die alten Pakete `com.scto.mobile.ide.ui.terminal` 
  im :app-Modul (verifizierbar per projektweiter Grep-Suche).