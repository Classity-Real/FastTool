Place a static Linux/Android fastboot binary here.

Required file:
  app/src/main/assets/fastboot

Recommended:
  - Architecture: arm64-v8a (aarch64)
  - Static or with minimal shared lib dependencies
  - From AOSP platform-tools / system/core fastboot

Optional ABI-specific names the runner also checks:
  fastboot-arm64-v8a
  fastboot-armeabi-v7a

On first flash the binary is copied to the app's private files directory
and marked executable, then invoked as:

  fastboot flash <partition> <image>

You can obtain a binary from:
  - Android platform-tools (extract the 'fastboot' binary for your ABI)
  - Building AOSP system/core/fastboot for android-arm64
  - Trusted third-party static builds used by other open-source Android tools

Without this file, flash commands will fail with a clear error.
