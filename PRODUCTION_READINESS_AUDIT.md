# Clipp Production-Readiness Audit — 2026-10-15

## A Executive Summary

Clipp has a credible local-first editing/export architecture, an explicit feature-capability gate, a canonical timeline/render graph, real waveform decoding, app-owned deletion safeguards, and a MediaStore-based publish flow. The current production decision is **NOT READY FOR PRODUCTION** because a confirmed P0 project-persistence defect remains unresolved, the current branch has not been rebuilt/tested after hardening, and no current device/export matrix has been executed.

The audit was frozen before source changes in `AUDIT_FINDINGS.md`. After the freeze, this branch applied narrow release-hygiene fixes that are independently reviewable: truthful single-share capability, explicit supported deep-link hosts, the normal haptic permission, generic keystore ignore rules, and an official package-level Media3 unstable-API opt-in.

No merge to `main` is authorized or performed.

## B Exact Source SHA

- Audited source branch: `main`
- Exact audited SHA: `585597d0ed98588e41924b0a06bd3d82de8d6b2b`
- Hardening branch: `astra/clipp-production-hardening`
- The hardening branch was created directly from the audited SHA after the findings snapshot was frozen.

## C Current Build Baseline

Audited toolchain/source configuration:

- Gradle 9.4.1
- Android Gradle Plugin 9.1.1
- Kotlin 2.2.10
- compileSdk 36.1
- targetSdk 36
- minSdk 24
- versionCode 1 / versionName 1.0
- Java source/target compatibility 11

Baseline `assembleDebug` and `assembleDebugAndroidTest` completed successfully before source hardening. The baseline unit run under Java 17 executed 39 tests with two Robolectric environment failures stating that Android SDK 36 requires Java 21. Temurin JDK 21 was installed and a rerun was started, but its final result was not captured before the local runner became unavailable. Therefore this audit does **not** claim the JDK 21 suite is green.

## D Current CI Status

No trustworthy current GitHub Actions signal was available for the audited SHA. Historical local/device notes exist but are not promoted to current CI evidence. The branch should not gain a broad CI gate until the local JDK 21/lint suite is first proven stable; once stable, CI should run unit tests, lint, debug assembly and androidTest assembly on every PR.

## E Architecture Map

Primary layers observed:

- Compose UI/editor orchestration: `EditorScreen.kt`, media picker, templates, export settings, project/home surfaces.
- Persistent project layer: Room project entities/DAO/ViewModel and versioned database.
- Canonical edit model: `EditorState` → `TimelineProject` conversion with typed layer payloads.
- Shared render planning: `TimelineRenderGraph` resolves normalized active layers/properties/time for preview/export consumers.
- Export rendering: `ExportRendering.kt`, Media3 effects/transformer integration and `VideoExporter`.
- Audio: custom Media3 `AudioProcessor` implementations, external audio, voiceover and waveform decode/cache.
- Publication: MediaStore on API 29+, app-owned external output + FileProvider on older Android.
- Local utilities: project storage cleanup, local analytics/crash recovery, performance mode and widgets/shortcuts.

The highest architectural risk is concentration: `EditorScreen.kt` is roughly 477 KB and owns models, persistence glue, player state and many editor tools.

## F Historical Revalidation

`HARDENING_NOTES.md` contains extensive September 2026 validation claims, including device/export results. Those notes are useful regression targets but are not treated as current proof because source has evolved and this audit had no connected device.

`CLIPP_REMAINING_WORK.txt` is directionally honest: it already calls out device/export matrices, signing, accessibility, security/Play work and low-end validation. Current source revalidation confirms many earlier architectural improvements remain present, especially the unified timeline/render graph, capability registry, output publication safeguards and local-first design.

## G Feature Truth Table

