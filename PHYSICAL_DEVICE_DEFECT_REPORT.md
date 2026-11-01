# Clipp: physical-device editing failures

Reported: 2026-10-17. Status: **Repairs implemented and verified on API 34; affected-phone retest and crash-cause confirmation pending.**

The user imported a video to edit on a physical phone and reported a dark preview, video frames that do not move when playback is attempted, and the app crashing. These are failures in the essential editing workflow. Passing export tests does not make this workflow usable.

## Implemented repair pass (2026-10-17)

Application changes now use a real Android main-looper position ticker, refresh the mapper after asynchronous project loading, and address SurfaceView output under Compose effects, legacy-path-only preview gating, duplicate status-bar padding, repeated media-privacy entry, overlapping recovery owners, wrapped transport labels, and crash-log key/stack loss. Budget mode uses a still blurred backdrop instead of a second active video decoder. Fullscreen surfaces detach when removed, and backgrounding pauses editor players.

Regression coverage now drives a small generated moving-video fixture through the actual editor and checks rendered TextureView pixels, clock progress, pause/seek, restoration without legacy paths, fullscreen return, lifecycle pause, and a single startup recovery/inset. An intermediate live test caught the clock still showing 00:00 with Media3 at 2,413 ms; the final implementation therefore uses Android real-time callbacks and authoritative player state, rather than relying on the Compose polling coroutine. Final verification is recorded below. The user's phone has not been re-tested; PHY-003's original crash cause remains unconfirmed.

## Repair verification and APK

- `:app:testDebugUnitTest`: **79 tests**, zero failures/errors/skips, after the final production-code change.
- Unfiltered API 34 Android run: **36/37 passed**. All **34 existing regression cases** passed; one new editor test selected the header Back button instead of the transport rewind button.
- After correcting the test to select the lowest clickable Back control and wait for the seek result, the final focused `EditorPlaybackInstrumentedTest` run passed **3/3**, zero failures/errors/skips (`BUILD SUCCESSFUL`). Production app code/APK did not change between these runs.
- Live coverage: imported moving H.264 video; nonblack initial texture and changing decoded frames; nonzero editor clock; pause/rewind with the native player at zero; restored canonical history without legacy paths; nonblack composed window pixels via PixelCopy; fullscreen exit/return; background pause/resume; one recovery dialog; returning-user startup; one status-bar inset on Home.
- The generated 30-second fixture contains no user recording and is packaged only in the test APK. Emulator: Android 14 / API 34, 360 x 800 at 160 dpi.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, **27,925,940 bytes**, SHA-256 `8b74501cd833c595fbc884fcf6fe05abb9276b3fa47d383e1046fadaa7a7e37c`.
- Machine-readable evidence: `reports/physical-test-2026-10-17/repair-verification.json`. Full regression and final focused logs: `reports/playback-final-regression.log`, `reports/playback-editor-final.log`.
- Preserved rendered-window evidence: `reports/physical-test-2026-10-17/editor-playback-verified.png` from the unfiltered run on this same production APK. That standalone test activity did not use the app's root navigation; the top-inset claim is verified by the separate actual `ClippApp` startup test, not inferred from this image. The final editor tests also enable edge-to-edge and pass their PixelCopy assertions; their app-owned image was removed by test-run cleanup.
- **Physical status:** the user's affected phone and original imported media have not been re-tested. PHY-003's exact crash cause is still unknown. Resource cleanup and stack-trace diagnostics are implemented; emulator success does not close the physical-device crash report.
- **Additional narrow-screen follow-up:** the 360dp capture exposes a crowded editor header: the project title has little room and the Export label can wrap. This remains a separate layout polish issue; the playback-time wrapping and duplicated system inset have been repaired.

## Evidence and limits

- **Physical-test evidence:** the user's report plus review of the supplied replacement screen recording. The app has not been independently operated on that phone in this session.
- **Recording reviewed:** `C:/Users/prem7/Downloads/WhatsApp Video 2026-10-17 at 2.55.08 PM.mp4`. It is approximately 63.05 seconds long; the recording contains H.264 video at 576 x 1280 and an AAC audio stream. These are recording properties, not the imported editing clip's properties.
- Review method: decoded the recording and inspected 32 sampled frames at approximately two-second intervals, full-size frames for the editor/recovery state, and half-second samples around recovery-dialog overlap and the return to the launcher. Timestamps below are recording elapsed time. Audible audio and the phone's crash logs were not assessed.
- The initially supplied `2.49.14 PM.mp4` path became unavailable; the user supplied the replacement above, which was successfully reviewed.
- The supplied MP4 was intended as evidence of the test. It is not assumed to be the original clip imported into Clipp.
- Phone model, Android version, RAM, installed APK/build identity, original clip format, and crash logs are currently unknown.
- The previous 76 passing JVM/UI tests and 34 passing API 34 emulator tests remain scoped evidence for the cases they exercised. They do not establish reliable interactive video editing on this phone.

