#!/system/bin/sh
# MobileIDE  - A powerful IDE for Android app development.
# Copyright (C) 2025  scto  <tschmid35@gmail.com>

# ==========================================
# OS Detection (Entpackt durch Kotlin!)
# ==========================================
if [ -d "$PREFIX/ubuntu/etc" ] || [ -f "$PREFIX/ubuntu.tar.gz" ]; then
    ROOTFS_DIR="$PREFIX/ubuntu"
else
    ROOTFS_DIR="$PREFIX/alpine"
fi

# ==========================================
# PRoot Arguments Setup
# ==========================================
if [ -n "$PROOT_EXEC" ] && [ -f "$PROOT_EXEC" ]; then
    PROOT_BIN="$PROOT_EXEC"
else
    PROOT_BIN="$PREFIX/bin/proot"
fi
INIT_SCRIPT="$PREFIX/init.sh"

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
ARGS="$ARGS -b $PREFIX/stat:/proc/stat"
ARGS="$ARGS -b $PREFIX/vmstat:/proc/vmstat"

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

ARGS="$ARGS -b /sys"

# Temp-Verzeichnis dynamisch im aktiven Rootfs anlegen
if [ ! -d "$ROOTFS_DIR/tmp" ]; then
 mkdir -p "$ROOTFS_DIR/tmp"
 chmod 1777 "$ROOTFS_DIR/tmp"
fi
ARGS="$ARGS -b $ROOTFS_DIR/tmp:/dev/shm"

# PRoot auf das korrekte Rootfs zeigen lassen
ARGS="$ARGS -r $ROOTFS_DIR"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

# Aufruf von 'bash' anstelle von 'sh'
$LINKER "$PROOT_BIN" $ARGS bash "$INIT_SCRIPT" "$@"