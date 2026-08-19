Erweitere die MobileIDE (/data/data/com.termux/files/home/MobileIDE) um vier
Funktionen und migriere anschließend ~/MaterialKolorBuilder als ThemeBuilder
in das Projekt. Jede Phase endet mit Build-Validierung, Unit-Tests, Commit UND
Push. Keine Phase endet ohne Push.

══════════════════════════════════════════════════════════════════
1. ZIELZUSTAND (Definition of Done)
══════════════════════════════════════════════════════════════════
1.1 TerminalSettingsScreen (app/.../ui/settings/SettingsScreen.kt):
    - KEINE hartcodierten Distro-Namen mehr in den Beschreibungstexten
      (aktuell wird „Alpine-U"/„alpine" statisch angezeigt, obwohl der
      Benutzer per Settings die Distro wählen kann)
    - Texte und Beschreibungen zeigen dynamisch die AKTUELL gewählte Distro
      aus der Preference `selected_distro` bzw. der Enum
      `TerminalEnvironmentOption.entries` (ubuntu | alpine) an
    - „Terminal zurücksetzen" / „Terminal neu installieren" bleiben funktional
      unverändert, nur die Texte werden dynamisch
1.2 ProjektScreen („MobileIDE Projekte"):
    - Neuer Eintrag/Button „Repository klonen" (Clone-Dialog laut Attachment):
      Felder: Repository URL, Clone into (parent folder), Branch (optional),
      Checkboxen: „Custom git flags", „Use credentials (private repo)",
      Buttons: Abbrechen / Klonen
    - Neue „Repository-Konfiguration" (laut Attachment):
      E-Mail (Committer), Benutzername, Token (Kein Passwort),
      Authentifizierungsmethode: HTTPS (Standard) | SSH-Schlüssel,
      Button „Verbindung testen" (grüner Toggle-Status),
      Button „Abbrechen"
      Hinweistext: „Verwende für GitHub/Gitee ein Personal Access Token
      anstelle deines Anmeldepassworts."
    - Klonen läuft über das vorhandene Terminal-Backend:
      TermuxExec.launchInternalTerminal + LogRouter.classify() → INSTALL-Kanal,
      damit der Klon-Vorgang im INSTALL-Log-Tab sichtbar ist
1.3 Projekt-Templates:
    - Jedes neue Projekt wird nach der Erstellung für Git vorbereitet:
      `git init` + initiale .gitignore + optional initialer Commit
      (Standard-Branch nach MobileIDE-Konvention, NICHT blind `master`)
    - Vorhandene Templates (ProjektScreen „Neues Projekt") werden um den
      git-init-Schritt erweitert
1.4 Filetree:
    - Neues Tab „ThemeBuilder" neben den bestehenden Filetree-Tabs
    - Tipp auf das Tab öffnet den ThemeBuilder (siehe 1.5)
1.5 ThemeBuilder:
    - ~/MaterialKolorBuilder wird analysiert (INVENTAR zuerst, NICHT aus dem
      Gedächtnis) und als ThemeBuilder in MobileIDE migriert/refaktoriert
    - Ziel: com.scto.mobile.ide-Package-Struktur, Integration in den
      Filetree-Tab „ThemeBuilder", Anbindung an das bestehende Theme-System
      (ToolingBottomSheet-Logik optional: ThemeBuilder-Ausgabe → LogRouter)
    - Altes ~/MaterialKolorBuilder bleibt als Quelle unangetastet

══════════════════════════════════════════════════════════════════
2. VERIFIZIERTE IST-ZUSTÄNDE (aus Reports, NICHT raten)
══════════════════════════════════════════════════════════════════
- Root MobileIDE: /data/data/com.termux/files/home/MobileIDE
- Root Xed-Editor (Quelle): /data/data/com.termux/files/home/Xed-Editor
- Toolchain: AGP 8.13.1, Kotlin 2.3.0, compileSdk 36 (Entscheidung aus
  Xed-Migration Phase 2: stabile MobileIDE-Toolchain)
- Baseline-Tag: xed-migration-baseline-20260817, Branch pre-xed-migration
- Distro-SSOT: TerminalEnvironmentOption.entries in
  features/terminal/.../ui/components/TerminalEnvironmentSelector.kt
  (Werte: ubuntu, alpine); Distro-Liste NICHT mehr hartcodiert in
  SettingsScreen.kt (Konsolidierung bdf76e1d)
- Terminal-Settings-Screen: app/src/main/java/com/scto/mobile/ide/ui/
  settings/SettingsScreen.kt mit Aktionen „Terminal zurücksetzen" und
  „Terminal neu installieren" sowie Statuszeile
  „Terminal Installed: Ja (is_terminal_installed = true)"
- Terminal-Launcher: TermuxExec.launchInternalTerminal in
  core/main/.../core/terminal/termux_exec/TermuxExec.kt, integriert mit
  LogRouter.classify() (5 Kerntabs INSTALL/BUILD/LSP/DIAGNOSE/IDE_LOGS,
  Commit d24c7842)
- ProjectPaths.kt ist SSOT für Pfadübersetzung (Commit c9561d5b)
- Reconciliation-Guard: scripts/reconcile_modules.sh (Exit 0 Pflicht)
- Build-Nachweis je Phase: ./gradlew assembleDebug
  :core:apk-builder:testDebugUnitTest :core:tooling:tooling-api:test
- ~/MaterialKolorBuilder: NICHT verifiziert → Phase 5 beginnt mit Inventar

══════════════════════════════════════════════════════════════════
3. PHASENPLAN (jede Phase: Checkpoint mit Build + Tests + Commit + Push)
══════════════════════════════════════════════════════════════════
PHASE 1 – BASELINE-CHECK (read-only)
1.1 git -C /MobileIDE status --porcelain; branch -vv; log --oneline -10
1.2 Ist-Analyse der betroffenen Dateien:
    - SettingsScreen.kt (Terminal-Settings-Abschnitt, hartcodierte Texte
      suchen: grep -rn "Alpine\|alpine" app/src/main/java/.../ui/settings/)
    - ProjektScreen/ProjectsScreen („MobileIDE Projekte", „Neues Projekt",
      Dateiname im Repo ermitteln, NICHT raten)
    - Projekt-Template-Erzeugung (Neues-Projekt-Flow, Template-Quellen)
    - Filetree (Tabs/Registerkarten-Struktur)
    - Vorhandenes Git-Modul/Features in MobileIDE (features:git?),
      git-Ausführung über Terminal/ProcessBuilder
1.3 Ergebnis-Datei build/feature-expansion-inventory.md anlegen
1.4 commit+push „docs(features): inventory for terminal texts, clone, git init,
    themebuilder"

PHASE 2 – TERMINAL-SETTINGS-TEXTE DYNAMISIEREN
2.1 In SettingsScreen.kt alle hartcodierten Distro-Erwähnungen in den
    Beschreibungstexten identifizieren („Alpine-U", „alpine", „Ubuntu")
2.2 Textbausteine aus der aktuell gespeicherten Preference `selected_distro`
    bzw. aus TerminalEnvironmentOption.entries ableiten:
    - Fallback bei unbekanntem Wert: alpine (MobileIDE-Konvention)
    - Beschreibung „Terminal neu installieren": dynamisch
      „…erstellt die <distro>-Umgebung neu (dauert ca. eine Minute)"
    - Statuszeile „Terminal Installed" bleibt unverändert funktional
2.3 KEINE Änderung der Aktionen selbst (Reset/Reinstall-Logik bleibt)
2.4 Build: ./gradlew assembleDebug → grün
2.5 commit+push „fix(settings): make terminal distro texts dynamic from
    selected_distro preference"

PHASE 3 – PROJEKTSCREEN: CLONE + REPOSITORY-KONFIGURATION
3.1 Neuer UI-Einstieg im ProjektScreen: Button/Eintrag „Repository klonen"
    (laut Attachment-Dialog)
3.2 Clone-Dialog implementieren:
    - Repository URL (Pflichtfeld, Validierung https://, git@, ssh://)
    - Clone into (parent folder) → Standard: ProjectPaths-Projektroot,
      über ProjectPaths-SSOT auflösen (c9561d5b), NICHT hartkodieren
    - Branch (optional)
    - Checkbox „Custom git flags" (erweitertes Flag-Feld)
    - Checkbox „Use credentials (private repo)" → öffnet Repository-Konfiguration
    - Buttons: Abbrechen | Klonen (Klone-Button hervorgehoben)
3.3 Repository-Konfiguration (laut Attachment):
    - E-Mail (Committer), Benutzername, Token (Kein Passwort)
    - Authentifizierungsmethode: HTTPS (Standard, grüner Haken) | SSH-Schlüssel
    - Hinweistext: „Verwende für GitHub/Gitee ein Personal Access Token
      anstelle deines Anmeldepassworts."
    - „Verbindung testen" → führt git-Credential-Check/Handshake aus,
      zeigt grünen Erfolgs-Toggle oder Fehlermeldung
    - Persistenz: Credentials NICHT im Klartext in SharedPreferences –
      Android Keystore/EncryptedSharedPreferences verwenden
3.4 Klon-Ausführung über vorhandenes Terminal-Backend:
    - TermuxExec.launchInternalTerminal (git clone …) aufrufen
    - LogRouter.classify() erfasst die Ausgabe im INSTALL-Kanal
      (Klon-Vorgang ist sichtbar im ToolingBottomSheet INSTALL-Tab)
    - Nach erfolgreichem Klon: Projekt erscheint in der Projektliste,
      Projekte-Liste neu laden
3.5 Credentials beim Klonen: https://<user>:<token>@<host>/<org>/<repo>.git
    temporär im Kommando (NICHT dauerhaft in Logs/History – Token-Maskierung
    im INSTALL-Log, falls nötig)
3.6 Unit-Tests:
    - URL-Validierung (https/ssh/git@, Fehlerfälle)
    - Credential-String-Aufbau (Token-Maskierung, keine Plaintext-Persistenz)
    - Branch-Optional-Logik
3.7 Build + Tests grün → commit+push
    „feat(projects): add clone repository dialog with credential config and
    install-channel logging"

PHASE 4 – TEMPLATES: GIT-INIT BEI PROJEKTERSTELLUNG
4.1 Template-Erzeugungsflow erweitern: Nach Anlage der Projektdateien wird
    automatisch ausgeführt:
    - git init (Branch-Konvention aus MobileIDE-Konfiguration, NICHT blind
      master/main raten – vorhandene Konvention im Repo prüfen)
    - .gitignore mit projektspezifischen Einträgen (Bau-Ordner je
      Template-Typ: .gradle/build für Android, node_modules für JS/TS …)
    - KEIN Zwangs-Commit; optional initialer Commit nur wenn konfiguriert
4.2 Status-Feedback im Neues-Projekt-Flow (Fortschritt/Fehler sichtbar)
4.3 Verhalten bei vorhandenem .git: nicht überschreiben, Hinweis loggen
4.4 Unit-Tests: .gitignore-Generierung je Template-Typ
4.5 Build + Tests grün → commit+push
    „feat(templates): prepare new projects with git init and gitignore"

PHASE 5 – FILETREE: TAB „THEMEBUILDER"
5.1 Filetree-Tab-Struktur prüfen (Datei + Tab-Definitionen im Repo finden)
5.2 Neues Tab „ThemeBuilder" mit Icon hinzufügen
5.3 Tab öffnet den ThemeBuilder-Screen (Phase 6), Navigation über
    bestehende App-Navigation (BackHandler-Kette beachten: ToolingBottomSheet
    → … → Drawer → Screen, gemäß terminal-settings-back-fix)
5.4 Tab ist nur aktiv, wenn ThemeBuilder-Modul verfügbar ist
5.5 Build + Tests grün → commit+push
    „feat(filetree): add ThemeBuilder tab entry"

PHASE 6 – ~/MATERIALKOLORBUILDER → THEMEBUILDER-MIGRATION
6.1 INVENTAR (Pflicht, NICHT aus dem Gedächtnis):
    - test -d ~/MaterialKolorBuilder; git status/log; Dateibaum
      (find ~/MaterialKolorBuilder -type f | wc -l, Struktur aufzeichnen)
    - Sprache/Framework, Build-System, Abhängigkeiten, Einstiegspunkte,
      Verwendung von Farb-/Theme-Funktionen (Material You / Monet?)
    - Ergebnis: build/materialkolor-inventory.md
6.2 Migrations-Entscheidung dokumentieren (in Report):
    a) 1:1-Code-Übernahme in MobileIDE-Modul (Package com.scto.mobile.ide.*)
       ODER
    b) Neuimplementierung der Kernfunktionen in MobileIDE-Architektur
       (Compose, com.scto.mobile.ide.ui.themebuilder) unter Beibehaltung der
       Farb-Logik von MaterialKolorBuilder
    Begründung im Report (Abhängigkeiten, Framework-Kompatibilität)
6.3 Integration:
    - ThemeBuilder-Screen (com.scto.mobile.ide.ui.themebuilder)
    - Anbindung an Filetree-Tab „ThemeBuilder" (Phase 5)
    - Erzeugte Themes werden über das bestehende Theme-System gespeichert
      (Konventionen im Repo prüfen: ThemeManager/themeDir – aus Xed-Dump
      bekannt: FileOperations.themeDir)
6.4 Test: mindestens ein Unit-Test für die Farbberechnung (Material-You-
    Kernlogik, falls extrahierbar)
6.5 Build + Tests grün → commit+push
    „feat(themebuilder): migrate MaterialKolorBuilder as ThemeBuilder into
    MobileIDE with filetree tab integration"

PHASE 7 – FINALE VALIDIERUNG
7.1 ./gradlew clean assembleDebug
    :core:apk-builder:testDebugUnitTest :core:tooling:tooling-api:test
7.2 bash scripts/reconcile_modules.sh → Exit 0 (Assets/Distros/Module
    unverändert konsolidiert)
7.3 On-Device-Smoke:
    a) Terminal-Settings: Texte zeigen aktuelle Distro (ubuntu/alpine
       wechseln → Texte ändern sich)
    b) ProjektScreen: „Repository klonen" öffnet Dialog; Klon eines
       öffentlichen GitHub-Repos → Fortschritt im INSTALL-Tab sichtbar;
       Projekt erscheint in Liste
    c) Repository-Konfiguration: HTTPS-Test, ungültiges Token → Fehler,
       gültiges Token → grüner Status
    d) „Neues Projekt": .git + .gitignore werden angelegt
    e) Filetree: Tab „ThemeBuilder" sichtbar; öffnet ThemeBuilder; erzeugtes
       Theme wird angewandt/gespeichert
    f) Bestehende Terminal-/Log-/BackHandler-Funktionen unverändert