## Release-blocking failures reported on the phone

These are separate symptoms; they may share an underlying cause. The table preserves the original physical observations; code repairs and their verification are recorded separately above.

| ID | Priority | Failure | User impact | Evidence |
| --- | --- | --- | --- | --- |
| PHY-001 | P0 | Imported video has a dark/black preview instead of visible video | The user cannot see the material being edited | User report and recording, about 38-54s |
| PHY-002 | P0 | Video frames do not advance when playback is attempted | The user cannot review timing, cuts, motion, or synchronization | User report; recording remains black rather than showing progressing footage |
| PHY-003 | P0 | App crashes during basic video editing | The editing session is interrupted; continuity and unsaved edits are at risk | User report; recording shows editor-to-launcher transition about 55.5s and relaunch about 58s, without proving the exit cause |
| PHY-004 | P0 | Playback clock/playhead stay at the start while Pause is displayed | Playback state and editing position cannot be trusted | Recording, sampled frames about 40-48s show 00:00 and the start playhead |

## Additional visible usability failures

| ID | Priority | Failure | Recording evidence | Expected |
| --- | --- | --- | --- | --- |
| PHY-005 | P1 | Two recovery flows overlap/cross-fade their modal content | About 20.0s: “We recovered your last session” overlaps “Recover Unsaved Project” | One coordinated recovery decision with clear outcomes |
| PHY-006 | P2 | Total-duration label wraps incorrectly | Editor frames around 40-54s display `00:4` with `3` on the next line instead of `00:43` | Keep the complete time readable on one line on the tested display/font settings |
| PHY-007 | P2 | Re-entering the app repeats the media-privacy Continue interstitial | Same “Choose media privately” screen appears about 4-14s and again about 62s after relaunch | Returning users can reach their projects/recovery directly; keep privacy guidance accessible without an unnecessary repeat gate |
| PHY-008 | P2 | Extra blank space above screens | Additional explicit user request after recording review; root navigation applied system padding without consuming it before child scaffolds | Apply the status-bar inset once, retaining safe system-bar clearance |

PHY-007 is a repeated startup-navigation behavior, not evidence that Android revoked media permission. The recording does not establish that recovery lost the project or that any export failed.

### Recording event log

| Approximate time | What is visible | Interpretation limit |
| --- | --- | --- |
| 4-14s | Media-privacy information screen with Continue | Time spent on this screen is not by itself proof of a hang |
| 16-19.5s | Session-recovery prompt | Supports that the app recorded an earlier unexpected close, not its cause |
| 20.0s | Recovery prompts overlap | Captured UI overlap; the exact overlap duration is not established |
| 20.5-22s | Unsaved-project recovery prompt | A second recovery flow is present |
| 24-30s | Existing `Project_Oct17` card, duration 00:43 | This is a saved-project/recovery test, not a demonstrated fresh import in this recording |
| 32-34s | “Budget Device Mode” toast | Does not establish phone model or RAM |
| 36s | Editor opens with a video placeholder | Brief initial loading alone is not counted as a failure |
| 38-54s | Preview is black; timeline thumbnails are visible by 40s | Thumbnails do not prove the preview decoder/surface works |
| 40-48s | Pause icon and bright level bars, but clock remains 00:00 and the playhead stays at the start | Confirms inconsistent/non-progressing playback UI; does not prove audible audio |
| About 42s | “Select a video clip first” toast before yellow clip selection appears | Not counted as a defect: the clip may legitimately have been unselected |
| 50-54s | Play icon returns; preview remains black | Pausing does not restore a visible frame in these samples |
| 55.0-55.5s | Editor changes to phone launcher | Cannot distinguish crash, OS termination, or manual exit from pixels alone |
| 58-60s | App splash on relaunch | Relaunch is visible |
| About 62s | Media-privacy information screen again | Repeat gate, not an Android permission-dialog observation |

Preserved evidence frames:

- [Recovery overlap at 20s](reports/physical-test-2026-10-17/recovery-overlap-20s.png)
- [Black preview and playback controls at 40s](reports/physical-test-2026-10-17/black-preview-40s.png)
- [Playback still at 00:00 at 48s](reports/physical-test-2026-10-17/stalled-playback-48s.png)

### PHY-001: black video preview

