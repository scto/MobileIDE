#!/bin/sh
# utils.sh - Shared utility functions for MobileIDE terminal and LSP installers

RESET='\033[0m'
BOLD_BLUE='\033[1;34m'
BOLD_YELLOW='\033[1;33m'
BOLD_RED='\033[1;31m'

BLUE_BG='\033[1;44m'
YELLOW_BG='\033[1;43m'
RED_BG='\033[1;41m'

info() {
  printf "\n${BLUE_BG}  INFO  ${RESET} ${BOLD_BLUE}%s${RESET}\n" "$1"
}

warn() {
  printf "\n${YELLOW_BG}  WARN  ${RESET} ${BOLD_YELLOW}%s${RESET}\n" "$1"
}

error() {
  printf "\n${RED_BG} ERROR ${RESET} ${BOLD_RED}%s${RESET}\n" "$1"
}

ask() {
  local prompt="$1"
  local response

  while true; do
    printf "\n${BLUE_BG}  ?  ${RESET} ${BOLD_BLUE}%s${RESET}\n" "$prompt"
    printf "%s" "[y/N]: "
    read -r response
    case "$response" in
      [Yy]|[Yy][Ee][Ss])
        return 0
        ;;
      [Nn]|[Nn][Oo]|"")
        return 1
        ;;
      *)
        warn "Please answer yes or no."
        ;;
    esac
  done
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

pkg_update() {
  info 'Updating package lists...'
  if command_exists apk; then
    apk update && apk upgrade
  elif command_exists apt-get; then
    apt-get update && apt-get upgrade -y
  elif command_exists apt; then
    apt update && apt upgrade -y
  elif command_exists pkg; then
    pkg update && pkg upgrade -y
  fi
}

pkg_install() {
  if command_exists apk; then
    apk add --no-cache "$@"
  elif command_exists apt-get; then
    apt-get install -y "$@"
  elif command_exists apt; then
    apt install -y "$@"
  elif command_exists pkg; then
    pkg install -y "$@"
  fi
}

pkg_remove() {
  if command_exists apk; then
    apk del "$@"
  elif command_exists apt-get; then
    apt-get remove -y "$@" && apt-get autoremove -y
  elif command_exists apt; then
    apt remove -y "$@" && apt autoremove -y
  elif command_exists pkg; then
    pkg uninstall -y "$@"
  fi
}

install_nodejs() {
  if command_exists node && command_exists npm; then
    return 0
  fi
  info "Installing Node.js & npm..."
  if command_exists apk; then
    apk add --no-cache nodejs npm
  elif command_exists apt-get || command_exists apt; then
    pkg_install curl ca-certificates
    curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - || true
    pkg_install nodejs npm || pkg_install nodejs
  elif command_exists pkg; then
    pkg install -y nodejs
  fi
}

uninstall_nodejs() {
  if ask "Do you want to uninstall Node.js? It was installed as a dependency of this language server."; then
    info "Uninstalling Node.js..."
    pkg_remove nodejs npm || pkg_remove nodejs
    info "Node.js uninstalled successfully."
  fi
}

install_jdk() {
  if command_exists java; then
    return 0
  fi
  info "Installing OpenJDK..."
  if command_exists apk; then
    apk add --no-cache openjdk17 || apk add --no-cache openjdk11 || apk add --no-cache openjdk21
  elif command_exists apt-get || command_exists apt; then
    pkg_install default-jdk || pkg_install openjdk-17-jdk || pkg_install openjdk-21-jdk
  elif command_exists pkg; then
    pkg install -y openjdk-17 || pkg install -y openjdk-21
  fi
}