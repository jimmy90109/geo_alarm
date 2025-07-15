import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/alarm_provider.dart';
import 'alarm_edit_screen.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:permission_handler/permission_handler.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  void initState() {
    super.initState();
    _requestPermissions();
  }

  Future<void> _requestPermissions() async {
    // request notification permission
    var notificationStatus = await Permission.notification.request();
    if (notificationStatus.isPermanentlyDenied) {
      await openAppSettings();
    }
    // request location permission
    var locationStatus = await Permission.location.request();
    if (locationStatus.isPermanentlyDenied) {
      await openAppSettings();
    }
  }

  @override
  Widget build(BuildContext context) {
    final alarms = ref.watch(alarmListProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.homeTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                    builder: (context) => const AlarmEditScreen()),
              );
            },
          ),
        ],
      ),
      body: alarms.isEmpty
          ? Center(child: Text(AppLocalizations.of(context)!.noAlarms))
          : ListView.builder(
              itemCount: alarms.length,
              itemBuilder: (context, index) {
                final alarm = alarms[index];
                return ListTile(
                  title: Text(alarm.name),
                  subtitle: Text(AppLocalizations.of(context)!.latLon(
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
                            title:
                                Text(AppLocalizations.of(context)!.alarmName),
                            content: Text(
                                AppLocalizations.of(context)!.onlyOneAlarm),
                            actions: [
                              TextButton(
                                onPressed: () => Navigator.of(context).pop(),
                                child: Text(AppLocalizations.of(context)!.ok),
                              ),
                            ],
                          ),
                        );
                        return;
                      }
                      ref
                          .read(alarmListProvider.notifier)
                          .toggleAlarm(alarm.id);
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
            ),
    );
  }
}
