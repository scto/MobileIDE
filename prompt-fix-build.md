Behebe den erneut auftretenden APK-Build-Fehler in MobileIDE (com.scto.mobile.ide) ENDGÜLTIG:

  Gradle-Fehler im Build-Tab:
  "Directory '/storage/emulated/0/ MobileIDEProjects' does not contain a Gradle build.
   A Gradle build's root directory should contain one of the possible settings files:
   settings.gradle, settings.gradle.kts, settings.gradle.dcl ..."
  → BUILD FEHLGESCHLAGEN (Exit-Code 1)

GLEICHZEITIG gilt: Der zuvor erstellte Fix (Commit fd7130ed, apkbuilder-pathfix-report.md)
behauptet die Behebung per PathTranslator-Segment-Assertions + expliziter workDir-Übergabe
und meldet BUILD SUCCESSFUL + 16 grüne Unit-Tests. Da der Fehler TROTZDEM auftritt, ist eine
der beiden Hypothesen wahr und muss bewiesen/ausgeschlossen werden:
  H1: Die installierte APK ist VERALTET (Fix-Code nie gebaut/installiert).
  H2: Es existiert EIN ZWEITER Code-Pfad (nicht handleRunApk), der Gradle mit dem
      Projekt-ROOT statt mit dem Projekt-Unterordner startet, bzw. MOBILEIDE_PROJECT_DIR
      wird in setup.sh/init.sh auf den Root statt auf <root>/<Projektname> gesetzt.

══════════════════════════════════════════════════════════════════
VERIFIZIERTER IST-ZUSTAND (aus Dump MobileIDE_20260816_043126.md + Reports)
══════════════════════════════════════════════════════════════════
- Fehlermeldung zeigt Pfad "/storage/emulated/0/ MobileIDEProjects":
  (a) OHNE Projektnamen "MyApp" → es wird im Projekt-ROOT gearbeitet,
  (b) Leerzeichen nach "0/" → mögliche fehlerhafte String-Konkatenation
      ("$root $name" statt "$root/$name") ODER reines UI-Wrap-Artefakt der
      Log-Anzeige – beides verifizieren, nicht annehmen.
- PROGRESS.md 2026-07-03: Container-Setup (setup.sh & init.sh) löst den Projektpfad
  NATIV über die Env-Variable MOBILEIDE_PROJECT_DIR auf (statt nested bash cd).
  → MUSS auf <sandbox>/sdcard/MobileIDEProjects/MyApp zeigen, NICHT auf den Root.
- PROGRESS.md 2026-07-04: CodeEditScreen.kt handleRunApk übergibt
  DistroManager.buildProotCommand als configureProcessBuilder-Lambda an ApkBuilder.kt;
  Gradle-Aufruf erfolgt als "bash ./gradlew" (noexec-Bypass für Shared Storage).
- PROGRESS.md 2026-07-04: DistroManager.kt hängt initCommand (wenn vorhanden) an die
  Session-Argumente an; MOBILEIDE_DISTRO=ubuntu ist die aktive Distro.
- PROGRESS.md 2026-07-03: NewProjectScreen.kt erzeugt Projekte aus Templates und hängt
  android.aapt2FromMavenOverride=/.mobileide/aapt2 an gradle.properties an.
- Fix-Report apkbuilder-pathfix-report.md: Ursache war zuvor die Konkatenation
  "$cleanPath/$folderName" in CodeEditScreen.kt; Fix = PathTranslator.kt mit strikten
  Segment-Assertions + expliziter workDir-Übergabe; 16 Unit-Tests grün; Commit fd7130ed.
- GradleTaskManager/GradleTasksDialog (ToolingBottomSheet, Build-Tab, Prompt 01) führen
  Gradle-Tasks EIGENSTÄNDIG aus und streamen via Flow<GradleLogLine> – dieser Pfad ist
  vom handleRunApk-Fix NICHT abgedeckt und muss separat auf seine workingDir-Auflösung
  geprüft werden (stark verdächtig für H2).
- BuildHelper.kt scannt Output-APKs und installiert via FileProvider.

══════════════════════════════════════════════════════════════════
PHASE A – BEWEISFÜHRUNG H1 vs. H2 (keine Änderung, nur Befund)
══════════════════════════════════════════════════════════════════
A1. VERIFIZIERE DEN INSTALLIERTEN BUILD (H1):
    a) Installierte APK-Version/versionCode prüfen (adb shell dumpsys package
       com.scto.mobile.ide | grep versionName/versionCode),
    b) Prüfen, ob die installierte APK nach Commit fd7130ed gebaut wurde
       (Build-Timestamp aus BuildConfig/app_versionname im Dump der installierten
       APK auslesen: adb shell run-as com.scto.mobile.ide cat ... bzw. aapt dump
       badging auf die im Projekt gebaute APK),
    c) Falls installierte Version älter als der Fix: NEU BAUEN und installieren
       (./gradlew assembleDebug + adb install -r) und Test WIEDERHOLEN. Falls der
       Fehler danach verschwindet → H1 bewiesen, Bericht, fertig.
