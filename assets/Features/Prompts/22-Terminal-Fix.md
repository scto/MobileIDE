Behebe die folgenden zwei Crash-Fehler im Terminal-Setup-Flow von MobileIDE
(Modul :features:terminal, Package com.scto.mobile.ide.features.terminal.ui):

=====================================================================
CRASH 1
=====================================================================
java.lang.IllegalStateException: RootFS Datei fehlt nach dem Download.
	at SetupWorker$prepareEnvironment$2.invokeSuspend(SetupWorker.kt:313)

=====================================================================
CRASH 2
=====================================================================
java.lang.IllegalStateException: RootFS Download fehlgeschlagen. Bitte Internetverbindung prüfen.
	at SetupWorker$prepareEnvironment$2.invokeSuspend(SetupWorker.kt:309)
Caused by: java.net.SocketTimeoutException: timeout
	at Downloader.download(Downloader.kt:151)
	at Downloader.downloadRootFs(Downloader.kt:303)
Caused by: java.net.SocketException: Socket closed
	(ConscryptEngineSocket / okhttp3 HttpURLConnectionImpl chain)

Beide Fehler treten während `reinstallTerminal()` -> `prepareEnvironment()` ->
`downloadRootFs()` -> `Downloader.download()` auf und resultieren in einem
Hard-Crash der App (Main-Thread, Compose Coroutine-Scope), statt einer
sauberen, für den Nutzer verständlichen Fehlerbehandlung mit Retry-Option.

=====================================================================
ANALYSE-SCHRITTE (zuerst durchführen)
=====================================================================
1. Öffne und analysiere die vollständige Implementierung von:
   - features/terminal/src/main/java/.../ui/Downloader.kt
     (insbesondere Zeilen 100-320, Methoden `download()`,
     `download$default()`, `downloadRootFs()`, `downloadRootFs$default()`)
   - features/terminal/src/main/java/.../ui/SetupWorker.kt
     (insbesondere Zeilen 180-320, Methoden `prepareEnvironment()`,
     `reinstallTerminal()`)
2. Identifiziere die aktuell verwendete HTTP-Client-Implementierung
   (`com.android.okhttp.internal.huc.HttpURLConnectionImpl` deutet auf
   die Nutzung der veralteten Android-internen okhttp-Bibliothek über
   `HttpsURLConnection`/`URL.openConnection()` hin, NICHT auf die
   Projekt-eigene `okhttp3`-Dependency aus `libs.versions.toml`).
3. Prüfe, ob und welche Timeout-Werte (`connectTimeout`,
   `readTimeout`) aktuell auf der Connection gesetzt sind (vermutlich
   keine oder zu niedrige Werte, was den `SocketTimeoutException` beim
   Lesen der Response-Header erklärt).
4. Prüfe, welche RootFS-Download-URL(s) verwendet werden und ob es
   sich um einen Single-Point-of-Failure-Mirror ohne Fallback handelt.

=====================================================================
FIX 1: Downloader.kt – Robuste Netzwerk-Implementierung
=====================================================================
1. Migriere `Downloader.download()` vollständig von der veralteten
   `java.net.URL` / `HttpsURLConnectionImpl` API auf die im Projekt
   bereits als Dependency vorhandene `okhttp3` Bibliothek (siehe
   `libs.okhttp` in `gradle/libs.versions.toml`), um konsistentes und
   zuverlässigeres Netzwerkverhalten zu garantieren.
2. Konfiguriere einen `OkHttpClient` mit expliziten, großzügigen
   Timeouts, passend für große RootFS-Archive über mobile/instabile
   Verbindungen:
   - connectTimeout = 30 Sekunden
   - readTimeout = 60 Sekunden
   - writeTimeout = 60 Sekunden
   - callTimeout = 0 (kein globales Call-Timeout, da große Downloads
     länger dauern können) oder alternativ ein hoher Wert (z. B. 20 Min)
