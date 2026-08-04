set -e

source "$LOCAL/bin/utils"

info 'Preparing...'
pkg_update

install() {
  if ! command_exists node || ! command_exists npm; then
    install_nodejs
  fi

  info 'Installing Markdown language server...'
  npm install -g --prefix /usr vscode-langservers-extracted
  info 'Markdown language server installed successfully.'
  read -n 1 -s -r -p "Press any key to close the terminal"; exit 0
}

uninstall() {
  info 'Uninstalling Markdown language server...'
  npm uninstall -g --prefix /usr vscode-langservers-extracted
  info 'Markdown language server uninstalled successfully.'
  uninstall_nodejs
  read -n 1 -s -r -p "Press any key to close the terminal"; exit 0
}

update() {
  info 'Updating Markdown language server...'
  npm update -g --prefix /usr vscode-langservers-extracted
  info 'Markdown language server updated successfully.'
  read -n 1 -s -r -p "Press any key to close the terminal"; exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
