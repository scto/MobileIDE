### Prompt-Datei: `16-ai-coding-assistant-integration.md`

# Ziel
Vervollständige und integriere das bestehende Package 
app/src/main/java/com/scto/mobile/ide/ui/editor/aicoding zu einem vollwertigen, 
nativen KI-Coding-Assistenten in der Compose-UI, der als Frontend für das im Projekt 
vorhandene Aider-CLI-Backend (scripts/aider_launcher.sh) dient, und binde ihn sinnvoll 
in den Editor sowie das bestehende Tab-Panel-System ein.

# Kontext
- scripts/aider_launcher.sh ist ein interaktives Bash-Menü, das das Python-Tool 
  "Aider" mit folgenden Providern/Modellen startet: Gemini (Flash-Reihe: 3.1/3/2.5/2.0/1.5, 
  Pro-Reihe: 3.1/3/2.5/2.0/1.5), OpenAI (o3-mini, GPT-4o, GPT-4o-mini), DeepSeek 
  (deepseek-chat, deepseek-reasoner), Claude (3.5 Sonnet, 3.5 Haiku, 3 Opus).
- Chat-Modi: auto, code, architect, ask, help (siehe CHAT_MODES-Array im Skript).
- API-Keys werden aus Secrets-Dateien geladen: 
  ~/.gemini_api_key.secrets, ~/.anthropic_api_key.secrets, 
  ~/.openai_api_key.secrets, ~/.deepseek_api_key.secrets 
  (Pfad-Basis: /data/data/<package>/files/home).
- Aider nutzt ein Python-VENV unter $HOME_DIR/.venv und liest projektspezifische 
  Kontextdateien automatisch mit ein: activeDevelopment.md, progress.md, systemDesign.md, 
  testStrategy.md, uiStrategy.md, techEnvironment.md, sowie eine optionale .aider.conf.yml.
- Zusätzliche Flags: --subtree-only (Sub-Tree Only Modus), --browser (für Computer-Use-Modelle).
- Das Projekt hat bereits ein etabliertes Tab-Panel-Muster in :core:tooling:tooling-impl 
  mit den Tabs Terminal, Problems, IDE, Build, LSP (siehe PROGRESS.md 2026-06-29 
  "Categorized Logs sheet").
- Die Ausführung von Bash-Skripten im Sandbox-Kontext läuft über die etablierten 
  Exec-/Terminal-Bridge-Muster (siehe ShellUtils, ubuntuProcess, launchTerminal aus 
  com.scto.mobile.ide.exec, genutzt z. B. von TypstInstallationManager.kt als Referenz).

# Anforderungen

## 1. AiderBridgeService (Backend-Logik)
- Neue Klasse, sinnvoll platziert in :core:tooling:tooling-server (analog zur 
  Gradle-Bridge) oder in einem neuen :features:aicoding-Modul, falls das bestehende 
  aicoding-Package bereits substanzielle eigene Logik enthält (vorher prüfen und 
  entsprechend entscheiden, wo die Grenze zwischen UI-Package und Backend-Modul liegt).
- Kapselt den Aufruf von aider_launcher.sh NICHT als interaktives Menü, sondern bietet 
  eine programmatische, nicht-interaktive Ansteuerung von Aider direkt:
  - `sendPrompt(model: String, mode: AiderChatMode, prompt: String, contextFiles: List<String>): Flow<AiderOutputLine>`
  - Baut den äquivalenten `aider`-Kommandozeilenaufruf zusammen (wie in `run_aider()` 
    im Original-Skript), inkl. automatischer Übernahme der Projekt-Kontextdateien 
    (activeDevelopment.md etc.) und optionaler .aider.conf.yml, aber gesteuert aus der 
    UI statt interaktiv im Terminal.
  - Lädt API-Keys aus den bestehenden Secrets-Dateien (gleiche Pfade wie im Original-Skript).
  - Nutzt intern denselben Exec-Mechanismus wie bereits etablierte Terminal-Aufrufe 
    (ShellUtils/ubuntuProcess-Äquivalent), damit die Ausführung im selben Sandbox-Kontext 
    läuft wie das restliche Terminal-Ökosystem.
  - Streamt die Aider-Ausgabe zeilenweise zurück (analog zum GradleLogLine-Muster aus 
    der Gradle-Tooling-Bridge), damit Antworten/Code-Diffs in Echtzeit angezeigt werden können.

