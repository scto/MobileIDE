#!/bin/sh
# Diagnose-Skript für MobileIDE Assets & LSP Toolchain
PACKAGE="com.scto.mobile.ide"
BASE_DIR="/data/data/$PACKAGE/local"
BIN_DIR="$BASE_DIR/bin"
LSP_DIR="$BIN_DIR/lsp"

echo "=== MobileIDE Asset & LSP Health Check ==="
echo "Package Base: $BASE_DIR"
echo ""

check_file() {
    local file="$1"
    local desc="$2"
    if [ -f "$file" ]; then
        if [ -x "$file" ]; then
            echo " [OK] $desc: $file (Ausführbar)"
        else
            echo " [WARNHINWEIS] $desc: $file (Existiert, aber NICHT ausführbar!)"
        fi
    else
        echo " [FEHLT] $desc: $file"
    fi
}

check_dir() {
    local dir="$1"
    local desc="$2"
    if [ -d "$dir" ]; then
        echo " [OK] $desc: $dir"
    else
        echo " [FEHLT] $desc: $dir"
    fi
}

echo "1. Prüfe Kern-Skripte im Terminal Host:"
check_file "$BIN_DIR/init-host" "init-host Skript"
check_file "$BIN_DIR/init" "init Skript"
check_file "$BIN_DIR/sandbox" "sandbox Skript"
check_file "$BIN_DIR/setup" "setup Skript"
check_file "$BIN_DIR/utils" "utils Skript"

echo ""
echo "2. Prüfe LSP Skripte:"
check_file "$LSP_DIR/kotlin.sh" "Kotlin LSP Skript"
check_file "$LSP_DIR/java.sh" "Java LSP Skript"
check_file "$LSP_DIR/bash.sh" "Bash LSP Skript"
check_file "$LSP_DIR/xml.sh" "XML LSP Skript"

echo ""
echo "3. Prüfe Container Verzeichnisse (/home & /root):"
check_dir "$BASE_DIR/ubuntu/home" "Ubuntu /home Verzeichnis"
check_dir "$BASE_DIR/ubuntu/root" "Ubuntu /root Verzeichnis"
check_dir "$BASE_DIR/alpine/home" "Alpine /home Verzeichnis"

echo ""
echo "=== Check Abgeschlossen ==="
