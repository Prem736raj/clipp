# Clipp Production-Readiness Audit — 2026-10-15

## A Executive Summary

Clipp has a credible local-first editor/export architecture: a typed editor model, canonical timeline/render graph, capability registry, real waveform decoding, bounded app-owned storage deletion, and MediaStore/FileProvider publication paths. The audit was frozen against `main` before production fixes in `AUDIT_FINDINGS.md`.

The confirmed P0 project-state-loss defect is fixed on `astra/clipp-production-hardening`. Save Draft and editor Back now serialize the current live document before marking the project clean, with deterministic JVM round-trip coverage. All current local release gates that can run without a device are green on JDK 21: **41/41 unit tests**, **lint with 0 errors**, `assembleDebug`, and `assembleDebugAndroidTest`.

The release decision remains **NOT READY FOR PRODUCTION** because no emulator/physical device is connected for current rendered-export, A/V sync, URI-recovery, accessibility, pre-Q sharing, or performance validation; explicit published-video track/dimension validation remains open; and a signed/minified release artifact has not been validated. No merge to `main` has been performed or authorized.

## B Exact Source SHA

- Audited source branch: `main`
- Exact frozen source SHA: `585597d0ed98588e41924b0a06bd3d82de8d6b2b`
- Hardening branch: `astra/clipp-production-hardening`
- Existing draft PR: `#3`, targeting `main`
- The pre-fix findings snapshot was frozen before P0/P1 source hardening.

## C Current Build Baseline

- Gradle: 9.4.1
- Android Gradle Plugin: 9.1.1
- Kotlin: 2.2.10
- compileSdk: 36.1
- targetSdk: 36
- minSdk: 24
- versionCode: 1
- versionName: 1.0
- Java source/target: 11
- Verification runtime: Temurin JDK 21 (`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`)

Current local branch verification passes on Windows with the Android SDK configured through escaped `local.properties` syntax. That local SDK-path correction is environment-only and is not committed.

## D Current CI Status

The previous self-modifying one-shot hardening workflow and patch scripts were removed. The branch now contains a normal read-only PR workflow at `.github/workflows/android-ci.yml` that installs Android 36.1 on Ubuntu/JDK 21 and runs:

- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`
- `assembleDebugAndroidTest`

Local equivalents are green. The remote workflow result is not claimed until the updated branch is pushed and GitHub executes the new head.

## E Architecture Map

- UI/editor orchestration: `EditorScreen.kt` and Compose tool/picker/export surfaces.
- Editor document state: `EditorState`, `EditorHistoryModel`, `EditorDocumentState`.
- Canonical timeline: `TimelineProject` plus typed `TimelineLayer` payloads.
- Shared timing/render planning: `TimelineRenderGraph` and mapper helpers.
- Capability truth source: `FeatureCapabilityRegistry`.
- Export pipeline: `VideoExporter`, Media3 Transformer/effects/audio processors, publish/validation helpers.
- Waveform: `WaveformRepository` using real `MediaExtractor`/`MediaCodec` decode with bounded caches.
- Persistence: Room `ProjectEntity`/database plus serialized editor history/timeline state.
- Owned-file safety: `ProjectStorage` canonical-path checks and app-owned cleanup roots.
- Batch: `BatchExportRunner`, sequentially using the same capability gate/exporter contract.

`EditorScreen.kt` remains large and owns too many persistence/UI responsibilities, but a broad rewrite is not justified inside this release-hardening pass.

## F Historical Revalidation

Historical hardening notes and device claims were treated as regression targets only. They are not counted as current release evidence because source and build configuration have changed since those runs.

The previous Java 17 Robolectric failures were revalidated as environment-only: SDK 36 requires Java 21. Under JDK 21 the current JVM suite passes 41/41.

## G Feature Truth Table

| Capability | Source status | Export/launch status |
| --- | --- | --- |
| Trim/split/reorder/crop/transform | Implemented | Locally model-tested; device export pending |
| Speed / speed curve | Implemented | Capability-gated; device export pending |
| Transitions | Implemented subset | Capability-gated; device export pending |
| Filters/color/effects/GLES | Implemented subset | Media3/GLES path; device visual parity pending |
| Text/captions/stickers/drawings/frames | Implemented | Canonical layers; device export pending |
| Photo/video/GIF overlays | Implemented subset | Unsupported combinations gated |
| Mask/blend/chroma | Implemented subset | Capability-gated |
| External audio / voiceover | Implemented | Device A/V sync and permission cases pending |
| Waveform | Real decode/cache | Keep as-is |
| Batch export | Sequential implementation | Device failure/cancel matrix pending |
| Single external share/open | Supported | Manifest/runtime aligned |
| Multi-share target | Not supported | No longer advertised |
| Cloud/account/billing/remote AI | Disabled/out of launch contract | Keep disabled |

## H KEEP AS-IS

The following components are strong and should not be rewritten in this pass:

- `FeatureCapabilityRegistry` and export unsupported-reason checks.
- Canonical `TimelineProject` / `TimelineRenderGraph` boundary.
- MediaStore `IS_PENDING` publication on API 29+ with failure cleanup.
- App-owned pre-Q export plus non-exported FileProvider and URI grants.
- `WaveformRepository` real decoding and bounded caches.
- `ProjectStorage` canonical app-owned deletion safeguards.
- Sequential `BatchExportRunner` behavior.
- Local-first product posture and disabled unsupported online/cloud/billing claims.

## I Model Round-Trip Audit

`EditorState` contains clips, canvas, overlays, text, captions/settings, stickers, drawings, frames, audio, and visual layer order. `EditorHistoryModel` persists the legacy current state together with the canonical `TimelineProject`.

Hardening adds editor UI metadata (`zoom`, persisted `pos`, optional tool value) to the typed history model and routes Save Draft/Back through one live snapshot serializer. New regression coverage proves the latest video/text/audio/layer ordering and metadata survive serialize/restore.

## J Timeline/Render Graph Audit

`TimelineProject` is schema-versioned and carries typed layer payloads, timing, z-order, visibility and common properties. `TimelineRenderGraph` resolves the canonical document for preview/export planning. Existing JVM tests validate migration/round-trip behavior, structural corruption detection, and shared preview/export frame planning.

Rendered pixel/codec equivalence still requires device fixtures; model-plan equality alone does not prove final media correctness.

## K Feature Capability Registry Audit

The registry truthfully separates export-ready, preview-only/experimental, disabled, and coming-later functionality. UI selection and export rejection use the same capability information, and batch export consults the same unsupported-reason gate.

No bypass or broadening is justified. New features should remain unavailable until their export path and regression evidence are real.

## L Core Editing Audit

Trim, split, reorder, mute, transform, crop, keyframes and media insertion are represented in the editor model and canonical timeline. The critical persistence risk around explicit Save Draft/Back is fixed in the branch.

Current source-level confidence is good; rendered output across device/API variants remains pending.

## M Speed Curve

Speed curves use typed normalized points and bounded export segmentation. Timing conversion exists for preview/export planning and unsupported combinations are capability-gated.

Keep implementation; verify duration, pitch and transition interactions on exported fixtures before release.

## N Transition

Transition families include crossfade/fade and directional/transform families with bounded duration/easing. Transition feasibility is checked against adjacent source properties.

Keep the existing gated surface. Required device fixtures: short clips, mixed resolutions/orientations, transition boundaries, speed-adjusted clips and consecutive transitions.

## O Effects/GLES

Media3/GLES effects intentionally consume APIs marked unstable. The branch now uses a correct package-level Java `@androidx.annotation.OptIn(markerClass = UnstableApi.class)` annotation. This eliminated the `UnsafeOptInUsageError` lint class without suppressions or a lint baseline.

Visual correctness remains device/export work, particularly effect ordering and overlays combined with crop/transform.

## P Text/Caption

Text/caption layers carry timing, transforms, styling and animation/keyframe payloads and participate in the canonical layer plan. The P0 persistence fix ensures a live text/caption edit is included when Save Draft/Back snapshots the project.

Device checks remain for font/layout differences, animations, captions near clip boundaries and non-default aspect ratios.

## Q Overlay

Photo/video/GIF overlays carry timeline timing, transform, opacity and supported decoration/animation parameters. Capability checks intentionally block unsupported combinations.

Current risk is rendered parity/performance, not silent capability overclaiming.

## R Mask/Blend/Chroma

Mask, blend and chroma features are centrally gated. Unsupported GIF/chroma/decor combinations are not treated as export-ready.

Keep the registry boundary and verify alpha edges, color keying and blend ordering through device media fixtures.

## S Audio Pipeline

The exporter builds Media3 processors for supported volume, keyframes/fades and gated DSP paths. `VolumeAudioProcessor.isActive()` and `KeyframedVolumeAudioProcessor.isActive()` now include `super.isActive()` consistently with the other processors. Current lint reports no `MissingSuperCall` errors.

Device work remains for A/V sync, speed/pitch combinations, loop/fade boundaries and mixed external/clip/voiceover audio.

## T Waveform

Waveforms are derived from real audio decode through `MediaExtractor`/`MediaCodec`, with versioned bounded disk cache and memory LRU behavior. The UI renders an honest no-audio/loading baseline rather than synthesizing fake peaks.

Keep as-is; performance-check long/high-bitrate sources on low-memory hardware.

## U Voiceover

Voiceover uses local recording and app-owned files. Lifecycle cleanup exists for active recorders and temporary recording state.

Release validation still needs permission denial, interruption, save/reopen, deletion/orphan and A/V sync tests on a physical device.

## V Exporter

Success is emitted only after publish/validation. Temporary output and published app file state are cleaned on error/cancel paths; API 29+ uses pending MediaStore rows and pre-Q uses app-owned external Movies/Clipp plus FileProvider.

`VideoExporter` still keeps mutable active-export fields on the instance. Current UI guards one active export and batch export is sequential, so no reachable concurrent-export P1 was confirmed. This remains P2 hardening: explicitly enforce one active session or isolate per-export state after release blockers are closed.

## W MediaStore

On API 29+, publication creates a MediaStore row with `IS_PENDING=1`, copies the completed temporary output, and clears pending only after success. Failure deletes the inserted row; cancellation after publication also attempts cleanup before success wins.

This is the correct scoped-storage pattern. It still needs API 29–36 device verification.

## X Export Validation

Current publication validation proves the output exists/is non-empty and that the published media reports positive duration. It does not yet explicitly assert a video track and positive encoded width/height.

This remains `CLIPP-P1-012`. Adding stricter metadata/track rejection without a runnable device export suite could create an unverified device-specific failure mode, so the bounded validation change is deferred until generated media fixtures can be executed. Release remains blocked on that evidence.

## Y URI/Media Picker

Photo Picker/OpenDocument and incoming intents use content URIs and attempt `takePersistableUriPermission` when providers allow it. This avoids broad storage permission.

`CLIPP-P1-011` remains open: providers can deny persistence or later invalidate access, and no explicit user relink/recovery flow was found. This is a release gate for projects expected to reopen durable external media.

## Z External Intent/Deep Link

The app now advertises only single `SEND` share for image/video and MIME-based `ACTION_VIEW` for video. The stale runtime `ACTION_SEND_MULTIPLE` first-item branch is removed. App-owned `clipp://` navigation is restricted to supported hosts: `project`, `new`, `quick-edit`, and `shortcut`.

