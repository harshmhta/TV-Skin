#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${TV_SERIAL:?Set TV_SERIAL to the authorized TV, e.g. YOUR_TV_IP:5555}"
ADB_PORT=${ADB_PORT:-5037}
adb_tv() { adb -P "$ADB_PORT" -s "$TV_SERIAL" "$@"; }
[[ "$(adb_tv get-state)" == device ]]
ANDROID_HOME=${ANDROID_HOME:-"$HOME/Library/Android/sdk"} ./gradlew :app:assembleDebug --console=plain
backup_dir="../backup/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup_dir"
adb_tv shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > "$backup_dir/home.txt"
adb_tv shell pm list packages -d > "$backup_dir/disabled-packages.txt"
adb_tv shell dumpsys package dev.astra.tv > "$backup_dir/astra-package.txt"
adb_tv shell settings get secure enabled_accessibility_services > "$backup_dir/accessibility.txt"
adb_tv install -r app/build/outputs/apk/debug/app-debug.apk
adb_tv shell am start -a android.intent.action.MAIN -c android.intent.category.HOME -n dev.cleanframe.tv/.MainActivity
printf 'Installed and opened CleanFrame. Previous home saved to %s/home.txt\n' "$backup_dir"
if [[ "${SET_HOME:-0}" == 1 ]]; then
    adb_tv shell cmd package set-home-activity dev.cleanframe.tv/.MainActivity
    adb_tv shell input keyevent KEYCODE_HOME
fi
