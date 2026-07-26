set -e

. "$LOCAL/bin/utils"

# --- 1. Umgebungs- & Verzeichnis-Vorbereitung (Problem 1 & 2) ---
PROOT_TMP_DIR="${PROOT_TMP_DIR:-${TMP_DIR:-$PRIVATE_DIR}/usr/tmp}"
export PROOT_TMP_DIR
export TMP_DIR="${TMP_DIR:-$PROOT_TMP_DIR}"
mkdir -p "$PROOT_TMP_DIR" "$TMP_DIR"
chmod 700 "$PROOT_TMP_DIR" 2>/dev/null || chmod 755 "$PROOT_TMP_DIR" 2>/dev/null || true

LOGDIR="$LOCAL/logs"
mkdir -p "$LOGDIR"

# Schreibrechte-Test für PROOT_TMP_DIR
touch "$PROOT_TMP_DIR/.write_test" 2>/dev/null || {
    error "FEHLER: PROOT_TMP_DIR ($PROOT_TMP_DIR) ist nicht beschreibbar!"
    error "Lösung: Prüfe App-Speicherrechte und stellen Sie sicher, dass ein app-eigenes Verzeichnis genutzt wird."
    exit 1
}
rm -f "$PROOT_TMP_DIR/.write_test"

# Prüfe/erzeuge mobileide-environment.properties (Problem 2)
ENV_PROPS="$LOCAL/mobileide-environment.properties"
if [ ! -f "$ENV_PROPS" ] && [ -f "$PRIVATE_DIR/mobileide-environment.properties" ]; then
    cp "$PRIVATE_DIR/mobileide-environment.properties" "$ENV_PROPS" 2>/dev/null || true
fi

if [ ! -f "$ENV_PROPS" ]; then
    info "Erzeuge Standard-Umgebungskonfiguration $ENV_PROPS..."
    cat << EOF > "$ENV_PROPS"
ANDROID_HOME=/root/android-sdk
ANDROID_SDK_ROOT=/root/android-sdk
ANDROID_NDK_HOME=/root/android-sdk/ndk-bundle
NDK_HOME=/root/android-sdk/ndk-bundle
CMAKE_HOME=/usr
PATH=/root/android-sdk/cmdline-tools/latest/bin:/root/android-sdk/platform-tools:/root/android-sdk/build-tools/35.0.1:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
PROOT_TMP_DIR=$PROOT_TMP_DIR
EOF
fi

info "Starte Ubuntu-Container-Setup..."

# --- 2. Vorab-Prüfungen vor Extraktion (Problem 4) ---
# a) RootFS-Archiv ermitteln und prüfen
TAR_PATH=""
for candidate in "$TMP_DIR/sandbox.tar.gz" "$PROOT_TMP_DIR/sandbox.tar.gz" "$PRIVATE_DIR/ubuntu.tar.gz" "$PRIVATE_DIR/alpine.tar.gz" "$PRIVATE_DIR/${MOBILEIDE_DISTRO:-ubuntu}.tar.gz"; do
    if [ -f "$candidate" ] && [ -s "$candidate" ]; then
        TAR_PATH="$candidate"
        break
    fi
done

if [ -z "$TAR_PATH" ]; then
    error "FEHLER: Kein gültiges RootFS-Archiv gefunden!"
    error "Geprüfte Pfade: $TMP_DIR/sandbox.tar.gz, $PRIVATE_DIR/ubuntu.tar.gz"
    error "Lösung: Bitte überprüfe deine Internetverbindung und starte den Download erneut."
    exit 1
fi

TAR_SIZE=$(wc -c < "$TAR_PATH" 2>/dev/null || echo 0)
if [ "$TAR_SIZE" -lt 1048576 ]; then
    error "FEHLER: Das RootFS-Archiv unter $TAR_PATH ist unvollständig oder beschädigt (Größe: ${TAR_SIZE} Bytes)."
    error "Lösung: Lösche die Datei und lade den Container neu herunter."
    exit 1
fi
info "RootFS-Archiv validiert: $TAR_PATH (${TAR_SIZE} Bytes)"

