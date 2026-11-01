# Changelog

## Editor playback repair — 2026-10-17

- Editor stability repair: track playback on Android's native clock, refresh playback mapping after project load; use texture-based preview surfaces with fullscreen cleanup; support restored timelines without legacy paths; pause players in the background and reduce budget-mode decoder load.
- Remove duplicate system-inset padding across navigation, unify recovery prompts, skip repeated startup guidance, and keep playback times readable on narrow screens.
- Add local stack-trace diagnostics and live editor playback regression coverage. Physical-phone confirmation remains pending.
## Export quality controls — 2026-10-17

- Added device-checked 480p, 720p, 1080p, and 4K export options with 24/30/60 fps limits and selectable target video bitrates.
- Wired selected dimensions, photo frame rate, video frame reduction, and video/audio bitrate requests into MP4 export. Explicit quality requests do not silently fall back to a lower resolution.
- Explicit quality exports use the project's canvas aspect ratio, including portrait and square output, with black padding. Custom fill/stretch modes and color/gradient backgrounds remain blocked.
- Added an approximate output file size and a concise export summary; replaced the export screen's implementation inventory with user-facing controls and actionable errors.
- Fixed fractional frame-rate conversion such as 60 to 24 fps while retaining original timestamps and lower-rate source footage.
- Preserved the existing automatic export path for callers that do not supply quality settings, including foreground batch export.

## Hardening branch — 2026-09-06

- Added a recoverable local Git baseline and Gradle wrapper.
- Removed the missing custom debug-keystore dependency.
- Started production hardening for truthful local-only editing and export behavior.
