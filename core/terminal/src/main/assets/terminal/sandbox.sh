PROOT_TMP_DIR="${PROOT_TMP_DIR:-$PRIVATE_DIR/usr/tmp}"
export PROOT_TMP_DIR
mkdir -p "$PROOT_TMP_DIR"
chmod 700 "$PROOT_TMP_DIR" 2>/dev/null || chmod 755 "$PROOT_TMP_DIR" 2>/dev/null || true

touch "$PROOT_TMP_DIR/.write_test" 2>/dev/null || {
    echo "ERROR: PROOT_TMP_DIR ($PROOT_TMP_DIR) is not writable!" >&2
    exit 1
}
rm -f "$PROOT_TMP_DIR/.write_test"

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
ARGS="$ARGS -b $EXT_HOME:/home"
ARGS="$ARGS -b $EXT_HOME:/root"
ARGS="$ARGS -b $PRIVATE_DIR"
ARGS="$ARGS -b $LOCAL/stat:/proc/stat"
ARGS="$ARGS -b $LOCAL/vmstat:/proc/vmstat"

if [ -d "/proc/self/fd" ]; then
  ARGS="$ARGS -b /proc/self/fd:/dev/fd"
fi

if [ -e "$(realpath /proc/self/fd/0 2>/dev/null)" ]; then
  ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin"
fi

if [ -e "$(realpath /proc/self/fd/1 2>/dev/null)" ]; then
  ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout"
fi

if [ -e "$(realpath /proc/self/fd/2 2>/dev/null)" ]; then
  ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr"
fi

ARGS="$ARGS -b $PRIVATE_DIR"
ARGS="$ARGS -b /sys"

if [ ! -d "$LOCAL/sandbox/tmp" ]; then
 mkdir -p "$LOCAL/sandbox/tmp"
 chmod 1777 "$LOCAL/sandbox/tmp"
fi

if [ -d "$LOCAL/sandbox" ]; then
 mkdir -p "$LOCAL/sandbox/etc"
 printf '%s\n' "nameserver 8.8.8.8" "nameserver 8.8.4.4" "nameserver 1.1.1.1" "nameserver 9.9.9.9" > "$LOCAL/sandbox/etc/resolv.conf"
fi

ARGS="$ARGS -b $LOCAL/sandbox/tmp:/dev/shm"

ARGS="$ARGS -r $LOCAL/sandbox"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

chmod -R +x $LOCAL/bin

if [ $# -gt 0 ]; then
    $PROOT $ARGS /bin/bash --rcfile $LOCAL/bin/init -i -c "$*"
else
    $PROOT $ARGS /bin/bash --rcfile $LOCAL/bin/init -i
fi