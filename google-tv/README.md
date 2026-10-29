# CleanFrame for Google TV

An Android port of AtharvaN16/TV-Skin. The APK uses the upstream HTML itself, with the original 1920×1080 stage, Manrope font, floating glass dock, highlight, labels, app grid and hold-OK editing. `build_web.py` injects Android behavior into `../prototype/index.html`; it does not maintain a second copy of the layout.

## Differences from the Tizen implementation

- Android APK and a standalone Home activity replace the signed Tizen widget and Tizen-specific Home routing. The existing Tizen source is retained in `launcher-app/`.
- Android's installed-app catalog, intents and TV provider replace the Tizen application APIs. The app dock contains apps, with an icon-only Inputs utility opening the TV's native input picker. Both gear buttons open native system settings.
- App-published Watch Next and preview channels replace the public-domain artwork slideshow. Artwork rotates, and Up opens full-screen continue-watching choices with Play/Open, position dots and publisher deep links. All apps has a bundled stock background.
- The HTML controls retain the original stage and geometry; a persistent native 3840×2160 decoder renders artwork beneath them on the tested Hisense. Dock/label glass updates when that frame is presented. This addresses the TV's 1080p UI limit without claiming that low-resolution source images contain 4K detail.
- White vectors cover the installed app catalog. The header adds the larger AM/PM clock and weather. The packaged reset profile contains this implementation's preferred apps and San Francisco/Celsius defaults and can be edited before building.
- An optional key-filter Accessibility service restores Hisense app shortcuts outside the launcher. Kid is disabled in this implementation. The service cannot retrieve window contents. Key mappings remain device-specific.
- Runtime permission/setup recovery is part of the APK. Clear Data restores the packaged profile and requests the Android access again; it does not require installation-time ADB permission grants.
- `astra-1.0.2.patch` is an optional companion change for a separate app: event previews and feed-resume links, sharper star/name updates, and playback-only screen-awake handling. It contains no account or signing configuration.

The app is an opaque, standalone Android Home activity. It discovers installed TV apps and persists the dock. Its app list starts empty until Android supplies the catalog, so prototype apps never flash during startup. Other Home launchers and physical inputs are excluded from the app list. All installed apps use white vector icons in both the dock and grid. The dock's icon-only input utility opens the Hisense input picker. The on-screen gear and remote gear open the TV's normal settings directly.

The background displays content published by the focused app to Android TV's Watch Next and preview channels, rotating every 12 seconds. Up opens the previous Apple TV clone's style of continue-playing view: full-screen artwork, caption/title, Play or Open button and position dots. Left/Right browses the available titles, OK opens the publisher's deep link, and Down/Back returns to the dock. Rotation pauses while choices are open. Highlighting All apps shows the bundled 4K landscape; unavailable artwork uses the same fallback. The app requests TV listing access on first launch and after a data reset. Up opens access recovery when that permission is missing.

