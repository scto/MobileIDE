Führe in EINEM Durchlauf die folgenden drei Aufgaben in der vorgegebenen 
Reihenfolge aus (Phase A → Phase B → Phase C). KEINE Phase überspringen, 
KEINE Reihenfolge ändern. Nach jeder Phase den Abschlussbericht-Teil 
ausfüllen, bevor die nächste Phase startet.

PROJEKT: com.scto.mobile.ide (MobileIDE)
PLUGIN-ORDNER: ~/MobileIDE/assets/Plugins/LSP/

════════════════════════════════════════════════════════════════════
PHASE A — DIE 6 FEHLENDEN LSP-PLUGIN-ZIPS ERZEUGEN
════════════════════════════════════════════════════════════════════

KONTEXT:
- ~/MobileIDE/assets/Plugins/LSP/ enthält bereits 20 gültige Plugin-ZIPs 
  (alle unzip -t-geprüft) plus catalog.json (mit SHA-256) und README.md.
- Es existieren 12 fest gebündelte LSP-Sprachen: bash, css, emmet, eslint, 
  html, java, json, kotlin, markdown, python, typescript, xml.
- Als ZIP vorhanden (9 der 12): bash_lsp, java_lsp, json_lsp, kotlin_lsp, 
  python_lsp, xml_lsp — plus cpp_lsp, toml_lsp, yaml_lsp und weitere 
  Nicht-LSP-Plugins (rust, typst, lua, go, zig, prettier, fs, plugin.json, 
  java.lsp, kmplsp).
- FEHLEND (6 Stück, alle Node-basiert): css_lsp, emmet_lsp, eslint_lsp, 
  html_lsp, markdown_lsp, typescript_lsp.
- Die zugehörigen LSP-Start-Skripte existieren unter local/bin/lsp/ 
  (css.sh, emmet.sh, eslint.sh, html.sh, markdown.sh, typescript.sh).

AUFGABEN:

A1. QUELLEN FÜR DIE 6 PLUGINS SICHERN
   - Für jede der 6 Sprachen feststellen, woher der Language-Server stammt 
     (die .sh-Skripte öffnen und referenzierte Pakete/Binaries ermitteln):
     - css       -> vscode-css-language-server (vscode-langservers-extracted, npm)
     - emmet     -> emmet-ls (npm)
     - eslint    -> eslint + vscode-eslint-language-server bzw. eslint-lsp (npm)
     - html      -> vscode-html-language-server (vscode-langservers-extracted, npm)
     - markdown  -> marksman ODER vscode-markdown-language-server (Binary bzw. npm)
     - typescript-> typescript-language-server + typescript (npm)
   - Prüfen, ob die Server-Binaries/Pakete bereits irgendwo im Projekt 
     vorhanden sind (local/bin/lsp/, lsp/-Ordner, node_modules-Bundle, 
     assets) und wiederverwendet werden können. Falls ja, NICHT erneut 
     downloaden, sondern referenzieren.

A2. PLUGIN-ORDNER ERSTELLEN
   - Lege für jede Sprache ein Quell-Plugin-Verzeichnis an (z. B. unter 
     ~/MobileIDE/plugins-src/com.scto.mobile.ide.<lang>_lsp/) mit:
     - plugin.json (id, name, version, category="language", description, 
       author="com.scto.mobile.ide", entryScript="lsp/<lang>.sh", 
       fileExtensions, dependencies=["runtime-node"])
     - ggf. icons/, grammar/-Definitionen, README
   - Versionsnummern sinnvoll wählen (z. B. 1.0.0, konsistent mit den 
     bestehenden _lsp-Paketen) und VOR der Katalog-Aktualisierung 
     festlegen.

A3. ZIP-ARCHIVE ERZEUGEN
   - Für jede Sprache erzeugen: com.scto.mobile.ide.<lang>_lsp-<version>.zip
     mit IDENTISCHER ZIP-Struktur wie die vorhandenen Archive (plugin.json 
     auf Root-Ebene — VORHER per unzip -l an einem Referenz-ZIP wie 
     com.scto.mobile.ide.bash_lsp-1.0.0.zip verifizieren und EXAKT 
     nachahmen).
   - Ziel: ~/MobileIDE/assets/Plugins/LSP/
   - Integritätstest: unzip -t für jedes neue ZIP — MUSS fehlerfrei sein.

A4. catalog.json AKTUALISIEREN
   - Für jedes neue ZIP einen Eintrag ergänzen (id, name, version, category, 
     downloadUrl relativ, sha256, sizeBytes, fileExtensions, dependencies).
   - SHA-256 via sha256sum berechnen und eintragen.
   - Gesamtzahl im Katalog danach: 26 Einträge.

