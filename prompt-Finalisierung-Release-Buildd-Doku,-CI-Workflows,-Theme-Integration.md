Führe die vier Abschlussaufgaben für MobileIDE (com.scto.mobile.ide) in der 
vorgegebenen Reihenfolge als separate Phasen aus. Nach jeder Phase den 
Berichtsteil ausfüllen. KEINE Phase überspringen, KEINE Reihenfolge ändern.

PROJEKT-KONTEXT:
- App-Repo: ~/MobileIDE (Branch main, letzter Commit 3d6bf8dd, sauber)
- Plugin-Quell-Repos: ~/Plugins
    ├── lsp/  (13 refaktorierte Quell-Plugins auf com.scto.mobile.ide.*)
    │   ├── xed-fsharp, xed-go, xed-go-tools, xed-java-lsp, xed-json-lsp,
    │   ├── xed-kmp-lsp, xed-kotlin, xed-lua-lsp, xed-prettier,
    │   ├── xed-prettier-standalone (nur Node/TS), xed-python-lsp, xed-rust,
    │   ├── xed-typst, xed-zig
    │   └── scto/ (Referenz-ZIPs com.scto.mobile.ide.*.zip, NICHT anfassen)
    └── theme/
        ├── xed-feslake/ (README.md, icon.png, theme.json, .github/workflows/zip.yml)
        └── xed-themes/ (themes/*.json = 11 Theme-Dateien, schema.json, 
                         screenshots/, README.md)
- ZIP-Katalog: ~/MobileIDE/assets/Plugins/LSP/ (26 Zips + catalog.json + README.md)
- Bereits erstellt: build/verification-report.md (Commit 3d6bf8dd)

══════════════════════════════════════════════════════════════════
PHASE A — RELEASE-BUILD MIT APK-SIGNIERUNG
══════════════════════════════════════════════════════════════════

A1. SIGNING-KONFIGURATION PRÜFEN/ANLEGEN
   - Prüfe ~/MobileIDE/app/build.gradle.kts auf ein vorhandenes signingConfigs:
     - Falls bereits konfiguriert: nur prüfen und nutzen.
     - Falls NICHT: Lege eine debug-taugliche Release-Signing-Konfiguration an:
       a) Keystore erzeugen via keytool:
          keytool -genkeypair -v -keystore ~/MobileIDE/keystore/release.jks \
                  -alias mobileide -keyalg RSA -keysize 2048 -validity 10000 \
                  -storepass <generiertes-Passwort> -keypass <generiertes-Passwort> \
                  -dname "CN=MobileIDE, OU=Mobile, O=com.scto.mobile.ide, L=, S=, C=DE"
       b) Passwörter in ~/MobileIDE/keystore/keystore.properties speichern 
          (storeFile, storePassword, keyAlias, keyPassword) — KEINE Passwörter 
          in build.gradle.kts oder Git-Commit schreiben!
       c) keystore.properties in .gitignore aufnehmen.
       d) build.gradle.kts: signingConfigs { release { ... } } konfigurieren 
          und buildTypes.release.signingConfig = signingConfigs.release setzen.
   - Falls in der Zukunft ein echtes Release-Keyfile eingesetzt wird: 
     ausschließlich darüber signieren (kein Hardcoding).

A2. RELEASE-BUILD
   - Ausführen: ./gradlew clean assembleRelease (in ~/MobileIDE)
   - MUSS mit Exit-Code 0 enden (BUILD SUCCESSFUL).
   - Warnungen im Log dokumentieren (compileSdk-Warnung, Deprecated-Features), 
     KEINE davon darf ein ERROR sein.
   - APK-Pfad notieren: app/build/outputs/apk/release/*.apk

A3. APK-VERIFIZIERUNG
   - apksigner verify --verbose --print-certs <apk> ausführen:
     a) v1/v2/v3-Signaturen vorhanden,
     b) Zertifikats-Details (Alias, Hash) notieren.
   - Größe + SHA-256 des APK berechnen:
     sha256sum <apk> → build/release-artifact-info.tsv speichern 
     (datei | groesse_bytes | sha256).
   - Inhalt prüfen: unzip -l <apk> | grep -c "assets/Plugins/LSP/.*\.zip" 
     == 26 UND assets/Plugins/LSP/catalog.json vorhanden.
   - Ergebnis in build/verification-report.md als neues Kapitel 
     "Release-Build & Signierung" ergänzen (Datum, APK-Pfad, Größe, SHA-256, 
     Signatur-Algorithmen, Zertifikat-Hash).

A4. (OPTIONAL, nur falls Gerät verfügbar) RELEASE-APK AUF GERÄT TESTEN
   - adb install -r <apk>, App starten, Plugin-Store öffnet 26 Plugins, 
     Rust-Plugin installieren/deinstallieren, Terminal + LSP-Status OK.

[PHASE-A-BERICHT: APK-Pfad, Größe, SHA-256, Signatur-Status, 
BUILD SUCCESSFUL-Bestätigung.]

══════════════════════════════════════════════════════════════════
PHASE B — DOKUMENTATION PFLEGEN
══════════════════════════════════════════════════════════════════

B1. VERIFIKATIONSBERICHT ALS REFERENZ-DOKUMENT ABLEGEN
   - ~/MobileIDE/build/verification-report.md erweitern/aktualisieren:
     a) Abschnitt "Plugin-Migration & Package-Refactoring" (13 Plugins, 
        Mapping-Tabelle alt→neu),
     b) Abschnitt "ZIP-Katalog" (26 Archive, Katalog konsistent),
     c) Abschnitt "Release-Build & Signierung" (aus Phase A),
     d) Abschnitt "CI-Workflows" (aus Phase C),
     e) Abschnitt "Themes" (aus Phase D).
   - Das Dokument nach docs/verification-report.md in das Repo übernehmen 
     (Kopie im build/-Ordner zusätzlich belassen).
   - Verlinkung: In ~/MobileIDE/README.md einen Abschnitt 
     "📦 Plugin-System & Verifikation" ergänzen, der auf 
     docs/verification-report.md und docs/plugin-catalog.md verweist.

B2. PLUGIN-KATALOG-DOKUMENTATION
   - docs/plugin-catalog.md erzeugen mit:
     a) Tabelle aller 26 Plugins (ID | Version | Kategorie | Dateiendungen | 
        SHA-256 gekürzt | Quelle: bundled | store-zip),
     b) Beschreibung der 12 fest gebündelten LSP-Sprachen + der 
        Store-ZIP-Installation (assets/Plugins/LSP/),
     c) Schema-Beschreibung von plugin.json/manifest.json (Felder und 
        Beispiel-JSON),
     d) Referenz auf catalog.json.
   - docs/plugins-migration.md erzeugen: Zusammenfassung der Migration 
     (bundled_plugins → assets/Plugins/, ZIP-Erzeugung, Package-Refactoring 
     auf com.scto.mobile.ide.*, offene Punkte).

B3. GIT-COMMIT DER DOKUMENTATION
   - Git add/commit der neuen Dateien (docs/verification-report.md, 
     docs/plugin-catalog.md, docs/plugins-migration.md, README.md-Update).
   - Commit-Message: "docs: add plugin verification report, catalog and 
     migration documentation"
   - Push auf origin/main.

[PHASE-B-BERICHT: Liste der erzeugten/aktualisierten Dokumente, 
Commit-Hash, Push-Status.]

══════════════════════════════════════════════════════════════════
PHASE C — CI-WORKFLOWS DER PLUGIN-REPOS PRÜFEN & KORRIGIEREN
══════════════════════════════════════════════════════════════════

C1. INVENTAR DER VORHANDENEN WORKFLOWS
   - Alle .github/workflows/*.yml unter ~/Plugins listen:
     a) lsp/xed-fsharp/.github/workflows/plugin-build.yml
     b) lsp/xed-go/.github/workflows/plugin-build-test.yml
     c) lsp/xed-go-tools/.github/workflows/plugin-release.yml
     d) lsp/xed-java-lsp/.github/workflows/plugin-build-test.yml
     e) lsp/xed-json-lsp/.github/workflows/plugin-build-test.yml
     f) lsp/xed-kmp-lsp/.github/workflows/plugin-build-test.yml
     g) lsp/xed-kotlin/.github/workflows/plugin-build.yml
     h) lsp/xed-lua-lsp/.github/workflows/plugin-build-test.yml
     i) lsp/xed-prettier/.github/workflows/plugin-build-test.yml
     j) lsp/xed-prettier/prettier-standalone/.github/workflows/ci.yml, 
        cd.yml, automation-autorelease.yml
     k) lsp/xed-prettier-standalone/.github/workflows/ci.yml, cd.yml, 
        automation-autorelease.yml
     l) lsp/xed-python-lsp/.github/workflows/plugin-build-test.yml
     m) lsp/xed-rust/.github/workflows/plugin-build-test.yml
     n) lsp/xed-typst/.github/workflows/plugin-build-test.yml
     o) lsp/xed-zig/.github/workflows/plugin-build-test.yml
     p) lsp/xed-json-lsp/light-json-lsp/.github/workflows/release.yml (Rust)
     q) theme/xed-feslake/.github/workflows/zip.yml
   - In build/ci-workflow-inventory.tsv erfassen: 
     repo | workflow | typ (build|test|release|cd) | letzter Status 
     (unbekannt/nicht ausgeführt).

C2. PRÜFKATEGORIEN JE WORKFLOW (LSP-QUELTPLUGINS)
   - a) Package-Konsistenz: Grep in Workflow-Dateien nach ALTEN 
     Package-Namen (io.kiquar.plugin.*, com.rk.*, com.androdev.*, 
     com.koner.*, com.rk.demo) — diese dürfen NACH dem Refactoring 
     NICHT mehr auftauchen.
   - b) Build-Skript-Konsistenz: Workflows rufen compileRelease/compileDebug 
     bzw. ./gradlew auf. Prüfen, dass die gerufenen Skripte/Tasks EXISTIEREN:
     - Plugins mit buildExtensionRelease/buildExtensionDebug-Tasks 
       (xed-go, xed-kotlin) → Workflow muss ./gradlew buildExtensionRelease 
       bzw. die passende Variante aufrufen,
     - Plugins mit createFinalZip (xed-rust, xed-typst, xed-java-lsp, ...) 
       → Workflow muss assembleRelease + :app:createFinalZip aufrufen,
     - Plugins, deren compileRelease-Skript `assembleDebug` nutzt 
       (laut build_log.txt-Vorbild) → Workflow entsprechend anpassen.
   - c) Artifact-Pfade: path: output/*.zip prüfen — der output/-Ordner wird 
     durch createFinalZip/buildExtension* erzeugt; sicherstellen, dass 
     beim CI-Lauf das ZIP tatsächlich unter output/ entsteht.
   - d) JDK/Toolchain: setup-java mit Java 21 konfigurieren, da die Plugins 
     jvmToolchain(21) nutzen (bei xed-json-lsp Toolchain 17 beachten — 
     ggf. Matrix oder separaten Job).
   - e) Manifest/icon: Für Plugins, deren createFinalZip icon.png, README.md, 
     CHANGELOG.md erwartet — sicherstellen, dass diese Dateien im Repo 
     liegen (sonst Build-Fehler im CI).

C3. KORREKTUREN
   - Defekte/überholte Workflows nach C2 korrigieren:
     a) Alte Package-Namen entfernen/ersetzen (falls in Pfaden oder 
        artefaktnamen verwendet),
     b) Falsche Gradle-Task-Namen auf die tatsächlich vorhandenen Tasks 
        umschreiben,
     c) Fehlende chmod +x auf ./gradlew und compile*-Skripte ergänzen 
        (im Workflow-Step, da Git-Berechtigungen oft verloren gehen),
     d) Bei rustbasiertem light-json-lsp/release.yml prüfen, ob der 
        Release-Tag-Flow (push tags v*) zum Plugin-Build passt; nur 
        dokumentieren, NICHT umbauen (gehört zu JSON-LSP-Binary).
   - Alle Änderungen in den jeweiligen Plugin-Repos committen:
     "ci: align workflows with com.scto.mobile.ide package refactoring"

C4. CI-AUSFÜHRUNG TESTEN (falls Remote-Zugriff möglich)
   - Falls GitHub-Access vorhanden: Einen Push auf main in einem 
     repräsentativen Plugin (z. B. xed-rust) auslösen und den 
     Workflow-Lauf beobachten — MUSS grün sein.
   - Falls kein Zugriff: Lokal den Workflow-Inhalt simulieren 
     (chmod +x gradlew compileRelease; ./compileRelease) und das Ergebnis 
     protokollieren.

[PHASE-C-BERICHT: Inventar-Tabelle, pro Workflow: geprüft ✅/❌ + Aktion 
(korrigiert/unverändert/nicht ausgeführt), Ergebnis der Testläufe.]

══════════════════════════════════════════════════════════════════
PHASE D — THEME-INTEGRATION (LÜCKEN SCHLIESSEN)
══════════════════════════════════════════════════════════════════

D1. BESTANDSAUFNAHME THEMES
   - ~/Plugins/theme/xed-feslake/: README.md, icon.png, theme.json 
     (eigenes Theme, Workflow zip.yml erzeugt feslake-theme.xed)
   - ~/Plugins/theme/xed-themes/: themes/ mit 11 JSON-Themes 
     (catppuccin-frappe, darcula, dark-modern, github, itsaky, 
     monokai-ryo-anthracite, one-dark-pro, one-monokai, solar, 
     tokyo-night-pro, vs-2017-dark), schema.json, screenshots/, README.md
   - Prüfen, wie die MobileIDE-App Themes lädt:
     a) Suche in ~/MobileIDE nach Theme-Klassen/Ordnern 
        (z. B. ThemeManager, themes/, theme.json-Parser, 
        EditorColorScheme-Implementierungen),
     b) Stelle fest, ob die App Themes aus assets/, aus 
        ~/Downloads/MobileIDE/themes oder per .xed-Datei (zip) lädt,
     c) Prüfe das Schema: xed-themes/schema.json mit den erwarteten 
        Feldern der App abgleichen (syntax-Namen, editor-Farben, ui-Theme, 
        dark/light).

D2. XED-FESLAKE INTEGRIEREN
   - Ziel: xed-feslake als vollwertiges Theme in MobileIDE verfügbar machen.
   - a) Struktur der App ermitteln (wo liegen bestehende Themes) und das 
      Theme entsprechend ablegen (assets/themes/feslake.theme.json bzw. 
      im Theme-Verzeichnis der App),
   - b) theme.json auf das App-Schema normalisieren (falls Abweichungen), 
   - c) icon.png übernehmen,
   - d) Standard-Konvention: dunkles Theme, editor-Hintergrund, 
      syntax-Farben — nach Schema prüfen,
   - e) In der App-Themenliste registrieren (falls Liste statisch: Eintrag 
      ergänzen; falls dynamisch: Verzeichnis-Scan reicht).

D3. XED-THEMES-SAMMLUNG INTEGRIEREN
   - Alle 11 Themes aus ~/Plugins/theme/xed-themes/themes/*.json in die 
     App-Theme-Struktur übernehmen.
   - a) Schema-Validierung: jedes Theme gegen das App-Schema prüfen 
      (jq bzw. manueller Feldabgleich), defekte/leere Dateien aussortieren 
      und in build/theme-validation.log notieren,
   - b) Namens-Konvention der App beachten (Dateinamen, IDs),
   - c) screenshots/ in docs/ übernehmen (optional, nur als Referenz),
   - d) schema.json von xed-themes mit dem App-Schema abgleichen — falls 
      das App-Schema zusätzliche Pflichtfelder hat, Theme-Dateien 
      entsprechend ergänzen (ohne die Original-Farbwerte zu verändern).

D4. VALIDIERUNG THEMES
   - App bauen (./gradlew assembleDebug) und starten:
     a) Themenliste enthält feslake + 11 Themes,
     b) Theme-Wechsel funktioniert (Darstellung, Editor-Syntax-Highlighting),
     c) Kein Crash beim Laden jedes einzelnen Themes,
     d) Dark/Light-Korrektzuordnung prüfen.
   - Ergebnis je Theme in build/theme-validation.log (thema | schema-ok | 
     geladen | getestet).

D5. COMMIT & PUSH
   - Theme-Dateien + Validierungslog im App-Repo committen:
     "feat(theme): integrate feslake and 11 community themes from ~/Plugins"

[PHASE-D-BERICHT: Tabelle der 12 Themes (Name | Quelle | Schema-OK | 
Status), Liste der ausgeschlossenen Dateien (falls vorhanden), 
Bestätigung dass der Theme-Wechsel getestet wurde.]

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase A: Fehlschlagen von assembleRelease oder apksigner → Stopp, 
  Bericht mit Fehlerdetails, KEINE weitere Phase.
- Phase B: Nur Dokumentation — kein harter Abbruch, aber fehlende 
  Dokumente als offene Punkte markieren.
- Phase C: Korrigierte Workflows müssen lokal zumindest den Gradle-Task 
  erfolgreich ausführen; sonst als offener Punkt im Bericht, weiter zu D.
- Phase D: Einzelne defekte Theme-Dateien dürfen ausgeschlossen werden 
  (Log), ABER nicht mehr als 2 von 12.

GIB AM ENDE:
- Abschlussbericht aller vier Phasen (A–D) mit den jeweiligen Tabellen,
- Gesamtübersicht: Release-Build ✅/❌ | Signatur ✅/❌ | Doku ✅/❌ | 
  CI ✅/❌ | Themes ✅/❌,
- Offene Punkte mit konkreten Behebungsempfehlungen,
- finales Baum-Diagramm von ~/MobileIDE/docs/ und der neuen 
  Theme-Verzeichnisse.
```rch createFinalZip/buildExtension* erzeugt; sicherstellen, dass 
     beim CI-Lauf das ZIP tatsächlich unter output/ entsteht.
   - d) JDK/Toolchain: setup-java mit Java 21 konfigurieren, da die Plugins 
     jvmToolchain(21) nutzen (bei xed-json-lsp Toolchain 17 beachten — 
     ggf. Matrix oder separaten Job).
   - e) Manifest/icon: Für Plugins, deren createFinalZip icon.png, README.md, 
     CHANGELOG.md erwartet — sicherstellen, dass diese Dateien im Repo 
     liegen (sonst Build-Fehler im CI).

C3. KORREKTUREN
   - Defekte/überholte Workflows nach C2 korrigieren:
     a) Alte Package-Namen entfernen/ersetzen (falls in Pfaden oder 
        artefaktnamen verwendet),
     b) Falsche Gradle-Task-Namen auf die tatsächlich vorhandenen Tasks 
        umschreiben,
     c) Fehlende chmod +x auf ./gradlew und compile*-Skripte ergänzen 
        (im Workflow-Step, da Git-Berechtigungen oft verloren gehen),
     d) Bei rustbasiertem light-json-lsp/release.yml prüfen, ob der 
        Release-Tag-Flow (push tags v*) zum Plugin-Build passt; nur 
        dokumentieren, NICHT umbauen (gehört zu JSON-LSP-Binary).
   - Alle Änderungen in den jeweiligen Plugin-Repos committen:
     "ci: align workflows with com.scto.mobile.ide package refactoring"

C4. CI-AUSFÜHRUNG TESTEN (falls Remote-Zugriff möglich)
   - Falls GitHub-Access vorhanden: Einen Push auf main in einem 
     repräsentativen Plugin (z. B. xed-rust) auslösen und den 
     Workflow-Lauf beobachten — MUSS grün sein.
   - Falls kein Zugriff: Lokal den Workflow-Inhalt simulieren 
     (chmod +x gradlew compileRelease; ./compileRelease) und das Ergebnis 
     protokollieren.

[PHASE-C-BERICHT: Inventar-Tabelle, pro Workflow: geprüft ✅/❌ + Aktion 
(korrigiert/unverändert/nicht ausgeführt), Ergebnis der Testläufe.]

══════════════════════════════════════════════════════════════════
PHASE D — THEME-INTEGRATION (LÜCKEN SCHLIESSEN)
══════════════════════════════════════════════════════════════════

D1. BESTANDSAUFNAHME THEMES
   - ~/Plugins/theme/xed-feslake/: README.md, icon.png, theme.json 
     (eigenes Theme, Workflow zip.yml erzeugt feslake-theme.xed)
   - ~/Plugins/theme/xed-themes/: themes/ mit 11 JSON-Themes 
     (catppuccin-frappe, darcula, dark-modern, github, itsaky, 
     monokai-ryo-anthracite, one-dark-pro, one-monokai, solar, 
     tokyo-night-pro, vs-2017-dark), schema.json, screenshots/, README.md
   - Prüfen, wie die MobileIDE-App Themes lädt:
     a) Suche in ~/MobileIDE nach Theme-Klassen/Ordnern 
        (z. B. ThemeManager, themes/, theme.json-Parser, 
        EditorColorScheme-Implementierungen),
     b) Stelle fest, ob die App Themes aus assets/, aus 
        ~/Downloads/MobileIDE/themes oder per .xed-Datei (zip) lädt,
     c) Prüfe das Schema: xed-themes/schema.json mit den erwarteten 
        Feldern der App abgleichen (syntax-Namen, editor-Farben, ui-Theme, 
        dark/light).

D2. XED-FESLAKE INTEGRIEREN
   - Ziel: xed-feslake als vollwertiges Theme in MobileIDE verfügbar machen.
   - a) Struktur der App ermitteln (wo liegen bestehende Themes) und das 
      Theme entsprechend ablegen (assets/themes/feslake.theme.json bzw. 
      im Theme-Verzeichnis der App),
   - b) theme.json auf das App-Schema normalisieren (falls Abweichungen), 
   - c) icon.png übernehmen,
   - d) Standard-Konvention: dunkles Theme, editor-Hintergrund, 
      syntax-Farben — nach Schema prüfen,
   - e) In der App-Themenliste registrieren (falls Liste statisch: Eintrag 
      ergänzen; falls dynamisch: Verzeichnis-Scan reicht).

D3. XED-THEMES-SAMMLUNG INTEGRIEREN
   - Alle 11 Themes aus ~/Plugins/theme/xed-themes/themes/*.json in die 
     App-Theme-Struktur übernehmen.
   - a) Schema-Validierung: jedes Theme gegen das App-Schema prüfen 
      (jq bzw. manueller Feldabgleich), defekte/leere Dateien aussortieren 
      und in build/theme-validation.log notieren,
   - b) Namens-Konvention der App beachten (Dateinamen, IDs),
   - c) screenshots/ in docs/ übernehmen (optional, nur als Referenz),
   - d) schema.json von xed-themes mit dem App-Schema abgleichen — falls 
      das App-Schema zusätzliche Pflichtfelder hat, Theme-Dateien 
      entsprechend ergänzen (ohne die Original-Farbwerte zu verändern).

D4. VALIDIERUNG THEMES
   - App bauen (./gradlew assembleDebug) und starten:
     a) Themenliste enthält feslake + 11 Themes,
     b) Theme-Wechsel funktioniert (Darstellung, Editor-Syntax-Highlighting),
     c) Kein Crash beim Laden jedes einzelnen Themes,
     d) Dark/Light-Korrektzuordnung prüfen.
   - Ergebnis je Theme in build/theme-validation.log (thema | schema-ok | 
     geladen | getestet).

D5. COMMIT & PUSH
   - Theme-Dateien + Validierungslog im App-Repo committen:
     "feat(theme): integrate feslake and 11 community themes from ~/Plugins"

[PHASE-D-BERICHT: Tabelle der 12 Themes (Name | Quelle | Schema-OK | 
Status), Liste der ausgeschlossenen Dateien (falls vorhanden), 
Bestätigung dass der Theme-Wechsel getestet wurde.]

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase A: Fehlschlagen von assembleRelease oder apksigner → Stopp, 
  Bericht mit Fehlerdetails, KEINE weitere Phase.
- Phase B: Nur Dokumentation — kein harter Abbruch, aber fehlende 
  Dokumente als offene Punkte markieren.
- Phase C: Korrigierte Workflows müssen lokal zumindest den Gradle-Task 
  erfolgreich ausführen; sonst als offener Punkt im Bericht, weiter zu D.
- Phase D: Einzelne defekte Theme-Dateien dürfen ausgeschlossen werden 
  (Log), ABER nicht mehr als 2 von 12.

GIB AM ENDE:
- Abschlussbericht aller vier Phasen (A–D) mit den jeweiligen Tabellen,
- Gesamtübersicht: Release-Build ✅/❌ | Signatur ✅/❌ | Doku ✅/❌ | 
  CI ✅/❌ | Themes ✅/❌,
- Offene Punkte mit konkreten Behebungsempfehlungen,
- finales Baum-Diagramm von ~/MobileIDE/docs/ u	
