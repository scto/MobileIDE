Schließe die drei dokumentierten Lücken des Reports
~/MobileIDE/build/gradle-path-fix2-report.md NACHTRÄGLICH und beweise damit, dass der
"Directory '.../ MobileIDEProjects' does not contain a Gradle build"-Fix in MobileIDE
(com.scto.mobile.ide) vollständig, nachvollziehbar und auf dem Gerät verifiziert ist.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND (aus gradle-path-fix2-report.md + MobileIDE-Dump)
══════════════════════════════════════════════════════════════════
- Report belegt: H2 = mehrere unabhängige Gradle-Invocation-Pfade
  (CodeEditScreen.kt handleRunApk/performBuild, GradleTaskManagerImpl.kt,
  ApkBuilder.kt) mit je eigener workingDir-/ENV-Konstruktion; Fix = zentrale
  Auflösung ProjectPaths.kt.
- ProjectPaths.kt:
  core/common/src/main/java/com/scto/mobile.ide/core/common/utils/ProjectPaths.kt
  (Host→Sandbox-Übersetzung /storage/emulated/0/… → /sdcard/…, Stripping von
  Leerzeichen-Artefakten, Assertion gegen " /"-Sequenzen).
- Integration laut Report: CodeEditScreen.kt, GradleTaskManagerImpl.kt,
  ApkBuilder.kt nutzen die zentrale Auflösung für ProcessBuilder.workingDir und
  MOBILEIDE_PROJECT_DIR.
- Tests: ./gradlew :core:apk-builder:testDebugUnitTest → BUILD SUCCESSFUL in 40s
  (16 Tests). Gesamtbuild: ./gradlew assembleDebug → BUILD SUCCESSFUL in 11m 47s
  (626 Tasks).
- OFFENE LÜCKEN (Gegenstand dieses Prompts):
  1) KEIN Commit-Hash / kein Push-Nachweis für den Fix (im Report nicht genannt),
  2) KEIN On-Device-Nachweis: nicht verifiziert, dass zur Laufzeit tatsächlich
     GRADLE_CWD_SANDBOX=<…/MyApp> und MOBILEIDE_PROJECT_DIR=<…/MyApp> gesetzt sind,
  3) Leerzeichen-Tatbestand ungeklärt: "/storage/emulated/0/ MobileIDEProjects"
     wurde nur als "could also introduce" beschrieben – nicht bewiesen, ob echtes
     Leerzeichen (Code-Bug) oder UI-Zeilenumbruch.
- Bekannte Laufzeit-Anker (aus Dump):
  * LogCatcher unter com.scto.mobile.ide.core.common.utils.LogCatcher, schreibt
    optional nach /sdcard/MobileIDEProjects/logs/ (Toggle in Settings).
  * MOBILEIDE_PROJECT_DIR wird nativ in setup.sh/init.sh aufgelöst (PROGRESS
    2026-07-03); aktive Distro MOBILEIDE_DISTRO=ubuntu.
  * Projekt-Root: /storage/emulated/0/MobileIDEProjects, Projektname z. B. MyApp.
  * App-Version laut README-Badge 0.3.2; versionCode/versionName aus
    app/build.gradle.kts.

══════════════════════════════════════════════════════════════════
PHASE A – GIT-NACHWEIS FÜR DEN FIX
══════════════════════════════════════════════════════════════════
A1. git -C ~/MobileIDE status + git log --oneline -10:
    a) Existiert bereits ein Commit, der ProjectPaths.kt + die 3 Integrations-Dateien
       enthält? (git log --all --oneline -- core/common/.../ProjectPaths.kt)
    b) Falls JA: Commit-Hash + Message + Branch notieren; prüfen ob gepusht
       (git branch -vv, git log origin/<branch> -1).
    c) Falls NEIN (Änderungen uncommittet im Working Tree): ERST verifizieren, dass
       der Arbeitsstand exakt dem Report entspricht (diff checken), DANN committen:
       "fix(apk-builder): centralize project-dir resolution in ProjectPaths for all
       Gradle invocations" und pushen.
A2. Falls es bereits einen früheren Fix-Commit gibt (apkbuilder-pathfix-report,
    fd7130ed): prüfen, ob die ProjectPaths-Änderungen IN diesem Commit oder separat
    liegen. Beide Commits + Hashes in den Report aufnehmen.
A3. Report-Abschnitt "Git-Nachweis" ergänzen: Commit-Hash, Branch, Push-Status,
    geänderte Dateien (git show --stat <hash>).

