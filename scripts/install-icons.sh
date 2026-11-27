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

# Avoid duplicate resource errors (XML + PNG same name)
rm -f \
  "$RES/drawable/ic_launcher.xml" \
  "$RES/drawable/ic_launcher_foreground.xml"

installed=0

decode_one() {
  local rel="$1" name="$2"
  if [[ -f "$B64DIR/$name" ]]; then
    base64 -d < "$B64DIR/$name" > "$RES/$rel"
    echo "wrote $RES/$rel ($(wc -c < "$RES/$rel") bytes)"
    installed=$((installed+1))
  fi
}

decode_one "mipmap-mdpi/ic_launcher.png" "mipmap-mdpi_ic_launcher.png.b64"
decode_one "mipmap-hdpi/ic_launcher.png" "mipmap-hdpi_ic_launcher.png.b64"
decode_one "mipmap-xhdpi/ic_launcher.png" "mipmap-xhdpi_ic_launcher.png.b64"
decode_one "mipmap-xxhdpi/ic_launcher.png" "mipmap-xxhdpi_ic_launcher.png.b64"

if [[ -f "$B64DIR/mipmap-xxxhdpi_ic_launcher.png.b64" ]]; then
  decode_one "mipmap-xxxhdpi/ic_launcher.png" "mipmap-xxxhdpi_ic_launcher.png.b64"
elif [[ -f "$B64DIR/xxxhdpi.part0" ]]; then
  # Sort parts numerically so order is part0 part1 part2 part3
  cat "$B64DIR"/xxxhdpi.part0 "$B64DIR"/xxxhdpi.part1 "$B64DIR"/xxxhdpi.part2 "$B64DIR"/xxxhdpi.part3 \
    | base64 -d > "$RES/mipmap-xxxhdpi/ic_launcher.png"
  cp "$RES/mipmap-xxxhdpi/ic_launcher.png" "$RES/drawable/ic_launcher.png"
  echo "wrote xxxhdpi+drawable from parts ($(wc -c < "$RES/mipmap-xxxhdpi/ic_launcher.png") bytes)"
  installed=$((installed+2))
fi

decode_one "drawable/ic_launcher.png" "drawable_ic_launcher.png.b64"
decode_one "drawable/ic_launcher_foreground.png" "drawable_ic_launcher_foreground.png.b64"

if [[ ! -f "$RES/drawable/ic_launcher_foreground.png" && -f "$RES/drawable/ic_launcher.png" ]]; then
  cp "$RES/drawable/ic_launcher.png" "$RES/drawable/ic_launcher_foreground.png"
  echo "copied launcher → foreground"
elif [[ ! -f "$RES/drawable/ic_launcher_foreground.png" && -f "$RES/mipmap-xxxhdpi/ic_launcher.png" ]]; then
  cp "$RES/mipmap-xxxhdpi/ic_launcher.png" "$RES/drawable/ic_launcher_foreground.png"
  echo "copied xxxhdpi → foreground"
fi

if [[ "$installed" -gt 0 ]]; then
  echo "Installed PNG icon(s) from base64"
else
  echo "No PNG base64 found"
fi
