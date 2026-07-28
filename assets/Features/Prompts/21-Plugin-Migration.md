Führe eine mehrstufige Migration und Refactoring von Extension-/Plugin-Modulen
zwischen den Projekten Xed-Editor und MobileIDE durch. Gehe die drei Schritte
STRIKT IN REIHENFOLGE durch, da spätere Schritte auf den Ergebnissen der
vorherigen aufbauen. Erstelle vor Beginn ein Backup/einen Git-Branch
(z. B. "refactor/extensions-migration"), damit Änderungen nachvollziehbar
und rückgängig machbar sind.

=====================================================================
SCHRITT 1: Submodule verschieben und refaktorieren
=====================================================================
Quelle:  ~/MobileIDE/core/extension
Ziel:    ~/MobileIDE/features/extensions

1. Verschiebe den kompletten Inhalt (inkl. .git-Submodule-Referenz,
   falls es sich um ein Git-Submodul handelt: .gitmodules entsprechend
   aktualisieren) von ~/MobileIDE/core/extension nach
   ~/MobileIDE/features/extensions.
2. Passe alle Package-Deklarationen im verschobenen Code an die neue
   Modul-/Ordnerstruktur an (z. B. Package-Segment von "core.extension"
   zu "features.extensions", sofern die Package-Namenskonvention dem
   Ordnerpfad folgt).
3. Aktualisiere alle Build-Konfigurationsdateien:
   - settings.gradle.kts (Modulpfad-Referenz ändern: ":core:extension"
     → ":features:extensions")
   - build.gradle.kts des Moduls selbst (ggf. Modulname anpassen)
   - Alle build.gradle.kts-Dateien anderer Module, die
     ":core:extension" referenzieren → auf ":features:extensions" ändern
     (u. a. app/build.gradle.kts, :extension-languages, alle
     :plugins:*-lsp Module, die die Extension-API konsumieren)
4. Durchsuche das GESAMTE Projekt (alle Module) nach:
   - Import-Statements, die auf das alte Package/den alten Pfad verweisen
   - String-Referenzen (z. B. Reflection, Ressourcen-Pfade)
   - Kommentare/Dokumentation mit altem Pfad
   und passe diese auf die neue Struktur an.
5. Stelle sicher, dass das Projekt nach der Verschiebung fehlerfrei
   kompiliert (Gradle-Build ausführen, Fehler beheben).
6. Dokumentiere alle geänderten Dateien in einer Änderungsliste.

=====================================================================
SCHRITT 2: Migration von Xed-Editor/features/extensions
=====================================================================
Quelle:  ~/Xed-Editor/features/extensions
Ziel:    ~/MobileIDE/features/extensions (Zusammenführung mit Ergebnis
         aus Schritt 1)

1. Analysiere zunächst die Struktur von ~/Xed-Editor/features/extensions
   und vergleiche sie mit der Ziel-Struktur ~/MobileIDE/features/extensions
   aus Schritt 1, um Namenskonflikte, doppelte Klassen oder
   überlappende Funktionalität zu identifizieren.
2. Kopiere/migriere alle relevanten Dateien aus
   ~/Xed-Editor/features/extensions in ~/MobileIDE/features/extensions,
   OHNE bereits bestehende, funktionierende MobileIDE-Komponenten
   zu überschreiben (bei Konflikten: sinnvoll zusammenführen,
   Duplikate entfernen, Konflikte im Bericht auflisten).
3. Passe den Package-Namen aller migrierten Dateien an die
   MobileIDE-Konvention an (Root-Package: "com.scto.mobile.ide" –
   z. B. "com.scto.mobile.ide.features.extensions").
4. Aktualisiere ALLE Import-Statements in den migrierten Dateien
   entsprechend dem neuen Package-Pfad.
5. Passe die Verzeichnisstruktur an die Source-Code-Konventionen
   von MobileIDE an (z. B. src/main/java/<package-pfad>/..., Gradle-
   Modulstruktur, Ressourcen-Ordner, AndroidManifest-Einträge falls
   vorhanden).
