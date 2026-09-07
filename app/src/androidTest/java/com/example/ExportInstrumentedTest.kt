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
        val completed = CountDownLatch(1)
        val outcome = AtomicReference<Outcome>()
        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            rotation = 90f,
            flipHorizontal = true
        )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            exporter.export(
                clips = listOf(clip),
                outputName = "Clipp_instrumented_photo.mp4",
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

        try {
            assertTrue("Export did not complete", completed.await(60, TimeUnit.SECONDS))
            val result = outcome.get()
            assertNotNull(result)
            assertNull(result?.error)
            assertNotNull(result?.uri)
            assertTrue((result?.metadata?.durationMs ?: 0L) > 0L)
            assertTrue((result?.metadata?.fileSizeBytes ?: 0L) > 0L)

            val publishedUri = result!!.uri!!
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
            context.contentResolver.delete(publishedUri, null, null)
        } finally {
            exporter.close()
            source.delete()
        }
    }

    private data class Outcome(
        val uri: Uri? = null,
        val metadata: ExportMetadata? = null,
        val error: String? = null
    )
}
