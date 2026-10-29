# Android TV verification

Tested on a Hisense SmartTV 4K, Android 12/API 31, with a 32-bit ARM userspace and an updated Android System WebView. The current native build targets `armeabi-v7a`; other device families and ABIs have not been verified.

## Launcher behavior

- CleanFrame 1.1.0 runs as an opaque, standalone Home activity. It does not require another launcher underneath it.
- The installed catalog contained 25 apps. Dock and All apps rendered white vectors without colored app tiles. Prototype app definitions/pins are absent until discovery completes; an uninstalled Viki entry cannot flash during startup.
- Dock, gear, group and focus geometry matched the upstream HTML at 1920×1080. Browser checks passed D-pad, OK press/release, hold-OK editing, move/pin/unpin persistence, an intentionally empty dock, native settings/input actions and preview deep links.
- Up opens the full-screen continue-watching layout with Play/Open, position dots and Left/Right selection. Down/Back returns to the dock. App previews rotate while that shelf is inactive.
- Physical YouTube, Prime, Disney, star and settings keys were exercised on the test remote. Kid is intentionally disabled. The physical YouTube shortcut was also confirmed from system Settings after a data reset. Synthetic ADB key injection did not reproduce every hardware-key path.

## UHD rendering and synchronization

- The panel is 3840×2160, but the firmware caps its normal UI at 1920×1080. Native decoder output was independently observed at 3840×2160. Four cached clips copied from the TV passed strict FFmpeg decoding without errors; each contains an initial I frame and a second P frame.
- One persistent decoder and surface display successive still images. Four-byte AVC delimiter normalization prevents the Android MP4 writer from truncating x264 slices. Recreating or looping a player per image is not used.
- Dock and label artwork commit from the frame-presented callback. Two navigation passes across seven pinned apps verified matching presented/background and glass-artwork keys. On the second pass, six apps reported callbacks in 171–244 ms and Netflix in 516 ms. First-visit requests varied from 244 to 3,643 ms, including network/encoding work. These are short device checks, not a sustained benchmark or a claim that unseen artwork loads instantly.
- Input images varied by publisher: some supplied 3840×2160 artwork, while sampled YouTube and Netflix images were 1280×720 and 853×480. UHD output does not recover source detail that the publisher does not supply.

## Weather and reset recovery

- The header displayed an 80px clock with AM/PM and live San Francisco weather in Celsius. Weather uses configured coordinates rather than a device-location permission.
- An actual Clear Data reset was performed after installing 1.1.0. The APK and Home selection remained; the six-app packaged profile and weather/clock defaults returned.
- The app displayed Android's TV-listings permission request and remote-button setup flow. Access recovered through native Android UI, without an ADB permission grant or secure-settings write. Eleven app preview sources returned, the key-filter service was enabled/bound, and the physical YouTube shortcut worked from Settings.
- Clear Data still erases subsequent custom preferences and Android grants. The app restores its bundled baseline and guides setup; it does not bypass Android's permission requirements.

## Optional Astra companion patch

- The patched app published 20 event previews with 3840×2160 artwork. Event links opened feed choices with remote focus, and Watch Next resumed the selected feed directly.
- Live playback reached Android PlaybackState 3 with an advancing position, an active Media3 session and KEEP_SCREEN_ON. Pause reached state 2 and released the screen-awake flag. Leaving the player disposed the session. No global screensaver timeout changed.
- A full unattended ambient-timeout test was not performed. Account data and signing material are local configuration and are not part of this repository.

## Automated checks

- Launcher APK build, all 11 Android unit tests and Android lint passed.
- Six JavaScript catalog/header tests and the browser interaction/geometry checks passed.
- The separate Astra release build, tests and lint passed with the companion patch.
- The installed APK signature and packaged profile/HTML assets were inspected. APKs, device backups and UI captures are excluded from version control.
