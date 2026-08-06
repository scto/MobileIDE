# Ziel
Behebe drei/vier zusammenhängende UI- und Sicherheitsprobleme in MobileIDE 
(com.scto.mobile.ide): falsches Projekt-Screen-Label "Web-Projekte", fehlerhaftes 
Layout des Repository-Konfigurationsdialogs (vertikal gestapelter Button-Text), 
unsichere/ungeeignete Speicherung von Git-Credentials, sowie den im Git-Settings-
Screen beobachteten Endlos-Wiederholungs-Bug der Einstellungssektionen.

# Kontext (siehe drei Screenshot-Anhänge)

## Screenshot 2 – Projekt-Übersichtsscreen
Der Projekt-Übersichtsscreen zeigt fälschlicherweise die Überschrift "**Web-Projekte**" 
über der Liste vorhandener Projekte (aktuell nur "MyApp"), obwohl es sich um die 
zentrale, allgemeine Projekt-Übersicht von MobileIDE handelt (native Android/Kotlin-
Projekte, nicht speziell Web-Projekte). Die Überschrift muss zu "**MobileIDE 
Projekte**" korrigiert werden. Es ist zu prüfen, ob dieser String hartkodiert ist 
oder ob es sich um einen fehlerhaften/veralteten String-Resource-Key aus der 
ursprünglichen Xed-Editor-Migration handelt (String-Resource, die versehentlich 
nicht an das MobileIDE-Branding angepasst wurde, siehe bekanntes Migrationsmuster 
`Xed`→`MobileIDE` aus `migrate_xed.py`).

## Screenshot 1 – Repository-Konfigurationsdialog (Layout-Bug)
Der über den Git-Tab im Filetree geöffnete "Repository-Konfiguration"-Dialog zeigt 
einen massiven Layout-Fehler: Der Button "**Konfiguration speichern**" wird NICHT 
horizontal, sondern mit vertikal gestapelten Einzelbuchstaben 
("K-o-n-f-i-g-u-r-a-t-i-o-n" / "s-p-e-i-c-h-e-r-n") in einer extrem schmalen, aber 
nahezu bildschirmhohen hellblauen Pill-Form am rechten Dialogrand dargestellt. Der 
Rest des Dialogs (Eingabefelder für Repository-URL/Username/Token/SSH-Key, 
erwartbar zwischen "Verbindung testen" und "Abbrechen") ist leer/nicht sichtbar. 
Dies deutet auf ein fehlendes `Modifier.fillMaxWidth()`/falsches `wrapContentWidth()`
am Button sowie vermutlich eine fehlerhafte `Column`/`Row`-Verschachtelung mit 
falscher Gewichtung (`weight()`) oder fixierter Höhe/Breite hin, die den 
Text-Layoutalgorithmus zwingt, in die Höhe statt in die Breite zu wachsen.

## Screenshot 3 – Git-Settings-Screen (Endlos-Wiederholungs-Bug)
Der Git-Settings-Screen zeigt die Sektionen "General" (mit "Colorize file names" 
Toggle), "Account" (Credentials/User Data), "Repository" (Submodules/Recursive 
submodules) NICHT EINMAL, sondern MEHRFACH HINTEREINANDER WIEDERHOLT (mindestens 
4-5 identische Wiederholungen sichtbar im Screenshot, vermutlich durch eine 
fehlerhafte Schleife/LazyColumn-Item-Duplizierung oder eine versehentlich mehrfach 
aufgerufene Compose-Funktion). Dies ist ein eigenständiger, kritischer Bug, der 
zusätzlich zur eigentlichen Anfrage behoben werden muss, da der Screen in diesem 
Zustand unbenutzbar ist.

