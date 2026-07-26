set -e

source "$LOCAL/bin/utils"

RUST_ANALYZER_VERSION="$2"
INSTALL_DIR="$HOME/.lsp/rust"

install_rustup() {
  info "Installing Rust toolchain..."

  curl --proto '=https' --tlsv1.3 -sSf https://sh.rustup.rs | sh -s -- -y

  export PATH="$HOME/.cargo/bin:$PATH"

  rustup component add rust-src

  info "Rust toolchain installed successfully."
}

check_rust_toolchain() {
  if ! command_exists cargo || ! command_exists rustc; then
    if ask "Rust toolchain is missing. Do you want to install Rust using rustup?"; then
      install_rustup
    else
      error "Rust toolchain is required for rust-analyzer."
      exit 1
    fi
  fi

  if command_exists rustup; then
    if ! rustup component list --installed | grep -q "^rust-src"; then
      if ask "Rust source component is missing. Do you want to install it?"; then
        info "Installing Rust source component..."
        rustup component add rust-src
      else
        warn "rust-src is missing. Some rust-analyzer features may not work."
      fi
    fi
  else
    warn "rustup not found. Cannot automatically install rust-src."
  fi
}

get_arch() {
  case "$(uname -m)" in
    x86_64)
      echo "x86_64-unknown-linux-gnu"
      ;;
    aarch64 | arm64)
      echo "aarch64-unknown-linux-gnu"
      ;;
    *)
      error "Unsupported architecture: $(uname -m)"
      exit 1
      ;;
  esac
}

install() {
  info 'Installing rust-analyzer language server...'

  apt install -y curl ca-certificates gzip
  check_rust_toolchain

  ARCH=$(get_arch)
  URL="https://github.com/rust-lang/rust-analyzer/releases/download/${RUST_ANALYZER_VERSION}/rust-analyzer-${ARCH}.gz"

  mkdir -p "$INSTALL_DIR"
  cd "$INSTALL_DIR"

  curl -L -o rust-analyzer.gz "$URL"

  info "Extracting..."
  gunzip -f rust-analyzer.gz

  chmod +x rust-analyzer

  echo "$RUST_ANALYZER_VERSION" > version.txt

  info 'rust-analyzer installed successfully.'
  exit 0
}

uninstall() {
  info 'Uninstalling rust-analyzer language server...'

  rm -rf "$INSTALL_DIR"

  info 'rust-analyzer uninstalled successfully.'
  exit 0
}

update() {
  info 'Updating rust-analyzer language server...'

  ARCH=$(get_arch)
  URL="https://github.com/rust-lang/rust-analyzer/releases/download/${RUST_ANALYZER_VERSION}/rust-analyzer-${ARCH}.gz"

  cd "$INSTALL_DIR"

  rm -f rust-analyzer rust-analyzer.gz

  curl -L -o rust-analyzer.gz "$URL"

  info "Extracting..."
  gunzip -f rust-analyzer.gz

  chmod +x rust-analyzer

  echo "$RUST_ANALYZER_VERSION" > version.txt

  info 'rust-analyzer updated successfully.'
  exit 0
}

case "$1" in
  --uninstall) uninstall;;
  --update) update;;
  *) install;;
esac