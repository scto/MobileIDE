# 📦 Prompt 1: Plugin-Store-Integration (catalog.json → Store-Screen)

```
Integriere den vorhandenen Plugin-Katalog ~/MobileIDE/assets/Plugins/LSP/catalog.json 
in den Plugin-Store von com.scto.mobile.ide, sodass die 20 erzeugten ZIP-Archive 
direkt aus der App heraus installierbar sind (offline, aus dem APK-Asset).

KONTEXT / IST-ZUSTAND:
- ~/MobileIDE/assets/Plugins/LSP/ enthält:
  - 20 Plugin-ZIPs (Namenskonvention <plugin-id>-<version>.zip), z. B. 
    com.koner.rust-1.0.2.zip, com.scto.mobile.ide.bash_lsp-1.0.0.zip, ...
  - catalog.json mit Einträgen pro ZIP: id, name, version, category, 
    downloadUrl (relativ), sha256, sizeBytes, fileExtensions, dependencies
  - README.md
- Es existiert bereits ein PluginStoreManager (Kotlin) mit fetchCatalog(), 
  downloadPlugin(), installPlugin(), uninstallPlugin(), updatePlugin(), 
  checkDependencies() sowie ein PluginStoreActivity-Screen (Tabs "Entdecken" 
  und "Installiert").
- Der PluginStoreManager lädt den Katalog bisher vermutlich NUR von einem 
  Remote-Endpoint (HTTPS). Die lokalen Asset-ZIPs werden aktuell nicht genutzt.

AUFGABEN:

1. LOKALEN ASSET-KATALOG IN DEN STORE EINSPEISEN
   - Erweitere PluginStoreManager um die Möglichkeit, den Katalog aus EINEM 
     der folgenden Quellen zu laden (in dieser Priorität):
     a) Lokales Asset: assets/Plugins/LSP/catalog.json (aus dem APK, via 
        AssetManager) — PRIORITÄT 1, da immer verfügbar,
     b) Remote-Katalog (bisherige HTTPS-Quelle) — PRIORITÄT 2,
     c) Gecachter Katalog aus früherem Abruf — PRIORITÄT 3.
   - Führe die Quellen zusammen (Merge by plugin-id): Lokale Plugins werden 
     immer angezeigt; Remote-Plugins ergänzen die Liste. Bei ID-Kollision 
     entscheidet die höhere Version.
   - Der downloadUrl-Wert im lokalen Katalog ist relativ (z. B. 
     "./com.koner.rust-1.0.2.zip"). Der Manager muss erkennen, dass diese 
     URLs auf ASSET-Pfade verweisen, und die ZIPs über den AssetManager 
     lesen statt über HTTP. Konvention festlegen: URLs ohne "http(s)://" 
     gelten als Asset-Pfad relativ zu assets/Plugins/LSP/.

2. ASSET-ZIPs INSTALLIERBAR MACHEN
   - Erweitere downloadPlugin() um den Asset-Fall:
     a) Asset-ZIP aus dem APK in einen App-interne Cache-Staging-Ordner 
        kopieren (z. B. filesDir/cache/plugins/<plugin-id>-<version>.zip),
     b) SHA-256 der kopierten Datei berechnen und mit catalog.json-Eintrag 
        abgleichen (bei Mismatch: Abbruch + Fehlermeldung, Cache-Eintrag 
        löschen),
     c) Danach den bestehenden Install-Flow verwenden (Entpacken nach 
        local/extensions/<plugin-id>/<version>/, Registrierung in 
        installed.json, ggf. chmod +x auf Skripte/Binaries).
   - Beim ersten App-Start NACH dem Update: Prüfen, ob der Asset-Katalog 
     zusätzliche Plugins enthält, die weder installiert noch als "Skip" 
     markiert sind — Nutzer per Benachrichtigung/Badge informieren 
     ("20 Plugins im Store verfügbar"), NICHT automatisch installieren.

3. UI-ANPASSUNGEN (PluginStoreActivity)
   - Tab "Entdecken":
     a) Lokale Asset-Plugins mit Quellen-Badge anzeigen ("Integriert" vs. 
        "Store"),
     b) Installieren-Button: Bei Asset-Plugins sofort ausführbar (kein 
        Download), mit Fortschrittsanzeige (Kopieren + Entpacken),
     c) Suchfeld/Filter funktionieren über die gemergte Liste.
   - Tab "Installiert":
     a) Installierte Asset-Plugins zeigen Version, Aktiv/Inaktiv-Schalter 
        und Deinstallieren-Button,
     b) Wenn eine lokal gebündelte Version NEUER ist als die installierte: 
        Update-Button anbieten (aus Asset, ohne Netz).
   - Detail-Dialog: Auch für Asset-Plugins vollständig befüllbar (Icon, 
     Beschreibung, Größe aus catalog.json; Icon ggf. aus dem Asset-ZIP 
     extrahieren und cachen).

4. OFFLINE-ERSTELLUNG VON installed.json KOMPATIBEL HALTEN
   - Prüfe, ob der bestehende Install-Pfad (local/extensions/<plugin-id>/<version>/) 
     mit der ZIP-Struktur kompatibel ist, die in den 20 Archiven verwendet wird 
     (plugin.json bzw. manifest.json auf Root-Ebene). Falls die Archive eine 
     andere Struktur haben (z. B. Top-Level-Ordner mit Plugin-ID), den Installer 
     so erweitern, dass er BEIDE Strukturen erkennt und normalisiert.

5. VALIDIERUNG
   - App frisch installieren (oder Update) und prüfen:
     a) PluginStoreActivity zeigt ALLE 20 Asset-Plugins an (kein Netz nötig),
     b) com.koner.rust-1.0.2.zip installieren: Fortschritt läuft, 
        installed.json enthält Eintrag, Dateien unter 
        local/extensions/com.koner.rust/1.0.2/ vorhanden, ausführbare 
        Dateien haben +x,
     c) Plugin deinstallieren: vollständige Entfernung,
     d) SHA-256-Fehlertest: catalog.json-Eintrag manuell verfälschen -> 
        Abbruch mit klarer Meldung,
     e) Remote-Katalog (falls konfiguriert) erscheint zusätzlich hinter den 
        Asset-Plugins.

Gib am Ende:
- Diff der geänderten Klassen (PluginStoreManager, PluginStoreActivity, 
  ggf. PluginInstallWorker),
- Aussage, welche Quelle (Asset/Remote/Cache) in welchem Zustand aktiv ist,
- Bestätigung, dass alle 20 Plugins offline installierbar sind.
```

