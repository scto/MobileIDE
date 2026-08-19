Reconcile alle generierten Plugin-Reports und -Tabellen von MobileIDE
(com.scto.mobile.ide) mit dem tatsächlichen Code-Zustand. Ziel: EINE
widerspruchsfreie, verifizierte Datenbasis. Code ist die Wahrheit – Tabellen,
die dem Code widersprechen, werden korrigiert/neu generiert, nicht umgekehrt.

══════════════════════════════════════════════════════════════════
KONTEXT / VERIFIZIERTER IST-ZUSTAND
══════════════════════════════════════════════════════════════════
Vorhandene Artefakte unter ~/MobileIDE/build/:
  a) plugin-consolidation-inventory.tsv    (zeigt bei 6 Modulen ALT-Packages)
  b) plugin-package-mapping.tsv            (zeigt NEU-Packages, aber 3 Muster)
  c) plugin-consolidation-and-apkbuilder-report.md (behauptet Migration ✅)
  d) lsp-script-inventory.tsv              (server_binary/install_skript teils
                                            falsch befüllt)
  e) kotlin-lsp-consistency.tsv            (konsistent, unverändert lassen)
  f) apkbuilder-pathfix-report.md          (konsistent, unverändert lassen)
Verifizierte Widersprüche:
  1) Migrationsstatus: inventory.tsv listet für fsharp-lsp, go-lsp,
     prettier-lsp, rust-lsp, typst-lsp, zig-lsp noch io.kiquar.plugin.* /
     com.koner.*, während der Report com.scto.mobile.ide.plugin.* behauptet.
  2) Namenskonventionen gemischt: com.scto.mobile.ide.plugin.<name> (Singular),
     com.scto.mobile.ide.plugins.<name> (Plural), com.scto.mobile.ide.<name>_lsp
     (Unterstrich).
  3) json-lsp: Mapping-TSV → com.scto.mobile.ide.json_lsp, Report →
     com.scto.mobile.ide.plugin.json (dito python-lsp).
  4) rootProject.name weiterhin alt: "xed-fs", "Go-Extension",
     "Extension-Template", "Prettier (Xed)", "ZigExtension", "Python LSP".
  5) lsp-script-inventory.tsv: sdk.jar (254 KB) als "server_binary" erfasst;
     "test/integration_test.sh" als install_skript von xed-prettier;
     xed-go-tools "8.5 MB" unplausibel; xed-fsharp "nur-binary" + kein Skript
     widersprüchlich; Spalte version_gradle enthält AGP-Version 8.13.1 statt
     Plugin-Version.
  Maßgebliche Code-Quellen: ~/MobileIDE/plugins/<modul>/ (Gradle-Module),
  ~/MobileIDE/assets/Plugins/LSP/catalog.json (Store-IDs + sha256 + sizeBytes),
  ~/MobileIDE/assets/Plugins/LSP/*.zip (26 Zips).

══════════════════════════════════════════════════════════════════
PHASE A — CODE-WAHRHEIT FESTSTELLEN (die 9 aktiven Module)
══════════════════════════════════════════════════════════════════
A1. Für jedes Modul in ~/MobileIDE/plugins/{fsharp,go,json,lua,prettier,
    python,rust,typst,zig}-lsp den IST-Zustand am Code ermitteln:
    a) grep -r "^package " app/src/main/java/ → tatsächliches Root-Package,
    b) grep -r "import com\.$rk\|koner\|kiquar\|androdev$\." app/src/main/java/
       → verbleibende ALT-Imports (NICHT-Doku),
    c) grep namespace + applicationId in app/build.gradle.kts,
    d) id + mainClass in manifest.json/plugin.json,
    e) rootProject.name in settings.gradle.kts,
    f) version-Feld in app/build.gradle.kts (Plugin-Version, NICHT AGP),
    g) zip-Name in assets/Plugins/LSP/ + zugehöriger catalog.json-Eintrag.
A2. Ergebnis je Modul als build/reconciliation-code-truth.tsv:
    modul | root_package_ist | alt_imports_anzahl | namespace_ist |
    appid_ist | manifest_id | rootProject_name | version_gradle |
    zip_name | catalog_id
A3. MIGRATIONS-URTEIL je Modul:
    - "MIGRIERT"  = root_package == com.scto.mobile.ide.plugin.* UND
                    0 ALT-Imports UND namespace/applicationId konsistent,
    - "TEILMIGRIERT" = package neu, aber ALT-Imports > 0 ODER
                    applicationId/manifest noch alt,
    - "NICHT MIGRIERT" = package noch io.kiquar.*/com.koner.*/com.rk.*.
    Diese drei Kategorien sind die EINZIGE Wahrheit für alle Folge-Entscheidungen.

══════════════════════════════════════════════════════════════════
PHASE B — NAMENSKONVENTION VERBINDLICH FESTSCHREIBEN
══════════════════════════════════════════════════════════════════
B1. EINZIGE Konvention festlegen (Abweichungen sind Fehler):
    com.scto.mobile.ide.plugin.<name>
    - name = Plugin-ID ohne Präfix, in lowercase, ohne Unterstriche
      (json-lsp → plugin.json, python-lsp → plugin.python, kmp-lsp →
      plugin.kmplsp).
    - VERBOTEN: plugins (Plural), <name>_lsp (Unterstrich), Mischformen.
B2. Sollte catalog.json (maßgeblich für Store) eine abweichende ID verwenden,
    entscheidet catalog.json über die ÖFFENTLICHE id, aber der PACKAGE-Name
    folgt B1. Abweichung im Report dokumentieren (id ≠ package ist erlaubt,
    MUSS aber explizit vermerkt sein).
