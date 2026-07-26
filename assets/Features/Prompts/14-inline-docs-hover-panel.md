### Prompt-Datei: `14-inline-docs-hover-panel.md`

# Ziel
Implementiere einen "Docs"-Tab im bestehenden BottomSheet-Tab-System, der KDoc/Javadoc-
Dokumentation aus Projekt-Dependencies extrahiert und im Editor-Kontext anzeigt, 
kombinierbar mit der Hover-Funktionalität aus der LSP-Integration (Prompt 05).

# Kontext
- Setzt idealerweise auf Prompt 05 (Hover via LSP) auf: Hover liefert bereits 
  Kurzinformationen inline; dieser Docs-Tab bietet die ausführlichere, durchsuchbare 
  Variante in einem dedizierten Bereich, analog zum bestehenden Build/Problems-Tab-Muster.
- Viele Dependencies bringen `-sources.jar` mit, aus denen KDoc/Javadoc-Kommentare 
  extrahierbar sind, auch ohne separate `-javadoc.jar`.

# Anforderungen
1. DocExtractor (Backend, sinnvoll in :features:lsp nach Konsolidierung, siehe Prompt 06):
   - Bei Cursor-Position auf einem Symbol (Klasse/Methode/Property) im Editor: nutze 
     zunächst die LSP-Hover-Antwort als primäre Quelle (schnell, bereits vorhanden).
   - Falls LSP keine oder nur minimale Dokumentation liefert: Fallback-Suche in 
     lokal vorhandenen `-sources.jar`-Dateien im Gradle-Cache (siehe Prompt 13 für 
     Cache-Pfad-Kenntnis) – extrahiere den KDoc/Javadoc-Kommentarblock direkt über 
     der Symbol-Deklaration per einfachem Parsing (Kommentar-Block unmittelbar vor 
     `fun`/`class`/`val`-Deklaration mit passendem Namen).
   - Cache extrahierte Dokumentationen pro Symbol im Arbeitsspeicher, um wiederholtes 
     Parsen derselben Datei zu vermeiden.

2. UI – "Docs"-Tab im BottomSheet:
   - Zeigt bei Cursor-Bewegung im Editor (mit leichtem Debounce, z. B. 300ms) automatisch 
     die Dokumentation des aktuell unter dem Cursor stehenden Symbols an, sofern der 
     Tab sichtbar/aufgeklappt ist (kein automatisches Aufklappen, nutzergesteuert).
   - Markdown-Rendering für Formatierungen (`@param`, `@return`, Code-Blöcke, Links).
   - "Pin"-Funktion: aktuelle Dokumentation fixieren, sodass sie bei weiterer 
     Cursor-Bewegung nicht überschrieben wird (nützlich zum Nachlesen während 
     man weiterschreibt).
   - Verlaufsliste der zuletzt angesehenen Symbole (History) zum schnellen Zurückspringen.

3. Performance:
   - Sources-Jar-Extraktion läuft ausschließlich auf Hintergrund-Thread, niemals 
     blockierend für Tastatureingaben im Editor.

# Akzeptanzkriterien
- Cursor auf einer Standard-AndroidX- oder Kotlin-Stdlib-Funktion mit vorhandenem 
  KDoc-Kommentar zeigt im Docs-Tab die tatsächliche, korrekt formatierte Dokumentation.
- Kein spürbares Editor-Lag durch die Docs-Extraktion, auch bei größeren sources.jar-Dateien.