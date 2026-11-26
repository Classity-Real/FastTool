# FastTool

**Material 3 Expressive fast-boot / OTG host tool — no root required.**

Native Android app that implements the original FastTool terminal concept.

## Status

- ✅ Pixel-perfect recreation of the original concept UI
- ✅ Interactive terminal with command input
- ✅ Built-in commands (`help`, `status`, `devices`, `otg`, `clear`, `version`…)
- ⏳ Real fastboot / OTG / WebUSB-style backends (next)

## Tech

- Kotlin
- Jetpack Compose
- Material 3
- Min SDK 26 / Target SDK 35

## Build

Open the project in **Android Studio** (Hedgehog or newer recommended) and run the `app` configuration.

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Design

The UI deliberately matches the original concept:

- Dark surfaces (`#121212` / `#161616`)
- Status indicator for OTG host state
- Large rounded terminal panel
- Thumb-friendly command bar + circular run button
- JetBrains Mono for the terminal

---

Made real from the original concept.
