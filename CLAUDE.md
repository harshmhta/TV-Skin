# CleanFrame — Samsung TV custom launcher (project guide)

A self-made, calm home screen for the owner's **own** Samsung TV (model **<your-TV-model>**, Tizen 6.0, 4K panel). Replaces Samsung's cluttered Smart Hub with a curated full-screen artwork background + a clean app dock. Personal use, fully reversible.

## Where things are
- `prototype/index.html` — the launcher UI + motion (built from `proto.template.html` via `proto.py`). **Source of truth for behaviour; do not fork it — the device build injects on top.**
- `launcher-app/CleanFrame/` — the Tizen web-app project (signed, installable).
- `launcher-app/device-build/` — the device layer injected into the prototype for the TV:
  - `device.css` — fullscreen + hide dev harness + **Chromium-76 fixes** + look tweaks.
  - `device_shim.html` — TV remote keys, real `tizen.application.launch`, polyfills, faked-glass enable, instant label, calm art cadence, no edit-hint.
  - `device_diag_DEBUG.html` — debug only (POSTs to Mac :8099, network-remote probe). **Strip for production.**
  - `build.sh` — reproduces the build and installs. `./build.sh` = clean; `./build.sh debug` = with diag.
  - `CleanFrame-signed.wgt` — last signed package.
- `reference/art/` — bundled artwork (greatwave/irises/wheatfield .jpg). Copied into `launcher-app/CleanFrame/art/`.
- `HANDOFF-CLAUDE.md` — current narrative handoff + next steps.
- `HOME-BUTTON.md` — root + Home-route research (**deepseek's** scope).
- `CLEANUP.md` — every Mac/TV change and how to undo it.

## Toolchain (on the Mac, all removable — see CLEANUP.md)
- `~/tizen-tools/jdk17` (Temurin 17 x64), `~/tizen-studio` + `~/tizen-studio-data` (Tizen Studio 6.1 + Certificate Manager + Samsung Certificate Extension + TV extensions).
- `tizen` CLI: `~/tizen-studio/tools/ide/bin/tizen` (needs `JAVA_HOME=~/tizen-tools/jdk17/Contents/Home`).
- Signing profile **`<your-cert-profile>`** (certs in `~/SamsungCertificate/<your-cert-profile>/`, distributor bound to TV **DUID <your-TV-DUID>**).
- `sdb`: `~/tizen-tools/sdb/data/tools/sdb` (also `~/tizen-studio/tools/sdb`). TV at **<your-TV-IP>**, Mac at **<your-Mac-IP>**.
- Temp root (volatile): `samsung-tv-root` v0.0.12 at `$TMPDIR/stvroot` — `archive-root root <your-TV-IP> --command '...'`.

## Build & install
```
cd ~/Desktop/SamsungTVLauncher/launcher-app/device-build && ./build.sh         # clean build + install
~/tizen-studio/tools/sdb shell 0 was_execute YOURPKGID0.CleanFrame     # launch (0 debug is blocked)
```
App id: **`YOURPKGID0.CleanFrame`** (package `YOURPKGID0`). Uninstall: `pkgcmd -u -n YOURPKGID0` as root (plain `tizen uninstall` is flaky here).

## Hard-won facts (don't rediscover)
- **Install must be signed.** Samsung `wascmd` needs a **10-char alphanumeric package id** (dotted ids → `install failed[118,-19] Parsing error`). Use a tool-generated id (`tizen create web-project -p tv-samsung-6.0`). Root/DB-surgery installs never launch. `tizen install` (not `sdb install` alone) does the on-device install.
- **TV is Chromium 76.** The prototype is modern-Chrome, so these needed fixing: `inset` shorthand (→ explicit T/R/B/L), flex `gap` (→ margin fallback via `.no-flex-gap`), `backdrop-filter` renders **tint-only** (→ faked glass `setRender('tv')` + `clip-path` for rounded corners since border-radius won't clip a transformed child), `Element.getAnimations` (polyfill), `aspect-ratio` (override). No `?.`/`??`/`:is()`/`replaceAll`/`.at()`.
- **UI renders at 1080p and the 4K panel upscales it** — platform limit, can't render web UI at native 4K.
- **Sound output & HDMI-input switching are NOT possible from the app.** No Tizen API. The network-remote (WebSocket 8001/8002) **never authorizes** app-initiated connections on this set — 14/14 `ms.channel.timeOut`, no Allow popup. Fallback = app-control deep-links to Samsung's Source/Sound menus (one press) + HDMI-ARC/Anynet+ for the soundbar.
- Memory notes: `<claude-memory-dir>` (signed-install-breakthrough, launcher-device-port).

## Style / working agreements
- Iterate on the chosen design in place; don't spin up new concepts. Be honest about verified vs unverified. Keep CLEANUP.md current. The TV is 4K but the web layer is FHD.
