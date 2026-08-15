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

LLEMINX_VERSION="0.31.0"
INSTALL_DIR="$HOME/.lsp/lemminx"

install() {
  info 'Installing LemMinX language server...'

  mkdir -p "$INSTALL_DIR"
  cd "$INSTALL_DIR"
  pkg_install curl ca-certificates
  install_jdk

  curl -L -o "server.jar" "https://download.eclipse.org/staging/2025-09/plugins/org.eclipse.lemminx.uber-jar_${LLEMINX_VERSION}.jar"
  echo "$LLEMINX_VERSION" > version.txt
  info 'LemMinX language server installed successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

uninstall() {
  info 'Uninstalling LemMinX language server...'
  rm -rf "$INSTALL_DIR"
  info 'LemMinX language server uninstalled successfully.'

  if ask "Do you want to uninstall OpenJDK? It was installed as a dependency of this language server."; then
    info "Uninstalling OpenJDK..."
    pkg_remove default-jdk || pkg_remove openjdk17 || pkg_remove openjdk-17
    info "OpenJDK uninstalled successfully."
  fi
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

update() {
  info 'Updating LemMinX language server...'
  cd "$INSTALL_DIR"
  rm -f "server.jar"
  curl -L -o "server.jar" "https://download.eclipse.org/staging/2025-09/plugins/org.eclipse.lemminx.uber-jar_${LLEMINX_VERSION}.jar"
  echo "$LLEMINX_VERSION" > version.txt
  info 'LemMinX language server updated successfully.'
printf "\n%s\n" "Press Enter to continue..."; read -r _unused 2>/dev/null || true
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac
