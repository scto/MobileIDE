# MobileIDE ![Stone Badge](https://stone.professorlee.work/api/stone/scto/MobileIDE)

![Version](https://img.shields.io/badge/version-0.3.2-blue?style=flat-square)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue?style=flat-square)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-green?style=flat-square)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-GPLv3-orange?style=flat-square)](LICENSE)

[ [**English**] ] |  [Deutsch](README_DE.md) ]

MobileIDE ist eine native Android-Entwicklungsumgebung (IDE) für die App-Entwicklung. Dieses vollständig mit Jetpack Compose entwickelte Projekt implementiert einen kompletten Workflow von der Codebearbeitung bis zum Erstellen von APKs direkt auf Ihrem Mobilgerät.

Dies ist ein experimentelles Entwicklungsprojekt; seine Kernarchitektur und Codelogik wurden in Zusammenarbeit mit mehreren KI-Modellen (Claude, Gemini, DeepSeek) erstellt.

 ## Screenshots

<div align="center">
<img src="https://github.com/user-attachments/assets/2eac6ea4-25a1-4a02-b814-2925ffb2092e" width="45%" />
<img src="https://github.com/user-attachments/assets/7999b42a-af56-4aea-b705-920e7e168844" width="45%" />
</div>

## Projektstruktur

Der Hauptcode befindet sich in `app/src/main/java/com/scto/mobile/ide/`.  Die Verzeichnisstruktur und die Funktionen sind wie folgt:

```text
com.scto.mobile.ide
├── build/ # Benutzerdefiniertes APK-Erstellungssystem
│ ├── ApkBuilder.java # Kernlogik zum Kompilieren und Verpacken von APKs
│ ├── ApkInstaller.kt # APK-Installation
│ └── ... # Verschlüsselung, ZipAligner
├── core/ # App-spezifische Kerninfrastruktur
│ └── utils/ # Hilfsprogramme (Backup, CodeFormatter, WorkspaceManager usw.)
├── files/ # Dateisystemmodul
│ ├── FileIcons.kt # Zuordnung von Symbolressourcen
│ └── FileTree.kt # Benutzeroberfläche für den Datei-Explorer  Logik
├── ui/ # Benutzeroberfläche (Jetpack Compose)
│ ├── components/ # Gemeinsame UI-Komponenten
│ ├── editor/ # Code-Editor
│ ├── preview/ # Web-Vorschau
│ ├── settings/ # Anwendungseinstellungen und Info-Bildschirme
│ ├── terminal/ # Terminalemulator (Alpine Linux-Integration)
│ ├── theme/ # Designsystem (Farben, Typografie)
│ └── welcome/ # Willkommens-/Onboarding-Bildschirm


**Wichtige Assets (`app/src/main/assets/`)**:

* `textmate/`: TextMate-Grammatiken und -Konfigurationen für Syntaxhervorhebung.

* `queries/`: Syntaxbaumabfragen. 
* `init-host.sh`, `init.sh`, `proot`, `rootfs.bin`: Dateien für die eingebettete Alpine Linux-Umgebung.

## Funktionen

* **Syntaxhervorhebung**: Basierend auf TextMate-Grammatikdateien, bietet sie optimale Unterstützung für HTML, CSS, JavaScript und JSON.

* **Projektmanagement**: Voller Dateisystemzugriff zur Erstellung und Verwaltung von Webprojekten mit mehreren Dateien.

* **Echtzeitvorschau**: Integrierte WebView-Vorschauumgebung mit Unterstützung für JavaScript-Interaktionstests.

* **Moderne Benutzeroberfläche**: Vollständig in Kotlin und Jetpack Compose geschrieben, mit Unterstützung für dynamische Designs.

* **Git-Integration**: Integrierte Git-Versionskontrolle mit visueller Commit-Historie, unterstützt Klonen, Commit, Push, Pull und Branch-Verwaltung. Ignoriert automatisch sensible Dateien und Build-Artefakte.

 ## Diskussion

* QQ-Gruppe: [1050254184](https://qm.qq.com/q/tFXuqMQDlK)
* TG-Gruppe: [Android_For_MobileIDE](https://t.me/Android_For_MobileIDE)

## Mitwirkende

<a href="https://github.com/scto/MobileIDE/graphs/contributors">
<img src="https://contributors-img.web.app/image?repo=scto/MobileIDE" />

</a>

## Lizenz

``` MobileIDE – Eine leistungsstarke IDE für die Android-App-Entwicklung.

 Copyright (C) 2025 scto <tschmid35@.com>

Dieses Programm ist freie Software: Sie können es weitergeben und/oder verändern
unter den Bedingungen der GNU General Public License, wie von der Free Software Foundation veröffentlicht, entweder Version 3 der Lizenz oder
(nach Ihrer Wahl) jede spätere Version.

Dieses Programm wird in der Hoffnung verbreitet, dass es nützlich sein wird,
aber OHNE JEGLICHE GEWÄHRLEISTUNG, insbesondere ohne die implizite Gewährleistung der
MARKTGÄNGIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK. Weitere Details finden Sie in der
GNU General Public License.

Sie sollten eine Kopie der GNU General Public License zusammen mit diesem Programm erhalten haben. Falls nicht, besuchen Sie <https://www.gnu.org/licenses/>.

 ```

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=scto/MobileIDE&type=Date)](https://star-history.com/#scto/MobileIDE&Date)