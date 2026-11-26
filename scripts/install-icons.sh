#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RES="$ROOT/app/src/main/res"
B64DIR="$(cd "$(dirname "$0")" && pwd)/icon_b64"

decode() {
  local rel="$1" name="$2"
  local dir="$RES/$(dirname "$rel")"
  mkdir -p "$dir"
  base64 -d < "$B64DIR/$name" > "$RES/$rel"
  echo "wrote $RES/$rel ($(wc -c < "$RES/$rel") bytes)"
}

decode "mipmap-mdpi/ic_launcher.png" "mipmap-mdpi_ic_launcher.png.b64"
decode "mipmap-hdpi/ic_launcher.png" "mipmap-hdpi_ic_launcher.png.b64"
decode "mipmap-xhdpi/ic_launcher.png" "mipmap-xhdpi_ic_launcher.png.b64"
decode "mipmap-xxhdpi/ic_launcher.png" "mipmap-xxhdpi_ic_launcher.png.b64"
decode "mipmap-xxxhdpi/ic_launcher.png" "mipmap-xxxhdpi_ic_launcher.png.b64"
decode "drawable/ic_launcher.png" "drawable_ic_launcher.png.b64"
decode "drawable/ic_launcher_foreground.png" "drawable_ic_launcher_foreground.png.b64"
echo "All launcher PNGs installed."
