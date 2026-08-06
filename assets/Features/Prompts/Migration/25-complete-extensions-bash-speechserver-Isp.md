# Ziel
Korrigiere die fehlerhafte/inkonsistente Syntax-Highlighting-Farbgebung für Kotlin-
Dateien im TreeSitter-basierten Highlighting-Pfad von MobileIDE (com.scto.mobile.ide),
sodass unterschiedliche Kotlin-Sprachelemente visuell klar unterscheidbar sind,
konsistent mit gängigen IDE-Konventionen (z. B. Android Studio Darcula / VS Code Dark+).

# Kontext (verifizierter Ist-Zustand, siehe status.md/PROGRESS.md + Screenshot-Anhang)
- Modul `:language-treesitter` bietet laut status.md (2026-06-28) TreeSitter-
  Unterstützung für Java, Kotlin, XML, Log und C++ (README.md ergänzt zusätzlich C++).
- WICHTIGER VORBEFUND: Am 2026-06-29 wurde laut status.md unter dem Abschnitt 
  "TreeSitter Kotlin Script (KTS) Query Fixes" bereits eine Korrektur durchgeführt: 
  das lokale Kotlin-Highlights-Schema wurde durch das offizielle 
  `fwcd/tree-sitter-kotlin`-Highlight-Schema ersetzt, um Parser-Fehler durch veraltete/
  falsche Kotlin-AST-Nodes (`multiline_lambda_parameter`, `receiver_type: (_)`) zu 
  beheben. Zusätzlich wurde ein Timing-Bug behoben ("Immediate Highlight Color 
  Application"), bei dem neu geöffnete Tabs monochromen Text zeigten, indem das aktive 
  Compose-`ColorScheme` gecacht und beim Laden von TreeSitter-Instanzen sofort 
  angewendet wird.
- Ebenfalls am 2026-06-29 wurde für XML eine analoge Korrektur durchgeführt 
  ("TreeSitter XML Grammar Corrections", Theme Mapping in `EditorViewModel.kt` zur 
  Bindung von XML-Query-Tags an CSS/HTML-Theme-Farben) – dies dient als Referenz-
  Vorgehen für die jetzt nötige Kotlin-Farbzuordnungs-Vervollständigung.
- TROTZ dieser vorherigen Korrektur zeigt der aktuelle Screenshot (MainActivity.kt, 
  package/import-Block, `class MainActivity : Compon...`, `override fun onCreate`, 
  Compose-Aufrufe wie `AppTheme{}`/`Scaffold(...)`) weiterhin folgende Probleme:
  1. `package`, `import`, `class`, `override`, `fun` erscheinen ALLE in derselben 
     orange/gelben Farbe – keine Unterscheidung zwischen Import-Direktiven und 
     Deklarations-/Modifier-Keywords.
  2. Importierte Paket-Pfade (`android.os.Bundle`, `androidx.activity.Com...`, 
     `androidx.compose.foun...`) erscheinen im selben hellen Lavendel-Ton wie echte 
     Typ-Referenzen im Code-Body (`Scaffold`, `CounterA...`) – keine Unterscheidung 
     zwischen `@namespace`/Import-Pfad und `@type`.
  3. Der Klassenname `MainActivity` und der Funktionsname `onCreate` sind nicht 
     erkennbar vom umgebenden Fließtext-Weiß abgesetzt – keine sichtbare 
     `@function`/`@type`-Hervorhebung für Deklarationsnamen.
  4. Compose-Funktionsaufrufe (`enableEdgeToEdge()`, `setContent {}`, `AppTheme {}`, 
     `Scaffold(...)`) sind farblich nicht von normalen Variablen unterschieden – kein 
     `@function.call`/`@constructor`-Capture erkennbar.
  5. Kommentare/Annotationen/String-Literale sind im sichtbaren Ausschnitt nicht 
     farblich hervorgehoben.
  6. Die Statusleiste zeigt "● LSP Fehler" (roter Punkt) – unklar, ob unabhängig 
     vom TreeSitter-Highlighting oder mitverursachend.
- Dies deutet darauf hin, dass entweder (a) die 2026-06-29-Korrektur nur die 
  Parser-Fehler behoben hat, aber KEINE feingranulare Capture-Differenzierung 
  zwischen Import/Keyword/Type/Function-Call eingeführt wurde, oder (b) das 
  Compose-Theme-Farb-Mapping in `EditorViewModel.kt` für Kotlin nie so vollständig 
  wie für XML auf die feingranularen fwcd-Capture-Gruppen erweitert wurde.
- Relevante zu prüfende Dateien: die Kotlin-Highlight-Query im `:language-treesitter`-
  Modul (nach 2026-06-29 basierend auf `fwcd/tree-sitter-kotlin`), sowie 
  `EditorViewModel.kt` (enthält laut status.md das Farb-Mapping zwischen TreeSitter-
  Capture-Tags und dem Compose-`ColorScheme`, siehe auch `NavigationUtils.kt`-
  Nachbarmodul `core.common.utils` für Utility-Konventionen laut Fixing.md).
- Zusätzlicher zu prüfender Faktor: das LogCatcher-Subsystem (siehe status.md 
  2026-06-29 "LogCatcher Configuration") protokolliert Editor-Operationen optional 
  ausführlich – kann bei Aktivierung zur Diagnose der Ursache des "LSP Fehler"-
  Indikators (Stufe 0.4) genutzt werden.

# Aufgabe

## STUFE 0 – Ursachenanalyse (Pflicht vor Korrektur)
0.1. Lokalisiere im Modul `:language-treesitter` die konkrete Kotlin-Highlight-
   Query-Datei (das nach 2026-06-29 eingesetzte `fwcd/tree-sitter-kotlin`-Schema, 
   vermutlich `queries/kotlin/highlights.scm`) und liste ALLE darin tatsächlich 
   vorhandenen Capture-Namen auf (z. B. `@keyword`, `@keyword.import`, `@type`, 
   `@function`, `@variable`, `@string`, `@comment`, `@property`, `@namespace`).

0.2. Lokalisiere in `EditorViewModel.kt` (bzw. der zuständigen Theme-Mapping-Stelle, 
   analog zum bestätigten XML-Mapping aus 2026-06-29) die tatsächliche Zuordnung 
   dieser Capture-Namen zu konkreten Compose-Farben, und vergleiche sie mit der in 
   0.1 ermittelten vollständigen Capture-Liste. Identifiziere exakt, welche Capture-
   Gruppen im Mapping FEHLEN oder auf dieselbe Farbe wie andere, semantisch 
   unterschiedliche Capture-Gruppen zeigen (dies erklärt die im Screenshot 
   beobachtete Farbgleichheit von package/import vs. class/override/fun sowie 
   Import-Pfaden vs. echten Typnamen).

0.3. Prüfe, ob eine der nachfolgenden, in `PROGRESS.md` dokumentierten späteren 
   Änderungen (z. B. Prompt 15 "Consolidate Terminal Features Module", 2026-07-26, 
   Verschiebung von Assets nach `features/terminal/src/main/assets/`) versehentlich 
   Query-/Theme-Mapping-Assets des `:language-treesitter`-Moduls betroffen hat, oder 
   ob dies ein rein isoliertes, von allen Migrationen unabhängiges Problem ist.

0.4. Kläre unabhängig davon den Grund für die Statusanzeige "LSP Fehler" (separater 
   Log-Check im Kotlin-LSP-Server-Prozess innerhalb von `:extension-languages`, siehe 
   status.md 2026-07-04 "Kotlin: Custom Edge server wrapper" – z. B. Start-Fehler des 
   Kotlin-Language-Servers oder fehlender Classpath) und dokumentiere, ob dies mit 
   dem Highlighting-Problem zusammenhängt oder komplett getrennt zu behandeln ist.

0.5. Liefere einen kurzen Befund-Report (welche Datei(en) die Ursache sind, welche 
   Capture-Namen fehlen/falsch gemappt sind), BEVOR mit der Korrektur begonnen wird.

## STUFE 1 – Korrektur der Highlight-Query und des Farb-Mappings
1.1. Ergänze/korrigiere die Kotlin-Highlight-Query (sofern in Stufe 0.1 als 
   unvollständig identifiziert) bzw. primär das Farb-Mapping in `EditorViewModel.kt`, 
   sodass mindestens folgende Node-Typen mit eigenen, semantisch unterscheidbaren 
   Farben versehen sind:
   - `package`/`import` als `@keyword.import` (eigene, gedämpfte Direktiven-Farbe, 
     z. B. Grau-Blau, NICHT identisch zu Deklarations-Keywords)
   - `class`/`interface`/`object`/`fun`/`val`/`var`/`override`/`open`/`private`/
     `public`/`companion` als `@keyword`/`@keyword.modifier` (kräftige, aber vom 
     Import-Keyword unterscheidbare Farbe, z. B. Orange/Magenta)
   - Klassennamen/Typ-Referenzen/generische Typparameter als `@type` (z. B. Türkis)
   - Import-Pfad-Segmente als eigene, gedämpftere `@namespace`-Capture-Gruppe, 
     NICHT identisch zu `@type`
   - Funktionsnamen bei Deklaration (`fun onCreate`) als `@function`
   - Funktions-/Composable-Aufrufe (`AppTheme{}`, `Scaffold(...)`) als 
     `@function.call`/`@constructor`, klar von reinem `@variable`-Text abgesetzt
   - String-Literale als `@string`, Kommentare als `@comment`, Annotationen als 
     `@attribute`

1.2. Halte dich bei der Farbwahl an die Konvention, die laut status.md (2026-06-29) 
   bereits für XML etabliert wurde ("Configured proper sora-editor theme color 
   mapping... to bind... XML query tags to CSS/HTML theme colors") – d. h. 
   konsistentes Cross-Language-Farbschema (Keywords/Typen/Funktionen/Strings/
   Kommentare sollen über Kotlin, Java, XML hinweg dieselbe semantische Farbfamilie 
   nutzen).

1.3. Falls in Stufe 0.1 festgestellt wird, dass das `fwcd/tree-sitter-kotlin`-Schema 
   selbst zwar granulare Captures bereitstellt, aber `EditorViewModel.kt` diese nicht 
   vollständig konsumiert: erweitere ausschließlich das Mapping in 
   `EditorViewModel.kt`, ohne die Query-Datei unnötig zu verändern (Wiederverwendung 
   der bereits korrekt funktionierenden 2026-06-29-Parser-Fixes).

## STUFE 2 – Verifikation
2.1. Öffne erneut eine Kotlin-Datei mit vergleichbarem Inhalt wie im Screenshot 
   (package/import-Block, Klassendeklaration mit `override fun`, Compose-Aufrufe wie 
   `Scaffold`/`AppTheme`) und bestätige visuell, dass:
   - `package`/`import` sich farblich von `class`/`override`/`fun` unterscheiden
   - Import-Pfade sich farblich von echten Typnamen im Code-Body unterscheiden
   - `MainActivity` (Klassenname) und `onCreate` (Funktionsname) erkennbar 
     hervorgehoben sind
   - Compose-Funktionsaufrufe (`AppTheme`, `Scaffold`) sich farblich von normalen 
     Variablen abheben
2.2. Baue das Gesamtprojekt (mindestens `:language-treesitter`, `:editor-lsp`, `:app`) 
   und bestätige Fehlerfreiheit.
2.3. Berichte zusätzlich, ob die separat identifizierte Ursache des "LSP Fehler"-
   Statusindikators aus Stufe 0.4 im selben Zug behoben werden konnte oder als 
   eigenständiges Folge-Ticket zu behandeln ist.

# Nicht-Ziele
- Keine Änderung der grundsätzlichen Editor-Font/Layout-Einstellungen.
- Keine Änderung an Highlight-Queries/Mappings anderer Sprachen (Java/XML/Log/C++), 
  außer zur reinen Farbkonsistenz-Orientierung (Lesen, nicht Ändern).
- Kein erneutes Ersetzen des bereits funktionierenden `fwcd/tree-sitter-kotlin`-
  Parser-Schemas, sofern es die benötigten Capture-Gruppen bereits bereitstellt.

# Akzeptanzkriterien
- Kotlin-Code zeigt klar unterscheidbare Farben für: Import-Direktiven, Deklarations-
  Keywords, Modifier-Keywords, Typnamen, Import-Pfade, Funktionsdeklarationen, 
  Funktions-/Composable-Aufrufe, Strings, Kommentare, Annotationen.
- Die im Screenshot dokumentierte Farbgleichheit zwischen `package`/`import` und 
  `class`/`override`/`fun` sowie zwischen Import-Pfaden und echten Typnamen existiert 
  nach der Korrektur nicht mehr.
- Ursache des "LSP Fehler"-Statusindikators ist identifiziert und im Report 
  dokumentiert (behoben oder als Folgeaufgabe markiert).
