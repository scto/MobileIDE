# shellcheck disable=SC2034
force_color_prompt=yes

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

if [ -e "/proc/self/fd" ]; then
  bind_fd=1
  for fd in 0 1 2; do
    if [ -e "/proc/self/fd/$fd" ]; then
      target=$(readlink "/proc/self/fd/$fd" 2>/dev/null)
      case "$target" in
        pipe:*|socket:*) bind_fd=0 ;;
      esac
    fi
  done
  if [ "$bind_fd" -eq 1 ]; then
    ARGS="$ARGS -b /proc/self/fd:/dev/fd"
  fi
fi

if [ -e "/proc/self/fd/0" ]; then
  target=$(readlink "/proc/self/fd/0" 2>/dev/null)
  case "$target" in
    pipe:*|socket:*) ;;
    *) ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin" ;;
  esac
fi

if [ -e "/proc/self/fd/1" ]; then
  target=$(readlink "/proc/self/fd/1" 2>/dev/null)
  case "$target" in
    pipe:*|socket:*) ;;
    *) ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout" ;;
  esac
fi

if [ -e "/proc/self/fd/2" ]; then
  target=$(readlink "/proc/self/fd/2" 2>/dev/null)
  case "$target" in
    pipe:*|socket:*) ;;
    *) ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr" ;;
  esac
fi


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

if [ $# -gt 0 ]; then
    $PROOT $ARGS /bin/bash --rcfile $LOCAL/bin/init -i -c "$*"
else
    $PROOT $ARGS /bin/bash --rcfile $LOCAL/bin/init -i
fi

