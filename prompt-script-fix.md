Behebe folgenden Distro-Mismatch-Bug in com.scto.mobile.ide (Termix/Sandbox-Terminal):

BEFUND:
- Environment-Variable MOBILEIDE_DISTRO=ubuntu
- /data/data/com.scto.mobile.ide/local/ubuntu/ ist vollständig provisioniert (28 Einträge, 
  aktueller Timestamp), sandbox-Symlink zeigt korrekt darauf, Shell startet erfolgreich 
  mit "root@localhost:~ #".
- /data/data/com.scto.mobile.ide/local/alpine/ enthält NUR einen leeren Unterordner "root", 
  KEIN /home, KEIN /etc, KEIN /usr, KEIN /bin — die Rootfs-Extraktion ist unvollständig 
  bzw. abgebrochen.
- Ursprünglicher Fehlerreport zeigte in der Termix-UI die Distro-Auswahl "alpine" (Tab-Leiste: 
  "Termix | alpine | +"), was zum Fehler chdir("/home"): No such file or directory und 
  Exit-Code 127 führte, weil die alpine-Rootfs nicht vollständig war.

AUFGABEN:

1. UI/STATE-KONSISTENZ PRÜFEN
   - Finde die Stelle im Code (TerminalFragment/TermixView/SessionManager o. ä.), die 
     die aktuell ausgewählte Distro in der Tab-Leiste anzeigt ("alpine" vs. "ubuntu").
   - Prüfe, ob dieser UI-State synchron mit der Umgebungsvariable MOBILEIDE_DISTRO ist, 
     die tatsächlich an init-host übergeben wird.
   - Falls der Nutzer im UI "alpine" auswählen kann, MUSS vor dem Terminal-Start 
     sichergestellt werden, dass export MOBILEIDE_DISTRO=alpine gesetzt wird UND die 
     zugehörige Rootfs bereits vollständig extrahiert wurde (nicht nur ein Platzhalter-Ordner).

2. ALPINE-EXTRAKTION REPARIEREN
   - Finde den Code, der die Alpine-Rootfs entpackt (vermutlich in shared_extraction.sh 
     oder einer Kotlin/Java-Klasse, die ein Tarball-Asset wie alpine-rootfs.tar.gz oder 
     alpine-minirootfs-*.tar.gz nach local/alpine/ extrahiert).
   - Prüfe, warum die Extraktion nur den Ordner "root" erzeugt hat – mögliche Ursachen:
     a) Der Tarball selbst ist unvollständig/beschädigt im APK-Assets-Verzeichnis 
        (Größe prüfen, ggf. neu herunterladen/bundlen),
     b) Die Extraktions-Logik bricht bei einem bestimmten Pfad/Symlink/Permission-Fehler 
        ab, ohne den restlichen Tarball weiter zu entpacken (Exception wird geschluckt 
        statt geloggt),
     c) Es wird nur ein Teil-Tarball (z. B. nur /root aus einem gesplitteten Archiv) 
        referenziert statt des vollständigen Alpine-minirootfs.
   - Ergänze robustes Error-Handling: Falls die Extraktion fehlschlägt, MUSS ein klarer 
     Fehler/Toast/Log ausgegeben werden ("Alpine-Setup fehlgeschlagen: <Grund>"), statt 
     stillschweigend eine unvollständige Rootfs zurückzulassen.
   - Nach erfolgreicher Extraktion: Validierung einbauen, die prüft, ob mindestens 
     /home, /etc, /usr, /bin in local/alpine/ existieren, bevor der Marker 
     .extracted_v<N> für Alpine gesetzt wird. Falls nicht, Extraktion als fehlgeschlagen 
     markieren und beim nächsten App-Start automatisch erneut versuchen.

3. FEHLENDES /home NACHTRÄGLICH ANLEGEN (Sofort-Fix / Fallback)
   - Ergänze in init-host bzw. im Setup-Skript nach dem mkdir -p "$DISTRO_DIR" zusätzlich:
     mkdir -p "$DISTRO_DIR/home" "$DISTRO_DIR/root" "$DISTRO_DIR/tmp"
   - Dies verhindert zumindest den chdir("/home")-Crash als Sofortmaßnahme, auch wenn 
     die eigentliche Alpine-Extraktion noch reparaturbedürftig ist.

4. DISTRO-SWITCH ABSICHERN
   - Baue eine Prüfung ein: Bevor ein Nutzer im UI zu einer Distro wechselt (Tab-Auswahl 
     "alpine" vs. "ubuntu" vs. weitere), muss geprüft werden, ob local/<distro>/home 
     existiert. Falls nicht: Extraktion automatisch anstoßen und Ladeindikator anzeigen, 
     statt direkt eine kaputte Shell zu starten.

5. DIAGNOSE-SKRIPT
   - Erstelle check_distro_rootfs.sh, das für jede unter local/ vorhandene Distro 
     (ubuntu, alpine, etc.) prüft: Existenz von home, etc, usr, bin, und die Gesamt-
     Anzahl an Dateien/Ordnern. Ausgabe als Tabelle: Distro | Vollständig (Ja/Nein) | 
     Fehlende Verzeichnisse.

6. VALIDIERUNG
   - Baue das Projekt neu, installiere auf Testgerät.
   - Wechsle im UI explizit zu "alpine", starte das Terminal.
   - Bestätige: kein chdir-Fehler mehr, kotlin.sh/init-host laufen korrekt, 
     check_distro_rootfs.sh meldet "alpine: Vollständig = Ja".
   - Wiederhole denselben Test für "ubuntu" zur Regressionsprüfung (darf nicht 
     kaputt gehen).

Gib am Ende:
- Diff aller geänderten Dateien (init-host, shared_extraction.sh, UI-State-Klasse, 
  neues Skript check_distro_rootfs.sh),
- klare Aussage zur Root Cause (unvollständige Alpine-Extraktion / State-Mismatch 
  zwischen UI und ENV-Variable / beides),
- Bestätigung, dass sowohl ubuntu als auch alpine jetzt fehlerfrei starten.
