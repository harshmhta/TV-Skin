#!/bin/bash
# Reproduce the CleanFrame device build and install it on the TV.
# Location-independent: works wherever the SamsungTVLauncher folder lives.
# Usage:  ./build.sh         (clean build + install)
#         ./build.sh debug   (include the debug diag: POST to Mac :8099 + probes)
set -e
# ============================================================================
# >>> EDIT THESE FOR YOUR TV <<<
TV_IP="${TV_IP:-YOUR_TV_IP}"                  # your TV's LAN IP (TV: Settings > General > Network > Network Status)
CERT_PROFILE="${CERT_PROFILE:-YOUR_CERT_PROFILE}"  # your Samsung cert profile name in Tizen Certificate Manager
APP_ID="${APP_ID:-YOURPKGID0.CleanFrame}"     # must match <tizen:application id> in launcher-app/CleanFrame/config.xml
# ============================================================================
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"   # the SamsungTVLauncher folder
APP="$ROOT/launcher-app/CleanFrame"
BD="$ROOT/launcher-app/device-build"
# --- toolchain (reinstall if removed; see CLAUDE.md) ---
export JAVA_HOME=~/tizen-tools/jdk17/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
TIZEN=~/tizen-studio/tools/ide/bin/tizen
SDB=~/tizen-studio/tools/sdb
DIAG="${1:-}"
python3 - "$ROOT" "$APP" "$BD" "$DIAG" <<'PY'
import sys, pathlib
root=pathlib.Path(sys.argv[1]); app=pathlib.Path(sys.argv[2]); bd=pathlib.Path(sys.argv[3]); diagmode=sys.argv[4] if len(sys.argv)>4 else ""
html=(root/"prototype/index.html").read_text().replace("ZOOM=1.045","ZOOM=1.10")
css=(bd/"device.css").read_text(); shim=(bd/"device_shim.html").read_text()
diag=(bd/"device_diag_DEBUG.html").read_text() if diagmode=="debug" else ""
meta='<meta charset="utf-8">\n<meta name="viewport" content="width=1920, initial-scale=1, maximum-scale=1, user-scalable=no">'
html=html.replace('<meta charset="utf-8">', meta, 1).replace("</head>","<style>\n"+css+"\n</style>\n</head>",1).replace("</body>",shim+"\n"+diag+"\n</body>",1)
(app/"index.html").write_text(html); print("index.html assembled (diag=%s)" % (diagmode or "none"))
PY
rm -rf "$APP/.buildResult"
"$TIZEN" build-web -- "$APP"
"$TIZEN" package -t wgt -s "$CERT_PROFILE" -- "$APP/.buildResult"
"$SDB" connect "$TV_IP"
"$TIZEN" install -n CleanFrame.wgt -- "$APP/.buildResult"
echo "Installed $APP_ID. Launch: $SDB shell 0 was_execute $APP_ID"