---

# 🔧 Prompt 2: 6 fehlende LSP-ZIPs erzeugen (css, emmet, eslint, html, markdown, typescript)

```
Ergänze die 6 fehlenden LSP-Plugin-ZIPs im Ordner ~/MobileIDE/assets/Plugins/LSP/ 
und aktualisiere catalog.json sowie README.md entsprechend.

KONTEXT / IST-ZUSTAND:
- ~/MobileIDE/assets/Plugins/LSP/ enthält bereits 20 gültige Plugin-ZIPs 
  (alle unzip -t-geprüft) plus catalog.json mit SHA-256 und README.md.
- Es existieren 12 fest gebündelte LSP-Sprachen: bash, css, emmet, eslint, 
  html, java, json, kotlin, markdown, python, typescript, xml.
- Als ZIPs vorhanden (9 der 12): bash_lsp, java_lsp, json_lsp, kotlin_lsp, 
  python_lsp, xml_lsp — plus cpp_lsp, toml_lsp, yaml_lsp und weitere 
  Nicht-LSP-Plugins (rust, typst, lua, go, zig, prettier, fs, plugin.json, 
  java.lsp, kmplsp).
- FEHLEND (6 Stück, alle Node-basiert): css_lsp, emmet_lsp, eslint_lsp, 
  html_lsp, markdown_lsp, typescript_lsp.
- Die zugehörigen LSP-Start-Skripte existieren unter local/bin/lsp/ 
  (css.sh, emmet.sh, eslint.sh, html.sh, markdown.sh, typescript.sh).

AUFGABEN:

1. QUELLEN FÜR DIE 6 PLUGINS SICHERN
   - Für jede der 6 Sprachen feststellen, woher der Language-Server stammt 
     (pfad der .sh-Skripte öffnen und referenzierte Pakete/Binaries ermitteln):
     - css       -> vscode-css-language-server (aus vscode-langservers-extracted, npm)
     - emmet     -> emmet-ls (npm)
     - eslint    -> eslint + vscode-eslint-language-server bzw. eslint-lsp (npm)
     - html      -> vscode-html-language-server (vscode-langservers-extracted, npm)
     - markdown  -> marksman ODER vscode-markdown-language-server (Binary bzw. npm)
     - typescript-> typescript-language-server + typescript (npm)
   - Prüfen, ob die Server-Binaries/Pakete bereits irgendwo im Projekt 
     vorhanden sind (local/bin/lsp/, lsp/-Ordner, node_modules-Bundle, 
     assets) und wiederverwendet werden können. Falls ja, NICHT erneut 
     downloaden, sondern referenzieren.

2. PLUGIN-ORDNER ERSTELLEN (analog zu bestehenden Quell-Plugins)
   - Lege für jede Sprache ein Quell-Plugin-Verzeichnis an (z. B. unter 
     ~/MobileIDE/plugins-src/com.scto.mobile.ide.<lang>_lsp/) mit:
     - plugin.json (id, name, version, category="language", description, 
       author="com.scto.mobile.ide", entryScript="lsp/<lang>.sh", 
       fileExtensions, dependencies=[runtime-node])
     - ggf. icons/, grammar/-Definitionen, README
   - Versionsnummern sinnvoll wählen (z. B. 1.0.0, konsistent mit den 
     bestehenden _lsp-Paketen) und in der Versionierung VOR der 
     Katalog-Aktualisierung festlegen.

3. ZIP-ARCHIVE ERZEUGEN (gleiche Konvention wie bestehende 20)
   - Für jede Sprache erzeugen: com.scto.mobile.ide.<lang>_lsp-<version>.zip
     mit identischer ZIP-Struktur wie die vorhandenen Archive (plugin.json 
     auf Root-Ebene — VORHER per unzip -l an einem Referenz-ZIP wie 
     com.scto.mobile.ide.bash_lsp-1.0.0.zip verifizieren und EXAKT 
     nachahmen).
   - Ziel: ~/MobileIDE/assets/Plugins/LSP/
   - Integritätstest: unzip -t für jedes neue ZIP — MUSS fehlerfrei sein.

4. catalog.json AKTUALISIEREN
   - Für jedes neue ZIP einen Eintrag ergänzen (id, name, version, category, 
     downloadUrl relativ, sha256, sizeBytes, fileExtensions, dependencies).
   - SHA-256 via sha256sum berechnen und eintragen.
   - Gesamtzahl im Katalog danach: 26 Einträge.

5. README.md AKTUALISIEREN
   - Die 6 neuen Pakete in die Tabelle/Liste aufnehmen (Name, Version, 
     Dateiendungen, Server-Technologie).
   - Hinweis ergänzen, dass diese 6 Plugins node-basiert sind und eine 
     Node-Runtime in der Sandbox voraussetzen.

6. VALIDIERUNG
   - find ~/MobileIDE/assets/Plugins/LSP -maxdepth 1 -name "*.zip" | wc -l 
     == 26.
   - Jedes neue ZIP: unzip -l (plugin.json enthalten), unzip -t (fehlerfrei), 
     sha256sum == catalog.json.
   - Stichproben-Testinstallation: typescript_lsp via PluginManager 
     installieren, .ts-Datei öffnen, Autocomplete/Diagnostics prüfen.
   - Für die übrigen 5 Sprachen zumindest Status-Check via 
     check_lsp_status.sh.

Gib am Ende:
- Tabelle der 6 neuen ZIPs (ID | Version | Größe | SHA-256 gekürzt | Status),
- Bestätigung: Katalog = 26 Einträge, alle 26 Zips fehlerfrei.
```

