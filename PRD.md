# GeoAlarm PRD

Last updated: 2026-06-30

This file describes GeoAlarm product behavior, user flows, constraints, and business logic. Read this when changing features, flows, permissions, scheduling, notifications, alarms, widgets, privacy, analytics, or app actions.

For UI visual rules, read `DESIGN.md`. For coding and architecture rules, read `AGENTS.md`.

## Product Summary

GeoAlarm is an Android location-based alarm app for commuters. A user chooses a destination and radius, starts the alarm, and the app alerts them when they reach the target area. The product is built for real transit use: buses, trains, metro rides, naps, locked screens, background monitoring, and quick dismissal on arrival.

## Stable Scope

In scope:

- Location-based alarms.
- Alarm creation and editing.
- One active alarm at a time.
- Active alarm monitoring screen.
- Arrival notification, vibration, optional ringtone, and optional full-screen arrival alert.
- Alarm schedules for recurring commute routines.
- Home screen widget for quick alarm start.
- App shortcuts and app actions.
- Settings for language, ringtone, payment shortcut, privacy/analytics, and reliability-related permissions.
- Onboarding and permission explanation.

Prototype/deferred:

- Place reminders and reminder checklist flows.
- Current `PlaceReminder*`, `place_reminder_*`, and Reminders-tab behavior should not be treated as final product requirements.

## Users

- Commuters who may sleep or stop paying attention during transit.
- Users who want a destination-based wake-up instead of a time-based alarm.
- Users who repeatedly travel to the same places and need fast start/schedule flows.
- Users who care about privacy and battery impact from location monitoring.

## Core Value Proposition

GeoAlarm lets users rest during a ride and trust that their phone will alert them near the destination, while keeping location processing on device.

## Core Functional Requirements

### Alarms

An alarm has:

- name
- latitude
- longitude
- radius
- enabled/active state
- icon key

Requirements:

- User can create, edit, delete, and start saved alarms.
- User can choose destination through map selection, current location fallback, shared Google Maps place handling, or place search.
- User can adjust trigger radius.
- User can choose an alarm icon.
- User cannot edit an active alarm.
- Deleting an alarm bound to a schedule should be blocked or require schedule handling according to existing behavior.
- Only one alarm can be active at a time.

### Active Alarm

Requirements:

- Active alarm monitors current location against the destination radius.
- Foreground service notification must communicate that monitoring is active.
- Progress/distance should update as the user approaches.
- When the user arrives, the app should alert through notification, vibration, optional ringtone, and full-screen arrival screen when permitted.
- User can cancel before arrival.
- User can turn off after arrival.
- If a payment shortcut is configured, show the shortcut after arrival/turn-off according to existing behavior.

### Schedules

A schedule has:

- linked alarm
- days of week
- hour
- minute
- enabled state

Requirements:

- User can create, edit, delete, enable, and disable schedules.
- Schedule must be linked to an existing alarm.
- At the scheduled time, the system sends a confirmation notification.
- User must tap the notification to actually enable the alarm.
- Schedule should not silently start location monitoring without user confirmation.
- Exact alarm permission is required for reliable schedule notifications.
- The product should explain schedule behavior clearly because it is not a traditional silent automation.

### Permissions And Reliability

GeoAlarm must preserve clear flows for:

- precise location
- background location
- notifications
- exact alarms / alarms and reminders
- full-screen intent / full-screen arrival alert
- battery optimization guidance

Requirements:

- Explain why a permission is needed before asking or sending the user to Settings.
- Background location is requested only when needed for enabled alarms.
- Precise location is required for reliable alarm-area detection.
- Notifications are essential for arrival alerts.
- Exact alarm permission is required for schedules.
- Battery optimization warning should be contextual and not overstate guarantees.
- Full-screen arrival alert should be optional and explained as improving lock-screen dismissal.

### Onboarding

Requirements:

- Explain core value: rest during transit and get alerted near arrival.
- Explain dynamic monitoring: lower frequency when far away, higher precision near destination.
- Explain privacy: location data is processed on device and not uploaded to a GeoAlarm server.
- Support language toggle.
- Support optional analytics opt-in when applicable.
- Do not block basic app exploration more than necessary.

### Settings

Requirements:

- User can change language.
- User can configure ringtone mode/default/custom ringtone where supported.
- User can configure payment app shortcut if supported payment apps are installed.
- User can manage analytics/privacy preferences.
- User can open privacy policy.
- User can see app version/about information.
- User can configure or open full-screen alert permission when relevant.

### Widget

Requirements:

- User can configure a widget with up to two alarms.
- Widget should show useful empty/configured/active states.
- Widget should support quick start of selected alarms.
- If launcher pinning is unsupported, show an appropriate message.

### Shortcuts, App Actions, App Functions

Requirements:

- User can start create-alarm or create-schedule flows from app shortcuts where supported.
- App actions/functions should route through existing use cases and validation.
- External entry points must preserve permission, one-active-alarm, and validation rules.

### Analytics And Ads

Requirements:

- Analytics is optional/anonymous and must not include live location, saved destinations, alarm names, radius settings, or schedule details.
- User must be able to opt out.
- Ad/privacy options should be available when required by consent state.
- Product copy should avoid implying location data is uploaded to GeoAlarm servers.

## User Flows

### Create Alarm

1. User taps add alarm.
2. User selects destination on map or through search/share.
3. User adjusts radius.
4. User enters alarm name and chooses icon.
5. User saves.
6. Alarm appears on the home alarms list.

### Start Alarm

1. User taps Start on an alarm.
2. App checks precise location, notification, background location, exact active-alarm constraints, and related reliability conditions.
3. If another alarm is active, user must resolve the conflict.
4. Alarm starts and foreground monitoring begins.
5. User sees active alarm state and notification.

### Arrival

1. Device enters the alarm radius.
2. App triggers arrival alert.
3. User sees notification/full-screen screen where available.
4. User taps Turn Off.
5. Optional payment shortcut is offered if configured.

### Create Schedule

1. User taps add schedule.
2. User selects time.
3. User selects days.
4. User selects linked alarm.
5. User saves.
6. At scheduled time, app sends confirmation notification.
7. User taps notification to enable the linked alarm.

### Configure Widget

1. User opens widget picker/configuration.
2. User selects up to two alarms.
3. Widget displays selected alarms.
4. User taps an alarm from the widget to start it, subject to the same permission and one-active-alarm rules.

## Business Logic And Constraints

- Only one alarm can be active at once.
- Active alarms cannot be edited.
- Schedules depend on existing alarms.
- Schedules require user confirmation at trigger time.
- Location monitoring should be battery-aware.
- Destination data is local user data and should remain on device.
- Optional analytics must never include sensitive alarm or location details.
- Payment shortcut is optional and secondary to alarm dismissal.
- Traditional Chinese and English are supported UI languages.

## Non-Goals For Current Stable Scope

- Finalizing place reminder/checklist features.
- Multi-active-alarm monitoring.
- Server-side location tracking.
- Social sharing of live trips.
- Complex route planning.
- Marketing landing page inside the app.
- Replacing native Android permission flows with custom-only flows.

## Open Product Questions

- Should the Reminders tab be hidden until the feature leaves prototype?
- Should schedules ever support optional automatic alarm start, or remain confirmation-only?
- Should the fallback brand palette move away from Material default purple?
- How prominent should payment shortcut setup be outside Taiwan transit/payment contexts?
- Should alarm creation remain a two-step map/details flow or become a single collapsible map-first screen?
