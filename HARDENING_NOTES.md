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
- Added a Robolectric storage regression test proving app-owned thumbnails can be deleted without deleting referenced source media.
- Shortcut and deep-link actions now wait for the Home route, execute once, load the latest project from Room, and reject missing project IDs safely.
- Project recovery no longer treats opening an editor as an edit, and recovery prompts are limited to Home/Projects launch surfaces.
- `MediaClip` trim windows are clamped to the source duration before timeline mapping, playlist setup, and export.
- Device media import and real MP4 export remain unverified until a test media fixture is exercised on the emulator.
