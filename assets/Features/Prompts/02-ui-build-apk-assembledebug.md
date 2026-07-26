# Ziel
Ermögliche das Bauen eines Android-Projekts (z. B. app:assembleDebug) direkt aus der 
Compose-UI (EditorScreen) und stelle sicher, dass ein tatsächlicher APK-Build erfolgt, 
dessen Ergebnis (Pfad, Erfolg/Fehler) in der UI angezeigt wird.

# Kontext
- Baut auf Prompt "01-gradle-ide-terminal-bridge" auf (GradleBridgeService.runTasks()).
- :core:apk-builder existiert bereits (AAPT2/D8/Signing/Zipalign-Toolset), 
  ApkBuilder.kt akzeptiert bereits eine configureProcessBuilder-Lambda für PRoot-Wrapping.
- Play-Button in CodeEditScreen.kt existiert schon für "Projekt ausführen" – prüfe, 
  ob er aktuell schon assembleDebug/assembleRelease aufruft oder nur ApkBuilder direkt nutzt.

# Anforderungen
1. Erweitere EditorScreen/CodeEditScreen um einen dedizierten "Build"-Button (getrennt vom 
   bestehenden Play/Run-Button), der eine Auswahl zwischen typischen Build-Varianten anbietet:
   - assembleDebug, assembleRelease, bundleRelease (falls App-Modul vorhanden), 
     sowie beliebige Variante via Freitext (Fallback für Custom-Module wie plugin-Projekte).
2. Der Build-Vorgang nutzt GradleBridgeService.runTasks(listOf(selectedTask), flags) 
   und zeigt den Fortschritt im bestehenden Build-BottomSheet (Tab "Build").
3. Nach Build-Abschluss:
   - Bei Exit-Code 0: Suche automatisch nach der generierten APK unter 
     `<module>/build/outputs/apk/<variant>/*.apk` (rekursiver Scan analog zu 
     createFinalZip-Task-Logik in den Plugin-build.gradle.kts-Dateien).
   - Zeige im Build-Tab eine Erfolgsmeldung mit vollem Pfad und Dateigröße an, 
     plus Buttons "Installieren" (Android PackageInstaller Intent) und 
     "Im Dateimanager öffnen" (FileOperations.openWithExternalApp, analog zu TypstPreviewRunner-Pattern).
   - Bei Exit-Code != 0: Markiere die zugehörigen Fehlerzeilen (LogLevel.ERROR) und springe 
     automatisch zur ersten Fehlerzeile im Log (Auto-Scroll-to-error).
4. Persistiere die zuletzt gewählte Build-Variante pro Projekt (SharedPreferences/DataStore) 
   als Default für den nächsten Build-Klick.

# Akzeptanzkriterien
- Klick auf "Build > assembleDebug" erzeugt nachweislich eine echte, valide APK-Datei im 
  erwarteten Output-Pfad.
- Fehlerfälle (z. B. Kotlin-Kompilierfehler) werden klar farblich hervorgehoben und sind 
  ohne Terminal-Wechsel direkt in der UI lesbar.
```
