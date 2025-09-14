import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geo_alarm/l10n/app_localizations.dart';
import '../providers/alarm_provider.dart';
import '../screens/alarm_edit_screen.dart';
import '../../data/models/alarm_model.dart';

class AlarmListWidget extends ConsumerWidget {
  const AlarmListWidget({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final alarms = ref.watch(alarmListProvider);
    final localizations = AppLocalizations.of(context)!;

    return alarms.isEmpty
        ? SliverFillRemaining(
            child: Center(
              child: Text(localizations.noAlarms),
            ),
          )
        : SliverList.builder(
            itemCount: alarms.length,
            itemBuilder: (context, index) {
              final alarm = alarms[index];
              return Dismissible(
                key: Key(alarm.id),
                direction: DismissDirection.endToStart,
                background: Container(
                  alignment: Alignment.centerRight,
                  padding: const EdgeInsets.only(right: 20),
                  color: Colors.red,
                  child: const Icon(
                    Icons.delete,
                    color: Colors.white,
                  ),
                ),
                onDismissed: (direction) {
                  _deleteAlarmWithUndo(context, ref, alarm, alarms);
                },
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 24.0),
                  title: Text(alarm.name),
                  subtitle: Text(
                    localizations.latLon(
                      alarm.latitude.toStringAsFixed(2),
                      alarm.longitude.toStringAsFixed(2),
                    ),
                  ),
                  trailing: _alarmSwitch(
                    alarm,
                    alarms,
                    context,
                    localizations,
                    ref,
                  ),
                  onTap: () {
                    // If alarm is enabled, show dialog to ask user to disable first
                    if (alarm.isEnabled) {
                      showDialog(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: Text(alarm.name),
                          content: Text(localizations.onlyEditWhenDisabled),
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
                    // Navigate to edit screen with alarm data
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (context) => AlarmEditScreen(alarm: alarm),
                      ),
                    );
                  },
                ),
              );
            },
          );
  }

  Widget _alarmSwitch(
    AlarmModel alarm,
    List<AlarmModel> alarms,
    BuildContext context,
    AppLocalizations localizations,
    WidgetRef ref,
  ) {
    return Switch(
      value: alarm.isEnabled,
      onChanged: (value) {
        final enabledCount = alarms.where((a) => a.isEnabled).length;
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
    );
  }

  void _deleteAlarmWithUndo(
    BuildContext context,
    WidgetRef ref,
    AlarmModel alarm,
    List<AlarmModel> alarms,
  ) {
    // Temporarily remove from provider
    ref.read(alarmListProvider.notifier).deleteAlarm(alarm.id);

    // Show SnackBar with undo option
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(AppLocalizations.of(context)!.alarmDeleted),
        duration: const Duration(seconds: 4),
        behavior: SnackBarBehavior.floating,
        dismissDirection: DismissDirection.horizontal,
        action: SnackBarAction(
          label: AppLocalizations.of(context)!.undo,
          onPressed: () {
            // Restore the alarm
            ref.read(alarmListProvider.notifier).restoreAlarm(alarm);
          },
        ),
      ),
    );
  }
}
