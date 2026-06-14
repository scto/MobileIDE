#!/bin/bash
ACTION=$1
VERSION=$2
SDK_ROOT="/opt/android-sdk"
if [ ! -d "$SDK_ROOT" ]; then
    SDK_ROOT="/root/android-sdk"
fi

if [ "$VERSION" = "26" ]; then
    FULL_VER="26.1.10909125"
elif [ "$VERSION" = "27" ]; then
    FULL_VER="27.0.12077973"
elif [ "$VERSION" = "28" ]; then
    FULL_VER="28.0.12433566"
elif [ "$VERSION" = "29" ]; then
    FULL_VER="29.0.12433566"
else
    FULL_VER="$VERSION"
fi

if [ "$ACTION" = "install" ]; then
    echo "Installing NDK $FULL_VER..."
    yes | sdkmanager --sdk_root="$SDK_ROOT" "ndk;$FULL_VER"
elif [ "$ACTION" = "uninstall" ]; then
    echo "Uninstalling NDK $FULL_VER..."
    sdkmanager --sdk_root="$SDK_ROOT" --uninstall "ndk;$FULL_VER"
fi
