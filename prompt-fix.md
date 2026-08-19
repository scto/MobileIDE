Behebe den Build-Fehler:
"Directory '/storage/emulated/0/MobileIDEProjects' does not contain a Gradle build."
(Exit-Code 1) beim APK-Build über den Play-Button / ApkBuilder in MobileIDE
(com.scto.mobile.ide), inklusive Multi-Distro-Korrektur (Ubuntu bevorzugen,
Alpine ignorieren) und Kotlin-LSP-Konsistenzprüfung (kotlin.sh vs.
kmp-lsp-installer.sh).

══════════════════════════════════════════════════════════════════
KONTEXT / VERIFIZIERTER IST-ZUSTAND
══════════════════════════════════════════════════════════════════
- Vorheriger Fehler "Projektverzeichnis ungültig ... /MyApp" wurde bereits durch
  Pfad-Übersetzung + PRoot-Bind + Fallback behoben.
- NEUER Fehler: Gradle startet mit dem ELTERN-Ordner
  /storage/emulated/0/MobileIDEProjects (OHNE "MyApp").
- MULTI-DISTRO (WICHTIG): MobileIDE unterstützt mehrere Rootfs unter
  /data/data/com.scto.mobile.ide/local/<distro>/ (ubuntu, alpine, ...).
  ALPINE IST IRRELEVANT, WENN UBUNTU INSTALLIERT IST (check_distro_rootfs.sh
  prüft home/etc/usr/bin). Der APK-Build läuft in der AKTIVEN Distro
  (MOBILEIDE_DISTRO). Alle Pfadauflösungen NUR gegen die aktive/bevorzugte
  Distro (ubuntu) prüfen; alpine-Fehler NICHT blockierend behandeln.
- KOTLIN-LSP (WICHTIG): Drei getrennte Kotlin-LSP-Pfade existieren:
  a) App built-in :features:lsp, local/bin/lsp/kotlin.sh → installiert fwcd
     kotlin-language-server nach /opt/kotlin-language-server (Symlink
     /usr/local/bin),
  b) Plugin xed-kotlin, kotlin-lsp-installer.sh → $HOME/.lsp/kotlin/bin/
     intellij-server (JetBrains, 262.8190.0),
  c) Plugin xed-kmp-lsp, kmp-lsp-installer.sh → $HOME/.lsp/kmp-lsp/kmp-lsp +
     kmp-jar-indexer (Hessesian kmp-lsp, Rust, v0.24.0).
  kotlin.sh nimmt KEINE Notiz von kmp-lsp-installer.sh — getrennte Binaries
  und Verzeichnisse. KmpServer registriert sich für .kt/.java/.swift und
  überlappt damit die built-in Kotlin-Registrierung (Doppel-Registrierung,
  bereits in Prompt 31 Schritt C durch Entfernen von :plugins:kotlin-kmp-lsp
  adressiert).
- Bekannte Codestellen: ApkBuilder.kt (configureProcessBuilder-Lambda,
  CodeEditScreen.kt handleRunApk → DistroManager.buildProotCommand),
  setup.sh/init.sh lösen Projektpfad via MOBILEIDE_PROJECT_DIR auf
  (PROGRESS.md 2026-07-03), NewProjectScreen.kt erzeugt Projekte aus Templates
  und hängt android.aapt2FromMavenOverride an gradle.properties an.
- Verdächtig: Pfad erscheint als "/storage/emulated/0/ MobileIDEProjects"
  (evtl. Leerzeichen nach "0/" durch String-Konkatenation).

══════════════════════════════════════════════════════════════════
PHASE A — DIAGNOSE & PFAD-TRACE (keine Änderung, nur Befund)
══════════════════════════════════════════════════════════════════
A1. AKTIVE DISTRO FESTSTELLEN (MULTI-DISTRO):
    a) echo "MOBILEIDE_DISTRO=[$MOBILEIDE_DISTRO]" (Host-Terminal und Sandbox),
    b) ls -la /data/data/com.scto.mobile.ide/local/ — welche Distros vorhanden?
    c) Für ubuntu: /data/data/com.scto.mobile.ide/local/ubuntu/{home,etc,usr,bin}
       vorhanden und vollständig? (check_distro_rootfs.sh nutzen)
    d) Falls ubuntu vollständig: alpine VOLLSTÄNDIG IGNORIEREN — keinen Test,
       keinen Fix, keine Fehlermeldung dazu. Nur im Bericht notieren
       ("alpine: übersprungen, ubuntu aktiv").
A2. PROJEKTPFAD IN DER AKTIVEN DISTRO PRÜFEN:
    a) ls -la /storage/emulated/0/MobileIDEProjects/
    b) ls -la /storage/emulated/0/MobileIDEProjects/MyApp/
    c) find <MyApp> -maxdepth 1 -name "*.gradle*" — Template vorhanden?
    d) In der Sandbox (ubuntu): echo "[$MOBILEIDE_PROJECT_DIR]"; ls -la darauf.
    e) find /storage/emulated/0 -maxdepth 1 -name "*MobileIDE*" -print0 | xxd
       (Leerzeichen-Check im Verzeichnisnamen → U4).
