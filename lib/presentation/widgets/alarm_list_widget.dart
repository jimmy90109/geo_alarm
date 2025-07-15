import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/alarm_provider.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

class AlarmListWidget extends ConsumerWidget {
  const AlarmListWidget({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final alarms = ref.watch(alarmListProvider);
    final localizations = AppLocalizations.of(context)!;
    return alarms.isEmpty
        ? Center(child: Text(localizations.noAlarms))
        : ListView.builder(
            itemCount: alarms.length,
            itemBuilder: (context, index) {
              final alarm = alarms[index];
              return ListTile(
                title: Text(alarm.name),
                subtitle: Text(localizations.latLon(
                  alarm.latitude.toStringAsFixed(2),
                  alarm.longitude.toStringAsFixed(2),
                )),
                trailing: Switch(
                  value: alarm.isEnabled,
                  onChanged: (value) {
                    final enabledCount =
                        alarms.where((a) => a.isEnabled).length;
                    // show dialog if more than one alarm is enabled
                    if (!alarm.isEnabled && enabledCount >= 1) {
                      showDialog(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: Text(localizations.alarmName),
                          content: Text(localizations.onlyOneAlarm),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.of(context).pop(),
                              child: Text(localizations.ok),
                            ),
                          ],
                        ),
                      );
                      return;
                    }
                    ref.read(alarmListProvider.notifier).toggleAlarm(alarm.id);
                  },
                ),
                onLongPress: () {
                  // Navigate to edit screen
                },
                onTap: () {
                  // Navigate to edit screen, or show details
                },
              );
            },
          );
  }
} 