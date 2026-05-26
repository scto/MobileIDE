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

load_secrets() {
    export UV_LINK_MODE=copy
    [ -f "$SECRET_GEMINI" ] && export GEMINI_API_KEY=$(cat "$SECRET_GEMINI" | xargs)
    [ -f "$SECRET_ANTHROPIC" ] && export ANTHROPIC_API_KEY=$(cat "$SECRET_ANTHROPIC" | xargs)
    [ -f "$SECRET_OPENAI" ] && export OPENAI_API_KEY=$(cat "$SECRET_OPENAI" | xargs)
    [ -f "$SECRET_DEEPSEEK" ] && export DEEPSEEK_API_KEY=$(cat "$SECRET_DEEPSEEK" | xargs)
}

# ==========================================
# START-LOGIK & KONFIGURATION
# ==========================================
run_aider() {
    local model=$1
    local mode_flag=$2
    
    echo -e "${G}Starte Aider mit Modell: ${W}$model ${mode_flag}${NC}"
    source "$VENV_PATH_VAL/bin/activate"
    
    if [ -z "$mode_flag" ]; then
        aider --model "$model"
    else
        # mode_flag wird absichtlich nicht in Anführungszeichen gesetzt, 
        # damit "--architect --subtree-only" als zwei Argumente erkannt werden
        aider --model "$model" $mode_flag
    fi
    
    echo -e "${Y}Aider beendet. Drücke Enter, um ins Menü zurückzukehren.${NC}"
    read
}

choose_run_mode() {
    local model=$1
    local default_mode=$2
    
    # Setze den anfänglichen Modus basierend auf der Empfehlung des Skripts
    if [ "$default_mode" == "--architect" ]; then
        CHAT_MODE_IDX=2
    else
        CHAT_MODE_IDX=0
    fi
    
    while true; do
        clear
        echo -e "${C}======================================================${NC}"
        echo -e "${W}${B}         START-KONFIGURATION                       ${NC}"
        echo -e "${C}======================================================${NC}"
        echo -e "Modell: ${G}$model${NC}"
        echo -e "------------------------------------------------------"
        
        local current_mode="${CHAT_MODES[$CHAT_MODE_IDX]}"
        local current_mode_desc="${CHAT_MODE_DESCS[$CHAT_MODE_IDX]}"
        
        local subtree_status="${R}OFF${NC}"
        if [ "$SUBTREE_MODE" = true ]; then
            subtree_status="${G}ON${NC}"
        fi

        echo -e "${G}1) Starten${NC}"
        echo -e "${C}2) Modus wechseln${NC} : ${W}$current_mode${NC} (${Y}$current_mode_desc${NC})"
        echo -e "${M}3) Sub-Tree Only${NC}  : $subtree_status"
        echo -e "------------------------------------------------------"
        echo -e "${R}0) Abbrechen${NC}"
        
        read -p "Wahl: " config_choice
        
        case $config_choice in
            1)
                # Flag-String dynamisch zusammenbauen
                local flags=""
                if [ "$current_mode" != "auto" ]; then
                    flags="--$current_mode"
                fi
                if [ "$SUBTREE_MODE" = true ]; then
                    flags="$flags --subtree-only"
                fi
                
                run_aider "$model" "$flags"
                return
                ;;
            2)
                # Modus durchwechseln (Cycle)
                CHAT_MODE_IDX=$(( (CHAT_MODE_IDX + 1) % ${#CHAT_MODES[@]} ))
                ;;
            3)
                # Sub-Tree Toggle
                if [ "$SUBTREE_MODE" = true ]; then
                    SUBTREE_MODE=false
                else
                    SUBTREE_MODE=true
                fi
                ;;
            0)
                return
                ;;
            *)
                echo -e "${R}Ungültige Eingabe.${NC}"; sleep 1
                ;;
        esac
    done
}

# ==========================================
# MODELLE AUFLISTEN LOGIK (ALLGEMEIN)
# ==========================================
list_aider_models() {
    local search_term=$1
    echo -e "\n${C}Lade verfügbare Modelle für: ${W}${search_term}${NC}"
    source "$VENV_PATH_VAL/bin/activate"
    
    local raw=$(aider --models "$search_term" 2>/dev/null)
    
    # Basis-Säuberung (Spiegelstriche und Leerzeichen weg)
    local cleaned=$(echo "$raw" | sed 's/^[ \t]*-[ \t]*//' | sed 's/^[ \t]*//' | grep -v '^\s*$')
    
    # Strikter Filter für Gemini: Wir wollen kein OCI, kein Vertex_AI und keine halben Wörter
    if [[ "$search_term" == "gemini/" || "$search_term" == "gemini" ]]; then
        cleaned=$(echo "$cleaned" | grep -i '^gemini/gemini')
    fi
    
    # ABSTEIGEND sortieren (-rV)
    echo "$cleaned" | sort -rV | uniq | while IFS= read -r line; do
        if [ -n "$line" ]; then
            echo -e " - ${W}${line}${NC}"
        fi
    done

    echo -e "\n${Y}Suche beendet. Drücke Enter, um zurückzukehren.${NC}"
    read
}

