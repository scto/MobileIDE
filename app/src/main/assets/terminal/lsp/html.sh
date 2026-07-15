#!/bin/bash
set -e
source "$(dirname "$0")/../utils"

install() {
  if ! command_exists node || ! command_exists npm; then
    install_nodejs
  fi
  info 'Installing HTML/CSS/JSON language servers...'
  npm install -g --prefix /usr vscode-langservers-extracted
  info 'HTML language server installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling HTML/CSS/JSON language servers...'
  npm uninstall -g --prefix /usr vscode-langservers-extracted
  info 'HTML language server uninstalled successfully.'
  uninstall_nodejs
  exit 0
}

update() {
  info 'Updating HTML/CSS/JSON language servers...'
  npm update -g --prefix /usr vscode-langservers-extracted
  info 'HTML language server updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
