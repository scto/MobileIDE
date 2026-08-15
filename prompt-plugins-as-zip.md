Erstelle sämtliche Plugins als installierbare ZIP-Archive und lege sie im Ordner 
~/MobileIDE/assets/Plugins/LSP ab, damit sie zu einem späteren Zeitpunkt über den 
Plugin-Store bzw. PluginManager nachinstalliert werden können.

KONTEXT / IST-ZUSTAND:
- ~/MobileIDE/assets/Plugins/ ist nach der letzten Bereinigung der zentrale 
  Plugin-Ordner und enthält bereits entpackte Plugin-Verzeichnisse 
  (Plugins/<plugin-id>/plugin.json + Ressourcen).
- Die App nutzt ein Plugin-System mit Manifest-Schema (id, name, version, category, 
  description, author, entryScript, fileExtensions, dependencies, icon) und einen 
  PluginStoreManager, der ZIP-Archive aus dem Plugin-Store herunterlädt, per 
  SHA-256-Checksumme verifiziert und nach local/extensions/<plugin-id>/<version>/ 
  entpackt.
- Als Referenzformat existieren bereits installierte Plugin-ZIPs mit Namens-
  Konvention <plugin-id>-<version>.zip (z. B. com.koner.rust-1.0.2.zip, 
  com.scto.mobile.ide.bash_lsp-1.0.0.zip).
- Es gibt 12 fest gebündelte LSP-Sprachen (bash, css, emmet, eslint, html, java, 
  json, kotlin, markdown, python, typescript, xml) sowie weitere LSP-/Tool-Plugins 
  (cpp, lua, rust, toml, yaml, go, zig, prettier, typst, json).

AUFGABEN:

1. BESTANDSAUFNAHME DER QUELL-PLUGINS
   - Liste alle vorhandenen Plugin-Verzeichnisse unter ~/MobileIDE/assets/Plugins/ 
     auf (find ~/MobileIDE/assets/Plugins -maxdepth 2 -name plugin.json).
   - Extrahiere aus jedem plugin.json die Felder: id, name, version, category, 
     description, author, entryScript, fileExtensions, dependencies.
   - Schreibe die komplette Liste in ~/MobileIDE/build/plugins-zip-manifest.tsv 
     (Tab-separiert: plugin-id | version | category | source-path).

2. ZIELORDNER VORBEREITEN
   - Erstelle den Zielordner ~/MobileIDE/assets/Plugins/LSP, falls nicht vorhanden 
     (mkdir -p).
   - ACHTUNG: Lege den Ordner NICHT innerhalb eines bestehenden Plugin-Verzeichnisses 
     an und stelle sicher, dass "LSP" selbst nicht als Plugin interpretiert wird 
     (kein plugin.json auf dieser Ebene). Falls die App ALLE Unterordner von 
     Plugins/ automatisch als Plugins lädt, stattdessen ein Ausnahme-/Ignore-
     Mechanismus prüfen (z. B. Ordner ohne plugin.json ignorieren) oder den 
     Zielordner als Plugins/LSP/ (ohne plugin.json) belassen und im PluginManager 
     sicherstellen, dass nur Verzeichnisse mit plugin.json als Plugin registriert 
     werden.

3. ZIEL-SET DER ZU ERSTELLENDEN PLUGIN-ZIPS
   Erstelle für JEDES der folgenden Plugins ein ZIP-Archiv. Die Liste umfasst die 
   12 gebündelten LSP-Sprachen PLUS die zusätzlichen LSP-/Tool-Plugins. 
   Pro Plugin gilt die Namenskonvention: <plugin-id>-<version>.zip

   LSP-SPRACH-PLUGINS:
   - com.scto.mobile.ide.bash_lsp-<version>.zip
   - com.scto.mobile.ide.css_lsp-<version>.zip
   - com.scto.mobile.ide.emmet_lsp-<version>.zip
   - com.scto.mobile.ide.eslint_lsp-<version>.zip
   - com.scto.mobile.ide.html_lsp-<version>.zip
   - com.scto.mobile.ide.java_lsp-<version>.zip
   - com.scto.mobile.ide.json_lsp-<version>.zip
   - com.scto.mobile.ide.kotlin_lsp-<version>.zip
   - com.scto.mobile.ide.markdown_lsp-<version>.zip
   - com.scto.mobile.ide.python_lsp-<version>.zip
   - com.scto.mobile.ide.typescript_lsp-<version>.zip
   - com.scto.mobile.ide.xml_lsp-<version>.zip

   ZUSÄTZLICHE LSP-PLUGINS:
   - com.scto.mobile.ide.cpp_lsp-<version>.zip
   - com.scto.mobile.ide.toml_lsp-<version>.zip
   - com.scto.mobile.ide.yaml_lsp-<version>.zip
   - com.scto.mobile.ide.plugin.lua-<version>.zip
   - com.scto.mobile.ide.plugin.python-<version>.zip
   - com.koner.rust-<version>.zip
   - com.koner.typst-<version>.zip
   - io.kiquar.plugin.go-<version>.zip
   - io.kiquar.plugin.zig-<version>.zip
   - com.koner.prettier-<version>.zip

   - Die jeweilige <version> AUS DEM plugin.json des Quell-Plugins lesen 
     (NICHT raten). Falls ein Quell-Plugin in ~/MobileIDE/assets/Plugins/ nicht 
     vorhanden ist, aber als ZIP im Root der Assets existiert hat (siehe 
     früheres Inventar), das ZIP als Quelle verwenden oder das Plugin neu 
     erstellen und in das ZIP aufnehmen.

