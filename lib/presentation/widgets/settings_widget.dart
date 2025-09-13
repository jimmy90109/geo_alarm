import 'package:flutter/material.dart';
import 'package:geo_alarm/l10n/app_localizations.dart';
import 'package:geo_alarm/main.dart';

class SettingsWidget extends StatelessWidget {
  const SettingsWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final supportedLocales = [
      const Locale('zh', 'TW'),
      const Locale('en'),
    ];
    Locale currentLocale = Localizations.localeOf(context);
    final localizations = AppLocalizations.of(context)!;

    void showLocaleSheet() {
      showModalBottomSheet(
        context: context,
        builder: (context) {
          return SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 40,
                  height: 6,
                  margin: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(
                    color: Colors.grey[400],
                    borderRadius: BorderRadius.circular(3),
                  ),
                ),
                ...supportedLocales.map((locale) {
                  String label =
                      locale.languageCode == 'zh' ? localizations.localeZh : localizations.localeEn;
                  return ListTile(
                    title: Text(label),
                    selected:
                        locale.languageCode == currentLocale.languageCode,
                    onTap: () {
                      Navigator.of(context).pop();
                      MyApp.setLocale(context, locale);
                    },
                  );
                }),
              ],
            ),
          );
        },
      );
    }

    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(localizations.switchLanguage),
          const SizedBox(height: 16),
          ElevatedButton(
            onPressed: showLocaleSheet,
            child: Text(currentLocale.languageCode == 'zh' ? localizations.localeZh : localizations.localeEn),
          ),
        ],
      ),
    );
  }
} 