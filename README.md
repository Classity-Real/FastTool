# FastTool

**Material 3 Expressive fast-boot / OTG host tool — no root required.**

FastTool is a focused terminal-style interface for interacting with Android devices over OTG / fastboot-style workflows without needing root on the host device.

This repository currently ships the **live terminal UI** based on the original concept design. The interface is fully interactive in the browser and ready to be wired to real device backends (WebUSB, ADB bridge, native companion, etc.).

## Features (current)

- Clean dark Material-inspired terminal UI matching the original concept
- Interactive command input with Enter / Run button
- Session output with history
- Built-in commands: `help`, `status`, `clear`, `otg`, `devices`, `version`
- Responsive phone-first layout (390×844 design target) that still works on desktop
- JetBrains Mono + system UI fonts

## Quick start

Just open `index.html` in a modern browser, or serve it:

```bash
npx serve .
# or
python -m http.server 8080
```

## Roadmap

- [ ] Real OTG / fastboot backend integration
- [ ] Device detection & status polling
- [ ] Command history persistence
- [ ] Multi-session support
- [ ] Native Android companion (optional)
- [ ] Template / profile system (the download button in the UI)

## Design notes

The UI is deliberately restrained:
- Dark surfaces (`#121212` / `#161616`)
- Soft status indicator for OTG host state
- Large rounded terminal panel
- Thumb-friendly command bar

Built to feel like a serious device tool rather than a novelty terminal skin.

---

Made real from the original Claude UI concept.
