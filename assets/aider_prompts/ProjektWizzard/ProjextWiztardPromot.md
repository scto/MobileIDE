# Context & Role

Du bist ein Senior Android Developer und Architekt, spezialisiert auf den Bau einer modernen On-Device Android/Java IDE (ähnlich wie CodeAssist oder AndroidIDE).
Deine Aufgabe ist es, das Projekt-Erstellungs-Feature (Project Wizard) im Kotlin-Code zu implementieren.
Beachte für den Gesamtkontext die Datei MobileIDE.md (Projektübersicht). Alle neuen Codes müssen zwingend in Kotlin geschrieben sein (Jetpack Compose für UI, Coroutines/Flow für asynchrone Logik).

# Feature Overview

Erstellung eines WizardScreen (UI) und eines WizardViewModel (Logik) im Modul app unter dem Pfad ui/projects.
Der Wizard erlaubt es dem Nutzer, ein neues App-Projekt basierend auf vordefinierten Templates zu erstellen, die als .zip-Archive im assets-Ordner der App liegen.

## 1. Asset & Template Architektur

Die Templates liegen im Ordner app/src/main/assets/templates/ als ZIP-Archive:
 * EmptyActivity.zip
 * EmptyComposeActivity.zip
 * NavigationDrawer.zip
 * BottomNavigation.zip
 * CppProject.zip
 * LibGDXProject.zip
 * BasicJavaApp.zip (und weitere)
**Struktur innerhalb jedes ZIP-Archivs:**
 * template.json: Beschreibt die Metadaten und Eigenschaften des Templates.
 * src/: Beinhaltet den Template-Quellcode (mit Platzhaltern wie ${PACKAGE_NAME}).
 * res/: Beinhaltet die Android-Ressourcen.
 * gradle/: Beinhaltet den Gradle Wrapper und den Versionskatalog (libs.versions.toml).
 * 
### Beispiel für template.json:

```json
{
  "id": "empty_compose_activity",
  "name": "Empty Compose Activity",
  "description": "Erstellt ein leeres Projekt mit Jetpack Compose Setup.",
  "properties": {
    "useKotlin": true,
    "useCompose": true,
    "useGradleKotlin": true,
    "minSdk": 24,
    "targetSdk": 34
  }
}

```

## 2. Implementierungs-Anforderungen

### Schritt 1: Datenmodelle (TemplateModels.kt)

Erstelle Kotlin Data Classes, die das template.json abbilden (z.B. mit kotlinx.serialization oder Gson).

### Schritt 2: WizardViewModel (WizardViewModel.kt)

Das ViewModel muss modern mit MVI/MVVM und StateFlow aufgebaut sein und folgende Logiken beinhalten:
 1. **Template Discovery:** Auslesen des assets/templates/ Ordners und Entpacken der template.json Dateien in den Arbeitsspeicher, um eine Liste der verfügbaren Templates im UI anzuzeigen.
 2. **Project Configuration State:** Speichern der Nutzereingaben (Project Name, Package Name, Minimum SDK, Save Location).
 3. **Extraction Engine (Coroutines/Dispatchers.IO):** - Entpacken des ausgewählten ZIP-Archivs in das Zielverzeichnis (Save Location).
   * Ausführen eines String-Replacements (z.B. ersetzen von ${PACKAGE_NAME} durch com.example.app in allen .kt, .xml und .gradle.kts Dateien).
   * Umstrukturierung des src-Ordners entsprechend dem Package-Namen (z.B. src/main/java/com/example/app/).
 4. **Gradle Downloader:** Eine Suspend-Funktion downloadLatestGradle(), die asynchron die aktuellste/benötigte Gradle-Distribution herunterlädt, entpackt und in den lokalen IDE-Cache legt, falls sie nicht schon existiert.
 5. 
### Schritt 3: WizardScreen (WizardScreen.kt)

Ein smartes Jetpack Compose UI, das Smartphone-freundlich (touch-optimiert) ist.
 * **State-Handling:** Konsumieren des StateFlows aus dem WizardViewModel.
 * **Flow:**
   * **Screen 1 (Template Selection):** Grid oder vertikale Liste der Templates (mit Icons und Beschreibung aus der JSON).
   * **Screen 2 (Project Details):** Formular für Projektname, Package-Name (automatisch abgeleitet vom Projektnamen), und Pfadauswahl.
   * **Screen 3 (Loading/Generation):** Progress-Indikator, der den Entpack-Vorgang und ggf. den Gradle-Download anzeigt.
 * **Design:** Modernes Material Design 3, abgerundete Ecken, klare Typografie. Extrahierbare Komponenten, um sie später zu erweitern.
 * 
## 3. Anweisungen für die Code-Generierung

Bitte generiere nun den Code für die folgenden Dateien komplett und lauffähig:
 1. Template.kt (Data Classes)
 2. WizardState.kt (UI States & Events)
 3. WizardViewModel.kt (Inklusive Entpack-Logik, String-Replacement und Gradle-Download-Skeleton)
 4. WizardScreen.kt (Compose UI mit Navigation zwischen den Wizard-Schritten)
 5. Eine Hilfsklasse ZipUtils.kt, die das sichere Entpacken und Manipulieren der Dateien via java.util.zip oder java.nio.file übernimmt.
 6. 
Schreibe robusten, gut kommentierten Code, der Exception-Handling (z.B. bei fehlenden Asset-Dateien oder Storage-Permissions) beinhaltet. Die Architektur soll mächtig genug sein, um später komplexere Scaffolding-Engines (wie Freemarker oder Apache Velocity) nachzurüsten, falls einfaches String-Replacement nicht mehr ausreicht.
