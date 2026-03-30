#!/bin/bash

# ==============================================================================
# CodeLikeBasti Master Setup - NO-SUDO Edition v1030 (Claude & Gemini Edition)
# Fokus: Android API 24 (Kotlin) | Multi-Model Expert Mode
# ==============================================================================

set -o pipefail

# --- PFADE ---
LOCAL_BIN="$HOME/.local/bin"
export PATH="$LOCAL_BIN:$PATH"
GEMINI_FILE="$HOME/.gemini_api_key.secrets"
ANTHROPIC_FILE="$HOME/.anthropic_api_key.secrets"
GITHUB_FILE="$HOME/.github_token.secrets"
VENV_PATH="$HOME/.aider_venv"

# --- FARBEN ---
GREEN='\033[0;32m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
RED='\033[0;31m'
NC='\033[m'

echo -e "${BLUE}>>> Starte CodeLikeBasti Aider Environment (v1030)...${NC}"

# --- 1. SECRET MANAGEMENT ---
check_secrets() {
    echo -e "${GREEN}Prüfe API-Keys und Tokens...${NC}"
    
    # Gemini Check
    if [ ! -f "$GEMINI_FILE" ]; then
        read -p "Gib deinen Gemini API Key ein: " user_gemini
        echo "$user_gemini" > "$GEMINI_FILE"
        chmod 600 "$GEMINI_FILE"
    fi
    export GEMINI_API_KEY=$(cat "$GEMINI_FILE")

    # Anthropic (Claude) Check
    if [ ! -f "$ANTHROPIC_FILE" ]; then
        echo -e "${PURPLE}Neuer Key benötigt: Anthropic (Claude)${NC}"
        read -p "Gib deinen Claude API Key ein: " user_claude
        echo "$user_claude" > "$ANTHROPIC_FILE"
        chmod 600 "$ANTHROPIC_FILE"
    fi
    export ANTHROPIC_API_KEY=$(cat "$ANTHROPIC_FILE")

    # GitHub Check
    if [ ! -f "$GITHUB_FILE" ]; then
        read -p "Gib deinen GitHub Token ein: " user_github
        echo "$user_github" > "$GITHUB_FILE"
        chmod 600 "$GITHUB_FILE"
    fi
    export GITHUB_TOKEN=$(cat "$GITHUB_FILE")
    export GH_TOKEN="$GITHUB_TOKEN"
}

# --- 2. VENV & DEPENDENCIES ---
setup_env() {
    # GitHub CLI (gh) lokal installieren, falls fehlt
    if ! command -v gh &> /dev/null; then
        GH_VERSION=$(curl -s https://api.github.com/repos/cli/cli/releases/latest | grep -Po '"tag_name": "v\K[0-9.]+')
        TEMP_DIR=$(mktemp -d)
        curl -sSL "https://github.com/cli/cli/releases/download/v${GH_VERSION}/gh_${GH_VERSION}_linux_amd64.tar.gz" -o "$TEMP_DIR/gh.tar.gz"
        tar -xzf "$TEMP_DIR/gh.tar.gz" -C "$TEMP_DIR"
        mv "$TEMP_DIR"/gh_*/bin/gh "$LOCAL_BIN/"
        rm -rf "$TEMP_DIR"
    fi

    # Python Venv & Aider
    if [ ! -d "$VENV_PATH" ]; then
        python3 -m venv "$VENV_PATH"
    fi
    source "$VENV_PATH/bin/activate"
    export UV_LINK_MODE=copy
    
    if ! command -v aider &> /dev/null; then
        pip install --upgrade pip aider-chat
    fi
}

# --- 3. LOKALE KI (OLLAMA) ---
check_local_ai() {
    if ! command -v ollama &> /dev/null; then
        curl -L https://ollama.com/download/ollama-linux-amd64 -o "$LOCAL_BIN/ollama"
        chmod +x "$LOCAL_BIN/ollama"
    fi
    if ! pgrep -x "ollama" > /dev/null; then
        ollama serve > /dev/null 2>&1 &
        sleep 3
    fi
    ollama pull deepseek-coder-v2:lite
}

# --- RUN ---
check_secrets
setup_env

# Standard-Flags
AIDER_FLAGS="--no-show-model-warnings --subtree-only"

clear
echo "================================================================"
echo -e "${BLUE}    AIDERS START - KOTLIN & ANDROID API 24 EXPERT MODE${NC}"
echo "================================================================"
echo -e " 1) ${PURPLE}Claude 3.5 Sonnet${NC}  (Empfehlung für komplexes Kotlin)"
echo " 2) Gemini 2.5 Pro      (Beste Cloud-Alternative)"
echo " 3) Gemini 2.5 Flash    (Schnell & Günstig)"
echo " 4) DeepSeek Lokal      (Keine Limits, 100% Privat)"
echo " 5) Secrets verwalten   (Keys löschen)"
echo " 6) Beenden"
echo "================================================================"
read -p "Wahl [1-6]: " choice

case $choice in
    1)
        # Claude 3.5 Sonnet (Newest Version)
        aider --model anthropic/claude-3-5-sonnet-20241022 $AIDER_FLAGS
        ;;
    2)
        aider --model gemini/gemini-2.5-pro $AIDER_FLAGS
        ;;
    3)
        aider --model gemini/gemini-2.5-flash $AIDER_FLAGS
        ;;
    4)
        check_local_ai
        aider --model ollama/deepseek-coder-v2:lite
        ;;
    5)
        rm -f "$GEMINI_FILE" "$ANTHROPIC_FILE" "$GITHUB_FILE"
        echo "Alle Keys gelöscht. Neustart..."
        sleep 1
        exec "$0"
        ;;
    6)
        exit 0
        ;;
    *)
        exec "$0"
        ;;
esac
