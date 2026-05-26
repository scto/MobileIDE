#!/bin/bash

# --- FARBEN ---
G="\e[0;32m"; C="\e[0;36m"; Y="\e[0;33m"; R="\e[0;31m"; W="\e[0;37m"; NC="\033[0m"

echo -e "${C}=== Ollama Local Installer ===${NC}"

# Prüfe Betriebssystem
OS_TYPE=$(uname -o)
ARCH_TYPE=$(uname -m)

install_linux() {
    echo -e "${Y}Erkenne Standard Linux. Nutze offizielles Install-Skript...${NC}"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL https://ollama.com/install.sh | sh
    else
        echo -e "${R}Fehler: curl ist nicht installiert.${NC}"
        exit 1
    fi
}

install_termux() {
    echo -e "${Y}Erkenne Termux/Android Umgebung.${NC}"
    echo -e "${W}Ollama benötigt unter Android normalerweise eine proot-Umgebung (z.B. Ubuntu).${NC}"
    echo -e "Möchtest du versuchen, Ollama über pkg zu suchen oder die proot-Anleitung sehen?"
    echo -e "1) Nach Paketen suchen"
    echo -e "2) Abbruch (Manuelle Installation in proot empfohlen)"
    read -p "Auswahl: " t_choice
    
    if [ "$t_choice" == "1" ]; then
        pkg update && pkg install ollama
    else
        echo -e "${C}Tipp: Installiere 'proot-distro', dann 'ubuntu' und dort das offizielle Skript.${NC}"
        exit 0
    fi
}

# Hauptlogik
if [[ "$OS_TYPE" == *"Android"* ]]; then
    install_termux
elif [[ "$OS_TYPE" == *"Linux"* ]]; then
    install_linux
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo -e "${Y}Erkenne macOS. Lade Installer herunter...${NC}"
    open https://ollama.com/download/Ollama-darwin.zip
else
    echo -e "${R}Unbekanntes System: $OS_TYPE${NC}"
    exit 1
fi

if command -v ollama >/dev/null 2>&1; then
    echo -e "\n${G}Ollama wurde erfolgreich installiert!${NC}"
    echo -e "${W}Starte den Server mit: ${C}ollama serve${NC}"
    echo -e "${W}Nutze ein Modell mit: ${C}ollama run llama3${NC}"
else
    echo -e "\n${R}Installation konnte nicht verifiziert werden.${NC}"
fi