4. ZIP-STRUKTUR DEFINIEREN (WICHTIG FÜR INSTALLATION)
   - Das ZIP muss so aufgebaut sein, dass der PluginManager es nach 
     local/extensions/<plugin-id>/<version>/ entpacken kann.
   - Empfohlene Struktur (A): Plugin-Inhalt DIREKT an der ZIP-Wurzel, d. h.
       plugin.json
       icons/...
       grammar/...
       snippets/...
     Der Installer entpackt das ZIP in den Zielordner und erwartet plugin.json 
     direkt unter dem Zielpfad.
   - Alternative Struktur (B): ZIP enthält einen Top-Level-Ordner mit der 
     Plugin-ID:
       <plugin-id>/plugin.json
       <plugin-id>/icons/...
     FALLS der PluginManager in der Vergangenheit Struktur (B) erwartet hat 
     (prüfen am bestehenden Referenz-ZIP z. B. com.koner.rust-1.0.2.zip per 
     unzip -l), MUSS dieselbe Struktur beibehalten werden.
   - FALLS unklar: Bestehendes Referenz-ZIP öffnen (unzip -l) und dessen 
     Struktur EXAKT nachahmen. Konsistenz ist wichtiger als Konvention.
   - Dateirechte: Skripte/Binaries im ZIP müssen nach der Extraktion ausführbar 
     sein (nach dem Entpacken chmod +x auf *.sh, *.bin, *.lsp in der 
     Installationsroutine sicherstellen; im ZIP selbst ggf. Unix-Berechtigungen 
     via zip --symlinks/permissions setzen, falls der Installer das auswertet).

5. MANIFEST & PRÜFSUMMEN MITLIEFERN
   - Erzeuge zusätzlich im Zielordner ~/MobileIDE/assets/Plugins/LSP:
     a) catalog.json — Katalog-Datei im Format des Plugin-Stores mit einem 
        Eintrag pro ZIP: id, name, version, category, downloadUrl (relativ, 
        z. B. "./<plugin-id>-<version>.zip" oder absolut je nach 
        Store-Mechanik), sha256, sizeBytes, fileExtensions, dependencies.
     b) Berechne für JEDES ZIP die SHA-256-Checksumme (sha256sum) und trage 
        sie in catalog.json ein.
   - Lege zusätzlich eine README.md in ~/MobileIDE/assets/Plugins/LSP an, die 
     erklärt: wie die Zips in den Plugin-Store-Katalog aufgenommen werden und 
     wie die Installation später (offline) erfolgen kann.

6. VALIDIERUNG
   - Für JEDES erzeugte ZIP:
     a) unzip -l <zip> ausführen und prüfen, dass plugin.json enthalten ist und 
        die Struktur der in Schritt 4 gewählten Konvention entspricht.
     b) unzip -t <zip> ausführen (Integritätstest) — MUSS fehlerfrei sein.
     c) sha256sum mit dem catalog.json-Eintrag abgleichen.
   - Gesamtliste: find ~/MobileIDE/assets/Plugins/LSP -maxdepth 1 -name "*.zip" 
     | wc -l — Anzahl MUSS der erwarteten Plugin-Anzahl entsprechen (12 LSP + 
     Zusatz-Plugins, je nach tatsächlichem Bestand).
   - Testinstallation: Nehme EIN BeispieL-ZIP und installiere es manuell über 
     den PluginManager (bzw. durch manuelles Entpacken nach 
     local/extensions/<plugin-id>/<version>/ mit anschließendem 
     check_lsp_status.sh) — bestätige, dass der LSP-Status für diese Sprache 
     "OK" meldet.
   - Bau nicht vergessen: Stelle sicher, dass ~/MobileIDE/assets/Plugins/LSP 
     in den Build einfließt (sourceSets prüfen) und das APK die Zips enthält.

7. ABSCHLUSS-DOKUMENTATION
   - Gib am Ende eine Tabelle aus:
       Plugin-ID | Version | ZIP-Datei | Größe | SHA-256 (gekürzt) | Status (OK/FEHLER)
   - Liste eventuelle Fehlerfälle (fehlendes plugin.json, defekte ZIP, 
     Struktur-Inkonsistenz) separat auf.

Gib am Ende:
- die vollständige Tabelle aller erzeugten ZIPs mit Prüfsummen,
- das finale Verzeichnis-Baum von ~/MobileIDE/assets/Plugins/LSP/,
- Bestätigung, dass alle Zips via unzip -t fehlerfrei sind und eine 
  Testinstallation erfolgreich war.