---

# ✅ Prompt 3: Verifikation (Git, CI-Build, APK-Inhalt)

```
Verifiziere nach Abschluss der ZIP-Erzeugung (20+6 Pakete) und der 
Plugin-Store-Integration den Gesamtzustand des Projekts: Git-Push, 
Build-Pipeline und APK-Inhalt.

KONTEXT / ERWARTETER SOLL-ZUSTAND:
- ~/MobileIDE/assets/Plugins/LSP/ enthält 26 Plugin-ZIPs, catalog.json 
  (mit SHA-256 je Eintrag) und README.md.
- Ein Commit/Push-Vorgang mit der Message "feat(plugins): generate 
  installable LSP plugin ZIP packages and store catalog in 
  assets/Plugins/LSP" wurde gestartet (task-158 bzw. Folge-Tasks).
- Die App soll die ZIPs als Asset bündeln und über den Plugin-Store 
  offline installierbar machen.

AUFGABEN:

1. GIT-ZUSTAND PRÜFEN
   - git status, git log -1 --oneline, git branch --show-current ausführen.
   - Prüfen: Ist der Commit vorhanden? Wurde er erfolgreich gepusht 
     (git push prüfen bzw. git log origin/<branch>)?
   - Falls Push fehlgeschlagen (Netzwerk/Auth): Push erneut anstoßen und 
     Fehler loggen.
   - Liste der im Commit enthaltenen Dateien prüfen:
     a) assets/Plugins/LSP/*.zip — MUSS 26 Archive enthalten,
     b) assets/Plugins/LSP/catalog.json — vorhanden und aktuell,
     c) assets/Plugins/LSP/README.md — vorhanden,
     d) geänderte Quellcode-Dateien (PluginStoreManager, 
        PluginStoreActivity, shared_extraction.sh) — im Commit enthalten.
   - NICHT im Commit enthalten dürfen sein: Backup-Archive, temporäre 
     Staging-Dateien, node_modules, .gradle, build/.

2. CI-/BUILD-PIPELINE PRÜFEN
   - Falls ein CI-System konfiguriert ist (GitHub Actions o. ä.): Status des 
     letzten Builds prüfen (Web-API oder CLI), Pipelines auf grün bestätigen.
   - Lokalen Release-Build ausführen: ./gradlew assembleRelease (bzw. 
     assembleDebug für Testzwecke) — MUSS mit Exit-Code 0 enden.
   - Build-Log auf WARNUNGEN/ERRORS scannen: Dateigrößen-Warnungen, 
     Duplicate-Resource-Fehler, "Dependency not found"-Meldungen.

3. APK-INHALT VERIFIZIEREN
   - Nach dem Build das APK (app/build/outputs/apk/release/*.apk) prüfen:
     a) aapt list <apk> bzw. unzip -l <apk> | grep -i "assets/Plugins/LSP" 
        -> Alle 26 Zips müssen enthalten sein,
     b) assets/Plugins/LSP/catalog.json muss enthalten sein,
     c) Gesamtgröße des APK notieren und mit vorheriger Größe vergleichen 
        (Dokumentation der Größensteigerung durch die ZIP-Assets),
     d) Prüfen, dass KEINE alten Asset-Reste enthalten sind 
        (lsp/, Features/, Screens/, *.md, einzelne Root-ZIPs — falls diese 
        laut Bereinigungs-Konzept entfernt sein sollten).
   - ggf. Signature-Verifizierung: apksigner verify.

4. RUNTIME-VERIFIKATION (falls Gerät/Emulator verfügbar)
   - APK installieren, App starten.
   - Prüfen: Plugin-Store-Screen listet 26 Plugins (offline), 
     com.koner.rust installieren + deinstallieren funktioniert,
     check_lsp_status.sh meldet für alle 12 Sprachen OK,
     Terminal startet (root@localhost-Prompt).
   - Logcat auf Fehler scannen: PluginManager-Exceptions, AssetNotFound, 
     SHA-Mismatch-Warnungen.

5. ABSCHLUSSBERICHT
   - Erstelle build/verification-report.md mit:
     a) Git-Status (Commit-Hash, Branch, Push-Status),
     b) CI-Status (falls vorhanden),
     c) APK-Größe vorher/nachher,
     d) Liste der 26 im APK enthaltenen ZIPs,
     e) Ergebnis der Runtime-Tests,
     f) Offene Punkte/Abweichungen vom SOLL-Zustand.

Gib am Ende:
- Zusammenfassung: GIT ✅/❌ | CI ✅/❌ | APK ✅/❌ | Runtime ✅/❌,
- Falls Abweichungen: konkrete Empfehlung zur Behebung.
```

