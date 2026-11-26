#!/usr/bin/env bash
# Download arm64 fastboot binary for local builds
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/app/src/main/assets"
mkdir -p "$OUT"
URL="https://github.com/rendiix/termux-adb-fastboot/raw/master/binary/arm64-v8a/bin/fastboot"
echo "Downloading fastboot (arm64-v8a)…"
curl -fsSL -o "$OUT/fastboot" "$URL"
chmod +x "$OUT/fastboot"
ls -la "$OUT/fastboot"
echo "Done → $OUT/fastboot"