menu_list_models() {
    while true; do
        clear
        echo -e "${M}======================================================${NC}"
        echo -e "${W}${B}         MODELLE AUFLISTEN                         ${NC}"
        echo -e "${M}======================================================${NC}"
        echo -e "${W}1) List OpenAI Models${NC}"
        echo -e "${G}2) List Anthropic Models${NC}"
        echo -e "${C}3) List Gemini Models${NC}"
        echo -e "${Y}4) List Deepseek Models${NC}"
        echo -e "------------------------------------------------------"
        echo -e "${R}0) Zurück${NC}"
        read -p "Wahl: " list_choice
        case $list_choice in
            1) list_aider_models "openai/" ;;
            2) list_aider_models "anthropic/" ;;
            3) list_aider_models "gemini/" ;;
            4) list_aider_models "deepseek/" ;;
            0) return ;;
            *) echo -e "${R}Ungültige Eingabe.${NC}"; sleep 1 ;;
        esac
    done
}

# ==========================================
# DYNAMISCHE MENÜ-GENERIERUNG & PARSING
# ==========================================

get_description() {
    local m=$1
    
    # --- GEMINI PRO MODELLE ---
    if [[ "$m" == *"gemini-3"* && "$m" == *"pro"* ]]; then echo "(Next-Gen Pro Modell)"
    elif [[ "$m" == *"gemini-2.5-pro"* ]]; then echo "(Brandneues Pro Modell - Höchste Logik)"
    elif [[ "$m" == *"gemini-2.0-pro"* ]]; then echo "(Experimentell: Höchste Intelligenz)"
    elif [[ "$m" == *"gemini-1.5-pro"* ]]; then echo "(2M Tokens - Stabil für riesige Refactorings)"
    
    # --- GEMINI FLASH SPEZIAL-VARIANTEN ---
    elif [[ "$m" == *"thinking"* ]]; then echo "(Deep Reasoning für Code)"
    elif [[ "$m" == *"lite"* ]]; then echo "(Ressourcenschonend & Superschnell)"
    elif [[ "$m" == *"8b"* ]]; then echo "(Blitzschnell, für kleine Tasks)"
    elif [[ "$m" == *"native-audio"* ]]; then echo "(Audio-Fokusiertes Modell)"
    
    # --- GEMINI FLASH STANDARD ---
    elif [[ "$m" == *"gemini-3"* && "$m" == *"flash"* ]]; then echo "(Next-Gen Flash Modell)"
    elif [[ "$m" == *"gemini-2.5-flash"* ]]; then echo "(Brandneuer Flash-Standard)"
    elif [[ "$m" == *"gemini-2.0-flash"* ]]; then echo "(Der schnelle & smarte Standard)"
    elif [[ "$m" == *"gemini-1.5-flash"* ]]; then echo "(1M/2M Tokens - Extrem schnell)"
    
    # --- OPENAI ---
    elif [[ "$m" == *"gpt-4o-mini"* ]]; then echo "(Schnell & Günstig)"
    elif [[ "$m" == *"gpt-4o"* ]]; then echo "(Bestes Allround-Modell)"
    elif [[ "$m" == *"o3-mini"* ]]; then echo "(Neuestes Coding Reasoning)"
    elif [[ "$m" == *"o1-mini"* ]]; then echo "(Mini Reasoning für Code)"
    elif [[ "$m" == *"o1-preview"* ]]; then echo "(Älteres Reasoning)"
    elif [[ "$m" == *"o1"* ]]; then echo "(Deep Reasoning - Finale Version)"
    elif [[ "$m" == *"gpt-4-turbo"* ]]; then echo "(Bewährtes starkes Modell)"
    
    # --- DEEPSEEK ---
    elif [[ "$m" == *"deepseek-reasoner"* || "$m" == *"r1"* ]]; then echo "(Das revolutionäre Deep-Thinking Modell!)"
    elif [[ "$m" == *"deepseek-chat"* || "$m" == *"v3"* ]]; then echo "(Pfeilschnell und extrem smart für Code)"
    elif [[ "$m" == *"deepseek-coder"* ]]; then echo "(Älteres Code Modell)"
    
    # --- CLAUDE ---
    elif [[ "$m" == *"claude-3-7-sonnet"* ]]; then echo "(BRANDNEU: Absoluter Coding-König!)"
    elif [[ "$m" == *"claude-3-5-sonnet"* ]]; then echo "(Der bewährte Vorgänger)"
    elif [[ "$m" == *"haiku"* ]]; then echo "(Schnell & Günstig)"
    elif [[ "$m" == *"opus"* ]]; then echo "(Creative Reasoning)"
    
    # --- FALLBACK ---
    else echo "(Modell der aktuellen Generation)"
    fi
}

