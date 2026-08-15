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
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
  echo ""
}

uninstall() {
  info 'Uninstalling Java language server...'
  rm -f /usr/local/bin/jdtls
  rm -rf /opt/jdtls
  info 'Java language server uninstalled successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
  echo ""
}

update() {
  info 'Updating Java language server...'
  install
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