# b) Zielverzeichnis vorbereiten und Schreibrechte testen
SANDBOX_DIR="$LOCAL/sandbox"
mkdir -p "$SANDBOX_DIR"
touch "$SANDBOX_DIR/.write_test" 2>/dev/null || {
    error "FEHLER: Zielverzeichnis $SANDBOX_DIR ist nicht beschreibbar!"
    exit 1
}
rm -f "$SANDBOX_DIR/.write_test"

# --- 3. PRoot-Argumente & Befehlsaufbau ---
ARGS="--kill-on-exit"
ARGS="$ARGS -w /"

for system_mnt in /apex /odm /product /system /system_ext /vendor \
 /linkerconfig/ld.config.txt \
 /linkerconfig/com.android.art/ld.config.txt \
 /plat_property_contexts /property_contexts; do

 if [ -e "$system_mnt" ]; then
  system_mnt=$(realpath "$system_mnt")
  ARGS="$ARGS -b ${system_mnt}"
 fi
done
unset system_mnt

ARGS="$ARGS -b /sdcard"
ARGS="$ARGS -b /storage"
ARGS="$ARGS -b /dev"
ARGS="$ARGS -b /data"
ARGS="$ARGS -b /dev/urandom:/dev/random"
ARGS="$ARGS -b /proc"
ARGS="$ARGS -b $PRIVATE_DIR"

if [ -d "/proc/self/fd" ]; then
  ARGS="$ARGS -b /proc/self/fd:/dev/fd"
fi

if [ -c "/proc/self/fd/0" ] || [ -f "/proc/self/fd/0" ]; then
  ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin"
fi

if [ -c "/proc/self/fd/1" ] || [ -f "/proc/self/fd/1" ]; then
  ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout"
fi

if [ -c "/proc/self/fd/2" ] || [ -f "/proc/self/fd/2" ]; then
  ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr"
fi

ARGS="$ARGS -b $PRIVATE_DIR"
ARGS="$ARGS -b /sys"

ARGS="$ARGS -r /"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

TAR_EXCLUDES="--exclude=etc/alternatives/* --exclude=var/run --exclude=var/lock --exclude=etc/rmt --exclude=etc/systemd/system/*.wants/* --exclude=usr/bin/awk --exclude=usr/bin/nawk --exclude=usr/bin/pager --exclude=usr/bin/which --exclude=usr/sbin/rmt"
TAR_OPTS="--hard-dereference --no-same-owner --no-same-permissions"

COMMAND="(cd $LOCAL/sandbox && (tar -xzf $TAR_PATH $TAR_EXCLUDES $TAR_OPTS || (gzip -dc $TAR_PATH | tar -xf - $TAR_EXCLUDES $TAR_OPTS) || tar -xf $TAR_PATH $TAR_EXCLUDES $TAR_OPTS))"

# --- 4. PRoot-Funktionstest (Problem 3) ---
info "Teste PRoot-Ausführbarkeit..."
set +e
$PROOT $ARGS /system/bin/sh -c "true" > "$LOGDIR/proot_test.log" 2>&1
proot_test_ret=$?
set -e

skip_proot=0
if [ "$proot_test_ret" -ne 0 ]; then
    warn "PRoot-Funktionstest ist mit Exit-Code $proot_test_ret fehlgeschlagen. Details:"
    tail -n 20 "$LOGDIR/proot_test.log" | while read -r l; do warn "  [proot-test] $l"; done
    warn "Überspringe PRoot-Extraktion und wechsle direkt zum Fallback."
    skip_proot=1
else
    info "PRoot-Funktionstest erfolgreich."
fi

# --- 5. Container-Extraktion mit Logging (Problem 1 & 4) ---
extracted_ok=0

if [ "$skip_proot" -eq 0 ]; then
    info "Entpacke Container via PRoot..."
    set +e
    $PROOT $ARGS /system/bin/sh -c "$COMMAND" > "$LOGDIR/proot_extract.log" 2>&1
    ret=$?
    set -e

    if [ "$ret" -eq 0 ]; then
        extracted_ok=1
        info "PRoot-Extraktion erfolgreich abgeschlossen."
    else
        warn "PRoot-Extraktion fehlgeschlagen (Exit-Code $ret). Fehler-Details aus $LOGDIR/proot_extract.log:"
        tail -n 30 "$LOGDIR/proot_extract.log" | while read -r l; do warn "  [proot] $l"; done
    fi
