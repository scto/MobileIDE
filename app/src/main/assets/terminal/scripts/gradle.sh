#!/bin/bash
ACTION=$1

if [ "$ACTION" = "install" ]; then
    echo "Installing Gradle build system..."
    apk add gradle
elif [ "$ACTION" = "uninstall" ]; then
    echo "Uninstalling Gradle build system..."
    apk del gradle
elif [ "$ACTION" = "clear-cache" ]; then
    echo "Clearing Gradle cache files..."
    rm -rf /root/.gradle/caches
    rm -rf /data/data/com.termux/files/home/.gradle/caches
fi
