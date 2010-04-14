#!/bin/sh
NDK=$HOME/opt/android-ndk
#----
set -e
PROJ_DIR=$(dirname "$PWD")
PROJ_NAME=$(basename "$PROJ_DIR")

mkdir -p "$NDK/apps/$PROJ_NAME"
cat <<EOF >"$NDK/apps/$PROJ_NAME/Application.mk"
APP_PROJECT_PATH := $PROJ_DIR
APP_MODULES      := test
EOF

cd $NDK
make APP="$PROJ_NAME"