fi

if [ "$extracted_ok" -eq 0 ]; then
    info "Starte direkte Fallback-Extraktion..."
    set +e
    sh -c "$COMMAND" > "$LOGDIR/fallback_extract.log" 2>&1
    ret=$?
    set -e

    if [ "$ret" -eq 0 ]; then
        extracted_ok=1
        info "Fallback-Extraktion erfolgreich abgeschlossen."
    else
        error "FEHLER: Auch die Fallback-Extraktion ist fehlgeschlagen (Exit-Code $ret). Fehler-Details:"
        tail -n 40 "$LOGDIR/fallback_extract.log" | while read -r l; do error "  [tar] $l"; done

        if grep -qi "no space left" "$LOGDIR/fallback_extract.log"; then
            error "Ursache: Nicht genügend freier Speicherplatz auf dem Gerät!"
        elif grep -qi "permission denied" "$LOGDIR/fallback_extract.log"; then
            error "Ursache: Schreibrechte im Zielverzeichnis fehlen!"
        elif grep -qi "not tar archive\|unexpected end" "$LOGDIR/fallback_extract.log"; then
            error "Ursache: RootFS-Archiv ist beschädigt!"
        fi
        exit 1
    fi
fi

SANDBOX_DIR="$LOCAL/sandbox"

# Post-processing: re-create container-internal symlinks safely
mkdir -p "$SANDBOX_DIR/var" "$SANDBOX_DIR/etc/alternatives" "$SANDBOX_DIR/usr/bin"
info "Repariere Container-interne Symlinks..."

if [ -f "$SANDBOX_DIR/usr/bin/mawk" ]; then
    ln -snf /usr/bin/mawk "$SANDBOX_DIR/etc/alternatives/awk" 2>/dev/null || true
    ln -snf /etc/alternatives/awk "$SANDBOX_DIR/usr/bin/awk" 2>/dev/null || true
    info "Symlink awk -> /usr/bin/mawk wiederhergestellt."
elif [ -f "$SANDBOX_DIR/usr/bin/gawk" ]; then
    ln -snf /usr/bin/gawk "$SANDBOX_DIR/etc/alternatives/awk" 2>/dev/null || true
    ln -snf /etc/alternatives/awk "$SANDBOX_DIR/usr/bin/awk" 2>/dev/null || true
    info "Symlink awk -> /usr/bin/gawk wiederhergestellt."
fi

ln -snf ../run "$SANDBOX_DIR/var/run" 2>/dev/null || true
ln -snf ../lock "$SANDBOX_DIR/var/lock" 2>/dev/null || true
info "Symlinks var/run und var/lock wiederhergestellt."

# Validierung der Vollständigkeit des entpackten RootFS
info "Prüfe Vollständigkeit des entpackten RootFS..."
if [ ! -f "$SANDBOX_DIR/bin/sh" ] && [ ! -f "$SANDBOX_DIR/usr/bin/sh" ] && [ ! -f "$SANDBOX_DIR/bin/bash" ] && [ ! -f "$SANDBOX_DIR/usr/bin/bash" ]; then
    error "FEHLER: Das entpackte RootFS ist unvollständig (Shell /bin/sh oder /usr/bin/bash nicht gefunden). Abbruch!"
    exit 1
fi
info "RootFS-Vollständigkeitsprüfung erfolgreich!"

info "Setting up the Ubuntu container…"

# values you want written
nameserver="nameserver 8.8.8.8
nameserver 8.8.4.4
nameserver 1.1.1.1
nameserver 9.9.9.9"

hosts="127.0.0.1   localhost.localdomain localhost

# IPv6.
::1         localhost.localdomain localhost ip6-localhost ip6-loopback
fe00::0     ip6-localnet
ff00::0     ip6-mcastprefix
ff02::1     ip6-allnodes
ff02::2     ip6-allrouters
ff02::3     ip6-allhosts"

