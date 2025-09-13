import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';
import '../../data/models/alarm_model.dart';
import '../../data/repositories/alarm_repository.dart';
import '../../data/datasources/local_datasource.dart';
import '../../core/services/notification_service.dart';
import '../../core/services/location_service.dart';

final localDataSourceProvider = Provider((ref) => LocalDataSource());

final alarmRepositoryProvider = Provider((ref) {
  return AlarmRepository(ref.read(localDataSourceProvider));
});

final locationServiceProvider = Provider((ref) => LocationService());

final alarmListProvider =
    StateNotifierProvider<AlarmListNotifier, List<AlarmModel>>((ref) {
  return AlarmListNotifier(
    ref.read(alarmRepositoryProvider),
    ref.read(locationServiceProvider),
  );
});

class AlarmListNotifier extends StateNotifier<List<AlarmModel>> {
  final AlarmRepository _repository;
  final LocationService _locationService;
  final _uuid = const Uuid();

  AlarmListNotifier(this._repository, this._locationService) : super([]) {
    loadAlarms();
    _setupNotificationCallback();
  }

  void _setupNotificationCallback() {
    // This will be called when user clicks "close alarm" button in notification
    NotificationService().handleAlarmClosed = _handleAlarmClosedFromNotification;
  }

  Future<void> _handleAlarmClosedFromNotification() async {
    if (kDebugMode) {
      print('Notification close button clicked - stopping alarm monitoring');
    }

    final currentAlarmId = _locationService.currentAlarmId;
    if (currentAlarmId != null) {
      // Use the existing _stopAlarmMonitoring method with alarm state update
      await _stopAlarmMonitoring(currentAlarmId, updateAlarmState: true);
    }
  }

  void loadAlarms() {
    state = _repository.getAllAlarms();
  }

  Future<void> addAlarm({
    required String name,
    required double latitude,
    required double longitude,
    required double radius,
  }) async {
    final newAlarm = AlarmModel(
      id: _uuid.v4(),
      name: name,
      latitude: latitude,
      longitude: longitude,
      radius: radius,
      isEnabled: false,
    );
    await _repository.addAlarm(newAlarm);
    loadAlarms();
  }

  Future<void> updateAlarm(AlarmModel alarm) async {
    await _repository.updateAlarm(alarm);
    loadAlarms();
  }

  Future<void> toggleAlarm(String id) async {
    final alarm = state.firstWhere((a) => a.id == id);

    if (!alarm.isEnabled) {
      // Enable alarm - start location monitoring
      await _startAlarmMonitoring(alarm);
    } else {
      // Disable alarm - stop location monitoring
      await _stopAlarmMonitoring(id, updateAlarmState: false);
    }

    // Update alarm state
    final updatedAlarm = AlarmModel(
      id: alarm.id,
      name: alarm.name,
      latitude: alarm.latitude,
      longitude: alarm.longitude,
      radius: alarm.radius,
      isEnabled: !alarm.isEnabled,
    );
    await _repository.updateAlarm(updatedAlarm);
    loadAlarms();
  }

  Future<void> _startAlarmMonitoring(AlarmModel alarm) async {
    try {
      await _locationService.startAlarmMonitoring(
        alarmId: alarm.id,
        alarmName: alarm.name,
        targetLat: alarm.latitude,
        targetLng: alarm.longitude,
        triggerRadius: alarm.radius,
      );
    } catch (e) {
      // Handle location service errors
      if (kDebugMode) {
        print('Failed to start alarm monitoring: $e');
      }
      rethrow;
    }
  }

  Future<void> _stopAlarmMonitoring(String alarmId, {bool updateAlarmState = true}) async {
    try {
      await _locationService.stopAlarmMonitoring();

      // Update alarm state to disabled only if requested
      if (updateAlarmState) {
        final alarm = state.firstWhere((a) => a.id == alarmId);
        final updatedAlarm = AlarmModel(
          id: alarm.id,
          name: alarm.name,
          latitude: alarm.latitude,
          longitude: alarm.longitude,
          radius: alarm.radius,
          isEnabled: false,
        );
        await _repository.updateAlarm(updatedAlarm);
        loadAlarms();
      }
    } catch (e) {
      if (kDebugMode) {
        print('Failed to stop alarm monitoring: $e');
      }
    }
  }

  Future<void> deleteAlarm(String id) async {
    await _repository.deleteAlarm(id);
    loadAlarms();
  }
}