A3. CODESTELLEN TRACEN: In ApkBuilder.kt und CodeEditScreen.kt dokumentieren,
    wo der Projektpfad (a) validiert, (b) übersetzt (toSandboxPath), (c) an
    PRoot/Gradle übergeben wird. Pro Stelle: exakter Pfad-Wert (Logcat/Debug).
A4. GRADLE-INVOCATION PRÜFEN: -p <pfad> oder nur cwd? Wird der Projektname
    (MyApp) an den Root angehängt? U1–U5 eindeutig zuordnen.

══════════════════════════════════════════════════════════════════
PHASE B — toSandboxPath HART ABSICHERN (falls U3)
══════════════════════════════════════════════════════════════════
B1. toSandboxPath MUSS den KOMPLETTEN Pfad inkl. letztem Segment (MyApp)
    unverändert weiterreichen. Verbieten: substringBeforeLast/substringAfterLast,
    File(path).parentFile, fehlerhafte removePrefix/removeSuffix.
B2. ASSERT: Ergebnis hat dieselbe Segmentanzahl wie der Eingang (Split auf '/').
    Bei Abweichung: Fehler loggen (Pfad, erwartete/tatsächliche Länge), NIE
    stillschweigend einen Elternpfad benutzen.
B3. Unit-Tests: /storage/emulated/0/MobileIDEProjects/MyApp → /sdcard/…/MyApp,
    /storage/emulated/0/a/b/c/d, /sdcard/… (unverändert), Pfad mit Leerzeichen.
    Assert: letztes Segment immer identisch.

══════════════════════════════════════════════════════════════════
PHASE C — FALLBACK-LOGIK & MULTI-DISTRO-AUFLÖSUNG (falls U1/U2)
══════════════════════════════════════════════════════════════════
C1. Fallback NIE auf Elternordner. Stattdessen:
    a) File(sandboxPath).exists()==false → Log "PROJECT_DIR_MISSING host=…
       sandbox=… distro=$MOBILEIDE_DISTRO",
    b) Projekt-Erzeugung prüfen (MyApp fehlt/leer → Phase D),
    c) Klare Nutzermeldung statt "no Gradle build".
C2. MOBILEIDE_PROJECT_DIR MUSS auf den VOLLEN Projektpfad zeigen
    (…/MobileIDEProjects/MyApp), aufgelöst in der AKTIVEN Distro. Prüfe
    alle Setter (setup.sh, init.sh, DistroManager.kt, CodeEditScreen.kt).
    Falls die aktive Distro ubuntu ist, MUSS der Pfad innerhalb des
    ubuntu-Namensraums liegen; alpine-Pfade verwerfen.
C3. Projektpfad-Konstruktion (Root + Name) NUR mit "/" verbinden, nie mit
    Leerzeichen.

══════════════════════════════════════════════════════════════════
PHASE D — PROJEKT-ERZEUGUNG REPARIEREN (falls U2)
══════════════════════════════════════════════════════════════════
D1. NewProjectScreen.kt / Template-Extraktion prüfen: Warum existiert MyApp
    nicht oder ohne settings.gradle.kts/build.gradle.kts/app/? Template-Assets
    im APK? Extraktionspfad korrekt? Fehler geschluckt?
D2. Fix + Verifikation: Nach "Neues Projekt MyApp" existieren
    …/MyApp/settings.gradle.kts, build.gradle.kts, gradle.properties
    (mit aapt2Override) und app/.

══════════════════════════════════════════════════════════════════
PHASE E — GRADLE-INVOCATION FIXEN (falls U5) + DISTRO-BIND
══════════════════════════════════════════════════════════════════
E1. In ApkBuilder/CodeEditScreen sicherstellen:
    a) workingDirectory des PRoot-Prozesses == übersetzter Projektpfad (…/MyApp),
    b) Gradle-Aufruf mit vollem Pfad: bash ./gradlew (kein -p auf den Root).
E2. DistroManager.buildProotCommand: Bindungen absichern (in der AKTIVEN
    Distro, ubuntu):
       -b /storage/emulated/0:/sdcard
       -b /storage/emulated/0:/storage/emulated/0
    (zusätzlich -b /sdcard:/sdcard prüfen). Alpine-Bindings NICHT erweitern —
    alpine ist irrelevant, wenn ubuntu installiert ist. PRoot
    stat/vmstat-Sanitize-Warnungen (PROGRESS.md) dürfen durch neue Bindings
    NICHT wieder auftreten.

══════════════════════════════════════════════════════════════════
PHASE F — LEERZEICHEN-CHECK (falls U4)
══════════════════════════════════════════════════════════════════
F1. Jede Pfad-Konstruktion auf "$root $name"-Muster prüfen und korrigieren.
F2. Sicherheitsnetz: Pfad trimmen und prüfen, dass kein Leerzeichen direkt nach
    "/storage/emulated/0/" oder "/sdcard/" steht, bevor er an Gradle geht.

