Refaktoriere ALLE Plugins im Verzeichnis ~/Plugins (Quell-Projekte) auf die 
MobileIDE-Struktur von com.scto.mobile.ide: Package-Namen, Verzeichnisstruktur, 
Imports, Manifeste, Gradle-Konfiguration und Ressourcen-Referenzen werden 
einheitlich an das Ziel-Namespace-Schema angepasst. Die Plugins sollen danach 
als Quellcode exakt den Konventionen der MobileIDE-App (com.scto.mobile.ide) 
entsprechen und ohne Anpassungen in das MobileIDE-Plugin-System integriert 
werden können.

PROJEKT-KONTEXT:
- Quell-Ordner: ~/Plugins
  ├── lsp/
  │   ├── scto/                          (fertige ZIPs com.scto.mobile.ide.*.zip — Referenz, NICHT umbenennen)
  │   ├── xed-fsharp/                    (Package io.kiquar.plugin.fs)
  │   ├── xed-go/                        (Package io.kiquar.plugin.go)
  │   ├── xed-go-tools/                  (Package io.kiquar.plugin.go — KONFLIKT mit xed-go)
  │   ├── xed-java-lsp/                  (Package com.androdev.java.lsp)
  │   ├── xed-json-lsp/                  (Package com.rk.xededitor.json, namespace com.rk.xededitor, applicationId com.rk.demo)
  │   ├── xed-kmp-lsp/                   (Package io.kiquar.plugin.kmplsp)
  │   ├── xed-kotlin/                    (Package io.kiquar.plugin.kotlin)
  │   ├── xed-lua-lsp/                   (Package com.rk.lua)
  │   ├── xed-prettier/                  (Package com.koner.prettier)
  │   ├── xed-prettier-standalone/       (Nur TS/Node-Submodul, KEIN Android-Package)
  │   ├── xed-python-lsp/                (Package com.rk.xededitor.python)
  │   ├── xed-rust/                      (Package com.koner.rust)
  │   ├── xed-typst/                     (Package com.koner.typst)
  │   └── xed-zig/                       (Package io.kiquar.plugin.zig)
  └── theme/
      ├── xed-feslake/                   (Theme, kein Java-Code)
      └── xed-themes/                    (Theme-Sammlung, kein Java-Code)
- MobileIDE-Ziel: App-Package com.scto.mobile.ide; gebündelte LSP-IDs im 
  Muster com.scto.mobile.ide.<sprache>_lsp; Katalog unter 
  ~/MobileIDE/assets/Plugins/LSP/catalog.json ist Referenz für gültige IDs.

══════════════════════════════════════════════════════════════════
1. ZIEL-NAMENSSCHEMA BESTIMMEN (VOR JEDEM REFACTORING)
══════════════════════════════════════════════════════════════════
   a) Lese ~/MobileIDE/assets/Plugins/LSP/catalog.json (falls vorhanden) 
      und extrahiere die kanonischen Plugin-IDs + mainClass-Namen.
   b) Leite daraus für jedes Plugin das ZIEL-Package ab. Default-Mapping 
      (NUR verwenden, wenn catalog.json nichts anderes vorgibt):

      Quelle               | Aktuelles Package             | Ziel-Package
      ---------------------|------------------------------|-----------------------------
      xed-fsharp           | io.kiquar.plugin.fs          | com.scto.mobile.ide.plugin.fs
      xed-go               | io.kiquar.plugin.go          | com.scto.mobile.ide.plugin.go
      xed-go-tools         | io.kiquar.plugin.go          | com.scto.mobile.ide.plugin.go.tools
      xed-java-lsp         | com.androdev.java.lsp        | com.scto.mobile.ide.plugins.java.lsp
      xed-json-lsp         | com.rk.xededitor.json        | com.scto.mobile.ide.json_lsp
      xed-kmp-lsp          | io.kiquar.plugin.kmplsp      | com.scto.mobile.ide.plugins.kotlin.kmplsp
      xed-kotlin           | io.kiquar.plugin.kotlin      | com.scto.mobile.ide.kotlin_lsp
      xed-lua-lsp          | com.rk.lua                   | com.scto.mobile.ide.plugin.lua
      xed-prettier         | com.koner.prettier           | com.scto.mobile.ide.plugin.prettier
      xed-python-lsp       | com.rk.xededitor.python      | com.scto.mobile.ide.python_lsp
      xed-rust             | com.koner.rust               | com.scto.mobile.ide.plugin.rust
      xed-typst            | com.koner.typst              | com.scto.mobile.ide.plugin.typst
      xed-zig              | io.kiquar.plugin.zig         | com.scto.mobile.ide.plugin.zig

      WICHTIG: Wenn catalog.json eine ANDERE ID für ein Plugin vorgibt 
      (z. B. io.kiquar.plugin.fs-0.1.0.zip), dann hat catalog.json 
      VORRANG — Ziel-Package = diese ID. Die Tabelle in 
      ~/MobileIDE/build/plugin-package-mapping.tsv dokumentieren 
      (quelle | altes_package | neues_package | quelle_mapping: catalog|default).
   c) Das Verzeichnis lsp/scto/ NICHT umbenennen (enthält bereits 
      fertige MobileIDE-ZIPs) — nur als Referenz nutzen.