| Feature family | Source status | Export claim | Current production status |
| --- | --- | --- | --- |
| Multi-clip trim/split/reorder | Implemented | Supported | Device verification pending |
| Constant speed | Implemented | Supported | Device verification pending |
| Speed curves | Implemented and gated | Supported through bounded segmentation | Device verification pending |
| Transitions | Implemented and capability-gated | Supported subset by source/type | Device verification pending |
| Filters/color/effects | Implemented | Registry/export gate controls unsupported cases | Device verification pending |
| Text/captions/stickers/drawings/frames | Implemented | Canonical visual layers/export rendering | Device verification pending |
| Photo/video/GIF overlays | Implemented | Capability-gated | Device verification pending |
| Masks/blends/chroma | Implemented subset | Explicit registry rules | Device verification pending |
| External music | Implemented | Supported subset | URI/device verification pending |
| Voiceover | Implemented locally | Supported as audio source | Device verification pending |
| Waveform | Real decoded waveform | N/A UI aid | Keep as-is |
| Batch export | Implemented sequential runner | Uses same unsupported-reason gate | Device verification pending |
| Templates | Implemented local templates/replacement | Must resolve to supported editor state | Device verification pending |
| Cloud/account/billing/online AI | Disabled/not part of launch contract | Not advertised as export-ready | Keep disabled |

## H KEEP AS-IS

The following designs are strong enough to retain during this hardening pass:

- Local-first project/media architecture; no account/backend dependency found in the app module.
- No broad storage permission.
- `FeatureCapabilityRegistry` as the single truthful capability/export-readiness gate.
- Canonical `TimelineProject` plus `TimelineRenderGraph` normalized frame planning.
- Real waveform extraction/decoding with bounded memory/disk cache rather than synthetic peaks.
- Project deletion constrained to app-owned canonical roots.
- MediaStore pending-publication flow and cleanup behavior.
- Environment-only release signing configuration with R8/resource shrinking enabled.
- Unsupported online/cloud/billing features remaining disabled rather than stubbed/faked.

## I Model Round-Trip Audit

`EditorState.toTimelineProject()` and the reverse conversion retain typed payloads while overlaying canonical common properties. Existing unit coverage includes `editorStateMigratesToUnifiedTimelineAndBack`, which exercises a meaningful subset of fields.

The model strategy is sound, but the matrix is not exhaustive enough to claim every advanced property is proven round-trip safe. Future coverage should explicitly include transition parameters, audio effects, speed curves, flip/crop/keyframes, masks/blends/chroma, overlay animations, captions and layer ordering. This is a test-coverage gap rather than evidence of a current model-loss defect.

## J Timeline/Render Graph Audit

`TimelineRenderGraph` normalizes the project once and resolves active layers, local time, z-order and common properties. Preview and export code both consume the canonical timeline representation. Existing tests compare preview/export frame plans and verify hidden/expired layers are excluded.

This is the correct architectural boundary. The remaining requirement is rendered-media validation on device, because equal frame plans do not prove codec/GLES/Media3 output parity.

## K Feature Capability Registry Audit

The registry is one of the strongest production-safety components in the project. It distinguishes export-ready, preview-only experimental, disabled and coming-later capabilities; `isSelectable` follows export readiness; export code calls `exportUnsupportedReasons()`; batch export uses the same gate.

No justified reason was found to weaken or bypass this registry. Any new editor feature should continue to enter the UI only after its export path and regression coverage are ready.

## L Core Editing Audit

Clip trim, split, reorder, mute, transforms, crop, keyframes and media insertion are represented in the editor model and feed the canonical timeline. Existing code normalizes clip timing/ranges. Core editing remains release-conditional because the current device matrix is absent and the P0 Save Draft/Back persistence path can still lose a newer live state.

## M Speed Curve

Speed curves have a typed `SpeedCurve`, normalized points, duration mapping, preview playback adjustment and export segmentation. The UI explicitly explains that export samples the curve into bounded segments and preserves pitch per segment. Transition combinations are capability-gated when timing cannot be rendered safely.

Status: code architecture acceptable; current fixture/device export validation required.

## N Transition

The project defines crossfade, fade, slide, push, zoom, wipe, spin and flip families with bounded duration/easing. Capability checks consider outgoing/incoming source properties. Preview logic and export rendering both have transition-specific paths.

Status: keep implementation; do not expand feature surface until current transition fixtures are re-run on device.

## O Effects/GLES

Advanced effects use Media3/GLES helpers and intentionally consume APIs marked unstable by Media3. Baseline lint therefore produced a large `UnsafeOptInUsageError` set. This branch adds the official package-level `androidx.annotation.OptIn(UnstableApi::class)` declaration rather than hiding the lint issue.

