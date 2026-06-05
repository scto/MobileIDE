set -e

source "$LOCAL/bin/utils"

info 'Preparing...'
$PKG_UPDATE && $PKG_UPGRADE

# Legacy migration cleanup (python-lsp-server + pipx)
legacy_cleanup() {
  if command_exists pipx && pipx list 2>/dev/null | grep -q "python-lsp-server"; then
    if ask "Legacy Python LSP (python-lsp-server via pipx) detected. Do you want to uninstall it before installing Pyright?"; then
      info "Uninstalling legacy Python language server..."
      pipx uninstall python-lsp-server || true
      info "Legacy Python language server removed."
    fi
  fi

  if command_exists pipx; then
    if ask "pipx is installed. It was previously used for Python LSP. Do you want to remove pipx as well?"; then
      info "Uninstalling pipx..."
      $PKG_REMOVE pipx
      [ "$OS_TYPE" = "ubuntu" ] && apt autoremove -y || true
      info "pipx uninstalled successfully."
    fi
  fi
}

install() {
  legacy_cleanup

  if ! command_exists node || ! command_exists npm; then
    install_nodejs
  fi

  info "Installing Pyright language server..."
  npm install -g --prefix /usr pyright
  info 'Pyright language server installed successfully.'
  exit 0
}

uninstall() {
  info "Uninstalling Pyright language server..."
  npm uninstall -g --prefix /usr pyright
  info 'Pyright language server uninstalled successfully.'
  uninstall_nodejs
  exit 0
}

update() {
  info "Updating Pyright language server..."
  npm update -g --prefix /usr pyright
  info 'Pyright language server updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac