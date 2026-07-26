### Prompt-Datei: `12-git-3way-merge-conflict-tool.md`

# Ziel
Implementiere ein visuelles 3-Wege-Merge-/Konfliktlösungstool innerhalb der bestehenden 
Git-Integration von MobileIDE (im README bereits als TODO "Git-Konfliktlösungstool" gelistet).

# Kontext
- Grundlegende Git-Integration (Commit, Push, Pull, Branch-Verwaltung) ist laut Projektstand 
  bereits vorhanden. Diese Erweiterung fügt speziell die Konfliktlösung hinzu.

# Anforderungen
1. Konflikterkennung:
   - Nach einem `git merge`/`git pull`/`git rebase`-Vorgang (ausgeführt über die bestehende 
     Git-Bridge, analog zur Gradle-Bridge aus Prompt 01), erkenne Dateien mit 
     Konflikt-Markern (`<<<<<<<`, `=======`, `>>>>>>>`) automatisch per Parsing 
     des `git status`-Outputs (Status "UU"/"AA"/"DU" etc.).
   - Zeige eine Übersichtsliste betroffener Dateien in einem BottomSheet/Dialog 
     ("Konflikte lösen: 3 Dateien betroffen").

2. Merge-View (neuer Screen, aufgerufen pro Konfliktdatei):
   - Drei-Spalten- oder Zwei-Spalten-mit-Toggle-Ansicht: "Lokal (HEAD)" | "Eingehend 
     (MERGE_HEAD)" | ggf. "Ergebnis" (editierbar).
   - Bei Bildschirmbreiten-Constraints auf Mobilgeräten: primär ein "Block-für-Block"-
     Ansatz statt echter 3-Spalten-Seite-an-Seite-Darstellung – zeige jeden Konfliktblock 
     einzeln mit klar erkennbarer Formatierung (farbliche Hervorhebung: Blau=lokal, 
     Grün=eingehend), darunter drei Buttons: "Lokal übernehmen", "Eingehend übernehmen", 
     "Beide übernehmen" (in dieser Reihenfolge zusammengeführt).
   - Zusätzlich manuelles Editierfeld für den Fall, dass keine der drei Standardoptionen 
     passt (freie Textbearbeitung des finalen Blocks).
   - Fortschrittsanzeige "Konflikt 2 von 5 in dieser Datei gelöst".

3. Abschluss:
   - Nach Lösung aller Konflikte in einer Datei: Button "Als gelöst markieren" 
     (entspricht `git add <file>`), entfernt Datei aus der Konfliktliste.
   - Wenn alle Dateien gelöst sind: Button "Merge abschließen" (`git commit` mit 
     automatisch generierter Merge-Commit-Message, editierbar vor dem Absenden).
   - Abbruch-Option jederzeit verfügbar: "Merge abbrechen" (`git merge --abort`), 
     mit Sicherheitsabfrage.

4. Diff-Darstellung nutzt möglichst dieselbe Editor-Highlighting-Infrastruktur 
   (Syntax-Highlighting je Dateityp) wie der normale Code-Editor, um Konsistenz zu wahren.

# Akzeptanzkriterien
- Ein bewusst provozierter Merge-Konflikt (zwei Branches mit sich überschneidenden 
  Änderungen in derselben Datei) wird korrekt erkannt, alle Konfliktblöcke werden 
  vollständig aufgelistet, und nach Auswahl/Bearbeitung entsteht eine valide, 
  konfliktfreie Datei, die erfolgreich committed werden kann.