package com.example

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportInstrumentedTest {
    @Test
    fun localPhotoIsRenderedAndPublishedAsValidatedMp4() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-fixture-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(40, 120, 220))
        }
        source.outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()

        val exporter = VideoExporter(context)
        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            rotation = 90f,
            flipHorizontal = true
        )

        var photoResult: Outcome? = null
        var videoResult: Outcome? = null
        try {
            photoResult = awaitExport(exporter, listOf(clip), "Clipp_instrumented_photo.mp4")
            assertSuccessful(photoResult!!)
            assertPublishedMp4(context, photoResult!!)

            val photoUri = photoResult!!.uri!!
            val photoDurationMs = photoResult!!.metadata!!.durationMs
            val videoClip = MediaClip(
                sourceUri = photoUri.toString(),
                originalDurationMs = photoDurationMs,
                trimEndMs = photoDurationMs,
                playbackSpeed = 1.5f,
                rotation = -90f,
                flipVertical = true
            )
            videoResult = awaitExport(exporter, listOf(videoClip), "Clipp_instrumented_video.mp4")
            assertSuccessful(videoResult!!)
            assertPublishedMp4(context, videoResult!!)
        } finally {
            photoResult?.uri?.let { context.contentResolver.delete(it, null, null) }
            videoResult?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            source.delete()
        }
    }

    private fun awaitExport(
        exporter: VideoExporter,
        clips: List<MediaClip>,
        outputName: String
    ): Outcome {
        val completed = CountDownLatch(1)
        val outcome = AtomicReference<Outcome>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            exporter.export(
                clips = clips,
                outputName = outputName,
                onProgress = {},
                onSuccess = { uri, metadata ->
                    outcome.set(Outcome(uri = uri, metadata = metadata))
                    completed.countDown()
                },
                onError = { message ->
                    outcome.set(Outcome(error = message))
                    completed.countDown()
                }
            )
        }
        assertTrue("Export did not complete", completed.await(60, TimeUnit.SECONDS))
        return outcome.get() ?: error("Export completed without a result")
    }

    private fun assertSuccessful(result: Outcome) {
        assertNull(result.error)
        assertNotNull(result.uri)
        assertTrue((result.metadata?.durationMs ?: 0L) > 0L)
        assertTrue((result.metadata?.fileSizeBytes ?: 0L) > 0L)
    }

    private fun assertPublishedMp4(
        context: android.content.Context,
        result: Outcome
    ) {
        val publishedUri = result.uri!!
        context.contentResolver.openAssetFileDescriptor(publishedUri, "r")?.use { descriptor ->
            assertTrue(descriptor.length > 0L)
        } ?: error("Published output cannot be opened")

        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, publishedUri)
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            assertTrue(duration > 0L)
        } finally {
            retriever.release()
        }
    }

    private data class Outcome(
        val uri: Uri? = null,
        val metadata: ExportMetadata? = null,
        val error: String? = null
    )
}
