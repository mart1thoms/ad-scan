#!/bin/bash
set -e

SCREEN_NAME="ad-scan"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/ad-scan-app/target/ad-scan-app.jar"

cd "$SCRIPT_DIR"

echo "==> Build..."
mvn clean install -q

echo "==> Arrêt de l'ancien screen '$SCREEN_NAME' si existant..."
screen -S "$SCREEN_NAME" -X quit 2>/dev/null || true

echo "==> Lancement dans screen '$SCREEN_NAME'..."
screen -dmS "$SCREEN_NAME" java -jar "$JAR"

echo "==> OK — pour attacher : screen -r $SCREEN_NAME"
