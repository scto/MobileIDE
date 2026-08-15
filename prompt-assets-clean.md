Reorganisiere den Root-Assets-Ordner von com.scto.mobile.ide:

  ~/MobileIDE/assets

IST-ZUSTAND (verifiziert per ls):
- Ordner: Features/, Plugins/, Screens/, bundled_plugins/, lsp/
- Dateien: MobileIDE_20260727_220829.md, "Typst Support.zip"
- Plugin-ZIP-Pakete im Root (21 Stück):
  com.koner.prettier-1.0.0.zip, com.koner.rust-1.0.2.zip, com.koner.typst-1.2.3.zip,
  com.scto.mobile.ide.bash_lsp-1.0.0.zip, com.scto.mobile.ide.cpp_lsp-1.0.0.zip,
  com.scto.mobile.ide.java_lsp-1.0.0.zip, com.scto.mobile.ide.json_lsp-1.0.0.zip,
  com.scto.mobile.ide.kotlin_lsp-1.0.0.zip, com.scto.mobile.ide.plugin.json-1.0.2.zip,
  com.scto.mobile.ide.plugin.lua-1.0.2.zip, com.scto.mobile.ide.plugin.python-1.0.3.zip,
  com.scto.mobile.ide.plugins.java.lsp-1.1.0.zip,
  com.scto.mobile.ide.plugins.kotlin.kmplsp-0.1.2.zip,
  com.scto.mobile.ide.python_lsp-1.0.0.zip, com.scto.mobile.ide.toml_lsp-1.0.0.zip,
  com.scto.mobile.ide.xml_lsp-1.0.0.zip, com.scto.mobile.ide.yaml_lsp-1.0.0.zip,
  io.kiquar.plugin.fs-0.1.0.zip, io.kiquar.plugin.go-0.2.0.zip,
  io.kiquar.plugin.zig-0.4.0.zip
- Weitere ZIP-Datei: "Typst Support.zip" (Name mit Leerzeichen, vermutlich 
  ebenfalls ein Plugin-Paket für Typst-Support, äquivalent zu com.koner.typst)

ZIELZUSTAND:
  ~/MobileIDE/assets/          -> enthält NUR noch den Ordner "Plugins"
  ~/MobileIDE/assets/Plugins/  -> enthält ALLE Plugins als entpackte Ordner 
                                  (Plugins/<plugin-id>/ mit plugin.json + Ressourcen)

AUFGABEN (IN DIESER REIHENFOLGE):

1. SICHERHEITS-BACKUP
   - Erstelle ein vollständiges Backup von ~/MobileIDE/assets, z. B. als 
     ~/MobileIDE/build/assets-backup-<timestamp>.tar.gz.
   - Backup erst nach bestandener Validierung (Punkt 7) löschen.

2. INVENTAR VON bundled_plugins ERFASSEN
   - Liste den Inhalt von ~/MobileIDE/assets/bundled_plugins/ vollständig auf 
     (find + ls -la) und schreibe das Inventar nach 
     ~/MobileIDE/build/plugins-migration.log.
   - Notiere pro Eintrag die Plugin-ID und Version, um sicherzustellen, dass 
     jeder Plugin-Inhalt aus bundled_plugins durch die ZIP-Pakete im Root oder 
     durch Neu-Erstellung abgedeckt ist. KEIN Plugin darf verloren gehen.

3. BESTEHENDE ORDNER "Plugins" UND "bundled_plugins" LÖSCHEN
   - Lösche ~/MobileIDE/assets/Plugins/ rekursiv (wird in Schritt 4 vollständig 
     neu aufgebaut).
   - Lösche ~/MobileIDE/assets/bundled_plugins/ rekursiv (nachdem das Inventar 
     aus Schritt 2 gesichert wurde).

4. ALLE PLUGIN-ZIP-PAKETE NACH assets/Plugins/ ENTPACKEN
   - Erstelle den Zielordner ~/MobileIDE/assets/Plugins/ neu.
   - Für JEDE der 21 Plugin-ZIPs im Root (com.koner.*, com.scto.mobile.ide.*, 
     io.kiquar.plugin.*) sowie "Typst Support.zip":
     a) ZIP-Struktur inspizieren (unzip -l):
        - Fall A: ZIP enthält oben ein Plugin-Verzeichnis (z. B. 
          com/koner/rust/ oder rust/ mit plugin.json darin) -> so extrahieren, 
          dass der Plugin-Inhalt unter Plugins/<plugin-id>/ landet.
        - Fall B: ZIP enthält plugin.json direkt an der Wurzel -> in einen 
          Unterordner Plugins/<plugin-id>/ extrahieren.
        - Die Plugin-ID aus dem Manifest (plugin.json -> Feld "id") oder aus dem 
          Dateinamen ableiten (z. B. com.koner.rust-1.0.2.zip -> 
          com.koner.rust, Version 1.0.2). Namen ohne Punkt-Konvention 
          ("Typst Support.zip") auf eine gültige Plugin-ID normalisieren 
          (z. B. com.koner.typst, falls das Manifest eine ID enthält, sonst 
          typst-support).
     b) Nach Extraktion prüfen, dass die Struktur 
        Plugins/<plugin-id>/plugin.json existiert; falls das ZIP eine 
        abweichende interne Ordnerstruktur hatte, diese auf das Zielformat 
        normalisieren.
     c) Das verarbeitete ZIP im Root LÖSCHEN (nach erfolgreicher Extraktion), 
        da es durch den entpackten Ordner ersetzt wird.
   - Fehlerprotokoll: Falls eine ZIP defekt ist (unzip-Fehler, fehlendes 
     plugin.json), NICHT löschen, sondern unter 
     ~/MobileIDE/build/failed-plugins.log notieren und die ZIP im Root 
     belassen.