A2. INVENTAR ALLER GRADLE-STARTSTELLEN (H2):
    Lokalisiere JEDE Stelle, die ein Gradle-Kommando erzeugt/startet, und dokumentiere
    je Fundstelle in build/gradle-invocation-inventory.tsv:
       datei:zeile | kommandobau | workingDir-auflösung | quell-pfad (host) |
       übersetzter pfad (sandbox) | env (MOBILEIDE_PROJECT_DIR?) | getestet?
    Mindestens prüfen:
    a) CodeEditScreen.kt handleRunApk (ApkBuilder + configureProcessBuilder),
    b) GradleTaskManager.kt / GradleTasksDialog.kt (ToolingBottomSheet Build-Tab),
    c) BuildHelper.kt / GradleTasksPanel (Task-Liste, Play-Icon),
    d) Jeder weitere Aufruf von "bash ./gradlew", "gradlew", "gradle" im Kotlin-Code
       (grep -rn "gradlew" app core features).
A3. MOBILEIDE_PROJECT_DIR-TRACE:
    a) Wo wird die Variable GESETZT? (CodeEditScreen? DistroManager.buildProotCommand?
       TerminalCommand.env? setup.sh? init.sh? ideenv?)
    b) Auf welchen WERT? (Root vs. <root>/<Projektname>; Host- vs. Sandbox-Pfad)
    c) Wird sie im Sandbox-Namensraum korrekt gemappt (/storage/emulated/0/... →
       /sdcard/... bzw. /storage/emulated/0/...)?
    d) Führe einen Debug-Logdruck ein (LogCatcher, Kanal BUILD): vor jedem Gradle-Start
       exakt loggen:  GRADLE_CWD_HOST=<...>  GRADLE_CWD_SANDBOX=<...>
                      MOBILEIDE_PROJECT_DIR=<...>  GRADLE_CMD=<...>
A4. LEERZEICHEN-CHECK:
    a) Suche ALLE Pfad-Konkatenationen, die ein " " enthalten könnten
       (grep -rn '" " *+' / ' + " "' / "\$root \$name" / "path.plus(\" \")" u. ä.),
    b) Prüfe, ob "/storage/emulated/0/ MobileIDEProjects" im Log durch Zeilenumbruch/
       UI-Wrapping des Build-Tabs verursacht wird (dann kein echter Bug, im Bericht
       vermerken) ODER wirklich ein Space im Pfad steckt (dann harter Bug → Phase C).
A5. Bericht build/gradle-path-fix2-befund.md: H1-Beweis ODER vollständiges
    Invocation-Inventar (A2) + MOBILEIDE_PROJECT_DIR-Wertkette (A3) + Leerzeichen-
    Befund (A4) + eindeutige Ursachenzuordnung (H1 | H2a zweiter Pfad | H2b falsche
    ENV | H2c Leerzeichen | Kombination).

══════════════════════════════════════════════════════════════════
PHASE B – EINZIGE, ZENTRALE PROJEKTPFAD-AUFLÖSUNG (Single Source of Truth)
══════════════════════════════════════════════════════════════════
B1. Führe EINE Funktion ein (z. B. ProjectPaths.resolveProjectSandboxDir(project)):
    - Eingang: Projekt-Objekt/Ordner (Name + Elternpfad) AUS DER AKTIVEN PROJEKT-
      VERWALTUNG (editorManager/ProjectManager),
    - Ausgang: hostDir = <projectsRoot>/<name>  UND  sandboxDir = übersetzt via
      PathTranslator.toSandboxPath(hostDir),
    - ASSERTIONS (wie im bisherigen PathTranslator-Fix): Segmentanzahl bleibt gleich,
      letztes Segment == Projektname, KEIN Leerzeichen im Pfad,
    - KEINE String-Konkatenation mit " " – ausschließlich "$parent/$name" bzw.
      File(parent, name).
B2. NUTZE DIESE Funktion an ALLEN Fundstellen aus A2 (handleRunApk, GradleTaskManager,
    BuildHelper, Gradle-Tasks-Panel). Es darf danach KEINE Stelle mehr geben, die den
    Projektpfad selbst baut.
B3. Gradle-Aufruf-Regel (verbindlich für alle Stellen):
    - workingDirectory des Prozesses == sandboxDir (volles Projektverzeichnis),
    - Kommando: bash ./gradlew <task> (nicht -p auf den Root),
    - env: MOBILEIDE_PROJECT_DIR=sandboxDir (VOLLER Projektpfad, nie Root).

