#!/bin/bash

# --- FARBEN ---
G="\e[0;32m"; C="\e[0;36m"; Y="\e[0;33m"; R="\e[0;31m"; W="\e[0;37m"; M="\e[0;35m"; B="\e[1m"; NC="\033[0m"

# --- PFADE (Vom Setup-Script injiziert) ---
VENV_PATH_VAL="/data/data/jkas.androidpe/files/home/.venv"
SECRET_GEMINI="/data/data/jkas.androidpe/files/home/.gemini_api_key.secrets"
SECRET_ANTHROPIC="/data/data/jkas.androidpe/files/home/.anthropic_api_key.secrets"
SECRET_OPENAI="/data/data/jkas.androidpe/files/home/.openai_api_key.secrets"
SECRET_DEEPSEEK="/data/data/jkas.androidpe/files/home/.deepseek_api_key.secrets"

load_secrets() {
    export UV_LINK_MODE=copy
    [ -f "$SECRET_GEMINI" ] && export GEMINI_API_KEY=$(cat "$SECRET_GEMINI" | xargs)
    [ -f "$SECRET_ANTHROPIC" ] && export ANTHROPIC_API_KEY=$(cat "$SECRET_ANTHROPIC" | xargs)
    [ -f "$SECRET_OPENAI" ] && export OPENAI_API_KEY=$(cat "$SECRET_OPENAI" | xargs)
    [ -f "$SECRET_DEEPSEEK" ] && export DEEPSEEK_API_KEY=$(cat "$SECRET_DEEPSEEK" | xargs)
}

run_aider() {
    local model=$1
    local flags=$2
    echo -e "\n${G}Starte Aider mit ${W}$model${NC}...\n"
    source "$VENV_PATH_VAL/bin/activate"
    export AIDER_MODEL="$model"
    aider --model "$model" $flags
    exit 0
}

choose_run_mode() {
    local model=$1
    local base_flags=$2

    while true; do
        clear
        echo -e "${C}=== AUSFÜHRUNGSMODUS ===${NC}"
        echo -e "Modell: ${W}$model${NC}"
        echo -e "------------------------------------------------------"
        echo -e "${G}1) Run Aider Console${NC}"
        echo -e "${C}2) Run Aider Browser${NC}"
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Wahl: " run_choice

        case $run_choice in
            1) run_aider "$model" "$base_flags" ;;
            2) 
                run_aider "$model" "$base_flags --browser" 
                ;;
            0) return ;;
            *) echo -e "${R}Ungültige Eingabe!${NC}"; sleep 1 ;;
        esac
    done
}

# ==========================================
# SUB-MENÜS FÜR GEMINI
# ==========================================

