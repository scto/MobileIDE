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
