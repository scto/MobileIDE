Führe die komplette Plugin-Integration für MobileIDE (com.scto.mobile.ide) in 
EINEM Durchlauf aus: LSP-Server-Entbündelung → Plugin-Store-Generator → 
GitHub Pages → CI-Korrektur → Theme-Integration → Release-Build → Doku → 
Gesamtverifikation. Keine Phase überspringen, keine Reihenfolge ändern. Nach 
jeder Phase den jeweiligen Berichtsteil in build/ ausfüllen.

PROJEKT-KONTEXT:
- App-Repo: ~/MobileIDE (Branch main, Commit 3d6bf8dd, aktuell sauber)
- Plugin-Quell-Repos: ~/Plugins (13 Android-Plugin-Repos + Themes)
    lsp/xed-fsharp      (io.kiquar.plugin.fs, v0.1.0)
    lsp/xed-go          (io.kiquar.plugin.go, v0.2.0, gopls-installer.sh)
    lsp/xed-go-tools    (io.kiquar.plugin.go, v0.2.0, install-gopls.sh)
    lsp/xed-java-lsp    (com.androdev.java.lsp, v1.1.0, java-lsp.sh)
    lsp/xed-json-lsp    (com.rk.xededitor.json, v1.0.2, bin/*/light-json-lsp)
    lsp/xed-kmp-lsp     (io.kiquar.plugin.kmplsp, v0.1.2, kmp-lsp-installer.sh)
    lsp/xed-kotlin      (io.kiquar.plugin.kotlin, v0.2.1, kotlin-lsp-installer.sh)
    lsp/xed-lua-lsp     (com.rk.lua, v1.0.2, bin/*/emmylua_ls, emmylua_check, luafmt)
    lsp/xed-prettier    (com.koner.prettier, v1.0.0)
    lsp/xed-prettier-standalone (Node/Bun, KEIN Android-Modul)
    lsp/xed-python-lsp  (com.rk.xededitor.python, v1.0.3, bin/*/ty)
    lsp/xed-rust        (com.koner.rust, v1.0.2, rust-lsp.sh)
    lsp/xed-typst       (com.koner.typst, v1.2.3, typst-cli.sh, typst-lsp.sh)
    lsp/xed-zig         (io.kiquar.plugin.zig, v0.4.0, zig-installer.sh)
    lsp/scto/           (Referenz-ZIPs com.scto.mobile.ide.*.zip, NICHT anfassen)
    theme/xed-feslake   (io.kiquar.theme.feslake, theme.json + icon.png + zip.yml)
    theme/xed-themes    (11 Theme-JSONs + schema.json)
- Manifest-Format (schema/schema.json): id, name, mainClass, version, 
  description, author{displayName,github}, license, tags, minAppVersion, 
  maxAppVersion, repository
- App-ZIP-Katalog: ~/MobileIDE/assets/Plugins/LSP/ (26 Zips + catalog.json)
- Store-Referenzstruktur (aus docs.zip): docs/index.html, docs/plugins.json, 
  docs/plugins-catalog.json, docs/schema/plugins-index.schema.json, 
  docs/scripts/generate_plugin_store.py

══════════════════════════════════════════════════════════════════
PHASE 0 — BESTANDSAUFNAHME & BACKUP
══════════════════════════════════════════════════════════════════
- Backup: ~/MobileIDE/build/backup-<timestamp>/ (App-Repo relevantes) und 
  ~/Plugins komplett als plugins-backup-<timestamp>.tar.gz.
- build/inventory.tsv anlegen mit: plugin | id | version | mainClass | 
  install_skript | server_binary | workflow | build_skript | thema.
- Alle Manifeste unter ~/Plugins/lsp/*/manifest.json und 
  theme/*/theme.json per jq parsen und die Tabelle füllen.
- Grep nach Install-Skripten: find in app/src/main/assets -name "*.sh".
- Grep nach gebündelten Server-Binaries: bin/, server/, light-json-lsp/, 
  emmylua_ls, emmylua_check, luafmt, ty, rust-analyzer, gopls, zls, jdtls.
- GitHub-Workflows inventarisieren: .github/workflows/*.yml pro Plugin.

══════════════════════════════════════════════════════════════════
PHASE 1 — LSP-SERVER-ENTBÜNDELUNG (Install-Skripte prüfen & Server löschen)
══════════════════════════════════════════════════════════════════
1.1 FÜR JEDES PLUGIN MIT INSTALL-SKRIPT (assets/*.sh):
   - Skript öffnen und analysieren: Was installiert es zur Laufzeit 
     (npm-Paket, GitHub-Release-Download, Binary)?
     Erwartete Zuordnung:
     xed-go       -> gopls-installer.sh      -> gopls (GitHub/GOBIN)
     xed-go-tools -> install-gopls.sh        -> gopls
     xed-java-lsp -> java-lsp.sh             -> jdtls (Eclipse JDT LS)
     xed-kmp-lsp  -> kmp-lsp-installer.sh    -> kmp-lsp
     xed-kotlin   -> kotlin-lsp-installer.sh -> intellij-server
     xed-rust     -> rust-lsp.sh             -> rust-analyzer
     xed-typst    -> typst-lsp.sh, typst-cli.sh -> typst-lsp, typst
     xed-zig      -> zig-installer.sh        -> zls
   - GEBÜNDELTE SERVER-DUPLIKATE im Plugin finden und LÖSCHEN (nach Backup):
     xed-json-lsp/bin/*/light-json-lsp       (Rust-Binary — wird aus Skript 
                                              bzw. ist gebündelt -> Entscheidung:
                                              falls install-Skript existiert: löschen)
     xed-lua-lsp/bin/*/emmylua_ls, emmylua_check, luafmt
     xed-python-lsp/bin/*/ty
     sowie weitere via grep gefundene Binaries.
   - REGELN: Install-Skript selbst BLEIBT. App-LSP-Skripte unter 
     ~/MobileIDE/local/bin/lsp/ BLEIBEN. Unklare Dateien NICHT löschen, 
     als "unsicher" in build/lsp-deletion-manifest.tsv markieren.
   - Backup VOR jeder Löschung nach ~/MobileIDE/build/lsp-server-backup/.
   - plugin.json/manifest.json prüfen: Referenzen auf gelöschte Binaries 
     entfernen bzw. auf Install-Skript umstellen.
1.2 ZIP-NEUAUFBAU (falls Plugin als ZIP in assets/Plugins/LSP/ existiert):
   - ./gradlew createFinalZip bzw. buildExtensionRelease/buildExtensionDebug.
   - Neues ZIP darf keine Server-Binaries mehr enthalten (unzip -l | grep).
   - catalog.json aktualisieren: sha256, sizeBytes neu berechnen; Anzahl 
     bleibt 26.
1.3 VALIDIERUNG: unzip -t auf alle neuen Zips (fehlerfrei), 
   sha256sum == catalog.json, grep auf alte Server-Pfade == 0.
   Commits je Plugin: "chore: remove bundled <server>, install via script".
   Log: build/lsp-script-inventory.tsv + build/lsp-deletion-report.md.

══════════════════════════════════════════════════════════════════
PHASE 2 — PLUGIN-STORE-GENERATOR (Python, nach docs.zip-Vorbild)
══════════════════════════════════════════════════════════════════
2.1 ERSTELLE tools/generate_plugin_store.py (im App-Repo oder Store-Repo):
   - Argumente: --plugins-dir (Standard ~/Plugins), --docs-dir 
     (Standard ~/MobileIDE/docs), --zips-dir (Standard 
     ~/MobileIDE/assets/Plugins/LSP), --catalog-out.
   - Funktion 1: find_manifests() — durchsucht plugins_dir rekursiv nach 
     manifest.json UND theme.json; parst id, name, mainClass, version, 
     description, author, license, tags, minAppVersion, repository, 
     fileExtensions, dependencies.
   - Funktion 2: build_plugins_json() — schreibt docs/plugins.json als 
     Array aller Plugins (inkl. _source-Pfad).
   - Funktion 3: build_catalog() — erzeugt docs/plugins-catalog.json im 
     Format: {"catalogVersion":1,"lastUpdated":ISO8601,"plugins":[...]}, 
     je Eintrag: id, name, version, category ("language"|"theme"|"tool"), 
     description, author, minAppVersion, downloadUrl, sizeBytes, sha256, 
     entryScript, dependencies, fileExtensions.
   - downloadUrl: bevorzugt GitHub-Release-URL aus repository + 
     <id>-<version>.zip; fällt zurück auf asset://Plugins/LSP/<datei>.zip 
     (relativer Asset-Pfad) falls ZIP lokal existiert.
   - SHA-256 & sizeBytes: aus zips_dir, falls Datei <id>-<version>.zip 
     vorhanden.
   - Funktion 4: write_index_html() — docs/index.html mit einfachem 
     Store-Frontend (lädt plugins-catalog.json per fetch, gruppiert nach 
     Kategorie, zeigt Install-Hinweise).
   - Funktion 5: validate() — prüft gegen docs/schema/plugins-index.schema.json 
     (erzeugt, falls fehlt: Draft 2020-12, required id/name/version).
   - Idempotent: jeder Lauf überschreibt plugins.json, plugins-catalog.json, 
     index.html vollständig aus den Manifesten — KEINE manuellen 
     Katalogänderungen nach jedem Lauf.
   - Ausführbar: python3 tools/generate_plugin_store.py --validate.
2.2 SCHEMA-DATEI docs/schema/plugins-index.schema.json erzeugen:
   - Felder wie oben, zusätzlich source: "github"|"asset"|"bundled".
2.3 GIT-COMMIT im Store-/App-Repo:
   "feat(store): add plugin store generator (plugins.json, catalog, index)"

══════════════════════════════════════════════════════════════════
PHASE 3 — GITHUB PAGES DEPLOYMENT (automatisch)
══════════════════════════════════════════════════════════════════
3.1 GitHub-Actions-Workflow .github/workflows/store-pages.yml im Store-Repo:
   - Trigger: push auf main/master (paths: docs/**, plugins/**, 
     tools/**) + workflow_dispatch + schedule (täglich).
   - Steps:
     a) checkout + setup-python 3.11,
     b) chmod +x tools/generate_plugin_store.py,
     c) python3 tools/generate_plugin_store.py --validate (bricht bei 
        Validierungsfehler ab),
     d) actions/upload-pages-artifact@v3 mit path: docs/,
     e) actions/deploy-pages@v4 (permissions: pages: write, 
        id-token: write; environment: github-pages).
   - Settings-Hinweis in docs/README.md: Pages-Quelle = "GitHub Actions".
3.2 PUSH-KONVENTION je Plugin-Repo: Tags für Releases prüfen 
   (light-json-lsp/release.yml nutzt push tags v*) — im Bericht 
   dokumentieren, NICHT umbauen.
3.3 VALIDIERUNG: Nach Deployment https://<org>.github.io/<store>/ 
   aufrufbar; index.html lädt plugins-catalog.json; alle 13 LSP-Plugins 
   + 2 Theme-Pakete gelistet.
   Commit: "ci(store): deploy plugin store to GitHub Pages via Actions"

══════════════════════════════════════════════════════════════════
PHASE 4 — CI-WORKFLOWS DER PLUGIN-REPOS PRÜFEN & KORRIGIEREN
══════════════════════════════════════════════════════════════════
4.1 INVENTAR (aus Phase 0) abgleichen: 
   - plugin-build.yml (xed-fsharp, xed-kotlin)
   - plugin-build-test.yml (xed-go, xed-java-lsp, xed-json-lsp, xed-kmp-lsp, 
     xed-lua-lsp, xed-prettier, xed-python-lsp, xed-rust, xed-typst, xed-zig)
   - plugin-release.yml (xed-go-tools)
   - zip.yml (xed-feslake)
   - ci.yml, cd.yml, automation-autorelease.yml (prettier-standalone)
   - release.yml (light-json-lsp, Rust)
4.2 JE WORKFLOW PRÜFEN:
   a) Alte Package-Namen (io.kiquar.*, com.rk.*, com.androdev.*, com.koner.*, 
      com.rk.demo) in Pfaden/Artefaktnamen -> auf neue com.scto.mobile.ide.* 
      umstellen oder entfernen,
   b) Gradle-Task-Namen mit vorhandenen Skripten abgleichen: 
      compileRelease/compileDebug (Shell, aus Repo-Wurzel) vs. 
      ./gradlew assembleRelease + :app:createFinalZip — Workflow muss das 
      NUTZBARE Skript aufrufen (Existenz prüfen, chmod +x sicherstellen),
   c) setup-java auf JDK 21 (bei xed-json-lsp Toolchain-Variante 17 
      beachten — Matrix oder separater Job),
   d) Artifact-Upload: output/*.zip (createFinalZip) bzw. build/*.zip — 
      Pfad muss im Lauf existieren,
   e) Bei Plugin-Repos, deren assets/ nach Phase 1 bereinigt wurden: 
      prüfen, dass keine Workflow-Schritte das alte Binary referenzieren.
   f) xed-feslake/zip.yml: erzeugt feslake-theme.xed aus theme.json+icon.png 
      — Task-ZIP-Pfad und Artefaktname prüfen.
4.3 KORREKTUREN COMMITTEN je Plugin-Repo:
   "ci: align workflows with com.scto.mobile.ide refactoring and server 
   unbundling"
4.4 TESTLAUF: Falls GitHub-Zugriff: Push auf main bei xed-rust auslösen und 
   Workflow-Lauf auf grün beobachten. Sonst lokal simulieren 
   (chmod +x gradlew compileRelease; ./compileRelease).

══════════════════════════════════════════════════════════════════
PHASE 5 — THEME-INTEGRATION (xed-feslake + 11 Community-Themes)
══════════════════════════════════════════════════════════════════
5.1 App-Theme-Struktur ermitteln (ThemeManager/Themenliste in ~/MobileIDE 
   suchen; Laden aus assets/themes/ oder Verzeichnis-Scan?).
5.2 xed-feslake: theme.json + icon.png nach 
   ~/MobileIDE/assets/themes/feslake.theme.json (bzw. Zielstruktur der App) 
   übernehmen; gegen App-Schema validieren (Schema aus 
   theme/xed-themes/schema.json als Referenz).
5.3 11 Themes aus ~/Plugins/theme/xed-themes/themes/*.json übernehmen:
   catppuccin-frappe, darcula, dark-modern, github, itsaky, 
   monokai-ryo-anthracite, one-dark-pro, one-monokai, solar, 
   tokyo-night-pro, vs-2017-dark.
   - Jede Datei gegen App-Schema prüfen (jq), fehlerhafte ausschließen 
     (max. 2), Log in build/theme-validation.log.
5.4 Registrierung in der App-Themenliste (statisch: Eintrag ergänzen; 
   dynamisch: Scan reicht). Theme-Wechsel in App testen 
   (12 Themes geladen, kein Crash, Dark/Light korrekt).
   Commit: "feat(theme): integrate feslake and 11 community themes"

══════════════════════════════════════════════════════════════════
PHASE 6 — RELEASE-BUILD MIT APK-SIGNIERUNG
══════════════════════════════════════════════════════════════════
6.1 Signing: ~/MobileIDE/keystore/release.jks via keytool erzeugen 
   (alias mobileide, RSA 2048, validity 10000); keystore.properties 
   (storeFile, storePassword, keyAlias, keyPassword) anlegen, in 
   .gitignore aufnehmen; build.gradle.kts signingConfigs.release + 
   buildTypes.release.signingConfig setzen. KEINE Passwörter committen.
6.2 ./gradlew clean assembleRelease — Exit-Code 0.
6.3 apksigner verify --verbose --print-certs (v1/v2/v3 vorhanden).
6.4 Größe + sha256sum des APK nach build/release-artifact-info.tsv.
6.5 Inhalt: unzip -l <apk> | grep -c "assets/Plugins/LSP/.*\.zip" == 26, 
   catalog.json vorhanden, keine alten Asset-Reste.
6.6 Optional Gerätetest: adb install -r, Store zeigt 26 Plugins offline, 
   Rust-Plugin installieren/deinstallieren, check_lsp_status.sh OK.

══════════════════════════════════════════════════════════════════
PHASE 7 — DOKUMENTATION
══════════════════════════════════════════════════════════════════
- build/verification-report.md erweitern: Git-Status, CI-Status, 
  APK-Größe vorher/nachher, 26 Zips im APK, Runtime-Test, offene Punkte.
- docs/verification-report.md (Kopie ins Repo), docs/plugin-catalog.md 
  (26 Plugins: ID|Version|Kategorie|Endungen|SHA-256 gekürzt|Quelle), 
  docs/plugins-migration.md (Migration + Refactoring + Entbündelung), 
  docs/store-architecture.md (GitHub Pages Store: Struktur, Workflow, 
  generate_plugin_store.py-Nutzung), README.md-Link-Abschnitt ergänzen.
- Alle Änderungen committen: "docs: add store, catalog, verification and 
  migration documentation" und pushen.

══════════════════════════════════════════════════════════════════
PHASE 8 — GESAMTVERIFIKATION & ABSCHLUSSBERICHT
══════════════════════════════════════════════════════════════════
- git status/log je Repo (App + 13 Plugin-Repos) — sauber & gepusht.
- CI: alle angepassten Workflows grün (bzw. lokal simuliert).
- Build: ./gradlew assembleDebug + assembleRelease == BUILD SUCCESSFUL.
- APK: 26 Zips + catalog.json enthalten; Signatur OK.
- Runtime: Store offline (26), Installation/Deinstallation funktioniert, 
  LSP-Status 12/12 OK, Terminal root@localhost, Theme-Wechsel OK.
- Abschlussbericht build/final-report.md mit Tabellen je Phase 
  (Aktion | Status ✅/❌ | Nachweis), offene Punkte mit 
  Behebungsempfehlung, finales Baum-Diagramm von ~/MobileIDE/docs/ und 
  ~/MobileIDE/assets/themes/.
- Gesamtaussage: "Alle 13 Plugins + 12 Themes integriert, Server 
  entbündelt, Store auf GitHub Pages live, CI grün, APK signiert und 
  verifiziert" ODER exakte Liste offener Punkte.

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase 1: Fehlendes Backup -> keine Löschung. Build-Fehler nach 
  Entbündelung -> Restore aus Backup, Punkt dokumentieren, weiter.
- Phase 2/3: Validierungsfehler im Generator oder Pages-Deploy-Fehler -> 
  Fehler beheben und Wiederholen; maximal 2 Versuche, danach als offener 
  Punkt markieren und weiter.
- Phase 4: Korrigierte Workflows müssen lokal zumindest den Gradle-Task 
  ausführen; sonst offener Punkt, weiter zu Phase 5.
- Phase 6: assembleRelease-Fehler -> Stopp, Bericht mit Fehlerdetails.

GIB AM ENDE:
- Ausgefüllte Berichte aller 8 Phasen,
- build/final-report.md mit Gesamtübersicht 
  (Entbündelung ✅/❌ | Store ✅/❌ | Pages ✅/❌ | CI ✅/❌ | Themes ✅/❌ | 
  Release ✅/❌ | Doku ✅/❌),
- Verzeichnis-Bäume von ~/MobileIDE/docs/, ~/MobileIDE/assets/themes/ und 
  ~/MobileIDE/assets/Plugins/LSP/,
- die abschließende Gesamtaussage.
