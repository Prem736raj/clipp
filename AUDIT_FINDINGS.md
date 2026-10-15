# Clipp Production-Readiness Audit Findings

Audit date: 2026-10-15

Frozen source baseline: `585597d0ed98588e41924b0a06bd3d82de8d6b2b` (`main`)

This file is the frozen pre-fix audit snapshot. Production-source hardening starts only after this snapshot. Prior historical hardening notes are treated as evidence to revalidate, not as a substitute for the current audit.

## CLIPP-P0-001 — Save/exit paths can mark a project clean without serializing the live editor document

- **Priority:** P0
- **Status:** CONFIRMED
- **Subsystem:** Project persistence / editor recovery
- **User-visible problem:** A user can make a valid edit that exists in the live Compose editor state, tap **Save Draft** or leave through the editor Back action, then reopen the project and receive an older persisted editor snapshot. The project may be marked clean even though the newest live state was never serialized, which defeats recovery and can lose edits.
- **Evidence:** `EditorScreen.kt` keeps the current document across `clips`, `overlays`, `texts`, `stickers`, `drawings`, `frames`, `audioClips`, `layerOrder`, and `canvasSettings`. Normal persistence helpers serialize `EditorHistoryModel(currentState = EditorState(...))`. `saveDraft`, however, copies the already-persisted `ProjectEntity.historyState` JSON and adds `zoom`, `pos`, and `tool` instead of serializing the live document. Multiple editor interactions update live state before/without an immediate history write; for example, adding a sticker updates the `stickers` list directly. The editor Back action also sets `isDirty = false` without first serializing the live state.
- **Root cause:** Persistence is duplicated across UI callbacks, and Save Draft/Back use project-row state instead of a single live-document serializer.
- **Exact files:** `app/src/main/java/com/example/EditorScreen.kt`
- **Proposed fix:** Centralize serialization of the live `EditorState` + undo/redo history and use it for Save Draft and explicit editor exit. Mark a project clean only after that serialization succeeds.
- **Regression test:** Add deterministic history serialization/round-trip coverage that proves the newest clips/layers/audio/order survive save and reopen. Add a focused UI/Robolectric test when local execution is available.
- **Device verification:** Create/edit sticker, text, overlay, drawing, audio and clip transforms; Save Draft; force-stop; reopen; verify exact state. Repeat using Back. Required on at least one API 24–28 device/emulator and one API 29+ device/emulator.

## CLIPP-P1-002 — ACTION_SEND_MULTIPLE is advertised but only the first stream is imported

- **Priority:** P1
- **Status:** CONFIRMED
- **Subsystem:** External intents / import
- **User-visible problem:** Android can present Clipp as a valid target for sharing multiple images/videos, but Clipp silently uses only the first URI.
- **Evidence:** `AndroidManifest.xml` advertises `android.intent.action.SEND_MULTIPLE` for image/video MIME types. `MainActivity.handleIncomingIntent()` reads the URI list and assigns only `uris[0]`; project creation uses a one-item `sourceMediaPaths` list.
- **Root cause:** Manifest capability exceeded implementation capability.
- **Exact files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/MainActivity.kt`
- **Proposed fix:** For v1, remove the SEND_MULTIPLE intent filter and corresponding dead branch. Keep the existing in-app multi-picker as the supported multi-import path.
- **Regression test:** Manifest/source test or Robolectric intent test proving single-share remains supported and multiple-share is no longer advertised.
- **Device verification:** Share one image and one video from two common providers; verify project creation. Confirm multi-share chooser does not offer Clipp.

## CLIPP-P1-003 — Vibrator fallback is used without declaring VIBRATE permission

- **Priority:** P1
- **Status:** CONFIRMED
- **Subsystem:** Haptics / manifest
- **User-visible problem:** Fallback vibration calls can be rejected and lint blocks the release gate.
- **Evidence:** `HapticUtil.kt` calls `Vibrator.vibrate(...)`; the manifest currently declares INTERNET and RECORD_AUDIO but not the normal `android.permission.VIBRATE` permission. Current lint reported three `MissingPermission` errors in this utility.
- **Root cause:** Manifest omission.
- **Exact files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/utils/HapticUtil.kt`
- **Proposed fix:** Declare the normal `VIBRATE` permission. No runtime permission dialog is required.
- **Regression test:** `lintDebug` must contain no HapticUtil `MissingPermission` error.
- **Device verification:** Exercise timeline scrub, play/pause, split and reorder haptics on a physical device.

## CLIPP-P1-004 — Media3 unstable API usage is unannotated and causes a large lint error set

