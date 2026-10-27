# Hand-off — CleanFrame launcher (state as of 2026-10-27, end of day)

Written by Claude (Opus) after a full session building + installing the real launcher. The owner is done for the day; **both apps were uninstalled and the TV is back to stock.** Nothing is left running on the TV. See `CLAUDE.md` for the project map and build commands.

## What got done this session

1. **Signed-install path solved end to end.** Built the Tizen toolchain (Tizen Studio 6.1 + Samsung cert `<your-cert-profile>` bound to DUID <your-TV-DUID>), learned that Samsung requires a **10-char alphanumeric package id** (dotted ids → `[118,-19] Parsing error`), and that `tizen install` (not `sdb install`) does the real install. The earlier root/DB-surgery install was a dead end (wrt needs a signed widget) and was **rolled back**.
2. **Test probe** (`YOURPKGID1.CFTest`) confirmed on-device: real `backdrop-filter` is **tint-only** (needs faked glass), `getAppsInfo()` works (400 apps), `launchAppControl()` works (Prime opened), FHD render upscaled to 4K. Probe since uninstalled.
3. **Real launcher built + installed** as `YOURPKGID0.CleanFrame` — the prototype ported to the TV via an injected device layer (`launcher-app/device-build/`). Working on-device: curated artwork with Ken-Burns zoom + 2-min crossfade, frosted-glass dock (faked), clean selection, all-apps grid, sound panel UI, real app launching for the dock apps.
4. **Chromium-76 compatibility swept** (the source of most visual bugs): `inset`, flex `gap`, faked glass + `clip-path` rounding, `getAnimations` polyfill, `aspect-ratio`, viewport meta. Animations retuned per owner (gentle but snappy; instant label; no unpin left-slide; no edit hint; gentle dock-entry on all-apps close).
5. **Sound output + HDMI-input switching investigated thoroughly and found blocked.** No Tizen API. The network-remote (handoff §B) **never authorizes** app connections on this TV: 14/14 `ms.channel.timeOut`, no Allow popup, tested external (Mac) and internal (launcher localhost 8001/8002). This is a firmware limitation, not a missing piece. 

## Verified real app-launch ids (for the dock)
youtube=`9Ur5IzDKqV.TizenYouTube` · netflix=`org.tizen.netflix-app` · xbox app=`GHI3a0zMSx.XboxGamePass` (but owner wants Xbox tile = HDMI input, not the app) · viki=`q394VGbAL0.RakutenViki` · prime=`org.tizen.primevideo` · disney=`MCmYXNxgcu.DisneyPlus` · tubi=`3KA0pm7a7V.TubiTV` · kocowa=`79p1eXqt51.KOCOWA2`.

## Open items / next steps (for whoever picks this up)
- **Home-button takeover → deepseek.** Hand it `YOURPKGID0.CleanFrame`. Install the launcher (`device-build/build.sh`), then flip the `XF86Home` `key_filter` row per `HOME-BUTTON.md`. Nothing on the Home route was touched this session.
- **Sound / input (owner's call on direction):** full in-app automation is impossible here. Pragmatic path = app-control **deep-links** to Samsung's Source picker (Xbox→HDMI) and Sound Output settings (one press), + **HDMI-ARC/Anynet+** to stop the soundbar reverting. The Xbox tile currently shows a placeholder toast (no longer launches the Xbox app). Need to find the Samsung settings deep-link app-control operations and wire them.
- **Production cleanup before shipping:** `build.sh` (clean mode) already omits the debug diag; also remove the `internet` privilege + `<access>` from `config.xml` (only needed for the debug POST). Pick a final art cadence (currently 2 min; `ART_HOLD`/`ZOOM` are build-time replaces).
- **Known ceiling:** UI is 1080p upscaled to the 4K panel — can't be native 4K (platform limit).
- **Polish still possible:** exact match to Figma (`<your-Figma-file-key>`, Home frame `4:2`), label/motion feel on the real SoC.

## State of the TV / Mac right now
- **TV:** stock. No launcher, no probe, Home route untouched, Dev Mode still on. Device List has some "Smart Device (Allowed)" entries from the remote-pairing tests (harmless). Access Notification was changed to "First Time Only" (was Off) — revert if desired.
- **Mac:** toolchain + cert + project all intact under `~/Desktop/SamsungTVLauncher/`, `~/tizen-tools`, `~/tizen-studio*`, `~/SamsungCertificate`. Signed wgt at `launcher-app/device-build/CleanFrame-signed.wgt`. Full removal steps in `CLEANUP.md`.
