Prüfe in den jeweiligen Asset-Ordnern aller Plugin-Quellprojekte unter 
~/Plugins (und ggf. der entpackten Plugin-Ordner unter 
~/MobileIDE/assets/Plugins/), ob ein Bash-Skript zur Installation des 
zugehörigen LSP-Servers (Language Server) vorhanden ist. Ist ein solches 
Installations-Skript vorhanden, LÖSCHE den entsprechenden Sprachserver 
(das vom Skript installierte Binary/Paket/Bundle) aus den Plugin-Assets 
und aktualisiere Manifest, ZIP-Archive und catalog.json konsistent. 

Der Zweck: Sprachserver werden durch die Install-Skripte zur Laufzeit 
bereitgestellt → doppelte/überflüssige Server-Bundles in den Plugin-ZIPs 
werden entfernt, um Größe, Build-Zeit und Install-Konflikte zu reduzieren.

PROJEKT-KONTEXT:
- Plugin-Quell-Repos: ~/Plugins
    ├── lsp/
    │   ├── scto/                  (fertige Referenz-ZIPs com.scto.mobile.ide.*.zip — NICHT anfassen)
    │   ├── xed-fsharp/            (Package com.scto.mobile.ide.plugin.fs)
    │   ├── xed-go/                (Package com.scto.mobile.ide.plugin.go)
    │   ├── xed-go-tools/          (Package com.scto.mobile.ide.plugin.go.tools)
    │   ├── xed-java-lsp/          (Package com.scto.mobile.ide.plugins.java.lsp)
    │   ├── xed-json-lsp/          (Package com.scto.mobile.ide.json_lsp)
    │   ├── xed-kmp-lsp/           (Package com.scto.mobile.ide.plugins.kotlin.kmplsp)
    │   ├── xed-kotlin/            (Package com.scto.mobile.ide.kotlin_lsp)
    │   ├── xed-lua-lsp/           (Package com.scto.mobile.ide.plugin.lua)
    │   ├── xed-prettier/          (Package com.scto.mobile.ide.plugin.prettier)
    │   ├── xed-prettier-standalone/ (nur Node/TS, kein Android-Modul)
    │   ├── xed-python-lsp/        (Package com.scto.mobile.ide.python_lsp)
    │   ├── xed-rust/              (Package com.scto.mobile.ide.plugin.rust)
    │   ├── xed-typst/             (Package com.scto.mobile.ide.plugin.typst)
    │   └── xed-zig/               (Package com.scto.mobile.ide.plugin.zig)
    └── theme/                     (NUR Themes, kein LSP — überspringen)
- App-Repo: ~/MobileIDE
    - assets/Plugins/LSP/          (26 ZIPs + catalog.json + README.md)
    - assets/Plugins/              (ggf. entpackte Plugin-Ordner)
    - local/bin/lsp/               (LSP-Start-Skripte der 12 Sprachen)
- WICHTIG: LSP-Start-Skripte unter local/bin/lsp/ (css.sh, emmet.sh, 
  eslint.sh, html.sh, markdown.sh, typescript.sh, bash.sh, java.sh, 
  json.sh, kotlin.sh, python.sh, xml.sh ...) sind NICHT Gegenstand der 
  Löschung — sie sind Teil der App-Sandbox und bleiben unangetastet.

══════════════════════════════════════════════════════════════════
PHASE 1 — BESTANDSAUFNAHME & INVENTAR
══════════════════════════════════════════════════════════════════

