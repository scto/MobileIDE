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
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
  echo ""
}

uninstall() {
  info 'Uninstalling Kotlin language server...'
  rm -f /usr/local/bin/kotlin-language-server
  rm -rf /opt/kotlin-language-server
  info 'Kotlin language server uninstalled successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
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
