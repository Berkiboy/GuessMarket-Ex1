#!/bin/bash
# Builds Exercise 3 without an IDE:
#   out-build/engine.jar          the engine (model, XML loader, DTOs)
#   out-build/GuessMarket.war     the server, with engine.jar and Gson inside WEB-INF/lib
#   out-build/client/             the JavaFX client: client.jar + dependencies + run.bat (Windows JavaFX jars)
#
# Needs lib/gson-2.11.0.jar, lib/okhttp (OkHttp + okio + kotlin-stdlib), lib/apache-tomcat-10.1.34 (for servlet-api.jar), lib/javafx (to compile)
# and lib/javafx-win (shipped with the client).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out-build"
GSON="$ROOT/lib/gson-2.11.0.jar"
SERVLET_API="$ROOT/lib/apache-tomcat-10.1.34/lib/servlet-api.jar"
FX_COMPILE="$(ls "$ROOT"/lib/javafx/*.jar | tr '\n' ':')"
FX_WIN_DIR="$ROOT/lib/javafx-win"
OKHTTP_DIR="$ROOT/lib/okhttp"
OKHTTP_CP="$(ls "$OKHTTP_DIR"/*.jar | tr '\n' ':')"

rm -rf "$OUT"
mkdir -p "$OUT/classes/engine" "$OUT/classes/server" "$OUT/classes/client" "$OUT/war/WEB-INF/lib" "$OUT/client"

echo "Compiling engine..."
javac --release 25 -encoding UTF-8 -d "$OUT/classes/engine" $(find "$ROOT/engine/src" -name '*.java')
jar --create --file "$OUT/engine.jar" -C "$OUT/classes/engine" .

echo "Compiling server..."
javac --release 25 -encoding UTF-8 -d "$OUT/classes/server" -cp "$OUT/engine.jar:$GSON:$SERVLET_API" \
    $(find "$ROOT/server/src" -name '*.java')
mkdir -p "$OUT/war/WEB-INF/classes"
cp -R "$OUT/classes/server/." "$OUT/war/WEB-INF/classes/"
cp -R "$ROOT/server/web/." "$OUT/war/"
cp "$OUT/engine.jar" "$GSON" "$OUT/war/WEB-INF/lib/"
jar --create --file "$OUT/GuessMarket.war" -C "$OUT/war" .

echo "Compiling client..."
javac --release 25 -encoding UTF-8 -d "$OUT/classes/client" -cp "$OUT/engine.jar:$GSON:$OKHTTP_CP$FX_COMPILE" \
    $(find "$ROOT/client-fx/src" -name '*.java')
# FXML and CSS resources keep their package folders inside the jar
(cd "$ROOT/client-fx/src" && find . \( -name '*.fxml' -o -name '*.css' \) | while read -r f; do
    mkdir -p "$OUT/classes/client/$(dirname "$f")"; cp "$f" "$OUT/classes/client/$f"; done)
jar --create --file "$OUT/client/guess-market-client.jar" --manifest "$ROOT/client-fx/src/META-INF/MANIFEST.MF" \
    -C "$OUT/classes/client" .
cp "$OUT/engine.jar" "$GSON" "$OKHTTP_DIR"/*.jar "$OUT/client/"
cp "$FX_WIN_DIR"/*.jar "$OUT/client/"
cp "$ROOT/client-fx/run.bat" "$OUT/client/"

echo "Done:"
ls -la "$OUT" "$OUT/client"
