#!/bin/bash
ACTION=$1
VERSION=$2
SDK_ROOT="/opt/android-sdk"
if [ ! -d "$SDK_ROOT" ]; then
    SDK_ROOT="/root/android-sdk"
fi

if [ "$VERSION" = "3.18" ]; then
    FULL_VER="3.18.0"
elif [ "$VERSION" = "3.20" ]; then
    FULL_VER="3.20.0"
elif [ "$VERSION" = "3.25" ]; then
    FULL_VER="3.25.0"
elif [ "$VERSION" = "4.3.1" ]; then
    FULL_VER="4.3.1"
else
    FULL_VER="$VERSION"
fi

if [ "$ACTION" = "install" ]; then
    echo "Installing CMake $FULL_VER..."
    yes | sdkmanager --sdk_root="$SDK_ROOT" "cmake;$FULL_VER"
elif [ "$ACTION" = "uninstall" ]; then
    echo "Uninstalling CMake $FULL_VER..."
    sdkmanager --sdk_root="$SDK_ROOT" --uninstall "cmake;$FULL_VER"
fi
