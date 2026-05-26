## System-Architektur
 * **Ansatz:** Multi-Modul Clean Architecture für Android.
 * **Schlüsselkomponenten:**
   * :app - Die Application-Klasse und Hilt-Root-Komponente.
   * :core:* - Grundbausteine (Data, Domain, Network, Editor-Engines, Terminal-Umgebung).
   * :feature:* - Gekapselte UI-Bildschirme (Onboarding, Dashboard, Settings, Code-Editor).
 * **Kommunikation:** Jegliche Kommunikation zwischen Daten- und UI-Schicht verläuft zwingend über die Domain-Schicht (:core:domain).
## Kern-Frameworks
 * Kotlin 2.2.0
 * Jetpack Compose für die gesamte UI.
 * Dagger-Hilt für Dependency Injection.
 * JGit (6.8.0) für Versionskontrolle.
 * Kotlin Coroutines & Flow für Asynchronität.
## Design Patterns
 * Repository Pattern (Data-Layer).
 * Use Cases (Domain-Layer).
 * MVVM / MVI mit ViewModels (UI-Layer).
