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
- Falls Abweichungen: konkrete Empfehlung zur Behebung./
