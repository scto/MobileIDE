#!/system/bin/sh
set -e

source "$LOCAL/bin/utils" 2>/dev/null

# CONFIGURATION
LSP_DIR_KT="$HOME/.lsp/kmp-lsp"
KOTLIN_LSP_VERSION="$1"

# HELPERS
get_arch() {
    case "$(uname -m)" in
        x86_64)
            echo "x86_64"
            ;;
        aarch64|arm64)
            echo "aarch64"
            ;;
        *)
            error "Unsupported architecture: $(uname -m)"
            ;;
    esac
}

get_kotlin_lsp_url() {
    local arch="$1"
    echo "https://github.com/Hessesian/kmp-lsp/releases/download/$KOTLIN_LSP_VERSION/kmp-lsp-linux-$arch.tar.gz"
}

install() {
    info "Installing KMP-LSP ${KOTLIN_LSP_VERSION}..."

    local arch=$(get_arch)
    local url=$(get_kotlin_lsp_url "$arch")
    local tmp_dir=$(mktemp -d)

    info "Downloading from $url"
    curl -L "$url" -o "$tmp_dir/archive.tar.gz"
    
    info "Extracting..."
    tar -xzf "$tmp_dir/archive.tar.gz" -C "$tmp_dir"

    mkdir -p "$LSP_DIR_KT"

    mv $tmp_dir/kmp-lsp $LSP_DIR_KT
    mv $tmp_dir/kmp-jar-indexer $LSP_DIR_KT

    rm -rf "$tmp_dir"

    chmod +x "$LSP_DIR_KT/kmp-lsp"
    chmod +x "$LSP_DIR_KT/kmp-jar-indexer"

    echo "$KOTLIN_LSP_VERSION" > "$LSP_DIR_KT/version.txt"

    info "KMP-LSP installed to $LSP_DIR_KT"
}

uninstall() {
    info "Uninstalling KMP-LSP..."
    rm -rf "$LSP_DIR_KT"
    info "Uninstalled successfully."
}

# MAIN
case "$1" in
    --uninstall)
        uninstall
        exit 0
        ;;
    --update)
        info "Updating KMP-LSP..."
        rm -rf "$LSP_DIR_KT"
        install
        exit 0
        ;;
    *)
        install
        if ! grep -q "export PATH=\$PATH:$LSP_DIR_KT" ~/.bashrc 2>/dev/null; then
            echo "export PATH=\$PATH:$LSP_DIR_KT" >> ~/.bashrc
        fi

        info "All done! Restart your terminal or run: source ~/.bashrc"
        exit 0
        ;;
esac