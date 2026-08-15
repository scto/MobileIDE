set -e

if [ -f "$(dirname "$0")/../utils" ]; then
  . "$(dirname "$0")/../utils"
elif [ -f "$LOCAL/bin/utils" ]; then
  . "$LOCAL/bin/utils"
elif [ -f "/data/data/com.scto.mobile.ide/local/bin/utils" ]; then
  . "/data/data/com.scto.mobile.ide/local/bin/utils"
elif [ -f "/data/user/0/com.scto.mobile.ide/local/bin/utils" ]; then
  . "/data/user/0/com.scto.mobile.ide/local/bin/utils"
fi

info 'Preparing...'
pkg_update

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
      pkg_remove pipx
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
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

uninstall() {
  info "Uninstalling Pyright language server..."
  npm uninstall -g --prefix /usr pyright
  info 'Pyright language server uninstalled successfully.'
  uninstall_nodejs
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

update() {
  info "Updating Pyright language server..."
  npm update -g --prefix /usr pyright
  info 'Pyright language server updated successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
