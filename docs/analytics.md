# Analytics Integration (Optional)

GeoAlarm uses a pluggable analytics interface so telemetry can be removed quickly in forked projects.

## What is tracked

- `app_first_open`: Sent once for users entering onboarding for the first time.
- `analytics_opt_in`: Sent once when onboarding is completed and analytics is still enabled.
- `alarm_turn_off_completed`: Sent when the user turns off an alarm after arrival.

No location coordinates, alarm names, radius, schedule details, or destination text are sent.

## Configure TelemetryDeck

Add to `local.properties`:

```properties
telemetrydeck.appId=YOUR_TELEMETRYDECK_APP_ID
```

If the value is empty, analytics signals are skipped.

## Remove analytics in a fork

1. Remove dependency `com.telemetrydeck:kotlin-sdk` from `app/build.gradle.kts`.
2. Change DI binding in `RepositoryModule` from `TelemetryDeckAppAnalytics` to `NoOpAppAnalytics`.
3. Optionally delete the `analytics/` package and analytics-related settings strings.