This closes the prior capability mismatch and app-link lint errors. Device chooser/routing smoke tests remain.

## AA FileProvider

The provider is non-exported and uses temporary URI grants. `file_paths.xml` still exposes whole cache and external-files roots using `.`. Current pre-Q export uses an app-owned Movies/Clipp location.

Root narrowing remains P2 until all legitimate share paths are enumerated and pre-Q share behavior can be tested.

## AB Room/Migration

Room is at database version 4, schema export is enabled, `MIGRATION_3_4` is explicit, and destructive fallback was not found.

Do not invent old migrations without evidence of previously shipped schemas. If earlier production schemas exist outside the repository, retrieve them before release and add real migration tests.

## AC Project Storage/Deletion

`ProjectStorage` canonicalizes `file://` paths and only deletes files under app-owned files/cache/externalFiles/externalCache roots. Bulk local-data deletion targets owned data/cache locations rather than gallery source media. Existing tests verify external source media is not deleted.

Keep as-is.

## AD Autosave/Recovery

Autosave is real: settings control a periodic editor loop and the ViewModel has debounced persistence. The critical explicit Save Draft/Back path now serializes the full live state through `serializeEditorHistorySnapshot()` and only marks clean after successful serialization.

If serialization fails, Save Draft/Back stays in the editor and shows an error instead of silently exiting clean. Device force-stop/reopen validation remains mandatory.

## AE Templates

Templates are local project/editor-state construction paths and eventually enter the same canonical capability/export boundary. They do not require an account/backend for launch.

Keep local architecture; export fixture validation remains pending.

## AF Batch Export

`BatchExportRunner` restores saved editor state, checks the same unsupported-reason gate and processes projects sequentially. Sequential execution reduces exposure to the exporter's instance-level mutable active state.

Device tests remain for partial failure, cancel/retry, multiple projects and URI expiry.

## AG Widget/Shortcut

Widgets/shortcuts use app-owned `clipp://` routes and the manifest now restricts resolution to supported hosts. Runtime routing maps known project/new/quick-edit/shortcut actions.

Smoke-test each widget size and shortcut route on device.

## AH Privacy