══════════════════════════════════════════════════════════════════
PHASE C – VERBLEIBENDE FEHLERQUELLEN SCHLIESSEN
══════════════════════════════════════════════════════════════════
C1. Falls A3 zeigt, dass MOBILEIDE_PROJECT_DIR falsch gesetzt wird:
    - Korrigiere die Setter (DistroManager.buildProotCommand / TerminalCommand.env /
      setup.sh / init.sh / ideenv), sodass IMMER <projectsRoot>/<Projektname>
      (sandboxübersetzt) gesetzt wird,
    - Verifiziere init.sh: cd "$MOBILEIDE_PROJECT_DIR" darf nicht mehr in den Root
      fallen; falls die Variable fehlt/leer ist, MUSS ein klarer Fehler kommen
      (exit 1 + Meldung), NICHT stillschweigend ins Root-/Home-Verzeichnis wechseln.
C2. Falls A4 ein echtes Leerzeichen bestätigt: Jede Pfad-Konstruktion mit Space
    korrigieren und durch File(parent, name) ersetzen. Zusätzlich Guard in
    PathTranslator: Ergebnispfad darf keinen " /"-Teilstring enthalten.
C3. Falls H1 bestätigt wurde, trotzdem C1/C2 defensiv absichern (Root-Fallback
    VERBIETEN: Statt auf den Root auszuweichen → Fehler "PROJECT_DIR_MISSING
    host=... sandbox=..." mit Anleitung, wie Phase C1 im ersten Pathfix-Prompt).
C4. Regression: Die aus PROGRESS.md bekannten Eigenschaften müssen erhalten bleiben:
    bash ./gradlew (noexec-Bypass), aapt2FromMavenOverride in gradle.properties neuer
    Projekte, initCommand-Forwarding für LSP-Installer, MOBILEIDE_DISTRO=ubuntu.

══════════════════════════════════════════════════════════════════
PHASE D – VALIDIERUNG & BERICHT
══════════════════════════════════════════════════════════════════
D1. ./gradlew assembleDebug → BUILD SUCCESSFUL.
D2. Unit-Tests: ./gradlew test (mind. PathTranslatorTest, neu: ProjectPathsTest)
    → ALLE grün, insbesondere: Root+Name → exakt "<root>/<name>", kein Space,
    Segment-Erhalt, /sdcard-Pfade unverändert.
D3. AUF DEM GERÄT (WICHTIG: frisch gebaute APK installieren, nicht alte!):
    a) Neues Projekt "MyApp" anlegen → verifiziere, dass
       /storage/emulated/0/MobileIDEProjects/MyApp/{settings.gradle.kts,
       build.gradle.kts, gradle.properties, app/} existiert,
    b) Play-Button drücken → LogCatcher/Build-Log MUSS zeigen:
       GRADLE_CWD_SANDBOX=/sdcard/MobileIDEProjects/MyApp
       MOBILEIDE_PROJECT_DIR=/sdcard/MobileIDEProjects/MyApp
    c) KEIN "does not contain a Gradle build", Gradle läuft im Projektverzeichnis,
       BUILD SUCCESSFUL, APK wird im Build-Tab als installierbar gefunden,
    d) Gradle-Tasks-Panel (Build-Tab) mit einer Task testen → SAME workingDir,
    e) Regression: bestehendes Projekt sowie Projekt unter /sdcard/... bauen.
D4. build/gradle-path-fix2-report.md:
    - H1- oder H2-Beweis mit Log-Auszügen (vorher/nachher),
    - Tabelle: Datei | Änderung | Test (aus A2-Inventar),
    - die tatsächlich installierte APK-Version + Commit-Hash des gebauten Codes,
    - offene Punkte.
D5. Commit: "fix(apk-builder): enforce single project-dir resolution for all Gradle
    invocations; fix MOBILEIDE_PROJECT_DIR to full project path; reinstall fixed APK"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGELN:
- Phase A MUSS zuerst einen eindeutigen Befund liefern (H1 oder H2a/b/c oder
  Kombination). Bei Mehrdeutigkeit NICHT raten – Diagnose wiederholen und Ausgaben
  im Befund festhalten.
- In Phase B2: Sobald eine Fundstelle aus A2 nicht über die zentrale Funktion läuft,
  STOPP und diese Stelle im Bericht listen – es darf NICHTS unentdeckt bleiben.
- Keine Änderungen an Rootfs-/Sandbox-Installation selbst (ubuntu/alpine) außer der
  Pfadauflösung aus C1.
- Kein "Schnellfix" durch Ausweichen auf den Projekt-ROOT – der Fallback auf den
  Elternordner ist ausdrücklich VERBOTEN (war die Wurzel des Originalfehlers).
