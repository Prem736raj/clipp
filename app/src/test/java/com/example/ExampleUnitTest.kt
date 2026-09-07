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

    val unsupported = EditorState(clips = listOf(basic.copy(posX = 0.2f)))
    assertTrue(unsupported.hasUnsupportedExportEdits())
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
