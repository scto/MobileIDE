set -e

source "$(dirname "$0")/../utils" || source "$LOCAL/bin/utils"

info 'Preparing...'
pkg_update
pkg_install curl tar
install_jdk

install() {
  info 'Installing Java language server (jdtls)...'
  mkdir -p /opt/jdtls
  curl -L -o "jdtls.tar.gz" "http://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz"
  tar -xzf jdtls.tar.gz -C /opt/jdtls
  rm jdtls.tar.gz
  chmod +x /opt/jdtls/bin/jdtls
  mkdir -p /usr/local/bin
  ln -sf /opt/jdtls/bin/jdtls /usr/local/bin/jdtls
  info 'Java language server installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling Java language server...'
  rm -f /usr/local/bin/jdtls
  rm -rf /opt/jdtls
  info 'Java language server uninstalled successfully.'
  exit 0
}

update() {
  info 'Updating Java language server...'
  install
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