- **Priority:** P1
- **Status:** CONFIRMED
- **Subsystem:** Renderer / build hygiene
- **User-visible problem:** Release lint is red, preventing a trustworthy release gate even when the code intentionally uses Media3 APIs marked unstable.
- **Evidence:** Current lint reported hundreds of `UnsafeOptInUsageError` findings across Media3-based renderer/editor files. Current source contains no `UnstableApi` opt-in declaration.
- **Root cause:** The project deliberately consumes Media3 effect/transformer APIs without explicit opt-in annotations.
- **Exact files:** Media3-using production files including `AdvancedVideoEffects.kt`, `BlendModeOverlayEffect.kt`, `EditorScreen.kt`, `ExportRendering.kt`, `VideoExporter.kt`, `WaveformRepository.kt` and any additional files identified by the next lint run.
- **Proposed fix:** Add explicit, scoped `@file:OptIn(androidx.media3.common.util.UnstableApi::class)` declarations only to files that intentionally use unstable Media3 APIs. Do not suppress unrelated lint and do not introduce a lint baseline to hide errors.
- **Regression test:** `lintDebug` must show zero `UnsafeOptInUsageError` in production sources.
- **Device verification:** Renderer/export regression matrix remains mandatory because opt-in annotations do not prove behavioral compatibility.

## CLIPP-P1-005 — Two audio processors violate BaseAudioProcessor isActive contract

- **Priority:** P1
- **Status:** CONFIRMED
- **Subsystem:** Audio export
- **User-visible problem:** A processor can report inactive when the base processor considers its configured format active; lint reports `MissingSuperCall`, and lifecycle behavior may diverge from other processors.
- **Evidence:** `VolumeAudioProcessor.isActive()` and `KeyframedVolumeAudioProcessor.isActive()` return only their custom gain/fade/keyframe conditions. Other custom processors in the same file use `super.isActive() || ...`. Current lint reported two `MissingSuperCall` errors.
- **Root cause:** Inconsistent overrides.
- **Exact files:** `app/src/main/java/com/example/VideoExporter.kt`
- **Proposed fix:** Include `super.isActive()` in both overrides.
- **Regression test:** Extend processor unit coverage for configured active formats and existing PCM gain/fade behavior; lint must contain no `MissingSuperCall` error.
- **Device verification:** Export clips with base volume, fade envelopes and volume keyframes; inspect audio presence and A/V sync.

## CLIPP-P1-006 — Custom-scheme manifest accepts routes the activity does not support