- Reported flow: import/select a video, enter the editor, attempt to view or play it.
- Actual: the user sees a dark screen where video should appear.
- Expected: a visible decoded first frame, followed by playback; if the clip cannot be opened, an actionable error with retry or replacement guidance.
- The recording demonstrates the issue while reopening an existing approximately 43-second project. It does not show a fresh media import. Whether every clip/format is affected and whether audio can still be heard remain unknown.
- Required diagnosis: record media/decoder errors, player readiness, first-frame events, surface attachment, selected video track, and project source URIs.

### PHY-002: playback without moving video

- Reported flow: attempt to play the imported video in the editor.
- Actual: the displayed video does not move.
- Expected: advancing video frames and a playhead/time display consistent with playback. Pause, resume, and scrubbing must show the corresponding frame.
- In the reviewed recording the time display and playhead do not advance in the sampled playing state around 40-48s. Audible audio remains unverified. Because the preview is black, the recording does not distinguish a frozen decoded frame from no decoded video reaching the surface.
- Required diagnosis: distinguish buffering/decoder failure, missing video output, repeated preparation/seeking, UI state mismatch, and a genuinely stalled app. Do not assume that a changed Play/Pause icon proves playback.

### PHY-003: crash during basic editing

- Reported flow: start editing a video and encounter the playback problems; the app also crashes.
- Actual: the editing session fails. The exact crashing tap and sequence are not yet established.
- Expected: basic import, playback, pause, scrub, and editing remain stable. Unsupported media should produce a recoverable error.
- The recording shows a transition to the launcher between approximately 55.0 and 55.5 seconds, followed by relaunch. It has no visible stack trace or system crash dialog. Exception class, native crash versus Java/Kotlin exception, out-of-memory termination, ANR/process kill/manual exit, and preservation of the latest edits remain unconfirmed.
- Required diagnosis: capture the phone's crash/logcat evidence, actual build identity, original source clip metadata, and the exact last action before failure. Check recovery separately; data loss has not been confirmed.

## Additional gaps confirmed in source code

These are source findings, not additional claims about what happened in the recording.

### SRC-001: no main-preview playback-error handling (P1)

`app/src/main/java/com/example/EditorScreen.kt`, around lines 1044-1059:

- The main `Player.Listener` handles `onIsPlayingChanged` and end-of-playback.
- It does not implement `onPlayerError`, buffering/ready state handling, or a first-frame callback in that listener.
- A decoder or media-open failure is therefore not connected through this listener to an actionable editor error state.
- Closure: show useful loading/error/retry states, record the actual error code/cause, and verify failure recovery on the phone.

### SRC-002: crash-report preference keys do not match (P1)

- `MainActivity.kt:101` saves the local error message under `crash_log`.
- `ContactSupportScreen.kt:36` reads `last_crash_log` and falls back to “No recent crashes recorded.”
- The support report can miss the message saved by the crash handler. This is a confirmed code inconsistency; it is not the cause of the crash itself.
- Closure: use a single shared crash-record format/key and prove that a captured test failure appears in the local support report. Respect the user's crash-recording preference.

### SRC-003: persisted crash information is too limited (P1)

- The local handler saves `throwable.message`, with an “Unknown error” fallback, rather than a full stack trace and cause chain.
- That local record does not provide the failing code location or the player/decoder context needed for these failures. Android's platform logs may still contain the full crash.
- Closure: retain bounded local diagnostic details, including build/device context and cause/stack information, and make them available for explicit user review/sharing. Do not send reports automatically.

### QA-001: existing test coverage does not establish interactive editor playback (P0 release gate)

- Connected tests are in `ExportInstrumentedTest.kt` plus `ExampleInstrumentedTest.kt`; the latter checks the application package name.
- Exported-frame checks exercise the exporter. The quality-control UI tests exercise export choices.
- These tests do not establish the real-phone sequence “import a real video, render it in the editor, play, pause, scrub, resume, and continue editing without crashing.”
- Closure: add tests for the actual editor/player surface and run the physical-device acceptance checks below. Keep export and interactive-preview results distinct in status reports.

### SRC-004: displayed level bars are not measured audio (P1 diagnostic/UX gap)

- `EditorScreen.kt:248-279` derives the five bars from `isPlaying`, mute, and master volume. It does not measure decoded audio samples.
- The bright bars seen with a black preview and 00:00 therefore cannot establish that sound or media position is advancing. They are currently a gain/play-state indicator rather than an audio-level meter.
- Closure: label the indicator accurately or drive a real meter from measured audio; keep buffering/stall/error state distinct from the Play/Pause icon.

### SRC-005: competing recovery controllers and repeat entry gate (P1/P2)

