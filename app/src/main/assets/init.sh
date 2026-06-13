#!/bin/bash

set -e

# 1. Environment variable configuration
export ANDROID_HOME=/opt/android-sdk
export ANDROID_ROOT=/system
export PROJECTS=/storage/emulated/0/MobileIDEProjects
export HOME=/root
export _JAVA_OPTIONS="-Xint"
export JAVA_TOOL_OPTIONS="-Xint"
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmake/3.22.1/bin:/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin

# 2. Write MobileIDETerminal style welcome message to /etc/motd
cat > /etc/motd <<'EOF'
Welcome to MobileIDETerminal!

The Alpine Wiki contains a large amount of how-to guides and general
information about administrating Alpine systems.
See <https://wiki.alpinelinux.org/>.

How to upgrade development tools:
- Upgrade Android SDK, Build-tools, NDK, CMake:
  sdkmanager --update
  (or install specific versions: sdkmanager "ndk;28.0.x" "platforms;android-34")
- Upgrade compiler & system packages (clang, llvm, make, nodejs, etc.):
  apk update && apk upgrade

Installing : apk add <pkg>
Updating   : apk update && apk upgrade
EOF

# 3. Configure DNS (if not already configured)
if [ ! -s /etc/resolv.conf ]; then
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
fi

# 4. Configure inputrc (arrow keys for history search) and bashrc (color support, bash completion, Android SDK paths)
cat > /root/.inputrc <<'EOF'
"\e[A": history-search-backward
"\e[B": history-search-forward
EOF

cat > /root/.bashrc <<'EOF'
# Source system-wide settings
if [ -f /etc/profile ]; then
    . /etc/profile
fi

# Android environment variables
export ANDROID_HOME=/opt/android-sdk
export ANDROID_ROOT=/system
export PROJECTS=/storage/emulated/0/MobileIDEProjects
export _JAVA_OPTIONS="-Xint"
export JAVA_TOOL_OPTIONS="-Xint"
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmake/3.22.1/bin:$PATH

# Colorized aliases
alias ls='ls --color=auto'
alias grep='grep --color=auto'
alias fgrep='fgrep --color=auto'
alias egrep='egrep --color=auto'
alias ll='ls -alF'
alias la='ls -A'
alias l='ls -CF'

# Colorized prompt (Green username@hostname current_path $)
export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@localhost \[\033[39m\]\w \[\033[0m\]\\$ "

# Bash completion
if [ -f /usr/share/bash-completion/bash_completion ]; then
    . /usr/share/bash-completion/bash_completion
fi
EOF

# 5. Check and initialize Alpine packages
echo -e "\e[34;1m[*] \e[0mChecking system packages...\e[0m"
REQUIRED_PACKAGES="bash gcompat glib nano nodejs npm bash-completion openjdk17 jdtls wget unzip clang llvm make gcc g++ binutils musl-dev git libc-dev paxctl"
MISSING_PACKAGES=""
for pkg in $REQUIRED_PACKAGES; do
    if ! apk info -e $pkg >/dev/null 2>&1; then
        MISSING_PACKAGES="$MISSING_PACKAGES $pkg"
    fi
done

if ! apk info -e kotlin-language-server >/dev/null 2>&1; then
    INSTALL_KOTLIN=true
else
    INSTALL_KOTLIN=false
fi

if [ -n "$MISSING_PACKAGES" ] || [ "$INSTALL_KOTLIN" = true ]; then
    echo -e "\e[34;1m[*] \e[0mUpdating repositories and installing missing packages:$MISSING_PACKAGES...\e[0m"
    apk update
    if [ -n "$MISSING_PACKAGES" ]; then
        apk add $MISSING_PACKAGES
    fi
    if [ "$INSTALL_KOTLIN" = true ]; then
        apk add -X http://dl-cdn.alpinelinux.org/alpine/edge/testing kotlin-language-server
    fi
fi

# Install JS/TS language servers globally if node is installed but they are missing
if ! command -v typescript-language-server >/dev/null 2>&1; then
    echo -e "\e[34;1m[*] \e[0mInstalling JS/TS/HTML/CSS Language Servers...\e[0m"
    rm -rf /usr/local/lib/node_modules
    npm install -g typescript typescript-language-server vscode-langservers-extracted
fi

# 6. Check and initialize Android SDK & NDK
if [ ! -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo -e "\e[34;1m[*] \e[0mAndroid SDK Command-Line Tools not found. Installing...\e[0m"
    mkdir -p "$ANDROID_HOME/temp"
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O "$ANDROID_HOME/temp/cmdline.zip"
    unzip -q "$ANDROID_HOME/temp/cmdline.zip" -d "$ANDROID_HOME/temp"
    mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
    # Ensure correct structure of cmdline-tools
    mv "$ANDROID_HOME/temp/cmdline-tools/"* "$ANDROID_HOME/cmdline-tools/latest/"
    rm -rf "$ANDROID_HOME/temp"
    echo -e "\e[32;1m[+] \e[0mAndroid SDK Command-Line Tools installed successfully.\e[0m"
fi

if [ ! -d "$ANDROID_HOME/platforms/android-33" ] || [ ! -d "$ANDROID_HOME/build-tools/33.0.2" ] || [ ! -d "$ANDROID_HOME/ndk/27.0.12077973" ] || [ ! -d "$ANDROID_HOME/platform-tools" ] || [ ! -d "$ANDROID_HOME/cmake/3.22.1" ]; then
    echo -e "\e[34;1m[*] \e[0mInstalling required Android SDK components: platform-tools, platforms;android-33, build-tools;33.0.2, ndk;27.0.12077973, cmake;3.22.1...\e[0m"
    yes | sdkmanager --licenses > /dev/null 2>&1 || true
    sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.2" "ndk;27.0.12077973" "cmake;3.22.1"
    echo -e "\e[32;1m[+] \e[0mAndroid SDK components installed successfully.\e[0m"
fi

# Apply paxctl -m to all executables in the Android SDK and JVM to bypass PaX/grsecurity MPROTECT restrictions
if command -v paxctl > /dev/null 2>&1; then
    echo -e "\e[34;1m[*] \e[0mApplying paxctl -m to Android SDK and JVM executables..."
    find /opt/android-sdk -type f -executable -exec paxctl -m {} \; 2>/dev/null || true
    find /usr/lib/jvm -type f -executable -exec paxctl -m {} \; 2>/dev/null || true
fi

# 7. Start an interactive Shell
if [ "$#" -eq 0 ]; then
    # Load system configuration
    if [ -f /etc/profile ]; then
        source /etc/profile
    fi

    # Clear screen after setup, before motd is shown
    clear

    # Display welcome message
    if [ -f /etc/motd ]; then
        cat /etc/motd
        echo "" # Leave a blank line for aesthetics
    fi

    # [🔥 Core modification] Enter project directory or workspace if available
    if [ -n "$MOBILEIDE_PROJECT_DIR" ] && [ -d "$MOBILEIDE_PROJECT_DIR" ]; then
        cd "$MOBILEIDE_PROJECT_DIR"
    elif [ -n "$MOBILEIDE_WORKSPACE" ] && [ -d "$MOBILEIDE_WORKSPACE" ]; then
        cd "$MOBILEIDE_WORKSPACE"
    else
        cd /
    fi

    # Start Bash
    /bin/bash
else
    # If parameters are passed (e.g., executing a specific command), execute them directly without entering interactive mode
    exec "$@"
fi