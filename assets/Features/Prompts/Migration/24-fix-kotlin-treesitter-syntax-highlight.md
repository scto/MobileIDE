# Ziel
Korrigiere die fehlerhafte/inkonsistente Syntax-Highlighting-Farbgebung für Kotlin- 
Dateien im TreeSitter-basierten Highlighting-Pfad von MobileIDE (com.scto.mobile.ide), 
sodass unterschiedliche Kotlin-Sprachelemente visuell klar unterscheidbar sind, 
konsistent mit gängigen IDE-Konventionen (z. B. Android Studio Darcula / VS Code 
Dark+).

# Kontext (beobachteter Ist-Zustand, siehe Screenshot Anhang)
Im CodeEditScreen (MainActivity.kt-Beispiel, Datei über den Kotlin-TreeSitter-Grammar-
Pfad gerendert, LSP-Statusanzeige unten links zeigt "● LSP Fehler") ist folgende 
fehlerhafte Farbverteilung sichtbar:

1. Die Schlüsselwörter `package`, `import`, `class`, `override`, `fun` sowie das 
   Kontroll-/Deklarationswort-Cluster erscheinen ALLE in derselben orangen/gelben 
   Farbe – obwohl `class`/`override`/`fun` (Deklarations-Keywords) sich farblich von 
   `package`/`import` (Direktiven) unterscheiden sollten. Dies deutet darauf hin, 
   dass die TreeSitter-Highlight-Query für Kotlin diese Node-Typen nicht differenziert 
   (z. B. fehlende oder falsche Capture-Namen wie `@keyword.import` vs. 
   `@keyword.function` vs. `@keyword.modifier` in der `.scm`-Query-Datei).

2. Importierte Klassen-/Paket-Pfade (`android.os.Bundle`, `androidx.activity.Com...`, 
   `androidx.compose.foun...` etc.) erscheinen in einem hellen Lavendel/Flieder-Ton, 
   identisch zur Farbe von tatsächlichen Typ-Referenzen im Code (z. B. `Scaffold`, 
   `CounterA...` in Zeile 21/22). Import-Pfade (Paket-Segmente, i. d. R. Identifier- 
   Ketten) sollten sich farblich von echten Typ-/Klassennamen im Code-Body 
  unterscheiden, tun es hier aber nicht – beides wirkt wie dieselbe `@type`- oder 
  `@variable`-Capture-Gruppe.

3. Der Klassenname nach `class` (`MainActivity`) sowie der Funktionsname nach 
   `override fun` (`onCreate`) sind im Screenshot nicht klar farblich vom 
   umgebenden Fließtext-Weiß abgesetzt – es fehlt eine erkennbare `@function` bzw. 
   `@type`-Hervorhebung für Deklarationsnamen.

4. Es ist keine sichtbare Unterscheidung zwischen Funktionsaufrufen 
   (`enableEdgeToDetection()`, `setContent {}`, `AppTheme {}`, `Scaffold(...)`) und 
   Klassennamen/Composable-Funktionen erkennbar – alles erscheint in derselben 
   Grundfarbe (Weiß) ohne semantische Abhebung, was für Compose-Code (PascalCase- 
   Funktionsaufrufe wie `AppTheme`, `Scaffold`, `CounterA...`) besonders 
   irreführend ist, da diese in Kotlin/Compose-Konvention üblicherweise wie Typen 
   eingefärbt werden sollten.

5. Kommentare, Annotationen (z. B. `@Composable`, sofern im Code vorhanden) und 
   String-Literale sind im sichtbaren Ausschnitt nicht erkennbar farblich 
   hervorgehoben – zu prüfen, ob die entsprechenden Captures in der Kotlin-Query 
   überhaupt vorhanden sind.

6. Die Statusleiste zeigt "● LSP Fehler" (roter Punkt) – zu klären, ob dieser 
   LSP-Fehlerzustand zusätzlich (unabhängig vom TreeSitter-Highlighting) die 
   korrekte Diagnostics-basierte Einfärbung/Unterstreichung verhindert oder ob dies 
   ein separates, unabhängiges Problem ist (siehe Stufe 0).

# Aufgabe

## STUFE 0 – Ursachenanalyse (Pflicht vor Korrektur)
0.1. Lokalisiere im Modul `:language-treesitter` bzw. `:editor-lsp` die konkrete 
   Kotlin-Highlight-Query-Datei (vermutlich `queries/kotlin/highlights.scm` oder 
   vergleichbar, analog zum bestehenden Muster für JS/CSS/HTML aus `.gitmodules`), 
   sowie die zugehörige Farbzuordnungs-/Theme-Mapping-Datei (Compose-Farbschema, das 
   TreeSitter-Capture-Namen wie `@keyword`, `@type`, `@function`, `@variable`, 
   `@string`, `@comment`, `@property` auf konkrete Farben abbildet).

0.2. Prüfe, ob die Kotlin-Grammatik/Query-Datei tatsächlich als vollständiges, 
   funktionierendes TreeSitter-Submodule vorliegt (siehe `.gitmodules`-Eintrag für 
   `tree-sitter-kotlin`) oder ob nur ein rudimentärer/generischer Fallback-Query-Satz 
   genutzt wird, der die oben beschriebene fehlende Differenzierung erklärt.

0.3. Kläre unabhängig davon den Grund für die Statusanzeige "LSP Fehler" (separater 
   Log-Check im Kotlin-LSP-Server-Prozess, z. B. Kotlin-Language-Server-Start-Fehler 
   oder fehlender Classpath) und dokumentiere, ob dies mit dem Highlighting-Problem 
   zusammenhängt oder komplett getrennt zu behandeln ist.

