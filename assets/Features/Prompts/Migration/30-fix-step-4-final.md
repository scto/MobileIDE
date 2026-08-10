Stufe 4 – Zwei parallele Untersuchungsaufträge: (A) endgültiger Pfad-Fix mit
Root-Cause-Analyse über Submodul-Grenzen hinweg, UND (B) Grundsatzfrage zur
Notwendigkeit der separaten LSP-Installationsroute.

═══════════════════════════════════════════════════════════════
TEIL A – Terminal-Asset-Pfad endgültig korrigieren
═══════════════════════════════════════════════════════════════

BESTÄTIGTER FAKT (vom Nutzer verifiziert, nicht erneut in Frage stellen):
Der korrekte, tatsächlich existierende Pfad auf dem Gerät lautet:

    /data/data/com.scto.mobile.ide/local/bin/lsp

NICHT (wie im letzten - unvollständigen - Fix als Priorität angenommen):

    /data/data/com.scto.mobile.ide/files/local/bin/lsp

Der letzte Fix hat in LspRegistry.kt eine Fallback-Logik eingebaut, die
FÄLSCHLICH zuerst den "files/"-Pfad prüft und erst danach auf einen
/data/user/0/...-Pfad zurückfällt. Das ist falsch priorisiert und hat das
Problem nicht vollständig gelöst: Der Terminal-Start übergibt weiterhin
".../files/local/bin/lsp/kotlin.sh" an init-host.sh, was zu "No such file
or directory" (Exit-Code 127) führt. Der nachfolgende "chdir(\"/home\"):
No such file or directory"-Fehler ist eine FOLGE davon (PRoot-Session-Init
bricht ab, bevor /home gemountet wird), NICHT die eigentliche Ursache.

AUFGABE:

1. Durchsuche das GESAMTE Projekt modul-übergreifend (nicht nur
   LspRegistry.kt) nach JEDER Stelle, die einen Pfad zusammenbaut, der
   "local/bin" oder "local/bin/lsp" enthält, oder die "context.filesDir"
   in Kombination mit "local" verwendet. Prüfe insbesondere:
   - :core:lsp
   - :extension-languages
   - :features:terminal (init-host.sh, MkSession.kt, TerminalScreen.kt -
     die in der letzten Änderung genannten Dateien)
   - alle :plugins:*-lsp Module (kotlin-lsp, java-lsp, etc.)
   Zeige für JEDEN Fund Datei, Zeile und die exakte resultierende
   Pfad-Zusammensetzung.

2. Stelle für jeden Fund fest: Zeigt er auf den ECHTEN Installationsort der
   Skripte/Server-Binaries (ohne "files/"), oder auf den falschen Pfad
   MIT "files/"? Erstelle eine vollständige Liste "korrekt" vs. "fehlerhaft".

3. Definiere GENAU EINE zentrale, einzige Quelle der Wahrheit für diesen
   Pfad (z. B. eine einzelne Funktion/Property, etwa in einem gemeinsam
   nutzbaren Utility-Objekt, das von :core:lsp, :extension-languages und
   :features:terminal gleichermaßen importiert werden kann - prüfe, ob es
   bereits eine geeignete gemeinsame Abhängigkeitsbasis gibt, bevor du eine
   neue erstellst). Diese Funktion MUSS immer und ausschließlich auf
   "/data/data/<applicationId>/local/bin/lsp" (ohne "files/") auflösen,
   OHNE einen "files/"-Fallback oder eine "files/"-Priorität zu enthalten,
   AUSSER es wird durch einen tatsächlichen Dateisystem-Existenz-Check zur
   Laufzeit zweifelsfrei belegt, dass in einer bestimmten Android-Version/
   einem bestimmten Gerätetyp tatsächlich ein "files/"-Pfad korrekt wäre
   (falls unsicher: teste dies NICHT per Annahme, sondern lasse die
   Funktion zur Laufzeit denjenigen der zwei Pfade wählen, der laut
   File.exists() tatsächlich existiert, mit Log-Ausgabe, WELCHER Pfad
   gewählt wurde - aber ändere die Priorität so, dass der
   "files/"-freie Pfad ZUERST geprüft wird, da dieser laut Nutzerangabe
   der tatsächlich korrekte ist).

4. Ersetze ALLE in Schritt 1 gefundenen Einzelimplementierungen durch
   Aufrufe dieser einen zentralen Funktion. Entferne redundante,
   parallele Pfad-Konstruktionen vollständig (Ziel: exakt EIN Code-Pfad
   für diese Pfadauflösung, konsistent mit dem übergeordneten
   Konsolidierungsziel aus Prompt 30).

5. Prüfe zusätzlich init-host.sh und MkSession.kt konkret: Wird der
   Skriptpfad (kotlin.sh etc.) als Kommandozeilenargument an init-host.sh
   in einer Form übergeben, die PRoot-intern nochmals neu aufgelöst/
   gebunden werden muss (-b Binding), oder wird er als bereits
   Host-aufgelöster absoluter Pfad direkt an "sh" übergeben, der innerhalb
   der PRoot-Sandbox möglicherweise einen anderen Wurzelkontext hat?
   Kläre, ob das Skript VOR dem PRoot-Start (Host-Kontext) oder NACH
   PRoot-Start (Container-Kontext, wo evtl. andere Bind-Mounts gelten)
   aufgerufen werden soll, und ob das aktuell konsistent gehandhabt wird.

