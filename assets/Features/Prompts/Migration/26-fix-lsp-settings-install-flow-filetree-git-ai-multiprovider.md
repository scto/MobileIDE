# Ziel
Behebe sechs voneinander unabhängige, aber im selben Arbeitsgang zu bearbeitende 
Probleme in MobileIDE (com.scto.mobile.ide): doppelte Sprachserver-Settings-Gruppe, 
abstürzender LSP-Installationsdialog, funktionsloser Terminal-Install-Toast, 
fehlender "Versteckte Dateien anzeigen"-Schalter im Filetree, komplett fehlende 
Git-Tab-Funktionalität im Filetree, sowie die Erweiterung des KI-Assistenten von 
einer reinen Aider-Anbindung zu einem Multi-Provider-Overlay.

# Kontext (siehe Screenshot-Anhang: LspSettingsScreen mit Bash/Emmet/Java/Kotlin/
XML/Python/TypeScript/CSS/ESLint/HTML/Markdown-Sprachservern, jeweils mit Install-/
Reload-/Toggle-Icons und "+ Externer LSP"-Button)

## Zu Punkt 1 (Doppelte Sprachserver-Gruppe)
Die im Screenshot gezeigte "Sprachserver"-Einstellungsgruppe (Einleitungstext 
"Sprachserver sind separate Prozesse, die intelligente Funktionen wie Code-
Vervollständigung, Fehlerhervorhebung und Inline-Dokumentation bereitstellen." + 
Liste der 11 Sprachserver) existiert laut Nutzerangabe BEREITS als eigenständiger 
Bereich direkt im Editor (vermutlich als Teil der EditorScreen-eigenen Settings, wo 
laut bekanntem Stand bereits ein LSP-Editor-Toggle mit TreeSitter/TextMate-Fallback-
Logik existiert). Diese Redundanz zwischen dem globalen `LspSettingsScreen` und den 
Editor-internen Settings muss aufgelöst werden – EINE der beiden Stellen ist die 
korrekte "Single Source of Truth", die andere ist zu entfernen bzw. zu einem Verweis 
zu vereinfachen.

## Zu Punkt 2 (LSP-Installationsdialog & Terminal-Install-Toast)
2.1. Beim Öffnen einer Kotlin-Quelldatei im Editor erscheint korrekt ein Dialog zur 
   Installation der Kotlin-LSP-Extension/Plugin. ABER: das Bestätigen dieses Dialogs 
   führt aktuell zum SOFORTIGEN ABSTURZ/SCHLIESSEN der gesamten App, ohne dass 
   irgendetwas installiert wird. Dies deutet auf einen unbehandelten Exception-Pfad 
   im Installations-Trigger hin (z. B. NullPointerException durch fehlenden Context, 
   fehlende Berechtigung, oder einen fehlerhaften Intent/Prozess-Start-Aufruf).

2.2. Zusätzlich erscheint am unteren Editor-Rand ein Toast, der fragt, ob das 
   zugehörige Bash-Installations-Skript für den jeweiligen Sprachserver 
   (Nutzer-Bezeichnung "Speachserver" – gemeint ist vermutlich der Installations-
   Vorgang des jeweiligen Sprachserver-Skripts innerhalb der Terminal-Sandbox-
   Infrastruktur) ausgeführt werden soll. Das Bestätigen dieses Toasts ÖFFNET 
   AKTUELL NUR DAS TERMINAL, FÜHRT ABER KEIN SKRIPT AUS. Erwartet wird, dass die 
   Bestätigung tatsächlich das zugehörige Installations-Skript (analog zu den 
   bereits für Terminal-Sandbox-Setup vorhandenen Skripten wie `idesetup`/
   `init.sh`/`setup.sh`) im Terminal-Kontext ausführt, nicht nur das Terminal öffnet.

## Zu Punkt 3 (Filetree-Lücken)
3.1. Im Filetree-Bereich, Tab "Dateien" (Files-Tab), fehlt eine Einstellungsoption 
   zum Ein-/Ausblenden versteckter Dateien (Dateien/Ordner mit führendem Punkt, 
   z. B. `.git`, `.gradle`).

3.2. Im Filetree-Bereich, Tab "Git" (Git-Tab), fehlt JEGLICHE Funktionalität – dies 
   deutet stark darauf hin, dass dieser Tab bislang nur als leere UI-Hülle existiert, 
   während die tatsächliche Git-Funktionalität (Status, Commit, Push, Pull, 
   Credentials-Verwaltung, Konfliktlösung) andernorts im Code bereits implementiert, 
   aber NICHT an diesen konkreten Filetree-Git-Tab angebunden ist (siehe Bezug zu 
   vorherigem Auftrag: reaktiviertes/sichtbar gemachtes `:features:git`-Modul mit 
   GitPanel/GitViewModel/GitConfigDialog/GitConflictResolutionDialog).

## Zu Punkt 4 (KI-Assistent Multi-Provider)
Der aktuelle KI-Assistent ist als Overlay implementiert, unterstützt aber laut 
Nutzerangabe FAKTISCH NUR Aider als einzigen Anbieter, obwohl das Overlay-Design 
augenscheinlich für diverse verschiedene KI-Anbieter ausgelegt sein soll/könnte. 
Die vorhandene Aider-spezifische Logik (Prozess-Start, Kommunikation, Ergebnis-
Darstellung im Overlay) MUSS zu EINEM von mehreren austauschbaren Providern innerhalb 
einer neuen, generischen Provider-Abstraktion migriert werden, die um weitere 
Anbieter erweiterbar ist.

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER. Nach jeder Stufe: Gesamtprojekt bauen 
(Gradle), Fehlerfreiheit bestätigen, Ergebnis-Report mit geänderten Dateien liefern, 
bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme (Pflicht vor jeder Code-Änderung)

0.1. Lokalisiere BEIDE Stellen, an denen Sprachserver-Einstellungen aktuell 
   angezeigt werden: den globalen `LspSettingsScreen` (siehe Screenshot) sowie die 
   Editor-interne Settings-Stelle, auf die sich der Nutzer bezieht. Dokumentiere den 
   exakten Dateipfad beider Implementierungen und liste auf, welche Funktionalität 
   (Install/Reload/Toggle/Externer-LSP-Button) an welcher Stelle vorhanden ist bzw. 
   fehlt, um zu entscheiden, welche Stelle konsolidiert/entfernt wird.

0.2. Lokalisiere den Installationsdialog-Trigger, der beim Öffnen einer Kotlin-Datei 
   erscheint (vermutlich in `CodeEditScreen.kt` oder einem zugehörigen ViewModel, 
   ausgelöst durch Datei-Endungs-Erkennung). Reproduziere den Absturz-Pfad durch 
   Log-/Stacktrace-Analyse (z. B. über LogCatcher/LogEntry-Infrastruktur, sofern 
   vorhanden) und identifiziere die exakte Exception-Ursache.

0.3. Lokalisiere den Toast-Trigger für die Bash-Skript-Installationsbestätigung 
   sowie den Code-Pfad, der aktuell NUR das Terminal öffnet. Identifiziere, welches 
   konkrete Skript (Pfad innerhalb der Terminal-Sandbox-Assets) tatsächlich 
   ausgeführt werden SOLLTE, und warum der aktuelle Code nur bis zum Öffnen des 
   Terminals kommt, ohne den Skript-Ausführungsbefehl an die Terminal-Session zu 
   übergeben.

0.4. Lokalisiere die Filetree-Implementierung (Tabs "Dateien" und "Git") und 
   dokumentiere: Existiert bereits eine Versteckte-Dateien-Filterlogik im 
   zugrundeliegenden FileTree-ViewModel, die nur nicht in der UI exponiert ist, oder 
   fehlt sie komplett? Ist der Git-Tab eine reine Platzhalter-Compose-Funktion ohne 
   jegliche Anbindung an GitViewModel/GitPanel, oder ruft er bereits (fehlerhaft) 
   Teile davon auf?

0.5. Lokalisiere die aktuelle KI-Assistent-Overlay-Implementierung und die darin 
   enthaltene Aider-spezifische Logik (Prozess-Start-Befehl, Konfigurationsdatei-
   Zugriff, Response-Parsing, UI-Rendering der Antworten). Dokumentiere den exakten 
   Umfang der Aider-Kopplung, um sie in Stufe 5 korrekt zu abstrahieren.

