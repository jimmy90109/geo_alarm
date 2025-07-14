import 'package:hive/hive.dart';
import '../models/alarm_model.dart';

class LocalDataSource {
  static const String _alarmBoxName = 'alarms';

  Future<void> init() async {
    await Hive.openBox<AlarmModel>(_alarmBoxName);
  }

  Box<AlarmModel> get _alarmBox => Hive.box<AlarmModel>(_alarmBoxName);

  Future<void> addAlarm(AlarmModel alarm) async {
    await _alarmBox.put(alarm.id, alarm);
  }

  Future<AlarmModel?> getAlarm(String id) async {
    return _alarmBox.get(id);
  }

  List<AlarmModel> getAllAlarms() {
    return _alarmBox.values.toList();
  }

  Future<void> updateAlarm(AlarmModel alarm) async {
    await _alarmBox.put(alarm.id, alarm);
  }

  Future<void> deleteAlarm(String id) async {
    await _alarmBox.delete(id);
  }
} 