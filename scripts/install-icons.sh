#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RES="$ROOT/app/src/main/res"
B64DIR="$ROOT/scripts/icon_b64"

mkdir -p \
  "$RES/mipmap-mdpi" \
  "$RES/mipmap-hdpi" \
  "$RES/mipmap-xhdpi" \
  "$RES/mipmap-xxhdpi" \
  "$RES/mipmap-xxxhdpi" \
  "$RES/drawable"

installed=0
for pair in \
  "mipmap-mdpi/ic_launcher.png:mipmap-mdpi_ic_launcher.png.b64" \
  "mipmap-hdpi/ic_launcher.png:mipmap-hdpi_ic_launcher.png.b64" \
  "mipmap-xhdpi/ic_launcher.png:mipmap-xhdpi_ic_launcher.png.b64" \
  "mipmap-xxhdpi/ic_launcher.png:mipmap-xxhdpi_ic_launcher.png.b64" \
  "mipmap-xxxhdpi/ic_launcher.png:mipmap-xxxhdpi_ic_launcher.png.b64" \
  "drawable/ic_launcher.png:drawable_ic_launcher.png.b64" \
  "drawable/ic_launcher_foreground.png:drawable_ic_launcher_foreground.png.b64"
do
  rel="${pair%%:*}"
  name="${pair##*:}"
  if [[ -f "$B64DIR/$name" ]]; then
    base64 -d < "$B64DIR/$name" > "$RES/$rel"
    echo "wrote $RES/$rel ($(wc -c < "$RES/$rel") bytes)"
    installed=$((installed+1))
  fi
done

if [[ "$installed" -gt 0 ]]; then
  echo "Installed $installed PNG icon(s) from base64"
else
  echo "No PNG base64 found — vector fallback remains"
fi
