import 'package:hive/hive.dart';

part 'alarm_model.g.dart';

@HiveType(typeId: 0)
class AlarmModel extends HiveObject {
  @HiveField(0)
  late String id;

  @HiveField(1)
  late String name;

  @HiveField(2)
  late double latitude;

  @HiveField(3)
  late double longitude;

  @HiveField(4)
  late double radius;

  @HiveField(5)
  late bool isEnabled;

  AlarmModel({
    required this.id,
    required this.name,
    required this.latitude,
    required this.longitude,
    required this.radius,
    required this.isEnabled,
  });

  AlarmModel copyWith({
    String? id,
    String? name,
    double? latitude,
    double? longitude,
    double? radius,
    bool? isEnabled,
  }) {
    return AlarmModel(
      id: id ?? this.id,
      name: name ?? this.name,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      radius: radius ?? this.radius,
      isEnabled: isEnabled ?? this.isEnabled,
    );
  }
} 