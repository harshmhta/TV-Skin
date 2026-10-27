# Cleanup list

Everything added to this Mac or the TV for the launcher project, and how to remove it.
Updated as we go.

## On the Mac

| What | Where | Remove with |
|---|---|---|
| Samsung `sdb` tool (official, from download.tizen.org) | `~/tizen-tools/` | `rm -rf ~/tizen-tools` |
| sdb's settings/keys (created on first run) | `~/.tizen-sdb/` or `~/.sdb/` (whichever exists) | `rm -rf ~/.tizen-sdb ~/.sdb` |
| Rosetta 2 (Apple) | system | Can't be cleanly uninstalled; it sits idle unless an Intel app runs |
| Temporary build/test files | `/tmp/tvbuild*`, `/tmp/tvref`, `/tmp/tvshots`, `/tmp/cleanframe-ref` | `rm -rf /tmp/tvbuild /tmp/tvbuild-scripts /tmp/tvref /tmp/tvshots /tmp/cleanframe-ref` (macOS also clears /tmp on restart) |
| `samsung-tv-root` v0.0.12 (temporary-root tool) | `$TMPDIR/stvroot` | `rm -rf $TMPDIR/stvroot` (temp; nothing persistent on the TV) |
| Routing backups taken before the Home remap | `backup/app-context.db`, `backup/home-route.txt` | Keep until the Home remap is confirmed stable, then delete |
| Project folder (designs, prototype, artwork list) | this folder | Keep or delete as you like |

## On the TV

| What | Remove with |
|---|---|
| Developer Mode | Home → Apps → 1 2 3 4 5 → Off → OK → unplug for 30 s |
| Remote-control permission for this Mac (if we approve the "Allow" popup) | Settings → General → External Device Manager → Device Connection Manager → Device List → remove |
| Any test app we install | Apps → highlight it → hold Enter → Delete (or via sdb) |
| Root (if we use `samsung-tv-root`) | Nothing to remove — it is volatile and ends with the session; a power-cycle clears any leftover agent |
| Home-button remap (if we flip the route) | Restore `backup/home-route.txt` with the SQL rollback in `HOME-BUTTON.md` §4 (or factory reset) |

---
## 2026-10-27 — Signed install toolchain + test app (Claude/Opus)

### Mac additions (all removable)
- `~/tizen-tools/jdk17/` — Temurin JDK 17 (x64). Remove: `rm -rf ~/tizen-tools/jdk17 ~/tizen-tools/jdk-dl`.
- `~/tizen-studio/` + `~/tizen-studio-data/` — Tizen Studio 6.1 (web-cli) + Certificate Manager + Samsung Certificate Extension + TV extensions. Remove: `rm -rf ~/tizen-studio ~/tizen-studio-data ~/.package-manager`.
- `~/tizen-tools/tizen-installer.bin` — installer (can delete).
- `~/SamsungCertificate/<your-cert-profile>/` — author + distributor certs (DUID <your-TV-DUID>). KEEP if you want to update/re-sign; else `rm -rf ~/SamsungCertificate`.
- Certificate profile `<your-cert-profile>` in `~/tizen-studio-data/profile/profiles.xml`.

### TV changes
- Manual DB-surgery install of `com.example.cleanframetest` was ROLLED BACK (gone).
- **Signed app installed the supported way:** package `YOURPKGID1`, appid `YOURPKGID1.CFTest` ("CF Test").
  - Remove: `tizen uninstall -p YOURPKGID1` (or Samsung menu), no root needed.

### KEY LEARNING — why the root/DB install never launched
- Samsung TV `wascmd` **requires a 10-char alphanumeric package id**. `com.example.cleanframetest` (dotted/26-char) → `install failed[118,-19] Parsing error`. A tool-generated id (`YOURPKGID1`) installs cleanly.
- The signed path (Samsung cert w/ DUID → `tizen package` → `tizen install`) is the ONLY one that produces a launchable web app. Root/DB injection can't (wrt needs the signed widget layout). CleanFrame uses the same signed path.

### Verdicts (all confirmed on-device 2026-10-27)
- backdrop-filter (real glass): WORKS → drop faked-glass fallback.
- getAppsInfo(): WORKS (400 apps).
- launchAppControl(): WORKS (Prime Video opened on ENTER).
- Smoothness: OK (min 109 fps; one ~600ms startup frame).

---
## 2026-10-27 end of day
- **TV: back to stock.** Uninstalled `YOURPKGID0.CleanFrame` (launcher) and `YOURPKGID1.CFTest` (probe). Home route never touched. Dev Mode still on.
- TV setting changed: Settings → General → External Device Manager → Device Connect Manager → **Access Notification** set to **First Time Only** (was Off). Revert to Off if desired. Device List has harmless "Smart Device (Allowed)" entries from remote-pairing tests.
- Mac: toolchain/cert/project all retained. Launcher reproducible via `launcher-app/device-build/build.sh`.
- Finding: sound-output + HDMI-input switching NOT possible from the app (network remote never authorizes: 14/14 ms.channel.timeOut). Fallback = Samsung deep-links + HDMI-ARC/Anynet+.

## 2026-10-27 — toolchain removed to reclaim disk (~4 GB+)
Removed: `~/tizen-studio`, `~/tizen-studio-data`, `~/.package-manager`, `~/tizen-tools/{jdk17,jdk-dl,tizen-installer.bin,sdb-dl}`, the samsung-tv-root temp dir, and /tmp working files.
KEPT: `~/Desktop/SamsungTVLauncher` (project), `~/SamsungCertificate` (<your-cert-profile> certs + backed-up `profiles.xml`), `~/tizen-tools/sdb` (to reach the TV).
To rebuild later: reinstall JDK 17 + Tizen Studio 6.1 (web-cli) + Certificate-Manager/cert-add-on/TV extensions (see CLAUDE.md), re-add the `<your-cert-profile>` profile pointing at `~/SamsungCertificate/<your-cert-profile>/` (or import `profiles.xml`), then run `launcher-app/device-build/build.sh`.
