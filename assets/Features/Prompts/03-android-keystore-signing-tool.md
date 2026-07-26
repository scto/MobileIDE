### Prompt-Datei: `03-android-keystore-signing-tool.md`

# Ziel
Implementiere ein GUI-Sign-Tool zur Erstellung und Verwaltung von Android-Signierschlüsseln 
(Keystores), nutzbar sowohl für Debug- als auch Release-Builds.

# Kontext
- Dies ist im README.md unter TODO als "GUI Keystore Wizard" gelistet.
- :core:apk-builder enthält bereits Signing-Logik für den Custom-Build-Pfad (AAPT2/D8/Zipalign).

# Anforderungen
1. Neues Modul oder Erweiterung von :core:apk-builder: KeystoreManager
   - Nutzt `keytool` (Teil des im PRoot-Container installierten OpenJDK) via 
     ProcessBuilder/PRoot-Wrapper, analog zum bestehenden Gradle-Bridge-Muster.
   - Funktionen: createKeystore(alias, password, keyPassword, validity, dn-Felder 
     [CN, OU, O, L, ST, C]), listKeystores(), deleteKeystore(path), 
     validateKeystore(path, password) (Test-Öffnen zur Passwortprüfung).

2. UI – "Signierung" Screen/Sheet (analog Settings-Struktur unter :app):
   - Formular: Keystore-Name/Pfad (Standard: `<project>/keystores/<name>.jks`), 
     Alias, Keystore-Passwort, Key-Passwort (mit "gleich wie Keystore-Passwort"-Toggle), 
     Gültigkeit in Jahren (Default 25), Zertifikats-Felder (Name, Organisation, Land etc.).
   - Passwortfelder mit Sichtbarkeits-Toggle, KEINE Klartext-Speicherung in Logs.
   - Speicherung der Zugangsdaten optional verschlüsselt via Android Keystore System 
     (EncryptedSharedPreferences) – Nutzer muss dem explizit zustimmen ("Passwort auf 
     diesem Gerät sicher speichern?").
   - Liste vorhandener Keystores pro Projekt mit Aktionen: Details anzeigen (Fingerprint, 
     Gültigkeit, Alias), Löschen, Als Standard für Release-Builds setzen.

3. Integration in Build-Flow (aus Prompt 02):
   - Bei `assembleRelease`/`bundleRelease` wird automatisch geprüft, ob eine 
     `signingConfigs.release`-Konfiguration im build.gradle(.kts) existiert. 
   - Falls nicht: Biete an, die im KeystoreManager gewählte Konfiguration automatisch 
     als `signingConfig` in `app/build.gradle.kts` einzutragen (mit Backup der Originaldatei 
     und klarer Diff-Vorschau vor dem Schreiben).
   - Sensible Werte (Passwörter) werden NICHT im Klartext in build.gradle.kts geschrieben, 
     sondern per `keystore.properties` (gitignored) + `Properties()`-Laderoutine referenziert.

# Akzeptanzkriterien
- Ein neu erstellter Keystore ist mit `keytool -list -v` verifizierbar gültig.
- Ein Release-Build mit aktivierter Signierkonfiguration erzeugt eine signierte, 
  installierbare APK (verifizierbar per `apksigner verify`, falls verfügbar, sonst 
  zumindest per erfolgreicher Installation auf einem Testgerät).
- Keine Passwörter werden im Klartext geloggt oder in Versionskontroll-relevante Dateien 
  geschrieben (keystore.properties muss automatisch zur .gitignore hinzugefügt werden).