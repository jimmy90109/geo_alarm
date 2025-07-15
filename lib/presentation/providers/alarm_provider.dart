import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../../data/models/alarm_model.dart';
import '../../data/repositories/alarm_repository.dart';
import '../../data/datasources/local_datasource.dart';
import '../../core/services/notification_service.dart';

final localDataSourceProvider = Provider((ref) => LocalDataSource());

final alarmRepositoryProvider = Provider((ref) {
  return AlarmRepository(ref.read(localDataSourceProvider));
});

final alarmListProvider =
    StateNotifierProvider<AlarmListNotifier, List<AlarmModel>>((ref) {
  return AlarmListNotifier(ref.read(alarmRepositoryProvider));
});

class AlarmListNotifier extends StateNotifier<List<AlarmModel>> {
  final AlarmRepository _repository;
  final _uuid = const Uuid();

  AlarmListNotifier(this._repository) : super([]) {
    loadAlarms();
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

    if (!alarm.isEnabled) {
      await NotificationService().showAlarmNotification(alarm.name, "鬧鐘已啟用");
    } else {
      await NotificationService().stopAlarmNotification();
    }
  }

  Future<void> deleteAlarm(String id) async {
    await _repository.deleteAlarm(id);
    loadAlarms();
  }
}