- `HomeScreen.kt:96-118` owns a crash/session-recovery prompt, while `MainActivity.kt:505-540` independently owns an unsaved-project recovery dialog. The recording shows their overlap around 20s.
- `MainActivity.kt:267` routes users who completed onboarding back to `permissions` on splash completion; the `permissions` route requires Continue before Home. This explains the repeated informational entry gate without implying media permission was lost.
- Closure: coordinate recovery in one owner and simplify subsequent app launches.

## Investigation candidates: not established causes

0. **Playback ticker can capture an outdated timeline mapper.** `EditorScreen.kt:784` recreates the immutable `TimelineMapper` when clips change, but the playback polling coroutine around lines 1296-1349 is keyed only by `Unit` and closes over that mapper. Project media/history loads asynchronously. `TimelineMapper.globalPositionForPlayer` returns zero for an absent segment. Verify whether the running coroutine retains the initial empty/old mapper; this is a specific candidate for the observed 00:00 playhead stall, not a demonstrated cause of the black surface or crash. A repair should restart with the current timeline or explicitly read an updated mapper and cover restoration/editing in tests.
1. **Multiple video players and decoder pressure.** The editor creates main, background, and overlay ExoPlayers around lines 1035-1091. The main and background playlists are both prepared around lines 1233-1237, and background playback is synchronized around lines 1296-1308. Measure decoder instances and memory on the affected phone; isolate primary playback with the other players disabled before attributing the crash to this design.
2. **Preview surface and Compose rendering.** The main `PlayerView` is wrapped in color-filter drawing, effects, clipping, and `graphicsLayer` transforms around lines 2154-2235. The blurred background is another `PlayerView`. Verify actual surface type, first-frame delivery, and surface behavior on the phone; compare an unmodified single-player view with the composed preview. No surface-related root cause has been proven.
3. **Preview depends on legacy project paths.** Normal and fullscreen preview branches check `project?.sourceMediaPaths?.firstOrNull()` around lines 2033 and 7174. Audit restored projects where current timeline clips have valid sources but that legacy list is empty. This conditional can omit the preview branch in that state; whether the reported project had that state is unknown.
4. **Lifecycle and repeated player changes.** Player release is tied to composition disposal, and the editor updates playlists and synchronizes players separately. Trace navigation, foreground/background transitions, seeking, and player preparation. Do not declare a lifecycle bug or playback-loop cause without a trace.

## Reproduction record to complete

- Phone model / RAM / Android version:
- Installed APK version and build/hash:
- Original imported clip: codec, dimensions, frame rate, HDR/SDR, duration, audio:
- Original clip plays correctly in the phone's gallery player: unverified.
- Exact taps from app launch until black preview / frozen video / crash:
- Audio during frozen preview: unknown.
- Playhead and displayed time during the sampled playing state: start position / 00:00 around 40-48s.
- Reopening: recording shows a saved project and recovery prompts, followed by the black editor preview. Rotation/background behavior and preservation of the latest edits remain unverified.
- Crash stack/logcat and Android process-exit reason:
- Saved-project recovery and preservation of recent edits: unverified.
- Recording reviewed: `C:/Users/prem7/Downloads/WhatsApp Video 2026-10-17 at 2.55.08 PM.mp4`; incident times are listed above.

## Repair and acceptance order

1. Reproduce and capture PHY-001/002/003 using the affected phone and original source clip. First isolate a plain single-player preview; then restore background rendering, transforms/effects, and overlays one at a time.
2. Fix first-frame rendering and playback/error state handling. Verify that visible motion, audio where present, time, and playhead agree; verify pause/resume, scrubbing, and clip boundaries.
3. Diagnose and fix the crash from its actual stack/exit reason. Repeat the user's sequence and a repeat-cycle stress test; verify saved edits survive reopening.
4. Correct local diagnostics and add editor playback regressions. Re-run export regressions after playback/rendering changes.
5. Resolve the overlapping recovery UI, wrapped duration, and repeated entry gate; verify recovery and compact-screen/font scaling behavior.
6. Resume additional product features after the basic physical editing workflow is reliable.

Minimum physical acceptance: a supported local video shows a first frame; plays continuously for at least 60 seconds or its full duration if shorter; responds to pause/resume and scrubbing; can be reopened and edited; and survives repeated import/play/edit/navigation cycles without black/frozen output or a crash. Test a freshly created project and a reopened saved project. Record the specific device, clip, build, and results. A short screenshot or successful MP4 export alone is insufficient evidence for these gates.

## Untested is not the same as broken

This report does not assert that every effect, transition, export setting, audio feature, or template failed physical testing. Those features were not individually established by the information available here. Automatic captions and other previously documented unfinished features remain roadmap items, separate from this physical-test failure report.

No playback or crash fix has been made in this documentation pass. Root causes and closure remain open.
