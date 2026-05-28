# Aider Workflow für den Project Wizard
Um das beste Ergebnis mit Aider zu erzielen, solltest du die Spezifikation als Datei übergeben und Aider dann den Startschuss geben.
## 1. Vorbereitung
 1. Speichere den Text aus der vorherigen Antwort in einer Datei namens ProjectWizardPrompt.md in deinem Projektverzeichnis.
 2. Stelle sicher, dass deine MobileIDE.md ebenfalls im Projektverzeichnis liegt, da sie den Gesamtkontext liefert.
 3. **Wichtig:** Mache einen Git-Commit (git add . && git commit -m "WIP"), bevor du Aider startest. So kannst du Änderungen leicht rückgängig machen.
## 2. Aider im Terminal starten
Starte Aider im Terminal und übergib direkt die relevanten Kontext-Dateien. Wenn du bereits eine MainActivity.kt oder eine Datei mit deiner Compose-Navigation hast, füge diese ebenfalls hinzu, damit Aider den Wizard dort einhängen kann.
Führe folgenden Befehl in deinem Terminal aus:
```bash
aider MobileIDE.md ProjectWizardPrompt.md

```
*(Optional: Wenn du das architect Modell nutzen willst, was für komplexe neue Features sehr zu empfehlen ist, starte mit aider --architect MobileIDE.md ProjectWizardPrompt.md)*
## 3. Die Anweisung (Prompt) im Aider-Chat
Sobald Aider gestartet ist und im Chat-Modus wartet, gibst du folgende Anweisung ein. Diese sagt Aider genau, was es mit den hinzugefügten Dateien tun soll:
> **Bitte lies dir die Spezifikation in der Datei ProjectWizardPrompt.md genau durch. Berücksichtige dabei auch die Projektarchitektur aus MobileIDE.md.**
> **Deine Aufgabe:**
> **Implementiere das komplette Project Wizard Feature in Kotlin. Erstelle dazu alle geforderten Dateien (TemplateModels, WizardState, WizardViewModel, WizardScreen, ZipUtils) im Ordner app/src/main/java/<DEIN_PACKAGE_PFAD>/ui/projects/.**
> **Hinweise:**
> **1. Erstelle die Verzeichnisstruktur für die Assets (app/src/main/assets/templates/), aber du musst keine echten ZIP-Dateien generieren (das mache ich später manuell). Erstelle nur eine beispielhafte template.json als Mock-Datei für das EmptyComposeActivity-Template, damit wir die Logik testen können.**
> **2. Nutze Jetpack Compose, Coroutines und StateFlow.**
> **3. Denke an die nötigen Imports und halte dich an moderne Android Best Practices.**
> **Bitte generiere jetzt den Code.**
> 
*(Hinweis: Ersetze <DEIN_PACKAGE_PFAD> durch deinen echten Package-Namen, z.B. com/myname/mobileide)*
## 4. Nächste Schritte nach der Generierung
Aider wird nun die Dateien erstellen und vorschlagen, sie zu speichern.
 * **Dependencies prüfen:** Falls Aider Bibliotheken wie kotlinx.serialization oder Gson verwendet, fordere Aider danach auf: *"Bitte füge die benötigten Dependencies für das ViewModel und Serialization zu meiner app/build.gradle.kts hinzu."*
 * **ZIPs erstellen:** Aider kann keine echten ZIP-Archive generieren. Du musst die .zip-Dateien (EmptyActivity.zip, etc.) nach der Code-Generierung selbst erstellen und in den Ordner app/src/main/assets/templates/ legen, damit der Code sie entpacken kann.