---

**Hinweis zur Reihenfolge:** Sinnvoll ist **2 → 1 → 3**: Zuerst die 6 fehlenden ZIPs erzeugen (damit der Katalog vollständig ist), dann die Store-Integration, und zuletzt die Gesamtverifikation nach dem finalen Push. Falls du die Store-Integration aber zuerst testen willst (mit den vorhandenen 20), ist auch 1 → 2 → 3 machbar – die Integration ist gegenüber zusätzlichen ZIPs tolerant, solange `catalog.json` per Merge eingelesen wird.ERN
   - Für jede der 6 Sprachen feststellen, woher der Language-Server stammt 
     (pfad der .sh-Skripte öffnen und referenzierte Pakete/Binaries ermitteln):
     - css       -> vscode-css-language-server (aus vscode-langservers-extracted, npm)
     - emmet     -> emmet-ls (npm)
     - eslint    -> eslint + vscode-eslint-language-server bzw. eslint-lsp (npm)
     - html      -> vscode-html-language-server (vscode-langservers-extracted, npm)
     - markdown  -> marksman ODER vscode-markdown-language-server (Binary bzw. npm)
     - typescript-> typescript-language-server + typescript (npm)
   - Prüfen, ob die Server-Binaries/Pakete bereits irgendwo im Projekt 
     vorhanden sind (local/bin/lsp/, lsp/-Ordner, node_modules-Bundle, 
     assets) und wiederverwendet werden können. Falls ja, NICHT erneut 
     downloaden, sondern referenzieren.

