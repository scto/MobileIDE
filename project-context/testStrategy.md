## Test-Strategie
 * **Testing Frameworks:** JUnit 4/5, Kotlin Test, MockK (für Mocking).
 * **UI Testing:** Jetpack Compose UI Testing Framework (für Instrumentierungstests auf dem Gerät).
## Arten von Tests
 * **Unit Tests:** Zwingend für die Domain-Schicht (Use Cases) und Data-Schicht (Repositories, Mapper).
 * **Integration Tests:** Für die Interaktion zwischen Editor-Engine und Dateisystem.
 * **UI Tests:** Für die Überprüfung der Custom Compose Components.
## Ansatz
 * TDD (Test Driven Design) wird bevorzugt. Teste Logik isoliert, ohne den Android-Kontext zu benötigen, wann immer möglich.
## Ausführen von Tests
 * ./gradlew testDebugUnitTest (für lokale Unit-Tests)
 * ./gradlew connectedAndroidTest (für UI-Tests auf dem Emulator/Gerät)
