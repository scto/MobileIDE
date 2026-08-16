#!/system/bin/sh
set -e

source "utils" 2>/dev/null

# CONFIGURATION
LSP_DIR_GO="$HOME/.lsp/go"
GOPLS_LSP_VERSION="$1"

# HELPERS
install() {
	info "Installing Go..."
	apt update
	apt install golang-go
	
    info "Installing Go Language Server $GOPLS_LSP_VERSION..."
	go install golang.org/x/tools/gopls@$GOPLS_LSP_VERSION
	mkdir $LSP_DIR_GO
	
	
	ln -sf "$HOME/go/bin/gopls" "$LSP_DIR_GO/gopls"
    chmod +x "$LSP_DIR_GO/gopls"
	echo "$GOPLS_LSP_VERSION" > "$LSP_DIR_GO/version.txt"
}

# MAIN
case "$1" in
    --uninstall)
        info "Uninstalling Go..."
        rm -rf "$LSP_DIR_GO"
        info "Uninstalled successfully."
        exit 0
        ;;
    --update)
        info "Updating Go LSP..."
        rm -rf "$LSP_DIR_GO"
        install
        exit 0
        ;;
    *)
        install
        if ! grep -q "export PATH=\$PATH:$LSP_DIR_GO/" ~/.bashrc; then
            echo "export PATH=\$PATH:$LSP_DIR_GO/" >> ~/.bashrc
        fi
		
        info "All done! Restart your terminal or run: source ~/.bashrc"
        exit 0
        ;;
esac