import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:vibration/vibration.dart';
import 'package:flutter/services.dart';
import 'dart:io';

class NotificationService {
  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();
  static const MethodChannel _channel = MethodChannel('geo_alarm/notification');

  // Singleton instance
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  Future<void> init() async {
    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    const InitializationSettings initializationSettings =
        InitializationSettings(android: initializationSettingsAndroid);

    await _notificationsPlugin.initialize(initializationSettings);

    // Set up MethodChannel callback handler
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onAlarmButtonClicked':
        final String action = call.arguments as String;
        if (action == 'close_alarm') {
          // Notify any listeners that alarm was closed by user
          await _onAlarmClosed();
        }
        break;
      default:
        throw PlatformException(code: 'Unimplemented', details: 'Method ${call.method} not implemented');
    }
  }

  Function? _alarmClosedCallback;

  set handleAlarmClosed(Function callback) {
    _alarmClosedCallback = callback;
  }

  Future<void> _onAlarmClosed() async {
    // Hide the notification first
    await hideLiveNotification();

    // Call the callback if set
    if (_alarmClosedCallback != null) {
      await _alarmClosedCallback!();
    }
  }

  // 新增：Android 13+ 請求通知權限
  Future<void> requestNotificationPermission() async {
    if (Platform.isAndroid &&
        int.parse(Platform.operatingSystemVersion.split('.')[0]) >= 33) {
      FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
          FlutterLocalNotificationsPlugin();
      flutterLocalNotificationsPlugin
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>()
          ?.requestNotificationsPermission();
    }
  }

  Future<void> showAlarmNotification(String title, String body) async {
    // Flutter local notification
    const AndroidNotificationDetails androidPlatformChannelSpecifics =
        AndroidNotificationDetails(
      'geo_alarm_channel',
      'Geo Alarm Channel',
      channelDescription: 'Channel for Geo Alarm notifications',
      importance: Importance.max,
      priority: Priority.high,
      showWhen: false,
      fullScreenIntent: true,
    );
    const NotificationDetails platformChannelSpecifics =
        NotificationDetails(android: androidPlatformChannelSpecifics);
    await _notificationsPlugin.show(0, title, body, platformChannelSpecifics);
    // 呼叫 Android 原生 Live Update 通知
    try {
      await _channel.invokeMethod('showAlarmNotification');
    } catch (e) {
      // ignore error on non-Android
    }
  }

  /// Show live notification with progress
  Future<void> showLiveNotification(String alarmName, double distance, int progress) async {
    try {
      await _channel.invokeMethod('showLiveNotification', {
        'alarmName': alarmName,
        'distance': distance,
        'progress': progress,
      });
    } catch (e) {
      // ignore error on non-Android
    }
  }

  /// Update live notification progress
  Future<void> updateLiveNotification(double distance, int progress, {bool isArrived = false}) async {
    try {
      await _channel.invokeMethod('updateLiveNotification', {
        'distance': distance,
        'progress': progress,
        'isArrived': isArrived,
      });
    } catch (e) {
      // ignore error on non-Android
    }
  }

  /// Trigger alarm vibration
  Future<void> triggerAlarmVibration() async {
    try {
      await _channel.invokeMethod('triggerAlarmVibration');
      await vibrate(); // Also use Flutter vibration as backup
    } catch (e) {
      // ignore error on non-Android, fallback to Flutter vibration
      await vibrate();
    }
  }

  /// Hide live notification
  Future<void> hideLiveNotification() async {
    try {
      await _channel.invokeMethod('hideLiveNotification');
    } catch (e) {
      // ignore error on non-Android
    }
  }

  Future<void> stopAlarmNotification() async {
    try {
      await _channel.invokeMethod('stopAlarm');
    } catch (e) {
      // ignore error on non-Android
    }
  }

  Future<void> vibrate() async {
    if (await Vibration.hasVibrator()) {
      Vibration.vibrate(duration: 1000, pattern: [500, 1000, 500, 2000]);
    }
  }

  Future<void> stopVibrate() async {
    Vibration.cancel();
  }
}
