### Prompt-Datei: `04-compose-layout-preview-module.md`

# Ziel
Vervollständige/implementiere das Submodul `:features:layout-preview`, das eine grafische 
Live-Vorschau von Jetpack-Compose-@Composable-Funktionen direkt im Editor anzeigt.

# Kontext
- Aktuell existiert laut settings.gradle.kts KEIN eigenes layout-preview-Modul – dies ist 
  daher primär Neuentwicklung, nicht Fertigstellung eines Fragments.
- Editor-Basis: sora-editor Integration in :editor, Tab-Verwaltung über EditorManager.

# Anforderungen
1. Neues Modul :features:layout-preview (Android-Library):
   - PreviewRenderer: Nutzt einen isolierten Compose-Render-Kontext (z. B. via ComposeView 
     in einem unsichtbaren/gehosteten Fenster oder – falls Volltext-Kompilierung zu teuer ist 
     – über eine On-Device-Kotlin-Skript-Auswertung mittels bereits vorhandener 
     quickjs-android/kotlin-reflect Dependencies als Fallback-Strategie evaluieren).
   - Da echtes Kotlin-Compile-on-Device für Live-Preview sehr teuer ist: Implementiere primär 
     einen "Build-basierten" Preview-Modus: bei Trigger wird ein minimaler, isolierter 
     Gradle-Task (`:app:compilePreviewKotlin` o. ä., ggf. via In-Memory-Kotlin-Compiler-Daemon) 
     ausgeführt, der die betroffene Datei + Preview-Wrapper-Activity kompiliert und im 
     Hintergrund als Bitmap rendert (Screenshot-Testing-Ansatz, analog Compose Preview Screenshot 
     Testing Libraries). Dokumentiere in einem README diese Design-Entscheidung explizit, 
     da eine reine In-App-Compose-Interpretation ohne Compiler technisch nicht robust möglich ist.

2. Erkennung im Editor (:editor / :app CodeEditScreen):
   - Bei geöffneter .kt-Datei: Scanne den sichtbaren/gesamten Dateitext per Regex/leichtgewichtigem 
     Parser auf `@Composable` Annotationen und extrahiere Funktionssignaturen ohne Parameter 
     (Preview-fähig) bzw. mit `@Preview`-Annotation.
   - Falls mindestens eine gefunden wird: zeige ein Preview-Icon (z. B. Icons.Outlined.Visibility) 
     oben rechts in der Editor-TopAppBar.

3. UI – Preview BottomSheet:
   - Aufklappbares BottomSheet (analog Build-Tab-Muster aus Prompt 01) mit:
     - Dropdown zur Auswahl der Composable-Funktion (falls mehrere im File).
     - Gerendertes Bild/Canvas der Composable-Ausgabe.
     - Refresh-Button (manueller Re-Render-Trigger) + optional Toggle "Auto-Refresh bei Speichern".
     - Lade-/Fehlerzustände (z. B. "Kompilierungsfehler" mit Fehlermeldung, falls Preview-Build 
       fehlschlägt).

4. Performance:
   - Preview-Renderings dürfen die UI nicht blockieren – vollständig auf Dispatchers.IO/ 
     Hintergrund-Thread mit Kotlinx-Coroutines, Ergebnis-Bitmap wird über einen State 
     zurück in den Compose-Tree gegeben.

# Akzeptanzkriterien
- Öffnen einer .kt-Datei mit mind. einer @Composable-Funktion zeigt zuverlässig das Preview-Icon.
- Klick auf Icon + Auswahl einer Funktion liefert innerhalb einer nachvollziehbaren Zeitspanne 
  (dokumentiere Ziel-SLA, z. B. < 10s für kleine Composables) eine visuelle Darstellung oder 
  eine klare, lesbare Fehlermeldung.