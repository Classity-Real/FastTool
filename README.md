# FastTool

**Material 3 Expressive fast-boot / OTG host tool — no root required.**

Native Android app that implements the original FastTool terminal concept.

## Status

- ✅ Pixel-accurate recreation of the original concept UI
- ✅ Interactive terminal with command input
- ✅ Built-in commands (`help`, `status`, `devices`, `otg`, `clear`, `version`…)
- ✅ GitHub Actions CI that builds the APK on every push
- ⏳ Real fastboot / USB host backends (next)

## Tech

| Item | Value |
|------|-------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Package | `dev.fasttool` |
| Min SDK | 26 |
| Target SDK | 35 |

## Build & Run

### Locally

1. Open the project in **Android Studio**
2. Let Gradle sync
3. Run the `app` configuration

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### CI

Every push / PR to `main` triggers a GitHub Actions workflow that builds the debug APK and uploads it as an artifact named **FastTool-debug**.

You can also trigger it manually from the Actions tab.

## Design

Faithful recreation of the original concept:

- Background `#121212`
- Terminal surface `#161616`
- Soft green status when ready, yellow while working
- Large 28dp rounded terminal panel
- 52dp circular action buttons
- Monospace terminal text

---

Made real from the original Android concept.
