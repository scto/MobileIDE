### Prompt-Datei: `13-gradle-dependency-cache-manager.md`

# Ziel
Implementiere eine UI zur Anzeige und Verwaltung des lokalen Gradle-/Maven-Dependency-Caches 
innerhalb der MobileIDE-Settings, da mobiler Speicherplatz ein kritischerer Faktor ist 
als auf Desktop-Entwicklungsumgebungen.

# Kontext
- Baut sinnvoll auf der Gradle-Bridge-Infrastruktur aus Prompt 01 auf (Prozessausführung 
  im PRoot-Kontext), ist aber unabhängig implementierbar.
- Relevanter Pfad: `~/.gradle/caches/modules-2/files-2.1/` innerhalb der Sandbox 
  (Pfad ggf. über die dynamische SandboxPaths-Auflösung aus Prompt 09 ermitteln, 
  statt hartkodiert).

# Anforderungen
1. CacheAnalyzer (Backend-Logik, sinnvoll in :core:tooling:tooling-server):
   - Scannt rekursiv den Gradle-Cache-Ordner und aggregiert pro Dependency 
     (group:artifact:version) die belegte Speichergröße (jar + pom + Metadaten + 
     sources/javadoc-Varianten falls vorhanden).
   - Ermittelt Gesamtgröße des Caches, sowie Top-N größte Einzelabhängigkeiten.
   - Optional: Ermittelt "verwaiste" Cache-Einträge, die zu keiner aktuell im Projekt 
     referenzierten Dependency mehr gehören (Abgleich mit `./gradlew :app:dependencies`-
     Output aller Module), um gezieltes Aufräumen zu ermöglichen.

2. UI – neuer Settings-Unterpunkt "Speicher & Cache":
   - Kreisdiagramm oder Balkendiagramm: Gesamtgröße Gradle-Cache, Android-SDK-Cache, 
     Build-Output-Ordner (build/-Verzeichnisse aller Module), getrennt ausgewiesen.
   - Liste der größten Einzelabhängigkeiten mit Name, Version, Größe.
   - Aktionen: 
     - "Gesamten Gradle-Cache löschen" (mit deutlicher Warnung: nächster Build lädt 
       alle Abhängigkeiten neu, benötigt Internetverbindung).
     - "Nur verwaiste Einträge löschen" (sicherere Variante).
     - "Build-Output-Ordner aller Module leeren" (`./gradlew clean`-Äquivalent über 
       die Gradle-Bridge).
   - Bestätigungsdialog vor jeder destruktiven Aktion mit Anzeige der freizugebenden 
     Speichermenge.

3. Aktualisierung:
   - Cache-Größenanzeige wird nicht bei jedem Settings-Öffnen neu berechnet 
     (potenziell teuer bei großem Cache), sondern mit Zeitstempel "Zuletzt aktualisiert" 
     und explizitem "Jetzt aktualisieren"-Button.

# Akzeptanzkriterien
- Angezeigte Gesamtgröße entspricht (im Toleranzbereich von Dateisystem-Rundungsfehlern) 
  der tatsächlich per `du -sh ~/.gradle/caches` ermittelbaren Größe.
- Nach "Gesamten Cache löschen" funktioniert ein nachfolgender Build weiterhin korrekt 
  (lädt Abhängigkeiten neu herunter, ohne Fehler).