- **Priority:** P1
- **Status:** CONFIRMED
- **Subsystem:** Deep links / external intents
- **User-visible problem:** Any `clipp://...` URI can resolve to the app even though the activity recognizes only the supported route hosts, causing misleading routing and an `AppLinkUrlError` lint failure.
- **Evidence:** Manifest declares only `<data android:scheme="clipp" />`. `MainActivity` handles the project, new, quick-edit and shortcut routes and validates project IDs before navigation.
- **Root cause:** Manifest filter is broader than routing implementation.
- **Exact files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/MainActivity.kt`
- **Proposed fix:** Restrict manifest filters to explicitly supported hosts/routes while retaining runtime validation.
- **Regression test:** Manifest/Robolectric route tests for allowed and unknown hosts; lint must remove the custom-scheme URL error.
- **Device verification:** `adb am start` supported routes and at least one unsupported route; verify no unintended navigation.

## CLIPP-P1-007 — Signing artifacts are not generically ignored

- **Priority:** P1
- **Status:** CONFIRMED RELEASE HYGIENE
- **Subsystem:** Signing / repository security
- **User-visible problem:** A developer can accidentally stage a release/upload keystore with a common `.jks` or `.keystore` name.
- **Evidence:** Repository/history audit found no committed keystore. `.gitignore` ignores `debug.keystore` but not generic `*.jks` or `*.keystore`. Release signing itself correctly requires environment-supplied credentials and a keystore path outside the repository.
- **Root cause:** Incomplete ignore patterns.
- **Exact files:** `.gitignore`
- **Proposed fix:** Add `*.jks` and `*.keystore`.
- **Regression test:** Repository scan remains empty for signing key material.
- **Device verification:** Not applicable.

## CLIPP-P1-008 — Release lint gate is currently red

- **Priority:** P1
- **Status:** CONFIRMED RELEASE BLOCKER
- **Subsystem:** Build quality gate
- **User-visible problem:** The project cannot claim release readiness while Android lint reports release-impacting errors.
- **Evidence:** Baseline `lintDebug` result: 395 errors, 173 warnings, 1 hint. Root causes include Media3 unstable API opt-ins, VIBRATE permission, audio processor super calls and custom-scheme routing. The `local.properties` escape finding is environment-only and is not a production-source defect.
- **Root cause:** Several concentrated root causes plus lower-severity warnings.
- **Exact files:** Lint report and files named by each root finding.
- **Proposed fix:** Fix root causes and rerun lint. Do not hide errors behind a baseline.
- **Regression test:** Fresh `lintDebug` on JDK 21/declared Android SDK.
- **Device verification:** Not directly applicable; renderer/device checks remain separate.

## CLIPP-P1-009 — Current audit has no connected-device export verification

- **Priority:** P1
- **Status:** OPEN RELEASE GATE
- **Subsystem:** Device/export matrix
- **User-visible problem:** Export correctness, codec behavior, URI handling and A/V sync can differ from JVM/build-time behavior.
- **Evidence:** Current audit baseline had no connected emulator or physical device. Historical September device claims exist but are not counted as a current pass.
- **Root cause:** Validation environment gap.
- **Exact files:** `app/src/androidTest/java/com/example/ExportInstrumentedTest.kt` and device matrix/report artifacts.
- **Proposed fix:** Run the existing broad generated-fixture export suite plus focused P0/P1 regressions on the required device/API matrix.
- **Regression test:** Existing instrumentation suite plus new targeted cases.
- **Device verification:** Mandatory before release decision can become READY.

## CLIPP-P1-010 — Java 21 unit rerun is not yet proven green in the current audit record

- **Priority:** P1
- **Status:** OPEN VALIDATION GATE
- **Subsystem:** Unit tests / toolchain
- **User-visible problem:** A green unit-test baseline has not been conclusively recorded for the Java version required by Robolectric with SDK 36.
- **Evidence:** Java 17 run executed 39 tests with two environment failures (`ExampleRobolectricTest`, `ProjectStorageTest`) stating Android SDK 36 requires Java 21. JDK 21 was installed and a rerun started, but its final result was not captured before the local runner session became unavailable.
- **Root cause:** Audit execution/session gap, not a confirmed production-code failure.
- **Exact files:** `app/src/test/java/com/example/ExampleRobolectricTest.kt`, `app/src/test/java/com/example/ProjectStorageTest.kt`, unit test reports.
- **Proposed fix:** Rerun `testDebugUnitTest` under JDK 21 and record exact test count/failures.
- **Regression test:** The rerun itself.
- **Device verification:** Not applicable.

## CLIPP-P1-011 — Source URI durability lacks an explicit relink recovery flow

- **Priority:** P1
- **Status:** OPEN / CONFIRMED DESIGN GAP
- **Subsystem:** URI persistence / project recovery
- **User-visible problem:** If a content provider does not grant durable read access or later revokes/moves media, reopening a saved project can leave one or more sources unreadable with no explicit relink path.
- **Evidence:** Visual picker stores URI strings and attempts `takePersistableUriPermission`; music uses `OpenDocument()` with the same persisted-read attempt. The attempt is correctly wrapped because not every provider supports it. No broad storage permission is used, which is correct. A dedicated missing-source relink flow was not found.
- **Root cause:** Durable access is provider-dependent and recovery UX is incomplete.
- **Exact files:** `MediaPickerScreen.kt`, `EditorScreen.kt`, project/media metadata helpers.
- **Proposed fix:** Detect unreadable project sources on reopen and offer targeted relink/replacement without adding broad storage permissions. Scope as a follow-up if it cannot be safely completed in this hardening pass.
- **Regression test:** Fake/unreadable URI project reopen coverage.
- **Device verification:** Revoke provider access/remove source, reopen project, verify actionable recovery.

## CLIPP-P1-012 — Export validation checks non-empty output and positive duration but not explicit video-track integrity

- **Priority:** P1
- **Status:** CONFIRMED VALIDATION GAP
- **Subsystem:** Export validation
- **User-visible problem:** A malformed or wrong-track MP4 with a positive duration could theoretically pass the final success gate.
- **Evidence:** `VideoExporter.publishAndValidate()` requires a non-empty temp file, publishes it, reads duration with `MediaMetadataRetriever`, deletes outputs with non-positive duration, then reports success. It does not explicitly confirm a video track and valid dimensions.
- **Root cause:** Minimal post-publish validation.
- **Exact files:** `app/src/main/java/com/example/VideoExporter.kt`
- **Proposed fix:** Add bounded metadata/track validation after publish (video track present, positive width/height, duration > 0) once instrumented export coverage can be rerun.
- **Regression test:** Valid MP4 accepted; audio-only/empty/malformed artifact rejected.
- **Device verification:** Required across generated fixture exports.

## CLIPP-P1-013 — Signed/minified release artifact has not been validated in this audit

- **Priority:** P1
- **Status:** OPEN EXTERNAL RELEASE GATE
- **Subsystem:** Release signing / R8
- **User-visible problem:** Debug builds do not prove that R8/resource shrinking and release signing preserve runtime behavior.
- **Evidence:** Release Gradle configuration correctly requires environment signing variables, enables minification and resource shrinking, and keeps credentials out of VCS. Current audit did not receive release signing material and therefore cannot claim a signed release build pass.
- **Root cause:** External signing environment/credential prerequisite.
- **Exact files:** `app/build.gradle.kts`, `app/proguard-rules.pro`
- **Proposed fix:** In release environment, build signed AAB/APK, run smoke/export tests, inspect R8 warnings and artifact signing.
- **Regression test:** `bundleRelease`/`assembleRelease` with real upload signing in secure environment.
- **Device verification:** Install release artifact and execute smoke/export matrix.

## CLIPP-P1-014 — Accessibility release verification is incomplete

- **Priority:** P1
- **Status:** OPEN RELEASE GATE
- **Subsystem:** Accessibility
- **User-visible problem:** Complex custom timeline gestures, dense editor controls and icon-only actions have not been verified for TalkBack, focus order, touch targets and large font/display sizes.
- **Evidence:** Source contains many content descriptions, but custom pointer-input/timeline surfaces and dense controls require device-level accessibility validation; no current accessibility matrix result is available.
- **Root cause:** Validation gap across a custom, gesture-heavy editor UI.
- **Exact files:** Primarily `EditorScreen.kt` and editor tool surfaces.
- **Proposed fix:** Run Android accessibility scanner/manual TalkBack pass; fix concrete blockers found rather than speculative redesign.
- **Regression test:** Compose semantics tests for any fixed controls.
- **Device verification:** TalkBack, font scaling and display scaling on representative phone.

## CLIPP-P2-015 — VideoExporter holds mutable per-export state on the exporter instance

- **Priority:** P2
- **Status:** CONFIRMED LATENT RISK
- **Subsystem:** Export concurrency/cancellation
- **User-visible problem:** If two exports overlap on one `VideoExporter` instance, shared `transformer`, temp output, published file and progress job fields can be overwritten, creating cancellation/cleanup races.
- **Evidence:** `VideoExporter` stores mutable instance fields for the active transformer/output/job. Each `export()` has a per-call cancel flag but writes those shared fields. Current editor UI guards one active export and batch export is sequential, reducing current exposure.
- **Root cause:** Per-job state is partly global to the exporter instance.
- **Exact files:** `app/src/main/java/com/example/VideoExporter.kt`
- **Proposed fix:** Either enforce one active export explicitly or isolate all job state into an export-session object. Address after current P0/P1 gates unless a reachable overlap path is found.
- **Regression test:** Start/cancel/retry and intentional overlap test.
- **Device verification:** Repeated export/cancel/retry stress.

## CLIPP-P2-016 — FileProvider roots are broader than current export sharing needs

- **Priority:** P2
- **Status:** CONFIRMED HARDENING OPPORTUNITY
- **Subsystem:** FileProvider
- **User-visible problem:** A future accidental `getUriForFile` call can grant access to a broader app cache/external-files subtree than necessary.
- **Evidence:** Provider is correctly `exported=false` with temporary URI grants, but `file_paths.xml` exposes cache `.` and external-files `.`. Current pre-Q export path only needs the app-owned Movies/Clipp output location.
- **Root cause:** Broad path configuration.
- **Exact files:** `app/src/main/res/xml/file_paths.xml`
- **Proposed fix:** Narrow roots after verifying every legitimate sharing path.
- **Regression test:** FileProvider URI creation for supported export/share files; unsupported paths rejected.
- **Device verification:** Pre-Q share/export flow.

## CLIPP-P2-017 — Historical Room migration provenance before version 3 is not demonstrated

- **Priority:** P2
- **Status:** OPEN PROVENANCE GAP
- **Subsystem:** Room migration
- **User-visible problem:** If versions 1 or 2 were ever shipped to users, a direct upgrade to database version 4 would require corresponding migration coverage.
- **Evidence:** Database is version 4, exports schema, contains a documented no-op 3→4 migration, and does not use destructive fallback. Current repository contains schema 4; older shipped schema provenance was not established in the audit.
- **Root cause:** Historical release/schema evidence is incomplete.
- **Exact files:** `app/src/main/java/com/example/data/AppDatabase.kt`, `app/schemas/...`
- **Proposed fix:** Confirm first shipped DB version from release history. Add migration tests/migrations only for versions that actually shipped; do not invent unsupported historical schemas.
- **Regression test:** Room MigrationTestHelper for every supported shipped upgrade path.
- **Device verification:** Upgrade install from oldest supported production build if available.

## CLIPP-P2-018 — EditorScreen is a very large state/UI monolith

- **Priority:** P2
- **Status:** CONFIRMED MAINTAINABILITY RISK
- **Subsystem:** Architecture
- **User-visible problem:** Persistence, preview, timeline gestures, audio, layers and tool panels share one very large composable file, increasing regression risk for otherwise small fixes.
- **Evidence:** `EditorScreen.kt` is roughly 477 KB and owns core models, history serialization, players, editor state and many tool surfaces.
- **Root cause:** Incremental feature growth without subsystem extraction.
- **Exact files:** `app/src/main/java/com/example/EditorScreen.kt`
- **Proposed fix:** After release hardening, extract persistence/document controller and tool-specific composables behind the existing canonical `EditorState`/`TimelineProject` boundary. Do not perform a broad rewrite in this production-hardening pass.
- **Regression test:** Preserve model/render graph tests during incremental extraction.
- **Device verification:** Normal editor regression suite after future refactors.

## CLIPP-P2-019 — Media3 dependencies are behind the current stable line

- **Priority:** P2
- **Status:** REVIEW, DO NOT BLINDLY UPGRADE
- **Subsystem:** Dependencies / renderer
- **User-visible problem:** Remaining on an older Media3 line can miss fixes, but a renderer library jump can itself change export behavior.
- **Evidence:** Repository uses Media3 1.5.1. Audit research found a newer stable Media3 line available in 2026. The app depends on effect/transformer APIs and has extensive renderer behavior to preserve.
- **Root cause:** Deliberate version stability during renderer hardening.
- **Exact files:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
- **Proposed fix:** Defer upgrade until the current device/export matrix is trustworthy; then upgrade in a separate renderer-focused change with before/after fixtures.
- **Regression test:** Full unit + instrumented export suite across old/new dependency versions.
- **Device verification:** Full export matrix.

## CLIPP-P2-020 — Local crash-recovery preference defaults are inconsistent with optional-consent semantics

- **Priority:** P2
- **Status:** CONFIRMED PRIVACY CONSISTENCY GAP
- **Subsystem:** Privacy / local crash recovery
- **User-visible problem:** Before explicit preference initialization, local crash logging paths/settings can default to enabled even though the consent dialog describes crash recovery as optional.
- **Evidence:** Analytics itself defaults disabled unless opted in and all observed statistics/crash messages remain on-device. `MainActivity`/privacy settings use a `crash_reporting_enabled` default of true in some reads, while the consent dialog offers Accept All vs Decline Optional.
- **Root cause:** Inconsistent local default value, not remote data collection.
- **Exact files:** `MainActivity.kt`, `PrivacySettingsScreen.kt`, `PrivacyConsentDialog.kt`
- **Proposed fix:** Align the pre-consent default to false in a later privacy cleanup, while preserving explicit user choices.
- **Regression test:** First-run preference test and decline/accept persistence.
- **Device verification:** First launch + crash recovery behavior.

## Validated KEEP-AS-IS decisions captured by the audit

- Canonical `TimelineProject` / render-graph boundary and preview/export frame-plan tests.
- Central `FeatureCapabilityRegistry` with truthful export gating and disabled online/cloud/billing claims.
- Real waveform decoding/cache implementation rather than generated/fake peaks.
- Project deletion constrained to app-owned canonical roots; no source/gallery deletion path found.
- MediaStore publish flow with `IS_PENDING` on API 29+, failure cleanup, and pre-Q app-owned output.
- Local-only optional analytics; no app-module Firebase/backend/account/billing dependency found.
- Environment-supplied release signing with minification/resource shrinking; no signing key material found in repository history.
- No broad storage permission.

## Freeze rule

This snapshot is frozen before production-source changes on `astra/clipp-production-hardening`. Any newly discovered issue after this point must be appended as a post-freeze finding with evidence; existing findings must not be retroactively rewritten to make the implementation appear cleaner.