## Sicherheitsproblem – Credentials-Speicherung
Aktuell werden Git-Credentials (Username/Token/Passwort/SSH-Key-Passphrase) 
vermutlich unverschlüsselt in normalen SharedPreferences oder einer Klartext-Datei 
gespeichert (zu verifizieren). Dies ist ein Sicherheitsrisiko. Die Speicherung MUSS 
auf eine geeignete, verschlüsselte Persistenzlösung umgestellt werden – bevorzugt 
NICHT reines Room (Room speichert standardmäßig unverschlüsselt in SQLite), sondern 
eine Kombination aus:
- **Android Keystore + EncryptedSharedPreferences** (Jetpack Security Crypto, 
  `androidx.security:security-crypto`) für sensible Einzelwerte (Token/Passwort/
  SSH-Passphrase), ODER
- **Room mit SQLCipher-Verschlüsselung** (`net.zetetic:android-database-sqlcipher`), 
  falls strukturierte Mehrfach-Repository-Credentials (mehrere Remotes mit je 
  eigenen Zugangsdaten) persistiert werden müssen und eine relationale Struktur 
  sinnvoller ist als einzelne Key-Value-Paare.
Die konkrete Entscheidung zwischen beiden Ansätzen ist in Stufe 0 anhand des 
tatsächlichen Datenmodells (ein globales Credential-Set vs. mehrere Repository-
spezifische Credential-Sets) zu treffen und zu begründen.

# WICHTIG – Vorgehen
Bearbeite die folgenden Stufen NACHEINANDER. Nach jeder Stufe: Gesamtprojekt bauen 
(Gradle), Fehlerfreiheit bestätigen, Ergebnis-Report mit geänderten Dateien liefern, 
bevor die nächste Stufe beginnt.

---

## STUFE 0 – Bestandsaufnahme (Pflicht vor jeder Code-Änderung)

0.1. Lokalisiere den Compose-Screen und/oder String-Resource-Eintrag, der aktuell 
   "Web-Projekte" anzeigt (z. B. `ProjectListScreen.kt` bzw. `strings.xml`-Key wie 
   `web_projects_title`). Kläre, ob es sich um einen isolierten String handelt oder 
   ob mehrere Vorkommen ("Web-Projekt" im Singular, im "+ Neues Projekt"-Dialog, in 
   Menü-Titeln etc.) ebenfalls betroffen sind.

0.2. Lokalisiere `GitConfigDialog.kt` (bzw. die exakte Compose-Funktion hinter 
   "Repository-Konfiguration") und analysiere die Layout-Struktur exakt an der 
   Stelle des "Konfiguration speichern"-Buttons: identifiziere die fehlerhafte 
   Modifier-Kette (fehlendes `fillMaxWidth()`, falsches `weight()` in einer `Row`, 
   oder ein fälschlich vertikal statt horizontal ausgerichteter Parent-Container).

0.3. Lokalisiere `GitSettingsScreen.kt` (bzw. äquivalent) und identifiziere die 
   exakte Ursache der in Screenshot 3 sichtbaren Endlos-Wiederholung der Sektionen 
   "General"/"Account"/"Repository" (z. B. eine `LazyColumn`, die über eine Liste 
   iteriert, welche fälschlicherweise mehrfach identische Einträge enthält, oder 
   eine rekursiv/mehrfach aufgerufene `@Composable`-Funktion ohne Abbruchbedingung, 
   oder ein Zustand, der bei jeder Recomposition erneut Items an eine bestehende 
   Liste anhängt statt sie zu ersetzen).

0.4. Lokalisiere die aktuelle Implementierung der Git-Credentials-Speicherung 
   (z. B. `GitCredentialsRepository.kt`/`GitConfigViewModel.kt`) und dokumentiere 
   exakt: Wo werden Username/Token/Passwort/SSH-Key-Daten aktuell gespeichert 
   (SharedPreferences-Datei, Room-Entity, Klartext-Datei im Filesystem)? Sind sie 
   verschlüsselt? Welches Datenmodell liegt vor (ein globales Credential-Set oder 
   mehrere Repository-spezifische Sets, z. B. bei mehreren Remotes/Projekten)?

