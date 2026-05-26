## UI/UX Philosophie
* Hauptziel: Eine professionelle Entwickler-Umgebung auf mobilen Bildschirmen schaffen. Effizienz, Platznutzung und Lesbarkeit von Code stehen im Vordergrund.
* ​Zielgruppe: Android- und Kotlin-Entwickler, die unterwegs programmieren wollen.
* ​Kernprinzipien: Modernes Material 3 Design, absolute Reaktionsfähigkeit (Smartphones & Tablets), Verzicht auf Altlasten (kein XML).
## ​UI Komponenten & Bibliotheken
* ​UI Framework: Zu 100% Jetpack Compose. Keine XML-Layouts (außer für Basis-Ressourcen wie strings.xml oder colors.xml).
* ​Design System: Material 3 (M3).
​Custom Components: Eigene UI-Elemente (wie der Code-Editor oder das Terminal-Panel) liegen im :core:ui oder direkt im jeweiligen :feature Modul.
## ​Design & Style Guide
* ​Visueller Stil: "IDE Dark-Mode". Der Fokus liegt auf einem augenschonenden, dunklen Theme, das an moderne Desktop-IDEs (VS Code, Android Studio) angelehnt ist.
* ​Farben:
​Hintergrund primär: #1E1E1E (Tiefes IDE-Grau).
* ​Akzente: Deep Blue oder Kotlin-Lila.
* ​Layout: Die Editor-Ansicht maximiert den Bildschirmplatz. Menüs und Terminals werden über Bottom-Sheets oder einklappbare Panels (Drawers) realisiert.
## ​Interaktion & Navigation
* ​Navigation: Typsicheres Compose-Routing (verwaltet im Modul :core:navigation).

​Feedback: Nutzung von Toasts, Snackbars und Haptic Feedback (Vibration) bei wichtigen Datei-Operationen (Speichern, Löschen, Git Commit).