### Prompt-Datei: `05-full-lsp-treesitter-integration.md`

# Ziel
Erweitere die bestehende TreeSitter-Syntax-Highlighting-Integration (:language-treesitter) und 
die LSP-Infrastruktur (:core:lsp, :editor-lsp, :extension-languages) zu einer vollständigen 
LSP-Feature-Integration im Editor: Hover, Rename, Go-to-Definition, Find-References, 
Code Completion, Diagnostics-Overlays, Signature Help.

# Kontext
- lsp4j ist bereits als Dependency vorhanden (siehe libs.versions.toml).
- LspRegistry/ScriptedLspServer-Pattern existiert bereits (siehe TypstServer.kt als Referenz-
  Implementierung eines LSP-Servers innerhalb des Extension-Systems).
- TreeSitter wird laut PROGRESS.md aktuell NUR für Syntax-Highlighting genutzt (Java, Kotlin, 
  XML, Log, C++), nicht für semantische Analyse/AST-Navigation im Editor.
- Bekanntes offenes TODO aus README: "LSP Diagnostics Overlays" (Fehler/Warnungs-Wellenlinien 
  im Editor) fehlen noch komplett.

# Anforderungen (in dieser Reihenfolge implementieren)
1. Diagnostics-Overlays (höchste Priorität, da im README als TODO markiert):
   - Abonniere `textDocument/publishDiagnostics` von allen aktiven LSP-Servern.
   - Rendere Wellenlinien (rot=Error, gelb=Warning, blau=Info) direkt im sora-editor via 
     dessen Span/Diagnostic-API (prüfe sora-editor Dependency-Version auf verfügbare 
     DiagnosticSpan-Unterstützung).
   - Zeige zusätzlich eine "Problems"-Liste im bereits bestehenden Logging-Tab-System 
     (:core:tooling:tooling-impl hat laut PROGRESS.md bereits einen "Problems"-Tab – 
     verbinde LSP-Diagnostics dort ein, statt nur Compiler-Fehler).

2. Hover:
   - `textDocument/hover` bei Long-Press/Cursor-Stillstand auslösen, Ergebnis als Popup/
     Tooltip mit Markdown-Rendering (Typinformation, Dokumentation) direkt über der Cursor-Position.

3. Go-to-Definition / Find-References:
   - Kontextmenü-Einträge im Editor (Long-Press-Menü oder Toolbar-Icon), Ergebnis-Navigation 
     bei Definition = direkter Sprung (ggf. Datei öffnen falls in anderem File), bei 
     References = Liste im BottomSheet mit Datei:Zeile-Einträgen, anklickbar.

4. Rename:
   - `textDocument/rename` via Kontextmenü "Umbenennen", Eingabedialog für neuen Namen, 
     Anwendung der WorkspaceEdit-Antwort über alle betroffenen Dateien mit Vorschau/Bestätigung 
     vor dem tatsächlichen Schreiben (WICHTIG: nie destruktiv ohne Diff-Vorschau).

5. Code Completion:
   - `textDocument/completion` an bestehende Editor-Autocomplete-Infrastruktur anbinden 
     (prüfe ob sora-editor bereits ein CompletionProvider-Interface hat, sonst Adapter bauen).

6. Signature Help:
   - `textDocument/signatureHelp` bei Funktionsaufruf-Eingabe (öffnende Klammer) anzeigen.

# Nicht-Ziele
- Kein eigener Sprachserver wird neu geschrieben – ausschließlich Anbindung bestehender 
  Server (jdtls, kotlin-language-server, tinymist, etc.) über das vorhandene ScriptedLspServer-Muster.

# Akzeptanzkriterien
- In einer .kt-Datei mit aktivem Kotlin-LSP: Hover zeigt Typinfo, Rename ändert eine Variable 
  projektweit korrekt, Diagnostics zeigen echte Compiler-Fehler als Wellenlinie inline im Editor.