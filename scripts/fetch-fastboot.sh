#!/usr/bin/env bash
# Download arm64 fastboot as libfastboot.so for jniLibs
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JNI="$ROOT/app/src/main/jniLibs/arm64-v8a"
ASSETS="$ROOT/app/src/main/assets"
mkdir -p "$JNI" "$ASSETS"
URL="https://github.com/rendiix/termux-adb-fastboot/raw/master/binary/arm64-v8a/bin/fastboot"
echo "Downloading fastboot (arm64-v8a) → libfastboot.so…"
curl -fsSL -o "$JNI/libfastboot.so" "$URL"
cp "$JNI/libfastboot.so" "$ASSETS/fastboot"
chmod +x "$JNI/libfastboot.so"
ls -la "$JNI/libfastboot.so"
echo "Done."
