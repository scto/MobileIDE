#!/system/bin/sh

# Read selected distribution from environment (default: ubuntu)
DISTRO="${MOBILEIDE_DISTRO:-ubuntu}"
DISTRO_DIR="$PREFIX/local/$DISTRO"

mkdir -p "$DISTRO_DIR"

if [ -z "$(ls -A "$DISTRO_DIR" | grep -vE '^(root|tmp)$')" ]; then
    if [ -f "$PREFIX/files/$DISTRO.tar.gz" ]; then
        tar -xf "$PREFIX/files/$DISTRO.tar.gz" -C "$DISTRO_DIR"
    fi
fi

if [ ! -e "$PREFIX/local/bin/proot" ]; then
    if [ -f "$PREFIX/files/proot" ]; then
        cp "$PREFIX/files/proot" "$PREFIX/local/bin"
    fi
fi

for sofile in "$PREFIX/files/"*.so.2; do
    [ -e "$sofile" ] || continue
    dest="$PREFIX/local/lib/$(basename "$sofile")"
    if [ ! -e "$dest" ]; then
        cp "$sofile" "$dest"
    fi
done

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
ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b $PREFIX/local/stat:/proc/stat"
ARGS="$ARGS -b $PREFIX/local/vmstat:/proc/vmstat"

if [ -e "/proc/self/fd" ]; then
  ARGS="$ARGS -b /proc/self/fd:/dev/fd"
fi

if [ -e "/proc/self/fd/0" ]; then
  ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin"
fi

if [ -e "/proc/self/fd/1" ]; then
  ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout"
fi

if [ -e "/proc/self/fd/2" ]; then
  ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr"
fi

ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b /sys"

if [ ! -d "$DISTRO_DIR/tmp" ]; then
 mkdir -p "$DISTRO_DIR/tmp"
 chmod 1777 "$DISTRO_DIR/tmp"
fi

ARGS="$ARGS -b $DISTRO_DIR/tmp:/dev/shm"
ARGS="$ARGS -r $DISTRO_DIR"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

# Set up DNS inside container rootfs if etc/resolv.conf doesn't exist or is empty
mkdir -p "$DISTRO_DIR/etc"
if [ ! -s "$DISTRO_DIR/etc/resolv.conf" ]; then
    echo "nameserver 8.8.8.8" > "$DISTRO_DIR/etc/resolv.conf"
    echo "nameserver 1.1.1.1" >> "$DISTRO_DIR/etc/resolv.conf"
fi

export LOCAL="$PREFIX/local"
if [ $# -gt 0 ]; then
    exec $LINKER $PREFIX/local/bin/proot $ARGS /bin/bash -c "source $PREFIX/local/bin/init && eval \$1" -- "$1"
else
    exec $LINKER $PREFIX/local/bin/proot $ARGS /bin/bash --rcfile $PREFIX/local/bin/init -i
fi