0.5. Liefere diese Bestandsaufnahme als eigenständigen Zwischen-Report, inkl. 
   begründeter Empfehlung für die Speicherlösung aus Stufe 3 (EncryptedShared-
   Preferences vs. Room+SQLCipher), BEVOR mit Stufe 1 fortgefahren wird.

---

## STUFE 1 – Korrektur "Web-Projekte" → "MobileIDE Projekte"

1.1. Ersetze alle in Stufe 0.1 identifizierten Vorkommen von "Web-Projekt(e)" durch 
   "MobileIDE Projekt(e)" (korrekte Groß-/Kleinschreibung und Singular/Plural-Form 
   je Kontext beachten, z. B. "MobileIDE Projekte" als Screen-Titel, "Neues MobileIDE 
   Projekt" falls im Dialog ebenfalls betroffen – NUR falls der Nutzer-Screenshot 2 
   dies auch am "+ Neues Projekt"-Button zeigt, dort steht aktuell nur "Neues 
   Projekt" ohne "Web"-Präfix, dies NICHT unnötig verändern, wenn dort kein Fehler 
   vorliegt).

1.2. Build- & visuelle Verifikation: Projekt-Übersichtsscreen zeigt "MobileIDE 
   Projekte" als Überschrift.

---

## STUFE 2 – Behebung des Git-Settings-Endlos-Wiederholungs-Bugs

2.1. Behebe die in Stufe 0.3 identifizierte Ursache der wiederholten Sektionen, 
   sodass "General", "Account" und "Repository" jeweils EXAKT EINMAL im Git-
   Settings-Screen erscheinen, mit korrektem Inhalt (Colorize-file-names-Toggle, 
   Credentials-Eintrag, User-Data-Eintrag, Submodules-Toggle, Recursive-submodules-
   Toggle).

2.2. Build- & visuelle Verifikation: Git-Settings-Screen zeigt jede Sektion nur 
   einmal, Scroll-Verhalten ist korrekt (kein unendlich langer, sich wiederholender 
   Inhalt mehr).

---

## STUFE 3 – Layout-Korrektur des Repository-Konfigurationsdialogs

3.1. Korrigiere die in Stufe 0.2 identifizierte Modifier-Kette des "Konfiguration 
   speichern"-Buttons, sodass er als normaler, horizontal ausgerichteter Button mit 
   angemessener Breite (z. B. `Modifier.fillMaxWidth()` innerhalb einer `Column`, 
   oder korrektes `weight(1f)` innerhalb einer `Row` gemeinsam mit "Abbrechen") 
   dargestellt wird, konsistent mit dem restlichen App-Design (z. B. wie der 
   "Abbrechen"-Button im selben Dialog korrekt dargestellt wird, oder wie der "+ 
   Neues Projekt"-Button in Screenshot 2 korrekt als horizontale Pill mit Icon+Text 
   dargestellt wird).

3.2. Stelle zusätzlich sicher, dass der restliche Dialog-Inhalt (Eingabefelder für 
   Repository-URL, Username, Token/Passwort, ggf. SSH-Key-Auswahl, User-Name/E-Mail 
   für Commits) tatsächlich sichtbar und mit dem Dialog scrollbar ist, falls der 
   Inhalt die verfügbare Höhe übersteigt (aktuell wirkt der Dialog leer – prüfen, ob 
   dies am selben Layout-Bug liegt oder ein separates Problem ist).

3.3. Build- & visuelle Verifikation: "Konfiguration speichern"-Button ist normal 
   horizontal lesbar, alle Eingabefelder des Dialogs sind sichtbar und nutzbar.

---

## STUFE 4 – Sichere Speicherung der Git-Credentials

