set -e

source "$LOCAL/bin/utils"

SCRIPT_NAME=$(basename "$0" .sh)
LOG_DIR="${MOBILEIDE_WORKSPACE:-$HOME}/logs"
mkdir -p "$LOG_DIR"
exec > >(tee -i "$LOG_DIR/lsp_install_${SCRIPT_NAME}.log") 2>&1


info 'Preparing...'
apt update && apt upgrade -y

install() {
  if ! command_exists node || ! command_exists npm; then
    install_nodejs
  fi

  info 'Installing Emmet language server...'
  npm install -g --prefix /usr @olrtg/emmet-language-server

  info 'Emmet language server installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling Emmet language server...'
  npm uninstall -g --prefix /usr @olrtg/emmet-language-server
  info 'Emmet language server uninstalled successfully.'
  uninstall_nodejs
  exit 0
}

update() {
  info 'Updating Emmet language server...'
  npm update -g --prefix /usr @olrtg/emmet-language-server
  info 'Emmet language server updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
