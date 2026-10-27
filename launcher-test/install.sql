PRAGMA foreign_keys=ON;
DELETE FROM package_app_info WHERE package='com.example.cleanframetest';
DELETE FROM package_info WHERE package='com.example.cleanframetest';
DELETE FROM package_localized_info WHERE package='com.example.cleanframetest';
DELETE FROM package_privilege_info WHERE package='com.example.cleanframetest';

INSERT INTO package_info
 (package, package_type, package_version, package_api_version, install_location,
  package_removable, package_system, installed_time, installed_storage, mainapp_id, root_path)
VALUES
 ('com.example.cleanframetest', 'wgt', '0.1.0', '2.4', 'auto', 'true', 'false',
  strftime('%s','now'), 'installed_internal', 'com.example.cleanframetest',
  '/opt/usr/apps/com.example.cleanframetest');

INSERT INTO package_app_info
 (app_id, app_component, app_exec, app_type, app_mainapp, app_installed_storage,
  app_package_type, app_package_system, app_removable, component_type, package,
  app_root_path, app_api_version)
VALUES
 ('com.example.cleanframetest', 'uiapp',
  '/opt/usr/apps/com.example.cleanframetest/bin/com.example.cleanframetest',
  'webapp', 'true', 'installed_internal', 'wgt', 'false', 'true', 'uiapp',
  'com.example.cleanframetest', '/opt/usr/apps/com.example.cleanframetest', '2.4');

INSERT INTO package_localized_info
 (package, package_locale, package_label, package_icon)
VALUES
 ('com.example.cleanframetest', 'No Locale', 'CF Test',
  '/opt/usr/apps/com.example.cleanframetest/shared/res/com.example.cleanframetest.png');

INSERT INTO package_privilege_info (package, privilege, type) VALUES
 ('com.example.cleanframetest', 'http://tizen.org/privilege/application.launch', 'public'),
 ('com.example.cleanframetest', 'http://tizen.org/privilege/application.info', 'public'),
 ('com.example.cleanframetest', 'http://tizen.org/privilege/internet', 'public'),
 ('com.example.cleanframetest', 'http://tizen.org/privilege/tizen', 'public'),
 ('com.example.cleanframetest', 'http://tizen.org/privilege/tv.inputdevice', 'public');