Status: source contract improved; lint and rendered-effect exports must be rerun.

## P Text/Caption

Text, caption and sticker layers carry timing, transforms, visibility/order and style/animation payloads. Preview uses shared render-graph timing, and export rendering has matching layer support. Some UI callbacks can update live Compose state before a persistence helper runs, which is one concrete trigger for P0 persistence loss if Save Draft/Back is used before a full live-state serialization.

Status: renderer path retained; persistence P0 blocks production readiness.

## Q Overlay

Photo/video/GIF overlays support timing, transform, opacity and selected decorations/animations. The capability registry intentionally disables or blocks export for combinations that are not supported, such as preview-only decoration cases.

Status: truthful gating is good; current overlay/GIF/video-overlay device exports remain required.

## R Mask/Blend/Chroma

Mask shape, blend mode and chroma-key capabilities are centrally checked before selection/export. GIF chroma is explicitly treated as preview-only where unsupported; supported photo/video chroma has export parameters.

Status: keep capability gating; rerun pixel/visual fixture exports before release.

## S Audio Pipeline

The exporter builds Media3 audio processors for volume, volume keyframes/fades and supported DSP paths. Existing unit tests cover PCM gain and fade behavior. Two `isActive()` overrides currently omit `super.isActive()`, producing baseline `MissingSuperCall` lint errors and requiring a small source fix before lint can be green.

A/V sync, pitch/speed interactions, loop boundaries and separate-track mixing require current device exports. Unsupported DSP remains correctly gated rather than silently omitted.

## T Waveform

`WaveformRepository` performs real `MediaExtractor`/`MediaCodec` decoding, caches decoded peaks with bounded memory/disk storage, validates cache format/version/count and bounds requested bucket counts. Preview does not fabricate random amplitudes when waveform data is unavailable.

Status: KEEP AS-IS.

## U Voiceover

Voiceover records AAC/MPEG-4 into an app-private `filesDir/voiceovers` directory, handles recorder cleanup, rejects empty recordings and then adds the result as a normal audio track. Project-storage cleanup includes the owned voiceover directory.

Status: architecture acceptable; permission denial, interrupted recording, deletion/orphan and export-sync device cases remain in the release matrix.

## V Exporter

Strengths:

- Export is blocked early when canonical state has unsupported edits.
- Transformation success is not reported until publication and validation complete.
- Cancellation/failure attempts cleanup.
- API 29+ publication uses MediaStore pending state.

Risks:

- Export validation currently proves non-empty output and positive duration but not explicit video-track/dimension integrity.
- Exporter instance fields (`transformer`, temporary/published output and progress job) are shared across calls; current UI and batch paths mostly serialize operations, but the class is not intrinsically overlap-safe.
- Two audio processor `isActive()` overrides require `super` handling.

## W MediaStore

On API 29+, output is inserted with `IS_PENDING=1`, copied through the resolver, then made visible with `IS_PENDING=0`; failure paths delete the inserted item. This is the correct scoped-storage publication model.

On pre-Q, output is copied into app-owned external Movies/Clipp and shared via FileProvider. That path needs explicit pre-Q device validation.

## X Export Validation

Current final validation checks that the temp artifact exists and is non-empty, publishes it, reads duration and deletes an invalid non-positive-duration result. This is useful but not strong enough to prove the published MP4 contains a valid video track with positive dimensions.

Status: P1 validation gap. Add explicit track/dimension validation when the instrumented export suite can be rerun, then include malformed/audio-only negative fixtures.

## Y URI/Media Picker

Visual media uses the system visual picker for multiple selections and stores content URI strings. The code attempts a persistable read grant for each URI. External music uses `OpenDocument`, also attempting a persisted read grant. Shared/opened intents attempt the same where supported.

This is preferable to broad storage permissions. The remaining P1 gap is missing-source recovery: providers can deny persistence or later invalidate a URI, and no full relink workflow was found.

## Z External Intent/Deep Link

Baseline behavior advertised both single and multiple share targets although the activity consumed only the first item from `SEND_MULTIPLE`. This branch removes the multi-share manifest capability and retains the truthful single-share target.