6. Baue, installiere, teste: Installieren-Klick für Kotlin-LSP muss ohne
   "No such file or directory"- und ohne "chdir(/home)"-Fehler laufen,
   Terminal zeigt echte Installationsausgabe, Exit-Code 0 am Ende.

═══════════════════════════════════════════════════════════════
TEIL B – Grundsätzliche Notwendigkeitsprüfung der separaten
LSP-Installationsroute (WICHTIG, VOR größerem Aufwand in Teil A klären)
═══════════════════════════════════════════════════════════════

HYPOTHESE DES NUTZERS (bitte ernsthaft prüfen, nicht ignorieren):
Möglicherweise ist diese gesamte separate "Installieren"-Dialog/Terminal-
Route für LSP-Server GAR NICHT NÖTIG, weil die einzelnen LSP-Plugin-Module
(:plugins:kotlin-lsp, :plugins:java-lsp, ggf. weitere) BEREITS EIGENSTÄNDIG
ihre benötigten Installer-Skripte aufrufen bzw. ihre Server-Binaries selbst
bei Bedarf herunterladen/entpacken (z. B. beim ersten Start einer Sprachdatei,
lazy-initialisiert). Falls das zutrifft, wäre die separate, fehleranfällige
Installations-UI (Dialog/Toast + Terminal-Screen) eine REDUNDANTE, PARALLELE
Implementierung derselben Funktionalität - exakt das Muster, das Prompt 30
bereits für :features:git und :features:runner identifiziert hat.

AUFGABE:

1. Untersuche JEDES :plugins:*-lsp Modul (insbesondere kotlin-lsp, java-lsp)
   und stelle fest: Ruft der jeweilige LSP-Client-Code beim Start/bei
   Bedarf selbstständig ein Setup-/Install-Skript auf (z. B. via eigenem
   ProcessBuilder-Aufruf, eigenem "ensureInstalled()"/"checkAndDownload()"-
   Mechanismus), UNABHÄNGIG von der zentralen Terminal-basierten
   Installations-UI? Zeige den entsprechenden Code, falls vorhanden.

2. Untersuche, was GENAU passiert, wenn ein Nutzer eine Kotlin-Datei öffnet,
   OHNE vorher jemals den "Installieren"-Button geklickt zu haben: Versucht
   das Plugin selbst, den Server zu installieren/zu starten? Schlägt es nur
   mit einer Fehlermeldung fehl? Ist das Verhalten identisch oder
   unterschiedlich zu dem Fall, wenn zuvor über den Terminal-Dialog
   "installiert" wurde?

3. Falls sich bestätigt, dass die Plugins ihre Server-Installation
   tatsächlich bereits selbst und vollständig übernehmen (oder übernehmen
   KÖNNTEN, mit geringem Zusatzaufwand): Erstelle einen konkreten
   Vorschlag, wie die separate Terminal-Installations-UI (Dialog, Toast,
   Terminal-Screen-Aufruf für LSP-Installation) ENTFERNT und durch einen
   einfachen, im Hintergrund laufenden, plugin-internen Installationslauf
   ersetzt werden kann (z. B. mit einem einfachen Fortschrittsindikator
   statt eines vollständigen Terminal-Screens). Liste konkret auf, welche
   Dateien/Funktionen dadurch komplett entfernt werden könnten.

4. Falls sich NICHT bestätigt, dass die Plugins dies selbst können (z. B.
   weil die Plugins bewusst NUR den LSP-Client-Teil enthalten und die
   Server-Binary-Beschaffung separat via Terminal/PRoot laufen MUSS, weil
   z. B. Kotlin-Sprachserver-Kompilierung/Installation Root-Zugriff auf
   das PRoot-Alpine-Rootfs benötigt, den das Plugin selbst nicht hat):
   Dokumentiere dies explizit als Begründung, WARUM die separate Route
   nötig ist, damit dies nicht erneut fälschlich als Redundanz
   missverstanden wird.

5. Liefere eine klare Empfehlung: "Separate Installationsroute BEIBEHALTEN
   und gemäß Teil A fixen" ODER "Separate Installationsroute ENTFERNEN und
   durch plugin-internen Mechanismus ersetzen" - mit Begründung basierend
   auf den tatsächlichen Code-Funden, nicht auf Vermutung.

═══════════════════════════════════════════════════════════════
ABSCHLUSS-REPORT
═══════════════════════════════════════════════════════════════

Liefere am Ende einen zusammenfassenden Report mit:
- Teil A: Liste aller korrigierten Pfad-Konstruktionsstellen (Datei+Zeile),
  bestätigter erfolgreicher End-to-End-Installationslauf ohne Terminal-Fehler
- Teil B: Klares Ergebnis der Redundanz-Prüfung mit Belegen (Codestellen),
  und finale Empfehlung inkl. Umsetzungsvorschlag, falls Entfernung
  empfohlen wird

Führe Teil B ZUERST/PARALLEL zu Teil A aus, aber committe BEIDE Ergebnisse
getrennt, damit im Fall "Route sollte entfernt werden" der Aufwand aus
Teil A nicht nachträglich als verschwendet erscheint - er bleibt in jedem
Fall als Dokumentation der Root-Cause-Analyse wertvoll.
