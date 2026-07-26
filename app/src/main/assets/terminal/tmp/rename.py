from pathlib import Path

# Definiere die Ersetzungsregeln in einer Map (Key: Suchen nach, Value: Ersetzen durch)
REPLACEMENTS = {
    "ARCH": "UNUNTU",
    "TERMIX": "MOBILEIDE",
    "arch": "ubuntu",
    "termix": "mobileide",
    "Termix": "MobikeIDE"
}

def replace_in_file(file_path: Path):
    try:
        # Datei einlesen
        content = file_path.read_text(encoding="utf-8")
        
        # Ersetzungen nacheinander durchführen
        original_content = content
        for search, replace in REPLACEMENTS.items():
            content = content.replace(search, replace)
        
        # Nur speichern, wenn sich tatsächlich etwas geändert hat
        if content != original_content:
            file_path.write_text(content, encoding="utf-8")
            print(f"✓ Aktualisiert: {file_path}")
        else:
            print(f"• Keine Änderungen in: {file_path}")
            
    except Exception as e:
        print(f"✗ Fehler beim Verarbeiten von {file_path}: {e}")

def main(directory_path: str):
    path = Path(directory_path)
    if not path.is_dir():
        print(f"Fehler: '{directory_path}' ist kein gültiges Verzeichnis.")
        return

    # Alle .sh Dateien im angegebenen Verzeichnis suchen
    bash_files = list(path.glob("*.sh"))
    
    if not bash_files:
        print("Keine .sh Dateien im Verzeichnis gefunden.")
        return

    print(f"Starte Ersetzung in {len(bash_files)} Bash-Skript(en)...")
    for file in bash_files:
        replace_in_file(file)

if __name__ == "__main__":
    # Pfad zum Verzeichnis, in dem die Bash-Skripte liegen ('.' steht für das aktuelle Verzeichnis)
    target_directory = "." 
    main(target_directory)
