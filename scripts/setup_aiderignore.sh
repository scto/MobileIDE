#!/bin/bash

# --- FARBEN ---
G="\e[0;32m"; C="\e[0;36m"; Y="\e[0;33m"; NC="\033[0m"

echo -e "${C}Erstelle .aiderignore Datei für Kotlin/Android Projekt...${NC}"

cat <<EOF > .aiderignore
# Aider Ignore - Optimiert für Kotlin/Android/Python

# --- Version Control & Internals ---
.git/
.aider*
!.aiderignore

# --- Kotlin / Java / Android (Sehr wichtig!) ---
.gradle/
build/
bin/
out/
app/build/
*.class
*.dex
*.apk
*.aar
*.jar
.idea/
*.iml
.vscode/

# --- Python / Venv ---
.venv/
__pycache__/
*.py[cod]
.pytest_cache/

# --- Secrets & Config ---
*.secrets
.env*

# --- OS & Large Files ---
.DS_Store
Thumbs.db
*.log
node_modules/
EOF

echo -e "${G}Erfolg: .aiderignore wurde im aktuellen Verzeichnis erstellt!${NC}"
echo -e "${Y}Tipp: Führe das Script im Hauptverzeichnis deines Git-Repos aus.${NC}"