package com.example

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectEntity
import com.example.data.ProjectStorage
import java.io.File
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
}
