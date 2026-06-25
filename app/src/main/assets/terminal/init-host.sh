#!/system/bin/sh

# Read selected distribution from environment (default: ubuntu)
DISTRO="${MOBILEIDE_DISTRO:-ubuntu}"
DISTRO_DIR="$PREFIX/local/$DISTRO"

mkdir -p "$DISTRO_DIR"

# Ensure local directories exist
LOCAL="$PREFIX/local"
mkdir -p "$LOCAL/bin" "$LOCAL/lib"

# Check/copy proot and shared libraries
if [ ! -e "$LOCAL/bin/proot" ]; then
    if [ -f "$PREFIX/files/proot" ]; then
        cp "$PREFIX/files/proot" "$LOCAL/bin/"
        chmod +x "$LOCAL/bin/proot"
    fi
fi

for sofile in "$PREFIX/files/"*.so.2; do
    [ -e "$sofile" ] || continue
    dest="$LOCAL/lib/$(basename "$sofile")"
    if [ ! -e "$dest" ]; then
        cp "$sofile" "$dest"
    fi
done

# Create the /sandbox symlink pointing to the current distribution directory
ln -snf "$DISTRO_DIR" "$LOCAL/sandbox"

# Set up required variables for setup.sh & sandbox.sh
export LOCAL
export TMP_DIR="${TMPDIR:-$PREFIX/cache}"
export PRIVATE_DIR="$PREFIX/files"
export PROOT="$LOCAL/bin/proot"
export EXT_HOME="$DISTRO_DIR/root"

# Check if setup was already performed successfully
if [ ! -f "$LOCAL/.terminal_setup_ok_DO_NOT_REMOVE" ]; then
    # Prepare the tarball for setup.sh
    if [ -f "$PREFIX/files/$DISTRO.tar.gz" ]; then
        mkdir -p "$TMP_DIR"
        cp "$PREFIX/files/$DISTRO.tar.gz" "$TMP_DIR/sandbox.tar.gz"
    fi
    
    # Run the setup script which will extract and configure the container, 
    # and then automatically chain to sandbox.sh
    exec sh "$LOCAL/bin/setup" "$@"
else
    # Run the sandbox launcher directly
    exec sh "$LOCAL/bin/sandbox" "$@"
fi

