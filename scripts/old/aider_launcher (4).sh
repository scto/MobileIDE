```bash
#!/bin/bash

# --- FARBEN ---
G="\e[0;32m"; C="\e[0;36m"; Y="\e[0;33m"; R="\e[0;31m"; W="\e[0;37m"; M="\e[0;35m"; B="\e[1m"; NC="\033[0m"

# --- PFADE (Vom Setup-Script injiziert) ---
VENV_PATH_VAL="/data/data/jkas.androidpe/files/home/.venv"
SECRET_GEMINI="/data/data/jkas.androidpe/files/home/.gemini_api_key.secrets"
SECRET_ANTHROPIC="/data/data/jkas.androidpe/files/home/.anthropic_api_key.secrets"
SECRET_OPENAI="/data/data/jkas.androidpe/files/home/.openai_api_key.secrets"
SECRET_DEEPSEEK="/data/data/jkas.androidpe/files/home/.deepseek_api_key.secrets"

# --- GLOBALE STATUS-VARIABLEN ---
SUBTREE_MODE=false

# Arrays für Chat-Modes
CHAT_MODES=("auto" "code" "architect" "ask" "help")
CHAT_MODE_DESCS=(
    "Launcher-Empfehlung (Standard)" 
    "Direktes Coden (am besten für Flash)" 
    "Planen & Coden (am besten für Pro/Komplexes)" 
    "Nur Fragen stellen (keine Dateiänderungen)" 
    "Fragen zur Bedienung von Aider"
)
CHAT_MODE_IDX=0
CURRENT_CHAT_MODE=${CHAT_MODES[$CHAT_MODE_IDX]}
CURRENT_CHAT_DESC=${CHAT_MODE_DESCS[$CHAT_MODE_IDX]}

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
    
    # Füge subtree-only hinzu, falls im Menü aktiviert (Lösung für >1000 Dateien)
    if [ "$SUBTREE_MODE" = true ]; then
        flags="$flags --subtree-only"
    fi

    # Füge Chat-Mode Logik hinzu
    if [ "$CURRENT_CHAT_MODE" != "auto" ]; then
        # Entferne eventuelles hartcodiertes --architect aus base_flags, um Konflikte zu vermeiden
        flags=${flags//--architect/}
        flags="$flags --chat-mode $CURRENT_CHAT_MODE"
    fi

    echo -e "\n${G}Starte Aider mit ${W}$model${NC}..."
    [ "$SUBTREE_MODE" = true ] && echo -e "${Y}Info: Large Repo Mode (--subtree-only) ist AKTIV.${NC}"
    echo -e "${Y}Info: Chat-Mode: ${W}${CURRENT_CHAT_MODE}${NC}"
    echo -e ""

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
        local status_color=$R; [ "$SUBTREE_MODE" = true ] && status_color=$G
        
        echo -e "${C}=== AUSFÜHRUNGSMODUS ===${NC}"
        echo -e "Modell: ${W}$model${NC}"
        echo -e "Subtree-only (Large Repo Fix): ${status_color}$( [ "$SUBTREE_MODE" = true ] && echo "AN" || echo "AUS" )${NC}"
        echo -e "------------------------------------------------------"
        echo -e "${G}1) Run Aider Console${NC}"
        echo -e "${C}2) Run Aider Browser${NC}"
        echo -e "------------------------------------------------------"
        echo -e "${Y}3) Toggle Large Repo Mode (--subtree-only)${NC}"
        echo -e "${M}4) Cycle Chat-Mode: ${W}${CURRENT_CHAT_MODE}${NC} ${C}(${CURRENT_CHAT_DESC})${NC}"
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Wahl: " run_choice

        case $run_choice in
            1) run_aider "$model" "$base_flags" ;;
            2) run_aider "$model" "$base_flags --browser" ;;
            3) 
                if [ "$SUBTREE_MODE" = true ]; then SUBTREE_MODE=false; else SUBTREE_MODE=true; fi
                ;;
            4) 
                # Rotiere durch die Chat-Modi
                CHAT_MODE_IDX=$(( (CHAT_MODE_IDX + 1) % 5 ))
                CURRENT_CHAT_MODE=${CHAT_MODES[$CHAT_MODE_IDX]}
                CURRENT_CHAT_DESC=${CHAT_MODE_DESCS[$CHAT_MODE_IDX]}
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
        echo -e "${C}=== GEMINI FLASH MODELLE (High Speed & Effizienz) ===${NC}"
        local mods=(
            "Flash Latest|gemini/gemini-flash-latest|Standard-Modell (Auto-Update)"
            "2.0 Flash|gemini/gemini-2.0-flash|Stabile V2.0"
            "2.0 Flash (001)|gemini/gemini-2.0-flash-001|Stabile V2.0.1"
            "2.5 Flash|gemini/gemini-2.5-flash|High Speed & Logik"
            "2.5 Flash (09-25)|gemini/gemini-2.5-flash-preview-09-2025|V2.5 Preview"
            "3.0 Flash Preview|gemini/gemini-3-flash-preview|Early Access V3"
            "Flash Lite Latest|gemini/gemini-flash-lite-latest|Günstigstes Auto-Update"
            "2.0 Flash Lite|gemini/gemini-2.0-flash-lite|V2.0 Lite"
            "2.0 Flash Lite (001)|gemini/gemini-2.0-flash-lite-001|V2.0.1 Lite"
            "2.5 Flash Lite|gemini/gemini-2.5-flash-lite|V2.5 Lite"
            "2.5 Flash Lite (06-17)|gemini/gemini-2.5-flash-lite-preview-06-17|Build 06-17"
            "2.5 Flash Lite (09-25)|gemini/gemini-2.5-flash-lite-preview-09-2025|Build 09-25"
            "3.1 Flash Lite Preview|gemini/gemini-3.1-flash-lite-preview|V3.1 Lite"
            "2.5 Flash Audio Latest|gemini/gemini-2.5-flash-native-audio-latest|Native Audio"
            "2.5 Flash Audio (09-25)|gemini/gemini-2.5-flash-native-audio-preview-09-2025|Audio Build"
            "2.5 Flash Audio (12-25)|gemini/gemini-2.5-flash-native-audio-preview-12-2025|Audio Build"
            "3.1 Flash Live Preview|gemini/gemini-3.1-flash-live-preview|Echtzeit Interaktion"
            "Gemma 2 9b IT|gemini/gemini-gemma-2-9b-it|Open Weights Klein"
            "Gemma 2 27b IT|gemini/gemini-gemma-2-27b-it|Open Weights Mittel"
            "Gemma 3 27b IT|gemini/gemma-3-27b-it|Open Weights V3"
        )
        for i in "${!mods[@]}"; do
            IFS='|' read -r name id desc <<< "${mods[$i]}"
            printf "${W}%2d)${NC} ${C}%-28s${NC} %s\n" "$((i+1))" "$name" "$desc"
        done
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Auswahl: " c
        [[ "$c" == "0" ]] && return
        if [[ "$c" -ge 1 && "$c" -le "${#mods[@]}" ]]; then
            IFS='|' read -r name id desc <<< "${mods[$((c-1))]}"
            # Standardmäßig keinen --architect Flag für Flash (Code-Modus ist meist besser)
            choose_run_mode "$id" ""
        fi
    done
}

menu_gemini_pro() {
    while true; do
        clear
        echo -e "${M}=== GEMINI PRO MODELLE (Intelligence & Experimental) ===${NC}"
        local mods=(
            "Pro Latest|gemini/gemini-pro-latest|Top Modell (Auto-Update)"
            "2.5 Pro (Kotlin Best)|gemini/gemini-2.5-pro|Hochkomplexe Logik"
            "2.5 Pro Exp (03-25)|gemini/gemini-2.5-pro-exp-03-25|Experimental Build"
            "2.5 Pro (05-06)|gemini/gemini-2.5-pro-preview-05-06|Build 05-06"
            "2.5 Pro (06-05)|gemini/gemini-2.5-pro-preview-06-05|Build 06-05"
            "3.0 Pro Preview|gemini/gemini-3-pro-preview|Next Gen Pro"
            "3.1 Pro Preview|gemini/gemini-3.1-pro-preview|V3.1 Flaggschiff"
            "2.5 Pro TTS|gemini/gemini-2.5-pro-preview-tts|Text-To-Speech Spezial"
            "3.1 Pro Custom Tools|gemini/gemini-3.1-pro-preview-customtools|Agenten & Tools"
            "2.5 Computer Use|gemini/gemini-2.5-computer-use-preview-10-2025|UI Control / Agent"
            "Robotics ER 1.5|gemini/gemini-robotics-er-1.5-preview|Robotics Focus"
            "LearnLM 1.5 Pro|gemini/learnlm-1.5-pro-experimental|Education Focus"
            "Lyria 3 Clip Preview|gemini/lyria-3-clip-preview|Audio Embedding"
            "Lyria 3 Pro Preview|gemini/lyria-3-pro-preview|Music/Audio Gen"
            "Exp 1114|gemini/gemini-exp-1114|Experimental Nov"
            "Exp 1206|gemini/gemini-exp-1206|Experimental Dec"
        )
        for i in "${!mods[@]}"; do
            IFS='|' read -r name id desc <<< "${mods[$i]}"
            printf "${W}%2d)${NC} ${M}%-28s${NC} %s\n" "$((i+1))" "$name" "$desc"
        done
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Auswahl: " c
        [[ "$c" == "0" ]] && return
        if [[ "$c" -ge 1 && "$c" -le "${#mods[@]}" ]]; then
            IFS='|' read -r name id desc <<< "${mods[$((c-1))]}"
            local f="--architect"
            [[ "$id" == *"computer-use"* ]] && f="--browser"
            choose_run_mode "$id" "$f"
        fi
    done
}

menu_gemini() {
    while true; do
        clear
        echo -e "${C}=== PROVIDER: GEMINI ===${NC}"
        echo -e "${C}1) Flash Modelle${NC} (Schnell, Effizient, Standard Coding)"
        echo -e "${M}2) Pro Modelle${NC}   (Intelligent, Planung, Kotlin-Architektur)"
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

```
