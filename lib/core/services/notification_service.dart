import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:vibration/vibration.dart';
import 'package:flutter/services.dart';
import 'dart:io';

class NotificationService {
  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();
  static const MethodChannel _channel = MethodChannel('geo_alarm/notification');

  Future<void> init() async {
    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    const InitializationSettings initializationSettings =
        InitializationSettings(android: initializationSettingsAndroid);

    await _notificationsPlugin.initialize(initializationSettings);
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