menu_gemini_flash() {
    while true; do
        clear
        echo -e "${C}=== GEMINI FLASH MODELLE (High Speed & Special) ===${NC}"
        local mods=(
            "Gemini 3.1 Flash Lite|gemini/gemini-3.1-flash-lite-preview|Lite Preview"
            "Gemini 3.1 Flash Live|gemini/gemini-3.1-flash-live-preview|Live Interaction"
            "Gemini 3 Flash Preview|gemini/gemini-3-flash-preview|Early Access V3"
            "Gemini 2.5 Flash|gemini/gemini-2.5-flash|Recommended Speed"
            "Gemini 2.5 Flash Lite|gemini/gemini-2.5-flash-lite|Stable Lite"
            "Gemini 2.5 Flash Lite (06-17)|gemini/gemini-2.5-flash-lite-preview-06-17|Build 06-17"
            "Gemini 2.5 Flash Lite (09-25)|gemini/gemini-2.5-flash-lite-preview-09-2025|Build 09-25"
            "Gemini 2.5 Flash Preview (09-25)|gemini/gemini-2.5-flash-preview-09-2025|V2.5 Preview"
            "Gemini 2.5 Computer Use|gemini/gemini-2.5-computer-use-preview-10-2025|UI Control"
            "Gemini 2.5 Flash Native Audio|gemini/gemini-2.5-flash-native-audio-latest|Audio Focus"
            "Gemini 2.5 Audio (09-25)|gemini/gemini-2.5-flash-native-audio-preview-09-2025|Audio Build"
            "Gemini 2.5 Audio (12-25)|gemini/gemini-2.5-flash-native-audio-preview-12-2025|Audio Build"
            "Gemini 2.0 Flash|gemini/gemini-2.0-flash|V2.0 Speed"
            "Gemini 2.0 Flash Exp|gemini/gemini-2.0-flash-exp|Latest experimental model, lightning fast and clever"
            "Gemini 2.0 Flash (001)|gemini/gemini-2.0-flash-001|V2.0.1"
            "Gemini 2.0 Flash Lite|gemini/gemini-2.0-flash-lite|V2.0 Lite"
            "Gemini 2.0 Flash Lite (001)|gemini/gemini-2.0-flash-lite-001|V2.0.1 Lite"
            "Gemini 1.5 Flash (002)|gemini/gemini-1.5-flash-002|V1.5.2"
            "Gemini 1.5 Flash Latest|gemini/gemini-1.5-flash-latest|1M/2M Tokens Extremely fast, ideal for efficiently processing huge contexts/many files."
            "Gemini Flash Latest|gemini/gemini-flash-latest|Auto-Update Flash"
            "Gemini Flash Lite Latest|gemini/gemini-flash-lite-latest|Auto-Update Lite"
            "Gemma 3 27b IT|gemini/gemma-3-27b-it|Open Model V3"
            "Gemma 2 27b IT|gemini/gemini-gemma-2-27b-it|Open Model V2"
            "Gemma 2 9b IT|gemini/gemini-gemma-2-9b-it|Open Model V2 Small"
            "Gemini Robotics ER 1.5|gemini/gemini-robotics-er-1.5-preview|Robotics Focus"
        )
        for i in "${!mods[@]}"; do
            IFS='|' read -r name id desc <<< "${mods[$i]}"
            printf "${W}%2d)${NC} ${C}%-32s${NC} %s\n" "$((i+1))" "$name" "$desc"
        done
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Auswahl: " c
        [[ "$c" == "0" ]] && return
        if [[ "$c" -ge 1 && "$c" -le "${#mods[@]}" ]]; then
            IFS='|' read -r name id desc <<< "${mods[$((c-1))]}"
            local f=""
            [[ "$id" == *"computer-use"* ]] && f="--browser"
            choose_run_mode "$id" "$f"
        fi
    done
}

menu_gemini_pro() {
    while true; do
        clear
        echo -e "${M}=== GEMINI PRO MODELLE (Intelligence & Experimental) ===${NC}"
        local mods=(
            "Gemini 3.1 Pro|gemini/gemini-3.1-pro-preview|Top Logic"
            "Gemini 3.1 Pro Tools|gemini/gemini-3.1-pro-preview-customtools|Custom Tools"
            "Gemini 3 Pro Preview|gemini/gemini-3-pro-preview|Next Gen Pro"
            "Gemini 2.5 Pro (Kotlin Best)|gemini/gemini-2.5-pro|Recommended for Coding"
            "Gemini 2.5 Pro Exp (03-25)|gemini/gemini-2.5-pro-exp-03-25|Experimental Build"
            "Gemini 2.5 Pro (05-06)|gemini/gemini-2.5-pro-preview-05-06|Build 05-06"
            "Gemini 2.5 Pro (06-05)|gemini/gemini-2.5-pro-preview-06-05|Build 06-05"
            "Gemini 2.5 Pro TTS|gemini/gemini-2.5-pro-preview-tts|Speech Synth"
            "Gemini Exp 1206|gemini/gemini-exp-1206|Experimental Dec"
            "Gemini Exp 1114|gemini/gemini-exp-1114|Experimental Nov"
            "Gemini Pro Latest|gemini/gemini-pro-latest|Auto-Update Pro"
            "LearnLM 1.5 Pro|gemini/learnlm-1.5-pro-experimental|Education Focus"
            "Lyria 3 Pro Preview|gemini/lyria-3-pro-preview|Music/Audio Gen"
            "Lyria 3 Clip Preview|gemini/lyria-3-clip-preview|Audio Embedding"
        )
        for i in "${!mods[@]}"; do
            IFS='|' read -r name id desc <<< "${mods[$i]}"
            printf "${W}%2d)${NC} ${M}%-32s${NC} %s\n" "$((i+1))" "$name" "$desc"
        done
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Auswahl: " c
        [[ "$c" == "0" ]] && return
        if [[ "$c" -ge 1 && "$c" -le "${#mods[@]}" ]]; then
            IFS='|' read -r name id desc <<< "${mods[$((c-1))]}"
            choose_run_mode "$id" "--architect"
        fi
    done
}

