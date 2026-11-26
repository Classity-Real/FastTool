#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
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
    out = res / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(base64.b64decode(data[key]))
    print(f"wrote {out} ({out.stat().st_size} bytes)")
print("All launcher PNGs installed.")
PY
