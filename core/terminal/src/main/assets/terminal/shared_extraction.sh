#!/bin/sh
# shared_extraction.sh - Gemeinsame Extraktions- & Diagnoseschnittstelle für MobileIDE
# Wird von setup.sh und idesetup eingebunden (source shared_extraction.sh)

# Ermittelt dynamisch das App-eigene Datenverzeichnis (kein Termux-Hardcoding)
resolve_app_data_dir() {
    local pkg_name="${APP_PACKAGE_NAME:-${MOBILEIDE_PACKAGE_NAME:-com.scto.mobile.ide}}"
    if [ -d "/data/user/0/${pkg_name}" ]; then
        echo "/data/user/0/${pkg_name}"
    else
        echo "${PRIVATE_DIR:-/data/data/${pkg_name}}"
    fi
}

# Validiert die PRoot-Binary funktional
validate_proot_binary() {
    local proot_bin="$1"
    local log_file="$2"

    if [ -z "$proot_bin" ] || [ ! -x "$proot_bin" ]; then
        echo "WARN   PRoot-Binary fehlt oder ist nicht ausführbar: $proot_bin" >&2
        return 1
    fi

    if ! "$proot_bin" --version > "$log_file" 2>&1; then
        echo "ERROR  PRoot-Binary ist nicht funktionsfähig ($proot_bin). Details:" >&2
        tail -n 20 "$log_file" >&2
        return 1
    fi
    return 0
}

# Führt Vorab-Checks aus: Speicherplatz, Schreibrechte, Archiv-Integrität
preflight_extraction_checks() {
    local target_dir="$1"
    local archive="$2"
    local min_free_mb="${3:-500}"

    mkdir -p "$target_dir"
    touch "$target_dir/.write_test" 2>/dev/null || {
        echo "ERROR  Zielverzeichnis $target_dir ist nicht beschreibbar!" >&2
        return 1
    }
    rm -f "$target_dir/.write_test"

    if [ ! -f "$archive" ] || [ ! -s "$archive" ]; then
        echo "ERROR  RootFS-Archiv $archive existiert nicht oder ist leer!" >&2
        return 1
    fi

    local archive_size
    archive_size=$(wc -c < "$archive" 2>/dev/null || echo 0)
    if [ "$archive_size" -lt 1048576 ]; then
        echo "ERROR  RootFS-Archiv $archive ist zu klein ($archive_size Bytes) / unvollständig!" >&2
        return 1
    fi

    return 0
}

# Ermittelt dynamisch die vom installierten tar unterstützten Optionen
get_supported_tar_opts() {
    local opts=""
    local help_out
    help_out=$(tar --help 2>&1 || true)

    if echo "$help_out" | grep -q "hard-dereference"; then
        opts="$opts --hard-dereference"
    fi
    if echo "$help_out" | grep -q "no-same-owner"; then
        opts="$opts --no-same-owner"
    fi
    if echo "$help_out" | grep -q "no-same-permissions"; then
        opts="$opts --no-same-permissions"
    fi
    echo "$opts"
}

# Führt die PRoot-basierte Extraktion aus
extract_via_proot() {
    local proot_bin="$1"
    local proot_args="$2"
    local archive="$3"
    local target_dir="$4"
    local log_file="$5"

    local app_data_dir
    app_data_dir="$(resolve_app_data_dir)"

    export PROOT_TMP_DIR="${PROOT_TMP_DIR:-${app_data_dir}/files/usr/tmp}"
    mkdir -p "$PROOT_TMP_DIR"
    chmod 700 "$PROOT_TMP_DIR" 2>/dev/null || chmod 755 "$PROOT_TMP_DIR" 2>/dev/null || true

    local tar_excludes="--exclude=etc/alternatives/* --exclude=var/run --exclude=var/lock --exclude=etc/rmt --exclude=etc/systemd/system/*.wants/* --exclude=usr/bin/awk --exclude=usr/bin/nawk --exclude=usr/bin/pager --exclude=usr/bin/which --exclude=usr/sbin/rmt"
    local tar_opts
    tar_opts="$(get_supported_tar_opts)"

    local cmd="(cd \"$target_dir\" && (tar -xzf \"$archive\" $tar_excludes $tar_opts || (gzip -dc \"$archive\" | tar -xf - $tar_excludes $tar_opts) || tar -xzf \"$archive\" $tar_excludes || (gzip -dc \"$archive\" | tar -xf - $tar_excludes) || tar -xzf \"$archive\" || tar -xf \"$archive\"))"

    set +e
    $proot_bin $proot_args /system/bin/sh -c "$cmd" > "$log_file" 2>&1
    local ret=$?
    set -e
    return $ret
}

# Führt die direkte tar-Fallback-Extraktion mit ALLEN notwendigen Flags aus
extract_via_tar_fallback() {
    local archive="$1"
    local target_dir="$2"
    local log_file="$3"

    local tar_excludes="--exclude=etc/alternatives/* --exclude=var/run --exclude=var/lock --exclude=etc/rmt --exclude=etc/systemd/system/*.wants/* --exclude=usr/bin/awk --exclude=usr/bin/nawk --exclude=usr/bin/pager --exclude=usr/bin/which --exclude=usr/sbin/rmt"
    local tar_opts
    tar_opts="$(get_supported_tar_opts)"

    local cmd="(cd \"$target_dir\" && (tar -xzf \"$archive\" $tar_excludes $tar_opts || (gzip -dc \"$archive\" | tar -xf - $tar_excludes $tar_opts) || tar -xzf \"$archive\" $tar_excludes || (gzip -dc \"$archive\" | tar -xf - $tar_excludes) || tar -xzf \"$archive\" || tar -xf \"$archive\"))"

    set +e
    sh -c "$cmd" > "$log_file" 2>&1
    local ret=$?
    set -e
    return $ret
}

# Post-Processing: erstellt notwendige Alternativen-Symlinks INNERHALB des Container-Rootfs neu
fix_alternatives_symlinks_inside_rootfs() {
    local target_dir="$1"

    mkdir -p "$target_dir/var" "$target_dir/etc/alternatives" "$target_dir/usr/bin"

    if [ -f "$target_dir/usr/bin/mawk" ]; then
        ln -snf /usr/bin/mawk "$target_dir/etc/alternatives/awk" 2>/dev/null || true
        ln -snf /etc/alternatives/awk "$target_dir/usr/bin/awk" 2>/dev/null || true
    elif [ -f "$target_dir/usr/bin/gawk" ]; then
        ln -snf /usr/bin/gawk "$target_dir/etc/alternatives/awk" 2>/dev/null || true
        ln -snf /etc/alternatives/awk "$target_dir/usr/bin/awk" 2>/dev/null || true
    fi

    ln -snf ../run "$target_dir/var/run" 2>/dev/null || true
    ln -snf ../lock "$target_dir/var/lock" 2>/dev/null || true
}

# Loggt die letzten N Zeilen einer Logdatei bei Fehlern
log_tail_on_failure() {
    local log_file="$1"
    local lines="${2:-50}"

    if [ -f "$log_file" ]; then
        echo "ERROR  Details zum Fehlschlag (letzte ${lines} Zeilen aus $log_file):" >&2
        tail -n "$lines" "$log_file" >&2
    fi
}
