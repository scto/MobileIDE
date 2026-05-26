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

# --- GLOBALE STATUS-VARIABLEN (Start-Konfiguration) ---
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
    local model="$1"
    local flags="$2"
    
    echo -e "\n${G}Starte Aider mit Modell: ${W}$model${NC}"
    
    # Sicherstellen, dass das VENV existiert, bevor wir es aktivieren
    if [ -f "$VENV_PATH_VAL/bin/activate" ]; then
        source "$VENV_PATH_VAL/bin/activate"
    else
        echo -e "${R}Fehler: Virtual Environment nicht gefunden unter $VENV_PATH_VAL${NC}"
        echo -e "${Y}Bitte überprüfe die Pfade. Drücke Enter, um zurückzukehren.${NC}"
        read -r
        return
    fi
    
    export AIDER_MODEL="$model"
    
    # Kommando als Array aufbauen
    local aider_cmd=("aider" "--model" "$model")
    
    # --- PROJEKT-KONTEXT & CONFIG HINZUFÜGEN ---
    echo -e "${C}Prüfe auf lokale Projekt-Kontextdateien...${NC}"
    
    if [ -f ".aider.conf.yml" ]; then
        aider_cmd+=("--config" ".aider.conf.yml")
        echo -e "  ${G}✓${NC} Gefunden: .aider.conf.yml"
    fi

    # Aktualisierte Liste basierend auf deinen McsIDE Entwicklungs-Dokumenten
    local context_files=(
        "activeDevelopment.md"
        "progress.md"
        "systemDesign.md"
        "testStrategy.md"
        "uiStrategy.md"
        "techEnvironment.md"
    )

    # Wenn die Dateien in einem Unterordner liegen, passe den Pfad hier temporär an.
    # Da sie meist im Root liegen, suchen wir sie direkt dort.
    for file in "${context_files[@]}"; do
        if [ -f "$file" ]; then
            aider_cmd+=("--read" "$file")
            echo -e "  ${G}✓${NC} Lese als Kontext: $file"
        fi
    done
    echo ""
    
    # --- ZUSÄTZLICHE FLAGS VERARBEITEN ---
    if [ -n "$flags" ]; then
        echo -e "Zusätzliche Flags: ${Y}$flags${NC}\n"
        # Flags anhand von Leerzeichen in ein Array aufsplitten
        read -r -a flag_array <<< "$flags"
        aider_cmd+=("${flag_array[@]}")
    fi
    
    # Aider mit dem komplettierten Array ausführen
    "${aider_cmd[@]}"
    
    echo -e "\n${Y}Aider beendet. Drücke Enter, um ins Menü zurückzukehren.${NC}"
    read -r
}