══════════════════════════════════════════════════════════════════
PHASE G — KOTLIN-LSP-KONSISTENZ (kotlin.sh vs. kmp-lsp-installer.sh)
══════════════════════════════════════════════════════════════════
G1. VERGLEICH DOKUMENTIEREN:
    - Öffne local/bin/lsp/kotlin.sh (built-in :features:lsp) und NOTIERE:
      welche Quelle (GitHub-Release), welches Ziel (/opt/kotlin-language-server
      oder /usr/local/bin), welcher Server-Name.
    - Öffne xed-kmp-lsp/app/src/main/assets/kmp-lsp-installer.sh und NOTIERE:
      Ziel $HOME/.lsp/kmp-lsp/kmp-lsp + kmp-jar-indexer, Quelle
      github.com/Hessesian/kmp-lsp, Version v0.24.0.
    - Öffne xed-kotlin/app/src/main/assets/kotlin-lsp-installer.sh: Ziel
      $HOME/.lsp/kotlin/bin/intellij-server (JetBrains, 262.8190.0).
    - Ergebnis-Tabelle in build/kotlin-lsp-consistency.tsv:
      Quelle | Skript | Zielverzeichnis | Binary | Version | Endungen |
      Registrierung (LspRegistry-Id) | Konflikt?
G2. KONSISTENZ-ENTSCHEIDUNG TREFFEN:
    - Bestätige: kotlin.sh und kmp-lsp-installer.sh sind BEWUSST getrennte,
      nicht gekoppelte Pfade (unterschiedliche Server-Projekte). kotlin.sh
      braucht KEINE Kenntnis des KMP-Installers, solange sichergestellt ist,
      dass NICHT beide gleichzeitig dieselbe Datei-Endung (.kt/.java) in
      LspRegistry registrieren.
    - Prüfe in der App, ob kmp-lsp (falls als Plugin installiert) eine
      DUPLIKAT-REGISTRIERUNG für .kt/.java gegenüber :features:lsp erzeugt.
      Falls ja: KMP-Plugin auf .swift + Indexing beschränken ODER aus dem
      Store/Install-Katalog ausschließen ODER Registrierungs-Dedup im
      LspRegistry einbauen (höchste Priorität: KEIN doppelter "LSP Fehler"-
      Status wie in Prompt 31 Schritt A).
    - LspSettingsScreen (nur 4 built-in Server Java/Kotlin/Bash/XML) um eine
      Sichtbarkeits-/Konfliktmeldung ergänzen, falls ein externer Kotlin-
      Server (kmp-lsp oder intellij-server) installiert wird: Warnung statt
      stillem Konflikt.
G3. VERIFIKATION: Kotlin-Datei öffnen → genau EIN LSP-Prozess aktiv, Status
    grün; java-Datei (nur falls KMP aktiv) → kein Doppel-Start; check_lsp_
    status.sh meldet kotlin OK.

══════════════════════════════════════════════════════════════════
PHASE H — VALIDIERUNG & BERICHT (Multi-Distro inklusive)
══════════════════════════════════════════════════════════════════
H1. ./gradlew assembleDebug → BUILD SUCCESSFUL (auf dem Host).
H2. Auf dem Gerät, mit Ubuntu als aktiver Distro (MOBILEIDE_DISTRO=ubuntu):
    a) Neues Projekt MyApp anlegen, Play-Button → KEIN "Projektverzeichnis
       ungültig", KEIN "Directory ... does not contain a Gradle build",
    b) Gradle läuft in …/MobileIDEProjects/MyApp (Ubuntu-Sandbox), APK wird
       gebaut und als installierbares APK gefunden (Host-Pfad-Übersetzung),
    c) Logcat: PROJECT_DIR_EXISTS host=… sandbox=… distro=ubuntu MyApp=true,
    d) Regression: bestehendes Projekt unter /sdcard/… und unter
       /storage/emulated/0/… jeweils einmal bauen.
H3. ALPINE-REGEL: Kein Test, kein Fix für alpine. Einziger Berichtspunkt:
       "alpine: übersprungen — ubuntu installiert/aktiv".
H4. Bericht build/apkbuilder-pathfix-report.md mit:
    - Befund Phase A (U1–U5 + aktive Distro),
    - Tabelle: Datei | Änderung | Test,
    - Phase-G-Tabelle (Kotlin-LSP-Konsistenz) mit Entscheidung,
    - Log-Auszüge vorher/nachher,
    - Commits:
      "fix(apk-builder): pass full project path incl. project name to Gradle;
      resolve in active distro (ubuntu); remove parent-fallback"
      "fix(lsp): document kotlin.sh vs kmp-lsp-installer.sh separation; prevent
      duplicate Kotlin LSP registration"

══════════════════════════════════════════════════════════════════
ABBRUCH-REGEL:
- Phase A muss eine eindeutige Ursache (U1–U5) liefern. Mehrdeutig → NICHT
  raten; Diagnose-Befehle erneut ausführen, Ausgaben im Bericht festhalten.
- alpine darf in keiner Phase blockieren (keine Abbruch-Regel basierend auf
  alpine).