══════════════════════════════════════════════════════════════════
2. BACKUP & BESTANDSAUFNAHME
══════════════════════════════════════════════════════════════════
   a) Vollständiges Backup von ~/Plugins anlegen (z. B. 
      ~/MobileIDE/build/plugins-src-backup-<timestamp>.tar.gz).
   b) Pro Plugin die Dateien identifizieren, die das Package referenzieren:
      - *.kt / *.java unter app/src/main/java/** (package-Deklaration 
        UND import-Anweisungen der EIGENEN Klassen),
      - app/src/main/AndroidManifest.xml (package-Attribut, android:name),
      - app/build.gradle.kts (namespace, applicationId, ggf. 
        resource-prefix),
      - manifest.json / plugin.json (id, mainClass),
      - settings.gradle.kts (rootProject.name),
      - proguard-rules.pro (Keep-Regeln mit alten Package-Namen),
      - res/**/strings.xml sowie drawable-Referenzen (nur falls 
        Package-Strings enthalten),
      - .github/workflows/*.yml (nur falls Ausgabe-/APK-Pfade oder 
        Package-Namen referenziert werden),
      - README/CHANGELOG (nur wenn Package-Namen dokumentiert sind).
   c) Ergebnis je Plugin in ~/MobileIDE/build/plugin-package-mapping.tsv 
      ergänzen (Liste der zu ändernden Dateien).

══════════════════════════════════════════════════════════════════
3. REFACTORING PRO PLUGIN (REIHENFOLGE JE PLUGIN)
══════════════════════════════════════════════════════════════════
   a) VERZEICHNISSTRUKTUR VERSCHIEBEN
      - Quell-Ordner verschieben, z. B.:
        git mv app/src/main/java/io/kiquar/plugin/fs \
               app/src/main/java/com/scto/mobile/ide/plugin/fs
        (falls kein Git-Repo: mv + neue Verzeichnisse anlegen)
      - KEINE Leer-Ordner der alten Struktur zurücklassen.
   b) package-DEKLARATIONEN
      - In JEDER .kt/.java-Datei die erste Zeile 
        `package <alt>;` → `package <neu>;` ändern.
   c) IMPORTS ANPASSEN
      - ALLE `import <alt>.*;`-Anweisungen, die EIGENE Klassen des 
        Plugins referenzieren, auf das neue Package umschreiben.
      - IMPORTANT: SDK-/Fremd-Imports aus der sdk.jar (z. B. 
        com.rk.extension.ExtensionAPI, com.rk.file.FileType, 
        com.rk.file.FileTypeManager, com.rk.icons.Icon, 
        com.rk.extension.ExtensionContext, io.github.rosemoe.sora.*, 
        androidx.*, com.google.*) bleiben UNVERÄNDERT — das sind 
        fremde APIs, keine Plugin-Packages.
      - R-Klasse: `import <alt>.R` → `import <neu>.R` (bzw. implizites 
        R im selben Package automatisch korrekt nach Schritt a).
   d) ANDROIDMANIFEST.XML
      - package-Attribut im <manifest> auf das neue Package setzen.
      - android:name-Attribute: Relative Namen (".Main") automatisch 
        korrekt; VOLL-QUALIFIZIERTE Namen (z. B. 
        "io.kiquar.plugin.fs.Main") auf das neue Package umschreiben.
      - Activity/Service/Receiver-Klassen ebenfalls prüfen.
   e) BUILD.GRADLE.KTS
      - namespace = "<neues Package>"
      - applicationId = "<neues Package>" (auf MobileIDE-Namensschema)
      - Keine Änderung an dependencies/compileOnly.
   f) MANIFEST.JSON / PLUGIN.JSON
      - "id" auf die kanonische MobileIDE-ID setzen.
      - "mainClass" auf die voll-qualifizierte neue Main-Klasse setzen 
        (z. B. "com.scto.mobile.ide.plugin.fs.Main").
   g) SETTINGS.GRADLE.KTS
      - rootProject.name auf das Ziel-Package/Plugin-ID setzen 
        (z. B. "com.scto.mobile.ide.plugin.fs" bzw. lesbarer Name).
   h) PROGUARD / RESSOURCEN
      - proguard-rules.pro: Keep-Regeln, die alte Package-Pfade 
        enthalten, auf neue Pfade umschreiben.
      - strings.xml/XML: Enthält ein String einen alten Package-Namen, 
        ersetzen (sonst unverändert lassen).
   i) CODE-INTERNE STRING-REFERENZEN
      - Grep nach dem alten Package-Namen in .kt/.java (z. B. 
        Klassennamen-Reflektion, Log-Tags, Intent-Action-Strings). 
        Diese gezielt auf das neue Package umstellen.

══════════════════════════════════════════════════════════════════
4. KONFLIKTE & SONDERFÄLLE
══════════════════════════════════════════════════════════════════
   a) xed-go vs. xed-go-tools (beide io.kiquar.plugin.go):
      - xed-go behält das Basis-Package (com.scto.mobile.ide.plugin.go).
      - xed-go-tools bekommt das abweichende Package 
        (com.scto.mobile.ide.plugin.go.tools), NUR falls die App nicht 
        bereits beide unter derselben ID registriert. Konflikt im 
        Mapping-Log dokumentieren.
   b) xed-json-lsp (namespace com.rk.xededitor, applicationId com.rk.demo):
      - Komplett auf com.scto.mobile.ide.json_lsp umstellen (namespace 
        UND applicationId), da "com.rk.demo" kein valides 
        MobileIDE-Package ist.
   c) xed-prettier-standalone (kein Android-Modul):
      - KEIN Package-Refactoring. Nur prüfen, ob die ZIP-Ausgabe 
        (createFinalZip) von xed-prettier darauf referenziert; falls ja, 
        Pfade konsistent halten.
   d) theme/xed-feslake & theme/xed-themes:
      - KEIN Java-Refactoring nötig. Nur prüfen, ob theme.json/README 
        IDs enthalten, die auf MobileIDE-Namen zeigen sollten; ggf. 
        Metadaten anpassen und im Log dokumentieren.
   e) lsp/scto/*.zip: NICHT verändern (Referenz-Deliverables).
   f) Falls ein Plugin NUR als ZIP im Projekt liegt und kein Quell-
      Verzeichnis hat: im Log vermerken, nicht refaktorieren.

══════════════════════════════════════════════════════════════════
5. VALIDIERUNG (NACH ALLEN REFACTORINGS)
══════════════════════════════════════════════════════════════════
   a) Grep-Check: `grep -r "<altes-package>" <plugin>/app/src` muss für 
      JEDES migrierte Plugin 0 Treffer liefern (Ausnahme: README/CHANGELOG 
      sind erlaubt, aber besser ebenfalls aktualisiert).
   b) Verzeichnis-Check: `find <plugin>/app/src/main/java -type d` zeigt 
      nur noch neue Package-Pfade.
   c) Manifest-Check: jq auf manifest.json/plugin.json — id + mainClass 
      zeigen auf neue Namen; mainClass-Klasse existiert unter neuem Pfad.
   d) BUILD PRO PLUGIN: In jedem Plugin-Ordner 
      `./gradlew assembleDebug` (bzw. buildExtensionDebug) ausführen — 
      MUSS mit Exit-Code 0 enden. Fehlerfälle in 
      ~/MobileIDE/build/plugin-package-migration.log festhalten.
   e) MobileIDE-Referenz-Check: Nach der Migration sicherstellen, dass 
      die App-Referenzen (catalog.json, LSP-Start-Skripte, 
      shared_extraction.sh, PluginStoreManager) zu den NEUEN IDs 
      passen; bei Abweichung App-Referenzen gezielt anpassen oder im Log 
      als offenen Punkt markieren.

══════════════════════════════════════════════════════════════════
6. KATALOG/ZIP-ABGLEICH (NUR WENN VOM ZIEL GEFORDERT)
══════════════════════════════════════════════════════════════════
   - Falls die MobileIDE-Struktur vorschreibt, dass ZIP-Archive und 
     Katalogeinträge exakt zum neuen Package-Namen passen:
     a) Betroffene Plugin-ZIPs unter 
        ~/MobileIDE/assets/Plugins/LSP/ mit NEUER Namenskonvention 
        <neue-id>-<version>.zip neu erzeugen (aus dem refaktorierten 
        Quellcode via createFinalZip/buildExtensionRelease),
     b) catalog.json aktualisieren (id, mainClass, sha256, sizeBytes, 
        downloadUrl), README.md synchron halten,
     c) Alte ZIPs mit altem Namen entfernen (vorher Backup).
   - Wenn die bestehenden ZIP-Namen bereits der MobileIDE-Konvention 
     entsprechen und NICHT angefasst werden sollen: Abgleich nur 
     protokollieren.

══════════════════════════════════════════════════════════════════
7. DOKUMENTATION
══════════════════════════════════════════════════════════════════
   - ~/MobileIDE/build/plugin-package-mapping.tsv: 
     plugin | altes_package | neues_package | quelle_mapping | build_ok
   - ~/MobileIDE/build/plugin-package-migration.log: 
     pro Plugin die geänderten Dateien + Entscheidungen 
     (Konflikte, Sonderfälle, offene Punkte).

GIB AM ENDE:
- Die vollständige Mapping-Tabelle (plugin | alt | neu | Build ✅/❌),
- Liste aller geänderten Dateien pro Plugin (zusammengefasst),
- Ergebnis des Grep-Checks (0 alte Package-Treffer je Plugin),
- Bestätigung, dass jedes Plugin eigenständig baut UND dass die 
  MobileIDE-Referenzen (catalog.json / LSP-Skripte) mit den neuen 
  Packages konsistent sind bzw. exakt gelistete offene Punkte.
