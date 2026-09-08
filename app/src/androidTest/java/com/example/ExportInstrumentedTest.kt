package com.example

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin
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

    @Test
    fun staticLayersAndColorEditsAreRenderedAndPublished() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-layers-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(210, 70, 40))
        }
        source.outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()

        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            cropRect = androidx.compose.ui.geometry.Rect(0.05f, 0.05f, 0.95f, 0.95f),
            filterType = FilterType.WARM,
            filterIntensity = 0.7f,
            adjustments = ColorAdjustments(brightness = 8f, saturation = 12f),
            effects = listOf(AppliedEffect(type = EffectType.GAUSSIAN_BLUR, intensity = 0.05f)),
            transitionNext = Transition(TransitionType.FADE_TO_BLACK, 200L)
        )
        val state = EditorState(
            clips = listOf(clip),
            texts = listOf(TextOverlay(text = "Clipp", durationMs = 1_000L, fontSize = 42f)),
            stickers = listOf(
                StickerOverlay(
                    modelId = "star",
                    content = "★",
                    category = StickerCategory.SHAPE,
                    durationMs = 1_000L,
                    posX = 0.2f,
                    posY = 0.2f
                )
            ),
            drawings = listOf(
                DrawOverlay(
                    durationMs = 1_000L,
                    strokes = listOf(
                        DrawStroke(
                            path = listOf(NormalizedOffset(0.1f, 0.1f), NormalizedOffset(0.9f, 0.9f)),
                            color = Color.Cyan,
                            width = 0.01f,
                            brushType = BrushType.PEN
                        )
                    )
                )
            ),
            frames = listOf(FrameOverlay(typeId = "clean_solid", durationMs = 1_000L, color = Color.White)),
            captions = listOf(AutoCaptionSegment(text = "caption", words = emptyList(), startTimeMs = 0L, durationMs = 1_000L))
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_layers.mp4", state)
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            source.delete()
        }
    }

    @Test
    fun transformClipKeyframesAreRenderedAndPublished() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-keyframes-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(60, 180, 110))
        }
        source.outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()

        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            posX = 0.4f,
            keyframes = mapOf(
                "posX" to listOf(Keyframe(timeMs = 0L, value = 0.4f), Keyframe(timeMs = 1_000L, value = 0.6f)),
                "posY" to listOf(Keyframe(timeMs = 0L, value = 0.45f), Keyframe(timeMs = 1_000L, value = 0.55f)),
                "scale" to listOf(Keyframe(timeMs = 0L, value = 0.9f), Keyframe(timeMs = 1_000L, value = 1.05f)),
                "rotation" to listOf(Keyframe(timeMs = 0L, value = -10f), Keyframe(timeMs = 1_000L, value = 10f))
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_keyframes.mp4")
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFramesDiffer(context, result!!, 100_000L, 800_000L)
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            source.delete()
        }
    }

    @Test
    fun simpleLayerAnimationsAreRenderedAndPublished() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-animated-layers-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(170, 80, 210))
        }
        source.outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()

        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true
        )
        val state = EditorState(
            clips = listOf(clip),
            texts = listOf(
                TextOverlay(
                    text = "Animated",
                    durationMs = 1_000L,
                    posY = 0.35f,
                    animIn = TextAnimIn.FADE_IN,
                    animLoop = TextAnimLoop.PULSE,
                    animOut = TextAnimOut.SCALE_OUT
                )
            ),
            stickers = listOf(
                StickerOverlay(
                    modelId = "star",
                    content = "★",
                    category = StickerCategory.SHAPE,
                    durationMs = 1_000L,
                    posX = 0.2f,
                    posY = 0.75f,
                    animIn = TextAnimIn.ROTATE_IN,
                    animLoop = TextAnimLoop.SWING,
                    animOut = TextAnimOut.FADE_OUT
                )
            ),
            overlays = listOf(
                OverlayClip(
                    sourceUri = Uri.fromFile(source).toString(),
                    originalDurationMs = 1_000L,
                    isPhoto = true,
                    trimEndMs = 1_000L,
                    posX = 0.8f,
                    posY = 0.75f,
                    scaleX = 0.2f,
                    scaleY = 0.2f,
                    entranceAnim = OverlayAnim.FADE,
                    exitAnim = OverlayAnim.SCALE,
                    chromaKey = ChromaKeySettings(enabled = true)
                )
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_animated_layers.mp4", state)
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFramesDiffer(context, result!!, 100_000L, 800_000L)
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            source.delete()
        }
    }

    @Test
    fun separateAudioTrackIsMixedIntoPublishedMp4() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-audio-base-", ".png", context.cacheDir)
        val audio = File.createTempFile("clipp-export-audio-", ".wav", context.cacheDir)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(40, 120, 220))
        }
        source.outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()
        writeMonoWav(audio, durationMs = 1_000L)

        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            isMuted = true
        )
        val state = EditorState(
            clips = listOf(clip),
            audioClips = listOf(
                AudioClip(
                    sourceUri = Uri.fromFile(audio).toString(),
                    displayName = "Music",
                    startTimeOnTimelineMs = 0L,
                    sourceDurationMs = 1_000L,
                    trimEndMs = 1_000L,
                    volume = 0.65f,
                    keyframes = mapOf(
                        "volume" to listOf(
                            Keyframe(timeMs = 0L, value = 0.2f),
                            Keyframe(timeMs = 1_000L, value = 0.8f)
                        )
                    ),
                    audioEffects = AudioEffects(fadeInMs = 200L, fadeOutMs = 200L)
                )
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_audio.mp4", state)
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!, requireAudio = true)
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            source.delete()
            audio.delete()
        }
    }

    private fun awaitExport(
        exporter: VideoExporter,
        clips: List<MediaClip>,
        outputName: String,
        editorState: EditorState = EditorState(clips = clips)
    ): Outcome {
        val completed = CountDownLatch(1)
        val outcome = AtomicReference<Outcome>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            exporter.export(
                clips = clips,
                outputName = outputName,
                editorState = editorState,
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
        result: Outcome,
        requireAudio: Boolean = false
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
            if (requireAudio) {
                assertTrue(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes")
            }
        } finally {
            retriever.release()
        }
    }

    private fun assertFramesDiffer(
        context: android.content.Context,
        result: Outcome,
        firstFrameUs: Long,
        secondFrameUs: Long
    ) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, result.uri!!)
            val first = retriever.getFrameAtTime(
                firstFrameUs,
                android.media.MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: error("Missing first keyframe output frame")
            val second = retriever.getFrameAtTime(
                secondFrameUs,
                android.media.MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: error("Missing second keyframe output frame")
            try {
                assertTrue("Keyframed transform did not change exported pixels", !first.sameAs(second))
            } finally {
                first.recycle()
                second.recycle()
            }
        } finally {
            retriever.release()
        }
    }

    private fun writeMonoWav(file: File, durationMs: Long) {
        val sampleRate = 44_100
        val sampleCount = (sampleRate * durationMs / 1_000L).toInt()
        val dataSize = sampleCount * 2
        FileOutputStream(file).use { output ->
            fun writeAscii(value: String) = output.write(value.toByteArray(Charsets.US_ASCII))
            fun writeIntLe(value: Int) {
                output.write(value and 0xFF)
                output.write((value shr 8) and 0xFF)
                output.write((value shr 16) and 0xFF)
                output.write((value shr 24) and 0xFF)
            }
            fun writeShortLe(value: Int) {
                output.write(value and 0xFF)
                output.write((value shr 8) and 0xFF)
            }
            writeAscii("RIFF")
            writeIntLe(36 + dataSize)
            writeAscii("WAVEfmt ")
            writeIntLe(16)
            writeShortLe(1)
            writeShortLe(1)
            writeIntLe(sampleRate)
            writeIntLe(sampleRate * 2)
            writeShortLe(2)
            writeShortLe(16)
            writeAscii("data")
            writeIntLe(dataSize)
            for (i in 0 until sampleCount) {
                val sample = (sin(2.0 * PI * 440.0 * i / sampleRate) * 8_000.0).toInt()
                writeShortLe(sample)
            }
        }
    }

    private data class Outcome(
        val uri: Uri? = null,
        val metadata: ExportMetadata? = null,
        val error: String? = null
    )
}