B3. Migration aller Module, die laut A3 "NICHT MIGRIERT" oder
    "TEILMIGRIERT" sind, auf das B1-Muster abschließen (gleiche
    Schrittfolge wie zuvor: Verzeichnis verschieben → package/import →
    Manifest/Manifest → build.gradle.kts namespace/applicationId →
    proguard → String-Referenzen → Grep-Nachweis 0).
    - json-lsp/python-lsp: KONFLIKT aus Widerspruch 3 auflösen → verbindlich
      com.scto.mobile.ide.plugin.json bzw. com.scto.mobile.ide.plugin.python
      (Report-Variante), sofern keine andere Vorgabe aus catalog.json.
    - Führe danach für ALLE 9 Module den Grep-Nachweis aus A1b aus und
      aktualisiere build/reconciliation-code-truth.tsv (root_package_ist,
      alt_imports_anzahl).

══════════════════════════════════════════════════════════════════
PHASE C — rootProject.name VEREINHEITLICHEN
══════════════════════════════════════════════════════════════════
C1. Konvention: "MobileIDE Plugin: <Plugin-Name>" (z. B.
    "MobileIDE Plugin: JSON LSP"). Alternativ exakt die Plugin-ID —
    EINE Konvention wählen und durchziehen.
C2. In jedem settings.gradle.kts der 9 Module anpassen; danach:
    cd plugins/<modul> && ./gradlew projects — MUSS den neuen Namen zeigen.

══════════════════════════════════════════════════════════════════
PHASE D — lsp-script-inventory.tsv NEU GENERIEREN (korrekt)
══════════════════════════════════════════════════════════════════
D1. Für jedes der 14 Quell-Repos unter ~/Plugins/lsp/xed-* (Upstream-Spiegel)
    die SPALTEN korrekt bestimmen:
    - install_skript: NUR Dateien unter app/src/main/assets/*.sh bzw.
      assets/*.sh, die tatsächlich einen Server installieren
      (gopls-installer.sh, install-gopls.sh, java-lsp.sh,
      kmp-lsp-installer.sh, kotlin-lsp-installer.sh, rust-lsp.sh,
      typst-cli.sh, typst-lsp.sh, zig-installer.sh).
      KEINE Testskripte (test/integration_test.sh), KEINE Gradle-Skripte.
    - server_binary: NUR echte LSP-Server-Binaries im Repo
      (bin/*/light-json-lsp, bin/*/emmylua_ls, bin/*/ty, …).
      sdk.jar, gradle-wrapper.jar, *.aar, *.apk sind KEINE Server-Binaries.
    - groesse_mb: echte Dateigrößen der erfassten Binaries (Stat),
      keine Schätzwerte.
    - aktion: nur für Dateien, die tatsächlich zu löschen/zu behalten sind,
      basierend auf dem aktuellen Stand (nicht aus dem Gedächtnis).
D2. Für Plugins ohne Install-Skript aber mit Binary: "nur-binary".
    Für Plugins ohne beides: "kein-server" (xed-prettier-standalone).
D3. Grep-Verifikation: jede Zeile der neuen TSV muss per find/stat
    nachprüfbar sein; keine Fantasie-Pfade.

══════════════════════════════════════════════════════════════════
PHASE E — ALLE TABELLEN KONSISTENT NEU AUSGEBEN
══════════════════════════════════════════════════════════════════
E1. plugin-consolidation-inventory.tsv NEU:
    modul | package_ist (aus A) | version_manifest (aus manifest.json) |
    version_gradle (aus build.gradle.kts VERSION-Feld, NICHT AGP) |
    agp_version (eigene Spalte) | jvmToolchain | rootProject_name (neu) |
    zip_name | catalog_id | migration_status (A3) | build_status.
E2. plugin-package-mapping.tsv NEU:
    plugin_id | quelle_mapping (alt→neu, NUR das B1-Muster) | status.
E3. plugin-consolidation-and-apkbuilder-report.md AKTUALISIEREN:
    bestehende Abschnitte erhalten, NEUER Abschnitt
    "Reconciliation (Datum)" mit: Code-Wahrheit-Tabelle (A2),
    Migrations-Urteile (A3), aufgelöste Widersprüche (1–5) mit Nachweis,
    verbleibende offene Punkte.
E4. Widerspruchs-Check zum Schluss:
    grep alte Packages in app/src/main/java + plugins/*/app/src/main/java
    → MUSS 0 sein (Ausnahme: docs/, README, CHANGELOG, die historische
    Erwähnungen mit Kommentar "historisch" enthalten dürfen).

══════════════════════════════════════════════════════════════════
PHASE F — VALIDIERUNG
══════════════════════════════════════════════════════════════════
F1. Je Modul: ./gradlew assembleRelease + :app:createFinalZip → BUILD
    SUCCESSFUL (für alle 9).
F2. Gesamt: ./gradlew assembleDebug → BUILD SUCCESSFUL.
F3. Zips: find assets/Plugins/LSP -name "*.zip" | wc -l == 26 UND
    catalog.json (sha256, sizeBytes) stimmt mit den Dateien überein
    (sha256sum -c).
F4. Keine Regression am APK-Builder: Projekt MyApp bauen → kein
    "does not contain a Gradle build".
F5. Commit: "docs(reconcile): align plugin tables with code truth, unify
    package convention com.scto.mobile.ide.plugin.*, regenerate
    lsp-script-inventory"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGEL:
- Phase A MUSS für jedes Modul einen eindeutigen Befund liefern. Ist ein
  Modul im Code nicht auffindbar (Ordner fehlt), NICHT raten — als
  "MISSING" markieren und im Report als offener Punkt listen.
- Phase B3 darf nur Module ändern, deren Status aus A3 "NICHT MIGRIERT"
  oder "TEILMIGRIERT" ist. Bereits migrierte Module bleiben unangetastet.