0.6. Liefere diese Bestandsaufnahme als eigenständigen Zwischen-Report, BEVOR mit 
   Stufe 1 fortgefahren wird.

---

## STUFE 1 – Doppelte Sprachserver-Settings-Gruppe entfernen

1.1. Basierend auf Stufe 0.1: entferne die redundante Sprachserver-Settings-Gruppe 
   an EINER der beiden Stellen. Bevorzugt zu entfernen ist der eigenständige, global 
   erreichbare `LspSettingsScreen` (siehe Screenshot) NUR DANN, wenn die Editor-
   interne Stelle bereits denselben Funktionsumfang (Install/Reload/Enable-Toggle 
   pro Sprachserver, "+ Externer LSP"-Button) bietet. Falls die Editor-interne 
   Stelle funktional ärmer ist: erweitere stattdessen DIESE um die fehlende 
   Funktionalität und entferne dann den globalen Screen, statt Funktionalität 
   ersatzlos zu verlieren.

1.2. Entferne alle NavHost-Routen-Registrierungen und UI-Einstiegspunkte 
   (Navigations-Buttons/Menüeinträge), die auf den entfernten Screen verwiesen 
   haben, unter Verwendung von `NavigationUtils.safeNavigate`-Konventionen für 
   verbleibende Referenzen.

1.3. Build- & Funktionsverifikation: Sprachserver-Einstellungen sind nur noch an 
   EINER Stelle im Editor sichtbar, mit vollem Funktionsumfang (alle 11 Sprachserver 
   + "Externer LSP"-Button funktionieren weiterhin).

---

## STUFE 2 – LSP-Installationsdialog-Absturz beheben

2.1. Behebe die in Stufe 0.2 identifizierte Absturzursache (z. B. fehlende Context-
   Referenz, unbehandelte Exception beim Download/Extraktions-Vorgang des Kotlin-
   Sprachservers, fehlerhafter Intent).

2.2. Stelle sicher, dass die Bestätigung des Installationsdialogs tatsächlich den 
   vollständigen Installationsvorgang anstößt (Download, Extraktion, Registrierung 
   in der Sprachserver-Liste), MIT sichtbarem Fortschrittsindikator (Progress-Icon, 
   analog zum bereits im Screenshot sichtbaren Reload-/Install-Icon-Muster), und 
   MIT korrekter Fehlerbehandlung (Fehlermeldung statt App-Absturz, falls Download 
   fehlschlägt, z. B. wegen fehlender Internetverbindung).

2.3. Build- & Funktionsverifikation: Öffne eine Kotlin-Datei in einem Testprojekt 
   ohne installierten Kotlin-Sprachserver, bestätige den Installationsdialog, 
   verifiziere, dass die App NICHT abstürzt, die Installation tatsächlich 
   durchgeführt wird, und der Sprachserver anschließend in der Settings-Liste als 
   "Installiert" markiert ist.

---

## STUFE 3 – Terminal-Install-Toast: tatsächliche Skript-Ausführung

3.1. Behebe den in Stufe 0.3 identifizierten Code-Pfad so, dass die Bestätigung des 
   Toasts nicht nur das Terminal öffnet, sondern dem geöffneten Terminal-Prozess 
   direkt den Befehl zur Ausführung des zugehörigen Installations-Skripts übergibt 
   (Muster: analog zur bestehenden Sandbox-Setup-Ausführung via `idesetup`/`init.sh`/
   `setup.sh`, unter Beachtung des bekannten entkoppelten Callback-Musters von 
   `TerminalBackEnd.kt` zur Vermeidung zirkulärer Abhängigkeiten zwischen 
   `:core:main` und `:app`).

3.2. Stelle sicher, dass der Nutzer im Terminal den laufenden Installationsprozess 
   (stdout/stderr) live mitverfolgen kann, und dass nach erfolgreichem Abschluss der 
   zugehörige Sprachserver-Eintrag in der Settings-Liste automatisch als 
   "Installiert" aktualisiert wird (Event-basiertes Update, kein manuelles Neuladen 
   nötig).

