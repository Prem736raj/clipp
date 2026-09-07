package com.example

import androidx.compose.ui.geometry.Rect
import com.example.data.ProjectEntity
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
  fun basicTimelineCanBeExportedAndAdvancedEditsAreBlocked() {
    val basic = MediaClip(
      sourceUri = "content://media/video/1",
      originalDurationMs = 1_000L,
      trimEndMs = 1_000L
    )
    val advanced = basic.copy(cropRect = Rect(0f, 0f, 0.8f, 1f))

    assertFalse(basic.hasUnsupportedExportEdits())
    assertTrue(advanced.hasUnsupportedExportEdits())
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
