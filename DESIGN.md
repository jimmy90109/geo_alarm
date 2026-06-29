# GeoAlarm Design System

Last updated: 2026-06-30

This file is for UI and visual design work. Read this when changing Compose UI, Stitch/Figma mockups, component styling, motion, spacing, iconography, layout, or accessibility.

For product behavior, read `PRD.md`. For coding and architecture rules, read `AGENTS.md`.

## Scope

The stable design baseline is the core GeoAlarm experience: alarms, alarm creation/editing, active alarm, schedules, settings, onboarding, widgets, and shortcuts.

## Design Principles

- Calm, practical, and trustworthy.
- Native Android first.
- Reliable under real commuting conditions.
- Clear enough to use while tired, rushed, or on a moving vehicle.
- Privacy-forward without sounding alarming.
- Bilingual-ready for English and Taiwan Traditional Chinese.

## Material 3

- Use Jetpack Compose Material 3 as the base.
- Prefer platform components over custom controls.
- Use `LargeFlexibleTopAppBar` or Material 3 top app bars for primary screens.
- Use Material cards only for functional grouped content: alarm rows, schedule rows, settings groups, native ad cards, and guide prompts.
- Use dialogs for blocking decisions and errors.
- Use modal bottom sheets for selectors, explanations, and optional setup.
- Use floating toolbar navigation on phones and navigation rail/floating vertical toolbar in landscape/tablet.
- Use FAB or FAB menu for creation actions.

## Color

Current implementation:

- Android 12+ dynamic color is enabled.
- Light fallback uses Material purple defaults.
- Dark fallback uses Material purple/pink defaults.
- Dark map styling may be used for map surfaces in dark mode.

Design direction:

- Dynamic color compatibility comes first.
- Future fallback palette should feel calm and transit/location-oriented, not decorative.
- Do not make the app dominated by one saturated hue.
- Avoid heavy purple gradients, marketing-style gradient backgrounds, and decorative color blobs.
- Use red/error colors only for true errors or destructive states.
- Use primary/primary-container for selection and important actions.
- Use surface/surface-container for cards and grouped controls.
- Use outline or dashed outline for lightweight guide prompts.

## Typography

Current implementation uses Google Sans through the Compose theme.

Rules:

- Use Material 3 typography roles.
- Use large display text only for true high-focus states, especially active alarm distance/arrival state.
- Use `titleLarge` or `titleMedium` for card titles and section headings.
- Use `bodyMedium` or `bodyLarge` for supporting explanations.
- Avoid shrinking text with viewport width.
- Keep letter spacing from the theme; do not add custom negative tracking.
- Test English and Traditional Chinese labels for truncation.

## Shape And Radius

- Keep most cards and controls aligned with Material 3 defaults unless the existing component requires otherwise.
- Functional cards should feel compact and native, not oversized.
- Guide cards may use 12dp rounded dashed outlines.
- Large active-alarm controls can use more generous shape if it improves touch ergonomics.
- Avoid nested cards.

## Spacing

Use an 8dp-based rhythm:

- 4dp for tiny internal offsets.
- 8dp for compact icon/text spacing.
- 12dp for grid gaps and medium internal rhythm.
- 16dp for standard screen padding and card padding.
- 24dp for major section spacing.

Existing alarm/schedule grids use adaptive columns around 300dp minimum width with 12dp gaps. Preserve that feel unless redesigning the whole list.

## Layout

Phone portrait:

- Single main column.
- Bottom floating navigation.
- Primary FAB near bottom end.
- Map controls should stay reachable near the bottom.

Landscape/tablet:

- Navigation rail or vertical floating toolbar on the side.
- Wider content uses adaptive grids or two-column settings layouts.
- Map/detail workflows may move controls into a side panel.

General:

- Respect status bars, navigation bars, display cutouts, and safe drawing insets.
- Keep primary controls away from system gesture zones.
- Constrain settings/form content width when full width would reduce readability.
- Touch targets should be at least 48dp.

## Icons

- Use Material Icons already available in the project unless a specific custom asset exists.
- Icon-only buttons require content descriptions.
- Alarm identity should use the existing alarm icon badge pattern.
- Navigation icons should remain simple and recognizable: alarm, settings, and any promoted top-level feature.
- Avoid custom decorative icons unless they communicate a functional state.

## Components

### Navigation

- Selected tab uses primary container treatment.
- Unselected tabs should remain quiet and legible.
- Labels should be short.
- Reminders should not define the stable nav design until the feature leaves prototype.

### FAB

- Use a large FAB for the primary add action.
- Use an expanding FAB menu when both new alarm and new schedule are available.
- Creation actions should include icons.

### Alarm Card

- Show icon badge, alarm name, and a clear Start button.
- The Start button should be visually obvious.
- Card click opens edit/details when allowed.

### Schedule Card

- Show time/day summary, linked alarm name, and enabled switch.
- Use switch only for enabled/disabled state.
- Keep schedule confirmation behavior explained elsewhere; do not overload the card.

### Settings Row/Card

- Group by General, Alarm, Privacy & Improvements, and About.
- Show concise title and current value/status.
- Use switches for binary settings.
- Use bottom sheets for selectors.

### Active Alarm

- Full-screen, not card-based.
- Distance/progress is the dominant visual signal.
- Use calm breathing/expanding motion while monitoring.
- Arrival state should make Turn Off large and obvious.
- Payment shortcut prompt is secondary.

### Maps

- Map is a primary work surface.
- Destination marker and radius must be clear.
- Search controls should not hide the selected destination/radius for long.
- Radius sliders should provide immediate visual feedback.

## Motion

Use motion to clarify state:

- FAB menu expansion/collapse.
- List highlight after returning from edit or creation.
- Active alarm progress and arrival transition.
- Onboarding scene transitions.
- Loading/search transitions.

Motion should be calm, short, and purposeful. Avoid decorative looping motion except the active alarm breathing/progress affordance.

## Accessibility

- All icon-only actions need content descriptions.
- Do not rely on color alone for selected, disabled, warning, or error states.
- Text must scale without clipping at common accessibility font sizes.
- Primary stop/dismiss controls must be large and reachable.
- FAB menus and navigation should support accessibility traversal.
- Maintain contrast in dynamic color, light mode, and dark mode.

## Content Style For UI

English:

- Sentence case for most labels.
- Short buttons: Start, Save, Cancel, Turn Off, Retry.
- Avoid long navigation labels.

Traditional Chinese:

- Natural Taiwan Traditional Chinese.
- Prefer terms such as 定位鬧鐘, 到站, 提醒, 前往設定, 背景監控.
- Short buttons: 開始, 儲存, 取消, 關閉, 重試.

Tone:

- Reassuring, direct, and commuter-aware.
- Explain permission needs before system prompts.
- Avoid noisy technical telemetry in normal user-facing states.