7.4 Report build/feature-expansion-report.md final (Entscheidungen,
    Testzahlen, PENDING)
7.5 Finaler Commit+Push:
    „feat: complete clone, dynamic distro texts, git-init templates,
    ThemeBuilder integration"

══════════════════════════════════════════════════════════════════
4. ABBRUCH-REGELN & FALLBACKS
══════════════════════════════════════════════════════════════════
- Nach JEDER Phase: Build/Tests NICHT grün → STOPP, Ursache beheben ODER
  git reset --hard <letzter grüner Commit> + Fallback dokumentieren. Nie eine
  rote Phase committen/pushen.
- Bestehende verifizierte Funktionen dürfen NICHT regressieren:
  Terminal (SessionService, Drawer, Desktop-Mode, ubuntu/alpine),
  Log-System (5 Kerntabs, LogRouter), APK-Builder, ProjectPaths-SSOT,
  reconcile_modules.sh.
- Credentials niemals im Klartext persistieren; Token in Logs maskieren.
- ~/MaterialKolorBuilder wird NIE verändert (nur gelesen/kopiert).
- Falls ~/MaterialKolorBuilder nicht existiert → Phase 6 stoppt, PENDING,
  ThemeBuilder-Tab wird deaktiviert, Report-Eintrag, Rest läuft weiter.
- Templates: bestehende .git-Verzeichnisse nie überschreiben.

══════════════════════════════════════════════════════════════════
5. GIT-DISZIPLIN
══════════════════════════════════════════════════════════════════
- Nach jedem Checkpoint: git add -A; git commit -m "<Message>";
  git push origin <aktueller-branch>; git log --oneline -3 (Verifikation)
- Jede Phase: EIN Code-Commit + optional EIN Doku-Commit
- Commit-Messages exakt wie in den Phasen vorgegeben
- Zwischenstände werden nie gepusht
