#!/bin/bash
ACTION=$1
VERSION=$2

if [ "$ACTION" = "install" ]; then
    echo "Installing OpenJDK $VERSION..."
    apk add "openjdk$VERSION"
elif [ "$ACTION" = "uninstall" ]; then
    echo "Uninstalling OpenJDK $VERSION..."
    apk del "openjdk$VERSION"
fi