3.3. Build- & Funktionsverifikation: Bestätigung des Toasts führt zu tatsächlicher, 
   sichtbarer Skript-Ausführung im Terminal mit anschließendem Status-Update.

---

## STUFE 4 – Filetree-Ergänzungen (Versteckte Dateien & Git-Tab-Funktionalität)

4a. Versteckte Dateien im "Dateien"-Tab:
   4a.1. Ergänze im Filetree-Header oder im zugehörigen Overflow-/Einstellungs-Menü 
      des "Dateien"-Tabs einen Umschalter "Versteckte Dateien anzeigen" (Ein/Aus), 
      persistiert in den Nutzereinstellungen (DataStore/SharedPreferences, analog 
      zu bestehenden Persistierungs-Mustern).
   4a.2. Verdrahte diesen Umschalter mit der Filter-Logik des FileTree-ViewModels, 
      sodass Dateien/Ordner mit führendem Punkt (`.git`, `.gradle`, `.idea` etc.) je 
      nach Schalterstellung ein- oder ausgeblendet werden.
   4a.3. Build- & Funktionsverifikation: Umschalten des Schalters zeigt/verbirgt 
      versteckte Dateien im Filetree sofort, ohne Neustart der App.

4b. Git-Tab-Funktionalität:
   4b.1. Verdrahte den bislang funktionslosen Git-Tab im Filetree tatsächlich mit 
      dem bereits vorhandenen `GitViewModel`/`GitPanel` (bzw. dem in einem 
      vorherigen Auftrag reaktivierten `:features:git`-Modul). Der Tab soll 
      mindestens zeigen: aktueller Branch, Liste geänderter/neuer/gelöschter 
      Dateien mit Status-Icons, Zugriff auf Commit/Push/Pull-Aktionen, sowie einen 
      Einstiegspunkt zum bestehenden `GitConfigDialog` (Credentials) und zur 
      bestehenden `GitConflictResolutionDialog` (bei Konflikten).
   4b.2. Falls das zugrundeliegende Git-Modul (siehe vorheriger Auftrag zu 
      `:features:git`) noch nicht reaktiviert/eingebunden ist, stelle dies als 
      Voraussetzung fest und reaktiviere es analog zum bekannten Vorgehen 
      (Entfernen der Auskommentierung in `settings.gradle.kts`, Dependency-
      Einbindung in `app/build.gradle.kts`).
   4b.3. Build- & Funktionsverifikation: Git-Tab im Filetree zeigt für ein Test-
      Repository korrekten Status an, Commit/Push/Pull funktionieren aus diesem Tab 
      heraus.

---

## STUFE 5 – KI-Assistent zu Multi-Provider-Overlay erweitern

5.1. Definiere eine generische `AiProvider`-Abstraktion (Interface/abstrakte Basis-
   klasse) mit mindestens: `id`, `displayName`, `isConfigured()`, `isAvailable()` 
   (z. B. Prüfung ob CLI-Tool/API-Key vorhanden), `sendPrompt(context, prompt): Flow<
   AiResponseChunk>` (streaming-fähig, damit Antworten wie bisher im Overlay 
   inkrementell dargestellt werden können), sowie einen Konfigurations-Screen-
   Verweis pro Provider (API-Key/Modell-Auswahl/CLI-Pfad, je nach Provider-Typ).

5.2. Migriere die bestehende Aider-spezifische Logik (Prozess-Start, Kommunikation, 
   Response-Parsing) VOLLSTÄNDIG in eine konkrete `AiderProvider`-Implementierung 
   dieser neuen Abstraktion, OHNE bestehende Aider-Funktionalität zu verändern oder 
   zu verschlechtern – reine Kapselung/Refaktorierung, keine Verhaltensänderung.

5.3. Ergänze das KI-Overlay-UI um eine Provider-Auswahl (z. B. Dropdown/Chip-Auswahl 
   am oberen Rand des Overlays, analog zum bestehenden Tab-Muster im 
   ToolingBottomSheet), die zwischen den registrierten Providern umschalten lässt, 
   inkl. visueller Kennzeichnung, welcher Provider aktuell konfiguriert/verfügbar 
   ist (Status-Badge analog zu "Nicht installiert"/"Installiert" im LSP-Screen).

