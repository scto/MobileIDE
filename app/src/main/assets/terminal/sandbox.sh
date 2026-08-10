# shellcheck disable=SC2034
force_color_prompt=yes

ARGS="--kill-on-exit"
ARGS="$ARGS -w /home"

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
[ -f "$LOCAL/stat" ] && ARGS="$ARGS -b $LOCAL/stat:/proc/stat"
[ -f "$LOCAL/vmstat" ] && ARGS="$ARGS -b $LOCAL/vmstat:/proc/vmstat"



ARGS="$ARGS -b $PRIVATE_DIR"
ARGS="$ARGS -b /sys"

if [ ! -d "$LOCAL/sandbox/tmp" ]; then
 mkdir -p "$LOCAL/sandbox/tmp"
 chmod 1777 "$LOCAL/sandbox/tmp"
fi

ARGS="$ARGS -b $LOCAL/sandbox/tmp:/dev/shm"

ARGS="$ARGS -r $LOCAL/sandbox"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

chmod -R +x $LOCAL/bin

SHELL_EXEC="${MOBILEIDE_SHELL:-/bin/bash}"
if [ ! -x "$SHELL_EXEC" ]; then
    SHELL_EXEC="/bin/bash"
fi

if [ $# -gt 0 ]; then
    $PROOT $ARGS "$SHELL_EXEC" --rcfile $LOCAL/bin/init -i -c "$*"
else
    $PROOT $ARGS "$SHELL_EXEC" --rcfile $LOCAL/bin/init -i
fi