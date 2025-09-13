// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'GeoAlarm';

  @override
  String get homeTitle => 'GeoAlarm';

  @override
  String get noAlarms => 'No alarms set.';

  @override
  String get addEditAlarm => 'Add/Edit Alarm';

  @override
  String get alarmName => 'Alarm Name';

  @override
  String get pleaseEnterName => 'Please enter a name';

  @override
  String radius(Object radius) {
    return 'Radius: $radius meters';
  }

  @override
  String get onlyOneAlarm => 'You can only activate one alarm at a time.';

  @override
  String get ok => 'OK';

  @override
  String latLon(Object lat, Object lon) {
    return 'Lat: $lat, Lon: $lon';
  }

  @override
  String get switchLanguage => 'Switch Language';

  @override
  String get localeZh => '繁體中文';

  @override
  String get localeEn => 'English';

  @override
  String get settings => 'Settings';

  @override
  String get alarmTab => 'Alarms';
}