## 2. Modell-/Provider-Konfiguration (1:1 aus dem Original-Skript übernehmen)
- Erstelle ein Datenmodell `AiderModelCatalog` mit exakt den im Original-Skript 
  gelisteten Provider-Gruppen und Modellen:
  - Gemini Flash: gemini-3.1-flash-lite-preview, gemini-3.1-flash-live-preview, 
    gemini-3-flash-preview, gemini-2.5-flash, gemini-2.5-flash-lite, 
    gemini-2.5-flash-lite-preview-06-17, gemini-2.5-flash-lite-preview-09-2025, 
    gemini-2.5-flash-preview-09-2025, gemini-2.5-computer-use-preview-10-2025, 
    gemini-2.5-flash-native-audio-latest, gemini-2.0-flash, gemini-2.0-flash-lite, 
    gemini-2.0-flash-exp, gemini-2.0-flash-001, gemini-2.0-flash-lite-001, 
    gemini-1.5-flash, gemini-1.5-flash-002, gemini-1.5-flash-latest, 
    gemini-flash-latest, gemini-flash-lite-latest.
  - Gemini Pro: gemini-3.1-pro-preview, gemini-3.1-pro-preview-customtools, 
    gemini-3-pro-preview, gemini-2.5-pro, gemini-2.5-pro-exp-03-25, 
    gemini-2.5-pro-preview-05-06, gemini-2.5-pro-preview-06-05, 
    gemini-2.5-pro-preview-tts, gemini-2.0-pro-exp-02-05, gemini-1.5-pro, 
    gemini-pro-latest, gemini-exp-1206, gemini-exp-1114, learnlm-1.5-pro-experimental.
  - OpenAI: o3-mini, gpt-4o, gpt-4o-mini.
  - DeepSeek: deepseek-chat, deepseek-reasoner.
  - Claude: claude-3-5-sonnet-20241022, claude-3-5-haiku-20241022, claude-3-opus-20240229.
  - Übernimm auch die im Original vorhandenen Kurzbeschreibungen als `description`-Feld 
    pro Modell (z. B. "DIE BESTE WAHL FÜR KOTLIN!" bei gemini-2.5-pro), damit der 
    fachliche Kontext für Nutzer erhalten bleibt.
  - Markiere Modelle mit "--browser"-Sonderbehandlung (Computer-Use) und 
    "--architect"-Standardmodus (Pro-Modelle) entsprechend als Metadaten-Flags.

## 3. UI-Integration – Zwei Einbindungsstellen (kombiniert für maximalen Nutzen)

### 3a. Neuer Tab "AI" im bestehenden Tab-Panel-System
- Erweitere das Tab-Panel in :core:tooling:tooling-impl (Terminal/Problems/IDE/Build/LSP) 
  um einen sechsten Tab "AI", konsistent im selben BottomSheet-Stil.
- Inhalt des AI-Tabs:
  - Oben: Provider-Auswahl (Chip-Row: Gemini/OpenAI/DeepSeek/Claude), darunter 
    Modell-Dropdown (gefiltert nach gewähltem Provider, mit Kurzbeschreibung als 
    Untertext je Eintrag).
  - Chat-Modus-Auswahl (Segmented Button/Dropdown: Auto/Code/Architect/Ask/Help), 
    mit denselben Beschreibungstexten wie im Original-Skript (CHAT_MODE_DESCS).
  - Chatverlauf-Ansicht (Prompt-Historie + Aider-Antworten, mit Markdown-Rendering 
    für Code-Blöcke, analog zum Docs-Panel-Konzept aus früheren Prompts).
  - Eingabefeld unten für den Prompt, Senden-Button, sowie ein Toggle 
    "Sub-Tree Only" (Checkbox, entspricht --subtree-only) und ein Options-Menü für 
    zusätzliche Flags (--browser nur sichtbar/aktivierbar bei Computer-Use-Modellen).
  - Anzeige, welche Kontextdateien automatisch mitgesendet werden (Chip-Liste: 
    activeDevelopment.md ✓, progress.md ✓, etc. – je nachdem was im Projekt vorhanden ist), 
    mit der Möglichkeit, einzelne davon abzuwählen.

