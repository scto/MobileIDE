Stufe 4 ist weiterhin nicht korrekt, das Verhalten hat sich aber verändert:

NEUES SYMPTOM nach deinem letzten Fix:
Klick auf "Installieren" navigiert jetzt korrekt zu einem Terminal-Screen
(nicht mehr fälschlich zum Speech-Server-Screen). Der Terminal-Screen wird
jedoch SOFORT wieder geschlossen, bevor irgendeine Installationsausgabe
sichtbar wird. Es ist unklar, ob:
  a) der zugrunde liegende Installer-Prozess sofort mit einem Fehler
     terminiert (z. B. Skript nicht gefunden, falscher Pfad, fehlende
     Ausführungsrechte, falsches Arbeitsverzeichnis) und der Terminal-Screen
     sich beim Prozessende automatisch/ungefragt schließt, oder
  b) der Terminal-Screen unabhängig vom Prozessstatus eine automatische
     "onFinish"/"onProcessExit"-Navigation zurück auslöst, die auch bei
     einem FEHLGESCHLAGENEN Prozess ausgeführt wird (statt nur bei Erfolg),
     oder
  c) es sich um denselben Navigations-Root-Cause wie zuvor handelt, nur dass
     jetzt eine andere, ebenfalls falsche Route (Terminal-Screen statt
     Speech-Server-Screen) getroffen wird, weil die Grundursache (falsche/
     fehlende Parametrisierung des Install-Ziels) noch nicht behoben ist,
     sondern nur ein Symptom verschoben wurde.

BITTE FOLGENDES TUN, IN DIESER REIHENFOLGE:

1. Finde die exakte Stelle, an der der Terminal-Screen für die LSP-
   Installation gestartet wird (vermutlich `TerminalLauncher`,
   `ScriptedLspServer`, oder ein `TerminalScreen`/`TerminalActivity`,
   aufgerufen über den in der letzten Stufe korrigierten Navigations-
   Callback). Zeige mir den exakten Code, der:
   - das auszuführende Kommando/Skript zusammensetzt (Pfad, Argumente,
     Arbeitsverzeichnis),
   - den Prozess startet,
   - auf das Prozessende reagiert (Exit-Code-Behandlung),
   - die Navigation zurück/Schließung des Terminal-Screens auslöst.

2. Führe das zusammengesetzte Installer-Kommando MANUELL im Terminal aus
   (außerhalb der App, direkt via adb shell oder lokal), mit exakt denselben
   Pfaden/Argumenten, die die App verwendet. Dokumentiere den tatsächlichen
   Exit-Code und die Fehlerausgabe (stderr). Das ist die Grundwahrheit dafür,
   ob der Installer-Prozess selbst fehlschlägt.

3. Falls der Prozess fehlschlägt (Exit-Code != 0): Behebe die Ursache
   (falscher Skriptpfad, fehlende Ausführungsrechte via chmod +x, falsches
   Arbeitsverzeichnis, fehlende Umgebungsvariable, fehlende Abhängigkeit
   z. B. curl/wget im verwendeten Busybox/Proot-Kontext). Zeige den
   korrigierten Kommandoaufbau.

4. UNABHÄNGIG davon, ob Punkt 3 einen Fehler ergab: Korrigiere die
   Exit-Behandlung im Terminal-Screen so, dass er sich NIEMALS automatisch
   schließt, wenn der Exit-Code des Prozesses != 0 ist. Bei Fehler MUSS die
   vollständige stdout/stderr-Ausgabe für den Nutzer sichtbar bleiben (kein
   Auto-Dismiss, kein Auto-Navigate-Back). Nur bei Exit-Code == 0 darf
   automatisch geschlossen/zurücknavigiert werden, und auch dann erst nach
   einer kurzen sichtbaren Erfolgsmeldung (z. B. 1-2 Sekunden oder Toast),
   nicht sofort.

5. Ergänze einen expliziten Log-Eintrag (via LogCatcher) beim Start und beim
   Ende des Installer-Prozesses inkl. Exit-Code, damit zukünftige ähnliche
   Fehler ohne manuelles Nachstellen im Logcat sichtbar sind.

6. Baue das Projekt, installiere die App neu, und teste erneut:
   - Kotlin-LSP-Installation über den Installieren-Button auslösen
   - Terminal-Screen MUSS sichtbar bleiben und die tatsächliche
     Installationsausgabe zeigen (nicht sofort verschwinden)
   - Bei Erfolg: Editor öffnet die Kotlin-Datei danach ohne "LSP-Fehler"
   - Bei Fehler: Fehlermeldung bleibt lesbar sichtbar, App stürzt nicht ab

Liefere nach Abschluss einen Report mit: exaktem vorherigem und
korrigiertem Kommandoaufbau, Exit-Code vor/nach dem Fix, und Screenshot-
Beschreibung des jetzt sichtbaren Terminal-Verhaltens.