A5. README.md AKTUALISIEREN
   - Die 6 neuen Pakete in die Tabelle/Liste aufnehmen (Name, Version, 
     Dateiendungen, Server-Technologie).
   - Hinweis ergänzen, dass diese 6 Plugins node-basiert sind und eine 
     Node-Runtime in der Sandbox voraussetzen.

A6. VALIDIERUNG PHASE A
   - find ~/MobileIDE/assets/Plugins/LSP -maxdepth 1 -name "*.zip" | wc -l 
     == 26.
   - Jedes neue ZIP: unzip -l (plugin.json enthalten), unzip -t (fehlerfrei), 
     sha256sum == catalog.json.
   - Stichproben-Testinstallation: typescript_lsp via PluginManager 
     installieren, .ts-Datei öffnen, Autocomplete/Diagnostics prüfen.
   - Für die übrigen 5 Sprachen zumindest Status-Check via 
     check_lsp_status.sh.

[PHASE-A-BERICHT HIER EINFÜGEN: Tabelle der 6 neuen ZIPs 
(ID | Version | Größe | SHA-256 gekürzt | Status), Bestätigung 
Katalog = 26 Einträge, alle 26 Zips fehlerfrei.]

════════════════════════════════════════════════════════════════════
PHASE B — PLUGIN-STORE-INTEGRATION (catalog.json → Store-Screen)
════════════════════════════════════════════════════════════════════

KONTEXT:
- Nach Phase A enthält ~/MobileIDE/assets/Plugins/LSP/ 26 Plugin-ZIPs + 
  catalog.json + README.md.
- Es existiert bereits ein PluginStoreManager (Kotlin) mit fetchCatalog(), 
  downloadPlugin(), installPlugin(), uninstallPlugin(), updatePlugin(), 
  checkDependencies() sowie ein PluginStoreActivity-Screen (Tabs 
  "Entdecken" und "Installiert").
- Der PluginStoreManager lädt den Katalog bisher vermutlich NUR von einem 
  Remote-Endpoint (HTTPS). Die lokalen Asset-ZIPs werden aktuell nicht 
  genutzt.

AUFGABEN:

B1. LOKALEN ASSET-KATALOG IN DEN STORE EINSPEISEN
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
     lesen statt über HTTP. Konvention: URLs ohne "http(s)://" gelten als 
     Asset-Pfad relativ zu assets/Plugins/LSP/.

B2. ASSET-ZIPs INSTALLIERBAR MACHEN
   - Erweitere downloadPlugin() um den Asset-Fall:
     a) Asset-ZIP aus dem APK in einen App-internen Cache-Staging-Ordner 
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
     ("26 Plugins im Store verfügbar"), NICHT automatisch installieren.

B3. UI-ANPASSUNGEN (PluginStoreActivity)
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

B4. installed.json-KOMPATIBILITÄT PRÜFEN
   - Prüfe, ob der bestehende Install-Pfad 
     (local/extensions/<plugin-id>/<version>/) mit der ZIP-Struktur 
     kompatibel ist, die in den 26 Archiven verwendet wird (plugin.json 
     bzw. manifest.json auf Root-Ebene). Falls die Archive eine andere 
     Struktur haben (z. B. Top-Level-Ordner mit Plugin-ID), den Installer 
     so erweitern, dass er BEIDE Strukturen erkennt und normalisiert.

B5. VALIDIERUNG PHASE B
   - App frisch installieren (oder Update) und prüfen:
     a) PluginStoreActivity zeigt ALLE 26 Asset-Plugins an (kein Netz nötig),
     b) com.koner.rust-1.0.2.zip installieren: Fortschritt läuft, 
        installed.json enthält Eintrag, Dateien unter 
        local/extensions/com.koner.rust/1.0.2/ vorhanden, ausführbare 
        Dateien haben +x,
     c) Ein node-basiertes LSP (z. B. typescript_lsp) installieren und 
        prüfen, dass der LSP-Status OK ist,
     d) Plugin deinstallieren: vollständige Entfernung,
     e) SHA-256-Fehlertest: catalog.json-Eintrag manuell verfälschen -> 
        Abbruch mit klarer Meldung,
     f) Remote-Katalog (falls konfiguriert) erscheint zusätzlich hinter 
        den Asset-Plugins.

[PHASE-B-BERICHT HIER EINFÜGEN: Diff der geänderten Klassen 
(PluginStoreManager, PluginStoreActivity, ggf. PluginInstallWorker), 
Aussage welche Quelle in welchem Zustand aktiv ist, Bestätigung dass 
alle 26 Plugins offline installierbar sind.]