### 3b. Editor-Kontextintegration ("Ask AI" direkt am Code)
- Ergänze im Editor-Kontextmenü (Long-Press-Auswahl / Selection-Toolbar) einen neuen 
  Eintrag "Mit KI bearbeiten" bzw. "Ask AI", sichtbar wenn Text im Editor markiert ist.
- Klick öffnet den AI-Tab (siehe 3a) mit vorbefülltem Prompt-Kontext:
  - Automatisch eingefügter Codeblock der Selektion inkl. Datei-Pfad und Zeilennummern 
    als Kontext-Präfix im Prompt-Feld (editierbar vor dem Senden).
  - Vorauswahl des Chat-Modus auf "code" (Standard für gezielte Code-Änderungen).
- Nach Erhalt einer Aider-Antwort mit vorgeschlagenen Datei-Änderungen (Aider gibt 
  standardmäßig Diffs/direkte Dateiänderungen aus): zeige eine Diff-Vorschau je 
  betroffener Datei (ähnliches Muster wie das Rename-Feature aus der LSP-Integration) 
  mit Buttons "Übernehmen" / "Verwerfen" pro Datei, bevor tatsächlich in die 
  Projektdateien geschrieben wird – NIEMALS automatisch und ungefragt Dateien überschreiben.

## 4. API-Key-Verwaltung in den Settings
- Neuer Settings-Unterpunkt "KI-Assistent (Aider)":
  - Eingabefelder für die vier API-Keys (Gemini, Anthropic, OpenAI, DeepSeek) mit 
    Sichtbarkeits-Toggle, Speicherung in denselben Secrets-Dateipfaden wie vom 
    Original-Skript erwartet (~/.gemini_api_key.secrets etc.), damit sowohl die neue 
    UI als auch das bestehende Bash-Skript weiterhin kompatibel funktionieren.
  - Anzeige des Aider-VENV-Status (installiert? Pfad? Python-Version?) mit 
    "Installieren/Reparieren"-Button, falls das VENV unter $HOME_DIR/.venv fehlt 
    (nutzt denselben Ubuntu-Sandbox-Installationsmechanismus wie andere CLI-Tools 
    im Projekt, z. B. analog zu TypstInstallationManager als Vorlage für den 
    Installations-Dialog-Flow).
  - Standard-Modell-Voreinstellung (welches Modell/Provider soll beim Öffnen des 
    AI-Tabs vorausgewählt sein).

## 5. Kompatibilität zum bestehenden Bash-Skript
- Das bestehende scripts/aider_launcher.sh bleibt unverändert nutzbar (z. B. für 
  Nutzer, die direkt im Terminal arbeiten wollen) – die neue UI ist eine zusätzliche, 
  gleichwertige Ansteuerungsmöglichkeit desselben Aider-Backends, keine Ablösung.
- Stelle sicher, dass beide Wege (UI und Bash-Menü) dieselben Kontext-Dateien, 
  Secrets-Pfade und VENV-Konfiguration nutzen, damit z. B. ein über die UI gespeicherter 
  API-Key auch beim Start via `bash scripts/aider_launcher.sh` im Terminal funktioniert.

# Nicht-Ziele
- Kein eigenes KI-Modell-Hosting oder eigene Inferenz – ausschließlich Ansteuerung der 
  bestehenden Aider-CLI-Integration mit den bereits unterstützten Cloud-Providern.
- Keine automatische, ungefragte Dateiänderung durch Aider-Vorschläge (siehe Punkt 3b, 
  Diff-Vorschau-Pflicht).

# Akzeptanzkriterien
- Ein im AI-Tab gesendeter Prompt mit gewähltem Modell (z. B. gemini-2.5-pro) liefert 
  eine echte Aider-Antwort, sichtbar als gestreamte Ausgabe im Chatverlauf.
- "Ask AI" aus einer Code-Selektion im Editor öffnet den AI-Tab mit korrekt 
  vorbefülltem Kontext (Dateiname, Zeilenbereich, Codeinhalt).
- Von Aider vorgeschlagene Dateiänderungen werden NIEMALS ohne explizite Bestätigung 
  ("Übernehmen"-Klick pro Datei) tatsächlich auf der Festplatte geschrieben.
- Ein über die neue Settings-UI gespeicherter API-Key ist unmittelbar danach auch bei 
  Ausführung von `bash scripts/aider_launcher.sh` im Terminal wirksam (gemeinsame 
  Secrets-Datei-Nutzung verifiziert).