5. VERBLEIBENDE PLUGINS AUS bundled_plugins-NACHLASS PRÜFEN
   - Vergleiche das Inventar aus Schritt 2 mit den in Schritt 4 erzeugten 
     Plugin-Ordnern. Für jede Plugin-ID aus bundled_plugins, die NICHT durch 
     ein ZIP-Paket abgedeckt ist: aus dem Backup (Schritt 1) den Plugin-Inhalt 
     wiederherstellen und nach Plugins/<plugin-id>/ übernehmen. So wird 
     sichergestellt, dass durch das Löschen von bundled_plugins kein Plugin 
     verloren geht.

6. ROOT-ASSETS-ORDNER BIS AUF "Plugins" LEEREN
   - Lösche im Root ~/MobileIDE/assets/ ALLE verbleibenden Einträge AUSSER 
     "Plugins":
       - Features/         -> löschen (nur Doku/Screenshots-Material)
       - Screens/          -> löschen
       - MobileIDE_20260727_220829.md -> löschen (Protokoll-Datei)
       - lsp/              -> VORHER PRÜFEN: Enthält dieser Ordner LSP-Archive 
         (z. B. lsp-<sprache>.tar.gz)? Falls ja: Ist jedes enthaltene LSP-Archiv 
         durch ein Plugin-ZIP abgedeckt (bash_lsp, cpp_lsp, java_lsp, json_lsp, 
         kotlin_lsp, python_lsp, toml_lsp, xml_lsp, yaml_lsp existieren als ZIPs)? 
         Für jedes abgedeckte Archiv -> lsp/ kann gelöscht werden. Falls ein 
         Archiv KEINEM ZIP-Paket entspricht (z. B. css, emmet, html, markdown, 
         typescript, lua), dieses Archiv VOR dem Löschen nach 
         Plugins/_internal/lsp/ verschieben und die Extraktionslogik 
         (shared_extraction.sh) auf den neuen Pfad anpassen.
       - Alle bereits verarbeiteten Plugin-ZIPs (sind in Schritt 4c gelöscht).
   - Dokumentiere jede Lösch-/Verschiebe-Entscheidung in 
     ~/MobileIDE/build/plugins-migration.log.

7. VALIDIERUNG
   - find ~/MobileIDE/assets -maxdepth 2 | sort ausführen und bestätigen:
     Root enthält NUR "Plugins", darunter ALLE erwarteten Plugin-Ordner 
     (mindestens: com.koner.prettier, com.koner.rust, com.koner.typst, 
     com.scto.mobile.ide.bash_lsp, com.scto.mobile.ide.cpp_lsp, 
     com.scto.mobile.ide.java_lsp, com.scto.mobile.ide.json_lsp, 
     com.scto.mobile.ide.kotlin_lsp, com.scto.mobile.ide.plugin.json, 
     com.scto.mobile.ide.plugin.lua, com.scto.mobile.ide.plugin.python, 
     com.scto.mobile.ide.plugins.java.lsp, 
     com.scto.mobile.ide.plugins.kotlin.kmplsp, 
     com.scto.mobile.ide.python_lsp, com.scto.mobile.ide.toml_lsp, 
     com.scto.mobile.ide.xml_lsp, com.scto.mobile.ide.yaml_lsp, 
     io.kiquar.plugin.fs, io.kiquar.plugin.go, io.kiquar.plugin.zig, 
     + Typst-Plugin + ggf. _internal/lsp).
   - Prüfe stichprobenartig, dass in jedem Plugin-Ordner plugin.json mit gültiger 
     id/version existiert.
   - Baue das Projekt neu (./gradlew assembleDebug) und prüfe korrekte APK-Packung.
   - Auf Testgerät: Terminal startet weiterhin (Ubuntu-Sandbox), LSP-Status für 
     alle verfügbaren Sprachen OK (check_lsp_status.sh), Plugin-Store zeigt alle 
     Plugins aus ~/MobileIDE/assets/Plugins/ korrekt an und Installieren/ 
     Deinstallieren funktioniert.
   - Erst nach bestandener Validierung: Backup aus Schritt 1 löschen.

8. VERBOTENE HANDLUNGEN
   - KEIN rm -rf auf ~/MobileIDE/assets ohne vorheriges Backup (Schritt 1).
   - NICHT das lsp/-Verzeichnis löschen, bevor nicht geprüft wurde, dass jedes 
     darin enthaltene Archiv durch ein ZIP-Paket abgedeckt ist (Schritt 6).
   - KEIN Plugin aus dem bundled_plugins-Inventar stillschweigend weglassen.
   - KEINE defekte ZIP löschen (gehört in failed-plugins.log, bleibt im Root 
     erhalten).

Gib am Ende:
- Zusammenfassung aus plugins-migration.log (gelöscht / entpackt / verschoben / 
  fehlgeschlagen),
- finales Baum-Diagramm von ~/MobileIDE/assets/ (textuell),
- Bestätigung, dass Build, Terminal, LSP-Status und Plugin-Store fehlerfrei 
  funktionieren und keine defekten ZIPs im Root verbleiben.
