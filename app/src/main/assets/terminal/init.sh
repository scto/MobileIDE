force_color_prompt=yes
shopt -s checkwinsize

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/games:/usr/local/bin:/usr/local/sbin:$LOCAL/bin:$PATH
export SHELL="bash"
export PS1="\[\e[1;32m\]\u@\h\[\e[0m\]:\[\e[1;34m\]\w\[\e[0m\] \\$ "

source "$LOCAL/bin/utils"

# Set timezone dynamically depending on OS
CONTAINER_TIMEZONE="UTC"  # or any timezone like "Asia/Kolkata"

if [ "$OS_TYPE" = "alpine" ]; then
    $PKG_INSTALL tzdata >/dev/null 2>&1 || true
    if [ -f "/usr/share/zoneinfo/$CONTAINER_TIMEZONE" ]; then
        cp "/usr/share/zoneinfo/$CONTAINER_TIMEZONE" /etc/localtime
        echo "$CONTAINER_TIMEZONE" > /etc/timezone
    fi
else
    ln -snf "/usr/share/zoneinfo/$CONTAINER_TIMEZONE" /etc/localtime
    echo "$CONTAINER_TIMEZONE" > /etc/timezone
    DEBIAN_FRONTEND=noninteractive dpkg-reconfigure -f noninteractive tzdata >/dev/null 2>&1
fi

if [[ -f ~/.bashrc ]]; then
    # shellcheck disable=SC1090
    source ~/.bashrc
fi

ensure_packages_once() {
    local marker_file="/.cache/.packages_ensured"
    
    # Exit early if already done
    [[ -f "$marker_file" ]] && return 0

    # Create cache dir
    mkdir -p "/.cache"

    # OS-specific base packages
    local MISSING=()
    if [ "$OS_TYPE" = "ubuntu" ]; then
        echo 'APT::Install-Recommends "false";' > /etc/apt/apt.conf.d/99norecommends
        echo 'APT::Install-Suggests "false";' >> /etc/apt/apt.conf.d/99norecommends
        local PACKAGES=("command-not-found" "sudo" "xkb-data" "libjemalloc-dev")
        for pkg in "${PACKAGES[@]}"; do
            if ! dpkg -s "$pkg" >/dev/null 2>&1; then MISSING+=("$pkg"); fi
        done
    elif [ "$OS_TYPE" = "alpine" ]; then
        # Alpine equivalents
        local PACKAGES=("sudo" "jemalloc-dev" "bash-completion")
        for pkg in "${PACKAGES[@]}"; do
            if ! apk info -e "$pkg" >/dev/null 2>&1; then MISSING+=("$pkg"); fi
        done
    fi

    # If nothing missing, just mark as done
    if [ ${#MISSING[@]} -eq 0 ]; then
        touch "$marker_file"
        return 0
    fi

    info "Installing missing base packages: ${MISSING[*]}"

    if export DEBIAN_FRONTEND=noninteractive && $PKG_UPDATE && $PKG_INSTALL "${MISSING[@]}"; then
       touch "$marker_file"
       clear
       info "Setup complete."
    else
        error "Failed to install packages."
        return 1
    fi

    # Update command-not-found database (Ubuntu only)
    if [ "$OS_TYPE" = "ubuntu" ]; then
        update-command-not-found 2>/dev/null || true
    fi
}

ensure_packages_once
unset -f ensure_packages_once

# Command not found handler (Ubuntu only, Alpine uses standard shell errors)
if [ "$OS_TYPE" = "ubuntu" ]; then
    if [ -x /usr/lib/command-not-found -o -x /usr/share/command-not-found/command-not-found ]; then
        function command_not_found_handle {
            if [ -x /usr/lib/command-not-found ]; then
               /usr/lib/command-not-found -- "$1"
               return $?
            elif [ -x /usr/share/command-not-found/command-not-found ]; then
               /usr/share/command-not-found/command-not-found -- "$1"
               return $?
            else
               printf "%s: command not found\n" "$1" >&2
               return 127
            fi
        }
    fi
fi

alias ls='ls --color=auto'
alias grep='grep --color=auto'
alias egrep='egrep --color=auto'
alias fgrep='fgrep --color=auto'

# Map pkg alias dynamically
if [ "$OS_TYPE" = "alpine" ]; then
    alias pkg='apk'
else
    alias pkg='apt'
fi

# ==========================================
# Smart KI-Assistenz Wrapper (Auto-Install)
# ==========================================
aider() {
    if [ -x /usr/local/bin/aider ]; then
        # Führe das echte Programm aus, wenn es existiert
        /usr/local/bin/aider "$@"
    else
        echo -e "${BOLD_YELLOW}Aider (KI-Assistenz) ist noch nicht installiert.${RESET}"
        if ask "Möchtest du Aider jetzt installieren?"; then
            bash "$LOCAL/bin/tools/aider.sh"
            
            # Nach erfolgreicher Installation direkt den ursprünglichen Befehl ausführen
            if [ -x /usr/local/bin/aider ]; then
                /usr/local/bin/aider "$@"
            fi
        fi
    fi
}

agy() {
    if [ -x /usr/local/bin/agy ]; then
        /usr/local/bin/agy "$@"
    elif [ -x "$HOME/.local/bin/agy" ]; then
        "$HOME/.local/bin/agy" "$@"
    else
        echo -e "${BOLD_YELLOW}Google Antigravity CLI (agy) ist noch nicht installiert.${RESET}"
        if ask "Möchtest du die Antigravity CLI jetzt installieren?"; then
            bash "$LOCAL/bin/tools/google-antigravity-cli.sh"
            
            if [ -x /usr/local/bin/agy ]; then
                /usr/local/bin/agy "$@"
            elif [ -x "$HOME/.local/bin/agy" ]; then
                "$HOME/.local/bin/agy" "$@"
            fi
        fi
    fi
}

if [[ -f /initrc ]]; then
    # shellcheck disable=SC1090
    source /initrc
fi

# ==========================================
# MOTD (Message of the Day)
# ==========================================
print_motd() {
    clear # Leert die Konsolenausgaben, die während des PRoot-Starts entstanden sein könnten
    
    echo -e "${BOLD_BLUE}╭──────────────────────────────────────────────────╮${RESET}"
    echo -e "${BOLD_BLUE}│${RESET}        Willkommen in der MobileIDE               ${BOLD_BLUE}│${RESET}"
    echo -e "${BOLD_BLUE}╰──────────────────────────────────────────────────╯${RESET}"
    
    if [ "$OS_TYPE" = "alpine" ]; then
        echo -e "  ${BOLD_YELLOW}System:${RESET}        Alpine Linux (Leichtgewicht)"
        echo -e "  ${BOLD_YELLOW}Paketmanager:${RESET}  apk (z.B. ${BOLD_BLUE}apk add nano${RESET})"
    else
        echo -e "  ${BOLD_YELLOW}System:${RESET}        Ubuntu (glibc)"
        echo -e "  ${BOLD_YELLOW}Paketmanager:${RESET}  apt (z.B. ${BOLD_BLUE}apt install nano${RESET})"
    fi
    
    echo -e "  ${BOLD_YELLOW}Befehle:${RESET}       Nutze ${BOLD_BLUE}mide --help${RESET} für die IDE-Tools"
    echo -e "  ${BOLD_YELLOW}Code-Runner:${RESET}   ${BOLD_BLUE}run ./dein-script.kt${RESET} (für Kotlin & Co.)"
    echo -e "  ${BOLD_YELLOW}Smart-Tools:${RESET}   Tippe einfach ${BOLD_BLUE}aider${RESET} oder ${BOLD_BLUE}agy${RESET} ein."
    echo -e "                 (Die KI-Tools werden beim allerersten"
    echo -e "                  Aufruf automatisch installiert!)"
    echo -e ""
}

# Begrüßung anzeigen
print_motd

# shellcheck disable=SC2164
cd "$WKDIR" || cd "$HOME"