The header has an 80px twelve-hour clock with a smaller AM/PM label, plus San Francisco weather in Celsius. Weather comes from [Open-Meteo](https://open-meteo.com/), refreshes every fifteen minutes while Home is active, and retains its last known value for short network outages. Requests stop when the launcher pauses. It uses the chosen city's coordinates, without requesting device location. The city, units and six-app reset layout are packaged in `app/src/main/assets/profile.json`.

Clear Cache removes prepared artwork, which is regenerated; it does not remove the installed features or preferences. Clear Data intentionally deletes preferences and revokes Android access. The app now restores its packaged dock defaults and guides the user through preview access and remote-button Accessibility setup again. Android requires those access approvals after a data reset. Subsequent custom dock edits remain preferences and are erased by a later Clear Data operation. The installer no longer grants preview access through ADB.

On this Hisense, Android caps its UI layer at 1920×1080 despite the 3840×2160 panel. The background is encoded locally into a two-frame 3840×2160 AVC clip and displayed through a persistent hardware decoder and SurfaceView, below the original HTML controls. A new image replaces the decoded frame without looping, resetting the decoder or recreating the video surface. The dock and label's glass artwork changes only after the frame-presented callback. Initial startup and decoder failure use full-page HTML artwork. The outer Activity remains opaque, so it does not reveal an older launcher.

Artwork fills the screen with its aspect ratio preserved and cropping where necessary. Stremio title links select the same title's landscape background instead of enlarging a portrait poster. Service-specific image URLs request 3840×2160 renditions where supported. A 4K render surface cannot restore detail absent from an app's source image: YouTube's shared thumbnails may be 1280×720 and Netflix's may be 853×480. Astra generates its own 3840×2160 game cards. Source and actual decoder-output dimensions are logged under `CleanFrame4K`.

The encoder uses optimized native code and ARM NEON. It encodes an initial key frame and an efficient second frame, rather than two complete key frames. HTTPS artwork caches by source URL across changing program-row IDs; content-provider artwork includes the program ID so republished event information is refreshed. The cache is capped at 32 clips. The neighboring app and next rotating preview are prepared after focus settles; new focus cancels stale work. Image work runs at background priority, and glass clipping changes only when its bounds change.

The standard D-pad, OK press/release, hold OK, Back, Menu and media keys work natively. Enable the optional `CleanFrame app buttons` accessibility service to use the Hisense app shortcuts across other apps. It filters key events and cannot read window contents. Hisense keys 190/293 open YouTube, 295 opens Prime, 363/607 open Disney+, 83/82/176 open native TV settings, and 172 opens Live TV. Kid key 1593 is disabled in this implementation. These mappings bypass the OEM handler that forwards requests to the disabled Google launcher. Physical YouTube, Prime, Disney, star and settings presses were verified on the test TV. Power, volume, voice, input and navigation keys retain their normal handlers.

## Build and check

Requires JDK 17, Android SDK 36, NDK 27.1.12297006, CMake 3.22.1 and Python 3. The bundled Gradle wrapper downloads Gradle 8.14.6. On macOS, the build downloads the pinned x264 source and builds the preview encoder for this TV's 32-bit ARM ABI. The revision is recorded in `native/x264-version.txt`, and its GPL license is bundled in the APK.

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
npm ci
npx playwright install chromium
npm test
```

Set `PLAYWRIGHT_CHROMIUM_EXECUTABLE` if using an existing Chromium installation. The browser checks compare the dock, gear, group and focus highlight geometry with the upstream HTML, then check the intentionally enlarged clock, weather, D-pad, launching, pin/reorder/unpin persistence, previews and missing content.

`tests/check_uhd.py CLIP.mp4 ...` checks TV-generated cached clips with ffprobe and a strict independent FFmpeg decode. The clips are in the app's private `cache/uhd-previews-v5` directory. Android 12's MP4 writer assumes four-byte AVC start codes between slices; `AvcSamples` normalizes x264's three-byte delimiters to prevent truncated slices and colored static. Unit tests cover the boundary and escaped payload data. Browser checks hold back the native frame-presented callback to verify that dock/label artwork cannot advance ahead of the UHD background.

## Install

Authorize network debugging on the TV first. The installer saves the previous home and disabled-package list without disabling any system packages.

```sh
adb connect YOUR_TV_IP:5555
TV_SERIAL=YOUR_TV_IP:5555 ./install.sh
```

After checking the app on screen, set it as Home:

```sh
adb -s YOUR_TV_IP:5555 shell cmd package set-home-activity dev.cleanframe.tv/.MainActivity
```

`SET_HOME=1` also sets Home during installation. `ADB_PORT` selects a separate local ADB server if needed.

The installer retains a record of the previous Home in `backup/<timestamp>/home.txt`. To roll back, re-enable that package if it was separately disabled, set its recorded activity as Home, switch off the CleanFrame Accessibility service and uninstall `dev.cleanframe.tv`. Disabling a previous launcher is a separate device configuration choice, not an installer requirement.

## Astra

`astra-1.0.2.patch` updates the separate `harshmhta/astra-tv` app. It adds a Media3 session and holds the player view awake only during requested READY or BUFFERING playback. Pause, idle, end, errors and leaving the player release the flag. It renames the app to Astra, sharpens the star, publishes actual event previews with locally generated 3840×2160 artwork, and publishes a continue-watching link for the selected feed. Preview links open event/feed choices; continue-watching links resume the exact feed. The signed 1.0.2 APK must match the installed signing certificate; use `adb install -r` to retain settings. The original 1.0.0 signing certificate was compared with the new build and matches. No global screensaver setting is changed.

See `VERIFIED.md` for the actual Hisense installation and tests, including physical app buttons and Astra's playing/paused screen-awake state. An unattended run for the full ambient timeout was not performed.