2. PLUGIN-ORDNER ERSTELLEN (analog zu bestehenden Quell-Plugins)
   - Lege für jede Sprache ein Quell-Plugin-Verzeichnis an (z. B. unter 
     ~/MobileIDE/plugins-src/com.scto.mobile.ide.<lang>_lsp/) mit:
     - plugin.json (id, name, version, category="language", description, 
       author="com.scto.mobile.ide", entryScript="lsp/<lang>.sh", 
       fileExtensions, dependencies=[runtime-node])
     - ggf. icons/, grammar/-Definitionen, README
   - Versionsnummern sinnvoll wählen (z. B. 1.0.0, konsistent mit den 
     bestehenden _lsp-Paketen) und in der Versionierung VOR der 
     Katalog-Aktualisierung festlegen.

3. ZIP-ARCHIVE ERZEUGEN (gleiche Konvention wie bestehende 20)
   - Für jede Sprache erzeugen: com.scto.mobile.ide.<lang>_lsp-<version>.zip
     mit identischer ZIP-Struktur wie die vorhandenen Archive (plugin.json 
     auf Root-Ebene — VORHER per unzip -l an einem Referenz-ZIP wie 
     com.scto.mobile.ide.bash_lsp-1.0.0.zip verifizieren und EXAKT 
     nachahmen).
   - Ziel: ~/MobileIDE/assets/Plugins/LSP/
   - Integritätstest: unzip -t für jedes neue ZIP — MUSS fehlerfrei sein.

4. catalog.json AKTUALISIEREN
   - Für jedes neue ZIP einen Eintrag ergänzen (id, name, version, category, 
     downloadUrl relativ, sha256, sizeBytes, fileExtensions, dependencies).
   - SHA-256 via sha256sum berechnen und eintragen.
   - Gesamtzahl im Katalog danach: 26 Einträge.

5. README.md AKTUALISIEREN
   - Die 6 neuen Pakete in die Tabelle/Liste aufnehmen (Name, Version, 
     Dateiendungen, Server-Technologie).
   - Hinweis ergänzen, dass diese 6 Plugins node-basiert sind und eine 
     Node-Runtime in der Sandbox voraussetzen.

6. VALIDIERUNG
   - find ~/MobileIDE/assets/Plugins/LSP -maxdepth 1 -name "*.zip" | wc -l 
     == 26.
   - Jedes neue ZIP: unzip -l (plugin.json enthalten), unzip -t (fehlerfrei), 
     sha256sum == catalog.json.
   - Stichproben-Testinstallation: typescript_lsp via PluginManager 
     installieren, .ts-Datei öffnen, Autocomplete/Diagnostics prüfen.
   - Für die übrigen 5 Sprachen zumindest Status-Check via 
     check_lsp_status.sh.

Gib am Ende:
- Tabelle der 6 neuen ZIPs (ID | Version | Größe | SHA-256 gekürzt | Status),
- Bestätigung: Katalog = 26 Einträge, alle 26 Zips fehlerfrei.
```

---

# ✅ Prompt 3: Verifikation (Git, CI-Build, APK-Inhalt)

```
Verifiziere nach Abschluss der ZIP-Erzeugung (20+6 Pakete) und der 
Plugin-Store-Integration den Gesamtzustand des Projekts: Git-Push, 
Build-Pipeline und APK-Inhalt.

