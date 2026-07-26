| Prio | Thema | Aufwand | Begründung |
|---|---|---|---|
| 1 | Gradle-Tooling-Bridge fertigstellen (Flags, Live-Log, Task-Icon) | mittel | Basis existiert schon (`tooling-impl`), größter Nutzen/Aufwand-Faktor |
| 2 | Build-Button → `assembleDebug` → APK-Ergebnis in UI | niedrig-mittel | baut direkt auf 1 auf, schneller Mehrwert |
| 3 | Sign-Tool (Keystore-Wizard) | niedrig-mittel | unabhängig, klar abgrenzbar, für Release-Flow nötig |
| 4 | Modul-Konsolidierung (`:features:*`) | mittel-hoch | sollte **vor** LSP/Layout-Preview-Ausbau erfolgen, sonst Technical Debt |
| 5 | Layout-Preview (`@Composable`-Vorschau) | hoch | großer neuer Feature-Bereich, abhängig von stabiler Editor-Architektur |
| 6 | Plugin-Migration (xed-Plugins) | mittel, iterativ | einzeln migrierbar, kein Blocker für anderes |
| 7 | Vollständige LSP/Treesitter-Integration (Hover/Rename) | sehr hoch | strategisch wichtig, aber am aufwändigsten – nach Konsolidierung angehen |