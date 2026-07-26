set -e

. "$LOCAL/bin/utils"

PROOT_TMP_DIR="${PROOT_TMP_DIR:-${TMP_DIR:-$PRIVATE_DIR}/usr/tmp}"
export PROOT_TMP_DIR
export TMP_DIR="${TMP_DIR:-$PROOT_TMP_DIR}"
mkdir -p "$PROOT_TMP_DIR"
chmod 700 "$PROOT_TMP_DIR" 2>/dev/null || chmod 755 "$PROOT_TMP_DIR" 2>/dev/null || true

touch "$PROOT_TMP_DIR/.write_test" 2>/dev/null || {
    echo "ERROR: PROOT_TMP_DIR ($PROOT_TMP_DIR) is not writable! Cannot continue setup." >&2
    exit 1
}
rm -f "$PROOT_TMP_DIR/.write_test"

info "Extracting the Ubuntu container…"

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

TAR_PATH=""
for candidate in "$TMP_DIR/sandbox.tar.gz" "$PROOT_TMP_DIR/sandbox.tar.gz" "$PRIVATE_DIR/ubuntu.tar.gz" "$PRIVATE_DIR/alpine.tar.gz" "$PRIVATE_DIR/${MOBILEIDE_DISTRO:-ubuntu}.tar.gz"; do
    if [ -f "$candidate" ] && [ -s "$candidate" ]; then
        TAR_PATH="$candidate"
        break
    fi
done

if [ -z "$TAR_PATH" ]; then
    error "No valid rootfs tarball found!"
    exit 1
fi

TAR_EXCLUDES="--exclude=etc/alternatives/* --exclude=var/run --exclude=var/lock --exclude=etc/rmt --exclude=etc/systemd/system/*.wants/* --exclude=usr/bin/awk --exclude=usr/bin/nawk --exclude=usr/bin/pager --exclude=usr/bin/which --exclude=usr/sbin/rmt"
TAR_OPTS="--hard-dereference --no-same-owner --no-same-permissions"

COMMAND="(cd $LOCAL/sandbox && (tar -xzf $TAR_PATH $TAR_EXCLUDES $TAR_OPTS 2>/dev/null || (gzip -dc $TAR_PATH | tar -xf - $TAR_EXCLUDES $TAR_OPTS 2>/dev/null) || tar -xf $TAR_PATH $TAR_EXCLUDES $TAR_OPTS 2>/dev/null))"

info "Validating PRoot binary..."
set +e
$PROOT $ARGS /system/bin/sh -c "true" 2>/dev/null
proot_test_ret=$?
set -e

if [ "$proot_test_ret" -ne 0 ]; then
    warn "PRoot pre-check test returned exit code $proot_test_ret"
fi

set +e
$PROOT $ARGS /system/bin/sh -c "$COMMAND"
ret=$?
set -e

DEGRADED_MARKER="$LOCAL/.sandbox_degraded"

if [ "$ret" -ne 0 ]; then
    warn "PRoot extraction failed (exit code $ret), falling back to direct extraction..."

    set +e
    sh -c "$COMMAND"
    ret=$?
    set -e

    if [ "$ret" -ne 0 ]; then
        error "Extraction failed completely (exit code $ret)! Cannot continue setup."
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