1.1 ASSET-ORDNER JE PLUGIN IDENTIFIZIEREN
   - Für JEDES Plugin unter ~/Plugins/lsp/* (ausgenommen scto/) die 
     Asset-Verzeichnisse ermitteln. Typische Kandidaten:
     a) <plugin>/app/src/main/assets/
     b) <plugin>/app/src/main/assets/plugins/
     c) <plugin>/assets/
     d) <plugin>/lsp/  bzw. <plugin>/server/   (Server-Binary)
     e) <plugin>/lib/  bzw. <plugin>/bin/      (Binaries/Skripte)
     f) <plugin>/node_modules/  (falls gebündelt)
   - Auch in ~/MobileIDE/assets/Plugins/<plugin-id>/ (falls entpackte 
     Plugin-Ordner existieren) denselben Scan durchführen.
   - Für JEDES Plugin in build/lsp-script-inventory.tsv erfassen:
     plugin | package_name | asset_ordner | install_skript | 
     server_binary | server_groesse_bytes | aktion (offen)

1.2 BASH-INSTALL-SKRIPTE SUCHEN
   - find je Asset-Ordner mit:
     find <asset_ordner> -type f $ -name "*.sh" -o -name "*.bash" $ -print
   - Zusätzlich nach typischen Install-Skript-Namen grep-en (auch ohne 
     .sh-Endung):
     install*.sh, setup*.sh, *install*.sh, *setup*.sh, bootstrap*, 
     build-server*, download-*, fetch-*, get-*, run-server*, start-*, 
     npm-install*, prepare-*
   - Ergebnis pro Plugin in build/lsp-script-inventory.tsv eintragen.

══════════════════════════════════════════════════════════════════
PHASE 2 — SKRIPT-INHALT ANALYSIEREN & SERVER-ZUORDNUNG
══════════════════════════════════════════════════════════════════

2.1 JEDES GEFUNDENE SKRIPT ÖFFNEN (cat/less) UND ANALYSIEREN
   - Feststellen, WAS das Skript installiert:
     a) npm-Pakete (npm install <pkg>, yarn add, pnpm add) → Name des 
        LSP-Pakets notieren (z. B. typescript-language-server, 
        vscode-css-language-server, emmet-ls, marksman, rust-analyzer),
     b) Downloads von URLs (curl/wget + URL) → Dateiname/Archiv notieren,
     c) Binaries aus Repo-Assets (z. B. ./bin/<server>, java -jar 
        <server>.jar, go build, cargo build) → Binary-Pfad notieren,
     d) Entpacken von Archiven (unzip/tar auf <server>.tar.gz/.zip) → 
        Zielpfad notieren,
     e) Symlinks/Chmod auf lokale Binaries,
     f) Umgebungsvariablen/Pfad-Exporte (PATH=...).
   - Ergebnis je Skript in build/lsp-script-inventory.tsv:
     install_skript | typ (npm|download|build|unpack|symlink) | 
     server_komponente | install_ziel

2.2 ZUGEHÖRIGEN SPRACHSERVER IM PLUGIN LOKALISIEREN
   - Ausgehend von der Server-Komponente aus 2.1 im GLEICHEN Plugin 
     die gebündelte Instanz suchen:
     a) node_modules/<server-paket>/  (npm-Komponenten des Servers),
     b) <server>.jar, <server>.class, libs/<server>-*.jar,
     c) bin/<server>, server/<server>, lsp/<server>  (Binary-Dateien),
     d) .tar.gz/.zip-Archive des Servers im assets/-Ordner,
     e) build-Artefakte (target/, build/, dist/) des Servers,
     f) Bei rustbasierten Servern (z. B. light-json-lsp): 
        target/release/<binary> bzw. installierte Binary.
   - WICHTIG: NUR den Sprachserver entfernen, der VOM SKRIPT INSTALLIERT 
     WIRD. Andere Bundles ohne Install-Skript NICHT anfassen.
   - Duplikat-Check: Wenn der Sprachserver zusätzlich in 
     ~/MobileIDE/local/bin/lsp/ bzw. ~/MobileIDE/assets/lsp/ existiert 
     (App-seitig), ist die Entfernung aus dem Plugin die korrekte 
     Duplikat-Bereinigung. Das im Log dokumentieren.

══════════════════════════════════════════════════════════════════
PHASE 3 — LÖSCHUNG DES SPRACHSERVERS
══════════════════════════════════════════════════════════════════

3.1 BACKUP
   - Vor JEDER Löschung: Backup der betroffenen Plugin-Verzeichnisse in 
     ~/MobileIDE/build/lsp-server-backup-<timestamp>/ ablegen 
     (komplette Ordnerkopie, NICHT nur den Server).

3.2 LÖSCH-KATALOG (build/lsp-deletion-manifest.tsv)
   - Pro Löschung dokumentieren:
     plugin | install_skript | server_pfad | server_komponente | 
     groesse_vorher_bytes | backup_pfad | datum
   - Lösch-Regeln:
     a) Entfernt werden: node_modules-Einträge des Servers, 
        Server-Binaries, Server-JARs, heruntergeladene Server-Archive, 
        Build-Artefakte des Servers.
     b) NICHT entfernt werden: 
        - Das Install-Skript selbst (bleibt im Plugin — Installation 
          erfolgt dadurch zur Laufzeit),
        - SDK-/App-API-Klassen (com.rk.extension.*, androidx.*),
        - Plugin-eigene .kt/.java-UI- und Main-Klassen,
        - Grammatik-/Syntax-Dateien (sofern nicht Teil des Servers),
        - LSP-Start-Skripte der App unter local/bin/lsp/.
     c) Bei UNKLARHEIT, ob eine Datei zum Server gehört: NICHT löschen, 
        sondern in build/lsp-deletion-manifest.tsv als "unsicher" 
        markieren und im Abschlussbericht als offenen Punkt nennen.
   - Ausführen: rm -rf / rm <server_pfad> (nach Dokumentation und Backup).
   - Ergebnis je Löschung in das Deletion-Manifest eintragen (status: 
     geloescht | uebersprungen-unsicher | nicht-geprueft).

3.3 MANIFEST & BUILD-KONFIG PRÜFEN
   - plugin.json / manifest.json des betroffenen Plugins öffnen:
     a) Referenziert es den gelöschten Server direkt (Pfad, Binary-Name)? 
        Dann Eintrag entfernen bzw. auf das Install-Skript verweisen,
     b) Dateigrößen-/Include-Listen (falls vorhanden), die auf 
        Server-Dateien zeigen, bereinigen,
     c) build.gradle.kts: Falls assets/ nur den Server enthielt, 
        sourceSets-Angaben unverändert lassen (asset-Ordner darf auch 
        leer/ohne Server bleiben).

══════════════════════════════════════════════════════════════════
PHASE 4 — ZIPS & KATALOG AKTUALISIEREN (falls Plugin als ZIP gepackt wird)
══════════════════════════════════════════════════════════════════

4.1 BETROFFENE ZIPS NEU ERZEUGEN
   - Für jedes Plugin mit Löschung und vorhandenem ZIP unter 
     ~/MobileIDE/assets/Plugins/LSP/:
     a) ZIP-Neuaufbau aus dem bereinigten Quellcode:
        ./gradlew createFinalZip (bzw. buildExtensionRelease / 
        buildExtensionDebug, je nach Plugin-Konvention),
     b) Sicherstellen, dass das neue ZIP KEINEN Server-Bundle mehr 
        enthält: unzip -l <zip> | grep -i <server-name> == 0 Treffer,
     c) Altes ZIP ersetzen (Name <neue-id>-<version>.zip bleibt gleich), 
        Backup des alten ZIP in ~/MobileIDE/build/.

4.2 catalog.json AKTUALISIEREN
   - sha256, sizeBytes der geänderten ZIPs neu berechnen und eintragen.
   - KEINE Einträge entfernen, nur die Metadaten der betroffenen Plugins 
     aktualisieren (id/version bleiben gleich).
   - README.md unter assets/Plugins/LSP/ bei Bedarf ergänzen 
     (Hinweis, dass LSP-Server zur Laufzeit per Install-Skript 
     bereitgestellt werden).

══════════════════════════════════════════════════════════════════
PHASE 5 — VALIDIERUNG
══════════════════════════════════════════════════════════════════

5.1 STATISCHE PRÜFUNGEN
   - a) find in jedem bereinigten Plugin: keiner der gelöschten 
        Server-Pfade existiert noch (0 Treffer),
   - b) grep -r "<server-name>" in den verbleibenden Assets: 
        0 Treffer auf Binary-Pfade (README/CHANGELOG-Ausnahmen notieren),
   - c) unzip -t auf alle neu erzeugten Zips: fehlerfrei,
   - d) sha256sum == catalog.json für alle geänderten Zips,
   - e) catalog.json: Anzahl Einträge == 26 (unverändert).

5.2 BUILD-TEST
   - Für JEDES bereinigte Plugin: ./gradlew assembleDebug (bzw. 
     buildExtensionDebug) → Exit-Code 0. Fehlschläge in 
     build/lsp-deletion-report.md als offene Punkte festhalten.

5.3 RUNTIME-TEST (falls Gerät/Emulator verfügbar)
   - App installieren, für jedes betroffene Plugin prüfen:
     a) Plugin installieren (aus ZIP aus assets/Plugins/LSP/),
     b) Install-Skript wird korrekt ausgeführt (Server wird zur Laufzeit 
        installiert),
     c) check_lsp_status.sh meldet für die Sprache OK,
     d) .<ext>-Datei öffnen → Autocomplete/Diagnostics funktionieren.

5.4 VERIFIKATIONSBERICHT
   - build/lsp-deletion-report.md erzeugen:
     a) Tabelle je Plugin: install_skript | server_geloescht | 
        zip_neu | build_ok | runtime_ok,
     b) Gesamtbilanz: entfernte Server-Bytes pro Plugin und gesamt,
     c) offene Punkte (unsichere Dateien, nicht getestete Plugins).

══════════════════════════════════════════════════════════════════
PHASE 6 — COMMIT & DOKUMENTATION
══════════════════════════════════════════════════════════════════

6.1 GIT-COMMITS
   - In JEDEM betroffenen Plugin-Repo (~/Plugins/lsp/<plugin>):
     "chore: remove bundled <server> LSP server, install via script at 
     runtime"
   - Im App-Repo (~/MobileIDE):
     "chore(plugins): update ZIPs and catalog after LSP server unbundling"
   - Push auf origin/main (bzw. jeweiligen Branch).

6.2 DOKUMENTE
   - build/lsp-script-inventory.tsv (Inventar aus Phase 1/2),
   - build/lsp-deletion-manifest.tsv (Phase 3),
   - build/lsp-deletion-report.md (Phase 5),
   - Referenz-Dokument docs/lsp-server-unbundling.md im App-Repo anlegen:
     Welche Plugins haben Install-Skripte, welche Server wurden entfernt, 
     wie läuft die Laufzeit-Installation ab.

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase 1/2: Findet sich in einem Plugin KEIN Install-Skript → Plugin 
  wird in der Inventar-Tabelle als "kein Skript" markiert und 
  ÜBERSPRUNGEN (kein Server wird gelöscht).
- Phase 3: Fehlt das Backup → Löschung unterlassen.
- Phase 5: Schlägt ein Build fehl, weil der Server noch referenziert 
  wird → Referenz beheben oder Löschung rückgängig machen (Restore aus 
  Backup), KEINE stillschweigende Löschung behalten.

GIB AM ENDE:
- build/lsp-script-inventory.tsv als Tabelle (Plugin | Skript | 
  Server-Komponente | Aktion),
- build/lsp-deletion-manifest.tsv als Tabelle (Plugin | gelöschter 
  Pfad | Größe | Status),
- Gesamtbilanz der entfernten Server-Bytes,
- Liste der Plugin-ZIPs, die neu erzeugt wurden (mit neuem SHA-256 
  gekürzt),
- Bestätigung: "Alle Plugins mit Install-Skript bereinigt, ZIPs/Katalog 
  konsistent, Builds grün" ODER die konkreten offenen Punkte.
