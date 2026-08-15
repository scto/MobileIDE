#!/bin/sh
# Diagnose-Skript für MobileIDE Distro Rootfs Integrität
PACKAGE="com.scto.mobile.ide"
BASE_DIR="/data/data/$PACKAGE/local"

echo "=== MobileIDE Distro Rootfs Health Check ==="
echo "Base Directory: $BASE_DIR"
echo ""

printf "%-12s | %-12s | %-10s | %-30s\n" "Distro" "Vollständig" "Dateien" "Fehlende Verzeichnisse"
printf "%-12s-+-%-12s-+-%-10s-+-%-30s\n" "------------" "------------" "----------" "------------------------------"

for distro_dir in "$BASE_DIR"/*/; do
    [ -d "$distro_dir" ] || continue
    distro=$(basename "$distro_dir")
    
    # Skip non-distro system directories
    case "$distro" in
        bin|lib|extensions|proot_tmp|cache|tmp) continue ;;
    esac

    missing=""
    [ ! -d "$distro_dir/home" ] && missing="$missing /home"
    [ ! -d "$distro_dir/etc" ] && [ ! -d "$distro_dir/root/etc" ] && missing="$missing /etc"
    [ ! -d "$distro_dir/usr" ] && [ ! -d "$distro_dir/bin" ] && missing="$missing /usr,/bin"
    [ ! -d "$distro_dir/root" ] && missing="$missing /root"

    file_count=$(find "$distro_dir" -type f 2>/dev/null | wc -l)

    if [ -z "$missing" ]; then
        is_complete="Ja (OK)"
        missing_str="Keine"
    else
        is_complete="NEIN"
        missing_str=$(echo "$missing" | xargs)
    fi

    printf "%-12s | %-12s | %-10s | %-30s\n" "$distro" "$is_complete" "$file_count" "$missing_str"
done

echo ""
echo "=== Check Abgeschlossen ==="
