import '../datasources/local_datasource.dart';
import '../models/alarm_model.dart';

class AlarmRepository {
  final LocalDataSource _localDataSource;

  AlarmRepository(this._localDataSource);

  Future<void> addAlarm(AlarmModel alarm) {
    return _localDataSource.addAlarm(alarm);
  }

  Future<AlarmModel?> getAlarm(String id) {
    return _localDataSource.getAlarm(id);
  }

  List<AlarmModel> getAllAlarms() {
    return _localDataSource.getAllAlarms();
  }

  Future<void> updateAlarm(AlarmModel alarm) {
    return _localDataSource.updateAlarm(alarm);
  }

  Future<void> deleteAlarm(String id) {
    return _localDataSource.deleteAlarm(id);
  }
} 