0.4. Liefere einen kurzen Befund-Report (welche Datei(en) die Ursache sind, welche 
   Capture-Namen fehlen/falsch gemappt sind), BEVOR mit der Korrektur begonnen wird.

## STUFE 1 – Korrektur der Highlight-Query
1.1. Ergänze/korrigiere die Kotlin-Highlight-Query so, dass mindestens folgende 
   Node-Typen mit eigenen, semantisch korrekten Captures versehen sind:
   - `package`/`import` als `@keyword.import` (eigene Direktiven-Farbe, z. B. 
     gedämpftes Grau-Blau, NICHT identisch zu Deklarations-Keywords)
   - `class`/`interface`/`object`/`fun`/`val`/`var`/`override`/`open`/`private`/
     `public`/`companion` als `@keyword`/`@keyword.modifier` (konsistente, aber vom 
     Import-Keyword unterscheidbare Farbe, z. B. kräftiges Orange/Magenta – 
     Modifier wie `override`/`private` ggf. leicht abgesetzt von Struktur-Keywords 
     wie `class`/`fun`)
   - Klassennamen nach `class`/Typ-Referenzen/generische Typparameter als `@type` 
     (z. B. Türkis/Teal, klar abgesetzt von Import-Pfaden)
   - Import-Pfad-Segmente (Paket-Identifier-Ketten) als eigene, gedämpftere 
     `@namespace`/`@module`-Capture-Gruppe, NICHT identisch zu `@type`
   - Funktionsnamen bei Deklaration (`fun onCreate`) als `@function`
   - Funktions-/Composable-Aufrufe (PascalCase-Aufrufe wie `AppTheme{}`, 
     `Scaffold(...)`) als `@function.call` bzw. `@constructor`, farblich klar von 
     reinem `@variable`-Text abgesetzt
   - String-Literale als `@string`, Kommentare als `@comment`, Annotationen 
     (`@Composable` etc.) als `@attribute`

1.2. Ordne jeder dieser Capture-Gruppen im Compose-Farbschema/Theme-Mapping des 
   Editors konkrete, gut unterscheidbare Farbwerte zu, orientiert an einer 
   etablierten Dark-Theme-Konvention (z. B. Keywords in kräftigem Orange/Rosa, Typen 
   in Türkis, Funktionsnamen in Gelb, Strings in Grün, Kommentare in Grau/Gedämpft, 
   Import-Pfade in gedämpftem Blaugrau) – Konsistenz mit bereits für andere Sprachen 
   (JS/CSS/HTML) genutzten Farbkonventionen im selben Editor-Farbschema beachten, 
   damit das Gesamt-Look&Feel einheitlich bleibt.

1.3. Falls in Stufe 0.2 festgestellt wird, dass die Kotlin-TreeSitter-Grammatik nur 
   unvollständig/fallback-artig eingebunden ist: ergänze das fehlende offizielle 
   `tree-sitter-kotlin`-Submodule korrekt in `.gitmodules` und binde dessen 
   vollständige, gepflegte Highlight-Query ein statt einer minimalen Eigenlösung.

## STUFE 2 – Verifikation
2.1. Öffne erneut eine Kotlin-Datei mit vergleichbarem Inhalt wie im Screenshot 
   (package/import-Block, Klassendeklaration mit override fun, Compose-Aufrufe wie 
   Scaffold/AppTheme) und bestätige visuell, dass:
   - `package`/`import` sich farblich von `class`/`override`/`fun` unterscheiden
   - Import-Pfade sich farblich von echten Typnamen im Code-Body unterscheiden
   - `MainActivity` (Klassenname) und `onCreate` (Funktionsname) erkennbar 
     hervorgehoben sind
   - Compose-Funktionsaufrufe (`AppTheme`, `Scaffold`) sich farblich von normalen 
     Variablen abheben
2.2. Baue das Gesamtprojekt (mindestens `:language-treesitter`, `:editor-lsp`, `:app`) 
   und bestätige Fehlerfreiheit.
2.3. Berichte zusätzlich, ob die separat identifizierte Ursache des "LSP Fehler"- 
   Statusindikators aus Stufe 0.3 im selben Zug behoben werden konnte oder als 
   eigenständiges Folge-Ticket zu behandeln ist.

# Nicht-Ziele
- Keine Änderung der grundsätzlichen Editor-Font/Layout-Einstellungen.
- Keine Änderung an Highlight-Queries anderer Sprachen (JS/CSS/HTML/Java/XML etc.), 
  außer zur reinen Farbkonsistenz-Orientierung (Lesen, nicht Ändern).

# Akzeptanzkriterien
- Kotlin-Code zeigt klar unterscheidbare Farben für: Import-Direktiven, Deklarations-
  Keywords, Modifier-Keywords, Typnamen, Import-Pfade, Funktionsdeklarationen, 
  Funktions-/Composable-Aufrufe, Strings, Kommentare, Annotationen.
- Die im Screenshot dokumentierte Farbgleichheit zwischen `package`/`import` und 
  `class`/`override`/`fun` sowie zwischen Import-Pfaden und echten Typnamen existiert 
  nach der Korrektur nicht mehr.
- Ursache des "LSP Fehler"-Statusindikators ist identifiziert und im Report 
  dokumentiert (behoben oder als Folgeaufgabe markiert).
