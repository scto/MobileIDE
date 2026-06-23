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

  info 'Installing Bash language server...'
  npm install -g --prefix /usr bash-language-server

  info 'Installing ShellCheck...'
  apt install -y shellcheck

  info 'Bash language server installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling Bash language server...'
  npm uninstall -g --prefix /usr bash-language-server
  info 'Bash language server uninstalled successfully.'
  uninstall_nodejs
  exit 0
}

update() {
  info 'Updating Bash language server...'
  npm update -g --prefix /usr bash-language-server
  info 'Bash language server updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac

