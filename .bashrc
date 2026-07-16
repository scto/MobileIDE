# --- Git Terminal Aliase ---
alias gc='git clone'
alias gcr='git clone --recursive'
alias ga='git add'
alias gcm='git commit'
alias gco='git checkout'
alias gl='git pull'
alias gp='git push'
alias gs='git status'
alias gcb='git checkout -b'           # Neuen Branch erstellen & wechseln
alias gd='git diff'                   # Unterschiede sehen
alias glog='git log --graph --oneline --all'
alias gcmm='git commit -m'            # Direktes Committen mit Nachricht

# --- LS & Datei Aliase ---
# ls mit deinen Anforderungen (Farbe, hidden, Ordner zuerst)
# KORREKTUR: "--color=always" verträgt sich oft nicht mit Pipes wie "column". "auto" ist sicherer.
alias ll='ls -A --group-directories-first --color=auto | column -c 2'
alias l='ls -CF'                      # Standard kompakt
alias la='ls -A'                      # Alle inkl. hidden
alias l.='ls -d .* --color=auto'      # Nur versteckte Dateien anzeigen

# --- Navigation & System ---
alias ..='cd ..'
alias ...='cd ../..'
alias c='clear'
alias h='history'
# Schneller Zugriff auf deine Android-Projekte
alias cdide='cd ~/projects/MobileIDE' 

# --- Kotlin/Java Workflow (Praktisch für Termux/MobileIDE) ---
alias ktc='kotlinc'                   # Kotlin Compiler
alias ktr='kotlin'                    # Kotlin Runner

# ==========================================
# 1. API Token & Secrets laden (Optimiert & DRY)
# ==========================================
# KORREKTUR: Schleife statt 7-mal fast identischem Code schont die Startzeit der Shell
SECRETS_DIR="$HOME/.secrets"
if [ -d "$SECRETS_DIR" ]; then
    for secret_path in "$SECRETS_DIR"/.*_key.secrets "$SECRETS_DIR"/.github_token.secrets; do
        if [ -f "$secret_path" ]; then
            # Extrahiere den Variablen-Namen aus dem Dateinamen
            # z.B. .gemini_api_key.secrets -> GEMINI_API_KEY
            var_name=$(basename "$secret_path" | sed -e 's/^\.//' -e 's/\.secrets$//' | tr '[:lower:]' '[:upper:]')
            export "$var_name"="$(cat "$secret_path" | xargs)"
        fi
    done
fi

# ==========================================
# 2. Android SDK & NDK Variablen
# ==========================================
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# NDK Pfad (Falls du eine spezifische Version hast, z.B. ndk/26.1.10909125 eintragen)
export NDK_HOME="$ANDROID_HOME/ndk/latest"
export ANDROID_NDK_HOME="$NDK_HOME"

# ==========================================
# 3. Java & Gradle Environment
# ==========================================
# KORREKTUR: "/" am Ende von JAVA_HOME entfernt (Konvention)
export JAVA_HOME="$PREFIX/lib/jvm/java-17-openjdk"

# Memory Limit für die JVM / den Gradle Daemon
# OPTIMIERUNG: -Dorg.gradle.jvmargs hinzugefügt, da Gradle _JAVA_OPTIONS manchmal ignoriert
export _JAVA_OPTIONS="-Xmx8G -Djava.awt.headless=true -Dorg.gradle.jvmargs=-Xmx8G"

# Gradle Cache-Verzeichnis explizit setzen
export GRADLE_USER_HOME="$HOME/.gradle"

# ==========================================
# 4. PATH Variablen setzen
# ==========================================
# KORREKTUR: PATH-Zuweisung zusammengefasst, um Dopplungen zu vermeiden und sauber zu priorisieren
export PATH="$JAVA_HOME/bin:$NDK_HOME:$ANDROID_HOME/cmake/bin:$HOME/.androidide/aapt2:$HOME/.local/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/tools:$PATH"

