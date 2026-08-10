# Ziel
Führe die in der Analyse identifizierten, offenen Punkte aus der bereits begonnenen
Modul-Konsolidierung (Prompt "30-consolidate-module-architecture-eliminate-duplicate-
code-paths.md") und die darin übersehenen Lücken in EXAKT VIER voneinander getrennten,
nacheinander auszuführenden Schritten (A, B, C, D) durch.

Jeder Schritt (A, B, C, D) ist EIGENSTÄNDIG abzuschließen und muss folgendes
enthalten, BEVOR der jeweils nächste Schritt startet:
  1. einen vollständigen BERICHT der durchgeführten Änderungen
  2. einen SEPARATEN GIT-COMMIT mit einer klaren, aussagekräftigen Message
  3. einen PUSH dieses Commits
  4. eine EXPLIZITE FRAGE AN DEN BENUTZER, ob der nächste Schritt gestartet werden
     soll (der Prozess MUSS hier anhalten und auf die Antwort des Benutzers warten;
     starte den nächsten Schritt NICHT automatisch).

WICHTIG – Reihenfolge und menschlicher Kontrollpunkt:
Bearbeite die Schritte STRENG in der Reihenfolge A → B → C → D. Nach jedem abgeschlossenen
Schritt gilt: STOPP, Bericht liefern, commit, push, und dann die explizite Frage
"Bitte bitte: Soll Schritt [nächster Schritt] gestartet werden? (ja/nein)" stellen.
Verfahre NICHT selbstständig weiter, bis der Benutzer der nächsten Stufe ausdrücklich
zugestimmt hat.

Kontext (unverändert übernehmen):
Das Projekt MobileIDE durchlief mehrere Migrationswellen (Xed-Editor → WebIDE →
MobileIDE). Prompt 30 zielte darauf ab, redundante Code-Pfade und doppelte Module zu
konsolidieren, damit Bugfix-Prompts künftig konsistent auf exakt EINEN Code-Pfad pro
Funktionsbereich treffen. Verifiziert wurde bisher, dass NUR Stufe 7 von Prompt 30
(Migrationsskripte nach scripts/migrations/archive/) tatsächlich umgesetzt ist. Die
eigentliche Modul-Konsolidierung (insbesondere Stufe 4: LSP-Zusammenführung) wurde
NICHT umgesetzt; sie ist Gegenstand der unten folgenden Schritte.

Die aktuelle Projektstruktur ist in der Datei MobileIDE_20260808_161840.md dokumentiert
und liefert die maßgebliche Grundlage für die Analyse jeder Stufe. Missverstehe die
vorherige Behauptung in einem früheren Report ("Stufe 4 vollständig abgeschlossen")
NICHT als Bestätigung für die folgende Arbeit: In jenem Report wurde nur ein Laufzeit-Bug
(Installations-Pfad) behoben, NICHT die geforderte Architektur-Konsolidierung der LSP-
Module.

═══════════════════════════════════════════════════════════════
SCHRITT A – "LSP-Fehler"-Statusindikator bei Kotlin: echte Ursachenanalyse
═══════════════════════════════════════════════════════════════

Dies ist ein reiner ANALYSE- und DIAGNOSE-Schritt. Es dürfen dabei keine dauerhaften,
nicht verifizierten Code-Änderungen vorgenommen werden, die nicht durch einen konkreten
Test abgesichert sind.

AUFGABE:
A1. Finde und beantworte, warum beim Öffnen einer Kotlin-Datei weiterhin der Status
    "● LSP Fehler" (bzw. der entsprechende rote Indikator) in der unteren Statusleiste
    erscheint, obwohl der LSP-Installationslauf des Sprachservers inzwischen
    funktioniert (Exit-Code 0 laut früherem Schritt 4-Report). Es existieren zwei
    Prompt-Dateien im Repo-Root (preview.md und urce-files.md), die dasselbe Kotlin-
    LSP-Problem beschreiben, aber dessen Ursache niemals abschließend festgestellt
    haben (in beiden ist Stufe 0.4 als ungeklärt geführt). Stelle die Ursache JETZT
    anhand des tatsächlichen Codes fest.
A2. Prüfe konkret den Start-/Initialisierungspfad des Kotlin-Language-Servers:
    - Wird der Server wirklich gestartet (Prozess vorhanden, binary gefunden,
      classpath korrekt)?
    - Welchen Exit-Code/Status meldet der LSP-Prozess beim Starten?
    - Kommt es zu einem Konflikt, weil FÜR DIESELBE SPRACHE KOTLIN MEHRERE
      Registrierungspfade existieren (siehe dazu auch Schritt C) und EINER davon
      einen Fehlerstatus liefert, obwohl ein anderer funktioniert?
    - Ist der "LSP Fehler"-Indikator an den richtigen Start-/Lifecycle-Status
      gekoppelt, oder wird er durch eine veraltete/falsche Statusquelle gespeist?
A3. Behebe die tatsächlich gefundene Ursache (regressionsfest), z. B. Fehler bei der
    Initialisierung, falscher Statusflow, unvollständiger Start. Stelle sicher, dass
    das Öffnen einer Kotlin-Datei nach dem Fix einen korrekten, positiv gemeldeten
    LSP-Lifestatus zeigt und KEIN "LSP Fehler".
A4. Teste den Fix gegen eine Kotlin-Datei sowie (stichprobenartig) gegen Java-, Python-
    und Bash-Datei, um festzustellen, dass keine Regression entsteht.

Bericht A: Zeige Ursache, betroffene Datei(en)+Zeilen, den durchgeführten Fix, und ein
Protokoll der LSP-Start-/Statusausgaben vor und nach dem Fix.

═══════════════════════════════════════════════════════════════
SCHRITT B – Prompt 30 sauber zu Ende bringen
═══════════════════════════════════════════════════════════════

Bringe die in Prompt 30 definierten, NOCH NICHT umgesetzten Stufen zu einem sauberen
Ende. Führe dazu die folgenden Unterpunkte aus (jeweils mit Build-Verifikation):

B1. Modul-Stand feststellen: Lies settings.gradle.kts aus dem Projekt und vergleiche
    mit dem Projekt-Tree. Stelle fest, welche der in Prompt 30 geforderten Soll-Zustände
    (Stufen 1-6, 8, 9) weiterhin unerfüllt sind.
B2. Stufe 4.2 UMSETZEN (kritischste Stufe): Verschmelze die Module
    :extension-languages und :editor-lsp zu EINEM Modul :features:lsp. Übertrage alle
    Inhalte konsistent, aktualisiere settings.gradle.kts und alle Build-/Include-
    Abhängigkeiten. Achtung: Deine App.-Modul-Instanz (MainActivity/App-Navigation)
    darf dabei nicht ihren funktionierenden Build verlieren.
B3. Stufe 8: Aktualisiere README.md UND README_DE.md, sodass sie den tatsächlichen
    Ist-Zustand der Module widerspiegeln (insbesondere: :features:lsp statt der
    getrennten :editor-lsp / :extension-languages). Entferne alle Verweise, die weiter
    fälschlich :editor-lsp als eigenständiges Modul nennen.
B4. Stufe 1, 2, 3, 6: Kläre den Status von :features:git, :features:runner, den
    Terminal-Submodulen (mobileide-cli, proot, link2symlink) sowie :features:settings.
    Falls sie deinen Befunden nach verwaist/leer/obsolet sind: Entferne sie sauber aus
    settings.gradle.kts (und ggf. ihrer Quellverzeichnisse). Falls sie noch gebraucht
    werden: behalte sie, kennzeichne den Soll-Zustand.
B5. Stufe 9: Führe eine Abschluss-Verifikation durch (schleife einmal durch alle
    Submodule, Baue, Teste einen Sprachserver sowie einen APK-Build). Dokumentiere den
    final modularen Ist-Zustand.

Bericht B: Zeige für jede Unterplanung (B1-B5) detailliert: Ausgangs-Zustand, ½
Durchführung, geänderte Dateien, Ergebnis. Zeige insbesondere das DIFF von
settings.gradle.kts vorher/nachher.

═══════════════════════════════════════════════════════════════
SCHRITT C – LSP-Vierfach-Verifikation inkl. kotlin-kmp-lsp (Prompt-30-Lücke)
═══════════════════════════════════════════════════════════════

Hintergrund: Prompt 30 betrachtete nur :plugins:kotlin-lsp / :plugins:java-lsp und
übersah die aktive Modul-Referenz :plugins:kotlin-kmp-lsp im include()-Block des
settings.gradle.kts. Das führt dazu, dass für Kotlin MÖGLICHERWEISE mehrere
Registrierungspfade parallel existieren (:core:lsp, :extension-languages /
:features:lsp, :editor-lsp, :plugins:kotlin-kmp-lsp). Dies kann einen Konflikt bzw.
Fehlerstatus erzeugen, der zu dem unter Schritt A untersuchten "LSP Fehler"-Indikator
beitragen kann.

AUFGABE:
C1. Führe eine vollständige Vierfach-Verifikation für die Programmier-Sprache Kotlin
    durch: Ermittle für JEDEN der genannten vier möglichen Pfade, ob und wie er den
    Kotlin-LSP-Server registriert/startet.
C2. Bestimme, ob :plugins:kotlin-kmp-lsp tatsächlich für Kotlin-LSP benötigt wird
    oder ob die Funktionalität bereits durch :features:lsp / :extension-languages
    abgedeckt ist.
C3. Falls es mehrfache/parallele Registrierungspfade gibt: Konsolidiere sie auf EINEN
    einzigen. Entferne Duplikate sauber aus settings.gradle.kts und Quellcode.
    Stelle sicher, dass nach der Konsolidierung genau EINE Quelle existiert, die die
    Kotlin-LSP-Initialisierung übernimmt.
C4. Verifiziere anschließend mit einem Kotlin-Datei-Test: genau EIN LSP-Fehlerstatus
    bleibt vermieden, Server startet korrekt, keine weiteren Konflikte.

Bericht C: Zeige die vier geprüften Pfade, deren Status, den finalen konsolidierten
einzigen Pfad und das Ergebnis des End-to-End-Tests.

═══════════════════════════════════════════════════════════════
SCHRITT D – Plugin-Migrationsreste bereinigen (zig, rust, python)
═══════════════════════════════════════════════════════════════

Hintergrund: migrate_xed.py hat bei der früheren Migration nur features/* prozessiert,
nicht plugins/*. Dadurch existieren in den Plugin-Modulen alte API- und Xed-Referenzen
weiter, die konsistent zu :features:platform-SDK / com.scto.mobile.ide.* umgestellt
werden müssen.

AUFGABE:
D1. Migriere :plugins:zig-lsp vollständig von den alten com.rk.*-APIs auf die
    einheitliche com.scto.mobile.ide.*-API:
    com.rk.file.BuiltinFileType, com.rk.file.sandboxHomeDir, com.rk.exec.isTerminalInstalled,
    com.rk.runner.RunnerManager, com.rk.icon, com.rk.exec.launchTerminal.
D2. Migriere :plugins:rust-lsp: Entferne die verbliebenen alten com.rk.-Referenzen im
    Graderdings-/Decodierungsteil und vereinheitliche die Package-Basis auf
    com.scto.mobile.ide.*.
D3. Korrigiere :plugins:python-lsp:
    - Korrigiere den fehlerhaften namespace/applicationId ("com.scto.mobile.ide.xededitor"/"
      com.scto.mobile.ide.demo") auf den konsistenten, korrekten Namen.
    - Ersetze alle schemas/schema.json sowie README.md Referenzen auf "Xed-Editor" durch
      "MobileIDE".
D4. Vereinheitliche die Gradle-/AGP-Toolchain über alle Plugins hinweg (aktuell
    divergieren zig-lsp und rust-lsp mit AGP/Gradle 9.2.0/9.5.1 von den übrigen
    Plugins). Bringe alle auf dieselbe, für das gesamte Projekt gültige Version.
D5. Baue nach jeder Migration die betroffenen Module, und verifiziere, dass sie sauber
    kompilieren und gegen den :features:platform-SDK-Standard laufen.
D6. Prüfe abschließend, ob im gesamten Repo noch irgendwo "Xed-Editor"-Verweise, alte
    "com.rk."-Importe oder doppelte LSP-/Install-Skripte existieren (auch außerhalb der
    drei genannten Plugins). Nenne sie und bereinige sie, sofern sie tatsächlich
    veraltet sind.

Bericht D: Zeige je Plugin (zig, rust, python) die Liste der migrierten Dateien, die
ersetzten API-Referenzen, die korrigierten Namespaces/Versionsnummern, und das Ergebnis
der Kompilierung.

═══════════════════════════════════════════════════════════════
ABSCHLUSS-Report nach Schritt D
═══════════════════════════════════════════════════════════════
Nach Abschluss von D liefere einen Gesamt-Report, der für A, B, C und D separat
bestätigt, welche Punkte durchgeführt wurden, mit Commit-Hashes (AB, B, C, D), und
zusammenfasst, welche ursprünglichen Duplikate/Migrationsreste im Repo verbleiben, sofern
irgendwelche.

═══════════════════════════════════════════════════════════════
FÜHRUNGSAUFLAGEN (verbindlich für ALLE Schritte)
═══════════════════════════════════════════════════════════════
1. Arbeite ausschließlich auf Branch bearbeitend, aber halte stets den aktuellen
   Stand (keine Force-Pushes).
2. Nach jedem Schritt (A, B, C, D):
   · Rufe ./gradlew build bzw. das entsprechende Build-Target auf, um Fehler zu
     prüfen.
   · Committe mit einer aussagekräftigen Message, die den Schritt benennt
     (z. B. "Prompt31 - Schritt A: LSP-Fehlerstatus bei Kotlin beseitigt").
   · Push.
   · Stoppe und stelle dem Benutzer die explizite Frage, ob der nächste Schritte
     gestartet werden soll. Warte auf die Antwort.
3. Mache KEINEN Schritt automatisch weiter, wenn der vorherige nicht explizit
   freigegeben wurde.
4. Ändere nichts, was nicht zur jeweils aktuellen Stufe gehört. Konsolidierung
   meint: genau ein Code-Pfad pro Funktionsbereich.