choose_run_mode() {
    local model=$1
    local default_arg=$2
    
    local extra_flags=""
    
    # Initiale Zuweisung basierend auf Parametern aus den Menüs
    if [ "$default_arg" == "--architect" ]; then
        CHAT_MODE_IDX=2
    elif [ "$default_arg" == "--browser" ]; then
        CHAT_MODE_IDX=0
        extra_flags="--browser"
    else
        CHAT_MODE_IDX=0
    fi
    
    while true; do
        clear
        echo -e "${C}======================================================${NC}"
        echo -e "${W}${B}         START-KONFIGURATION                       ${NC}"
        echo -e "${C}======================================================${NC}"
        echo -e "Modell: ${G}$model${NC}"
        if [ -n "$extra_flags" ]; then
            echo -e "Spezial-Flags: ${Y}$extra_flags${NC}"
        fi
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
                local final_flags="$extra_flags"
                if [ "$current_mode" != "auto" ]; then
                    final_flags="$final_flags --$current_mode"
                fi
                if [ "$SUBTREE_MODE" = true ]; then
                    final_flags="$final_flags --subtree-only"
                fi
                
                run_aider "$model" "$final_flags"
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
# SUB-MENÜS FÜR GEMINI
# ==========================================
menu_gemini_flash() {
    while true; do
        clear
        echo -e "${C}=== GEMINI FLASH MODELLE (High Speed) ===${NC}"
        echo -e "${W}Info: Flash-Modelle sind blitzschnell und bieten ein ${G}1 Mio. Tokens Kontextfenster${W}."
        echo -e "      Kostenloses Limit (API): ${G}15 Requests/Min | 1 Mio Tokens/Min | 1500 Req/Tag${W}.${NC}\n"
        local mods=(
            "Gemini 3.1 Flash Lite|gemini/gemini-3.1-flash-lite-preview|Lite Preview (1M Tokens Context, extrem schnell)"
            "Gemini 3.1 Flash Live|gemini/gemini-3.1-flash-live-preview|Live Interaction (1M Tokens Context)"
            "Gemini 3 Flash Preview|gemini/gemini-3-flash-preview|Early Access V3 (1M Tokens Context)"
            "Gemini 2.5 Flash (Performance)|gemini/gemini-2.5-flash|Top-Empfehlung für Alltag & Speed! (1M Tokens Context, Free: 15 Req/Min)"
            "Gemini 2.5 Flash Lite|gemini/gemini-2.5-flash-lite|Stable Lite (Effizient, ressourcenschonend, 1M Tokens Context)"
            "Gemini 2.5 Flash Lite (06-17)|gemini/gemini-2.5-flash-lite-preview-06-17|Build 06-17"
            "Gemini 2.5 Flash Lite (09-25)|gemini/gemini-2.5-flash-lite-preview-09-2025|Build 09-25"
            "Gemini 2.5 Flash Preview (09-25)|gemini/gemini-2.5-flash-preview-09-2025|Preview"
            "Gemini 2.5 Computer Use|gemini/gemini-2.5-computer-use-preview-10-2025|Vision/UI Control (Spezialmodell für Browser/UI)"
            "Gemini 2.5 Flash Native Audio|gemini/gemini-2.5-flash-native-audio-latest|Audio Input (Sprachsteuerung)"
            "Gemini 2.0 Flash|gemini/gemini-2.0-flash|Stable 2.0 (Sehr schnell, 1M Tokens Context)"
            "Gemini 2.0 Flash Lite|gemini/gemini-2.0-flash-lite|Lite 2.0 (1M Tokens Context)"
            "Gemini 2.0 Flash Exp|gemini/gemini-2.0-flash-exp|Experimentell, blitzschnell & clever (1M Tokens Context)"
            "Gemini 2.0 Flash (001)|gemini/gemini-2.0-flash-001|V2.0.1 Update (1M Tokens Context)"
            "Gemini 2.0 Flash Lite (001)|gemini/gemini-2.0-flash-lite-001|V2.0.1 Lite (1M Tokens Context)"
            "Gemini 1.5 Flash|gemini/gemini-1.5-flash|Legacy Stable (1M Tokens Context)"
            "Gemini 1.5 Flash (002)|gemini/gemini-1.5-flash-002|V1.5.2 Update (1M/2M Tokens Context)"
            "Gemini 1.5 Flash Latest|gemini/gemini-1.5-flash-latest|1M/2M Context (Ideal um effizient riesige Codebases zu verarbeiten)"
            "Gemini Flash Latest|gemini/gemini-flash-latest|Auto-Update Flash (Immer die aktuellste Version, 1M Tokens Context)"
            "Gemini Flash Lite Latest|gemini/gemini-flash-lite-latest|Auto-Update Lite (1M Tokens Context)"
        )
        for i in "${!mods[@]}"; do
            IFS='|' read -r name id desc <<< "${mods[$i]}"
            printf "${W}%2d)${NC} ${C}%s${NC}\n" "$((i+1))" "$name"
            echo -e "    ${Y}↳ $desc${NC}"
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
        echo -e "${M}=== GEMINI PRO MODELLE (Intelligence Focus) ===${NC}"
        echo -e "${W}Info: Pro-Modelle bieten tiefgreifende Logik und ein ${M}2 Mio. Tokens Kontextfenster${W}."
        echo -e "      Kostenloses Limit (API): ${M}2 Requests/Min | 32.000 Tokens/Min | 50 Req/Tag${W}.${NC}\n"
        local mods=(
            "Gemini 3.1 Pro (Latest Preview)|gemini/gemini-3.1-pro-preview|Top Intelligence & Logik (2M Tokens Context)"
            "Gemini 3.1 Pro Tools|gemini/gemini-3.1-pro-preview-customtools|Fokus auf Function Calling"
            "Gemini 3 Pro Preview|gemini/gemini-3-pro-preview|Next Gen Pro (2M Tokens Context)"
            "Gemini 2.5 Pro (Kotlin Special)|gemini/gemini-2.5-pro|DIE BESTE WAHL FÜR KOTLIN! Perfekt für komplexe Architektur, Refactoring & Code-Gen. (2M Tokens)"
            "Gemini 2.5 Pro Exp (03-25)|gemini/gemini-2.5-pro-exp-03-25|Experimental Build (2M Tokens Context)"
            "Gemini 2.5 Pro Pre (05-06)|gemini/gemini-2.5-pro-preview-05-06|Build 05-06"
            "Gemini 2.5 Pro Pre (06-05)|gemini/gemini-2.5-pro-preview-06-05|Build 06-05"
            "Gemini 2.5 Pro TTS|gemini/gemini-2.5-pro-preview-tts|Speech Synthesis Spezialist"
            "Gemini 2.0 Pro Exp|gemini/gemini-2.0-pro-exp-02-05|Logic Specialist (Hervorragend für komplexe Algorithmen, 2M Tokens Context)"
            "Gemini 1.5 Pro|gemini/gemini-1.5-pro|Robuster Allrounder mit riesigem 2M Tokens Fenster (Zuverlässig für alle Sprachen)"
            "Gemini Pro Latest|gemini/gemini-pro-latest|Auto-Update Pro (Immer die aktuellste Pro-Version, 2M Tokens Context)"
            "Gemini Exp 1206|gemini/gemini-exp-1206|Experimental Dec (2M Tokens Context)"
            "Gemini Exp 1114|gemini/gemini-exp-1114|Experimental Nov (2M Tokens Context)"
            "LearnLM 1.5 Pro Exp|gemini/learnlm-1.5-pro-experimental|Educational (Optimiert für Erklärungen und Lernen)"
        )
        for i in "${!mods[@]}"; do
            IFS='|' read -r name id desc <<< "${mods[$i]}"
            printf "${W}%2d)${NC} ${M}%s${NC}\n" "$((i+1))" "$name"
            echo -e "    ${Y}↳ $desc${NC}"
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

# ==========================================
# PROVIDER MENÜS
# ==========================================
menu_gemini() {
    while true; do
        clear
        echo -e "${C}=== PROVIDER: GEMINI ===${NC}"
        echo -e "${C}1) Flash Modelle${NC} (Schnell, 1 Mio Tokens, 15 Req/Min)"
        echo -e "${M}2) Pro Modelle${NC} (Intelligent, 2 Mio Tokens, Kotlin-Spezialist)"
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
        echo -e "${W}1) o3-mini${NC} (Reasoning Focus)"
        echo -e "${W}2) GPT-4o${NC} (Allrounder)"
        echo -e "${W}3) GPT-4o-mini${NC} (Fast)"
        echo -e "------------------------------------------------------"
        echo -e "${Y}0) Zurück${NC}"
        read -p "Wahl: " o
        case $o in
            1) choose_run_mode "o3-mini" "--architect" ;;
            2) choose_run_mode "gpt-4o" "" ;;
            3) choose_run_mode "gpt-4o-mini" "" ;;
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
