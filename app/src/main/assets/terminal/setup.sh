set -e

. "$LOCAL/bin/utils"

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

ARGS="$ARGS -b $PRIVATE_DIR"
ARGS="$ARGS -b /sys"

ARGS="$ARGS -r /"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

COMMAND="(cd $LOCAL/sandbox && (tar -xzf $TMP_DIR/sandbox.tar.gz || (gzip -dc $TMP_DIR/sandbox.tar.gz | tar -xf -)))"

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

info "Setting up the Ubuntu container…"

# values you want written
nameserver="nameserver 8.8.8.8
nameserver 8.8.4.4"

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

# Read selected setup options if the file exists
INSTALL_JDK="17"
INSTALL_GRADLE="apt"
INSTALL_SDK="35"
INSTALL_BUILD_TOOLS="35.0.0"
INSTALL_CMDLINE_TOOLS="true"
INSTALL_GIT="true"

if [ -f "$LOCAL/setup_options.properties" ]; then
    . "$LOCAL/setup_options.properties"
fi

# Build packages list
packages="bash-completion command-not-found sudo xkb-data libjemalloc-dev"
if [ "$INSTALL_GIT" = "true" ]; then
    packages="$packages git"
fi

if [ "$INSTALL_JDK" = "17" ]; then
    packages="$packages openjdk-17-jdk"
elif [ "$INSTALL_JDK" = "21" ]; then
    packages="$packages openjdk-21-jdk"
fi

if [ "$INSTALL_GRADLE" = "apt" ]; then
    packages="$packages gradle gradle-completion"
fi

info "Installing selected packages inside Ubuntu container: $packages..."
sh $LOCAL/bin/sandbox "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y $packages"

# Mark packages as ensured to prevent slow startup in init.sh
mkdir -p "$SANDBOX_DIR/.cache"
touch "$SANDBOX_DIR/.cache/.packages_ensured"
sh $LOCAL/bin/sandbox "update-command-not-found" >/dev/null 2>&1 || true

if [ "$INSTALL_GRADLE" != "none" ] && [ "$INSTALL_GRADLE" != "apt" ]; then
    info "Installing custom Gradle version $INSTALL_GRADLE..."
    sh $LOCAL/bin/sandbox "apt-get install -y wget unzip && wget -q https://services.gradle.org/distributions/gradle-${INSTALL_GRADLE}-bin.zip -O /tmp/gradle.zip && mkdir -p /opt/gradle && unzip -o -d /opt/gradle /tmp/gradle.zip && ln -sf /opt/gradle/gradle-${INSTALL_GRADLE}/bin/gradle /usr/bin/gradle && rm /tmp/gradle.zip"
fi

if [ "$INSTALL_SDK" != "none" ]; then
    info "Installing Android SDK Platform $INSTALL_SDK and Build-Tools $INSTALL_BUILD_TOOLS..."
    sh $LOCAL/bin/sandbox "apt-get install -y wget unzip"
    
    if [ "$INSTALL_CMDLINE_TOOLS" = "true" ]; then
        info "Installing Command-line tools..."
        sh $LOCAL/bin/sandbox "mkdir -p /root/android-sdk/cmdline-tools && wget -q -O /tmp/sdk.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && unzip -o /tmp/sdk.zip -d /root/android-sdk/cmdline-tools && rm /tmp/sdk.zip && mv /root/android-sdk/cmdline-tools/cmdline-tools /root/android-sdk/cmdline-tools/latest || true"
        
        info "Running sdkmanager to install platforms;android-$INSTALL_SDK and build-tools;$INSTALL_BUILD_TOOLS..."
        sh $LOCAL/bin/sandbox "yes | /root/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/root/android-sdk \"platforms;android-${INSTALL_SDK}\" \"build-tools;${INSTALL_BUILD_TOOLS}\""
    fi
fi

info "Configuring build tools environment automatically..."
sh $LOCAL/bin/sandbox "mkdir -p /root/etc && (
  jdk_dir=\"\"
  if command -v javac >/dev/null 2>&1; then
      jdk_dir=\$(dirname \$(dirname \$(readlink -f \$(which javac))))
  else
      for d in /usr/lib/jvm/java-17-openjdk*; do
          if [ -d \"\$d\" ]; then
               jdk_dir=\"\$d\"
               break
          fi
      done
  fi
  if [ -z \"\$jdk_dir\" ]; then
      for d in /usr/lib/jvm/java-21-openjdk*; do
          if [ -d \"\$d\" ]; then
               jdk_dir=\"\$d\"
               break
          fi
      done
  fi
  printf \"JAVA_HOME=\$jdk_dir\nANDROID_SDK_ROOT=/root/android-sdk\nGRADLE_USER_HOME=/root/.gradle\nAAPT2_HOME=/.mobileide\n\" > /root/etc/mobileide-environment.properties
)"


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

info "Configuring bashrc..."

write_bashrc() {
    cat > "$1" << 'EOF'
# Load bash completion
if [ -f /etc/profile.d/bash_completion.sh ]; then
    . /etc/profile.d/bash_completion.sh
elif [ -f /usr/share/bash-completion/bash_completion ]; then
    . /usr/share/bash-completion/bash_completion
fi

# Automatically export the properties to the environment
if [ -f "/etc/mobileide-environment.properties" ]; then
    set -a
    source "/etc/mobileide-environment.properties"
    set +a
fi
EOF
}

if [ -n "$EXT_HOME" ]; then
    mkdir -p "$EXT_HOME"
    write_bashrc "$EXT_HOME/.bashrc"
fi

if [ -z "$EXT_HOME" ] || [ ! "$EXT_HOME" -ef "$SANDBOX_DIR/root" ]; then
    mkdir -p "$SANDBOX_DIR/root"
    write_bashrc "$SANDBOX_DIR/root/.bashrc"
fi

if [ $# -gt 0 ]; then
    sh $LOCAL/bin/sandbox "$@"
else
    clear
    sh $LOCAL/bin/sandbox
fi