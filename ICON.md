# Launcher icon (your USB PNG)

Android supports PNG icons fine. The GitHub text API used here can't push binary PNGs directly, so use one of these:

## Option A — Drop PNGs in the repo (simplest)

Add your USB icon as:

```
app/src/main/res/mipmap-mdpi/ic_launcher.png      (48×48)
app/src/main/res/mipmap-hdpi/ic_launcher.png      (72×72)
app/src/main/res/mipmap-xhdpi/ic_launcher.png     (96×96)
app/src/main/res/mipmap-xxhdpi/ic_launcher.png    (144×144)
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png   (192×192)
app/src/main/res/drawable/ic_launcher_foreground.png  (432×432, for adaptive)
```

Manifest already points at `@mipmap/ic_launcher`.

You can upload via the GitHub web UI (Add file → Upload).

## Option B — Base64 for CI

Put `scripts/icon_b64/all_icons.json` (generated from your PNG) and CI will decode it before the build.

```bash
# local helper after you have density PNGs in artifacts/icons
python3 - <<'PY'
import base64, json
from pathlib import Path
base = Path('path/to/icons')
data = {}
for rel in [
  'mipmap-mdpi/ic_launcher.png',
  'mipmap-hdpi/ic_launcher.png',
  'mipmap-xhdpi/ic_launcher.png',
  'mipmap-xxhdpi/ic_launcher.png',
  'mipmap-xxxhdpi/ic_launcher.png',
  'drawable/ic_launcher.png',
  'drawable/ic_launcher_foreground.png',
]:
  p = base / rel
  if p.exists():
    data[rel.replace('/','_') + '.b64'] = base64.b64encode(p.read_bytes()).decode()
Path('scripts/icon_b64/all_icons.json').write_text(json.dumps(data))
print('wrote scripts/icon_b64/all_icons.json')
PY
```

Until PNGs are present, the vector USB icon is used as fallback.
