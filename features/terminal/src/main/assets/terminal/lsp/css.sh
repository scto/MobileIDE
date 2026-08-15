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

install() {
  if ! command_exists node || ! command_exists npm; then
    install_nodejs
  fi

  info 'Installing extracted VSCode language servers...'
  npm install -g --prefix /usr vscode-langservers-extracted
  info 'CSS language server installed successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

uninstall() {
  if ask "Are you sure you want to uninstall the extracted VSCode language servers? This will also remove the HTML, CSS and JSON language servers."; then
    info 'Uninstalling extracted VSCode language servers...'
    npm uninstall -g --prefix /usr vscode-langservers-extracted
    info 'Extracted VSCode language servers uninstalled successfully.'
    uninstall_nodejs
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
  fi
}

update() {
  info 'Updating extracted VSCode language servers...'
  npm update -g --prefix /usr vscode-langservers-extracted
  info 'Extracted VSCode language servers updated successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
