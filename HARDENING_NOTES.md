# Clipp production hardening

## Baseline — 2026-09-06

- Source arrived without Git metadata. A local `main` baseline was created at `ea8be73`.
- Hardening work is on `audit/production-hardening-2026-09-06`.
- Gradle 9.4.1 wrapper was generated and the Android SDK was verified locally.
- Clean debug build: `./gradlew.bat clean :app:assembleDebug` — passed after removing the missing custom debug-keystore dependency.
- Baseline debug APK: 28,641,471 bytes (`app-debug.apk`).
- Baseline unit tests: blocked by the template `GreetingScreenshotTest` referencing a missing `Greeting` composable.
- Hardware baseline: `small_phone` AVD is installed; no emulator/device was connected during this run, so cold-start and memory measurements remain pending.
- Dependency report: `reports/baseline-debugRuntimeClasspath.txt`.
- `.env` is absent and `.env` is ignored by Git. No credentials were added.
- `grep_out.txt` and `temp.txt` were confirmed unreferenced and removed on the hardening branch.

## Scope decisions

- The launch baseline is local and account-free. Cloud sync, account login, billing, AI processing, and unsupported export formats must not claim completion until their real backends/pipelines exist.
- The current Kotlin namespace remains `com.example` temporarily to keep the large source tree build-safe. The application ID remains `com.clipp.ai.video.editor.maker`; package migration should be isolated in a later commit.

## Validation — 2026-09-07

- Process-local Android SDK setup: `C:\Users\prem7\AppData\Local\Android\Sdk`.
- `./gradlew.bat clean testDebugUnitTest assembleDebug` — passed.
- `./gradlew.bat testDebugUnitTest assembleDebug` after trim/recovery changes — passed.
- `./gradlew.bat connectedDebugAndroidTest` on the `small_phone` API 36 AVD — passed.
- Debug APK installed and `com.example.MainActivity` launched on `small_phone`; process stayed alive with no fatal startup exception in the sampled logcat.
- After the export capability update, `./gradlew.bat --no-daemon --max-workers=2 testDebugUnitTest assembleDebug` — passed; `connectedDebugAndroidTest` — passed; the rebuilt APK installed and `MainActivity` remained alive on the API 36 emulator with no sampled fatal startup exception.
- Added a Robolectric storage regression test proving app-owned thumbnails can be deleted without deleting referenced source media.
- Shortcut and deep-link actions now wait for the Home route, execute once, load the latest project from Room, and reject missing project IDs safely.
- Project recovery no longer treats opening an editor as an edit, and recovery prompts are limited to Home/Projects launch surfaces.
- `MediaClip` trim windows are clamped to the source duration before timeline mapping, playlist setup, and export.
- Export now renders basic constant speed (with pitch preservation option), rotation, horizontal/vertical flip, clip positioning, transform keyframes (position/scale/rotation), per-clip volume, mute, trims, still-image durations, crop, supported filters/adjustments, Gaussian blur, static text/captions/stickers/drawings/frames, deterministic fade/scale/rotate/pulse/wave/swing animation for text and stickers, animated fade/scale image overlays, photo-overlay chroma key, image overlays, fade-to-black/white, separate audio tracks, and audio volume keyframes/fade envelopes through Media3 effects/audio processors. Crop-animation keyframes, slide/typewriter/glitch/mask/blend animations, video overlays/chroma key, unsupported transitions, and advanced audio processors (EQ, reverb, delay, pitch effects, ducking, and crossfade) remain blocked.
- Local usage analytics are now opt-in and disabling them clears the stored dashboard data; Delete All Local Data also clears the in-memory analytics snapshot. Cloud/device-transfer backup rules explicitly exclude local Clipp project state.
- `./gradlew.bat --no-daemon --no-configuration-cache :app:assembleRelease` was intentionally blocked with the expected signing-variable error; no keystore or passwords are present in the repository/session.
- Added device export regressions using generated local images: a single image re-exported as a video clip with speed/rotation/flip, plus a two-source image timeline that verifies order and combined duration. Media3 rendering, MediaStore publication, output reopening, non-zero size, and positive duration all pass on `small_phone` API 36. Real user-selected video-file and multi-video video-source coverage remain pending.
- Added device regressions for the layered visual renderer and separate WAV audio mixing; both publish validated MP4 output with positive duration on `small_phone` API 36.
- Added a device regression for transform keyframes that verifies published output frames differ across the animation timeline on `small_phone` API 36.
- Added a device regression for supported text, sticker, and image-overlay animations that verifies published output frames differ across the animation timeline on `small_phone` API 36.
- Added a device regression covering the chroma-key export path for a photo overlay alongside supported layer animations on `small_phone` API 36; the pure pixel test verifies key-color removal and foreground preservation. Video-overlay chroma key remains intentionally blocked.
- Added local Music import and microphone Voiceover recording controls. Voiceover requires runtime microphone permission and stores recordings in the app's private files directory.