menu_gemini() {
    while true; do
        clear
        echo -e "${C}=== PROVIDER: GEMINI ===${NC}"
        echo -e "${C}1) Flash Modelle${NC} (Schnell & Günstig)"
        echo -e "${M}2) Pro Modelle${NC} (Intelligent & Kotlin-Spezialisten)"
        echo -e "${W}3) Liste Modelle${NC} (Befehl: list-models gemini/)"
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Wahl: " g
        case $g in
            1) menu_gemini_flash ;;
            2) menu_gemini_pro ;;
            3) 
                echo -e "\n${G}Abfrage der verfügbaren Gemini-Modelle...${NC}\n"
                source "$VENV_PATH_VAL/bin/activate"
                aider --list-models gemini/
                echo -e "\n${Y}Drücke Enter, um zum Menü zurückzukehren...${NC}"
                read
                ;;
            0) return ;;
        esac
    done
}

# ==========================================
# PROVIDER MENÜS
# ==========================================

menu_openai() {
    while true; do
        clear
        echo -e "${W}=== PROVIDER: OPENAI ===${NC}"
        echo -e "${W}1) o1${NC} (High Reasoning)"
        echo -e "${W}2) o3-mini${NC} (Fast Reasoning Focus)"
        echo -e "${W}3) GPT-4o${NC} (Allrounder)"
        echo -e "${W}4) GPT-4o-mini${NC} (Fast)"
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Wahl: " o
        case $o in
            1) choose_run_mode "o1" "--architect" ;;
            2) choose_run_mode "o3-mini" "--architect" ;;
            3) choose_run_mode "gpt-4o" "" ;;
            4) choose_run_mode "gpt-4o-mini" "" ;;
            0) return ;;
        esac
    done
}

menu_deepseek() {
    while true; do
        clear
        echo -e "${Y}=== PROVIDER: DEEPSEEK ===${NC}"
        echo -e "${Y}1) DeepSeek V3${NC} (Chat/Coding)"
        echo -e "${Y}2) DeepSeek R1${NC} (Reasoning/Thinking)"
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Wahl: " d
        case $d in
            1) choose_run_mode "deepseek/deepseek-chat" "" ;;
            2) choose_run_mode "deepseek/deepseek-reasoner" "--architect" ;;
            0) return ;;
        esac
    done
}

menu_claude() {
    while true; do
        clear
        echo -e "${G}=== PROVIDER: CLAUDE (ANTHROPIC) ===${NC}"
        echo -e "${G}1) Claude 3.5 Sonnet${NC} (Best Coding Performance)"
        echo -e "${G}2) Claude 3.5 Haiku${NC} (Fast)"
        echo -e "${G}3) Claude 3 Opus${NC} (Creative Reasoning)"
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Wahl: " cl
        case $cl in
            1) choose_run_mode "claude-3-5-sonnet-20241022" "--architect" ;;
            2) choose_run_mode "claude-3-5-haiku-20241022" "" ;;
            3) choose_run_mode "claude-3-opus-20240229" "--architect" ;;
            0) return ;;
        esac
    done
}

# ==========================================
# HAUPTMENÜ
# ==========================================

while true; do
    clear
    echo -e "${C}======================================================${NC}"
    echo -e "${W}${B}         AIDER AI - HAUPTMENÜ                      ${NC}"
    echo -e "${C}======================================================${NC}"
    load_secrets

    echo -e "${C}1) Gemini${NC}"
    echo -e "${W}2) OpenAI${NC}"
    echo -e "${Y}3) DeepSeek${NC}"
    echo -e "${G}4) Claude${NC}"
    echo -e "------------------------------------------------------"
    echo -e "${R}5) Beenden${NC}"
    echo ""
    read -p "Provider wählen: " main_choice

    case $main_choice in
        1) menu_gemini ;;
        2) menu_openai ;;
        3) menu_deepseek ;;
        4) menu_claude ;;
        5) exit 0 ;;
        *) echo -e "${R}Ungültige Eingabe!${NC}"; sleep 1 ;;
    esac
done