set -e

source "$LOCAL/bin/utils"

info 'Preparing...'
$PKG_UPDATE && $PKG_UPGRADE

LLEMINX_VERSION="0.31.0"
INSTALL_DIR="$HOME/.lsp/lemminx"

install() {
  info 'Installing LemMinX language server...'

  mkdir -p "$INSTALL_DIR"
  cd "$INSTALL_DIR"
  
  if [ "$OS_TYPE" = "alpine" ]; then
    $PKG_INSTALL curl ca-certificates openjdk17
  else
    $PKG_INSTALL curl ca-certificates default-jdk
  fi
  
  curl -L -o "server.jar" "https://download.eclipse.org/staging/2025-09/plugins/org.eclipse.lemminx.uber-jar_${LLEMINX_VERSION}.jar"
  echo "$LLEMINX_VERSION" > version.txt
  info 'LemMinX language server installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling LemMinX language server...'
  rm -rf "$INSTALL_DIR"
  info 'LemMinX language server uninstalled successfully.'

  if ask "Do you want to uninstall OpenJDK? It was installed as a dependency of this language server."; then
    info "Uninstalling OpenJDK..."
    if [ "$OS_TYPE" = "alpine" ]; then
      $PKG_REMOVE openjdk17
    else
      $PKG_REMOVE default-jdk
      apt autoremove -y
    fi
    info "OpenJDK uninstalled successfully."
  fi
  exit 0
}

update() {
  info 'Updating LemMinX language server...'
  cd "$INSTALL_DIR"
  rm "server.jar"
  curl -L -o "server.jar" "https://download.eclipse.org/staging/2025-09/plugins/org.eclipse.lemminx.uber-jar_${LLEMINX_VERSION}.jar"
  echo "$LLEMINX_VERSION" > version.txt
  info 'LemMinX language server updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac