#!/usr/bin/env bash

set -euo pipefail


############################################
# McsIDE Android Studio Migrator
# v7
############################################


SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

WORK_DIR="${SCRIPT_DIR}/.migration"

OUTPUT_DIR="${SCRIPT_DIR}/app/src/main/assets/templates"


mkdir -p "$WORK_DIR"
mkdir -p "$OUTPUT_DIR"


############################################
# Logging
############################################

info() {
  echo "[MIGRATOR] $1"
}

error() {
  echo "[ERROR] $1"
  exit 1
}


############################################
# Args
############################################

SOURCE="${1:-}"

[ -z "$SOURCE" ] \
  && error "Usage: ./migrate-android-studio-template.sh <zip|folder>"

[ ! -e "$SOURCE" ] \
  && error "Source not found"


############################################
# Cleanup
############################################

rm -rf "$WORK_DIR"

mkdir -p "$WORK_DIR"


############################################
# Extract
############################################

if [[ "$SOURCE" == *.zip ]]; then

  info "Extracting zip..."

  unzip -q "$SOURCE" -d "$WORK_DIR"

  SEARCH_DIR="$WORK_DIR"

else

  SEARCH_DIR="$SOURCE"

fi


############################################
# Detect Template Root
############################################

detect_template_dir() {

  local base="$1"
  local found


  ##########################################
  # Kotlin DSL Templates
  ##########################################

  found=$(
    find "$base" \
      -name "*Template.kt" \
      -print \
      -quit
  )

  if [ -n "$found" ]; then

    dirname "$found"

    return 0
  fi


  ##########################################
  # XML Templates
  ##########################################

  found=$(
    find "$base" \
      -name "template.xml" \
      -print \
      -quit
  )

  if [ -n "$found" ]; then

    dirname "$found"

    return 0
  fi


  ##########################################
  # FTL Templates
  ##########################################

  found=$(
    find "$base" \
      -name "recipe.xml.ftl" \
      -print \
      -quit
  )

  if [ -n "$found" ]; then

    dirname "$found"

    return 0
  fi


  return 1
}


TEMPLATE_DIR="$(
  detect_template_dir "$SEARCH_DIR"
)" || error "Android Studio template not detected"


info "Using template:"
echo "$TEMPLATE_DIR"


############################################
# Target
############################################

TEMPLATE_ID="$(basename "$TEMPLATE_DIR")"

TARGET="$OUTPUT_DIR/$TEMPLATE_ID"

rm -rf "$TARGET"

mkdir -p "$TARGET"


############################################
# Thumbnail
############################################

THUMBNAIL=$(
  find "$TEMPLATE_DIR" \
    \( \
      -name "*.png" \
      -o -name "*.webp" \
    \) \
    -print \
    -quit || true
)

if [ -n "$THUMBNAIL" ]; then

  cp "$THUMBNAIL" \
    "$TARGET/thumbnail.png"
fi


############################################
# Source Detection
############################################

CONTENT_ROOT=""


# Kotlin DSL
if [ -d "$TEMPLATE_DIR/src" ]; then

  CONTENT_ROOT="$TEMPLATE_DIR"

# Classic
elif [ -d "$TEMPLATE_DIR/root" ]; then

  CONTENT_ROOT="$TEMPLATE_DIR/root"

else

  error "No src/ or root/ found"

fi


############################################
# Copy
############################################

copy_if_exists() {

  local source="$1"
  local target="$2"

  if [ -d "$source" ]; then

    info "Migrating $(basename "$source")"

    cp -R "$source" "$target"
  fi
}


copy_if_exists \
  "$CONTENT_ROOT/src" \
  "$TARGET/src"

copy_if_exists \
  "$CONTENT_ROOT/res" \
  "$TARGET/res"

copy_if_exists \
  "$CONTENT_ROOT/assets" \
  "$TARGET/assets"

copy_if_exists \
  "$CONTENT_ROOT/gradle" \
  "$TARGET/gradle"


############################################
# Manifest
############################################

MANIFEST=$(
  find "$CONTENT_ROOT" \
    -name "AndroidManifest.xml*" \
    -print \
    -quit || true
)

if [ -n "$MANIFEST" ]; then

  mkdir -p "$TARGET/manifest"

  cp "$MANIFEST" \
    "$TARGET/manifest/AndroidManifest.xml"
fi


############################################
# Placeholder Migration
############################################

info "Migrating placeholders..."


find "$TARGET" -type f | while read -r FILE
do

  sed -i \
    -e 's/${packageName}/__PACKAGE__/g' \
    -e 's/${applicationName}/__PROJECT_NAME__/g' \
    -e 's/${activityClass}/__ACTIVITY_NAME__/g' \
    -e 's/${fragmentClass}/__FRAGMENT_NAME__/g' \
    -e 's/${className}/__CLASS_NAME__/g' \
    "$FILE"


  if [[ "$FILE" == *.ftl ]]; then

    mv "$FILE" "${FILE%.ftl}"
  fi

done


############################################
# Metadata
############################################

cat > "$TARGET/template.json" << EOF
{
  "id": "$TEMPLATE_ID",
  "name": "$TEMPLATE_ID",
  "description": "Migrated Android Studio Template",
  "version": "1.0.0"
}
EOF


############################################
# Done
############################################

info "Migration complete"

echo
echo "Output:"
echo "$TARGET"
echo