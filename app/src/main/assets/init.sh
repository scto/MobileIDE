#!/bin/bash

set -e

# 1. Environment variable configuration
# HOME still points to /root so that the program can find configuration files (like .bashrc), but we will manually cd to / later.
export PATH=/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin
export HOME=/root

# 2. Write ReTerminal style welcome message to /etc/motd
cat > /etc/motd <<'EOF'
Welcome to ReTerminal!

The Alpine Wiki contains a large amount of how-to guides and general
information about administrating Alpine systems.
See <https://wiki.alpinelinux.org/>.

Installing : apk add <pkg>
Updating : apk update && apk upgrade
EOF

# 3. Configure DNS (if not already configured)
if [ ! -s /etc/resolv.conf ]; then
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
fi

# 4. Beautify terminal prompt (Green username@hostname current_path $)
export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@localhost \[\033[39m\]\w \[\033[0m\]\\$ "

# 5. Check and initialize environment (Node.js & LSP)
# Only execute on the first run (when the node command is not found)
if ! command -v node > /dev/null 2>&1; then
    echo -e "\e[34;1m[*] \e[0mInitializing Environment (Alpine 3.21.0)...\e[0m"

    # Update the repository index and install basic dependencies
    apk update
    apk add bash gcompat glib nano nodejs npm

    # Install LSP services (WebIDE core function)
    echo -e "\e[34;1m[*] \e[0mInstalling Language Servers...\e[0m"
    rm -rf /usr/local/lib/node_modules
    npm install -g typescript typescript-language-server vscode-langservers-extracted

    echo -e "\e[32;1m[+] \e[0mEnvironment Ready! Please restart the app if LSP doesn't work immediately.\e[0m"
fi

# 6. Start an interactive Shell
if [ "$#" -eq 0 ]; then
    # Load system configuration
    if [ -f /etc/profile ]; then
        source /etc/profile
    fi

    # Display welcome message
    if [ -f /etc/motd ]; then
        cat /etc/motd
        echo "" # Leave a blank line for aesthetics
    fi

    # [🔥 Core modification] Enter the Alpine system root directory / by default
    cd /

    # Start Bash
    /bin/bash
else
    # If parameters are passed (e.g., executing a specific command), execute them directly without entering interactive mode
    exec "$@"
fi