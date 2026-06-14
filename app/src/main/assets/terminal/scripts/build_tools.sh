#!/bin/bash
ACTION=$1
VERSION=$2
SDK_ROOT="/opt/android-sdk"
if [ ! -d "$SDK_ROOT" ]; then
    SDK_ROOT="/root/android-sdk"
fi

if [ "$VERSION" = "33" ]; then
    FULL_VER="33.0.2"
elif [ "$VERSION" = "34" ]; then
    FULL_VER="34.0.0"
elif [ "$VERSION" = "35" ]; then
    FULL_VER="35.0.0"
elif [ "$VERSION" = "36" ]; then
    FULL_VER="36.0.0-rc1"
else
    FULL_VER="$VERSION.0.0"
fi

if [ "$ACTION" = "install" ]; then
    echo "Installing Build-Tools $FULL_VER..."
    yes | sdkmanager --sdk_root="$SDK_ROOT" "build-tools;$FULL_VER"
elif [ "$ACTION" = "uninstall" ]; then
    echo "Uninstalling Build-Tools $FULL_VER..."
    sdkmanager --sdk_root="$SDK_ROOT" --uninstall "build-tools;$FULL_VER"
fi
