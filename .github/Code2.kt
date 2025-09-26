Ich kann kein komplettes Android-Projekt erstellen und dir liefern. Ich bin ein großes Sprachmodell und kann keine Dateien erzeugen oder ein Android Studio Projekt für dich einrichten.  Ich kann dir aber den vollständigen Code für ein solches Projekt geben, den du dann selbst in Android Studio einfügen und ausführen musst.

**1. Neues Projekt in Android Studio erstellen:**

Erstelle ein neues Android Studio Projekt mit diesen Einstellungen:

* **Empty Compose Activity**
* **Sprache:** Kotlin
* **Minimale SDK-Version:**  Wähle eine passende Version (z.B. API 21)

**2. Code ersetzen:**

Ersetze den gesamten Inhalt deiner `MainActivity.kt` Datei mit dem Code aus meiner vorherigen Antwort.  Achte darauf,  `com.yourpackage.name` durch deinen tatsächlichen Package-Namen zu ersetzen.

**3. Abhängigkeiten hinzufügen:**

Gehe in die Datei `build.gradle.kts` (Module: app) und füge die folgenden Abhängigkeiten hinzu (wenn nicht schon vorhanden):


```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.navigation:navigation-compose:2.7.5") //Navigation hinzufügen
}
```

**4.  Imports hinzufügen:**

Stelle sicher, dass du alle benötigten Imports in deiner `MainActivity.kt` Datei hast, inklusive:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
```


**5. Projekt synchronisieren und ausführen:**

Synchronisiere dein Projekt in Android Studio und führe es dann auf einem Emulator oder einem angeschlossenen Android-Gerät aus.


Dieser Prozess erfordert, dass du Android Studio installiert hast und mit der Erstellung von Android-Projekten vertraut bist.  Wenn du Probleme hast, musst du dich an die Android Studio Dokumentation wenden oder nach spezifischen Fehlern suchen, die auftreten.  Ich kann dir bei der Fehlerbehebung helfen, wenn du mir den Fehlercode zeigst.