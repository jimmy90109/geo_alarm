// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Chinese (`zh`).
class AppLocalizationsZh extends AppLocalizations {
  AppLocalizationsZh([String locale = 'zh']) : super(locale);

  @override
  String get appTitle => '地點鬧鐘';

  @override
  String get homeTitle => '地點鬧鐘';

  @override
  String get noAlarms => '尚未設定鬧鐘。';

  @override
  String get addEditAlarm => '新增/編輯鬧鐘';

  @override
  String get alarmName => '鬧鐘名稱';

  @override
  String get pleaseEnterName => '請輸入名稱';

  @override
  String radius(Object radius) {
    return '半徑：$radius 公尺';
  }

  @override
  String get onlyOneAlarm => '同一時間只能啟用一個鬧鐘。';

  @override
  String get ok => '確定';

  @override
  String latLon(Object lat, Object lon) {
    return '緯度：$lat，經度：$lon';
  }

  @override
  String get switchLanguage => '切換語言';

  @override
  String get localeZh => '繁體中文';

  @override
  String get localeEn => '英文';

  @override
  String get settings => '設定';

  @override
  String get alarmTab => '鬧鐘';
}

/// The translations for Chinese, as used in Taiwan (`zh_TW`).
class AppLocalizationsZhTw extends AppLocalizationsZh {
  AppLocalizationsZhTw(): super('zh_TW');

  @override
  String get appTitle => '地點鬧鐘';

  @override
  String get homeTitle => '地點鬧鐘';

  @override
  String get noAlarms => '尚未設定鬧鐘。';

  @override
  String get addEditAlarm => '新增/編輯鬧鐘';

  @override
  String get alarmName => '鬧鐘名稱';

  @override
  String get pleaseEnterName => '請輸入名稱';

  @override
  String radius(Object radius) {
    return '半徑：$radius 公尺';
  }

  @override
  String get onlyOneAlarm => '同一時間只能啟用一個鬧鐘。';

  @override
  String get ok => '確定';

  @override
  String latLon(Object lat, Object lon) {
    return '緯度：$lat，經度：$lon';
  }

  @override
  String get switchLanguage => '切換語言';

  @override
  String get localeZh => '繁體中文';

  @override
  String get localeEn => '英文';

  @override
  String get settings => '設定';

  @override
  String get alarmTab => '鬧鐘';
}
