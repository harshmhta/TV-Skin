INSERT OR REPLACE INTO widgetinfo
 (id, app_index, title, install_path, icon_path, version, size, install_date, pkgname,
  install_type, pkg_type, widget_category, is_removable, is_display, install_state,
  resources_path, is_app_available, is_network, is_hidden, is_lock)
VALUES
 ('3202601027001', 321, 'CF Test',
  '/opt/usr/home/owner/apps_rw/com.example.cleanframetest/',
  '/opt/share/webappservice/apps_icon/all/3202601027001/',
  '0.1.0', 0, datetime('now'), 'com.example.cleanframetest',
  'normal', 'wgt', '', 1, 1, 3,
  '/opt/usr/home/owner/apps_rw/com.example.cleanframetest/', 1, 0, 0, 0);
