#!/bin/sh
# check_lsp_status.sh - Diagnostic tool for MobileIDE bundled LSPs & Plugin Store extensions

PACKAGE="com.scto.mobile.ide"
BASE_DIR="/data/data/$PACKAGE/local"
BIN_DIR="$BASE_DIR/bin"
LSP_DIR="$BIN_DIR/lsp"
EXT_DIR="$BASE_DIR/extensions"
SANDBOX_DIR="$BASE_DIR/sandbox"

printf "========================================================================================================\n"
printf "                       MobileIDE LSP & Plugin Store Status Diagnostics                                 \n"
printf "========================================================================================================\n"
printf "Base Directory: %s\n" "$BASE_DIR"
printf "Extensions Dir: %s\n" "$EXT_DIR"
printf "\n"

printf "%-32s | %-10s | %-8s | %-10s | %-12s | %-12s\n" "LSP / Plugin ID" "Typ" "Version" "Skript OK" "Runtime OK" "Status"
printf "%s\n" "--------------------------------------------------------------------------------------------------------"

# 1. Check Bundled LSPs
for script in "$LSP_DIR"/*.sh; do
    if [ -f "$script" ]; then
        name=$(basename "$script" .sh)
        script_status="Ja"
        if [ ! -x "$script" ]; then
            script_status="Kein +x"
        fi
        
        runtime_status="Ja"
        case "$name" in
            kotlin|java)
                if [ ! -d "$SANDBOX_DIR/usr/lib/jvm" ] && [ ! -x "$SANDBOX_DIR/usr/bin/java" ]; then
                    runtime_status="Nein (JDK)"
                fi
                ;;
            python|bash|css|html|json|typescript|eslint|emmet|markdown)
                if [ ! -x "$SANDBOX_DIR/usr/bin/node" ] && [ ! -x "$SANDBOX_DIR/usr/bin/python3" ]; then
                    runtime_status="Warnung"
                fi
                ;;
        esac

        printf "%-32s | %-10s | %-8s | %-10s | %-12s | %-12s\n" "$name (bundled)" "Bundled" "1.0.0" "$script_status" "$runtime_status" "Aktiv"
    fi
done

# 2. Check Plugin Store Installed Extensions
if [ -d "$EXT_DIR" ]; then
    for ext_plugin in "$EXT_DIR"/*; do
        if [ -d "$ext_plugin" ] && [ "$(basename "$ext_plugin")" != "installed.json" ]; then
            plugin_id=$(basename "$ext_plugin")
            manifest="$ext_plugin/manifest.json"
            
            ver="1.0.0"
            if [ -f "$manifest" ]; then
                ver=$(grep '"version"' "$manifest" 2>/dev/null | head -n 1 | cut -d '"' -f 4)
                [ -z "$ver" ] && ver="1.0.0"
            fi

            script_ok="Ja"
            [ ! -f "$manifest" ] && script_ok="Kein Manifest"

            printf "%-32s | %-10s | %-8s | %-10s | %-12s | %-12s\n" "$plugin_id" "Store" "$ver" "$script_ok" "Ja" "Installiert"
        fi
    done
fi

printf "%s\n" "--------------------------------------------------------------------------------------------------------"
printf "\n"
