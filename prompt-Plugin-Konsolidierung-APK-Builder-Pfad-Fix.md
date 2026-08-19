Behebe die beiden bekannten Hauptprobleme von MobileIDE (com.scto.mobile.ide) in 
ZWEI getrennten Teilprojekten:

TEIL 1: Plugin-Chaos beenden — eine einzige Source-of-Truth für alle Plugins, 
        vollständige Namensraum-Migration, korrekte Asset-Ablage.
TEIL 2: APK-Builder-Fehler 
        "[ERROR] [ApkBuilder] Projektverzeichnis ungültig oder existiert nicht: 
        /storage/emulated/0/MobileIDEProjects/MyApp" — Pfad-Mapping zwischen 
        Android-Host und PRoot-Sandbox korrigieren.

BEIDE Teile sind getrennt zu bearbeiten und zu committen. Nach jedem Teil: 
Build-Validierung + Bericht. Teil 1 VOR Teil 2 (unabhängige Tasks, aber 
Reihenfolge hält den Arbeitsbaum sauber).

══════════════════════════════════════════════════════════════════
KONTEXT / VERIFIZIERTER IST-ZUSTAND
══════════════════════════════════════════════════════════════════

A) PLUGIN-CHAOS — 5 parallele Ablageorte:
   1) ~/Plugins/lsp/xed-*        Externe Quell-Repos (xed-fsharp, xed-go, 
                                 xed-go-tools, xed-java-lsp, xed-json-lsp, 
                                 xed-kmp-lsp, xed-kotlin, xed-lua-lsp, 
                                 xed-prettier, xed-prettier-standalone, 
                                 xed-python-lsp, xed-rust, xed-typst, xed-zig)
   2) ~/MobileIDE/plugins/*-lsp  Aktive Gradle-Module: :plugins:json-lsp, 
                                 :plugins:lua-lsp, :plugins:python-lsp, 
                                 :plugins:typst-lsp, :plugins:go-lsp, 
                                 :plugins:rust-lsp, :plugins:zig-lsp, 
                                 :plugins:fsharp-lsp, :plugins:prettier-lsp 
                                 (in settings.gradle.kts registriert)
   3) ~/MobileIDE/app/src/main/assets/bundled_plugins/  (ALTER Zielordner, wird 
      vom Gradle-Task "copyPluginToAssets" weiterhin befüllt!)
   4) ~/MobileIDE/assets/Plugins/                        (entpackte Plugin-Ordner)
   5) ~/MobileIDE/assets/Plugins/LSP/                    (26 Zips + catalog.json)

B) MIGRATIONSTATUS INKONSISTENT (trotz "0 Abweichungen"-Berichten real belegt):
   - xed-json-lsp:   namespace com.rk.xededitor, applicationId com.rk.demo
   - xed-lua-lsp:    Package com.rk.lua
   - xed-python-lsp: Package com.rk.xededitor.python
   - xed-java-lsp:   Package com.androdev.java.lsp
   - xed-kmp-lsp:    Package io.kiquar.plugin.kmplsp (teilweise com.scto.mobile.ide.*)
   - xed-typst:      ZWEI Versionen existieren (einmal com.koner.typst mit 
                     com.rk.*-Imports, einmal bereits com.scto.mobile.ide.*-Imports)
   - xed-zig:        Main.kt migriert, ZigServer.kt noch com.rk.*/io.kiquar.*-Imports
   - migrate_xed.py verarbeitete NUR features/*, NIE plugins/* (belegt in 
     30-consolidate-module-architecture-eliminate-duplicate-code-paths.md)

C) BUILD-KONFIG DRIFT:
   - rootProject.name uneinheitlich: "Prettier (Xed)", "KMP-LSP-Extension", 
     "Go Tools", "JSON-LSP", "Extension-Template", "Java-LSP", "xed-fs", 
     "Go-Extension", "xed-typst", "xed-rust", "ZigExtension", "Python LSP", 
     "Kotlin-Extension"
   - jvmToolchain gemischt 17/21; AGP 8.13.1 vs 9.2.x; Gradle 9.2.1 vs 9.5.1
   - Build-Logs zeigen: "Cannot find a Java installation ... matching: 
     {languageVersion=21 ...} foojay (Service 'SystemInfo' is not available)"
   - Manifest-Schemata: Xed-Schema (manifest.json: id/name/mainClass/version/
     author{displayName,github}/repository/minAppVersion als INTEGER) vs. 
     Store-Schema (plugin.json: id/name/version/category/description/author/
     minAppVersion als STRING/entryScript/fileExtensions/dependencies/arch)

D) GRADLE-TASK "copyPluginToAssets" (in plugins/typst-lsp/app/build.gradle.kts):
   val copyPluginToAssets by tasks.registering(Copy::class) {
       ...
       into(File(rootDir, "../../app/src/main/assets/bundled_plugins"))
   }
   → Erzeugt Duplikate im ALTEN Verzeichnis app/src/main/assets/bundled_plugins/ 
     statt im eigentlichen Ziel assets/Plugins/LSP/.

E) APK-BUILDER-FEHLER:
   - Fehler: [ERROR] [ApkBuilder] Projektverzeichnis ungültig oder existiert 
     nicht: "/storage/emulated/0/MobileIDEProjects/MyApp"
   - Build läuft in PRoot-Sandbox (ApkBuilder.kt akzeptiert 
     configureProcessBuilder-Lambda; CodeEditScreen.kt übergibt 
     DistroManager.buildProotCommand — PROGRESS.md 2026-07-04).
   - Sandbox hat eigenes FS unter /data/data/com.scto.mobile.ide/local/<distro>/; 
     nur explizit per -b gebundene Pfade sind sichtbar.
   - setup.sh/init.sh lösen Projektpfad über $MOBILEIDE_PROJECT_DIR im Container 
     auf (PROGRESS.md 2026-07-03).
   - Host: /storage/emulated/0 == /sdcard (Symlink). In PRoot NICHT zwingend 
     gebunden.

══════════════════════════════════════════════════════════════════
TEIL 1 — PLUGIN-KONSOLIDIERUNG
══════════════════════════════════════════════════════════════════

PHASE 1.0 — BACKUP & BESTANDSAUFNAHME (Pflicht, keine Änderung)
   - Backup: ~/MobileIDE/build/plugins-consolidation-backup-<timestamp>.tar.gz 
     (app/src/main/assets/bundled_plugins/, assets/Plugins/, plugins/) und 
     ~/Plugins als plugins-src-backup-<timestamp>.tar.gz.
   - Erstelle build/plugin-consolidation-inventory.tsv:
     plugin | quelle (xed-repo|mobileide-plugins|bundled|assets|LSP-zip) | 
     package_alt | package_neu? | version_manifest | version_gradle | 
     rootProject.name | jvmToolchain | agp_version | zip_in_LSP? | status
   - Grep im GESAMTEN Projekt (inkl. plugins/, app/src/main/assets/, ~/Plugins) 
     nach: com.rk., com.androdev., com.koner., io.kiquar., com.rk.demo — 
     Trefferliste als build/old-package-refs.txt ablegen (datei:zeile:match).
   - Dokumentiere pro Plugin, welche der 5 Quellen existieren und welcher Stand 
     jeweils NEUER ist (git log -1, mtime, Version im Manifest vs. ZIP-Name).

PHASE 1.1 — SOURCE OF TRUTH FESTSCHREIBEN
   - ENTSCHEIDUNG: Die EINZIGE Quell-Source-of-Truth für die 9 aktiven 
     Gradle-Module ist ~/MobileIDE/plugins/<plugin>/. Alle externen 
     xed-Repos unter ~/Plugins dienen nur noch als UPSTREAM-Referenz und 
     dürfen NICHT in den Build einfließen.
   - Synchronisiere jeden ~/MobileIDE/plugins/*-Ordner mit seinem 
     ~/Plugins/lsp/xed-*-Pendant NUR für Dateien, die im MobileIDE-Ordner 
     fehlen oder dort ALT sind (kein Überschreiben neuerer MobileIDE-Dateien). 
     Entscheidung pro Datei in build/plugin-sync-log.tsv dokumentieren 
     (datei | aktion: kopiert|übersprungen|ersetzt-mobileide | grund).
   - ~/Plugins bleibt unverändert (reine Upstream-Spiegel, kein Commit dort 
     erforderlich außer explizit geforderte Korrekturen).

PHASE 1.2 — NAMENSRAUM-MIGRATION KOMPLETTIEREN (plugins/*)
   - Für JEDES der 9 aktiven Module in ~/MobileIDE/plugins/:
     a) Verzeichnisstruktur: 
        app/src/main/java/<altes-package>/ → app/src/main/java/<neues-package>/
        ZIEL-PACKAGE-MAPPING:
        json-lsp:   com.rk.xededitor.json        → com.scto.mobile.ide.json_lsp
        lua-lsp:    com.rk.lua                   → com.scto.mobile.ide.plugin.lua
        python-lsp: com.rk.xededitor.python      → com.scto.mobile.ide.python_lsp
        typst-lsp:  com.koner.typst              → com.scto.mobile.ide.plugin.typst
        go-lsp:     io.kiquar.plugin.go          → com.scto.mobile.ide.plugin.go
        rust-lsp:   com.koner.rust               → com.scto.mobile.ide.plugin.rust
        zig-lsp:    io.kiquar.plugin.zig         → com.scto.mobile.ide.plugin.zig
        fsharp-lsp: io.kiquar.plugin.fs          → com.scto.mobile.ide.plugin.fs
        prettier-lsp: com.koner.prettier         → com.scto.mobile.ide.plugin.prettier
        (catalog.json in assets/Plugins/LSP/ hat VORRANG: Sollte eine ID dort 
         anders lauten, diese übernehmen und Abweichung dokumentieren.)
     b) package-Deklarationen in ALLEN .kt/.java-Dateien ändern,
     c) import <altes-package>.* → import <neues-package>.* NUR für EIGENE 
        Klassen; fremde SDK-Imports (com.rk.extension.*, com.rk.file.* usw. 
        aus sdk.jar) NUR umschreiben, wenn es sich um MobileIDE-SDK-Klassen 
        handelt, die bereits unter com.scto.mobile.ide.* existieren — sonst 
        Referenz im Bericht als offenen Punkt markieren (SDK-Version prüfen!),
     d) AndroidManifest.xml: package-Attribut + voll-qualifizierte 
        android:name-Attribute,
     e) app/build.gradle.kts: namespace UND applicationId auf neues Package,
     f) manifest.json/plugin.json: id + mainClass auf neue Namen,
     g) proguard-rules.pro: Keep-Regeln mit alten Pfaden aktualisieren,
     h) code-interne Strings (Log-Tags, Intent-Actions, Reflektion) anpassen,
     i) Grep-Nachweis: 0 Treffer auf das alte Package im Modul.
   - Beachte Sonderfall xed-go vs xed-go-tools (beide io.kiquar.plugin.go): 
     go-tools NUR dokumentieren — NICHT als Modul in settings.gradle.kts 
     aktivieren, da Kollision.

PHASE 1.3 — copyPluginToAssets-TASK KORRIGIEREN
   - In JEDEM plugins/*/app/build.gradle.kts (bzw. in einer zentralen 
     Konvention, falls vorhanden): 
     into(File(rootDir, "../../app/src/main/assets/bundled_plugins"))
     → into(File(rootDir, "../../assets/Plugins/LSP"))
   - Falls mehrere Module denselben Task definieren: EIN gemeinsames 
     Konventions-Skript prüfen (rootProject build.gradle.kts, convention 
     plugins) und die Task-Logik dort zentralisieren; Duplikate entfernen.
   - Ziel: Der Task kopiert <plugin-id>-<version>.zip nach 
     assets/Plugins/LSP/ — der Ordner, der vom PluginStoreManager + 
     catalog.json gelesen wird.
   - Nach Korrektur: Gradle-Task einmal ausführen und prüfen, dass das ZIP im 
     KORREKTEN Zielordner landet und KEINE neuen Dateien in bundled_plugins/ 
     erzeugt werden.

