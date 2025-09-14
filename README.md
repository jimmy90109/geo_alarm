# GeoAlarm - A Location-Based Alarm Clock

![Platform: Flutter](https://img.shields.io/badge/Platform-Flutter-blue.svg)
![Status: In-Development](https://img.shields.io/badge/Status-In--Development-lightgrey.svg)

An intelligent alarm clock app built with Flutter that triggers an alarm when you enter a predefined geographical area. Perfect for commuters who want to nap on the train or bus without worrying about missing their stop.

## 🌟 Core Features

* **📍 Set Destination via Map**: Easily set your destination by long-pressing on an interactive map.
* **🔍 Search for Destinations**: Find your destination quickly by searching for addresses, landmarks, or station names using the Google Places API.
* **⭕ Adjustable Trigger Radius**: Define a geofence by setting a radius (e.g., 500m, 1km, 2km) around your destination.
* **🔔 Background Monitoring**: The app reliably tracks your location in the background, even when the app is closed or the screen is locked, using a foreground service to prevent the OS from terminating it.
* **🔊 Loud & Intrusive Alarm**: When you enter the target area, a loud, vibrating, full-screen alarm is triggered, ensuring you wake up.
* **🗂️ Alarm Management**: Save, view, edit, and delete your alarms in a clean and simple list.
* **Toggle On/Off**: Easily activate or deactivate any saved alarm with a single switch.

*(Add screenshots of your app here)*

## 🛠️ Tech Stack & Architecture

This project is built using a clean, scalable, and layered architecture to separate concerns and improve maintainability.

### Architecture
* **Presentation Layer**: The UI, built with Flutter widgets. State management is handled by **Riverpod** to ensure a reactive and predictable state.
* **Core Services Layer**: The business logic heart of the app.
    * `LocationService`: Manages all GPS-related tasks using `geolocator`.
    * `BackgroundService`: The critical service that runs in the background using `flutter_background_service`, checking the user's location against active alarms.
    * `NotificationService`: Handles triggering the full-screen alarm, sound (`audioplayers`), and vibration (`vibration`).
* **Data Layer**:
    * `Repository Pattern`: A single source of truth for alarm data.
    * `Hive`: A lightweight and fast NoSQL database for local data persistence (saving the alarm list).

### Key Packages
* **State Management**: `flutter_riverpod`
* **Location & Maps**: `geolocator`, `Maps_flutter`
* **Background Execution**: `flutter_background_service`
* **Alarm & Notifications**: `flutter_local_notifications`, `audioplayers`, `vibration`
* **Local Storage**: `hive`, `hive_flutter`
* **APIs**: Google Maps Platform (Maps SDK, Places API, Geocoding API)

### Directory Structure
```
lib/
├── core/
│   ├── services/
│   │   ├── background_service.dart
│   │   ├── location_service.dart
│   │   └── notification_service.dart
│   └── utils/
│
├── data/
│   ├── models/
│   │   └── alarm_model.dart
│   ├── repositories/
│   │   └── alarm_repository.dart
│   └── datasources/
│       └── local_datasource.dart
│
└── presentation/
├── providers/
├── screens/
│   ├── home_screen.dart
│   └── alarm_edit_screen.dart
└── widgets/
```

## 🚀 Getting Started

Follow these instructions to get the project up and running on your local machine.

### Prerequisites

* Flutter SDK (version 3.35.3)
* An IDE like VS Code or Android Studio
* A Google Maps Platform API Key

### Installation & Setup

1.  **Clone the repository:**
    ```sh
    git clone [https://github.com/your-username/geoalarm.git](https://github.com/your-username/geoalarm.git)
    cd geoalarm
    ```

2.  **Install dependencies:**
    ```sh
    flutter pub get
    ```

3.  **Configure API Keys:**
    You need to add your Google Maps API key. Make sure you have enabled **Maps SDK for Android**, **Maps SDK for iOS**, and **Places API** in your Google Cloud Console.

    * **For Android:**
        1. Copy `android/local.properties.example` to `android/local.properties`
        2. Add your Google Maps API key to the `maps.apiKey` field:
           ```properties
           flutter.buildMode=debug
           flutter.versionName=1.0.0
           flutter.versionCode=1
           maps.apiKey=YOUR_GOOGLE_MAPS_API_KEY_HERE
           sdk.dir=/path/to/your/Android/sdk
           flutter.sdk=/path/to/your/flutter
           ```
        3. The API key is automatically injected into `AndroidManifest.xml` via the `${GOOGLE_MAPS_API_KEY}` placeholder

    * **For iOS:**
        Open `ios/Runner/AppDelegate.swift` and add your key inside the `application` function:
        ```swift
        import UIKit
        import Flutter
        import GoogleMaps // Add this import

        @UIApplicationMain
        @objc class AppDelegate: FlutterAppDelegate {
          override func application(
            _ application: UIApplication,
            didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
          ) -> Bool {
            GMSServices.provideAPIKey("YOUR_KEY_HERE") // Add this line
            GeneratedPluginRegistrant.register(with: self)
            return super.application(application, didFinishLaunchingWithOptions: launchOptions)
          }
        }
        ```

    **Security Note:** The `local.properties` file is excluded from version control to keep your API keys secure.

4.  **Configure Permissions:**
    This app requires location permissions. The necessary keys are already included in `AndroidManifest.xml` and `Info.plist`, but ensure you understand them. The app will request "Always Allow" location access to function correctly when in the background.

5.  **Run the app:**
    ```sh
    flutter run
    ```

## ⚠️ Key Challenges & Considerations

* **Background Execution & Battery Optimization**: The biggest challenge is ensuring the background service is not killed by the OS, especially on heavily customized Android versions (like Xiaomi, Huawei, OnePlus). The app should guide users to disable battery optimization for GeoAlarm.
* **Permission Handling**: The app must gracefully handle cases where the user denies location permissions.
* **Reliability**: The alarm trigger logic must be robust and tested across various real-world scenarios (e.g., poor GPS signal, network loss).

## 🌿 Branching Strategy

- `main`: Stable release branch, every merge triggers a release
- `dev`: Development integration branch
- `feature/*`: Feature development branches

## 🤝 Contributing

1. Create a feature branch from `dev`
2. Submit a Pull Request to `dev` when complete
3. After testing stability, create PR from `dev` to `main`


