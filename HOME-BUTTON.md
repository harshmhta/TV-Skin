# Home button — findings and procedure

Date: 2026-10-26. Owner's own TV. Supersedes section C of `HANDOFF.md`.

## TL;DR

- **It is not blocked.** Temporary **root (uid 0)** was obtained on this TV with the
  public `samsung-tv-root` tool. The earlier "root is impossible" conclusion was wrong.
- The **Home button is a routing table entry**, not firmware. It lives in
  `/opt/dbspace/.app-context.db`, table `key_filter`:
  ```
  com.samsung.tv.csfs | XF86Home | | | | launch | package:com.samsung.tv.csfs | entry_point:smarthub_key
  ```
  Change that target to our launcher's app id and Home opens the launcher.
- What is still missing is only the **launcher app itself** (build + install). Two ways,
  see §5. The TV is ready; the remap is a one-line DB change plus a service reload.

## 1. Root, confirmed

Tool: `samsung-tv-root` v0.0.12 (github.com/chris-ritsen/samsung-tv-root), macOS arm64
release, checksum verified. Route used: **`archive-root`** (model-independent; uses the
public SVE-2025-50109 package-name foothold + Samsung's own capability-bearing `dotnet`).

Evidence from `archive-root probe <your-TV-IP>`:
```
authenticated: true
uid: 0   euid: 0   gid: 0   egid: 0
cap_eff: 0000003fffffffff
runtime: 3.1 (detected)
smack: User
system_files_unchanged: true
staging_cleaned: true
preflight_capabilities:
  /usr/bin/dotnet = cap_setgid,cap_sys_admin+ei
  /usr/sbin/sdbd-tarlauncher = cap_setgid,cap_setuid+eip
```
Notes:
- Volatile only. Nothing persistent is installed; root is re-acquired per session.
- The root agent keeps Samsung's `User` SMACK label, so not every file is writable,
  but `/opt/dbspace` and `/opt/usr/apps` are.
- `sdb shell` still prints `closed`. That is expected; the foothold is the package-name
  injection, not the shell.
- First probe failed because macOS had not yet allowed the binary inbound. Second run
  worked. If a callback ever fails again, check Firewall → Options → allow
  `samsung-tv-root`.

Working command (from the extracted release dir):
```sh
./samsung-tv-root --sdb ~/tizen-tools/sdb/data/tools/sdb archive-root root <your-TV-IP> \
  --command 'id' --command 'uname -a'
```
TV: `Linux Samsung 4.1.10 #1 SMP PREEMPT ... armv7l`, Tizen 6.0.

## 2. How Home is actually routed (confirmed on this TV)

`/opt/dbspace/.app-context.db` → `key_filter` holds two `XF86Home` rows:

| appid | key | longpress | request | target_selector | args |
|---|---|---|---|---|---|
| `org.tizen.remote-management` | XF86Home | 5300 | launch | `package:org.tizen.remote-management` | |
| `com.samsung.tv.csfs` | XF86Home | | launch | `package:com.samsung.tv.csfs` | `entry_point:smarthub_key` |

- Row 2 is the normal Home press → Samsung Smart Hub (`com.samsung.tv.csfs`, entry
  point `smarthub_key`).
- Row 1 is the ~5.3 s long-press.
- `managed_app` also lists `com.samsung.tv.csfs` with `ui_type=partial`.

Other routing facts:
- `db/appfw/home_appid` = **`org.tizen.tv-viewer`** (the live-TV app). This is **not**
  the Home button on this model; do not rely on it. It matches why the community's
  "set home_appid" trick alone would look like it did nothing here.
- `db/appfw/startup_appid` **does not exist** on this firmware.
- Tizen's documented "main operation" handler is also available and is what
  `berteotti/custom-tv` uses:
  `app_operation_set_current_app("http://tizen.org/appcontrol/operation/main", appId)`
  from `/usr/lib/libcapi-appfw-app-operation-tv.so.0`, **called as uid 5001**, not root.
  This is the cleaner, API-level route; the `key_filter` row is the raw route. Try the
  API first, fall back to the DB row.

## 3. Useful on-TV commands (all run through the root session)

- Installed apps: `app_launcher -l`
- Start an app: `app_launcher -s <appid>`
- Read Home routing: `sqlite3 /opt/dbspace/.app-context.db "select * from key_filter where key='XF86Home';"`
- Package DB: `/opt/dbspace/.pkgmgr_parser.db` (`package_info`, `package_app_info`, ...)
- Key event injection: `/usr/bin/input_keyevent` (syntax not yet confirmed; `timeout`
  and `strings` are absent on the TV).

Known app ids seen: `com.samsung.tv.csfs` (Smart Hub), `org.tizen.tv-viewer` (live TV),
`com.samsung.tv.coba.home` (Search), `GHI3a0zMSx.XboxGamePass` (Xbox),
`XtU8fQk2gN` (ESPN, wgt), `Ob3XhAVMu6` (AMC+, wgt).

## 4. Backups already taken (keep private)

- `backup/app-context.db` — copy of `/opt/dbspace/.app-context.db` before any change.
- `backup/home-route.txt` — the two `XF86Home` rows above.
- Re-take before every change; compare SHA-256.

Rollback for the DB route (always run this before uninstalling the launcher):
```sh
sqlite3 /opt/dbspace/.app-context.db \
 "update key_filter set target_selector='package:com.samsung.tv.csfs', args='entry_point:smarthub_key' \
  where appid='com.samsung.tv.csfs' and key='XF86Home';"
```
Rollback for the API route: `app_operation_set_current_app(main, "com.samsung.tv.csfs")`.
Factory reset is the final backstop and always restores Samsung's Home.

## 5. What is left: get the launcher onto the TV

The remap needs a target app id. Two install paths:

### A. Signed install (standard, clean, recommended long-term)
Tizen Studio CLI + Java, owner creates a Samsung developer certificate tied to the TV's
DUID (owner must sign in; an agent must not), build the prototype as a `.wgt`, sign,
`sdb install`. Clean uninstall, survives updates, no DB surgery for the app itself.
Cost: one-time setup + the owner's Samsung sign-in.

### B. Root install (no Samsung account, faster)
We already have root, so the package manager can be bypassed:
1. Package `prototype/` as a Tizen web app (`config.xml` + assets) under a chosen
   package id, e.g. `com.example.cleanframe`.
2. As root: place it under `/opt/usr/apps/<pkgid>/`, copy the file ownership/SMACK label
   from an existing wgt app (e.g. `XtU8fQk2gN`), and insert matching rows into
   `.pkgmgr_parser.db` (`package_info`, `package_app_info`, `package_localized_info`).
3. Verify with `app_launcher -l` and `app_launcher -s <appid>`.
4. Then remap Home (§6).
Risk: unsupported registration; may need a reboot; app may not appear until the package
DB is consistent. Undo: remove the dir and the DB rows.

Either way, the app id must stay stable once Home points at it.

## 6. The remap (after the app is installed and launches)

1. Back up `.app-context.db` again.
2. Prefer the API route: run a tiny .NET helper as uid 5001 that calls
   `app_operation_set_current_app(main, "<our app id>")` and reads it back.
3. If Home still opens Smart Hub, edit the `key_filter` row for
   `com.samsung.tv.csfs`/`XF86Home` to `target_selector='package:<our app id>'`, `args=''`,
   then reload the app-context service or reboot.
4. Test: open another app → press **Home** → launcher appears. Then test standby/wake and
   a cold power-cycle (persistence is the known gap in the public projects).
5. Keep the rollback command from §4 in `CLEANUP.md`.

### The `__NO_RAISE_WINDOW` gotcha (from the public write-ups)
Pointing Home straight at a paused launcher can leave it stuck behind the loading screen
after Netflix, because the TV raises its home app through a path that does not raise a
paused app. The proven fix is to point Home at a **tiny key app** that immediately calls
`tizen.application.launch(<launcher>)` and exits. Power-on uses `startup_appid` (absent
here) or Autorun Last App, so a failure in the key app cannot black-screen the TV.

## 7. Safety rules for this work

- This is owner-authorized work on owner-owned hardware. Use only existing public tooling.
- Back up before every write; verify `system_files_unchanged` where the tool reports it.
- Never leave Home pointing at an app that is not installed.
- Keep `CLEANUP.md` current; factory reset is the guaranteed reset.
