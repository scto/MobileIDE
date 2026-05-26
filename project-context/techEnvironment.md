## Entwicklungs- & Tech-Umgebung
 * **Sprache:** Kotlin 2.2.0 (100% Kotlin Projekt)
 * **Build-System:** Gradle 8.11.2 (Kotlin DSL Scripts: build.gradle.kts)
 * **SDKs:** Android Min SDK 26, Target SDK 35, Compile SDK 36.
 * **Java:** JDK 17 erforderlich.
Die Abhängigkeiten werden zentral über einen Version Catalog (gradle/libs.versions.toml) verwaltet.
### Setup-Prozess
 1. Repository klonen.
 2. Projekt in Android Studio (Ladybug oder neuer) öffnen.
 3. Gradle-Sync abwarten (lädt Compose BOM, Hilt, etc. herunter).
 4. App auf einem Gerät/Emulator (mind. Android 8.0) ausführen.
### Setup Notizen
 * Manuelle Service-Locators sind verboten; alles muss über Dagger-Hilt (@Inject, @Module, @InstallIn) bereitgestellt werden.
 * Die App benötigt umfassende Dateisystem-Berechtigungen (MANAGE_EXTERNAL_STORAGE bei Android 11+), um Repositories lokal klonen zu können. Dies wird vom :core:onboarding Modul gehandhabt.
### Fehlersuche
 * Bei Namespace-Problemen (z. B. "R is not resolved"): Prüfe, ob die build.gradle.kts des jeweiligen Moduls den korrekten Namespace (z.B. com.scto.mcs.feature.settings) gesetzt hat.
 * Kompilierfehler in Compose: Stelle sicher, dass die Jetpack Compose Compiler Version zur Kotlin-Version (2.2.0) passt.