3. Implementiere automatische Retry-Logik mit exponentiellem Backoff
   (analog zum bereits etablierten Pattern in `setup.sh` für
   `apt-get`, siehe `Crashlog-Fix-2.md`: 3 Versuche, 5s Backoff):
   - Bei `SocketTimeoutException`, `SocketException` (inkl. "Socket
     closed"), oder HTTP-Statuscodes 5xx: bis zu 3 Wiederholungen mit
     steigender Wartezeit (z. B. 3s, 6s, 12s) vor dem endgültigen Fehler.
   - Logge jeden Retry-Versuch verständlich (z. B. via LogCatcher aus
     `com.scto.mobile.ide.core.common.utils.LogCatcher`).
4. Implementiere Download mit Fortschrittsanzeige und Unterstützung
   für Resume/Wiederaufnahme abgebrochener Downloads via HTTP
   `Range`-Header, falls die Zieldatei bereits partiell existiert
   (verhindert erneuten kompletten Download bei Verbindungsabbrüchen).
5. Stelle sicher, dass der Download IMMER zuerst in eine temporäre
   Datei (`<ziel>.part` oder `<ziel>.tmp`) geschrieben wird und erst
   nach erfolgreicher, vollständiger Übertragung (inkl. optionaler
   Checksummen-Prüfung, falls eine SHA256/MD5-Prüfsumme für das RootFS
   verfügbar ist) atomar in die finale Zieldatei umbenannt wird
   (`File.renameTo()` bzw. `Files.move()` mit `ATOMIC_MOVE`). Dies
   behebt direkt CRASH 1 ("RootFS Datei fehlt nach dem Download"), da
   unvollständige/abgebrochene Downloads dadurch nie fälschlich als
   "vorhanden" erkannt werden können.
6. Füge nach Abschluss des Downloads eine explizite Existenz- UND
   Größenprüfung der Zieldatei hinzu (Datei muss existieren UND eine
   plausible Mindestgröße haben, z. B. > 1 MB, um leere/korrupte
   Dateien zu erkennen), bevor `downloadRootFs()` als erfolgreich
   zurückkehrt.
7. Prüfe die konfigurierte(n) RootFS-Download-URL(s) und ergänze,
   falls nur eine einzige URL/Mirror hinterlegt ist, mindestens einen
   Fallback-Mirror. Bei Fehlschlag des Primär-Mirrors automatisch auf
   den nächsten Mirror wechseln, bevor der Gesamtfehler geworfen wird.

=====================================================================
FIX 2: SetupWorker.kt – Fehlerbehandlung & UX
=====================================================================
1. Ersetze das direkte Werfen von
   `IllegalStateException("RootFS Datei fehlt nach dem Download.")`
   und
   `IllegalStateException("RootFS Download fehlgeschlagen. Bitte Internetverbindung prüfen.")`
   (Zeilen ~309 und ~313) durch eine dedizierte, typisierte
   Exception-Klasse, z. B. `RootFsSetupException(message: String, cause: Throwable? = null)`,
   die zwischen folgenden Fehlerkategorien unterscheidet:
   - `NetworkError` (Timeout, Verbindungsabbruch, DNS-Fehler)
   - `IncompleteDownloadError` (Datei fehlt/zu klein nach Download)
   - `StorageError` (kein Speicherplatz, Schreibrechte fehlen)
   - `ChecksumMismatchError` (falls Prüfsummenvalidierung implementiert wird)
2. Fange diese Exception in `prepareEnvironment()` bzw. eine Ebene
   höher in `reinstallTerminal()` ab, sodass der Fehler NICHT als
   ungefangene Exception den Coroutine-Scope terminiert und die App
   crasht, sondern als kontrollierter Fehlerzustand an die UI
   zurückgegeben wird (z. B. über einen `sealed class SetupState` mit
   `SetupState.Error(message: String, isRetryable: Boolean)`).
3. Zeige dem Nutzer bei Netzwerkfehlern eine klare, actionable
   Fehlermeldung mit einem "Erneut versuchen"-Button in der zugehörigen
   Compose-UI (`TerminalSetupUI.kt` bzw. der Setup-Fortschrittsanzeige),
   der `reinstallTerminal()` erneut anstößt, ohne dass die App komplett
   neu gestartet werden muss.
4. Stelle vor dem Download-Start eine grundlegende Konnektivitätsprüfung
   sicher (z. B. via `ConnectivityManager.activeNetwork` Check), um
   dem Nutzer sofort eine verständliche Meldung zu zeigen, falls
   überhaupt keine Internetverbindung besteht, statt erst nach einem
   30-60 Sekunden Timeout zu scheitern.
5. Stelle sicher, dass bei einem fehlgeschlagenen Setup-Versuch
   angelegte Teil-Dateien/-Verzeichnisse (`.part`-Dateien, leere
   RootFS-Verzeichnisse) korrekt aufgeräumt werden, damit ein späterer
   erneuter Versuch nicht durch Altlasten blockiert wird.
6. Ergänze strukturiertes Logging (via `LogCatcher`) für jeden
   Setup-Schritt (Download gestartet, Fortschritt, Retry, Erfolg,
   Fehler) zur besseren Diagnose zukünftiger Probleme.

=====================================================================
TESTS & VERIFIKATION
=====================================================================
1. Schreibe/aktualisiere Unit-Tests (falls Testinfrastruktur im Modul
   vorhanden, z. B. via Robolectric) für:
   - Erfolgreichen Download-Flow
   - Retry-Verhalten bei simuliertem `SocketTimeoutException`
   - Atomares Umbenennen nach unvollständigem Download
   - Fallback-Mirror-Wechsel
2. Führe einen manuellen Testdurchlauf des kompletten
   `reinstallTerminal()`-Flows aus (inkl. simuliertem Verbindungsabbruch,
   z. B. durch Flugmodus während des Downloads), um zu verifizieren,
   dass die App NICHT mehr crasht, sondern eine kontrollierte
   Fehlermeldung mit Retry-Option anzeigt.
3. Stelle sicher, dass der gesamte Build (`./gradlew assembleDebug`)
   nach den Änderungen fehlerfrei durchläuft.

=====================================================================
DOKUMENTATION
=====================================================================
Aktualisiere `PROGRESS.md` mit einem neuen Eintrag unter dem aktuellen
Datum, der folgende Punkte zusammenfasst:
- Migration von Downloader.kt auf okhttp3 mit Retry/Backoff
- Atomares Download-Handling (.part-Datei + Rename) zur Behebung von
  "RootFS Datei fehlt nach dem Download"
- Fallback-Mirror-Unterstützung für RootFS-Downloads
- Neue strukturierte SetupState-Fehlerbehandlung mit Retry-UI statt
  Hard-Crash bei Netzwerkfehlern

Gib abschließend eine Zusammenfassung aller geänderten Dateien mit
kurzer Beschreibung der jeweiligen Änderung aus.