5.4. Implementiere mindestens EINEN weiteren konkreten Provider zusätzlich zu Aider 
   (z. B. einen generischen `ApiKeyBasedProvider` für OpenAI-kompatible REST-APIs 
   mit konfigurierbarem Endpoint/Modellname/API-Key, damit die Architektur 
   nachweislich erweiterbar ist und nicht nur theoretisch abstrahiert wurde), inkl. 
   zugehörigem Konfigurations-Settings-Screen (Route "settings/ai/<provider-id>").

5.5. Registriere alle Provider zentral (analog zum `CommandProvider`-Registrierungs-
   muster aus `:core:commands`, sofern bereits reaktiviert) beim App-Start, sodass 
   das Overlay dynamisch alle registrierten Provider anzeigt, ohne hartkodierte 
   Provider-Liste in der UI-Schicht.

5.6. Build- & Funktionsverifikation: KI-Overlay zeigt mindestens zwei auswählbare 
   Provider (Aider + neuer Provider), Umschalten funktioniert, bestehende Aider-
   Funktionalität bleibt unverändert nutzbar, neuer Provider ist konfigurierbar und 
   sendet/empfängt nachweislich Prompts/Antworten.

---

## STUFE 6 – Abschlussverifikation

6.1. Vollständiger Gradle-Build aller betroffenen Module.

6.2. Manuelle Durchklick-Bestätigung aller sechs behobenen Punkte:
   - Nur noch eine Sprachserver-Settings-Stelle vorhanden.
   - Kotlin-LSP-Installationsdialog installiert tatsächlich, ohne Absturz.
   - Terminal-Install-Toast führt tatsächlich das zugehörige Skript aus.
   - "Versteckte Dateien anzeigen"-Schalter im Dateien-Tab funktioniert.
   - Git-Tab im Filetree zeigt echten Status und ermöglicht echte Git-Aktionen.
   - KI-Overlay bietet mehrere auswählbare, funktionsfähige Provider.

6.3. Liefere einen finalen Gesamt-Report mit allen geänderten/migrierten Dateien pro 
   Stufe.

# Nicht-Ziele
- Keine Neuerstellung der bereits vorhandenen Git-Kernkomponenten 
  (GitConfigDialog/GitViewModel/GitPanel/GitConflictResolutionDialog) – nur 
  Anbindung an den bislang funktionslosen Filetree-Git-Tab.
- Keine Verhaltensänderung der bestehenden Aider-Integration jenseits der reinen 
  Kapselung in die neue Provider-Abstraktion.
- Keine vollständige Neuimplementierung des LSP-Installations-Backends – nur 
  Behebung des konkreten Absturz-Bugs und der Terminal-Skript-Ausführungslücke.
- Keine Änderung an der grundsätzlichen Terminal-Session-Architektur jenseits der in 
  Stufe 3 beschriebenen Skript-Übergabe.

# Akzeptanzkriterien
- Sprachserver-Einstellungen existieren nur noch an einer Stelle, mit vollem 
  bisherigen Funktionsumfang.
- Kotlin-LSP-Installationsdialog führt zu erfolgreicher Installation ohne App-
  Absturz, mit korrektem Status-Update danach.
- Terminal-Install-Toast-Bestätigung führt zu tatsächlicher, sichtbarer Ausführung 
  des zugehörigen Installations-Skripts im Terminal.
- Filetree-"Dateien"-Tab bietet einen funktionierenden "Versteckte Dateien anzeigen"-
  Schalter.
- Filetree-"Git"-Tab zeigt echten Repository-Status und ermöglicht Commit/Push/Pull/
  Credentials-Verwaltung/Konfliktlösung.
- KI-Assistent-Overlay unterstützt nachweislich mehr als einen Anbieter (mindestens 
  Aider + einen weiteren), mit funktionierender Provider-Auswahl und -Konfiguration, 
  ohne bestehende Aider-Funktionalität zu beeinträchtigen.
- Gesamtprojekt-Build ist am Ende fehlerfrei.
