set -e

source "$LOCAL/bin/utils"

info 'Preparing...'
$PKG_UPDATE && $PKG_UPGRADE

INSTALL_DIR="/usr/local/aider-venv"
BIN_DIR="/usr/local/bin"

install() {
  info 'Installing required dependencies for Aider...'
  
  # Installiere Python, Git und grundlegende Build-Tools (für Tokenizer & Tree-sitter)
  if [ "$OS_TYPE" = "alpine" ]; then
    $PKG_INSTALL python3 py3-pip git rust cargo binutils clang make libffi-dev
  else
    $PKG_INSTALL python3 python3-venv python3-pip git rustc cargo binutils clang make libffi-dev
  fi

  info 'Installing Aider...'
  if [ -d "$INSTALL_DIR" ]; then
    warn "Aider environment already exists in $INSTALL_DIR. Updating instead..."
    update
    exit 0
  fi

  # Isoliertes Virtual Environment erstellen (Best Practice für Python CLI Tools)
  python3 -m venv "$INSTALL_DIR"
  
  # Pip aktualisieren und Aider installieren
  "$INSTALL_DIR/bin/pip" install --upgrade pip
  "$INSTALL_DIR/bin/pip" install aider-chat

  # Symlink erstellen, damit 'aider' global aufrufbar ist
  mkdir -p "$BIN_DIR"
  ln -sf "$INSTALL_DIR/bin/aider" "$BIN_DIR/aider"

  info 'Aider installed successfully.'
  info "You can now start it globally by typing 'aider'."
  exit 0
}

uninstall() {
  if ask "Are you sure you want to uninstall Aider?"; then
    info 'Uninstalling Aider...'
    rm -rf "$INSTALL_DIR"
    rm -f "$BIN_DIR/aider"
    info 'Aider uninstalled successfully.'
  fi
  exit 0
}

update() {
  if [ ! -d "$INSTALL_DIR" ]; then
    error "Aider is not installed yet. Please install it first."
    exit 1
  fi
  
  info 'Updating Aider...'
  "$INSTALL_DIR/bin/pip" install --upgrade aider-chat
  info 'Aider updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac