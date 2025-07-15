import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'alarm_edit_screen.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:permission_handler/permission_handler.dart';
import '../widgets/alarm_list_widget.dart';
import '../widgets/settings_widget.dart';

enum HomeTab { alarms, settings }

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  HomeTab _selectedTab = HomeTab.alarms;

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
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.homeTitle),
        actions: _selectedTab == HomeTab.alarms
            ? [
                IconButton(
                  icon: const Icon(Icons.add),
                  onPressed: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                          builder: (context) => const AlarmEditScreen()),
                    );
                  },
                ),
              ]
            : null,
      ),
      body: _buildBody(),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedTab.index,
        onTap: (index) {
          setState(() {
            _selectedTab = HomeTab.values[index];
          });
        },
        items: [
          BottomNavigationBarItem(
            backgroundColor: Theme.of(context).colorScheme.primary,
            icon: const Icon(Icons.alarm),
            label: AppLocalizations.of(context)!.alarmTab,
          ),
          BottomNavigationBarItem(
            backgroundColor: Theme.of(context).colorScheme.primary,
            icon: const Icon(Icons.settings),
            label: AppLocalizations.of(context)!.settings,
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    switch (_selectedTab) {
      case HomeTab.alarms:
        return const AlarmListWidget();
      case HomeTab.settings:
        return const SettingsWidget();
    }
  }
}
