import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/services/location_service.dart';
import '../providers/alarm_provider.dart';
import 'alarm_edit_screen.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  void initState() {
    super.initState();
    _requestPermission();
  }

  Future<void> _requestPermission() async {
    final locationService = LocationService();
    await locationService.handleLocationPermission();
  }

  @override
  Widget build(BuildContext context) {
    final alarms = ref.watch(alarmListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('GeoAlarm'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const AlarmEditScreen()),
              );
            },
          ),
        ],
      ),
      body: alarms.isEmpty
          ? const Center(child: Text('No alarms set.'))
          : ListView.builder(
              itemCount: alarms.length,
              itemBuilder: (context, index) {
                final alarm = alarms[index];
                return ListTile(
                  title: Text(alarm.name),
                  subtitle: Text('Lat: ${alarm.latitude.toStringAsFixed(2)}, Lon: ${alarm.longitude.toStringAsFixed(2)}'),
                  trailing: Switch(
                    value: alarm.isEnabled,
                    onChanged: (value) {
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
            ),
    );
  }
} 