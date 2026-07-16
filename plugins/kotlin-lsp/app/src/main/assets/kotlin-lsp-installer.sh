#!/system/bin/sh
set -e

if [ -z "$LOCAL" ]; then
    echo "ERROR: LOCAL variable not set"
    exit 1
fi

source "$LOCAL/bin/utils" 2>/dev/null || {
    echo "ERROR: Cannot source utils from $LOCAL/bin/utils"
    exit 1
}

# CONFIGURATION
LSP_DIR_KT="$HOME/.lsp/kotlin"
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
            exit 1
            ;;
    esac
}

get_kotlin_lsp_url() {
    local arch="$1"
    echo "https://download-cdn.jetbrains.com/language-server/kotlin-server/$KOTLIN_LSP_VERSION/kotlin-server-$KOTLIN_LSP_VERSION-$arch.tar.gz"
}

install() {
    info "Installing Kotlin Language Server ${KOTLIN_LSP_VERSION}..."

    local arch=$(get_arch)
    local url=$(get_kotlin_lsp_url "$arch")
    local tmp_dir=$(mktemp -d)

    apt install -y curl tar gzip 2>/dev/null || pkg install curl tar gzip 2>/dev/null
	
    curl -L "$url" | tar -xz -C "$tmp_dir"

    mkdir -p "$LSP_DIR_KT"

    mv "$tmp_dir"/*/* "$LSP_DIR_KT/"

    rm -rf "$tmp_dir"

    chmod +x "$LSP_DIR_KT/bin/intellij-server"
	
	echo "$KOTLIN_LSP_VERSION" > $LSP_DIR_KT/version.txt
	
    info "Kotlin LSP installed to $LSP_DIR_KT"
}

# MAIN
case "$1" in
    --uninstall)
        info "Uninstalling Kotlin LSP..."
        rm -rf "$LSP_DIR_KT"
        info "Uninstalled successfully."
        exit 0
        ;;
    --update)
        info "Updating Kotlin LSP..."
        rm -rf "$LSP_DIR_KT"
        install
        exit 0
        ;;
    *)
        install
        if ! grep -q "export PATH=\$PATH:$LSP_DIR_KT/bin" ~/.bashrc; then
            echo "export PATH=\$PATH:$LSP_DIR_KT/bin" >> ~/.bashrc
        fi
		
        info "All done! Restart your terminal or run: source ~/.bashrc"
        exit 0
        ;;
esac