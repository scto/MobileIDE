#!/bin/bash
ACTION=$1
VERSION=$2
SDK_ROOT="/opt/android-sdk"
if [ ! -d "$SDK_ROOT" ]; then
    SDK_ROOT="/root/android-sdk"
fi

if [ "$ACTION" = "install" ]; then
    echo "Installing Android-SDK platform $VERSION..."
    yes | sdkmanager --sdk_root="$SDK_ROOT" "platforms;android-$VERSION"
elif [ "$ACTION" = "uninstall" ]; then
    echo "Uninstalling Android-SDK platform $VERSION..."
    sdkmanager --sdk_root="$SDK_ROOT" --uninstall "platforms;android-$VERSION"
fi