Observed analytics/crash statistics remain local and optional analytics defaults disabled. No account/backend/Firebase/ads/billing/remote analytics dependency is introduced by this hardening pass.

A P2 consent-default consistency item remains around local crash-recovery preference defaults. Play Data Safety and public privacy-policy URL are external launch requirements.

## AI Security

- `android:allowBackup="false"` with backup/data-extraction rules.
- No broad storage permission added.
- FileProvider is non-exported and grant-based.
- Generic `*.jks` / `*.keystore` patterns are ignored.
- No signing files were found in audited history.
- Deep-link surface is restricted to implemented routes.

Owner signing secrets must remain outside Git and CI logs.

## AJ Dependency

Dependency warnings exist, including newer Media3 releases. A broad dependency upgrade is not justified inside this production-hardening pass because renderer/codec behavior can change.

Keep the current renderer line until the device/export fixture baseline is trustworthy; then upgrade Media3 separately with before/after media evidence.

## AK Performance

The editor includes budget/balanced/quality performance modes, reduced preview budgets, bounded waveform/thumbnail caches and overscan/viewport logic.

Release performance is still unverified for long timelines, 4K inputs, multiple video overlays, memory pressure and thermal throttling. Measure memory, responsiveness, export time and cancellation cleanup on representative hardware.

## AL Device Matrix

**Current result: NOT EXECUTED.** `adb devices -l` returned no connected devices.

Required before production release:

1. API 24–28 representative device/emulator: import/edit, pre-Q export/FileProvider/share.
2. API 29–34 representative device/emulator: MediaStore publish, persisted URI reopen, generated export fixtures.
3. API 35–36 current device/emulator: target-SDK/picker/MediaStore/export behavior.
4. Low-memory physical device where available: long timeline, effects/overlays, cancel/retry.
5. Physical-device audio/haptics pass: voiceover, A/V sync, speed/pitch, scrub/split/reorder haptics.

## AM Accessibility

Source contains many content descriptions, but dense editor controls and custom pointer/timeline surfaces require TalkBack/focus-order/touch-target verification on device.

This remains `CLIPP-P1-014` and is not waived by a green JVM/lint build.

## AN Architecture/Monolith

`EditorScreen.kt` remains a large orchestration surface and duplicated ownership previously caused the confirmed persistence P0. The hardening fix centralizes the high-risk live snapshot path without attempting a destabilizing editor rewrite.

After release stabilization, extract persistence/document control first, then tool panels behind the same typed model.

## AO CI/CD

Temporary self-modifying CI scripts/workflow were removed. `.github/workflows/android-ci.yml` is a conventional read-only PR verification job using JDK 21 and Android 36.1.

It runs unit tests, lint, debug app assembly and androidTest APK assembly. Device instrumentation should remain a separate job only after emulator/device fixtures are reliable enough to avoid noisy false failures.

## AP Release/Play

Still required outside the source-only audit:

- owner-controlled signed/minified AAB/APK build;
- install/smoke/export from the signed artifact;
- R8/proguard warning review;
- Play Console Data Safety and privacy-policy URL;
- listing/assets/target-device compatibility checks;
- final versioning/release notes.

No signing secret should be committed to satisfy these steps.

## AQ Production Scorecard

| Area | Status | Evidence / blocker |
| --- | --- | --- |
| Project safety | **LOCAL PASS** | P0 fixed; deterministic round-trip regression green; device reopen pending |
| Build/unit | **PASS** | JDK 21, 41/41 tests |
| Lint | **PASS** | 0 errors; no baseline/suppression |
| Debug artifacts | **PASS** | `assembleDebug` + `assembleDebugAndroidTest` |
| Export architecture | **CONDITIONAL** | Strong publish/cleanup gate; rendered device matrix absent |
| Preview/export parity | **CONDITIONAL** | Shared timeline/frame plan tested; rendered parity absent |
| A/V sync | **UNVERIFIED** | Device exports required |
| URI durability | **BLOCKED** | Relink/recovery gap remains |
| Export integrity validation | **BLOCKED** | Explicit video track/dimensions not yet device-validated |
| Accessibility | **UNVERIFIED** | TalkBack/device matrix absent |
| Performance | **UNVERIFIED** | Representative hardware matrix absent |
| Signed release | **UNVERIFIED** | Owner signing/release build not run |

## AR P0–P3 Backlog

### P0

- No confirmed P0 remains in the branch. `CLIPP-P0-001` is fixed locally and awaits device reopen/recovery validation.

### P1