Baseline custom-scheme routing accepted any `clipp://` host in the manifest while code handled only `project`, `new`, `quick-edit` and `shortcut`. This branch restricts manifest hosts to those supported routes; runtime project-ID validation remains in place.

## AA FileProvider

Provider configuration is `exported=false` with temporary URI grants, which is correct. `file_paths.xml` exposes whole cache and external-files roots via `.`; this is broader than current pre-Q export sharing appears to require.

Status: P2 hardening item. Narrow only after every legitimate share path is enumerated and tested; do not break current export/share behavior in this P0/P1 pass.

## AB Room/Migration

Database version is 4 with schema export enabled, explicit 3→4 migration and no destructive fallback. That is safer than destructive migration.

The audit could not prove whether DB versions 1/2 were ever shipped to users. Do not invent migrations. Confirm release history, then add MigrationTestHelper coverage for every actually shipped upgrade path.

## AC Project Storage/Deletion

`ProjectStorage` deletes `file://` paths only after canonicalizing them under app-owned files/cache/externalFiles/externalCache roots. Bulk local-data deletion targets owned voiceover/projects/thumbnails/proxies/export-temp directories and cache. Existing tests cover deleting owned artifacts without deleting external source media.

Status: KEEP AS-IS, with regression tests retained.

## AD Autosave/Recovery

Autosave is real rather than cosmetic: editor settings support 30 seconds, 1 minute, 5 minutes, 10 minutes or Off, and the ViewModel has a debounce path. Opening a project itself does not mark it dirty, which avoids false recovery prompts.

The blocking defect is explicit Save Draft/Back behavior: Save Draft copies a prior history JSON instead of serializing the full live editor document, and Back can mark clean without first serializing live state. Because some live-state edits precede persistence callbacks, a project can be marked clean with a stale snapshot. This is P0.

## AE Templates

Templates are local editor-state/project construction paths rather than remote content/account dependencies. Template replacement eventually enters the same editor/export capability boundary.

Status: keep local architecture; current template-generated projects require export fixtures in the device matrix.

## AF Batch Export

`BatchExportRunner` loads saved editor state, calls the same `exportUnsupportedReasons()` gate and processes work sequentially, which reduces current exposure to the exporter's shared mutable per-job state.

Status: architecture acceptable; run cancel/failure/partial-success and multiple-project device tests before release.

## AG Widget/Shortcut

Manifest widget receivers are explicit app components with widget-update filters. App-owned `clipp://` shortcut routes are parsed into a limited action set. This branch narrows the external VIEW manifest to the handled hosts.

Status: smoke-test each widget size and shortcut route on device; no reason to expand route surface.

## AH Privacy

Observed analytics are local SharedPreferences and analytics defaults disabled unless enabled by the user. No app-module backend/Firebase/account/billing dependency was found. Privacy text states media/content is not uploaded.

A P2 consistency issue remains: local crash-recovery preference reads default true in some paths before explicit consent, although the stored crash information remains on-device. Align that default later. Play Console Data Safety and a privacy-policy URL remain external launch requirements.

## AI Security

Positive findings:

- No signing key material found in repository/history audit.
- Release signing consumes environment-provided credentials.
- No broad storage permission.
- FileProvider is non-exported and grant-based.
- Project deletion is constrained to owned roots.
- Deep-link project IDs are validated before navigation.

This branch adds generic `*.jks` and `*.keystore` ignore rules. Remaining security hardening is primarily FileProvider path minimization and release/dependency scanning after the core release gates are green.

## AJ Dependency

Media3 is pinned at 1.5.1 while newer stable versions exist in 2026. A broad dependency upgrade during renderer hardening would add unnecessary behavioral risk, so no mass upgrade is justified in this pass.

Upgrade Media3 separately only after a trustworthy device/export fixture baseline exists. Other dependency warnings should be triaged by security/compatibility impact rather than bulk-updated.

## AK Performance

The editor includes a performance mode, reduced preview sizes on budget devices, bounded thumbnail/waveform caches and viewport/overscan logic. These are good guardrails.

Performance is not release-proven for long timelines, 4K inputs, multiple video overlays, memory pressure, thermal throttling or 4 GB-class devices. Measure peak memory, frame responsiveness, export duration and cancellation cleanup on representative hardware.

