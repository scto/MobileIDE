set -e

source "$LOCAL/bin/utils"

info 'Preparing...'
apt update && apt upgrade -y

install() {
  info 'Installing Java language server...'
  apt install -y jdtls
  info 'Java language server installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling Java language server...'
  apt remove -y jdtls
  apt autoremove -y
  info 'Java language server uninstalled successfully.'
  exit 0
}

update() {
  info 'Updating Java language server...'
  apt install --only-upgrade -y jdtls
  info 'Java language server updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