get_default_mode() {
    local m=$1
    if [[ "$m" == *"pro"* || "$m" == *"gpt-4o"* || "$m" == *"sonnet"* || "$m" == *"opus"* || "$m" == *"deepseek-chat"* ]]; then
        echo "--architect"
    else
        echo ""
    fi
}

show_dynamic_menu() {
    local search_q=$1
    local filter_re=$2
    local title=$3

    while true; do
        clear
        echo -e "${C}======================================================${NC}"
        echo -e "${W}${B}         ${title}                            ${NC}"
        echo -e "${C}======================================================${NC}"
        echo -e "${Y}Frage API ab, säubere Liste und sortiere Versionen absteigend...${NC}"

        source "$VENV_PATH_VAL/bin/activate" 2>/dev/null
        
        local raw=""
        if [ "$search_q" == "openai" ]; then
            raw=$( (aider --models "gpt"; aider --models "o1"; aider --models "o3") 2>/dev/null )
            # INJEKTION: Garantiert, dass Standard-Modelle nie fehlen
            raw+=$'\ngpt-4o\ngpt-4o-mini\no1\no3-mini\no1-preview\no1-mini'
        elif [ "$search_q" == "gemini" ]; then
            raw=$(aider --models "$search_q" 2>/dev/null)
            # INJEKTION: Garantiert, dass 1.5, 2.0, 2.5 etc. IMMER in der Liste sind
            raw+=$'\ngemini/gemini-1.5-flash-002\ngemini/gemini-1.5-flash-8b\ngemini/gemini-1.5-pro-002\ngemini/gemini-2.0-flash\ngemini/gemini-2.0-flash-thinking-exp-01-21\ngemini/gemini-2.0-pro-exp-02-05\ngemini/gemini-2.5-flash\ngemini/gemini-2.5-pro\ngemini/gemini-3.0-flash\ngemini/gemini-3.1-flash'
        elif [ "$search_q" == "claude" ]; then
            raw=$(aider --models "$search_q" 2>/dev/null)
            raw+=$'\nclaude-3-7-sonnet-20250219\nclaude-3-5-sonnet-20241022\nclaude-3-5-haiku-20241022\nclaude-3-opus-20240229'
        elif [ "$search_q" == "deepseek" ]; then
            raw=$(aider --models "$search_q" 2>/dev/null)
            raw+=$'\ndeepseek/deepseek-reasoner\ndeepseek/deepseek-chat\ndeepseek/deepseek-coder'
        else
            raw=$(aider --models "$search_q" 2>/dev/null)
        fi
        
        # 1. Entferne Spiegelstriche (- ) und führende Leerzeichen
        local cleaned=$(echo "$raw" | sed 's/^[ \t]*-[ \t]*//' | sed 's/^[ \t]*//' | grep -v '^\s*$')
        
        # 2. Anbieter-spezifische STRICKTE Bereinigung
        if [ "$search_q" == "gemini" ]; then
            # Nur saubere Google AI Studio Modelle (gemini/gemini-...), filtert OCI, Vertex_AI, etc. heraus
            cleaned=$(echo "$cleaned" | grep -iE '^gemini/gemini')
        fi
        
        # 3. Anwenden des übergebenen Filters (z.B. "flash" oder "pro") 
        #    UND saubere Version-Sortierung ABSTEIGEND (-rV), Duplikate entfernen (uniq)
        local parsed_models=$(echo "$cleaned" | grep -iE "$filter_re" | sort -rV | uniq)
        
        local models=()
        while IFS= read -r line; do
            local clean_line=$(echo "$line" | tr -d '\r' | xargs)
            if [ -n "$clean_line" ]; then
                models+=("$clean_line")
            fi
        done <<< "$parsed_models"

        clear
        echo -e "${C}======================================================${NC}"
        echo -e "${W}${B}         ${title}                            ${NC}"
        echo -e "${C}======================================================${NC}"

        if [ ${#models[@]} -eq 0 ]; then
            echo -e "${R}Keine Modelle gefunden!${NC}"
            echo -e "Aider konnte keine Modelle für '$search_q' (Filter: '$filter_re') finden."
            echo -e "------------------------------------------------------"
            echo -e "${R}0) Zurück${NC}"
            read -p "Wahl: " choice
            if [[ "$choice" == "0" ]]; then return; fi
            continue
        fi

        local i=1
        for m in "${models[@]}"; do
            local desc=$(get_description "$m")
            # Formatierung: Farbige Nummer, weißer Name, gelbe dynamische Beschreibung
            echo -e "${C}${i}) ${W}${m}${NC} ${Y}${desc}${NC}"
            ((i++))
        done
        echo -e "------------------------------------------------------"
        echo -e "${R}0) Zurück${NC}"
        
        read -p "Wahl: " choice
        
        if [[ "$choice" == "0" ]]; then
            return
        elif [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le "${#models[@]}" ]; then
            local selected_index=$((choice - 1))
            local selected_model="${models[$selected_index]}"
            local default_mode=$(get_default_mode "$selected_model")
            choose_run_mode "$selected_model" "$default_mode"
        else
            echo -e "${R}Ungültige Eingabe.${NC}"; sleep 1
        fi
    done
}

# ==========================================
# UNTERMENÜS FÜR GEMINI (PRO / FLASH)
# ==========================================

menu_gemini_pro() {
    show_dynamic_menu "gemini" "pro" "GEMINI PRO MODELLE"
}

menu_gemini_flash() {
    show_dynamic_menu "gemini" "flash" "GEMINI FLASH MODELLE"
}

menu_gemini() {
    while true; do
        clear
        echo -e "${C}======================================================${NC}"
        echo -e "${W}${B}         GEMINI MODELLE (Google)                   ${NC}"
        echo -e "${C}======================================================${NC}"
        echo -e "Tipp: Für das große Refactoring nutze 2.5 Flash oder 1.5 Pro!"
        echo ""
        echo -e "${C}1) Gemini Pro Modelle${NC}   (Für komplexe Aufgaben & tiefes Verständnis)"
        echo -e "${C}2) Gemini Flash Modelle${NC} (Für riesigen Kontext & maximale Geschwindigkeit)"
        echo -e "------------------------------------------------------"
        echo -e "${R}0) Zurück${NC}"
        read -p "Wahl: " gem
        case $gem in
            1) menu_gemini_pro ;;
            2) menu_gemini_flash ;;
            0) return ;;
            *) echo -e "${R}Ungültige Eingabe.${NC}"; sleep 1 ;;
        esac
    done
}