6. Passe alle betroffenen Plugins/Extensions inhaltlich an:
   - Ersetze Xed-Editor-spezifische API-Aufrufe, Interfaces oder
     Basisklassen durch die entsprechenden MobileIDE-Äquivalente
     (z. B. ExtensionAPI/ExtensionContext-Interface, das bereits in
     den migrierten Plugins wie "zig-lsp" via
     com.rk.extension.ExtensionAPI verwendet wird).
   - Passe manifest.json der einzelnen Plugins an die
     MobileIDE-Konventionen an (Felder: id, name, mainClass, version,
     description, author, repository, license, tags, minAppVersion,
     maxAppVersion), gemäß dem bereits etablierten Schema in
     ~/MobileIDE/plugins/*/manifest.json und schema/schema.json.
7. Aktualisiere Build-Dateien (build.gradle.kts, settings.gradle.kts),
   um die migrierten Module/Extensions korrekt einzubinden.
8. Stelle sicher, dass das MobileIDE-Projekt nach der Migration
   fehlerfrei kompiliert.

=====================================================================
SCHRITT 3: Migration von Xed-Editor/core/main/.../com/rk/extension
=====================================================================
Quelle:  ~/Xed-Editor/core/main/src/main/java/com/rk/extension
Ziel:    zu bestimmen – analysiere die MobileIDE-Projektstruktur und
         wähle den fachlich sinnvollsten Zielort. Da es sich hierbei
         um die grundlegenden Extension-API-Basisklassen/Interfaces
         (ExtensionAPI, ExtensionContext, etc.) handelt, die von
         mehreren Modulen (u. a. :extension-languages und allen
         :plugins:*-lsp Modulen) genutzt werden, ist der sinnvollste
         Zielort voraussichtlich ~/MobileIDE/features/extensions
         (als gemeinsame API-Basis, konsistent mit Schritt 1 & 2)
         oder alternativ ein dediziertes API-Submodul
         ~/MobileIDE/core/extension-api, falls eine strikte Trennung
         zwischen API und Feature-Implementierung gewünscht ist.

1. Analysiere den Inhalt von com/rk/extension (Basisklassen,
   Interfaces, Annotationen, Utility-Klassen etc.) und entscheide
   anhand der bestehenden MobileIDE-Architektur (Trennung von
   API/Core vs. Feature-Implementierung), wo dieser Code am
   sinnvollsten hinpasst. Begründe die Entscheidung kurz im
   Abschlussbericht.
2. Verschiebe/kopiere die Dateien an den gewählten Zielort.
3. Passe den Package-Namen von "com.rk.extension" auf das
   entsprechende MobileIDE-Package um (Root: "com.scto.mobile.ide" –
   konsistent mit den in Schritt 2 bereits migrierten mainClass-
   Referenzen in den manifest.json-Dateien halten!).
4. Aktualisiere ALLE Imports im gesamten MobileIDE-Projekt, die auf
   das alte Package "com.rk.extension.*" verweisen (inkl. aus
   Schritt 1 und 2 migriertem Code, sowie in allen bereits
   existierenden :plugins:*-lsp Modulen wie "zig-lsp", "typst-lsp"
   etc., die "import com.rk.extension.ExtensionAPI" und
   "import com.rk.extension.ExtensionContext" verwenden).
5. Aktualisiere alle "mainClass"-Felder in manifest.json-Dateien
   sämtlicher Plugins/Extensions, damit sie auf die neuen,
   vollqualifizierten Klassennamen verweisen.
6. Passe betroffene Plugins/Extensions an, falls sich durch die
   Package-Umbenennung Kompilierfehler oder API-Inkompatibilitäten
   ergeben (z. B. Interface-Signaturen, Basisklassen-Referenzen).
7. Führe eine projektweite Suche nach verbleibenden Referenzen auf
   "com.rk.extension" durch (Code, Ressourcen, Konfigurationsdateien,
   Dokumentation, insbesondere in Plugins-Development.md) und
   aktualisiere diese.
8. Stelle sicher, dass das gesamte MobileIDE-Projekt nach allen drei
   Schritten fehlerfrei kompiliert und ein vollständiger Gradle-Build
   erfolgreich durchläuft.

=====================================================================
ABSCHLUSSBERICHT
=====================================================================
Erstelle nach Abschluss aller drei Schritte einen zusammenfassenden
Bericht mit:

1. Übersicht aller verschobenen/migrierten Dateien und Ordner
   (alt → neu), gegliedert nach Schritt 1, 2, 3
2. Liste aller geänderten Package-Namen (alt → neu)
3. Liste aller angepassten manifest.json-Dateien mit altem und
   neuem "mainClass"-Wert
4. Liste identifizierter und gelöster Namenskonflikte/Duplikate
   zwischen Xed-Editor- und MobileIDE-Code
5. Begründung der Zielort-Entscheidung aus Schritt 3
6. Liste aller Build-Konfigurationsänderungen
   (settings.gradle.kts, build.gradle.kts)
7. Ergebnis des finalen Gradle-Builds (erfolgreich/fehlgeschlagen,
   ggf. verbleibende Fehler mit Dateiangabe)
8. Empfehlungen für manuelle Nacharbeiten (falls automatisierte
   Refaktorierung an bestimmten Stellen nicht sicher möglich war,
   z. B. bei komplexer Reflection-Nutzung oder dynamischem
   Klassenladen von Extensions)
9. Aktualisiere abschließend die Datei ~/MobileIDE/MIGRATION_STATUS.md
   um die drei neuen Migrationsschritte (mit Status "Vollständig"
   oder "Offen" je nach Ergebnis).

**Hinweis:** Anhand deiner hochgeladenen Projektdatei konnte ich sehen, dass `:core:extension` bereits in `settings.gradle.kts` referenziert wird und Root-Package `com.scto.mobile.ide` sowie das Extension-API-Pattern (`ExtensionAPI`/`ExtensionContext` aus `com.rk.extension.*`, z. B. im `zig-lsp`-Plugin) bereits existieren – das habe ich in der Wiederholung präzisiert eingebaut (Schritt 3 als konkretere Zielort-Empfehlung: `~/MobileIDE/features/extensions`).
