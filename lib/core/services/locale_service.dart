import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LocaleService {
  static const String _localeKey = 'selected_locale';
  static LocaleService? _instance;
  late SharedPreferences _prefs;

  LocaleService._();

  static LocaleService get instance {
    _instance ??= LocaleService._();
    return _instance!;
  }

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
  }

  /// Get the saved locale or determine locale based on system language
  Future<Locale> getLocale() async {
    // Check if user has previously set a locale
    final savedLocaleCode = _prefs.getString(_localeKey);

    if (savedLocaleCode != null) {
      // Return saved locale
      if (savedLocaleCode == 'zh_TW') {
        return const Locale('zh', 'TW');
      } else if (savedLocaleCode == 'zh') {
        return const Locale('zh');
      } else {
        return const Locale('en');
      }
    }

    // First time launch - determine locale based on system
    final systemLocale = WidgetsBinding.instance.platformDispatcher.locale;
    Locale appLocale;

    if (systemLocale.languageCode == 'zh') {
      // If system is Chinese, use Chinese
      appLocale = const Locale('zh', 'TW');
    } else {
      // All other languages default to English
      appLocale = const Locale('en');
    }

    // Save the determined locale
    await setLocale(appLocale);
    return appLocale;
  }

  /// Save locale to shared preferences
  Future<void> setLocale(Locale locale) async {
    String localeCode;
    if (locale.languageCode == 'zh') {
      localeCode = locale.countryCode == 'TW' ? 'zh_TW' : 'zh';
    } else {
      localeCode = 'en';
    }

    await _prefs.setString(_localeKey, localeCode);
  }

  /// Clear saved locale (for testing purposes)
  Future<void> clearLocale() async {
    await _prefs.remove(_localeKey);
  }
}