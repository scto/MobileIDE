#!/system/bin/sh
set -e

source "utils" 2>/dev/null

# CONFIGURATION
INSTALL_DIR_ZIG="$HOME/.local/zig"
LSP_DIR_ZLS="$HOME/.local/zig/zls"

ZIG_VERSION="0.13.0"
ZLS_VERSION="$1"

if [ -z "$ZLS_VERSION" ]; then
    error "No ZLS version provided"
    exit 1
fi

# HELPERS
get_arch() {
    case "$(uname -m)" in
        x86_64)
            echo "x86_64"
            ;;
        aarch64|arm64)
            echo "aarch64"
            ;;
        armv7l|arm)
            echo "armv7l"
            ;;
        *)
            error "Unsupported architecture: $(uname -m)"
            exit 1
            ;;
    esac
}

get_zig_url() {
    local arch="$1"
    echo "https://ziglang.org/download/${ZIG_VERSION}/zig-linux-${arch}-${ZIG_VERSION}.tar.xz"
}

get_zls_url() {
    local arch="$1"
    echo "https://github.com/zigtools/zls/releases/download/${ZLS_VERSION}/zls-${arch}-linux.tar.xz"
}

# ZIG INSTALL
install_zig() {
    info "Installing Zig ${ZIG_VERSION}..."

    local arch=$(get_arch)
    local url=$(get_zig_url "$arch")
    local tmp_dir=$(mktemp -d)

    apt install -y curl tar xz-utils 2>/dev/null || pkg install curl tar xz-utils 2>/dev/null

    mkdir -p "$HOME/.local/bin"
    curl -L "$url" | tar -xJ -C "$tmp_dir"

    mkdir -p "$INSTALL_DIR_ZIG"
    rm -rf "$INSTALL_DIR_ZIG" 2>/dev/null || true
    mv "$tmp_dir"/* "$INSTALL_DIR_ZIG"

    rm -rf "$tmp_dir"
    echo "$ZIG_VERSION" > "$INSTALL_DIR_ZIG/version.txt"

    chmod +x "$INSTALL_DIR_ZIG/zig"

    ln -sf "$INSTALL_DIR_ZIG/zig" "$HOME/.local/bin/zig"

    info "Zig installed to $INSTALL_DIR_ZIG"
}

# ZLS INSTALL
install_zls() {
    info "Installing ZLS ${ZLS_VERSION}..."

    local arch=$(get_arch)
    local url=$(get_zls_url "$arch")
    local tmp_dir=$(mktemp -d)

    curl -L "$url" | tar -xJ -C "$tmp_dir"

    mkdir -p "$LSP_DIR_ZLS"
    mv "$tmp_dir/zls" "$LSP_DIR_ZLS/zls"
    chmod +x "$LSP_DIR_ZLS/zls"

    ln -sf "$LSP_DIR_ZLS/zls" "$HOME/.local/bin/zls"

    rm -rf "$tmp_dir"
    echo "$ZLS_VERSION" > "$LSP_DIR_ZLS/zls_version.txt"

    info "ZLS installed to $LSP_DIR_ZLS/zls"
}

# MAIN
case "$1" in
    --uninstall)
        info "Uninstalling Zig and ZLS..."
        rm -rf "$INSTALL_DIR_ZIG"
        rm -rf "$LSP_DIR_ZLS"
        rm -f "$HOME/.local/bin/zig"
        rm -f "$HOME/.local/bin/zls"
        info "Uninstalled successfully."
        exit 0
        ;;
    --update)
        info "Updating..."
        rm -rf "$INSTALL_DIR_ZIG"
        rm -rf "$LSP_DIR_ZLS"
        rm -f "$HOME/.local/bin/zig"
        rm -f "$HOME/.local/bin/zls"
        install_zig
        install_zls
        exit 0
        ;;
    *)
        install_zig
        install_zls

        if ! grep -q "export PATH=\$PATH:\$HOME/.local/bin" ~/.bashrc; then
            echo "export PATH=\$PATH:\$HOME/.local/bin" >> ~/.bashrc
        fi

        info "All done! Restart your terminal or run: source ~/.bashrc"
        info "Check: zig version && zls --version"
        exit 0
        ;;
esac