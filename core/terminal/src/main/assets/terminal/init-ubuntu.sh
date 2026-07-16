#!/bin/sh
set -e

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/root
export DOTNET_GCHeapHardLimit=1C0000000

resolve_runtime_hostname() {
    if [ -n "$MOBILEIDE_GUEST_HOSTNAME" ]; then
        printf '%s' "$MOBILEIDE_GUEST_HOSTNAME"
        return 0
    fi

    if command -v hostname >/dev/null 2>&1; then
        runtime_name=$(hostname 2>/dev/null || true)
        if [ -n "$runtime_name" ]; then
            printf '%s' "$runtime_name"
            return 0
        fi
    fi

    if [ -r /etc/hostname ]; then
        IFS= read -r file_name < /etc/hostname || true
        if [ -n "$file_name" ]; then
            printf '%s' "$file_name"
            return 0
        fi
    fi

    printf '%s' "ubuntu"
}

install_hostname_wrapper() {
    wrapper_dir=/tmp/mobileide-runtime/bin
    mkdir -p "$wrapper_dir"
    cat > "$wrapper_dir/hostname" <<'EOF'
#!/bin/sh
resolve_name() {
    if [ -n "$MOBILEIDE_GUEST_HOSTNAME" ]; then
        printf '%s\n' "$MOBILEIDE_GUEST_HOSTNAME"
        return 0
    fi

    if [ -r /etc/hostname ]; then
        IFS= read -r file_name < /etc/hostname || true
        if [ -n "$file_name" ]; then
            printf '%s\n' "$file_name"
            return 0
        fi
    fi

    printf '%s\n' "ubuntu"
}

case "$1" in
    ""|-s|--short|-f|--fqdn)
        resolve_name
        ;;
    *)
        printf '%s\n' "hostname is managed from /etc/hostname in this session" >&2
        exit 1
        ;;
esac
EOF
    chmod 755 "$wrapper_dir/hostname"
    PATH="$wrapper_dir:$PATH"
    export PATH
}

RUNTIME_HOSTNAME=$(resolve_runtime_hostname)
export MOBILEIDE_GUEST_HOSTNAME="$RUNTIME_HOSTNAME"
export HOST="$RUNTIME_HOSTNAME"
export HOSTNAME="$RUNTIME_HOSTNAME"
install_hostname_wrapper

mkdir -p /etc
if [ -L /etc/resolv.conf ]; then
    rm -f /etc/resolv.conf
fi

if [ ! -s /etc/resolv.conf ]; then
    rm -f /etc/resolv.conf
    printf 'nameserver 1.1.1.1\nnameserver 8.8.8.8\n' > /etc/resolv.conf
fi

if [ -f /etc/locale.gen ] && [ ! -f /etc/locale-gen.done ]; then
    echo "[MobileIDE] Generating locales..."
    sed -i -E "s/^#\s*(en_US.UTF-8\s+UTF-8)/\1/" /etc/locale.gen || true
    locale-gen || true
    touch /etc/locale-gen.done
fi



printf '\033]0;%s\007' "$RUNTIME_HOSTNAME"

ubuntu_bash_ps1="\[\e]0;\u@$RUNTIME_HOSTNAME:\w\a\]\[\e[38;5;81m\]\u\[\033[39m\]@$RUNTIME_HOSTNAME \[\033[39m\]\w \[\033[0m\]\\$ "

reattach_tty_streams() {
    tty >/dev/null 2>&1 && return 0

    if [ -e /dev/tty ] && exec 0<>/dev/tty 1>&0 2>&0 && tty >/dev/null 2>&1; then
        return 0
    fi

    fd0_resolved=$(readlink -f /proc/self/fd/0 2>/dev/null || true)
    case "$fd0_resolved" in
        /dev/pts/*|/dev/tty*)
            if [ -e "$fd0_resolved" ] && exec 0<>"$fd0_resolved" 1>&0 2>&0 && tty >/dev/null 2>&1; then
                return 0
            fi
            ;;
    esac

    if [ -n "$MOBILEIDE_HOST_TTY" ] && [ -e "$MOBILEIDE_HOST_TTY" ] && exec 0<>"$MOBILEIDE_HOST_TTY" 1>&0 2>&0 && tty >/dev/null 2>&1; then
        return 0
    fi

    return 1
}

if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

launch_interactive_shell() {
    set +e

    if [ -z "$MOBILEIDE_SHELL" ]; then
        MOBILEIDE_SHELL=/bin/sh
    fi

    shell_name=$(basename "$MOBILEIDE_SHELL")

    if [ "$shell_name" = "bash" ]; then
        if [ -z "$TERM" ] || [ "$TERM" = "dumb" ]; then
            export TERM=xterm-256color
        fi
        export HOST="$RUNTIME_HOSTNAME"
        export HOSTNAME="$RUNTIME_HOSTNAME"
        export PS1="$ubuntu_bash_ps1"
    fi

    if [ -n "$MOBILEIDE_SHELL" ] && [ -x "$MOBILEIDE_SHELL" ]; then
        reattach_tty_streams || true
        case "$shell_name" in
            bash)
                exec "$MOBILEIDE_SHELL" -i
                ;;
            zsh|ksh)
                exec "$MOBILEIDE_SHELL" -il
                ;;
            sh|ash|dash)
                exec "$MOBILEIDE_SHELL" -i
                ;;
            *)
                exec "$MOBILEIDE_SHELL"
                ;;
        esac
        echo "[MobileIDE] Failed to exec MOBILEIDE_SHELL=$MOBILEIDE_SHELL, falling back..."
    fi

    exec /bin/sh -i
}

if [ "$#" -eq 0 ]; then
    cd "$HOME" || cd /
    launch_interactive_shell
else
    exec "$@"
fi