KONTEXT / ERWARTETER SOLL-ZUSTAND:
- ~/MobileIDE/assets/Plugins/LSP/ enthält 26 Plugin-ZIPs, catalog.json 
  (mit SHA-256 je Eintrag) und README.md.
- Ein Commit/Push-Vorgang mit der Message "feat(plugins): generate 
  installable LSP plugin ZIP packages and store catalog in 
  assets/Plugins/LSP" wurde gestartet (task-158 bzw. Folge-Tasks).
- Die App soll die ZIPs als Asset bündeln und über den Plugin-Store 
  offline installierbar machen.

AUFGABEN:

1. GIT-ZUSTAND PRÜFEN
   - git status, git log -1 --oneline, git branch --show-current ausführen.
   - Prüfen: Ist der Commit vorhanden? Wurde er erfolgreich gepusht 
     (git push prüfen bzw. git log origin/<branch>)?
   - Falls Push fehlgeschlagen (Netzwerk/Auth): Push erneut anstoßen und 
     Fehler loggen.
   - Liste der im Commit enthaltenen Dateien prüfen:
     a) assets/Plugins/LSP/*.zip — MUSS 26 Archive enthalten,
     b) assets/Plugins/LSP/catalog.json — vorhanden und aktuell,
     c) assets/Plugins/LSP/README.md — vorhanden,
     d) geänderte Quellcode-Dateien (PluginStoreManager, 
        PluginStoreActivity, shared_extraction.sh) — im Commit enthalten.
   - NICHT im Commit enthalten dürfen sein: Backup-Archive, temporäre 
     Staging-Dateien, node_modules, .gradle, build/.

2. CI-/BUILD-PIPELINE PRÜFEN
   - Falls ein CI-System konfiguriert ist (GitHub Actions o. ä.): Status des 
     letzten Builds prüfen (Web-API oder CLI), Pipelines auf grün bestätigen.
   - Lokalen Release-Build ausführen: ./gradlew assembleRelease (bzw. 
     assembleDebug für Testzwecke) — MUSS mit Exit-Code 0 enden.
   - Build-Log auf WARNUNGEN/ERRORS scannen: Dateigrößen-Warnungen, 
     Duplicate-Resource-Fehler, "Dependency not found"-Meldungen.

3. APK-INHALT VERIFIZIEREN
   - Nach dem Build das APK (app/build/outputs/apk/release/*.apk) prüfen:
     a) aapt list <apk> bzw. unzip -l <apk> | grep -i "assets/Plugins/LSP" 
        -> Alle 26 Zips müssen enthalten sein,
     b) assets/Plugins/LSP/catalog.json muss enthalten sein,
     c) Gesamtgröße des APK notieren und mit vorheriger Größe vergleichen 
        (Dokumentation der Größensteigerung durch die ZIP-Assets),
     d) Prüfen, dass KEINE alten Asset-Reste enthalten sind 
        (lsp/, Features/, Screens/, *.md, einzelne Root-ZIPs — falls diese 
        laut Bereinigungs-Konzept entfernt sein sollten).
   - ggf. Signature-Verifizierung: apksigner verify.

4. RUNTIME-VERIFIKATION (falls Gerät/Emulator verfügbar)
   - APK installieren, App starten.
   - Prüfen: Plugin-Store-Screen listet 26 Plugins (offline), 
     com.koner.rust installieren + deinstallieren funktioniert,
     check_lsp_status.sh meldet für alle 12 Sprachen OK,
     Terminal startet (root@localhost-Prompt).
   - Logcat auf Fehler scannen: PluginManager-Exceptions, AssetNotFound, 
     SHA-Mismatch-Warnungen.

5. ABSCHLUSSBERICHT
   - Erstelle build/verification-report.md mit:
     a) Git-Status (Commit-Hash, Branch, Push-Status),
     b) CI-Status (falls vorhanden),
     c) APK-Größe vorher/nachher,
     d) Liste der 26 im APK enthaltenen ZIPs,
     e) Ergebnis der Runtime-Tests,
     f) Offene Punkte/Abweichungen vom SOLL-Zustand.

Gib am Ende:
- Zusammenfassung: GIT ✅/❌ | CI ✅/❌ | APK ✅/❌ | Runtime ✅/❌,
- Falls Abweichungen: konkrete Empfehlung zur Beh
