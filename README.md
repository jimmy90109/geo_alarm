# GeoAlarm - A Location-Based Alarm Clock

![Downloads](https://img.shields.io/github/downloads/jimmy90109/geo_alarm/total)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/jimmy90109/geo_alarm)
![GitHub last commit](https://img.shields.io/github/last-commit/jimmy90109/geo_alarm)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)

![Min SDK](https://img.shields.io/badge/Min%20SDK-31%2B-orange.svg)
![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)

An intelligent alarm clock app built with **Kotlin** and **Jetpack Compose** that triggers an alarm when you enter a predefined geographical area. Perfect for commuters who want to nap on the train or bus without worrying about missing their stop.

## 🌟 Core Features

* **📍 Set Destination via Map**: Easily set your destination by tapping on an interactive Google Map.
* **🔍 Search for Destinations**: Find your destination quickly by searching for addresses, landmarks, or station names.
* **⭕ Adjustable Trigger Radius**: Define a geofence by setting a radius around your destination.
* **🔔 Background Monitoring**: The app reliably tracks your location in the background using a foreground service.
* **🔊 Intrusive Alarm**: When you enter the target area, a vibrating alarm is triggered.
* **🗂️ Alarm Management**: Save, view, edit, and delete your alarms in a clean and simple list.
* **Toggle On/Off**: Easily activate or deactivate any saved alarm with a single switch.
* **🎵 Ringtone Customization**: Choose your preferred ringtone or search for new ones.
* **🎧 Intelligent Audio Output**: Plays arrival sound through headphones if connected to avoid public disturbance; otherwise vibrates.
* **🫥 Optional Anonymous Usage Analytics**: Privacy-first telemetry for product stability insights, with an in-app opt-out switch.
* **🌐 Multi-language**: Supports English and Traditional Chinese (繁體中文).

## 🛠️ Tech Stack & Architecture

This project is built using modern Android development practices with a clean architecture.

### Technology
* **Language**: Kotlin
* **UI Framework**: Jetpack Compose with Material Design 3
* **Architecture**: MVVM with Repository Pattern
* **Database**: Room (SQLite)
* **Dependency Injection**: Manual DI via Application class
* **Maps**: Google Maps SDK for Android
* **Background Service**: Foreground Service for reliable location tracking

### Directory Structure
```
app/src/main/java/com/github/jimmy90109/geoalarm/
├── data/
│   ├── Alarm.kt              # Data entity
│   ├── AlarmDao.kt           # Room DAO
│   ├── AlarmRepository.kt    # Repository
│   ├── AppDatabase.kt        # Room database
│   └── RingtoneSettings.kt   # Ringtone preferences
│
├── service/
│   └── GeoAlarmService.kt    # Foreground service for location monitoring
│
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt     # Main alarm list screen
│   │   ├── AlarmEditScreen.kt # Create/Edit alarm screen
│   │   └── SettingsScreen.kt # Settings & Ringtone config
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── utils/
│   └── AudioUtils.kt         # Audio focus & output management
│
├── GeoAlarmApplication.kt    # Application class
└── MainActivity.kt           # Main entry point
```

## 🚀 Getting Started

Follow these instructions to get the project up and running on your local machine.

### Prerequisites

* Android Studio (latest version recommended)
* Android SDK 35+
* A Google Maps Platform API Key

### Installation & Setup

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/jimmy90109/geo_alarm.git
    cd geo_alarm
    ```

2.  **Configure API Keys:**
    You need to add your Google Maps API key. Make sure you have enabled **Maps SDK for Android** in your Google Cloud Console.

    1. Copy `local.properties.example` to `local.properties` (or create a new one)
    2. Add your Google Maps API key:
       ```properties
       MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE
       ```

    **Security Note:** The `local.properties` file is excluded from version control to keep your API keys secure.

3.  **Build and Run:**
    Open the project in Android Studio and run on an emulator or physical device.

4.  **(Optional) Configure TelemetryDeck analytics:**
    ```properties
    telemetrydeck.appId=YOUR_TELEMETRYDECK_APP_ID
    ```
    Setup and removal guide: `docs/analytics.md`

## ⚠️ Key Challenges & Considerations

* **Background Execution & Battery Optimization**: The biggest challenge is ensuring the foreground service is not killed by the OS, especially on heavily customized Android versions (like Xiaomi, Huawei, OnePlus). The app should guide users to disable battery optimization for GeoAlarm.
* **Permission Handling**: The app must gracefully handle cases where the user denies location permissions.
* **Reliability**: The alarm trigger logic must be robust and tested across various real-world scenarios.

## 🌿 Branching Strategy

- `main`: Stable release branch
- `dev`: Development integration branch
- `feature/*`: Feature development branches

## 🤝 Contributing

1. Create a feature branch from `dev`
2. Submit a Pull Request to `dev` when complete
3. After testing stability, create PR from `dev` to `main`

## 📝 Note

This project was originally built with Flutter and has been rewritten in pure Kotlin with Jetpack Compose for better Android platform integration and performance. The original Flutter code is available in the Git history.
