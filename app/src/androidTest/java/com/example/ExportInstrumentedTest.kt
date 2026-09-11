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
import kotlinx.coroutines.runBlocking
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
    fun multipleSourceClipsAreConcatenatedInOrderAndDuration() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstSource = File.createTempFile("clipp-export-multi-first-", ".png", context.cacheDir)
        val secondSource = File.createTempFile("clipp-export-multi-second-", ".png", context.cacheDir)
        writeSolidPng(firstSource, android.graphics.Color.RED)
        writeSolidPng(secondSource, android.graphics.Color.BLUE)

        val firstClip = MediaClip(
            sourceUri = Uri.fromFile(firstSource).toString(),
            originalDurationMs = 700L,
            trimEndMs = 700L,
            isPhoto = true
        )
        val secondClip = MediaClip(
            sourceUri = Uri.fromFile(secondSource).toString(),
            originalDurationMs = 900L,
            trimEndMs = 900L,
            isPhoto = true
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(
                exporter,
                listOf(firstClip, secondClip),
                "Clipp_instrumented_multi_clip.mp4"
            )
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            val durationMs = result!!.metadata!!.durationMs
            assertTrue("Unexpected multi-clip duration: $durationMs", durationMs in 1_400L..1_800L)
            assertFrameHasDominantColor(context, result!!, 200_000L, expected = "red")
            assertFrameHasDominantColor(context, result!!, 1_000_000L, expected = "blue")
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            firstSource.delete()
            secondSource.delete()
        }
    }

    @Test
    fun multipleVideoSourceClipsAreConcatenatedInOrderAndDuration() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstSource = File.createTempFile("clipp-export-video-first-", ".png", context.cacheDir)
        val secondSource = File.createTempFile("clipp-export-video-second-", ".png", context.cacheDir)
        writeSolidPng(firstSource, android.graphics.Color.RED)
        writeSolidPng(secondSource, android.graphics.Color.GREEN)

        val sourceExporter = VideoExporter(context)
        val exporter = VideoExporter(context)
        var firstVideo: Outcome? = null
        var secondVideo: Outcome? = null
        var result: Outcome? = null
        try {
            firstVideo = awaitExport(
                sourceExporter,
                listOf(
                    MediaClip(
                        sourceUri = Uri.fromFile(firstSource).toString(),
                        originalDurationMs = 1_000L,
                        trimEndMs = 1_000L,
                        isPhoto = true
                    )
                ),
                "Clipp_instrumented_video_source_first.mp4"
            )
            secondVideo = awaitExport(
                sourceExporter,
                listOf(
                    MediaClip(
                        sourceUri = Uri.fromFile(secondSource).toString(),
                        originalDurationMs = 1_000L,
                        trimEndMs = 1_000L,
                        isPhoto = true
                    )
                ),
                "Clipp_instrumented_video_source_second.mp4"
            )
            assertSuccessful(firstVideo!!)
            assertSuccessful(secondVideo!!)

            val firstDurationMs = firstVideo!!.metadata!!.durationMs
            val secondDurationMs = secondVideo!!.metadata!!.durationMs
            val firstClip = MediaClip(
                sourceUri = firstVideo!!.uri!!.toString(),
                originalDurationMs = firstDurationMs,
                trimEndMs = firstDurationMs
            )
            val secondClip = MediaClip(
                sourceUri = secondVideo!!.uri!!.toString(),
                originalDurationMs = secondDurationMs,
                trimEndMs = secondDurationMs
            )

            result = awaitExport(
                exporter,
                listOf(firstClip, secondClip),
                "Clipp_instrumented_multi_video_clip.mp4"
            )
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            val expectedDurationMs = firstDurationMs + secondDurationMs
            assertTrue(
                "Unexpected multi-video duration: ${result!!.metadata!!.durationMs}",
                result!!.metadata!!.durationMs in (expectedDurationMs - 250L)..(expectedDurationMs + 250L)
            )
            assertFrameHasDominantColor(context, result!!, 200_000L, expected = "red")
            assertFrameHasDominantColor(
                context,
                result!!,
                (firstDurationMs + 200L) * 1_000L,
                expected = "green"
            )
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            firstVideo?.uri?.let { context.contentResolver.delete(it, null, null) }
            secondVideo?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            sourceExporter.close()
            firstSource.delete()
            secondSource.delete()
        }
    }

    @Test
    fun localTemplateCreatesRestorableStateAndExports() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstSource = File.createTempFile("clipp-template-first-", ".png", context.cacheDir)
        val secondSource = File.createTempFile("clipp-template-second-", ".png", context.cacheDir)
        writeSolidPng(firstSource, android.graphics.Color.rgb(220, 70, 50))
        writeSolidPng(secondSource, android.graphics.Color.rgb(40, 120, 220))
        val mediaUris = listOf(Uri.fromFile(firstSource).toString(), Uri.fromFile(secondSource).toString())
        val template = TemplateRepo.find("local_cinematic_story")!!
        val project = runBlocking {
            TemplateProjectFactory.build(
                context = context,
                template = template,
                mediaUris = mediaUris,
                textValues = listOf("Travel day", "A local template")
            )
        }
        assertNotNull(project)
        assertTrue(project!!.historyState.isNotBlank())
        assertTrue(project.sourceMediaPaths == mediaUris)
        val restoredState = editorHistoryMoshi
            .adapter(EditorHistoryModel::class.java)
            .fromJson(project.historyState)
            ?.currentState
        assertNotNull(restoredState)
        assertTrue(restoredState!!.clips.size == 2)
        assertTrue(restoredState.texts.size == 2)
        assertTrue(restoredState.clips.all { it.transitionNext.type == TransitionType.FADE_TO_WHITE || it == restoredState.clips.last() })

        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(
                exporter,
                restoredState.clips,
                "Clipp_instrumented_template.mp4",
                restoredState
            )
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            firstSource.delete()
            secondSource.delete()
        }
    }

    @Test
    fun batchRunnerExportsMultipleSavedProjectsSequentially() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstSource = File.createTempFile("clipp-batch-first-", ".png", context.cacheDir)
        val secondSource = File.createTempFile("clipp-batch-second-", ".png", context.cacheDir)
        writeSolidPng(firstSource, android.graphics.Color.rgb(210, 70, 40))
        writeSolidPng(secondSource, android.graphics.Color.rgb(40, 120, 220))
        val firstUri = Uri.fromFile(firstSource).toString()
        val secondUri = Uri.fromFile(secondSource).toString()
        val projects = listOf(
            com.example.data.ProjectEntity(
                name = "Batch First",
                duration = "00:01",
                sourceMediaPaths = listOf(firstUri)
            ),
            com.example.data.ProjectEntity(
                name = "Batch Second",
                duration = "00:01",
                sourceMediaPaths = listOf(secondUri)
            )
        )

        val results = try {
            runBlocking {
                BatchExportRunner(context).export(projects)
            }
        } finally {
            firstSource.delete()
            secondSource.delete()
        }

        try {
            assertTrue(results.size == 2)
            assertTrue(
                results.joinToString { result ->
                    "${result.projectName}: succeeded=${result.succeeded}, error=${result.errorMessage}, duration=${result.metadata?.durationMs}"
                },
                results.all { it.succeeded && (it.metadata?.durationMs ?: 0L) > 0L }
            )
            results.mapNotNull { it.outputUri }.forEach { outputUri ->
                assertPublishedMp4(context, Outcome(outputUri, null, null))
            }
        } finally {
            results.mapNotNull { it.outputUri }.forEach { outputUri ->
                context.contentResolver.delete(outputUri, null, null)
            }
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
            effects = listOf(
                AppliedEffect(type = EffectType.GAUSSIAN_BLUR, intensity = 0.05f),
                AppliedEffect(type = EffectType.MIRROR),
                AppliedEffect(type = EffectType.SHAKE),
                AppliedEffect(type = EffectType.COMIC_BOOK),
                AppliedEffect(type = EffectType.PENCIL_SKETCH),
                AppliedEffect(type = EffectType.POP_ART),
                AppliedEffect(type = EffectType.FILM_GRAIN),
                AppliedEffect(type = EffectType.ANAMORPHIC_FLARE),
                AppliedEffect(type = EffectType.SPARKLE)
            ),
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
    fun proceduralCinematicEffectsChangePublishedFrames() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-cinematic-effects-", ".png", context.cacheDir)
        writeSolidPng(source, android.graphics.Color.rgb(120, 120, 120))
        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            effects = listOf(
                AppliedEffect(type = EffectType.FILM_GRAIN, intensity = 0.8f),
                AppliedEffect(type = EffectType.SPARKLE, intensity = 0.8f),
                AppliedEffect(type = EffectType.ANAMORPHIC_FLARE, intensity = 0.8f)
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_cinematic_effects.mp4")
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
    fun cropKeyframesAreRenderedAndPublished() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-crop-keyframes-", ".png", context.cacheDir)
        writeVerticalSplitPng(source, android.graphics.Color.RED, android.graphics.Color.BLUE)
        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            keyframes = mapOf(
                "cropLeft" to listOf(Keyframe(timeMs = 0L, value = 0f), Keyframe(timeMs = 1_000L, value = 0.5f)),
                "cropTop" to listOf(Keyframe(timeMs = 0L, value = 0f), Keyframe(timeMs = 1_000L, value = 0f)),
                "cropRight" to listOf(Keyframe(timeMs = 0L, value = 0.5f), Keyframe(timeMs = 1_000L, value = 1f)),
                "cropBottom" to listOf(Keyframe(timeMs = 0L, value = 1f), Keyframe(timeMs = 1_000L, value = 1f))
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_crop_keyframes.mp4")
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFrameHasDominantColor(context, result!!, 100_000L, expected = "red")
            assertFrameHasDominantColor(context, result!!, 800_000L, expected = "blue")
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
    fun animatedVideoOverlayIsRenderedAndPublished() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val baseSource = File.createTempFile("clipp-export-video-overlay-base-", ".png", context.cacheDir)
        val redSource = File.createTempFile("clipp-export-video-overlay-red-", ".png", context.cacheDir)
        val greenSource = File.createTempFile("clipp-export-video-overlay-green-", ".png", context.cacheDir)
        writeSolidPng(baseSource, android.graphics.Color.BLUE)
        writeSolidPng(redSource, android.graphics.Color.RED)
        writeSolidPng(greenSource, android.graphics.Color.GREEN)

        val overlayExporter = VideoExporter(context)
        val exporter = VideoExporter(context)
        var overlayResult: Outcome? = null
        var result: Outcome? = null
        try {
            overlayResult = awaitExport(
                overlayExporter,
                listOf(
                    MediaClip(
                        sourceUri = Uri.fromFile(redSource).toString(),
                        originalDurationMs = 1_000L,
                        trimEndMs = 1_000L,
                        isPhoto = true
                    ),
                    MediaClip(
                        sourceUri = Uri.fromFile(greenSource).toString(),
                        originalDurationMs = 1_000L,
                        trimEndMs = 1_000L,
                        isPhoto = true
                    )
                ),
                "Clipp_instrumented_video_overlay_source.mp4"
            )
            assertSuccessful(overlayResult!!)

            val overlayDurationMs = overlayResult!!.metadata!!.durationMs
            val baseClip = MediaClip(
                sourceUri = Uri.fromFile(baseSource).toString(),
                originalDurationMs = overlayDurationMs,
                trimEndMs = overlayDurationMs,
                isPhoto = true
            )
            val state = EditorState(
                clips = listOf(baseClip),
                overlays = listOf(
                    OverlayClip(
                        sourceUri = overlayResult!!.uri!!.toString(),
                        originalDurationMs = overlayDurationMs,
                        isPhoto = false,
                        trimEndMs = overlayDurationMs,
                        scaleX = 0.5f,
                        scaleY = 0.5f,
                        keyframes = mapOf(
                            "opacity" to listOf(
                                Keyframe(timeMs = 0L, value = 1f),
                                Keyframe(timeMs = overlayDurationMs, value = 1f)
                            )
                        )
                    )
                )
            )
            result = awaitExport(
                exporter,
                listOf(baseClip),
                "Clipp_instrumented_video_overlay.mp4",
                state
            )
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFrameHasDominantColor(context, result!!, 200_000L, expected = "red")
            assertFrameHasDominantColor(context, result!!, 1_200_000L, expected = "green")
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            overlayResult?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            overlayExporter.close()
            baseSource.delete()
            redSource.delete()
            greenSource.delete()
        }
    }

    @Test
    fun videoOverlayChromaKeyIsRenderedAndPublished() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val baseSource = File.createTempFile("clipp-export-video-chroma-base-", ".png", context.cacheDir)
        val redSource = File.createTempFile("clipp-export-video-chroma-red-", ".png", context.cacheDir)
        val greenSource = File.createTempFile("clipp-export-video-chroma-green-", ".png", context.cacheDir)
        writeSolidPng(baseSource, android.graphics.Color.BLUE)
        writeSolidPng(redSource, android.graphics.Color.RED)
        writeSolidPng(greenSource, android.graphics.Color.GREEN)

        val overlayExporter = VideoExporter(context)
        val exporter = VideoExporter(context)
        var overlayResult: Outcome? = null
        var result: Outcome? = null
        try {
            overlayResult = awaitExport(
                overlayExporter,
                listOf(
                    MediaClip(
                        sourceUri = Uri.fromFile(redSource).toString(),
                        originalDurationMs = 1_000L,
                        trimEndMs = 1_000L,
                        isPhoto = true
                    ),
                    MediaClip(
                        sourceUri = Uri.fromFile(greenSource).toString(),
                        originalDurationMs = 1_000L,
                        trimEndMs = 1_000L,
                        isPhoto = true
                    )
                ),
                "Clipp_instrumented_video_chroma_source.mp4"
            )
            assertSuccessful(overlayResult!!)

            val overlayDurationMs = overlayResult!!.metadata!!.durationMs
            val baseClip = MediaClip(
                sourceUri = Uri.fromFile(baseSource).toString(),
                originalDurationMs = overlayDurationMs,
                trimEndMs = overlayDurationMs,
                isPhoto = true
            )
            val state = EditorState(
                clips = listOf(baseClip),
                overlays = listOf(
                    OverlayClip(
                        sourceUri = overlayResult!!.uri!!.toString(),
                        originalDurationMs = overlayDurationMs,
                        isPhoto = false,
                        trimEndMs = overlayDurationMs,
                        scaleX = 0.5f,
                        scaleY = 0.5f,
                        chromaKey = ChromaKeySettings(
                            enabled = true,
                            keyColorArgb = 0xff00ff00L,
                            similarity = 0.16f,
                            smoothness = 0.08f
                        )
                    )
                )
            )
            result = awaitExport(
                exporter,
                listOf(baseClip),
                "Clipp_instrumented_video_chroma.mp4",
                state
            )
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFrameHasDominantColor(context, result!!, 200_000L, expected = "red")
            assertFrameHasDominantColor(context, result!!, 1_200_000L, expected = "blue")
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            overlayResult?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            overlayExporter.close()
            baseSource.delete()
            redSource.delete()
            greenSource.delete()
        }
    }

    @Test
    fun animatedGifOverlayIsRenderedAndPublished() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val baseSource = File.createTempFile("clipp-export-gif-overlay-base-", ".png", context.cacheDir)
        val gifSource = File.createTempFile("clipp-export-gif-overlay-", ".gif", context.cacheDir)
        writeSolidPng(baseSource, android.graphics.Color.BLUE)
        writeAnimatedGif(gifSource)

        val baseClip = MediaClip(
            sourceUri = Uri.fromFile(baseSource).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true
        )
        val state = EditorState(
            clips = listOf(baseClip),
            overlays = listOf(
                OverlayClip(
                    sourceUri = Uri.fromFile(gifSource).toString(),
                    originalDurationMs = 1_000L,
                    isPhoto = false,
                    isGif = true,
                    trimEndMs = 1_000L,
                    // The compact 2x2 fixture is deliberately enlarged so
                    // the exported frame remains measurable after H.264
                    // filtering; this test targets frame timing, not source
                    // asset resolution.
                    scaleX = 48f,
                    scaleY = 48f
                )
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(baseClip), "Clipp_instrumented_gif_overlay.mp4", state)
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFrameHasDominantColor(context, result!!, 200_000L, expected = "green")
            assertFrameHasDominantColor(context, result!!, 800_000L, expected = "red")
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            baseSource.delete()
            gifSource.delete()
        }
    }

    @Test
    fun proceduralLightEffectsChangePublishedFrames() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-light-effects-", ".png", context.cacheDir)
        writeSolidPng(source, android.graphics.Color.rgb(90, 110, 130))
        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            effects = listOf(
                AppliedEffect(type = EffectType.LIGHT_LEAK, intensity = 0.8f),
                AppliedEffect(type = EffectType.LENS_FLARE, intensity = 0.8f),
                AppliedEffect(type = EffectType.BOKEH, intensity = 0.8f)
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_light_effects.mp4")
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
    fun partialProceduralEffectIsRenderedOnlyInsideItsWindow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-timed-effect-", ".png", context.cacheDir)
        writeSolidPng(source, android.graphics.Color.rgb(90, 110, 130))
        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            effects = listOf(
                AppliedEffect(
                    type = EffectType.LIGHT_LEAK,
                    intensity = 1f,
                    startTimeMs = 300L,
                    endTimeMs = 700L
                )
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_timed_effect.mp4")
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFramesDiffer(context, result!!, 200_000L, 500_000L)
            assertFramesDiffer(context, result!!, 500_000L, 800_000L)
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            source.delete()
        }
    }

    @Test
    fun shaderDistortionEffectsAreRenderedIntoPublishedFrames() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = File.createTempFile("clipp-export-shader-effects-", ".png", context.cacheDir)
        writeVerticalSplitPng(source, android.graphics.Color.RED, android.graphics.Color.BLUE)
        val clip = MediaClip(
            sourceUri = Uri.fromFile(source).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            effects = listOf(
                AppliedEffect(type = EffectType.RGB_SPLIT, intensity = 0.8f),
                AppliedEffect(type = EffectType.WAVE, intensity = 0.8f),
                AppliedEffect(type = EffectType.FISHEYE, intensity = 0.55f),
                AppliedEffect(type = EffectType.PIXELATE, intensity = 0.35f)
            )
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(clip), "Clipp_instrumented_shader_effects.mp4")
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
    fun photoCrossfadeRendersNextPhotoBeforeTheCut() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstSource = File.createTempFile("clipp-export-crossfade-first-", ".png", context.cacheDir)
        val secondSource = File.createTempFile("clipp-export-crossfade-second-", ".png", context.cacheDir)
        writeSolidPng(firstSource, android.graphics.Color.RED)
        writeSolidPng(secondSource, android.graphics.Color.BLUE)
        val firstClip = MediaClip(
            sourceUri = Uri.fromFile(firstSource).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true,
            transitionNext = Transition(TransitionType.CROSSFADE, 400L)
        )
        val secondClip = MediaClip(
            sourceUri = Uri.fromFile(secondSource).toString(),
            originalDurationMs = 1_000L,
            trimEndMs = 1_000L,
            isPhoto = true
        )
        val exporter = VideoExporter(context)
        var result: Outcome? = null
        try {
            result = awaitExport(exporter, listOf(firstClip, secondClip), "Clipp_instrumented_crossfade.mp4")
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFrameHasDominantColor(context, result!!, 200_000L, expected = "red")
            assertFrameIsMixed(context, result!!, 800_000L)
            assertFrameHasDominantColor(context, result!!, 1_100_000L, expected = "blue")
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            firstSource.delete()
            secondSource.delete()
        }
    }

    @Test
    fun simpleVideoCrossfadeRendersNextVideoBeforeTheCut() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstSource = File.createTempFile("clipp-export-video-crossfade-first-", ".png", context.cacheDir)
        val secondSource = File.createTempFile("clipp-export-video-crossfade-second-", ".png", context.cacheDir)
        writeSolidPng(firstSource, android.graphics.Color.RED)
        writeSolidPng(secondSource, android.graphics.Color.GREEN)

        val sourceExporter = VideoExporter(context)
        val exporter = VideoExporter(context)
        var firstVideo: Outcome? = null
        var secondVideo: Outcome? = null
        var result: Outcome? = null
        try {
            firstVideo = awaitExport(
                sourceExporter,
                listOf(
                    MediaClip(
                        sourceUri = Uri.fromFile(firstSource).toString(),
                        originalDurationMs = 1_000L,
                        trimEndMs = 1_000L,
                        isPhoto = true
                    )
                ),
                "Clipp_instrumented_video_crossfade_first.mp4"
            )
            secondVideo = awaitExport(
                sourceExporter,
                listOf(
                    MediaClip(
                        sourceUri = Uri.fromFile(secondSource).toString(),
                        originalDurationMs = 1_000L,
                        trimEndMs = 1_000L,
                        isPhoto = true
                    )
                ),
                "Clipp_instrumented_video_crossfade_second.mp4"
            )
            assertSuccessful(firstVideo!!)
            assertSuccessful(secondVideo!!)

            val firstDurationMs = firstVideo!!.metadata!!.durationMs
            val secondDurationMs = secondVideo!!.metadata!!.durationMs
            val firstClip = MediaClip(
                sourceUri = firstVideo!!.uri!!.toString(),
                originalDurationMs = firstDurationMs,
                trimEndMs = firstDurationMs,
                transitionNext = Transition(TransitionType.CROSSFADE, 400L)
            )
            val secondClip = MediaClip(
                sourceUri = secondVideo!!.uri!!.toString(),
                originalDurationMs = secondDurationMs,
                trimEndMs = secondDurationMs
            )

            result = awaitExport(
                exporter,
                listOf(firstClip, secondClip),
                "Clipp_instrumented_video_crossfade.mp4"
            )
            assertSuccessful(result!!)
            assertPublishedMp4(context, result!!)
            assertFrameHasDominantColor(context, result!!, 200_000L, expected = "red")
            assertFrameHasRedGreenMix(
                context,
                result!!,
                (firstDurationMs - 200L).coerceAtLeast(100L) * 1_000L
            )
            assertFrameHasDominantColor(
                context,
                result!!,
                (firstDurationMs + 200L) * 1_000L,
                expected = "green"
            )
        } finally {
            result?.uri?.let { context.contentResolver.delete(it, null, null) }
            firstVideo?.uri?.let { context.contentResolver.delete(it, null, null) }
            secondVideo?.uri?.let { context.contentResolver.delete(it, null, null) }
            exporter.close()
            sourceExporter.close()
            firstSource.delete()
            secondSource.delete()
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
                        .copy(
                            eqPreset = "Bass Boost",
                            reverbPreset = "Room",
                            reverbAmount = 0.35f,
                            delayTimeMs = 80L,
                            delayFeedback = 0.35f,
                            pitchSemitones = 3f,
                            distortion = 0.2f
                        )
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

    private fun assertFrameHasDominantColor(
        context: android.content.Context,
        result: Outcome,
        frameTimeUs: Long,
        expected: String
    ) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, result.uri!!)
            val frame = retriever.getFrameAtTime(
                frameTimeUs,
                android.media.MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: error("Missing frame at $frameTimeUs")
            try {
                val pixel = frame.getPixel(frame.width / 2, frame.height / 2)
                val red = (pixel ushr 16) and 0xff
                val green = (pixel ushr 8) and 0xff
                val blue = pixel and 0xff
                when (expected) {
                    "red" -> assertTrue("Expected red frame, got $red/$green/$blue", red > green + 40 && red > blue + 40)
                    "blue" -> assertTrue("Expected blue frame, got $red/$green/$blue", blue > red + 40 && blue > green + 40)
                    "green" -> assertTrue("Expected green frame, got $red/$green/$blue", green > red + 40 && green > blue + 40)
                    else -> error("Unknown expected color: $expected")
                }
            } finally {
                frame.recycle()
            }
        } finally {
            retriever.release()
        }
    }

    private fun assertFrameIsMixed(
        context: android.content.Context,
        result: Outcome,
        frameTimeUs: Long
    ) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, result.uri!!)
            val frame = retriever.getFrameAtTime(
                frameTimeUs,
                android.media.MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: error("Missing frame at $frameTimeUs")
            try {
                val pixel = frame.getPixel(frame.width / 2, frame.height / 2)
                val red = (pixel ushr 16) and 0xff
                val blue = pixel and 0xff
                assertTrue("Expected a crossfade frame, got $red/$blue", red > 30 && blue > 30)
            } finally {
                frame.recycle()
            }
        } finally {
            retriever.release()
        }
    }

    private fun assertFrameHasRedGreenMix(
        context: android.content.Context,
        result: Outcome,
        frameTimeUs: Long
    ) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, result.uri!!)
            val frame = retriever.getFrameAtTime(
                frameTimeUs,
                android.media.MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: error("Missing frame at $frameTimeUs")
            try {
                val pixel = frame.getPixel(frame.width / 2, frame.height / 2)
                val red = (pixel ushr 16) and 0xff
                val green = (pixel ushr 8) and 0xff
                val blue = pixel and 0xff
                assertTrue("Expected red/green transition frame, got $red/$green/$blue", red > 30 && green > 30 && red > blue + 20 && green > blue + 20)
            } finally {
                frame.recycle()
            }
        } finally {
            retriever.release()
        }
    }

    private fun writeSolidPng(file: File, color: Int) {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }
        file.outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()
    }

    private fun writeVerticalSplitPng(file: File, leftColor: Int, rightColor: Int) {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                bitmap.setPixel(x, y, if (x < bitmap.width / 2) leftColor else rightColor)
            }
        }
        file.outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()
    }

    private fun writeAnimatedGif(file: File) {
        file.writeBytes(
            byteArrayOf(
                0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
                0x02, 0x00, 0x02, 0x00, 0x80.toByte(), 0x00, 0x00,
                0x00, 0xff.toByte(), 0x00, 0xff.toByte(), 0x00, 0x00,
                0x21, 0xf9.toByte(), 0x04, 0x00, 0x32, 0x00, 0x00, 0x00,
                0x2c, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x02, 0x00, 0x00,
                0x02, 0x03, 0x04, 0x80.toByte(), 0x04, 0x00,
                0x21, 0xf9.toByte(), 0x04, 0x00, 0x32, 0x00, 0x00, 0x00,
                0x2c, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x02, 0x00, 0x00,
                0x02, 0x03, 0x0c, 0x92.toByte(), 0x04, 0x00,
                0x3b
            )
        )
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
