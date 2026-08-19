Konsolidiere die Modul-/Submodul-Struktur von MobileIDE (com.scto.mobile.ide), um
Duplikate und Namensverwirrungen zu beseitigen. Konkret sind diese Symptome zu
lösen:
  1. Doppelte/verwirrende Settings-UI: app/.../ui/settings/SettingsScreen.kt vs.
     :features:terminal/TerminalSettingsScreen.kt (im ursprünglichen Prompt fälschlich
     als Quelle angenommen; real ist app/.../ui/settings der aktive Ort).
  2. Weitere Duplikate gleicher Art systematisch finden (Enum/Utility/Klasse gleichen
     Namens in mehreren Modulen).
  3. Assets-Duplikate im Terminal-Bereich: assets/terminal/** (colorschemes/, LSP-
     Skripte, Rootfs-/Setup-Assets) – es soll NUR EIN Modul/Submodul die Assets
     halten, alle anderen referenzieren es.
  4. Ein Reconciliation-Skript, das die Konsolidierung künftig automatisch prüft.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND (aus Reports + Dump)
══════════════════════════════════════════════════════════════════
- Module laut settings.gradle.kts: :app, :core:common, :core:tooling:tooling-api,
  :core:tooling:tooling-impl, :features:terminal, :core:apk-builder u. a.
- Bekannte Duplikat-Kandidaten (aus Diagnose/Recon):
  * SettingsScreen.kt in app/.../ui/settings UND TerminalSettingsScreen-Referenz in
    TerminalRoutes.kt (Route "settings/terminal").
  * TerminalEnvironmentSelector.kt (features:terminal) + Distro-Listen in
    SettingsScreen.kt (app) – zwei Quellen für Distro-Werte.
  * assets/terminal/colorschemes/ (5 Themes) + ggf. Farb-Sets in core.
  * LSP-Skripte: lsp-script-inventory.tsv + kotlin-lsp-consistency.tsv existieren –
    konsolidierte Verzeichnisstruktur prüfen.
  * ToolingLogManager (tooling-api) vs. LogCatcher (core:common) vs.
    ToolingLogManagerImpl (tooling-impl) – verifizieren, dass kein Doppel.
- WICHTIG: Assets dürfen KEINEN Pfadbruch verursachen – alle referenzierenden
  Stellen (ColorSchemeManager, ScriptedLspServer, DistroManager, setup.sh) müssen
  nach dem Umzug aktualisiert werden.

══════════════════════════════════════════════════════════════════
PHASE A – INVENTAR & DUPLIKAT-SCAN (rein lesend)
══════════════════════════════════════════════════════════════════
A1. Scan: finde ALLE Duplikat-Kandidaten (gleicher Klassen-/Enum-/Utility-Name in
    >1 Modul) und alle Settings-bezogenen Dateien:
      grep -rn "class SettingsScreen\|object TerminalEnvironmentSelector\|enum class
      TerminalEnvironmentOption\|class ColorSchemeManager\|TerminalColors\|class
      DistroManager" app core features --include="*.kt"
A2. Asset-Inventar: finde alle Verzeichnisse/Dateien unter */src/main/assets/:
      find app core features -path "*/src/main/assets/*" -type d | sort
    Für jedes Asset: Verzeichnisgröße, enthaltene Dateien (colorschemes/*.json,
    lsp-scripts/*.sh, rootfs/*.tar.gz, setup.sh etc.), und welche Kotlin-Klassen
    darauf zugreifen (grep -rn "assets/terminal\|colorschemes\|lsp-scripts" ...).
A3. Modul-Abhängigkeitsgraph: Welches Modul hängt von welchem ab (settings.gradle.kts
    + je build.gradle.kts) – um zu bestimmen, WOHIN die konsolidierten Assets
    gehören (Kandidat: :core:common oder :features:terminal als einziger Halter).
A4. Erstelle build/module-consolidation-inventory.tsv:
    duplikat | modul A | modul B | referenzierende stellen | empfohlener halter |
    umzug-risiko (HOCH/MITTEL/NIEDRIG) | entscheidung (PENDING)

══════════════════════════════════════════════════════════════════
PHASE B – KONSOLIDIERUNG NACH ENTSCHEIDUNG
══════════════════════════════════════════════════════════════════
B1. SETTINGS-UI vereinheitlichen: EINE SettingsScreen-Komponente als Single Source
    of Truth (empfohlen: app/.../ui/settings/SettingsScreen.kt behalten, weil die
    Route und die Preferences dort liegen). TerminalRoutes.kt/TerminalSettingsScreen-
    Reste entfernen ODER als Wrapper auf die zentrale Komponente zeigen lassen.
    KEINE zwei unabhängigen Terminal-Settings-UI mehr.
B2. DISTRO: Eine einzige Distro-Quelle (Enum TerminalEnvironmentOption oder neue
    zentrale Distro-Enum in :features:terminal) – von SettingsScreen UND
    DistroManager/TerminalEnvironmentSelector genutzt. String-Duplikat
    listOf("ubuntu","alpine") in SettingsScreen durch Enum-Werte ersetzen.
B3. ASSETS: Alle Terminal-Assets (colorschemes, LSP-Skripte, Setup-Skripte,
    Rootfs-Helfer) an EINEN Ort verschieben – empfohlen:
    :features:terminal/src/main/assets/terminal/** (falls :features:terminal die
    meisten Konsumenten hat) ODER :core:common/.../assets/terminal/** (falls mehrere
    Module es brauchen – anhand A3 entscheiden). ALLE Referenzstellen (ColorScheme-
    Manager, ScriptedLspServer, DistroManager, SetupWorker, setup.sh/init.sh) auf
    den neuen Pfad umbiegen. Alte Pfade löschen – KEINE Duplikate behalten.
B4. Jede Umstellung mit Compile+Test belegen (assembleDebug + bestehende Unit-Tests).

══════════════════════════════════════════════════════════════════
PHASE C – RECONCILIATION-SKRIPT (dauerhaft)
══════════════════════════════════════════════════════════════════
C1. Erstelle scripts/reconcile_modules.sh (bash, ausführbar, läuft in Termux):
    a) Modul-Duplikat-Scan: prüft, dass keine .kt-Datei mit gleichem Typnamen in
       >1 Modul existiert (wgrep über app core features),
    b) Asset-Einzelhalter-Check: prüft, dass nur EIN assets/terminal-Verzeichnis
       existiert (Kandidatenliste aus B3) und dass keine referenzierende Kotlin-
       Datei auf einen ALTEN Pfad zeigt (grep auf alte Pfadteile),
    c) Distro-Quellen-Check: nur EINE Quelle für distro-Werte (grep auf
       listOf("ubuntu" oder "debian"),
    d) Settings-UI-Check: nur EINE SettingsScreen-Datei mit terminal-Bezug,
    e) Exit-Code 0 = sauber; 1 = Funde; Ausgabe als TSV mit datei:zeile,
    f) Optional: --fix-Modus führt die gefundenen, eindeutig automatisierbaren
       Umzüge aus (nur bei eindeutigem Zielpfad, sonst Fehler).
C2. Skript dokumentieren (README-Zeile) und als Checkpoint in die Entwicklung
    aufnehmen: nach jedem größeren Prompt ausführbar.
C3. Report build/module-consolidation-report.md: Inventar (A4), getroffene
    Entscheidungen + Begründung (B1-B3), Skript-Ausgabe (C1), Regression-Tabelle.

══════════════════════════════════════════════════════════════════
PHASE D – VALIDIERUNG
══════════════════════════════════════════════════════════════════
D1. ./gradlew assembleDebug → BUILD SUCCESSFUL (KEIN Pfadbruch durch Asset-Umzug).
D2. ./gradlew test → alle bestehenden Tests grün (v. a. PathTranslator, LogRouter,
    ApkBuilder 16er-Suite).
D3. scripts/reconcile_modules.sh → Exit 0 (keine Duplikate mehr).
D4. On-Device-Smoke: Terminal-Settings öffnen (Distro-Liste korrekt), Farbschema
    wechseln (Asset-Referenz intakt), LSP-Installer starten (LSP-Asset intakt).
D5. Commits:
    "refactor(modules): consolidate terminal settings UI and distro source of
    truth; single owner for terminal assets"
    "chore(tooling): add reconcile_modules.sh duplicate and asset ownership check"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase A MUSS vollständig sein, bevor eine Datei bewegt wird. Unklare Duplikate
  NICHT raten → als PENDING im Inventory markieren und im Report begründen.
- Asset-Umzug NUR wenn ALLE Referenzstellen bekannt sind (A2). Ist eine Referenz
  nicht auffindbar (z. B. Reflection/Files), STOPP und als offener Punkt melden.
- KEINE funktionalen Änderungen an Logik/Verhalten – reine Struktur-Konsolidierung.
- Skript darf im --fix-Modus NIE eine Datei löschen ohne vorherige Sicherung.
