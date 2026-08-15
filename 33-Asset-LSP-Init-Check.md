Behebe folgende Terminal- und LSP-Fehler im Projekt com.scto.mobile.ide:

FEHLERBERICHT:
1. Terminal-Ausgabe (Termix/Alpine-Umgebung):
   chdir("/home"): No such file or directory
   c: /data/user/0/com.scto.mobile.ide/local/bin/ init-host
   sh /data/data/com.scto.mobile.ide/local/bin/lsp/kotlin.sh: No such file or directory
   [Process completed (code 127) - press Enter]

2. LSP-Status im Editor meldet Fehler (vermutlich Folgefehler, da kotlin.sh fehlt).

3. Vorheriger Crash-Report (Kontext):
   java.lang.NoClassDefFoundError: Lcom/scto/mobile/ide/lsp/LspRegistry;
   Caused by: ClassNotFoundException: com.scto.mobile.ide.lsp.LspRegistry
   -> Bereits behobene/geprüfte Punkte: ProGuard-Keep-Regeln für com.scto.mobile.ide.lsp.**, 
      Multidex-Konfiguration, Modul-Dependency auf :lsp

ANALYSE-HYPOTHESE:
Es scheint ein Asset-Extraktions-Problem vorzuliegen: Beim App-Start (oder bei Terminal-Init 
"init-host") werden Shell-Skripte und Toolchain-Dateien (u. a. lsp/kotlin.sh, /home-Verzeichnis 
für die Alpine/proot-Umgebung) NICHT korrekt aus den App-Assets in das interne Verzeichnis 
/data/data/com.scto.mobile.ide/local/bin/ kopiert bzw. entpackt. Exit-Code 127 = "command not 
found" bestätigt, dass die Datei zur Laufzeit fehlt, obwohl sie im APK als Asset vorhanden 
sein sollte.

AUFGABEN:

1. ASSET-INVENTAR PRÜFEN
   - Durchsuche das Projekt nach dem Assets-Verzeichnis (app/src/main/assets/ oder 
     alpine-Rootfs-Bundle) und liste alle enthaltenen Dateien unter lsp/ auf 
     (insbesondere kotlin.sh, init-host, sowie das Alpine-Basis-Rootfs mit /home-Verzeichnis).
   - Prüfe, ob kotlin.sh und init-host überhaupt im APK/AAB gepackt werden 
     (build.gradle -> sourceSets, aaptOptions noCompress für .sh-Dateien, 
     ggf. androidResources { noCompress += listOf("sh") }).
   - Prüfe, ob .sh-Dateien beim APK-Build durch AAPT/Zipalign komprimiert wurden 
     (das würde die Ausführung als Skript verhindern, wenn sie nicht zuvor entpackt werden).

2. EXTRAKTIONS-LOGIK IM CODE FINDEN UND FIXEN
   - Suche die Stelle im Code (vermutlich TerminalManager, AlpineBootstrap, RootfsInstaller 
     oder AssetExtractor-Klasse), die für das Entpacken der Alpine-Rootfs sowie der 
     lsp/*.sh-Skripte beim ersten Start zuständig ist.
   - Prüfe, ob diese Extraktion:
     a) VOR dem ersten Terminal-/LSP-Start abgeschlossen ist (keine Race Condition 
        zwischen Coroutine-Start der LspRegistry und der Extraktions-Coroutine),
     b) Fehler beim Kopieren korrekt loggt/wirft statt sie zu verschlucken,
     c) einen Marker/Flag-File (z. B. .extracted_v<versionCode>) verwendet, um bei 
        App-Updates eine erneute Extraktion zu erzwingen (sonst bleiben alte/fehlende 
        Dateien nach Update bestehen).
   - Stelle sicher, dass nach dem Entpacken chmod +x auf alle .sh-Dateien im 
     local/bin/-Verzeichnis (inkl. lsp/kotlin.sh und init-host) angewendet wird.

3. /home-VERZEICHNIS FIX (chdir-Fehler)
   - Prüfe die proot/Alpine-Startkonfiguration (Kommandozeilen-Argumente für proot, 
     z. B. -w /home oder chdir-Aufruf im init-host-Skript).
   - Stelle sicher, dass das Verzeichnis /home innerhalb der entpackten Alpine-Rootfs 
     tatsächlich existiert (mkdir -p home im Extraktions-Code oder im Rootfs-Tarball enthalten).
   - Falls /home dynamisch erstellt werden soll, ergänze im Extraktions-/Init-Code:
     File(rootfsDir, "home").mkdirs()

4. LSP-INIT ENTKOPPELN VOM TERMINAL-INIT
   - Prüfe, ob LspRegistry.kt bzw. der Coroutine-Aufruf in MainActivity.kt (Zeile ~196) 
     auf denselben kotlin.sh-Pfad zugreift wie das Terminal.
   - Füge eine Prüfung ein, die vor dem LSP-Start via File(path).exists() && 
     File(path).canExecute() validiert, ob kotlin.sh vorhanden und ausführbar ist. 
     Falls nicht: Auto-Trigger der Asset-Extraktion, dann Retry (max. 3 Versuche mit 
     kurzer Verzögerung), statt sofort mit ClassNotFoundException/Exit 127 zu crashen.

5. DIAGNOSE-SKRIPT ERSTELLEN
   - Erstelle check_lsp_assets.sh, das per adb shell (oder run-as com.scto.mobile.ide) prüft:
     a) Existenz von /data/data/com.scto.mobile.ide/local/bin/lsp/kotlin.sh
     b) Existenz von /data/data/com.scto.mobile.ide/local/bin/init-host
     c) Existenz und Ausführbarkeit (ls -la) beider Dateien
     d) Existenz von <rootfs>/home
   - Ausgabe: klare OK/FEHLT-Liste pro Datei.

6. VALIDIERUNG
   - Baue das Projekt neu (./gradlew assembleDebug).
   - Installiere auf Testgerät/Emulator, starte die App, triggere Terminal- und LSP-Start.
   - Führe check_lsp_assets.sh aus und bestätige, dass alle Dateien nun vorhanden 
     und ausführbar sind sowie /home existiert.
   - Prüfe, ob der LSP-Status im Editor jetzt fehlerfrei ist und das Terminal 
     ohne exit code 127 startet.

Gib am Ende:
- eine Liste aller geänderten/erstellten Dateien mit Diff,
- eine kurze Zusammenfassung der Root Cause (Asset nicht gepackt / nicht entpackt / 
  Race Condition / fehlendes Verzeichnis / Rechte-Problem),
- Bestätigung, ob check_lsp_assets.sh nach dem Fix "alles OK" meldet.
