# FastTool

**Material 3 Expressive fast-boot / OTG host tool — no root required.**

Native Android app that implements the original FastTool terminal concept.

## Status

- ✅ Pixel-accurate recreation of the original concept UI
- ✅ Interactive terminal with command input + history feel
- ✅ Built-in commands (`help`, `status`, `devices`, `otg`, `clear`, `version`…)
- ⏳ Real fastboot / USB host backends (next)

## Tech

| Item | Value |
|------|-------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Min SDK | 26 |
| Target SDK | 35 |
| Package | `com.classity.fasttool` |

## Build & Run

1. Open the project in **Android Studio** (Koala / Ladybug or newer recommended)
2. Let Gradle sync
3. Run the `app` configuration on a device or emulator

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Design

Faithful recreation of the original concept:

- Background `#121212`
- Terminal surface `#161616`
- Soft green status when ready, yellow while working
- Large 28dp rounded terminal panel
- 52dp circular action buttons
- Monospace terminal text

The UI is intentionally restrained so it feels like a real device tool rather than a flashy terminal skin.

## Next steps

- USB Host / fastboot protocol integration
- Real device detection
- Command profiles / templates (the download button)
- Persistent session history

---

Made real from the original Android concept.
