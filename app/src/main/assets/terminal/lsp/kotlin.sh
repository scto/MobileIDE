set -e

source "$LOCAL/bin/utils"

info 'Preparing...'
apt update && apt upgrade -y
apt install -y curl unzip default-jdk

install() {
  info 'Installing Kotlin language server...'
  mkdir -p /opt
  cd /opt
  curl -L -o "server.zip" "https://github.com/fwcd/kotlin-language-server/releases/latest/download/server.zip"
  unzip -o server.zip
  rm server.zip
  mv server kotlin-language-server || true
  chmod +x /opt/kotlin-language-server/bin/kotlin-language-server
  ln -sf /opt/kotlin-language-server/bin/kotlin-language-server /usr/local/bin/kotlin-language-server
  info 'Kotlin language server installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling Kotlin language server...'
  rm -f /usr/local/bin/kotlin-language-server
  rm -rf /opt/kotlin-language-server
  info 'Kotlin language server uninstalled successfully.'
  exit 0
}

update() {
  info 'Updating Kotlin language server...'
  install
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