## AL Device Matrix

**Current audit result: NOT EXECUTED.** No emulator/physical device was connected in the current validation session.

Required minimum matrix before release:

1. API 24–28 representative device/emulator: import, edit, pre-Q FileProvider export/share.
2. API 29–34 representative device/emulator: MediaStore publish, URI persistence/reopen, export fixtures.
3. API 35–36 current device/emulator: target-SDK behavior, latest picker/MediaStore/export fixtures.
4. Low-memory/4 GB-class physical device where available: long timeline, effects/overlays, cancel/retry.
5. At least one physical-device audio test for voiceover, A/V sync and haptics.

The existing `ExportInstrumentedTest.kt` already contains broad generated-fixture coverage and should be the starting point rather than replacing it with ad-hoc smoke tests.

## AM Accessibility

Many icon controls include content descriptions, but the app is a dense custom gesture editor. No current TalkBack/focus/touch-target/font-scale matrix was executed. Custom timeline pointer surfaces, drag-only operations and dense toolbar interactions need manual and automated accessibility checks.

Status: P1 release gate pending concrete device findings.

## AN Architecture/Monolith

The canonical timeline/render graph and capability registry are good seams. The main risk is UI/persistence concentration in `EditorScreen.kt`. A production hardening pass should not perform a big rewrite.

After release stabilization, extract a document/persistence controller first, because the confirmed P0 is a direct consequence of multiple UI paths owning persistence semantics. Then extract tool panels incrementally behind the same typed model.

## AO CI/CD

Current release confidence depends too heavily on historical/local manual runs. Once the JDK 21 suite and lint are locally proven, add a minimal PR workflow that runs:

- `testDebugUnitTest` on JDK 21
- `lintDebug`
- `assembleDebug`
- `assembleDebugAndroidTest`
- artifact/test-report upload on failure

Device instrumentation can remain a separate emulator/device job after it is stable enough not to create noisy false negatives. Do not weaken release signing or check secrets into CI config.

## AP Release/Play

Target SDK 36 matches the current Android/Play target direction for 2026. Release signing is externalized and the release build enables minification/shrinking.

Outstanding launch actions include a signed/minified release build, Play Console Data Safety completion, privacy-policy URL, store listing/assets, target-device compatibility checks and final AAB/device smoke testing. No release should be cut from this branch while a P0 remains or device/export validation is absent.

## AQ Production Scorecard

| Area | Status | Reason |
| --- | --- | --- |
| Project safety | **BLOCKED** | P0 live-state Save Draft/Back persistence defect |
| Export architecture | CONDITIONAL | Strong gate/publication flow; output integrity/device matrix incomplete |
| Preview/export parity | CONDITIONAL | Shared canonical model/render graph; rendered device parity pending |
| A/V sync | UNVERIFIED | Requires current device exports |
| URI durability | CONDITIONAL | Persisted-grant attempts good; relink flow missing |
| Storage/security | GOOD WITH P2 | Scoped storage/deletion strong; FileProvider paths broad |
| Privacy | GOOD WITH FOLLOW-UP | Local-first; crash default consistency + Play declarations pending |
| Build/lint | BLOCKED | Hardening changes not rebuilt; two known MissingSuperCall errors plus rerun required |
| Accessibility | UNVERIFIED | No current TalkBack/device matrix |
| Release signing | UNVERIFIED | Secure configuration exists; signed minified artifact not built in audit |

## AR P0–P3 Backlog

### P0

- `CLIPP-P0-001`: serialize the full live editor document before Save Draft/clean exit and prove reopen/recovery.

### P1

- Re-run lint after package-level Media3 opt-in and manifest fixes.
- Fix two `BaseAudioProcessor.isActive()` overrides to call/include `super.isActive()`.
- Strengthen output validation to require a real video track/positive dimensions.
- Finish missing-source/relink behavior or explicitly gate release on durable-source tests.
- Capture green JDK 21 unit result.
- Execute current device/export/A/V matrix.
- Validate signed/minified release artifact.
- Complete accessibility verification.

### P2

