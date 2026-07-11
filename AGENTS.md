# GeoAlarm Agent Guide

Last updated: 2026-06-30

This file is for coding agents working on GeoAlarm. Read this when changing Kotlin, Jetpack Compose implementation, architecture, tests, build files, navigation, data flow, or Git workflow.

For UI visual rules, read `DESIGN.md`. For product behavior and business rules, read `PRD.md`.

## Project

GeoAlarm is a Kotlin Android app using Jetpack Compose and Material 3. The app helps commuters create location-based alarms, start one active alarm, monitor progress in the background, and stop the alarm on arrival.

Treat current `PlaceReminder*`, `place_reminder_*`, and Reminders-tab work as prototype code unless the user explicitly asks to promote or modify it.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Compose Navigation with typed routes
- Hilt dependency injection
- Room for local alarm and schedule storage
- DataStore for preferences
- Google Maps SDK, Places SDK, Play services location
- Glance widgets
- JUnit tests and Android instrumentation tests

## Architecture

- Keep UI in `app/src/main/java/com/github/jimmy90109/geoalarm/ui`.
- Keep screens thin: collect state, render composables, and dispatch ViewModel actions.
- Keep business decisions in ViewModels, repositories, use cases, or service classes as appropriate.
- Prefer explicit UI state data classes and action/effect sealed types over scattered mutable state.
- Use `collectAsStateWithLifecycle` for ViewModel flows in composables.
- Keep route definitions in `navigation`.
- Keep database entities, DAOs, repositories, and converters in `data`.
- Keep platform integrations isolated in `service`, `receiver`, `widget`, `appactions`, `appfunctions`, `util`, or `utils`.
- Do not introduce parallel repositories or singleton state when an existing repository owns the behavior.

## Kotlin Rules

- Follow existing package structure and naming.
- Prefer immutable `val` and data classes.
- Use sealed interfaces/classes for UI actions, effects, route-like models, and result models when the state space is closed.
- Keep public APIs small and intentional.
- Prefer readable Kotlin over clever chained expressions when business logic is safety-sensitive.
- Use `runCatching` only when failure handling is explicit.
- Avoid hardcoded user-facing strings in Kotlin; put strings in resources.
- Keep comments sparse and useful. Add comments only where they explain non-obvious behavior, Android quirks, or reliability constraints.

## Compose Rules

- Reuse `GeoAlarmTheme`, existing typography, and Material 3 components.
- Hoist state unless the state is purely local UI affordance.
- Keep composables focused and name them after the UI concept they render.
- Use `Modifier` as the first optional parameter for reusable composables.
- Use `WindowInsets`, safe drawing, status bar, navigation bar, and cutout handling where controls approach screen edges.
- Keep touch targets at least 48dp.
- Prefer native controls: `Button`, `IconButton`, `Switch`, `Slider`, `TimePicker`, `ModalBottomSheet`, dialogs, Material cards, floating toolbars, and FAB menus.
- Use haptics for meaningful toggles, context actions, and time-picker ticks where existing patterns already do so.
- Use previews when practical for reusable components, but do not block small fixes on preview coverage.

## Naming

- Screens: `FeatureScreen`.
- ViewModels: `FeatureViewModel`.
- UI state: `FeatureUiState`.
- UI actions: `FeatureAction`.
- One-time effects: `FeatureEffect`.
- Repositories: `FeatureRepository`.
- DAOs: `FeatureDao`.
- Route models: keep under `AppRoutes` or `MainRoutes`.
- Resource strings: use clear feature prefixes when scope is not obvious, such as `widget_*`, `settings_*`, `payment_shortcut_*`.

## Resources And Localization

- Add or update English strings in `app/src/main/res/values/strings.xml`.
- Add or update Traditional Chinese strings in `app/src/main/res/values-zh-rTW/strings.xml`.
- Keep Chinese natural for Taiwan users.
- Avoid overly long button labels; test mentally against narrow phones and larger font sizes.
- Do not hardcode copy in composables except temporary previews.

## Testing

Add or update tests when changing:

- ViewModel logic
- repository behavior
- parsing
- scheduling
- permission-gated behavior
- business rules such as one-active-alarm constraints
- widget selection encoding
- app action parsing/use cases

Useful commands:

```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

Run the narrowest meaningful test first, then broaden if the change touches shared behavior. If tests cannot be run, state why in the final response.

## Git Flow

- Preserve user changes. The worktree may already contain prototype or unrelated edits.
- Do not revert files you did not intentionally change.
- Do not run destructive Git commands unless explicitly requested.
- Default branch naming for new agent branches should use the `codex/` prefix.
- Prefer small commits with focused messages when the user asks for commits.
- Do not stage or commit without an explicit user request.

## Reliability Constraints

GeoAlarm depends on Android background behavior. Changes must preserve:

- precise location permission flow
- background location permission flow
- notification permission flow
- exact alarm permission flow
- foreground service behavior
- full-screen arrival alert behavior
- battery optimization guidance
- one-active-alarm-at-a-time logic
- local-only handling of saved destination data

When in doubt, read `PRD.md` before changing behavior.
