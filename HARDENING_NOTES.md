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
- Export now renders basic constant speed (with pitch preservation option), rotation, horizontal/vertical flip, clip positioning, transform keyframes (position/scale/rotation/crop), per-clip volume, mute, trims, still-image durations, crop, supported filters/adjustments, Gaussian blur, mirror, shake, comic-book/pencil-sketch/pop-art color treatments, deterministic film-grain/sparkle/anamorphic-flare/light-leak/lens-flare/bokeh textures, source-dependent GLES distortion effects (RGB split, digital glitch, signal error, datamosh, motion/radial/tilt-shift blur, kaleidoscope, fisheye, wave, pixelate, prism, and neon), static text/captions/stickers/drawings/frames, deterministic fade/scale/rotate/pulse/wave/swing animation for text and stickers, animated fade/scale image, GIF, and video overlays, photo/video-overlay chroma key, image overlays, crossfade/slide/zoom/spin/flip transitions between adjacent simple photo/video sources, photo-only wipe transitions, fade-to-black/white, separate audio tracks, audio volume keyframes/fade envelopes, four deterministic EQ choices, pitch shifting, bounded delay, room/hall reverb, and a bounded distortion waveshaper through Media3 effects/audio processors. Partial-duration legacy effects, slide/typewriter/mask/blend animations, complex transitions involving edited video sources, oil-painting and other unimplemented visual effects, noise reduction, voice effects, ducking, audio crossfade, and audio speed automation remain blocked.
- The newly exportable effects are validated only for full-clip timing; the editor reports a limitation when an effect is trimmed to a sub-range because the current Media3 effect API path cannot safely toggle those GPU effects per frame.
- Local usage analytics are now opt-in and disabling them clears the stored dashboard data; Delete All Local Data also clears the in-memory analytics snapshot. Cloud/device-transfer backup rules explicitly exclude local Clipp project state.
- `./gradlew.bat --no-daemon --no-configuration-cache :app:assembleRelease` was intentionally blocked with the expected signing-variable error; no keystore or passwords are present in the repository/session.
- Added device export regressions using generated local images: a single image re-exported as a video clip with speed/rotation/flip, plus a two-source image timeline that verifies order and combined duration. Media3 rendering, MediaStore publication, output reopening, non-zero size, and positive duration all pass on `small_phone` API 36.
- Added a device regression that creates two real MP4 sources, concatenates them as video clips, and verifies published order, combined duration, and readable output on `small_phone` API 36. Manual real-user import coverage and the broader device matrix remain pending.
- Added device regressions for the layered visual renderer and separate WAV audio mixing; both publish validated MP4 output with positive duration on `small_phone` API 36.
- Added a device regression for transform keyframes that verifies published output frames differ across the animation timeline on `small_phone` API 36.
- Added a device regression for supported text, sticker, and image-overlay animations that verifies published output frames differ across the animation timeline on `small_phone` API 36.
- Added device regressions covering photo- and video-overlay chroma-key export; pure pixel checks and a published MP4 check verify key-color removal and foreground preservation on `small_phone` API 36.
- Added a device regression covering a timestamped MP4 overlay: a red source segment and a green source segment are composited over a base project and the published output is checked at both timestamps on `small_phone` API 36. Overlay position/scale/rotation/opacity keyframes are validated before export.
- Added a device regression for deterministic film-grain, sparkle, and anamorphic-flare overlays; the published output frames differ across timestamps on `small_phone` API 36.
- Added a device regression for deterministic light-leak, lens-flare, and bokeh overlays; the published output frames differ across timestamps on `small_phone` API 36.
- Added a device regression for an animated two-frame GIF overlay; the published output is checked for its green and red frames at separate timestamps on `small_phone` API 36.
- Added local Music import and microphone Voiceover recording controls. Voiceover requires runtime microphone permission and stores recordings in the app's private files directory.
- Built-in local templates now replace selected media and persist a restorable editor history containing clips, text, supported effects, animations, and transitions. The `local_cinematic_story` template has a device regression that restores the saved state and publishes a valid MP4 on `small_phone` API 36. Remote template catalogs, AI-generated templates, cloud accounts, and billing remain disabled.
- Editor history serialization now explicitly supports Compose `Color` values and stores the canvas color bit pattern as a signed `Long`, preventing template/project saves from silently producing empty history JSON.
- Batch export now lets the user select saved local projects and renders them sequentially through the verified exporter, preserving completed outputs when a later project fails or the batch is cancelled. A device regression covers two saved image projects exported in one batch on `small_phone` API 36. Background batch execution, retry queues, and cloud/billing delivery are not included.
- Crop keyframes now render as a normalized per-frame zoom/pan transform, with validation for keyframe ranges and rectangle ordering. A device regression verifies that published frames switch between two crop regions on `small_phone` API 36.
- Added the first unified timeline architecture slice in `TimelineProject.kt`: a versioned canonical document now represents primary video, visual overlays, captions, stickers, drawings, frames, and audio as typed layers with shared timing, z-order, visibility/lock state, transforms, opacity, blend mode, keyframes, and volume properties. Legacy `EditorState` is converted at load/save/export boundaries so existing projects remain readable while the renderer migrates incrementally. Structural validation, Moshi persistence compatibility, legacy round-trip, and exporter-boundary regressions are covered by unit tests.
- Added the shared `TimelineRenderGraph.kt` frame planner: preview and export now resolve active layers, local time, visibility, keyframed transforms/opacity/volume, and deterministic visual z-order from the same canonical timeline. Preview uses the plan for the active primary clip and visual layer rendering; export iterates the same canonical visual-layer order. JVM regressions cover preview/export frame-plan parity and hidden/expired-layer exclusion.
- 2026-09-11 validation note: the canonical timeline change still passes the debug build and unit suite. The connected Android suite was attempted twice after starting `small_phone`, but the emulator lost its Android `package`/`activity` services before installation/test execution; both attempts ran zero tests and are not counted as device verification.