- Make `VideoExporter` intrinsically single-session or isolate per-export state.
- Narrow FileProvider roots after path inventory.
- Confirm Room pre-v3 release provenance/migrations.
- Align local crash-recovery default with optional-consent semantics.
- Upgrade Media3 only in a separate renderer-regression change.
- Incrementally split the EditorScreen monolith.

### P3

- Triage non-blocking lint/style/deprecation/locale warnings after release blockers.
- Continue UI polish/performance micro-optimizations only with measured evidence.

## AS Implemented Fixes

Implemented **after** `AUDIT_FINDINGS.md` was frozen:

1. Removed the manifest `SEND_MULTIPLE` share target so Clipp no longer advertises a capability that imported only the first stream.
2. Added the normal `android.permission.VIBRATE` permission used by haptic fallbacks.
3. Restricted `clipp://` VIEW routing to supported hosts: `project`, `new`, `quick-edit`, `shortcut`.
4. Added generic `*.jks` and `*.keystore` repository ignore rules.
5. Added `app/src/main/java/com/example/package-info.kt` with the official `androidx.annotation.OptIn(UnstableApi::class)` package-level Media3 opt-in.

No P2 refactor or dependency upgrade was mixed into the hardening changes.

## AT Exact Files Changed

Current branch changes relative to audited `main` at the time of this report:

- `.gitignore`
- `AUDIT_FINDINGS.md`
- `PRODUCTION_READINESS_AUDIT.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/package-info.kt`

The confirmed P0 file `app/src/main/java/com/example/EditorScreen.kt` has deliberately **not** been remotely replaced in this session because the local patch runner became unavailable and a blind full replacement of a roughly 477 KB source file would be unsafe.

## AU Test Results

Baseline, before branch changes:

- `assembleDebug`: PASS
- `assembleDebugAndroidTest`: PASS
- `testDebugUnitTest` on Java 17: 39 tests, 2 environment failures (`ExampleRobolectricTest`, `ProjectStorageTest`) because SDK 36 requires Java 21
- JDK 21 installed; rerun final result: NOT CAPTURED
- `lintDebug`: FAIL — 395 errors, 173 warnings, 1 hint; concentrated known root causes include Media3 opt-in, VIBRATE permission, deep-link URL configuration and two audio `MissingSuperCall` findings

After branch changes:

- No local build/test/lint result is claimed. The connected local execution tunnel became unavailable (`turn/session finished`, then `401 tunnel_use_forbidden` on the fallback workspace tunnel).
- No device result is claimed.

## AV Device Verification Remaining

Required before production approval:

- Full `ExportInstrumentedTest` generated-fixture suite on current branch.
- Save Draft + force-stop + reopen across layer/audio/edit types after P0 fix.
- Back/exit persistence after P0 fix.
- Single-share image/video flows; confirm multi-share is no longer offered.
- Deep-link allowed/unknown route behavior.
- Pre-Q FileProvider export/share.
- API 29+ MediaStore pending/publish/cancel/failure behavior.
- Speed curves, transitions, effects, masks/blends/chroma, GIF/video overlays.
- External/looped audio, fades/keyframes/pitch/effects, voiceover and A/V sync.
- Cancel/retry/repeated export stress.
- Low-memory/long timeline/4K representative workloads.
- Haptics and TalkBack/accessibility pass on physical hardware.

## AW External Actions Remaining

- Restore a working local/CI runner and capture JDK 21 unit/lint/build evidence.
- Supply release signing material only through the secure release environment; build/install signed minified output.
- Run the device matrix above.
- Complete Play Console Data Safety and privacy-policy/store metadata.
- Confirm historical Room schema/release provenance if any pre-v3 build shipped.
- After all P0/P1 release gates are resolved, update this report with immutable test/device evidence rather than historical claims.

## AX Final Release Decision

**NOT READY FOR PRODUCTION.**

Reasons are concrete rather than precautionary: `CLIPP-P0-001` remains open; current hardening changes have not been rebuilt/linted due runner unavailability; two known audio `MissingSuperCall` lint errors remain; export validation and durable-URI recovery still have P1 gaps; and no current device/export/accessibility/signed-release matrix has been executed.

The branch is suitable for review as a **draft production-hardening PR**, not for merge/release. `main` must remain untouched until the P0 is fixed and the required validation evidence is green.