══════════════════════════════════════════════════════════════════
PHASE B – ON-DEVICE-LAUFZEITNACHWEIS (GRADLE_CWD_SANDBOX / MOBILEIDE_PROJECT_DIR)
══════════════════════════════════════════════════════════════════
B1. SICHERSTELLEN, dass auf dem Gerät die FRISCH GEBaUTE APK installiert ist
    (adb install -r app/build/outputs/apk/debug/*.apk). Vorher:
    adb shell dumpsys package com.scto.mobile.ide | grep -E "versionName|versionCode"
    und mit dem SOLL aus app/build.gradle.kts + dem Commit-Stand (A1) abgleichen.
B2. Debug-Log-Ausgabe an JEDER Gradle-Invocation-Stelle (CodeEditScreen.kt,
    GradleTaskManagerImpl.kt, ApkBuilder.kt) sicherstellen – via LogCatcher
    (com.scto.mobile.ide.core.common.utils.LogCatcher) mit Mindestfeldern:
        GRADLE_CWD_HOST=<host-wd>
        GRADLE_CWD_SANDBOX=<übersetzter-wd>
        MOBILEIDE_PROJECT_DIR=<env-wert>
        GRADLE_CMD=<kommando>
    Falls solche Zeilen noch nicht existieren: temporär ergänzen, mit einer
    LogCatcher-Verbose-Zeile vor Prozessstart; danach wieder entfernen ODER als
    bewusst dauerhafte Debug-Zeile (Kanal BUILD) dokumentieren.
B3. Testlauf auf dem Gerät:
    a) Neues Projekt "MyApp" anlegen (oder bestehendes verwenden), Play-Button /
       Build-Tab-Task ausführen,
    b) LogCatcher-Ausgabe prüfen unter /sdcard/MobileIDEProjects/logs/ bzw. adb
       logcat | grep GRADLE_CWD:
       MUSS zeigen: GRADLE_CWD_SANDBOX=/sdcard/MobileIDEProjects/MyApp
                    MOBILEIDE_PROJECT_DIR=/sdcard/MobileIDEProjects/MyApp
       NICHTS darf auf den Root (…/MobileIDEProjects) oder mit Leerzeichen zeigen.
    c) KEIN "does not contain a Gradle build"; BUILD SUCCESSFUL; APK wird im
       Build-Tab gefunden (BuildHelper-Output-Pfad notieren).
B4. Zusätzlich manuell im Terminal (ubuntu-Sandbox, MOBILEIDE_DISTRO=ubuntu):
    cd /sdcard/MobileIDEProjects/MyApp && pwd → exakt dieser Pfad; und
    echo "$MOBILEIDE_PROJECT_DIR" → exakt …/MyApp.
B5. Report-Abschnitt "On-Device-Nachweis" mit Log-Auszügen (vorher/nachher),
    installierter APK-Version und Bestätigung der GRADLE_CWD_SANDBOX-Zeilen.

══════════════════════════════════════════════════════════════════
PHASE C – LEERZEICHEN-TATBESTAND KLÄREN
══════════════════════════════════════════════════════════════════
C1. Beweis am Code: Grep über ALLE Pfad-Konstruktionen, die zu Gradle/ApkBuilder/
    TerminalCommand/Env führen, auf Muster mit Spaces:
      - grep -rn '" "' core app features | grep -i -E "path|dir|gradle|project"
      - grep -rn '"\$root \$name"\|plus(" ")\|" " +\|\$parent " +\$child'
    Ergebnis: Liste der Fundstellen ODER Nachweis "keine Space-Konkatenation
    vorhanden".
C2. Beweis am Gerät: Den tatsächlich an Gradle übergebenen workingDir HEX-kodiert
    loggen (GRADLE_CWD_SANDBOX_HEX=$(printf %s "$wd" | xxd -p)), damit ein
    verstecktes Leerzeichen (0x20) nach "0/" eindeutig sichtbar wird.
    a) 0x2f 0x20 ("/ ") im Pfad → ECHTES Leerzeichen (Bug, war getroffen von
       ProjectPaths-Cleanup),
    b) kein 0x20 → das Leerzeichen in der Fehlermeldung war ein UI-Zeilenumbruch
       der Build-Log-Anzeige (kein Code-Bug; im Report als "UI-Wrap, kein Pfadbug"
       dokumentieren).
C3. Beide Befunde (Code-Grep + HEX-Log) als Tabelle in den Report:
    Befund | Ort | Beleg | Bewertung (echtes Leerzeichen / UI-Artefakt).

══════════════════════════════════════════════════════════════════
PHASE D – REGRESSION & REPORT-FINALISIERUNG
══════════════════════════════════════════════════════════════════
D1. Regression auf dem Gerät:
    a) Projekt unter /storage/emulated/0/MobileIDEProjects UND unter /sdcard/…
       je einmal bauen → beide Erfolg, GRADLE_CWD_SANDBOX jeweils auf den
       Projekt-Unterordner,
    b) Gradle-Tasks-Panel (Build-Tab) mit einer Task (z. B. :app:assembleDebug)
       → SAME workingDir-Nachweis wie B3,
    c) LSP-Installer über initCommand (z. B. Zig) → kein Konflikt mit neuem
       workingDir-Verhalten.
D2. Report ~/MobileIDE/build/gradle-path-fix2-report.md um die drei Abschnitte
    "Git-Nachweis", "On-Device-Nachweis", "Leerzeichen-Tatbestand" ergänzen
    (A3, B5, C3) und oben im Dokument die Versionszeile
    "Stand: <Datum>, erweitert um Nachsorge" ergänzen.
D3. Falls in B2 temporäre Debug-Zeilen ergänzt wurden: entgültige Entscheidung
    treffen – (a) entfernen ODER (b) als permanente, bewusst dokumentierte
    LogCatcher-Zeilen (Kanal BUILD) belassen. Entscheidung im Report festhalten.
D4. Commit (falls neue Datei-Änderungen): "docs(apk-builder): add runtime and git
    evidence for ProjectPaths fix, clarify whitespace root cause" + Push.

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase A: Falls die Dateien weder committet noch als uncommittete Änderungen
  auffindbar sind (Fix komplett verschwunden), STOPP und Bericht – der Fix muss
  aus dem Report-Code ggf. neu erstellt werden, NICHT raten.
- Phase B: Falls das Gerät nicht erreichbar ist (kein adb), den Nachweis soweit
  möglich per Emulator/Test führen und die fehlenden Laufzeitbelege im Report als
  "offen (Gerät nicht verbunden)" markieren – NICHT erfinden.
- Phase C: Wenn der HEX-Beweis kein Leerzeichen zeigt, wird der Tatbestand
  ehrlich als UI-Artefakt dokumentiert – KEIN erfundener Code-Bug.
