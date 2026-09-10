package com.example

import androidx.compose.ui.geometry.Rect
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.data.ProjectEntity
import com.example.viewmodel.PerformanceMode
import com.example.viewmodel.EditorPerformanceProfile
import com.example.viewmodel.EditorDocumentState
import com.example.viewmodel.PlaybackUiCadence
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun editorDocumentStateStartsWithIndependentEmptyDocument() {
    val state = EditorDocumentState()

    assertTrue(state.clips.value.isEmpty())
    assertTrue(state.overlays.value.isEmpty())
    assertTrue(state.texts.value.isEmpty())
    assertTrue(state.captions.value.isEmpty())
    assertEquals(CaptionSettings(), state.captionSettings.value)
    assertTrue(state.stickers.value.isEmpty())
    assertTrue(state.drawings.value.isEmpty())
    assertTrue(state.frames.value.isEmpty())
    assertTrue(state.audioClips.value.isEmpty())
    assertTrue(state.layerOrder.value.isEmpty())
    assertEquals(CanvasSettingsState(), state.canvasSettings.value)
    assertTrue(state.undoStack.value.isEmpty())
    assertTrue(state.redoStack.value.isEmpty())
  }

  @Test
  fun editorPerformanceProfileTightensPreviewAndCacheBudgetsByMode() {
    val budget = EditorPerformanceProfile.forMode(PerformanceMode.BETTER_PERFORMANCE)
    val balanced = EditorPerformanceProfile.forMode(PerformanceMode.BALANCED)
    val quality = EditorPerformanceProfile.forMode(PerformanceMode.BEST_QUALITY)

    assertEquals(854, budget.previewMaxWidth)
    assertEquals(480, budget.previewMaxHeight)
    assertTrue(budget.thumbnailMemoryCacheFraction < balanced.thumbnailMemoryCacheFraction)
    assertTrue(balanced.thumbnailMemoryCacheFraction < quality.thumbnailMemoryCacheFraction)
    assertTrue(budget.thumbnailDiskCacheFraction < quality.thumbnailDiskCacheFraction)
    assertTrue(budget.previewMaxWidth < quality.previewMaxWidth)
    assertEquals(96f, budget.timelineFrameWidthDp)
    assertEquals(48f, quality.timelineFrameWidthDp)
    assertTrue(budget.thumbnailOverscanPx < quality.thumbnailOverscanPx)
  }

  @Test
  fun playbackUiCadenceScalesStateUpdatesWithoutChangingPlaybackRate() {
    assertEquals(
      PlaybackUiCadence.BETTER_PERFORMANCE_INTERVAL_MS,
      PlaybackUiCadence.intervalMs(PerformanceMode.BETTER_PERFORMANCE, isPlaying = true)
    )
    assertEquals(
      PlaybackUiCadence.BALANCED_INTERVAL_MS,
      PlaybackUiCadence.intervalMs(PerformanceMode.BALANCED, isPlaying = true)
    )
    assertEquals(
      PlaybackUiCadence.BEST_QUALITY_INTERVAL_MS,
      PlaybackUiCadence.intervalMs(PerformanceMode.BEST_QUALITY, isPlaying = true)
    )
    assertEquals(
      PlaybackUiCadence.IDLE_INTERVAL_MS,
      PlaybackUiCadence.intervalMs(PerformanceMode.BEST_QUALITY, isPlaying = false)
    )
  }

  @Test
  fun newProjectsStartWithHonestLocalMetadata() {
    val project = ProjectEntity(name = "Test", duration = "00:01")

    assertEquals("local_only", project.syncStatus)
    assertEquals(0L, project.sizeBytes)
    assertEquals(emptyList<String>(), project.sourceMediaPaths)
  }

  @Test
  fun editorHistoryRoundTripsCanvasColor() {
    val state = EditorState(
      canvasSettings = CanvasSettingsState(
        backgroundColorValue = androidx.compose.ui.graphics.Color(0xff11223344u).value.toLong()
      )
    )

    val json = editorHistoryMoshi
      .adapter(EditorHistoryModel::class.java)
      .toJson(EditorHistoryModel(currentState = state))
    val restored = editorHistoryMoshi
      .adapter(EditorHistoryModel::class.java)
      .fromJson(json)
      ?.currentState

    assertEquals(state.canvasSettings.backgroundColorValue, restored?.canvasSettings?.backgroundColorValue)
  }

  @Test
  fun editorStateMigratesToUnifiedTimelineAndBack() {
    val first = MediaClip(
      id = "video-1",
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L
    )
    val second = MediaClip(
      id = "video-2",
      sourceUri = "content://media/video/2",
      originalDurationMs = 2_000L,
      trimEndMs = 2_000L,
      playbackSpeed = 2f
    )
    val overlay = OverlayClip(
      id = "overlay-1",
      sourceUri = "content://media/image/1",
      originalDurationMs = 1_500L,
      isPhoto = true,
      trimEndMs = 1_500L,
      startTimeOnTimelineMs = 250L,
      posX = 0.25f,
      scaleX = 0.4f,
      scaleY = 0.6f,
      opacity = 0.75f,
      blendMode = OverlayBlendModeType.SCREEN
    )
    val text = TextOverlay(
      id = "text-1",
      text = "Title",
      startTimeOnTimelineMs = 500L,
      durationMs = 1_000L,
      posY = 0.2f,
      keyframes = mapOf("opacity" to listOf(Keyframe(timeMs = 0L, value = 0f), Keyframe(timeMs = 1_000L, value = 1f)))
    )
    val caption = AutoCaptionSegment(
      id = "caption-1",
      text = "Hello",
      words = emptyList(),
      startTimeMs = 700L,
      durationMs = 500L
    )
    val sticker = StickerOverlay(
      id = "sticker-1",
      modelId = "star",
      content = "★",
      category = StickerCategory.SHAPE,
      startTimeOnTimelineMs = 900L,
      durationMs = 500L,
      keyframes = mapOf("scale" to listOf(Keyframe(timeMs = 0L, value = 0.5f)))
    )
    val audio = AudioClip(
      id = "audio-1",
      sourceUri = "content://media/audio/1",
      startTimeOnTimelineMs = 100L,
      sourceDurationMs = 2_000L,
      trimEndMs = 2_000L,
      volume = 0.4f
    )
    val state = EditorState(
      clips = listOf(first, second),
      overlays = listOf(overlay),
      texts = listOf(text),
      captions = listOf(caption),
      stickers = listOf(sticker),
      audioClips = listOf(audio),
      layerOrder = listOf(sticker.id, overlay.id, text.id)
    )

    val document = state.toTimelineProject()

    assertEquals(TIMELINE_PROJECT_SCHEMA_VERSION, document.schemaVersion)
    assertEquals(2_000L, document.durationMs)
    assertEquals(7, document.layers.size)
    assertTrue(document.validate().isEmpty())
    assertEquals(0L, document.layers.first { it.id == first.id }.startTimeMs)
    assertEquals(1_000L, document.layers.first { it.id == second.id }.startTimeMs)
    assertEquals(0, document.layers.first { it.id == sticker.id }.zIndex)
    assertEquals(1, document.layers.first { it.id == overlay.id }.zIndex)
    assertEquals(2, document.layers.first { it.id == text.id }.zIndex)
    assertEquals(3, document.layers.first { it.id == caption.id }.zIndex)

    val restored = document.toEditorState()
    assertEquals(listOf(first.id, second.id), restored.clips.map { it.id })
    assertEquals(0.25f, restored.overlays.single().posX)
    assertEquals(0.4f, restored.overlays.single().scaleX)
    assertEquals(0.6f, restored.overlays.single().scaleY)
    assertEquals(0.75f, restored.overlays.single().opacity)
    assertEquals(text.keyframes, restored.texts.single().keyframes)
    assertEquals(sticker.keyframes, restored.stickers.single().keyframes)
    assertEquals(audio.startTimeOnTimelineMs, restored.audioClips.single().startTimeOnTimelineMs)
    assertEquals(listOf(sticker.id, overlay.id, text.id), restored.layerOrder)
  }

  @Test
  fun unifiedTimelineIsPersistedAndOldHistoryStillRestores() {
    val state = EditorState(
      clips = listOf(
        MediaClip(
          id = "legacy-video",
          sourceUri = "content://media/video/legacy",
          originalDurationMs = 500L,
          trimEndMs = 500L
        )
      )
    )

    val json = editorHistoryMoshi
      .adapter(EditorHistoryModel::class.java)
      .toJson(EditorHistoryModel(currentState = state))
    assertTrue(json.contains("\"timelineProject\""))

    val restored = editorHistoryMoshi
      .adapter(EditorHistoryModel::class.java)
      .fromJson(json)
      ?.restoredEditorState()
    assertEquals(listOf("legacy-video"), restored?.clips?.map { it.id })

    val oldJson = editorHistoryMoshi
      .adapter(EditorHistoryModel::class.java)
      .toJson(EditorHistoryModel(currentState = state, timelineProject = null))
    val restoredOld = editorHistoryMoshi
      .adapter(EditorHistoryModel::class.java)
      .fromJson(oldJson)
      ?.restoredEditorState()
    assertEquals(listOf("legacy-video"), restoredOld?.clips?.map { it.id })
  }

  @Test
  fun unifiedTimelineReportsStructuralCorruptionInsteadOfDroppingLayers() {
    val invalid = TimelineProject(
      layers = listOf(
        TimelineLayer(
          id = "duplicate",
          kind = TimelineLayerKind.TEXT,
          track = TimelineTrackType.VISUAL_OVERLAY,
          startTimeMs = 0L,
          durationMs = 1_000L,
          zIndex = 0,
          payload = TimelineLayerPayload()
        ),
        TimelineLayer(
          id = "duplicate",
          kind = TimelineLayerKind.TEXT,
          track = TimelineTrackType.AUDIO,
          startTimeMs = 0L,
          durationMs = 1_000L,
          zIndex = 1,
          payload = TimelineLayerPayload()
        )
      )
    )

    val errors = invalid.validate()
    assertTrue(errors.any { it == "duplicate layer id: duplicate" })
    assertTrue(errors.any { it == "missing layer payload: duplicate" })
    assertTrue(errors.any { it == "invalid layer track: duplicate" })
  }

  @Test
  fun previewAndExportUseTheSameResolvedTimelineFramePlan() {
    val clip = MediaClip(
      id = "primary",
      sourceUri = "content://media/video/primary",
      originalDurationMs = 2_000L,
      trimEndMs = 2_000L
    )
    val overlay = OverlayClip(
      id = "overlay",
      sourceUri = "content://media/image/overlay",
      originalDurationMs = 1_000L,
      isPhoto = true,
      trimEndMs = 1_000L,
      startTimeOnTimelineMs = 500L,
      posX = 0.2f,
      keyframes = mapOf(
        "posX" to listOf(
          Keyframe(timeMs = 0L, value = 0.2f, easing = EasingType.LINEAR),
          Keyframe(timeMs = 1_000L, value = 0.8f, easing = EasingType.LINEAR)
        ),
        "opacity" to listOf(
          Keyframe(timeMs = 0L, value = 0.4f, easing = EasingType.LINEAR),
          Keyframe(timeMs = 1_000L, value = 1f, easing = EasingType.LINEAR)
        )
      )
    )
    val text = TextOverlay(
      id = "title",
      text = "Title",
      startTimeOnTimelineMs = 0L,
      durationMs = 2_000L
    )
    val audio = AudioClip(
      id = "music",
      sourceUri = "content://media/audio/music",
      startTimeOnTimelineMs = 400L,
      sourceDurationMs = 2_000L,
      trimEndMs = 2_000L
    )
    val state = EditorState(
      clips = listOf(clip),
      overlays = listOf(overlay),
      texts = listOf(text),
      audioClips = listOf(audio),
      layerOrder = listOf(text.id, overlay.id)
    )

    val project = state.toTimelineProject()
    val previewFrame = TimelineRenderGraph(project).frameAt(750L)
    val exportFrame = TimelineRenderGraph(project).frameAt(750L)

    assertEquals(previewFrame, exportFrame)
    assertEquals("primary", previewFrame.primaryVideo?.id)
    assertEquals(750L, previewFrame.primaryVideo?.localTimeMs)
    assertEquals(listOf("title", "overlay"), previewFrame.visualLayers.map { it.id })
    assertEquals(250L, previewFrame.visualLayer("overlay")?.localTimeMs)
    assertEquals(0.35f, previewFrame.visualLayer("overlay")?.properties?.transform?.positionX ?: 0f, 0.0001f)
    assertEquals(0.55f, previewFrame.visualLayer("overlay")?.properties?.opacity ?: 0f, 0.0001f)
    assertEquals(listOf("music"), previewFrame.audioLayers.map { it.id })
    assertEquals(350L, previewFrame.audioLayers.single().localTimeMs)
  }

  @Test
  fun renderGraphExcludesHiddenAndExpiredLayers() {
    val hidden = TextOverlay(
      id = "hidden",
      text = "Hidden",
      startTimeOnTimelineMs = 0L,
      durationMs = 2_000L,
      isVisible = false
    )
    val expired = StickerOverlay(
      id = "expired",
      modelId = "star",
      content = "★",
      category = StickerCategory.SHAPE,
      startTimeOnTimelineMs = 0L,
      durationMs = 300L
    )
    val active = FrameOverlay(
      id = "active",
      typeId = "clean_solid",
      startTimeOnTimelineMs = 200L,
      durationMs = 1_000L
    )
    val graph = TimelineRenderGraph(
      EditorState(
        texts = listOf(hidden),
        stickers = listOf(expired),
        frames = listOf(active),
        layerOrder = listOf(hidden.id, expired.id, active.id)
      ).toTimelineProject()
    )

    assertEquals(listOf("active"), graph.frameAt(500L).visualLayers.map { it.id })
    assertTrue(graph.frameAt(1_500L).visualLayers.isEmpty())
  }

  @Test
  fun basicTimelineAndSupportedVisualEditsCanBeExported() {
    val basic = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L
    )
    val cropped = basic.copy(cropRect = Rect(0f, 0f, 0.8f, 1f))
    val supported = basic.copy(
      playbackSpeed = 1.5f,
      maintainPitch = false,
      rotation = 90f,
      flipHorizontal = true,
      volume = 0.5f
    )

    assertFalse(basic.hasUnsupportedExportEdits())
    assertFalse(supported.hasUnsupportedExportEdits())
    assertFalse(cropped.hasUnsupportedExportEdits())

    val positioned = EditorState(clips = listOf(basic.copy(posX = 0.2f)))
    assertFalse(positioned.hasUnsupportedExportEdits())
  }

  @Test
  fun supportedAdvancedEffectsCanBeExportedAndUnsupportedEffectsAreBlocked() {
    val clip = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      effects = listOf(
        AppliedEffect(type = EffectType.MIRROR),
        AppliedEffect(type = EffectType.SHAKE),
        AppliedEffect(type = EffectType.COMIC_BOOK),
        AppliedEffect(type = EffectType.PENCIL_SKETCH),
        AppliedEffect(type = EffectType.POP_ART),
        AppliedEffect(type = EffectType.FILM_GRAIN),
        AppliedEffect(type = EffectType.ANAMORPHIC_FLARE),
        AppliedEffect(type = EffectType.SPARKLE),
        AppliedEffect(type = EffectType.RGB_SPLIT),
        AppliedEffect(type = EffectType.WAVE),
        AppliedEffect(type = EffectType.FISHEYE),
        AppliedEffect(type = EffectType.PIXELATE)
      )
    )

    assertFalse(EditorState(clips = listOf(clip)).hasUnsupportedExportEdits())
    val partialProceduralEffect = clip.copy(
      effects = listOf(AppliedEffect(type = EffectType.LIGHT_LEAK, startTimeMs = 100L, endTimeMs = 700L))
    )
    assertFalse(EditorState(clips = listOf(partialProceduralEffect)).hasUnsupportedExportEdits())
    val partialEffect = clip.copy(
      effects = listOf(AppliedEffect(type = EffectType.MIRROR, startTimeMs = 100L))
    )
    assertTrue(EditorState(clips = listOf(partialEffect)).hasUnsupportedExportEdits())
    val blockedEffect = clip.copy(effects = listOf(AppliedEffect(type = EffectType.OIL_PAINTING)))
    assertTrue(EditorState(clips = listOf(blockedEffect)).hasUnsupportedExportEdits())
  }

  @Test
  fun capabilityRegistryKeepsPreviewOnlyControlsOutOfNewEdits() {
    assertTrue(FeatureCapabilityRegistry.effect(EffectType.MIRROR).isSelectable)
    assertFalse(FeatureCapabilityRegistry.effect(EffectType.OIL_PAINTING).isSelectable)
    assertFalse(FeatureCapabilityRegistry.speedCurve().isSelectable)
    assertFalse(FeatureCapabilityRegistry.overlayAnimation(OverlayAnim.SLIDE).isSelectable)
    assertFalse(FeatureCapabilityRegistry.blendMode(OverlayBlendModeType.SCREEN).isSelectable)
    assertFalse(FeatureCapabilityRegistry.maskShape(MaskShape.CIRCLE).isSelectable)
    assertFalse(FeatureCapabilityRegistry.transition(TransitionType.PUSH_LEFT).isSelectable)

    val photo = MediaClip(
      sourceUri = "content://media/image/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      isPhoto = true
    )
    assertTrue(
      FeatureCapabilityRegistry.transition(
        TransitionType.CROSSFADE,
        outgoing = photo,
        incoming = photo.copy(id = "second")
      ).isSelectable
    )
    assertFalse(
      FeatureCapabilityRegistry.transition(
        TransitionType.CROSSFADE,
        outgoing = photo.copy(rotation = 90f),
        incoming = photo.copy(id = "second")
      ).isSelectable
    )
  }

  @Test
  fun newlyGatedDecorationsAnd3dTextAreBlockedFromExport() {
    val clip = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L
    )
    val decoratedOverlay = OverlayClip(
      sourceUri = "content://media/image/1",
      originalDurationMs = 1_000L,
      isPhoto = true,
      trimEndMs = 1_000L,
      borderWidth = 2f,
      shadowRadius = 4f
    )
    val threeDText = TextOverlay(text = "3D", is3D = true)

    assertTrue(EditorState(clips = listOf(clip), overlays = listOf(decoratedOverlay)).hasUnsupportedExportEdits())
    assertTrue(EditorState(clips = listOf(clip), texts = listOf(threeDText)).hasUnsupportedExportEdits())
  }

  @Test
  fun simpleSourceTransitionsAreAllowedOnlyForSimpleAdjacentSources() {
    val firstPhoto = MediaClip(
      sourceUri = "content://media/image/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      isPhoto = true,
      transitionNext = Transition(TransitionType.CROSSFADE, 300L)
    )
    val secondPhoto = MediaClip(
      sourceUri = "content://media/image/2",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      isPhoto = true
    )
    assertFalse(EditorState(clips = listOf(firstPhoto, secondPhoto)).hasUnsupportedExportEdits())

    val video = secondPhoto.copy(isPhoto = false, sourceUri = "content://media/video/2")
    assertFalse(EditorState(clips = listOf(firstPhoto, video)).hasUnsupportedExportEdits())
    assertTrue(
      EditorState(
        clips = listOf(
          firstPhoto.copy(transitionNext = Transition(TransitionType.WIPE_LEFT, 300L)),
          video
        )
      ).hasUnsupportedExportEdits()
    )

    val firstVideo = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      transitionNext = Transition(TransitionType.CROSSFADE, 300L)
    )
    val secondVideo = firstVideo.copy(
      id = "second-video",
      transitionNext = Transition()
    )
    assertFalse(EditorState(clips = listOf(firstVideo, secondVideo)).hasUnsupportedExportEdits())
    assertTrue(
      EditorState(
        clips = listOf(firstVideo, secondVideo.copy(rotation = 90f))
      ).hasUnsupportedExportEdits()
    )
  }

  @Test
  fun transformAndCropKeyframesCanBeExportedButInvalidCropKeyframesAreBlocked() {
    val basic = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L
    )
    val animatedTransform = basic.copy(
      posX = 0.35f,
      keyframes = mapOf(
        "posX" to listOf(Keyframe(timeMs = 0L, value = 0.35f), Keyframe(timeMs = 1_000L, value = 0.65f)),
        "scale" to listOf(Keyframe(timeMs = 0L, value = 0.9f), Keyframe(timeMs = 1_000L, value = 1.1f)),
        "rotation" to listOf(Keyframe(timeMs = 0L, value = 0f), Keyframe(timeMs = 1_000L, value = 15f))
      )
    )
    val animatedCrop = basic.copy(
      keyframes = mapOf(
        "cropLeft" to listOf(Keyframe(timeMs = 0L, value = 0f), Keyframe(timeMs = 1_000L, value = 0.5f)),
        "cropRight" to listOf(Keyframe(timeMs = 0L, value = 0.5f), Keyframe(timeMs = 1_000L, value = 1f))
      )
    )

    assertFalse(EditorState(clips = listOf(animatedTransform)).hasUnsupportedExportEdits())
    assertFalse(EditorState(clips = listOf(animatedCrop)).hasUnsupportedExportEdits())
    val invalidCrop = basic.copy(
      keyframes = mapOf(
        "cropLeft" to listOf(Keyframe(timeMs = 0L, value = 0.8f)),
        "cropRight" to listOf(Keyframe(timeMs = 0L, value = 0.2f))
      )
    )
    assertTrue(EditorState(clips = listOf(invalidCrop)).hasUnsupportedExportEdits())
  }

  @Test
  fun simpleLayerAnimationsCanBeExportedButUnsupportedAnimationsAreBlocked() {
    val clip = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L
    )
    val text = TextOverlay(
      text = "Animated",
      durationMs = 1_000L,
      animIn = TextAnimIn.FADE_IN,
      animLoop = TextAnimLoop.PULSE,
      animOut = TextAnimOut.SCALE_OUT
    )
    val sticker = StickerOverlay(
      modelId = "star",
      content = "★",
      category = StickerCategory.SHAPE,
      durationMs = 1_000L,
      animIn = TextAnimIn.ROTATE_IN,
      animLoop = TextAnimLoop.SWING,
      animOut = TextAnimOut.FADE_OUT
    )
    val imageOverlay = OverlayClip(
      sourceUri = "content://media/image/1",
      originalDurationMs = 1_000L,
      isPhoto = true,
      trimEndMs = 1_000L,
      entranceAnim = OverlayAnim.FADE,
      exitAnim = OverlayAnim.SCALE
    )

    assertFalse(EditorState(clips = listOf(clip), texts = listOf(text), stickers = listOf(sticker), overlays = listOf(imageOverlay)).hasUnsupportedExportEdits())
    assertFalse(EditorState(clips = listOf(clip), overlays = listOf(imageOverlay.copy(isPhoto = false, isGif = true))).hasUnsupportedExportEdits())
    assertTrue(EditorState(clips = listOf(clip), texts = listOf(text.copy(animIn = TextAnimIn.TYPEWRITER))).hasUnsupportedExportEdits())
    assertTrue(EditorState(clips = listOf(clip), overlays = listOf(imageOverlay.copy(entranceAnim = OverlayAnim.SLIDE))).hasUnsupportedExportEdits())
  }

  @Test
  fun videoOverlayKeyframesAreAllowedButInvalidOverlayKeyframesAreBlocked() {
    val overlay = OverlayClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      keyframes = mapOf(
        "posX" to listOf(Keyframe(timeMs = 0L, value = 0.35f), Keyframe(timeMs = 1_000L, value = 0.65f)),
        "scaleX" to listOf(Keyframe(timeMs = 0L, value = 0.25f), Keyframe(timeMs = 1_000L, value = 0.5f)),
        "scaleY" to listOf(Keyframe(timeMs = 0L, value = 0.25f), Keyframe(timeMs = 1_000L, value = 0.5f)),
        "opacity" to listOf(Keyframe(timeMs = 0L, value = 0.2f), Keyframe(timeMs = 1_000L, value = 1f))
      )
    )

    assertFalse(EditorState(overlays = listOf(overlay)).hasUnsupportedExportEdits())
    assertTrue(
      EditorState(
        overlays = listOf(overlay.copy(keyframes = mapOf("scaleX" to listOf(Keyframe(timeMs = 1_001L, value = 0.5f)))) )
      ).hasUnsupportedExportEdits()
    )
    assertTrue(
      EditorState(
        overlays = listOf(overlay.copy(keyframes = mapOf("opacity" to listOf(Keyframe(timeMs = 0L, value = 1.2f)))) )
      ).hasUnsupportedExportEdits()
    )
  }

  @Test
  fun invalidExportSpeedAndVolumeAreBlocked() {
    val clip = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      playbackSpeed = 20f,
      volume = -0.1f
    )

    assertTrue(clip.hasUnsupportedExportEdits())
  }

  @Test
  fun volumeProcessorAppliesPcmGain() {
    val processor = VolumeAudioProcessor(0.5f)
    processor.configure(AudioProcessor.AudioFormat(44_100, 1, C.ENCODING_PCM_16BIT))
    processor.flush()

    val input = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).apply {
      putShort(1_000)
      putShort(-2_000)
      flip()
    }
    processor.queueInput(input)

    val output = processor.output.order(ByteOrder.LITTLE_ENDIAN)
    assertEquals(500, output.short.toInt())
    assertEquals(-1_000, output.short.toInt())
  }

  @Test
  fun keyframedVolumeProcessorAppliesFadeEnvelope() {
    val processor = KeyframedVolumeAudioProcessor(
      baseGain = 1f,
      durationMs = 4L,
      fadeInMs = 2L,
      fadeOutMs = 2L,
      volumeKeyframes = emptyList()
    )
    processor.configure(AudioProcessor.AudioFormat(1_000, 1, C.ENCODING_PCM_16BIT))
    processor.flush()

    val input = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).apply {
      repeat(4) { putShort(10_000) }
      flip()
    }
    processor.queueInput(input)

    val output = processor.output.order(ByteOrder.LITTLE_ENDIAN)
    assertEquals(0, output.short.toInt())
    assertEquals(5_000, output.short.toInt())
    assertEquals(10_000, output.short.toInt())
    assertEquals(5_000, output.short.toInt())
  }

  @Test
  fun audioEnvelopeAutomationIsAllowedButAdvancedAudioIsBlocked() {
    val clip = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      keyframes = mapOf(
        "volume" to listOf(
          Keyframe(timeMs = 0L, value = 0.2f),
          Keyframe(timeMs = 1_000L, value = 0.8f)
        )
      ),
      audioEffects = AudioEffects(fadeInMs = 100L, fadeOutMs = 100L)
    )
    val audio = AudioClip(
      sourceUri = "content://media/audio/1",
      startTimeOnTimelineMs = 0L,
      sourceDurationMs = 1_000L,
      trimEndMs = 1_000L,
      keyframes = mapOf("volume" to listOf(Keyframe(timeMs = 0L, value = 0.4f))),
      audioEffects = AudioEffects(fadeInMs = 100L, fadeOutMs = 100L)
    )

    assertFalse(EditorState(clips = listOf(clip), audioClips = listOf(audio)).hasUnsupportedExportEdits())
    assertTrue(EditorState(clips = listOf(clip.copy(audioEffects = AudioEffects(eqPreset = "Bass")))).hasUnsupportedExportEdits())
    assertTrue(EditorState(clips = listOf(clip.copy(keyframes = mapOf("pan" to listOf(Keyframe(timeMs = 0L, value = 0f)))))).hasUnsupportedExportEdits())
  }

  @Test
  fun supportedAdvancedAudioProcessorsAreAllowed() {
    val effects = AudioEffects(
      eqPreset = "Bass Boost",
      reverbPreset = "Room",
      delayTimeMs = 80L,
      delayFeedback = 0.4f,
      pitchSemitones = 3f,
      distortion = 0.35f
    )
    val clip = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L,
      audioEffects = effects
    )

    assertFalse(EditorState(clips = listOf(clip)).hasUnsupportedExportEdits())
    assertTrue(
      EditorState(clips = listOf(clip.copy(audioEffects = effects.copy(delayTimeMs = 1_001L))))
        .hasUnsupportedExportEdits()
    )
  }

  @Test
  fun distortionAndDelayProcessorsChangePcmSamples() {
    val distortion = DistortionAudioProcessor(0.8f)
    distortion.configure(AudioProcessor.AudioFormat(1_000, 1, C.ENCODING_PCM_FLOAT))
    distortion.flush()
    val distortionInput = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).apply {
      putFloat(0.8f)
      flip()
    }
    distortion.queueInput(distortionInput)
    val distorted = distortion.output.order(ByteOrder.LITTLE_ENDIAN).float
    assertTrue("Distortion should change the sample", kotlin.math.abs(distorted - 0.8f) > 0.01f)

    val delay = FeedbackDelayAudioProcessor(delayMs = 10L, feedback = 0.4f)
    delay.configure(AudioProcessor.AudioFormat(1_000, 1, C.ENCODING_PCM_FLOAT))
    delay.flush()
    val delayInput = ByteBuffer.allocate(14 * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
      putFloat(1f)
      repeat(13) { putFloat(0f) }
      flip()
    }
    delay.queueInput(delayInput)
    val delayedOutput = delay.output.order(ByteOrder.LITTLE_ENDIAN)
    repeat(10) { delayedOutput.float }
    assertTrue("Delay should produce an echo", delayedOutput.float > 0.1f)
  }

  @Test
  fun chromaKeyRemovesKeyColorAndPreservesForeground() {
    val settings = ChromaKeySettings(
      enabled = true,
      keyColorArgb = 0xff00ff00L,
      similarity = 0.05f,
      smoothness = 0.01f
    )

    assertEquals(0, chromaKeyPixelArgb(0xff00ff00.toInt(), settings) ushr 24 and 0xff)
    assertEquals(255, chromaKeyPixelArgb(0xffff0000.toInt(), settings) ushr 24 and 0xff)
  }

  @Test
  fun photoAndVideoChromaKeyAreAllowedButGifChromaKeyIsBlocked() {
    val photo = OverlayClip(
      sourceUri = "content://media/image/1",
      originalDurationMs = 1_000L,
      isPhoto = true,
      trimEndMs = 1_000L,
      chromaKey = ChromaKeySettings(enabled = true)
    )
    val video = photo.copy(isPhoto = false)
    val gif = photo.copy(isPhoto = false, isGif = true)

    assertFalse(EditorState(overlays = listOf(photo)).hasUnsupportedExportEdits())
    assertFalse(EditorState(overlays = listOf(video)).hasUnsupportedExportEdits())
    assertTrue(EditorState(overlays = listOf(gif)).hasUnsupportedExportEdits())
  }

  @Test
  fun timelineMapperMapsAcrossTrimmedClips() {
    val first = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 10_000L,
      trimStartMs = 1_000L,
      trimEndMs = 5_000L
    )
    val second = MediaClip(
      sourceUri = "content://media/video/2",
      originalDurationMs = 8_000L,
      trimStartMs = 2_000L,
      trimEndMs = 6_000L
    )
    val mapper = TimelineMapper(listOf(first, second))

    assertEquals(8_000L, mapper.totalDurationMs)
    assertEquals(PlayerSeekPosition(0, 2_000L), mapper.playerSeekPosition(2_000L))
    assertEquals(PlayerSeekPosition(1, 0L), mapper.playerSeekPosition(4_000L))
    assertEquals(6_000L, mapper.globalPositionForPlayer(1, 2_000L))
  }

  @Test
  fun timelineMapperAccountsForConstantSpeed() {
    val clip = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 10_000L,
      trimEndMs = 10_000L,
      playbackSpeed = 2f
    )
    val mapper = TimelineMapper(listOf(clip))

    assertEquals(5_000L, mapper.totalDurationMs)
    assertEquals(PlayerSeekPosition(0, 4_000L), mapper.playerSeekPosition(2_000L))
    assertEquals(2_000L, mapper.globalPositionForPlayer(0, 4_000L))
  }

  @Test
  fun invalidTrimWindowIsClampedToSourceDuration() {
    val clip = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 10_000L,
      trimStartMs = -2_000L,
      trimEndMs = 25_000L
    )

    assertEquals(10_000L, clip.durationMs)
    assertEquals(0L, clip.effectiveTrimStartMs)
    assertEquals(10_000L, clip.effectiveTrimEndMs)
    assertEquals(10_000L, TimelineMapper(listOf(clip)).totalDurationMs)
  }
}
