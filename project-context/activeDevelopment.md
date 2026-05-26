## Aktueller Fokus
 * **Massives Architektur-Refactoring & Konsolidierung:** Das gesamte Projekt wird aktuell in den Namespace com.scto.mcside migriert. Alte Referenzen (com.web.webide, com.rk, com.srvhive) werden eliminiert.
 * **Feature-Integration:** Die Funktionalität aus veralteten Verzeichnissen (wie xed) wird in die regulären :core und :feature Module integriert und konsolidiert.
## Nächste Schritte
 1. Abschluss der App-Modul Migration (Header entfernen, chinesische Strings übersetzen).
 2. Abschluss der XED-Migration für :feature:settings und :core:editor (Ordner für Ordner).
 3. Zentralisierung aller String-Ressourcen aus allen Modulen in :core:resources.
 4. Ersetzen aller hartkodierten UI-Strings im Quellcode durch referenzierte Strings (R.string...).
 5. Abschließender Build-Test des :app Moduls.
## Getroffene Entscheidungen
 * **Kein XML mehr:** Das UI wird ausschließlich in Compose geschrieben. Alt-Code wird umgeschrieben.
 * **Strikte Modulgrenzen:** Feature-Module dürfen nicht direkt miteinander kommunizieren, sondern nur über das Core-Domain-Modul oder Navigationsevents.
 * **Dynamisches Aider-Prompting:** Aufgrund von Context-Window-Limits (Token-Limits) werden Refactorings streng nach Modulen unterteilt. Große Bäume (wie XED) werden schrittweise aufgelöst.
## Bekannte Probleme / Risiken
 * **"Token Limit Exceeded":** Zu viele Dateien gleichzeitig in den KI-Kontext zu laden, führt zu Abbrüchen. Workaround: Strikte /clear und /drop Routinen zwischen den Refactoring-Schritten anwenden.
 * **"R is not resolved":** Beim Verschieben von Dateien können die generierten Android-Ressourcen-Klassen den Bezug verlieren. Dies muss durch die Aktualisierung der Imports auf com.scto.mcside.core.resources.R oder den lokalen App-Namespace gelöst werden.
