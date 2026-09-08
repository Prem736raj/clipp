package com.example

import androidx.compose.ui.geometry.Rect
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.data.ProjectEntity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun newProjectsStartWithHonestLocalMetadata() {
    val project = ProjectEntity(name = "Test", duration = "00:01")

    assertEquals("local_only", project.syncStatus)
    assertEquals(0L, project.sizeBytes)
    assertEquals(emptyList<String>(), project.sourceMediaPaths)
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
  fun transformClipKeyframesCanBeExportedButCropKeyframesAreBlocked() {
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
      keyframes = mapOf("cropLeft" to listOf(Keyframe(timeMs = 0L, value = 0f), Keyframe(timeMs = 1_000L, value = 0.1f)))
    )

    assertFalse(EditorState(clips = listOf(animatedTransform)).hasUnsupportedExportEdits())
    assertTrue(EditorState(clips = listOf(animatedCrop)).hasUnsupportedExportEdits())
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
    assertTrue(EditorState(clips = listOf(clip), texts = listOf(text.copy(animIn = TextAnimIn.TYPEWRITER))).hasUnsupportedExportEdits())
    assertTrue(EditorState(clips = listOf(clip), overlays = listOf(imageOverlay.copy(entranceAnim = OverlayAnim.SLIDE))).hasUnsupportedExportEdits())
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
  fun photoChromaKeyIsAllowedButVideoChromaKeyIsBlocked() {
    val photo = OverlayClip(
      sourceUri = "content://media/image/1",
      originalDurationMs = 1_000L,
      isPhoto = true,
      trimEndMs = 1_000L,
      chromaKey = ChromaKeySettings(enabled = true)
    )
    val video = photo.copy(isPhoto = false)

    assertFalse(EditorState(overlays = listOf(photo)).hasUnsupportedExportEdits())
    assertTrue(EditorState(overlays = listOf(video)).hasUnsupportedExportEdits())
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