════════════════════════════════════════════════════════════════════
PHASE C — GESAMTVERIFIKATION (Git, Build, APK-Inhalt)
════════════════════════════════════════════════════════════════════

KONTEXT / ERWARTETER SOLL-ZUSTAND NACH PHASE A + B:
- ~/MobileIDE/assets/Plugins/LSP/ enthält 26 Plugin-ZIPs, catalog.json 
  (mit SHA-256 je Eintrag) und README.md.
- Die App bündelt die ZIPs als Asset und macht sie über den Plugin-Store 
  offline installierbar.
- Ein Commit/Push-Vorgang mit der Message "feat(plugins): generate 
  installable LSP plugin ZIP packages and store catalog in 
  assets/Plugins/LSP" wurde gestartet (task-158 bzw. Folge-Tasks).

AUFGABEN:

C1. GIT-ZUSTAND PRÜFEN
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
   - NICHT im Commit enthalten sein dürfen: Backup-Archive, temporäre 
     Staging-Dateien, node_modules, .gradle, build/.

C2. CI-/BUILD-PIPELINE PRÜFEN
   - Falls CI konfiguriert ist (GitHub Actions o. ä.): Status des letzten 
     Builds prüfen, Pipelines auf grün bestätigen.
   - Lokalen Release-Build ausführen: ./gradlew assembleRelease (bzw. 
     assembleDebug für Testzwecke) — MUSS mit Exit-Code 0 enden.
   - Build-Log auf WARNUNGEN/ERRORS scannen: Dateigrößen-Warnungen, 
     Duplicate-Resource-Fehler, "Dependency not found"-Meldungen.

C3. APK-INHALT VERIFIZIEREN
   - Nach dem Build das APK (app/build/outputs/apk/release/*.apk) prüfen:
     a) unzip -l <apk> | grep -i "assets/Plugins/LSP" -> Alle 26 Zips 
        müssen enthalten sein,
     b) assets/Plugins/LSP/catalog.json muss enthalten sein,
     c) Gesamtgröße des APK notieren und mit vorheriger Größe vergleichen 
        (Dokumentation der Größensteigerung durch die ZIP-Assets),
     d) Prüfen, dass KEINE alten Asset-Reste enthalten sind 
        (lsp/, Features/, Screens/, *.md, einzelne Root-ZIPs — falls diese 
        laut Bereinigungs-Konzept entfernt sein sollten).
   - ggf. Signatur-Verifizierung: apksigner verify.

C4. RUNTIME-VERIFIKATION (falls Gerät/Emulator verfügbar)
   - APK installieren, App starten.
   - Prüfen: Plugin-Store-Screen listet 26 Plugins (offline), 
     com.koner.rust installieren + deinstallieren funktioniert,
     check_lsp_status.sh meldet für alle 12 Sprachen OK,
     Terminal startet (root@localhost-Prompt).
   - Logcat auf Fehler scannen: PluginManager-Exceptions, AssetNotFound, 
     SHA-Mismatch-Warnungen.

C5. ABSCHLUSSBERICHT
   - Erstelle build/verification-report.md mit:
     a) Git-Status (Commit-Hash, Branch, Push-Status),
     b) CI-Status (falls vorhanden),
     c) APK-Größe vorher/nachher,
     d) Liste der 26 im APK enthaltenen ZIPs,
     e) Ergebnis der Runtime-Tests,
     f) Offene Punkte/Abweichungen vom SOLL-Zustand.

[PHASE-C-BERICHT HIER EINFÜGEN: Zusammenfassung 
GIT ✅/❌ | CI ✅/❌ | APK ✅/❌ | Runtime ✅/❌, bei Abweichungen 
konkrete Behebungsempfehlung.]

════════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase A bricht ab, wenn ein ZIP unzip -t nicht besteht oder die 
  Katalog-Anzahl nicht 26 erreicht. KEIN Wechsel zu Phase B.
- Phase B bricht ab, wenn die Offline-Installation eines Asset-Plugins 
  fehlschlägt. KEIN Wechsel zu Phase C.
- Phase C wird auch bei fehlgeschlagenem CI-Build durchgeführt, der 
  Abschlussbericht markiert den Fehler dann als offenen Punkt.

GIB AM ENDE:
- Den ausgefüllten Abschlussbericht aller drei Phasen,
- Die finalen Verzeichnis-Bäume von ~/MobileIDE/assets/Plugins/LSP/ und 
  build/verification-report.md,
- Eine abschließende Gesamtaussage: "Alle 26 Plugins fehlerfrei erzeugt, 
  offline installierbar und im Repository/APK verifiziert" ODER die 
  konkreten offenen Punkte.
