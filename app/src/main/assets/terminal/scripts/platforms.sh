#!/bin/bash
ACTION=$1
VERSION=$2
SDK_ROOT="/opt/android-sdk"
if [ ! -d "$SDK_ROOT" ]; then
    SDK_ROOT="/root/android-sdk"
fi

if [ "$ACTION" = "install" ]; then
    echo "Installing Platform-Tools and setting revision $VERSION..."
    yes | sdkmanager --sdk_root="$SDK_ROOT" "platform-tools"
    mkdir -p "$SDK_ROOT/platform-tools"
    echo "Pkg.Revision=$VERSION.0.0" > "$SDK_ROOT/platform-tools/source.properties"
elif [ "$ACTION" = "uninstall" ]; then
    echo "Uninstalling Platform-Tools..."
    rm -rf "$SDK_ROOT/platform-tools"
fi