- `CLIPP-P1-009`: execute current device/export/A/V matrix.
- `CLIPP-P1-011`: implement and verify relink/recovery for expired/non-persistable source URIs.
- `CLIPP-P1-012`: add explicit video-track/positive-dimension publish validation with instrumented fixtures.
- `CLIPP-P1-013`: validate signed/minified release artifact.
- `CLIPP-P1-014`: complete accessibility device matrix.

Closed P1s in this branch: truthful single-share capability, VIBRATE permission, Media3 opt-in/lint, audio processor super calls, supported deep-link hosts, signing ignore hygiene, lint gate, and JDK 21 unit rerun.

### P2

- Make `VideoExporter` intrinsically single-session or isolate per-export state.
- Narrow FileProvider roots after share-path enumeration.
- Align local crash-recovery consent defaults.
- Verify/provide older Room migrations only if prior shipped schemas exist.

### P3

- Triage deprecations/style/locale warnings after release blockers.
- Incrementally reduce editor monolith and duplicate UI state ownership.
- Evaluate dependency upgrades only with renderer regression media.

## AS Implemented Fixes

1. Central live editor snapshot serialization for Save Draft, Back and regular history persistence.
2. Typed persisted editor `zoom`/`pos`/tool metadata in `EditorHistoryModel`.
3. Regression proving latest video/text/audio/layer state survives history round-trip.
4. `super.isActive()` handling for the two affected audio processors.
5. Correct package-level AndroidX Media3 opt-in using `package-info.java`.
6. VIBRATE permission and generic keystore ignore rules (earlier branch hardening).
7. `SEND_MULTIPLE` capability removal, including stale runtime branch.
8. Supported custom-scheme hosts and MIME-only video Open With filter.
9. Waveform indentation/lint correction with no behavioral change.
10. Replacement of self-modifying one-shot hardening automation with normal PR verification CI.

## AT Exact Files Changed

Relative to audited `main`, the final hardening branch changes the following durable areas:

- `.gitignore`
- `.github/workflows/android-ci.yml`
- `AUDIT_FINDINGS.md`
- `PRODUCTION_READINESS_AUDIT.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/EditorScreen.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/VideoExporter.kt`
- `app/src/main/java/com/example/package-info.java`
- `app/src/test/java/com/example/ExampleUnitTest.kt`

Temporary `.github/scripts/*`, `one-shot-hardening.yml`, and the invalid Kotlin `package-info.kt` are removed from the hardening branch before final review.

## AU Test Results

Current JDK 21 local evidence:

- `ExampleRobolectricTest`: 1 passed.
- `ExampleUnitTest`: 38 passed.
- `ProjectStorageTest`: 2 passed.
- **Total: 41 tests, 41 passed, 0 failures, 0 errors, 0 skipped.**
- Focused new persistence snapshot regression: passed.
- `lintDebug`: **BUILD SUCCESSFUL**, 0 errors; report retains 177 warnings and 1 hint.
- `assembleDebug`: **BUILD SUCCESSFUL**.
- `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**.
- `git diff --check`: clean after the final documentation/CI edits.

No instrumentation tests were executed because no device/emulator is connected; only the instrumentation APK was assembled.

## AV Device Verification Remaining

- Save Draft -> force-stop -> reopen with clip transform, text, sticker, overlay, drawing, audio and layer-order changes.
- Back/exit persistence with the same state types.
- Preview/export pixel/timing parity across core effects/transitions/layers.
- A/V sync, speed/pitch, external audio, voiceover and fades.
- API 29+ MediaStore publish/delete/cancel behavior.
- API 24–28 FileProvider export/share.
- Picker/incoming-provider URI persistence and expired-URI recovery.
- Cancel/retry/repeated export stress and batch partial failures.
- TalkBack/focus/touch-target accessibility.
- Long/4K/overlay performance and low-memory behavior.

## AW External Actions Remaining

- Connect required emulator/physical-device matrix and execute instrumented export fixtures.
- Implement/validate durable-source relink UX and stronger video-track/dimension publish validation.
- Build/install/test the owner-signed minified release artifact.
- Complete Play Data Safety, privacy-policy URL, listing/assets and final store checks.
- Review the updated PR CI result after push.

## AX Final Release Decision

**NOT READY FOR PRODUCTION.**

The original confirmed P0 is fixed and the local JDK 21 unit/lint/debug-build gates are green, which materially improves the branch. Production release is still blocked by open P1 release evidence: no current device/export/A/V/accessibility matrix, no explicit relink recovery for expired source URIs, no device-backed video-track/dimension output validation, and no signed/minified release artifact verification.

Draft PR `#3` is appropriate for review and CI. `main` must remain unmerged until those release gates are resolved and explicitly approved.
