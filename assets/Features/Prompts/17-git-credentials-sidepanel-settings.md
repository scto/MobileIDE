### Prompt-Datei: `10-interactive-debugger-jdwp.md`

```
# Ziel
Implementiere einen interaktiven Debugger für Android-Apps, die aus MobileIDE gebaut und 
ausgeführt werden, mit Breakpoints, Variableninspektion und Step-Kontrollen im Editor 
(bereits im README als offenes TODO gelistet).

# Kontext
- Baut auf der Gradle-Bridge (Prompt 01), der Build-Funktionalität (Prompt 02) und 
  idealerweise auf der LSP-Integration (Prompt 05) auf, da Breakpoint-UI ähnliche 
  Editor-Overlay-Mechanismen braucht wie Diagnostics-Wellenlinien.
- Zielprotokoll: JDWP (Java Debug Wire Protocol), da Android-Apps über die ART-VM 
  darüber debuggbar sind (analog `adb jdwp` / `jdb`).

# Anforderungen
1. DebugSessionManager (neues Modul :features:debugger oder Teil von :core:runner):
   - Startet die App im Debug-Modus (`am start -D` bzw. entsprechendes Äquivalent über 
     den vorhandenen App-Installations-/Start-Mechanismus aus :core:apk-builder/:core:runner).
   - Verbindet sich über JDWP (z. B. via `jdb` im PRoot-Terminal-Kontext, gesteuert über 
     die :features:exec-Bridge aus Prompt 06, oder direkt über eine JDWP-Client-Library 
     falls in Kotlin/JVM verfügbar) mit dem laufenden Prozess.
   - Stellt Funktionen bereit: setBreakpoint(file, line), removeBreakpoint(file, line), 
     resume(), stepOver(), stepInto(), stepOut(), pause(), getStackFrames(), 
     getVariables(frameId), evaluateExpression(expr, frameId).

2. Editor-Integration (Breakpoints setzen):
   - Klick in die Zeilennummern-Gutter des Editors (sora-editor) setzt/entfernt einen 
     Breakpoint (rotes Icon in der Gutter-Spalte).
   - Bei aktiver Debug-Session und erreichtem Breakpoint: Hervorhebung der aktuellen 
     Zeile (z. B. gelber Hintergrund), automatisches Scrollen zur Zeile falls Datei 
     bereits offen, sonst automatisches Öffnen der Datei.

3. Debug-BottomSheet (Tab "Debug", analog zum bestehenden Build/Problems-Tab-Muster):
   - Variablen-Baumansicht (aktueller Stack-Frame: lokale Variablen, this-Referenz, 
     aufklappbar für verschachtelte Objekte).
   - Call-Stack-Liste (anklickbar, wechselt den inspizierten Frame).
   - Steuer-Buttons: Resume, Step Over, Step Into, Step Out, Stop – als Icon-Leiste 
     oben im Debug-Tab.
   - Konsole für Ausdrucksauswertung ("Watch"-Eingabefeld: Ausdruck eingeben, 
     Ergebnis im aktuellen Frame-Kontext anzeigen).

4. Lifecycle:
   - Debug-Start-Button im EditorScreen (ggf. als Long-Press-Variante des bestehenden 
     Run/Play-Buttons: "Debuggen" statt "Ausführen").
   - Saubere Session-Beendigung bei App-Absturz, manuellem Stop oder Editor-Schließen 
     (keine verwaisten JDWP-Verbindungen/Zombie-Prozesse).

# Akzeptanzkriterien
- Ein gesetzter Breakpoint in einer laufenden Debug-Session hält die App zuverlässig an 
  der richtigen Zeile an.
- Variablenwerte im Debug-Tab entsprechen nachweislich den tatsächlichen Laufzeitwerten 
  (verifizierbar durch Vergleich mit bekanntem Testszenario, z. B. Schleifenzähler).
- Step Over/Into/Out funktionieren gemäß Standard-Debugger-Semantik.
```