set -e

source "$(dirname "$0")/../utils" || source "$LOCAL/bin/utils"

info 'Preparing...'
pkg_update
pkg_install curl unzip
install_jdk

install() {
  info 'Installing Kotlin language server...'
  mkdir -p /opt
  cd /opt
  curl -L -o "server.zip" "https://github.com/fwcd/kotlin-language-server/releases/latest/download/server.zip"
  unzip -o server.zip
  rm server.zip
  mv server kotlin-language-server || true
  chmod +x /opt/kotlin-language-server/bin/kotlin-language-server
  mkdir -p /usr/local/bin
  ln -sf /opt/kotlin-language-server/bin/kotlin-language-server /usr/local/bin/kotlin-language-server
  info 'Kotlin language server installed successfully.'
  read -n 1 -s -r -p "Press any key to continue..."
  echo ""
}

uninstall() {
  info 'Uninstalling Kotlin language server...'
  rm -f /usr/local/bin/kotlin-language-server
  rm -rf /opt/kotlin-language-server
  info 'Kotlin language server uninstalled successfully.'
  read -n 1 -s -r -p "Press any key to continue..."
  echo ""
}

update() {
  info 'Updating Kotlin language server...'
  install
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
