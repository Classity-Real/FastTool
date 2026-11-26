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

# 1) Preferred: all_icons.json (single file with every density)
if [[ -f "$B64DIR/all_icons.json" ]]; then
  python3 - "$ROOT" <<'PY'
import base64, json, sys
from pathlib import Path
root = Path(sys.argv[1])
res = root / "app/src/main/res"
data = json.loads((root / "scripts/icon_b64/all_icons.json").read_text())
mapping = {
  "mipmap-mdpi_ic_launcher.png.b64": "mipmap-mdpi/ic_launcher.png",
  "mipmap-hdpi_ic_launcher.png.b64": "mipmap-hdpi/ic_launcher.png",
  "mipmap-xhdpi_ic_launcher.png.b64": "mipmap-xhdpi/ic_launcher.png",
  "mipmap-xxhdpi_ic_launcher.png.b64": "mipmap-xxhdpi/ic_launcher.png",
  "mipmap-xxxhdpi_ic_launcher.png.b64": "mipmap-xxxhdpi/ic_launcher.png",
  "drawable_ic_launcher.png.b64": "drawable/ic_launcher.png",
  "drawable_ic_launcher_foreground.png.b64": "drawable/ic_launcher_foreground.png",
}
for key, rel in mapping.items():
    if key not in data:
        continue
    out = res / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(base64.b64decode(data[key]))
    print(f"wrote {out} ({out.stat().st_size} bytes)")
print("Icons installed from all_icons.json")
PY
  exit 0
fi

# 2) Individual .b64 files
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
    echo "wrote $RES/$rel"
    installed=$((installed+1))
  fi
done

if [[ "$installed" -gt 0 ]]; then
  echo "Installed $installed icon file(s) from individual base64"
  exit 0
fi

echo "No PNG icon base64 found — using vector drawable fallback (already in repo)"
exit 0
