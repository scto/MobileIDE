set -e

source "$LOCAL/bin/utils"

info 'Preparing...'
apt update && apt upgrade -y

install() {
  info 'Installing Kotlin language server...'
  apt install -y kotlin-language-server
  info 'Kotlin language server installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling Kotlin language server...'
  apt remove -y kotlin-language-server
  apt autoremove -y
  info 'Kotlin language server uninstalled successfully.'
  exit 0
}

update() {
  info 'Updating Kotlin language server...'
  apt install --only-upgrade -y kotlin-language-server
  info 'Kotlin language server updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
