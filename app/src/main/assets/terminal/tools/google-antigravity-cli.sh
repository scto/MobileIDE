set -e

source "$LOCAL/bin/utils"

info 'Preparing...'
$PKG_UPDATE && $PKG_UPGRADE

install() {
  info 'Installing dependencies for Google Antigravity CLI...'
  
  if [ "$OS_TYPE" = "alpine" ]; then
    # Alpine nutzt musl. Da das Google CLI-Binary für glibc kompiliert ist (Go),
    # benötigen wir zwingend die Kompatibilitätsschichten gcompat und libc6-compat.
    $PKG_INSTALL curl bash ca-certificates gcompat libc6-compat
  else
    $PKG_INSTALL curl bash ca-certificates
  fi

  info 'Installing Google Antigravity CLI...'
  
  # Das offizielle Installationsskript von Google ausführen
  curl -fsSL https://antigravity.google/cli/install.sh | bash

  # Stelle sicher, dass der Befehl global im Pfad liegt (falls das Google-Skript 
  # ihn nur nach ~/.local/bin legt)
  if [ -f "$HOME/.local/bin/agy" ]; then
    mkdir -p /usr/local/bin
    ln -sf "$HOME/.local/bin/agy" /usr/local/bin/agy
  fi

  info 'Google Antigravity CLI installed successfully.'
  info "You can now start the agent by typing 'agy'."
  exit 0
}

uninstall() {
  if ask "Are you sure you want to uninstall Google Antigravity CLI?"; then
    info 'Uninstalling Google Antigravity CLI...'
    
    # Binaries entfernen
    rm -f /usr/local/bin/agy
    rm -f "$HOME/.local/bin/agy"
    
    # Konfigurations- und Cache-Verzeichnisse entfernen
    rm -rf "$HOME/.config/antigravity"
    rm -rf "$HOME/.local/share/antigravity"
    rm -rf "$HOME/.gemini" # Fallback für alte Gemini CLI Caches
    
    info 'Google Antigravity CLI uninstalled successfully.'
  fi
  exit 0
}

update() {
  info 'Updating Google Antigravity CLI...'
  # Das Installationsskript von Google führt auch Updates durch
  curl -fsSL https://antigravity.google/cli/install.sh | bash
  info 'Google Antigravity CLI updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac