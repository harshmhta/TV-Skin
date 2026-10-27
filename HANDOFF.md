# Samsung TV custom launcher — hand-off

> **2026-10-27 — read `HANDOFF-CLAUDE.md` first.** It supersedes §6A for the
> immediate next step: a test app is half-installed on the TV, the manual install
> recipe is documented, and the Home-button redirect is explicitly deferred to
> deepseek. Project files now live at `~/SamsungTVLauncher/`.

Project by the TV's owner (personal use, own device). This doc lets another agent continue.
Date: 2026-10-26. Last updated by **deepseek** after confirming temporary root and mapping the
Home-button route — see `HOME-BUTTON.md` and §6C.

---

## 1. Goal

Replace the cluttered Samsung Smart Hub experience with a calm, self-made home screen
("launcher") on the owner's own TV: fewer ads, faster, a curated full-screen artwork
background, and a clean app dock. Everything reversible.

## 2. The TV (confirmed facts)

- Model: **<your-TV-model>** (<your-TV-model>). Platform codename `<platform-codename>`.
- **Tizen 6.0** (2021 software — newer than the 2020 Tizen 5.5 first assumed).
- On LAN at **<your-TV-IP>**, wireless MAC `<your-TV-MAC>`. Owner's Mac: **<your-Mac-IP>**.
- **Developer Mode: ON** and persistent (confirmed `developerMode:1`, `developerIP:<your-Mac-IP>`).
- Enabling Dev Mode on this TV: Home → Apps → type `12345` → On + Host PC IP <your-Mac-IP> →
  OK → **unplug 30 s** (a standby restart did NOT save it; a full power-cycle did).

### What `sdb` reports (read-only `capability`)
- `can_launch:tv-samsung` → **signed .wgt apps can be installed and launched.** ✅
- `platform_version:6.0`
- `intershell_support:disabled`, `sdb shell` returns empty/"closed" → **no developer shell.**
- `sdbd_rootperm:disabled`, `rootonoff_support:disabled`, `appcmd_support:disabled`,
  `secure_protocol:enabled` → root/shell paths are closed on this retail firmware.
- `cpu_arch:armv7`. Browser engine on Tizen 6.0 supports `backdrop-filter` per Samsung specs
  (unverified on this exact set) → real frosted glass **may** work, which would retire the
  "faked glass" fallback. **Test with a tiny app before committing.**

## 3. Design — decided

Direction: **"Home · Compact dock"** over a full-screen curated artwork.

- **Dock**: default order YouTube · Netflix · Xbox · Viki · Prime Video, then `All apps` | `Sound`.
  Nothing locked — every app can be moved/unpinned (hold OK → edit: ◀▶ move, ▲+OK unpin,
  OK/Back done). New pins append at the end. Max 7 apps. Inputs (TV, Xbox, Expansion USB,
  PC on TV) behave like apps.
- **Label**: app-name pill sits ABOVE the focused app, hidden on launch, appears on first
  move, fades after 5 s idle. Animation = **odometer roll** (full-height, 240 ms, no blur).
- **Sound panel** (opens above the Sound dock item): "Play sound on" → Soundbar / TV speaker /
  Samsung sound settings. Single-line rows, 520px, no subheadings, NO active checkmark
  (Samsung doesn't expose which output is active).
- **All apps**: grid of every installed app + Sources & Sound row. Hold OK on a tile to pin.
  Focused tile = white 32% fill + 2.4px inner white stroke + background-blur 80.
- **Settings gear**: top-right, frosted circle.
- **Artwork**: curated **bundled** set (~47 approved, target ~60). 2 min per piece, 4.5% slow
  centre zoom, blur-out/swap/blur-in transition. Day/dusk/night moods (could switch by time).
  Sources: Met, Art Institute of Chicago, Cleveland, Rijksmuseum (all CC0/public domain).
  **MoMA excluded** (images not open).

### Critical rendering rules (TV is Chromium 69-era on 5.5; 6.0 is newer but still constrained)
- Glass = either real `backdrop-filter` (if 6.0 supports it on this set) OR faked: a lined-up
  piece of a **pre-blurred** copy of the artwork. Decide via test app.
- Blurred art copies must be **tiny pre-blurred images (≈320×180) stretched**, never full-size
  images with live CSS blur — Chrome/TV silently drop those under GPU-memory pressure
  (this caused intermittent "no blur in All apps").
- **Never fade a parent of a `backdrop-filter` element** — the browser drops the blur while the
  ancestor is translucent. Fade the frosted element itself.
- Animate transform/opacity only. No `??` / `?.` (older engine).

## 4. Files / assets

- **Figma** (live, editable): `<your-Figma-file-key>` — frames: Home (`4:2`), Sound expanded
  (`8:10`), plus earlier Home/Sound/All-apps frames.
- **Interactive prototype** (the source of truth for behaviour + motion):
  `prototype/index.html` (+ `art_engine.js`, built from `proto.template.html` via `proto.py`).
  Open in a Chromium browser. Controls row includes: scenarios (headphones/long names/
  unplugged/missing icon), label animation, TV-accurate render toggle, art timing, and a
  **Run self-test** button.
- **Prototype test tooling** (real-time, via Chrome DevTools): `prototype/tools/*.mjs`
  (`cdp_selftest.mjs`, `cdp_races.mjs`, `cdp_shots.mjs`). IMPORTANT: test in REAL time —
  headless `--virtual-time-budget` does NOT advance CSS transitions and gives false results.
- **Artwork shortlist**: `reference/artworks-candidates.json` + `reference/artworks-contact-sheet.html`.
  Curation scripts in `/tmp/tvbuild-scripts/` (`curate.py`, `discover_night.py`, `update_set.py`).
  NOTE: the **Met search API is retired (HTTP 410)** — resolve Met object IDs via Wikidata
  property `P3634`, then the Met objects API.
- **Reference images/icons**: `reference/art/`, `reference/icons/`, `reference/cleanframe/`.
- **Cleanup list**: `CLEANUP.md` — every Mac/TV change and how to undo it.

## 5. Environment / tools installed

- Samsung **sdb** (official): `~/tizen-tools/sdb/data/tools/sdb` (Intel x86_64 binary).
- **Rosetta 2** installed (sdb is Intel-only; Apple Silicon Mac). Not cleanly removable — idle.
- Connect: `~/tizen-tools/sdb/data/tools/sdb connect <your-TV-IP>` → device `<your-TV-model>`.
- **`samsung-tv-root` v0.0.12** (macOS arm64 release, checksum verified) — temporary root.
  Extracted at `$TMPDIR/stvroot
  samsung-tv-root-0.0.12-macos-arm64/` (temp; re-download from
  github.com/chris-ritsen/samsung-tv-root/releases if gone). Run with
  `--sdb ~/tizen-tools/sdb/data/tools/sdb`. Add the binary to the macOS firewall allow list
  if the TV callback fails.
- NOT yet installed: Java + Tizen packaging CLI + Samsung certificate (needed for the *signed*
  install path only). Certificate creation requires the **owner to sign into their Samsung
  account** — an agent must not do this for them.

## 6. Remaining work

### A. Build & install the launcher (the supported path — recommended)
1. Install Tizen CLI/packaging + Java in `~/tizen-tools` (add to CLEANUP.md).
2. Owner creates a Samsung developer certificate (Samsung account sign-in) tied to the TV's
   DUID (Menu → Support → About → Unique Device ID), author + distributor cert.
3. First real step = a **tiny test .wgt** that checks on the actual TV:
   - Does `backdrop-filter` (real glass) work on this 6.0 set? If yes, drop faked glass.
   - Can the app list installed apps via `tizen.application.getAppsInfo()` and launch them via
     `launchAppControl` (public privilege — normal cert should suffice)?
   - Does **Autorun Last App** reopen a sideloaded app at power-on?
   - Smoothness of the slow zoom + odometer label on the real SoC.
4. Build the full launcher from the prototype (HTML/CSS/JS → .wgt), sign, `sdb install`.
5. Reversibility: uninstall app, Dev Mode off, factory reset as backstop.

### B. Sound output + input switching (unverified — needs the network-remote test)
- Samsung gives apps NO API to read/set sound output. The planned approach: the launcher sends
  **remote key-presses** over Samsung's network remote (ws://TV:8001/8002) to walk
  Settings → Sound → Sound Output, or to switch HDMI/USB inputs. TV shows an **Allow** popup
  for the Mac the first time. This needs testing; it may only work from phone/Mac, not from
  the app on the TV itself. Falls back to just opening Samsung's Sound menu.
- Simpler real fix for the soundbar-reverting-to-TV-speaker problem: **HDMI ARC + Anynet+**
  if the soundbar has an ARC port.

### C. Home-button takeover — **UNBLOCKED** (see `HOME-BUTTON.md`)
- Goal: make the Home button (or power-on) open the launcher instead of Samsung's bar.
- **Temporary root (uid 0) was obtained on this TV** with the public `samsung-tv-root`
  v0.0.12 `archive-root` route (SVE-2025-50109 package-name foothold + Samsung's own
  capability-bearing `dotnet`). The earlier "root impossible" conclusion was wrong.
  Volatile only; nothing persistent is installed.
- **Home is a routing-table entry**, not firmware: `/opt/dbspace/.app-context.db`,
  `key_filter`, row `com.samsung.tv.csfs | XF86Home | launch | package:com.samsung.tv.csfs |
  entry_point:smarthub_key`. `db/appfw/home_appid` is `org.tizen.tv-viewer` here and is
  **not** the Home button on this model. `startup_appid` does not exist.
- **Remaining work is only the app install**: build the prototype as a Tizen app, then
  either sign it with a Samsung developer certificate (owner sign-in) or install it via
  root (no account). Then flip the `key_filter` row (or the app-operation `main` handler)
  and test Home, standby/wake, and cold boot.
- Backups taken: `backup/app-context.db`, `backup/home-route.txt`.
- Full detail, commands, rollback, and the `__NO_RAISE_WINDOW` gotcha: **`HOME-BUTTON.md`**.
- Public references: `samsung-tv-root` (chris-ritsen), `berteotti/custom-tv`,
  `jss8649/quiet-tizen`, CleanFrame `scripts/native-home/`.
- **Fallback if we ever lose root:** pin the launcher first in Samsung's home bar
  (Home → OK = 2 presses), trim Samsung's other tiles, and enable Autorun Last App so it opens
  at power-on.

### D. Ads/bloat cleanup (no code, reversible — not yet done)
- Settings → Support → Terms & Policy: off Viewing Information Services (ACR) + Interest-Based Ads.
- Settings → General → Smart Features: Autorun Smart Hub off, Autorun Last App on.
- DNS blocking (router or TV): block `samsungads.com`, `samsungacr.com`, `samsungadhub.com`,
  `adgrx.com`. Do NOT block `samsungcloudsolution.*` (breaks store/sign-in). YouTube ads not
  DNS-blockable. Preinstalled apps can only be hidden, not deleted.

## 7. Owner preferences (style)

- In Figma hints, key names bold: "Hold **OK**…", "**Back** to close".
- Iterate on the chosen direction in place; don't spin up new concepts/pages unless asked.
- Be honest about what's unverified vs confirmed; don't overstate.
- Everything added to Mac/TV must be removable — keep `CLEANUP.md` current.

## 8. Where this stands right now (deepseek, 2026-10-26)

- **Home-button blocker is solved in principle.** Root works, the `XF86Home` route is known,
  and the routing DB is backed up. Nothing on the TV has been changed.
- **Waiting on the owner's decision** for how to install the launcher before flipping Home:
  - **A. Root install** (recommended): package the prototype as a Tizen web app, register it
    with the root session (no Samsung account). Fastest, reversible.
  - **B. Prove the remap first**: temporarily point Home at an existing app (e.g. Xbox),
    confirm, then restore.
  - **C. Signed install**: Tizen Studio + owner's Samsung sign-in, then clean `sdb install`.
- The owner chose to **wait** on the install path for now. Do not write to the TV's package
  or routing DBs until they pick one.
- Next agent: read `HOME-BUTTON.md` first, then `backup/home-route.txt`. Re-verify root with
  `archive-root probe <your-TV-IP>` before any change (root is volatile per session).

— deepseek
