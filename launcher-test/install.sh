#!/bin/sh
# Root install of the CleanFrame test web app. Idempotent: removes and re-adds.
set -e

PKG=com.example.cleanframetest
APP=$PKG
DIR=/opt/usr/apps/$PKG
DB=/opt/dbspace/.pkgmgr_parser.db
NOW=$(date +%s)

rm -rf "$DIR"
mkdir -p "$DIR/bin" "$DIR/res" "$DIR/shared/res" "$DIR/tmp"
tar xzf /tmp/testapp.tgz -C "$DIR/res"
ln -sf /usr/bin/wrt "$DIR/bin/$APP"
cp "$DIR/res/icon.png" "$DIR/shared/res/$APP.png"

chown -R 201:root "$DIR"
chmod 755 "$DIR" "$DIR/bin" "$DIR/res" "$DIR/shared" "$DIR/tmp"

chsmack -a "User::Home" "$DIR"
chsmack -a "User::Pkg::${PKG}::RO" -r "$DIR/bin"
chsmack -a "User::Pkg::${PKG}::RO" -r "$DIR/res"
chsmack -a "User::Home" -r "$DIR/shared"
chsmack -a "User::Pkg::${PKG}" "$DIR/tmp"

sqlite3 "$DB" <<SQL
PRAGMA foreign_keys=ON;
DELETE FROM package_app_info WHERE package='$PKG';
DELETE FROM package_info WHERE package='$PKG';
DELETE FROM package_localized_info WHERE package='$PKG';
DELETE FROM package_privilege_info WHERE package='$PKG';

INSERT INTO package_info
 (package, package_type, package_version, package_api_version, install_location,
  package_removable, package_system, installed_time, installed_storage, mainapp_id, root_path)
VALUES
 ('$PKG', 'wgt', '0.1.0', '2.4', 'auto', 'true', 'false', '$NOW',
  'installed_internal', '$APP', '$DIR');

INSERT INTO package_app_info
 (app_id, app_component, app_exec, app_type, app_mainapp, app_installed_storage,
  app_package_type, app_package_system, app_removable, component_type, package,
  app_root_path, app_api_version)
VALUES
 ('$APP', 'uiapp', '$DIR/bin/$APP', 'webapp', 'true', 'installed_internal',
  'wgt', 'false', 'true', 'uiapp', '$PKG', '$DIR', '2.4');

INSERT INTO package_localized_info
 (package, package_locale, package_label, package_icon)
VALUES
 ('$PKG', 'No Locale', 'CF Test', '$DIR/shared/res/$APP.png');

INSERT INTO package_privilege_info (package, privilege, type) VALUES
 ('$PKG', 'http://tizen.org/privilege/application.launch', 'public'),
 ('$PKG', 'http://tizen.org/privilege/application.info', 'public'),
 ('$PKG', 'http://tizen.org/privilege/internet', 'public'),
 ('$PKG', 'http://tizen.org/privilege/tizen', 'public'),
 ('$PKG', 'http://tizen.org/privilege/tv.inputdevice', 'public');
SQL

echo "--- rows ---"
sqlite3 -line "$DB" "select package,package_type,root_path,mainapp_id from package_info where package='$PKG';"
sqlite3 -line "$DB" "select app_id,app_exec,app_type from package_app_info where package='$PKG';"
echo "--- files ---"
ls -lZ "$DIR" "$DIR/bin" "$DIR/res" | head -20
echo "--- launcher list ---"
app_launcher -l | grep -i "cleanframe" || echo "NOT LISTED"
