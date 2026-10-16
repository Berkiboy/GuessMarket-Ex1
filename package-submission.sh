#!/bin/bash
# Builds everything and assembles the Exercise 3 submission folder and zip.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SUB="$ROOT/submission3"
ZIP_NAME="GuessMarket_Ex3_323873414_206593733.zip"

"$ROOT/build.sh" > /dev/null

rm -rf "$SUB"
DIR="$SUB/GuessMarket_Ex3_323873414_206593733"
mkdir -p "$DIR/sample-files"
cp "$ROOT/out-build/GuessMarket.war" "$DIR/"
cp -R "$ROOT/out-build/client" "$DIR/client"
cp "$ROOT"/sample-data/ex3/*.xml "$DIR/sample-files/"
cp "$ROOT/readme.docx" "$DIR/readme.docx"

# The submission is one folder, compressed as a single zip
(cd "$SUB" && zip -q -r -X "$ZIP_NAME" "$(basename "$DIR")")
echo "Submission ready: $SUB/$ZIP_NAME"
unzip -l "$SUB/$ZIP_NAME"