# ensure etc directory exists
mkdir -p "$SANDBOX_DIR/etc"

# write hostname
printf '%s\n' "MobileIDE" > "$SANDBOX_DIR/etc/hostname"

# write resolv.conf (create file if not exists, then overwrite)
: > "$SANDBOX_DIR/etc/resolv.conf"
printf '%s\n' "$nameserver" > "$SANDBOX_DIR/etc/resolv.conf"

# write hosts
printf '%s\n' "$hosts" > "$SANDBOX_DIR/etc/hosts"

groupFile="$SANDBOX_DIR/etc/group"
aid="$(id -g)"

linesToAdd="
inet:x:3003
everybody:x:9997
android_app:x:20455
android_debug:x:50455
android_cache:x:$((10000 + aid))
android_storage:x:$((40000 + aid))
android_media:x:$((50000 + aid))
android_external_storage:x:1077
"
# create the file if it doesn't exist
[ -f "$groupFile" ] || : > "$groupFile"

existing="$(cat "$groupFile")"

# iterate through lines
echo "$linesToAdd" | while IFS= read -r line; do
    [ -z "$line" ] && continue
    gid="${line##*:}"  # get part after last colon
    case "$existing" in
        *:"$gid"*) : ;;   # already exists → skip
        *) printf '%s\n' "$line" >> "$groupFile" ;;
    esac
done

rm "$TMP_DIR"/sandbox.tar.gz
# DO NOT REMOVE THIS FILE JUST DON'T, TRUST ME
touch $LOCAL/.terminal_setup_ok_DO_NOT_REMOVE

info "Installing Node.js APT hook…"

mkdir -p "$SANDBOX_DIR/etc/apt/apt.conf.d"
mkdir -p "$SANDBOX_DIR/usr/local/bin"

cat > "$SANDBOX_DIR/etc/apt/apt.conf.d/99node-hook" << 'EOF'
DPkg::Post-Invoke {
    "if [ -x /usr/bin/node ]; then /usr/local/bin/node-postinstall.sh; fi";
};
EOF

cat > "$SANDBOX_DIR/usr/local/bin/node-postinstall.sh" << 'EOF'
#!/bin/sh
set -e

echo "[node-hook] Running Node.js post-install hook..."

JEMALLOC=""

echo "[node-hook] Searching for jemalloc..."

for path in \
    /usr/lib/*/libjemalloc.so* \
    /usr/lib/libjemalloc.so* \
    /lib/*/libjemalloc.so* \
    /lib/libjemalloc.so*; do

    if [ -e "$path" ]; then
        JEMALLOC="$path"
        echo "[node-hook] Found jemalloc: $JEMALLOC"
        break
    fi
done

if [ -z "$JEMALLOC" ]; then
    echo "[node-hook] jemalloc not installed, skipping"
    exit 0
fi

if [ ! -e /usr/bin/node ]; then
    echo "[node-hook] Node binary not found, skipping"
    exit 0
fi

if [ -e /usr/bin/node.distrib ]; then
    echo "[node-hook] Node already wrapped, skipping"
    exit 0
fi

echo "[node-hook] Verifying node binary..."

if file /usr/bin/node | grep -q ELF; then
    echo "[node-hook] Wrapping Node.js with jemalloc..."

    mv /usr/bin/node /usr/bin/node.distrib

    cat > /usr/bin/node << WRAP
#!/bin/sh
LD_PRELOAD=$JEMALLOC exec /usr/bin/node.distrib "\$@"
WRAP

    chmod +x /usr/bin/node

    echo "[node-hook] Node wrapper installed successfully"
else
    echo "[node-hook] /usr/bin/node is not an ELF binary, skipping"
fi
EOF

chmod +x "$SANDBOX_DIR/usr/local/bin/node-postinstall.sh"

info "Node.js APT hook installed"

if [ $# -gt 0 ]; then
    if [ "$1" != "true" ]; then
        sh $@
    fi
else
    clear
    sh $LOCAL/bin/sandbox
fi