export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export ROOT=/root
export HOME=/home

if [ ! -s /etc/resolv.conf ]; then
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
fi

export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\\$ "
# shellcheck disable=SC2034
export PIP_BREAK_SYSTEM_PACKAGES=1
required_packages="bash gcompat glib nano curl git ca-certificates"
missing_packages=""
for pkg in $required_packages; do
    if ! apk info -e $pkg >/dev/null 2>&1; then
        missing_packages="$missing_packages $pkg"
    fi
done
if [ -n "$missing_packages" ]; then
    echo -e "\e[34;1m[*] \e[0mInstalling Important packages\e[0m"
    apk update && apk upgrade
    apk add $missing_packages
    if [ $? -eq 0 ]; then
        echo -e "\e[32;1m[+] \e[0mSuccessfully Installed\e[0m"
    fi
    echo -e "\e[34m[*] \e[0mUse \e[32mapk\e[0m to install new packages\e[0m"
fi

#fix linker warning
if [[ ! -f /linkerconfig/ld.config.txt ]];then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

# Setup bash history and colors in ~/.bashrc and ~/.profile
if [ ! -f "$HOME/.bashrc" ]; then
    cat << 'EOF' > "$HOME/.bashrc"
# Enable color support of ls and also add handy aliases
alias ls='ls --color=auto'
alias dir='dir --color=auto'
alias vdir='vdir --color=auto'
alias grep='grep --color=auto'
alias fgrep='fgrep --color=auto'
alias egrep='egrep --color=auto'

# Colored prompt
export PS1='\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\$ '

# Bash history settings
export HISTFILE=$HOME/.bash_history
export HISTSIZE=2000
export HISTFILESIZE=5000
export HISTCONTROL=ignoreboth
shopt -s histappend
EOF
fi

if [ ! -f "$HOME/.profile" ]; then
    cat << 'EOF' > "$HOME/.profile"
if [ -n "$BASH_VERSION" ]; then
    if [ -f "$HOME/.bashrc" ]; then
        . "$HOME/.bashrc"
    fi
fi
EOF
fi

if [ "$#" -eq 0 ]; then
    source /etc/profile
    export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\\$ "
    cd $HOME

    echo -e "\e[32;1mWillkommen bei MobileIDE Terminal.\e[0m"
    echo -e "\e[34mNutze den Paketmanager \e[32mapk\e[0m für diese Alpine-Distribution."
    echo ""
    echo ""
    echo ""
    echo -e "  \e[33mapk update && apk upgrade && apk add <paket>\e[0m"
    echo ""

    if [ -x /bin/bash ]; then
        exec /bin/bash
    elif [ -x /usr/bin/bash ]; then
        exec /usr/bin/bash
    else
        exec /bin/ash
    fi
else
    exec "$@"
fi