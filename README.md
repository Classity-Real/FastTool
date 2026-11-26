# FastTool

**Material 3 Expressive fastboot tool — no root required.**

Native Android app for flashing devices over OTG using real fastboot.

## Features

- Live USB host detection
- Flash templates:
  - **Boot + Userdata**
  - **Boot + Userdata + Cache**
- Bundled arm64 `fastboot` binary (auto-downloaded in CI)
- Terminal UI from the original concept

## Fastboot binary

Source used by CI:

https://github.com/rendiix/termux-adb-fastboot/blob/master/binary/arm64-v8a/bin/fastboot

**Local build:**

```bash
chmod +x scripts/fetch-fastboot.sh
./scripts/fetch-fastboot.sh
./gradlew assembleDebug
```

**CI:** downloads the binary into `app/src/main/assets/fastboot` before building.

## Build

```bash
./scripts/fetch-fastboot.sh   # once
./gradlew assembleDebug
```

Or push to `main` and download the **FastTool-debug** artifact from Actions.

## Package

`dev.fasttool`

---

Made real from the original Android concept.
