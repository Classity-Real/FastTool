# FastTool

**Material 3 Expressive fastboot tool — no root required.**

Native Android app for flashing devices over OTG using real fastboot.

## Features

- Live USB host detection (attach / detach / permission)
- Flash templates:
  - **Boot + Userdata**
  - **Boot + Userdata + Cache**
- Select images per partition → Flash
- Bundled `fastboot` binary runner (extract + exec)
- Terminal UI matching the original concept

## Fastboot binary (required)

The app expects a static fastboot binary in assets:

```
app/src/main/assets/fastboot
```

Arm64 is recommended. You can also name it `fastboot-arm64-v8a`.

On first use it is extracted to the app private directory and executed as:

```text
fastboot flash <partition> <image>
```

Get a binary from Android platform-tools or an AOSP build of `system/core/fastboot`.

## Build

```bash
./gradlew assembleDebug
```

Or use the GitHub Actions workflow — it uploads the APK as **FastTool-debug**.

## Package

`dev.fasttool`

## Status

| Piece | State |
|-------|--------|
| UI | Done |
| Templates | Done |
| USB detection | Done |
| fastboot exec path | Done |
| Bundled binary | **You must add `assets/fastboot`** |
| Full protocol over USB bulk | Optional alternative later |

---

Made real from the original Android concept.
