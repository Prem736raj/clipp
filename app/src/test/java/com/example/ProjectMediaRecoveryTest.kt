package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectMediaRecoveryTest {
    @Test
    fun relinkingPrimaryUriUpdatesCurrentUndoAndOverlayWhileLinkedAudioKeepsClipBinding() {
        val clip = MediaClip(
            id = "clip-1",
            sourceUri = "file:///missing.mp4",
            originalDurationMs = 4_000L,
            trimStartMs = 500L,
            trimEndMs = 3_500L,
            playbackSpeed = 1.25f
        )
        val overlay = OverlayClip(
            id = "overlay-1",
            sourceUri = clip.sourceUri,
            originalDurationMs = 4_000L,
            trimEndMs = 2_000L,
            startTimeOnTimelineMs = 700L
        )
        val linkedAudio = AudioClip(
            id = "audio-1",
            sourceClipId = clip.id,
            startTimeOnTimelineMs = 200L,
            sourceDurationMs = 4_000L,
            trimEndMs = 3_000L
        )
        val state = EditorState(
            clips = listOf(clip),
            overlays = listOf(overlay),
            audioClips = listOf(linkedAudio)
        )
        val issue = ProjectMediaIssue(
            sourceUri = clip.sourceUri,
            references = ProjectMediaRecovery.references(state).filter { it.sourceUri == clip.sourceUri }
        )
        val history = EditorHistoryModel(
            undoStack = listOf(HistoryAction(state, "before relink")),
            currentState = state
        )

        val updated = ProjectMediaRecovery.relink(history, issue, "content://replacement/video")
        val restored = updated.restoredEditorState()!!

        assertEquals("content://replacement/video", restored.clips.single().sourceUri)
        assertEquals("content://replacement/video", restored.overlays.single().sourceUri)
        assertEquals("clip-1", restored.audioClips.single().sourceClipId)
        assertNull(restored.audioClips.single().sourceUri)
        assertEquals(500L, restored.clips.single().trimStartMs)
        assertEquals(3_500L, restored.clips.single().trimEndMs)
        assertEquals(1.25f, restored.clips.single().playbackSpeed)
        assertEquals("content://replacement/video", updated.undoStack.single().state.clips.single().sourceUri)
    }

    @Test
    fun relinkingDirectAudioDoesNotChangeVisualMedia() {
        val state = EditorState(
            clips = listOf(
                MediaClip(
                    id = "clip",
                    sourceUri = "content://video",
                    originalDurationMs = 2_000L,
                    trimEndMs = 2_000L
                )
            ),
            audioClips = listOf(
                AudioClip(
                    id = "music",
                    sourceUri = "content://missing-audio",
                    startTimeOnTimelineMs = 0L,
                    sourceDurationMs = 2_000L,
                    trimEndMs = 2_000L
                )
            )
        )
        val issue = ProjectMediaIssue(
            sourceUri = "content://missing-audio",
            references = ProjectMediaRecovery.references(state).filter { it.ownerId == "music" }
        )

        val updated = ProjectMediaRecovery.relink(state, issue, "content://replacement-audio")

        assertEquals("content://video", updated.clips.single().sourceUri)
        assertEquals("content://replacement-audio", updated.audioClips.single().sourceUri)
    }

    @Test
    fun unresolvedLinkedAudioCanBeConvertedToDirectReplacement() {
        val state = EditorState(
            audioClips = listOf(
                AudioClip(
                    id = "orphan-audio",
                    sourceClipId = "deleted-clip",
                    startTimeOnTimelineMs = 0L,
                    sourceDurationMs = 1_500L,
                    trimEndMs = 1_500L
                )
            )
        )
        val references = ProjectMediaRecovery.references(state)
        assertEquals(1, references.size)
        assertTrue(references.single().sourceUri.isBlank())
        val issue = ProjectMediaIssue("", references)

        val updated = ProjectMediaRecovery.relink(state, issue, "content://replacement-audio")

        assertNull(updated.audioClips.single().sourceClipId)
        assertEquals("content://replacement-audio", updated.audioClips.single().sourceUri)
    }

    @Test
    fun legacyVisualIssueInfersImagePickerForMissingPhoto() {
        val issue = ProjectMediaRecovery.legacyVisualIssue("file:///storage/missing-frame.png")

        assertEquals(ProjectMediaExpectation.IMAGE, issue.references.single().expectation)
        assertEquals(listOf("image/*"), issue.acceptedMimeTypes().toList())
    }
}
