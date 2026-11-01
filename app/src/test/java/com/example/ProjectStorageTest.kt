package com.example

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectEntity
import com.example.data.ProjectStorage
import java.io.File
import com.example.AudioClip
import com.example.EditorHistoryModel
import com.example.EditorState
import com.example.editorHistoryMoshi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProjectStorageTest {
    @Test
    fun deletesOwnedThumbnailButNeverSourceMedia() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val thumbnail = File(context.cacheDir, "project-thumb.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val source = File.createTempFile("clipp-source-", ".mp4").apply {
            writeBytes(byteArrayOf(4, 5, 6))
        }
        val project = ProjectEntity(
            name = "Storage test",
            duration = "00:01",
            sourceMediaPaths = listOf(Uri.fromFile(source).toString()),
            thumbnailUri = Uri.fromFile(thumbnail).toString()
        )

        ProjectStorage.deleteOwnedFiles(context, project)

        assertFalse(thumbnail.exists())
        assertTrue(source.exists())
        source.delete()
    }

    @Test
    fun deletesProjectVoiceoverFromHistoryButNeverExternalSource() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val voiceover = File(context.filesDir, "voiceovers/project-voiceover.m4a").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val source = File.createTempFile("clipp-source-", ".mp4").apply {
            writeBytes(byteArrayOf(5, 6, 7, 8))
        }
        val history = editorHistoryMoshi.adapter(EditorHistoryModel::class.java).toJson(
            EditorHistoryModel(
                currentState = EditorState(
                    audioClips = listOf(
                        AudioClip(
                            sourceUri = Uri.fromFile(voiceover).toString(),
                            startTimeOnTimelineMs = 0L,
                            sourceDurationMs = 1_000L,
                            trimEndMs = 1_000L
                        )
                    )
                )
            )
        )
        val project = ProjectEntity(
            name = "Voiceover cleanup",
            duration = "00:01",
            sourceMediaPaths = listOf(Uri.fromFile(source).toString()),
            historyState = history
        )

        ProjectStorage.deleteOwnedFiles(context, project)

        assertFalse(voiceover.exists())
        assertTrue(source.exists())
        source.delete()
    }

    @Test
    fun deletesOwnedImportedSourceButNeverExternalSource() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val imported = File(context.filesDir, "imports/project-import.mp4").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val external = File.createTempFile("clipp-external-", ".mp4").apply {
            writeBytes(byteArrayOf(5, 6, 7, 8))
        }
        val project = ProjectEntity(
            name = "Imported source cleanup",
            duration = "00:01",
            sourceMediaPaths = listOf(
                Uri.fromFile(imported).toString(),
                Uri.fromFile(external).toString()
            )
        )

        ProjectStorage.deleteOwnedFiles(context, project)

        assertFalse(imported.exists())
        assertTrue(external.exists())
        external.delete()
    }

    @Test
    fun orphanCleanupPreservesReferencedAndExternalFiles() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val referencedVoiceover = File(context.filesDir, "voiceovers/referenced.m4a").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val orphanVoiceover = File(context.filesDir, "voiceovers/orphan.m4a").apply {
            writeBytes(byteArrayOf(5, 6, 7))
        }
        val orphanImport = File(context.filesDir, "imports/orphan.mp4").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(8, 9))
        }
        val staleExport = File(context.cacheDir, "export-${System.nanoTime()}.mp4").apply {
            writeBytes(byteArrayOf(10, 11, 12, 13, 14))
        }
        val unrelatedCache = File(context.cacheDir, "keep-${System.nanoTime()}.bin").apply {
            writeBytes(byteArrayOf(15))
        }
        val external = File.createTempFile("clipp-external-orphan-", ".mp4").apply {
            writeBytes(byteArrayOf(16, 17))
        }
        val history = editorHistoryMoshi.adapter(EditorHistoryModel::class.java).toJson(
            EditorHistoryModel(
                currentState = EditorState(
                    audioClips = listOf(
                        AudioClip(
                            sourceUri = Uri.fromFile(referencedVoiceover).toString(),
                            startTimeOnTimelineMs = 0L,
                            sourceDurationMs = 1_000L,
                            trimEndMs = 1_000L
                        )
                    )
                )
            )
        )
        val project = ProjectEntity(
            name = "Orphan cleanup",
            duration = "00:01",
            sourceMediaPaths = listOf(Uri.fromFile(external).toString()),
            historyState = history
        )

        val result = ProjectStorage.cleanupOrphanedOwnedFiles(context, listOf(project))

        assertTrue(referencedVoiceover.exists())
        assertFalse(orphanVoiceover.exists())
        assertFalse(orphanImport.exists())
        assertFalse(staleExport.exists())
        assertTrue(unrelatedCache.exists())
        assertTrue(external.exists())
        assertTrue(result.filesDeleted >= 3)
        assertTrue(result.bytesFreed >= 10L)
        unrelatedCache.delete()
        external.delete()
    }
}
