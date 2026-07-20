#!/bin/sh
set -e
rm -rf output
GRADLE_ARGS=""
if [ -d "/data/data/com.termux/files/usr/bin" ]; then
    GRADLE_ARGS="-Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2"
fi
sh ./gradlew assembleRelease $GRADLE_ARGS
sh ./gradlew :app:createFinalZip $GRADLE_ARGS
