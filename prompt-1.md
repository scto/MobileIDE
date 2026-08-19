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