4.1. Implementiere die in Stufe 0.5 empfohlene Speicherlösung:
   - Falls **EncryptedSharedPreferences** gewählt wird: Ergänze Dependency 
     `androidx.security:security-crypto` im passenden Modul (`:features:git` bzw. 
     `:core:lsp`-Nachbarmodul-Konvention folgend), erstelle einen 
     `MasterKey`-basierten `EncryptedSharedPreferences`-Store, migriere alle 
     bestehenden Zugriffe von `GitCredentialsRepository` auf diesen Store.
   - Falls **Room + SQLCipher** gewählt wird (bei mehreren Repository-spezifischen 
     Credential-Sets): Ergänze Dependency `net.zetetic:android-database-sqlcipher`, 
     definiere eine `GitCredentialEntity` (Felder: `repositoryId`, `username`, 
     `encryptedToken`, `encryptedPassword`, `sshKeyAlias`, `createdAt`), binde die 
     Datenbank-Passphrase selbst über Android Keystore ab (Passphrase NIE im 
     Klartext im Code oder in normalen Preferences).

4.2. Implementiere eine **einmalige Migrationsroutine**, die beim ersten App-Start 
   nach diesem Update bestehende, unverschlüsselt gespeicherte Credentials (falls in 
   Stufe 0.4 als vorhanden identifiziert) automatisch in den neuen, verschlüsselten 
   Speicher überträgt und die alte Klartext-Quelle danach sicher löscht 
  (überschreiben, nicht nur `.delete()`, um Datenreste zu vermeiden).

4.3. Passe `GitConfigDialog`/`GitConfigViewModel` so an, dass Lese-/Schreibzugriffe 
   ausschließlich über die neue, verschlüsselte Speicherschicht erfolgen.

4.4. Stelle sicher, dass SSH-Key-Passphrasen und Tokens NIEMALS in Log-Ausgaben 
   (LogCatcher/LogEntry) oder Crash-Reports landen (Redaction/Maskierung in allen 
   betroffenen Logging-Aufrufen ergänzen, falls aktuell nicht vorhanden).

4.5. Build- & Funktionsverifikation: Speichere Test-Credentials über den Dialog, 
   verifiziere über Root-Zugriff/adb, dass die zugrundeliegende Datei tatsächlich 
   verschlüsselt/nicht im Klartext lesbar ist. Starte die App neu, bestätige, dass 
   "Verbindung testen" mit den gespeicherten, verschlüsselten Credentials weiterhin 
   erfolgreich funktioniert.

---

## STUFE 5 – Abschlussverifikation

5.1. Vollständiger Gradle-Build aller betroffenen Module.

5.2. Manuelle Durchklick-Bestätigung aller vier behobenen Punkte (Projekt-Titel, 
   Dialog-Layout, Settings-Wiederholungs-Bug, verschlüsselte Credential-Speicherung).

5.3. Liefere einen finalen Gesamt-Report mit allen geänderten Dateien pro Stufe, 
   inkl. expliziter Bestätigung, dass keine Klartext-Credentials mehr auf dem Gerät 
   verbleiben.

# Nicht-Ziele
- Keine Änderung der grundsätzlichen Git-Funktionslogik (Commit/Push/Pull/Clone-
  Verhalten) jenseits der Credential-Speicherschicht.
- Keine Änderung des allgemeinen App-Farbschemas/Theme jenseits der reinen Layout-
  Korrektur des einen betroffenen Buttons.
- Keine Umbenennung von "Projekt" in einen anderen Begriff außer der spezifisch 
  angefragten Korrektur "Web-Projekte" → "MobileIDE Projekte".

# Akzeptanzkriterien
- Projekt-Übersichtsscreen zeigt "MobileIDE Projekte" statt "Web-Projekte".
- "Konfiguration speichern"-Button im Repository-Konfigurationsdialog ist normal 
  horizontal lesbar, restlicher Dialog-Inhalt ist sichtbar und nutzbar.
- Git-Settings-Screen zeigt jede Sektion (General/Account/Repository) exakt einmal.
- Git-Credentials (Username/Token/Passwort/SSH-Passphrase) werden nachweislich 
  verschlüsselt gespeichert (EncryptedSharedPreferences oder Room+SQLCipher), 
  bestehende Klartext-Daten wurden migriert und sicher gelöscht, Logging enthält 
  keine Klartext-Credentials.
- Gesamtprojekt-Build ist am Ende fehlerfrei.
