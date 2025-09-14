import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart';
import '../utils/distance_calculator.dart';
import 'notification_service.dart';

class LocationService {
  Future<bool> handleLocationPermission() async {
    bool serviceEnabled;
    LocationPermission permission;

    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      // Location services are not enabled don't continue
      // accessing the position and request users of the
      // App to enable the location services.
      return false;
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return false;
      }
    }

    if (permission == LocationPermission.deniedForever) {
      // Permissions are denied forever, handle appropriately.
      return false;
    }

    // When we reach here, permissions are granted and we can
    // continue accessing the position of the device.
    return true;
  }

  Future<Position?> getCurrentPosition() async {
    final hasPermission = await handleLocationPermission();
    if (!hasPermission) return null;
    try {
      return await Geolocator.getCurrentPosition(
          desiredAccuracy: LocationAccuracy.high);
    } catch (e) {
      if (kDebugMode) {
        print(e);
      }

      return null;
    }
  }

  Stream<Position> getPositionStream() {
    return Geolocator.getPositionStream(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: 50, // meters - more frequent updates for alarm
      ),
    );
  }

  StreamSubscription<Position>? _alarmSubscription;
  String? _currentAlarmId;
  double? _initialDistance;

  /// Start monitoring location for alarm
  Future<void> startAlarmMonitoring({
    required String alarmId,
    required String alarmName,
    required double targetLat,
    required double targetLng,
    required double triggerRadius,
  }) async {
    final hasPermission = await handleLocationPermission();
    if (!hasPermission) return;

    final startPos = await getCurrentPosition();
    if (startPos == null) return;

    _currentAlarmId = alarmId;
    _initialDistance = DistanceCalculator.calculateDistance(
      startPos.latitude,
      startPos.longitude,
      targetLat,
      targetLng,
    );

    // Show initial live notification
    await NotificationService().showLiveNotification(
      alarmName,
      _initialDistance!,
      0,
    );

    // Start location monitoring
    _alarmSubscription?.cancel();
    _alarmSubscription = getPositionStream().listen((position) async {
      await _handleLocationUpdate(
        position,
        alarmName,
        targetLat,
        targetLng,
        triggerRadius,
      );
    });
  }

  Future<void> _handleLocationUpdate(
    Position position,
    String alarmName,
    double targetLat,
    double targetLng,
    double triggerRadius,
  ) async {
    if (_initialDistance == null) return;

    final currentDistance = DistanceCalculator.calculateDistance(
      position.latitude,
      position.longitude,
      targetLat,
      targetLng,
    );

    // Calculate progress (0-100%)
    final progress = _calculateProgress(currentDistance, triggerRadius);

    // Update live notification
    await NotificationService().updateLiveNotification(
      currentDistance,
      progress,
    );

    // Check if alarm should be triggered
    if (currentDistance <= triggerRadius && progress >= 100) {
      await _triggerAlarm(alarmName);
    }
  }

  int _calculateProgress(double currentDistance, double triggerRadius) {
    if (_initialDistance == null) return 0;

    if (currentDistance <= triggerRadius) {
      return 100;
    }

    final travelled = _initialDistance! - currentDistance;
    final totalToTravel = _initialDistance! - triggerRadius;

    if (totalToTravel <= 0) return 100;

    final progress = (travelled / totalToTravel * 100).clamp(0, 100);
    return progress.round();
  }

  Future<void> _triggerAlarm(String alarmName) async {
    await NotificationService().triggerAlarmVibration();
    await NotificationService().updateLiveNotification(
      0, // Distance becomes 0 when arrived
      100,
      isArrived: true,
    );
  }

  /// Stop alarm monitoring
  Future<void> stopAlarmMonitoring() async {
    _alarmSubscription?.cancel();
    _alarmSubscription = null;
    _currentAlarmId = null;
    _initialDistance = null;
    await NotificationService().hideLiveNotification();
  }

  /// Get current monitoring alarm ID
  String? get currentAlarmId => _currentAlarmId;

  /// Check if currently monitoring an alarm
  bool get isMonitoring => _alarmSubscription != null && _currentAlarmId != null;
}
