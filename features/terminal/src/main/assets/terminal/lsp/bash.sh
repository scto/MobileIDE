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

  info 'Installing Bash language server...'
  npm install -g --prefix /usr bash-language-server

  info 'Installing ShellCheck...'
  pkg_install shellcheck || true

  info 'Bash language server installed successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

uninstall() {
  info 'Uninstalling Bash language server...'
  npm uninstall -g --prefix /usr bash-language-server
  info 'Bash language server uninstalled successfully.'
  uninstall_nodejs
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

update() {
  info 'Updating Bash language server...'
  npm update -g --prefix /usr bash-language-server
  info 'Bash language server updated successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