# ==========================================
# WEITERE UNTERMENÜS FÜR MODELLE ZUM STARTEN
# ==========================================

menu_openai() {
    show_dynamic_menu "openai" "gpt|o1|o3" "OPENAI MODELLE"
}

menu_deepseek() {
    show_dynamic_menu "deepseek" "deepseek" "DEEPSEEK MODELLE"
}

menu_claude() {
    show_dynamic_menu "claude" "claude" "CLAUDE MODELLE"
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

    echo -e "${C}1) Gemini${NC}    (inkl. 2.5 Flash & 1.5 Pro)"
    echo -e "${W}2) OpenAI${NC}    (inkl. o3-mini & GPT-4o)"
    echo -e "${Y}3) DeepSeek${NC}  (inkl. DeepSeek R1)"
    echo -e "${G}4) Claude${NC}    (inkl. Claude 3.7 Sonnet)"
    echo -e "------------------------------------------------------"
    echo -e "${M}5) List Models${NC} (Zeigt verfügbare Modelle der API)"
    echo -e "------------------------------------------------------"
    echo -e "${R}6) Beenden${NC}"
    read -p "Wahl: " main
    case $main in
        1) menu_gemini ;;
        2) menu_openai ;;
        3) menu_deepseek ;;
        4) menu_claude ;;
        5) menu_list_models ;;
        6|0) echo -e "${G}Tschüss!${NC}"; exit 0 ;;
        *) echo -e "${R}Ungültige Eingabe.${NC}"; sleep 1 ;;
    esac
done