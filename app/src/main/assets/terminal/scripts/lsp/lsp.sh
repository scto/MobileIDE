#!/bin/bash
ACTION=$1
TYPE=$2

if [ "$TYPE" = "jdtls" ]; then
    if [ "$ACTION" = "install" ]; then
        echo "Installing Java Language Server (jdtls) and dependencies..."
        apk add openjdk17 jdtls
    elif [ "$ACTION" = "uninstall" ]; then
        echo "Uninstalling Java Language Server..."
        apk del jdtls
    fi
elif [ "$TYPE" = "kotlin" ]; then
    if [ "$ACTION" = "install" ]; then
        echo "Installing Kotlin Language Server..."
        apk add -X http://dl-cdn.alpinelinux.org/alpine/edge/testing kotlin-language-server
    elif [ "$ACTION" = "uninstall" ]; then
        echo "Uninstalling Kotlin Language Server..."
        apk del kotlin-language-server
    fi
elif [ "$TYPE" = "typescript" ]; then
    if [ "$ACTION" = "install" ]; then
        echo "Installing TypeScript Language Server..."
        apk add nodejs npm
        npm install -g typescript typescript-language-server
    elif [ "$ACTION" = "uninstall" ]; then
        echo "Uninstalling TypeScript Language Server..."
        npm uninstall -g typescript-language-server
    fi
elif [ "$TYPE" = "web" ]; then
    if [ "$ACTION" = "install" ]; then
        echo "Installing Web (HTML/CSS/JSON) Language Servers..."
        apk add nodejs npm
        npm install -g vscode-langservers-extracted
    elif [ "$ACTION" = "uninstall" ]; then
        echo "Uninstalling Web Language Servers..."
        npm uninstall -g vscode-langservers-extracted
    fi
fi