PHASE 1.4 — bundled_plugins/ & ALT-ORDNER BEREINIGEN
   - app/src/main/assets/bundled_plugins/: Nach Sicherung (Phase 1.0) und 
     nachdem Phase 1.3 sicherstellt, dass kein Task mehr dorthin schreibt: 
     rekursiv löschen.
   - app/src/main/assets/ weiter prüfen: Sind dort noch alte Plugin-Reste 
     (z. B. lsp/, terminal/lsp-Reste)? Diese der asset-Bereinigung 
     unterziehen (vgl. früherer prompt-assets-clean): Nur behalten, was die 
     App wirklich braucht (terminal/, textmate/, queries/).
   - Prüfen, ob der asset-Zielpfad in build.gradle.kts der App 
     (sourceSets/main/assets) NUR noch ~/MobileIDE/assets (Root) referenziert 
     und assets/Plugins/LSP/ inkl. catalog.json ins APK gepackt wird.

PHASE 1.5 — MANIFEST-SCHEMATA & ROOTPROJECT-NAMEN VEREINHEITLICHEN
   - Manifest-Konvention festlegen (Doku in docs/plugin-catalog.md): 
     MobileIDE-Schema = { id, name, mainClass, version, description?, 
     author{displayName,github?}, license?, tags[], minAppVersion (INTEGER, 
     VersionCode), maxAppVersion?, repository?, category?, entryScript?, 
     fileExtensions[], dependencies[] }.
   - Jedes plugins/*/manifest.json auf dieses Schema validieren (jq + 
     schema/schema.json als Referenz). Fehlerhafte minAppVersion als String 
     (z. B. "0.0.1") auf INTEGER korrigieren oder aus dem Xed-Schema die 
     integer-Werte (87/88/95/99) übernehmen und im Bericht dokumentieren.
   - rootProject.name in settings.gradle.kts jedes Moduls auf einheitliches 
     Muster setzen: "MobileIDE Plugin: <Plugin-Name>" ODER exakt der Plugin-Id 
     (eine Konvention wählen und durchziehen). Mindestens: KEINE Namen mehr 
     mit "(Xed)", "Extension-Template", "Prettier (Xed)".

PHASE 1.6 — TOOLCHAIN VEREINHEITLICHEN (nur MobileIDE-plugins)
   - Alle 9 Module auf AGP 8.13.1, Kotlin 2.3.0, Gradle Wrapper 9.2.1 
     (entspricht dem im MobileIDE-Hauptprojekt verifizierten Stand) bringen.
   - jvmToolchain vereinheitlichen auf 17 (da die Sandbox-Umgebung auf dem 
     Gerät verlässlich JDK 17 auflöst; die Build-Logs belegen, dass JDK 21 
     NICHT gefunden wird: "Cannot find a Java installation ... 
     {languageVersion=21}"). Alternativ: jvmToolchain(21) entfernen und 
     kompilieren mit dem per idesetup installierten JDK. Entscheidung 
     dokumentieren und konsistent anwenden.
   - compileOptions source/targetCompatibility auf 17 vereinheitlichen.
   - Gradle-Toolchain-Fehler aus den build_logs (foojay resolver scheitert in 
     Sandbox) durch lokale JDK-Auflösung ersetzen: In gradle.properties je 
     Modul prüfen, ob org.gradle.java.home bzw. die Umgebung JAVA_HOME 
     korrekt gesetzt wird; sicherstellen, dass kein foojay-Resolver 
     erforderlich ist.

PHASE 1.7 — VALIDIERUNG TEIL 1
   - a) Grep projektweit (app/src, plugins/, assets/): 0 Treffer auf 
        com.rk., com.androdev., com.koner., io.kiquar. — Ausnahme: 
        Doku-Dateien (docs/, README, CHANGELOG) dürfen historische 
        Erwähnungen enthalten, müssen dann aber als "historisch" markiert sein.
   - b) Jedes Modul einzeln bauen:
        cd plugins/<plugin> && ./gradlew assembleRelease && ./gradlew :app:createFinalZip
        — MUSS BUILD SUCCESSFUL liefern (in der Sandbox bzw. auf dem Host 
        mit JDK 17). Fehlerfälle in build/plugin-build-results.tsv.
   - c) find app/src/main/assets/bundled_plugins — muss leer/nicht existent sein.
   - d) find assets/Plugins/LSP -name "*.zip" | wc -l == 26 (unverändert) UND 
        für die 9 geänderten Module: neue Zips vorhanden + catalog.json 
        (sha256, sizeBytes) aktualisiert.
   - e) Gesamt-App-Build: ./gradlew assembleDebug — BUILD SUCCESSFUL.
   - f) APK-Inhalt: unzip -l <apk> | grep assets/Plugins/LSP — 26 Zips + 
        catalog.json enthalten; KEINE bundled_plugins-Einträge.
   - COMMIT TEIL 1 (im App-Repo):
     "refactor(plugins): consolidate single source of truth, complete 
     com.scto.mobile.ide.* namespace migration, fix asset copy target, 
     unify toolchain"

══════════════════════════════════════════════════════════════════
TEIL 2 — APK-BUILDER-PFAD-FIX (Host ↔ Sandbox)
══════════════════════════════════════════════════════════════════

PHASE 2.0 — DIAGNOSE VERIFIZIEREN
   - Lokalisiere ApkBuilder im Modul :core:apk-builder 
     (app/src/main/java/com/scto/mobile/ide/... bzw. core/apk-builder/...).
   - Verifiziere die Fehlerquelle: Suche nach der Meldung 
     "Projektverzeichnis ungültig" bzw. "Project directory" + der 
     Existenzprüfung File(projectPath).exists().
   - Prüfe den Aufrufpfad: CodeEditScreen.kt handleRunApk → 
     ApkBuilder(...).configureProcessBuilder { DistroManager.buildProotCommand(...) }.
   - Führe auf dem Testgerät im MobileIDE-Terminal aus:
       ls -la /storage/emulated/0/MobileIDEProjects/MyApp    # Host-Sicht
       ls -la /sdcard/MobileIDEProjects/MyApp                # Host-Sicht
       # In der Sandbox (Ubuntu):
       echo $MOBILEIDE_PROJECT_DIR
       ls -la $MOBILEIDE_PROJECT_DIR
       ls -la /sdcard/ 2>/dev/null
       ls -la /storage/emulated/0/ 2>/dev/null
   - Protokolliere, ob (1) Host-Pfad existiert und Sandbox ihn nicht sieht 
     (Mapping-Fehler) oder (2) auch der Host-Pfad nicht existiert 
     (Projekt wirklich fehlt / falscher Pfad aus SAF-URI).

PHASE 2.1 — PFAD-ÜBERSETZUNGSFUNKTION IMPLEMENTIEREN
   - Erstelle in :core:apk-builder (oder :core:common, falls dort bereits 
     Sandbox-Helfer liegen) eine zentrale, testbare Funktion:
     fun PathTranslator.toSandboxPath(hostPath: String, sandboxHome: String): String
   - Regeln:
     a) startsWith("/storage/emulated/0/") → "/sdcard/" + Rest
     b) startsWith("/storage/emulated/")   → "/sdcard/" + Rest (nach slash)
     c) startsWith("/sdcard/")             → unverändert (bereits Sandbox-Form)
     d) sonst → unverändert
   - Zusätzlich eine Umkehrfunktion toHostPath für Ausgabepfade 
     (Sandbox-/sdcard → Host-/storage/emulated/0), damit das Ergebnis-APK 
     korrekt gefunden/installiert wird.
   - Unit-Tests für die Übersetzungsfunktion schreiben (auch ohne Gerät 
     lauffähig, z. B. in :core:apk-builder/src/test/).

PHASE 2.2 — APKBUILDER-INTEGRATION
   - In ApkBuilder:
     a) VOR der Existenzprüfung: projectPath = PathTranslator.toSandboxPath(projectPath)
        UND in der Fehlermeldung BOTH Pfade ausgeben 
        ("Host: X, Sandbox: Y"), damit künftige Fehler sofort diagnostizierbar sind.
     b) Der übersetzte Pfad wird an den PRoot-Prozess übergeben.
     c) Zusätzlich $MOBILEIDE_PROJECT_DIR im Prozess-Environment konsistent 
        auf den übersetzten Pfad setzen (setup.sh/init.sh lesen diese Variable — 
        PROGRESS.md 2026-07-03).
     d) Nach Abschluss: Wenn der Output-Pfad in der Sandbox liegt 
        (/sdcard/...), zurück auf den Host-Pfad übersetzen, bevor das APK 
        gescannt/installiert wird (BuildHelper.kt).

PHASE 2.3 — PROOT-BIND ABSICHERN (DistroManager.buildProotCommand)
   - Prüfe in DistroManager.buildProotCommand (Module :features:terminal bzw. 
     der Sandbox-Komponente), ob /storage/emulated/0 gebunden ist. 
   - Falls NICHT vorhanden, ergänze die Bindungen:
       -b /storage/emulated/0:/sdcard
       -b /storage/emulated/0:/storage/emulated/0
   - Damit sind BEIDE Schreibweisen in der Sandbox nutzbar. 
   - Prüfe, ob zusätzlich -b /sdcard:/sdcard nötig ist (je nach Android- 
     Version/Symlink). Dokumentiere die finale Bind-Liste im Bericht.
   - Beachte bestehende Bind-Sicherheitslogik (stat/vmstat-Prävention aus 
     PROGRESS.md): neue Bindings müssen die "can't sanitize binding"-Warnung 
     NICHT erzeugen — ggf. mit gleichem Muster wie bei stat/vmstat behandeln.

PHASE 2.4 — ZWEISTUFIGE VALIDIERUNG MIT LOG-AUSGABE
   - Implementiere in ApkBuilder eine Prüfung mit klarem Logcat:
     a) Host-Check: File(hostPath).exists() → log "HOST_EXISTS=true/false"
     b) Sandbox-Check: File(sandboxPath).exists() → log "SANDBOX_EXISTS=true/false"
     c) Fallback-Kette: "/storage/emulated/0/..." → "/sdcard/..." → 
        "$MOBILEIDE_PROJECT_DIR" (Env) — erste existierende Variante gewinnt, 
        Reihenfolge und Ergebnis loggen.
     d) Bei allen false: Fehlermeldung mit BOTH Pfaden + konkreter Ursache 
        ("Projekt existiert auf Host nicht" vs. "Sandbox-Bind fehlt") 
        ausgeben, KEINE kryptische Meldung.

PHASE 2.5 — VALIDIERUNG TEIL 2 (Gerät/Emulator)
   - a) Build: ./gradlew assembleDebug — BUILD SUCCESSFUL.
   - b) Neues Projekt MyApp aus Template anlegen (oder vorhandenes öffnen).
   - c) Play-Button in CodeEditScreen.kt drücken → kein 
      "[ERROR] [ApkBuilder] Projektverzeichnis ungültig"-Fehler mehr.
   - d) Build läuft in der Sandbox, APK wird gebaut, nach Erfolg als 
      installierbares APK gefunden/angezeigt (Host-Pfad-Übersetzung wirkt).
   - e) Logcat prüfen: HOST_EXISTS/SANDBOX_EXISTS-Zeilen vorhanden und 
      konsistent.
   - f) Regression: Projekt unter /sdcard/MobileIDEProjects/ und unter 
      /storage/emulated/0/MobileIDEProjects/ jeweils einmal bauen.
   - COMMIT TEIL 2 (im App-Repo):
     "fix(apk-builder): translate host paths to PRoot sandbox paths, bind 
     /storage/emulated/0, two-stage validation with diagnostics"

══════════════════════════════════════════════════════════════════
GESAMT-ABSCHLUSS
══════════════════════════════════════════════════════════════════
- Abschlussbericht build/plugin-consolidation-and-apkbuilder-report.md:
  a) Tabelle Teil 1: je Plugin (quelle | alt-package | neu-package | 
     manifest-ok | rootProject.name | toolchain | zip-aktuell | build ✅/❌)
  b) Tabelle Teil 2: Diagnose-Ergebnis | Pfad-Übersetzung | Bind-Liste | 
     Test-Ergebnisse (2 Pfadvarianten)
  c) Offene Punkte mit Behebungsempfehlung (z. B. SDK-API-Drift 
     com.rk.* vs. com.scto.mobile.ide.* in sdk.jar, go-tools-Kollision, 
     nicht migrierte Reste außerhalb der 9 Module).
- Gesamtaussage: "Plugins konsolidiert auf eine Source-of-Truth, alle 
  com.scto.mobile.ide.*-Packages konsistent, Assets sauber in 
  assets/Plugins/LSP, APK-Builder baut Projekte aus der Sandbox korrekt 
  mit /sdcard-Pfaden" ODER exakte Liste der offenen Punkte.
