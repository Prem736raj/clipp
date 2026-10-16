package com.example

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.tanh

data class ExportMetadata(
    val durationMs: Long,
    val fileSizeBytes: Long
)

class ExportHandle internal constructor(private val cancelAction: () -> Unit) {
    fun cancel() = cancelAction()
}

internal fun AudioEffects.hasUnsupportedExportAutomation(): Boolean {
    return fadeInMs < 0L || fadeOutMs < 0L ||
        crossfadeMs != 0L ||
        eqPreset !in EXPORT_EQ_PRESETS ||
        reverbPreset !in EXPORT_REVERB_PRESETS ||
        delayTimeMs !in 0L..MAX_EXPORT_DELAY_MS ||
        (delayTimeMs == 0L && delayFeedback != 0f) ||
        !delayFeedback.isFinite() || delayFeedback !in 0f..MAX_EXPORT_DELAY_FEEDBACK ||
        !pitchSemitones.isFinite() || pitchSemitones !in -MAX_EXPORT_PITCH_SEMITONES..MAX_EXPORT_PITCH_SEMITONES ||
        noiseReductionIntensity != 0f ||
        isWindNoiseReduction ||
        voicePreset != "None" ||
        voiceEffectIntensity != 1f ||
        !distortion.isFinite() || distortion !in 0f..1f ||
        speed != 1f ||
        !reverbAmount.isFinite() || reverbAmount !in 0f..1f
}

internal val EXPORT_EQ_PRESETS = listOf("Flat", "Bass Boost", "Treble Boost", "Vocal")
internal val EXPORT_REVERB_PRESETS = listOf("None", "Room", "Hall")
internal const val MAX_EXPORT_DELAY_MS = 1_000L
internal const val MAX_EXPORT_DELAY_FEEDBACK = 0.95f
internal const val MAX_EXPORT_PITCH_SEMITONES = 12f
internal const val MAX_AUDIO_LOOP_EXPORT_SEGMENTS = 4_096L

internal data class AudioExportSegment(
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val timelineOffsetMs: Long
) {
    val durationMs: Long get() = (sourceEndMs - sourceStartMs).coerceAtLeast(0L)
}

private fun AudioClip.exportSourceWindow(): LongRange? {
    val safeSourceDurationMs = sourceDurationMs.coerceAtLeast(0L)
    if (safeSourceDurationMs <= 0L) return null
    val sourceStartMs = trimStartMs.coerceIn(0L, safeSourceDurationMs)
    val sourceEndMs = trimEndMs.coerceIn(sourceStartMs, safeSourceDurationMs)
    if (sourceEndMs <= sourceStartMs) return null
    return sourceStartMs until sourceEndMs
}

internal fun AudioClip.requiredExportSegmentCount(videoDurationMs: Long): Long {
    val window = exportSourceWindow() ?: return 0L
    val timelineStartMs = startTimeOnTimelineMs.coerceAtLeast(0L)
    val availableTimelineMs = (videoDurationMs - timelineStartMs).coerceAtLeast(0L)
    if (availableTimelineMs <= 0L) return 0L
    if (!isLooped) return 1L

    val cycleDurationMs = (window.last + 1L - window.first).coerceAtLeast(1L)
    val fullCycles = availableTimelineMs / cycleDurationMs
    return fullCycles + if (availableTimelineMs % cycleDurationMs == 0L) 0L else 1L
}

internal fun AudioClip.buildExportSegments(videoDurationMs: Long): List<AudioExportSegment> {
    val window = exportSourceWindow() ?: return emptyList()
    val sourceStartMs = window.first
    val sourceEndMs = window.last + 1L
    val cycleDurationMs = sourceEndMs - sourceStartMs
    val timelineStartMs = startTimeOnTimelineMs.coerceAtLeast(0L)
    var remainingMs = (videoDurationMs - timelineStartMs).coerceAtLeast(0L)
    if (remainingMs <= 0L) return emptyList()

    if (!isLooped) {
        val durationMs = minOf(cycleDurationMs, remainingMs)
        return listOf(AudioExportSegment(sourceStartMs, sourceStartMs + durationMs, 0L))
    }

    val requiredSegments = requiredExportSegmentCount(videoDurationMs)
    if (requiredSegments > MAX_AUDIO_LOOP_EXPORT_SEGMENTS) return emptyList()

    return buildList(requiredSegments.toInt()) {
        var timelineOffsetMs = 0L
        while (remainingMs > 0L) {
            val durationMs = minOf(cycleDurationMs, remainingMs)
            add(
                AudioExportSegment(
                    sourceStartMs = sourceStartMs,
                    sourceEndMs = sourceStartMs + durationMs,
                    timelineOffsetMs = timelineOffsetMs
                )
            )
            timelineOffsetMs += durationMs
            remainingMs -= durationMs
        }
    }
}

/**
 * Renders the base timeline into a real MP4. Trims, still-image durations, and
 * muted source audio are represented in the composition. Higher-level editor
 * layers are rejected by the screen until their renderer is wired in.
 */
class VideoExporter(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var transformer: Transformer? = null
    private var temporaryOutput: File? = null
    private var publishedAppFile: File? = null
    private var progressJob: Job? = null

    fun export(
        clips: List<MediaClip>,
        outputName: String = "Clipp_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.mp4",
        editorState: EditorState = EditorState(clips = clips),
        onProgress: (Int) -> Unit,
        onSuccess: (Uri, ExportMetadata) -> Unit,
        onError: (String) -> Unit
    ): ExportHandle {
        val canceled = AtomicBoolean(false)
        val outputFile = File(appContext.cacheDir, "export-${UUID.randomUUID()}.mp4")
        temporaryOutput = outputFile

        // Export through the canonical document boundary. The renderer still
        // consumes the legacy payloads today, but all timing/order/shared
        // properties are now normalized by one versioned layer model first.
        val canonicalState = editorState
            .copy(clips = clips)
            .toTimelineProject()
            .toEditorState()
        val unsupportedReasons = canonicalState.exportUnsupportedReasons()
        if (unsupportedReasons.isNotEmpty()) {
            onError("Export blocked: ${unsupportedReasons.joinToString(", ")}")
            return ExportHandle { canceled.set(true) }
        }
        val exportClips = canonicalState.clips
        val editedItems = mutableListOf<EditedMediaItem>()
        var clipStartMs = 0L
        exportClips.forEachIndexed { index, clip ->
            clip.toExportSpeedSegments().forEach { segment ->
                segment.toEditedMediaItem(
                    state = canonicalState,
                    clipIndex = index,
                    clipStartMs = clipStartMs
                )?.let {
                    editedItems += it
                    clipStartMs += segment.durationMs
                }
            }
        }
        if (editedItems.isEmpty()) {
            onError("There are no exportable clips in this project")
            return ExportHandle { canceled.set(true) }
        }

        val sequences = mutableListOf(EditedMediaItemSequence(editedItems))
        sequences += buildAudioSequences(canonicalState, exportClips, clipStartMs)
        val composition = Composition.Builder(sequences).build()

        val builtTransformer = Transformer.Builder(appContext)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    progressJob?.cancel()
                    if (canceled.get()) {
                        cleanupTemporaryOutput()
                        return
                    }
                    scope.launch {
                        val published = withContext(Dispatchers.IO) {
                            publishAndValidate(outputFile, outputName)
                        }
                        if (canceled.get()) {
                            published?.let { deletePublished(it) }
                            cleanupTemporaryOutput()
                        } else if (published == null) {
                            cleanupTemporaryOutput()
                            onError("Clipp could not publish a valid MP4")
                        } else {
                            cleanupTemporaryOutput()
                            onProgress(100)
                            onSuccess(
                                published.first,
                                ExportMetadata(
                                    durationMs = published.second,
                                    fileSizeBytes = published.third
                                )
                            )
                        }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    progressJob?.cancel()
                    cleanupTemporaryOutput()
                    if (BuildConfig.DEBUG) {
                        Log.e("ClippExporter", "Media3 export failed", exportException)
                    }
                    if (!canceled.get()) {
                        onError("Export failed: ${exportException.getErrorCodeName()}")
                    }
                }
            })
            .build()
        transformer = builtTransformer

        progressJob = scope.launch {
            val holder = ProgressHolder()
            while (isActive && !canceled.get()) {
                val state = builtTransformer.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress.coerceIn(0, 99))
                }
                delay(250)
            }
        }

        runCatching { builtTransformer.start(composition, outputFile.absolutePath) }
            .onFailure {
                progressJob?.cancel()
                cleanupTemporaryOutput()
                if (BuildConfig.DEBUG) Log.e("ClippExporter", "Could not start export", it)
                if (!canceled.get()) {
                    val detail = it.message?.takeIf(String::isNotBlank) ?: it::class.simpleName
                    onError("Could not start export${detail?.let { value -> ": $value" } ?: ""}")
                }
            }

        return ExportHandle {
            if (canceled.compareAndSet(false, true)) {
                progressJob?.cancel()
                runCatching { builtTransformer.cancel() }
                cleanupTemporaryOutput()
            }
        }
    }

    fun close() {
        progressJob?.cancel()
        runCatching { transformer?.cancel() }
        cleanupTemporaryOutput()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun MediaClip.toEditedMediaItem(
        state: EditorState,
        clipIndex: Int,
        clipStartMs: Long
    ): EditedMediaItem? {
        val normalized = normalized()
        if (normalized.sourceUri.isBlank()) return null
        normalized.safeOriginalDurationMs.takeIf { it > 0L } ?: return null
        val startMs = normalized.effectiveTrimStartMs
        val endMs = normalized.effectiveTrimEndMs
        if (endMs <= startMs) return null
        val safeSpeed = normalized.playbackSpeed
        val imageDurationMs = if (normalized.isPhoto) {
            ((endMs - startMs) / safeSpeed).toLong().coerceAtLeast(1L)
        } else {
            0L
        }
        val builder = MediaItem.Builder().setUri(Uri.parse(normalized.sourceUri))
        if (normalized.isPhoto) {
            builder.setImageDurationMs(imageDurationMs)
        } else {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
        }

        val videoEffects = buildExportVideoEffects(
            context = appContext,
            state = state,
            clip = normalized,
            clipStartMs = clipStartMs,
            clipDurationMs = normalized.durationMs
        ).toMutableList()
        if (!normalized.isPhoto && safeSpeed != 1f) {
            videoEffects += SpeedChangeEffect(safeSpeed)
        }
        val audioProcessors = mutableListOf<AudioProcessor>()
        val effectiveVolume = (normalized.volume * state.canvasSettings.masterVolume).coerceIn(0f, 1f)
        val clipVolumeKeyframes = normalized.keyframes["volume"].orEmpty().map { keyframe ->
            keyframe.copy(value = keyframe.value.coerceIn(0f, 1f) * state.canvasSettings.masterVolume)
        }
        val clipEffects = normalized.audioEffects
        val hasClipAudioAutomation = clipVolumeKeyframes.isNotEmpty() ||
            clipEffects.fadeInMs > 0L || clipEffects.fadeOutMs > 0L
        if (!normalized.isPhoto && !normalized.isMuted && hasClipAudioAutomation) {
            audioProcessors += KeyframedVolumeAudioProcessor(
                baseGain = effectiveVolume,
                durationMs = normalized.durationMs,
                fadeInMs = clipEffects.fadeInMs,
                fadeOutMs = clipEffects.fadeOutMs,
                volumeKeyframes = clipVolumeKeyframes
            )
        } else if (!normalized.isPhoto && !normalized.isMuted && effectiveVolume != 1f) {
            audioProcessors += VolumeAudioProcessor(effectiveVolume)
        }
        if (!normalized.isPhoto && !normalized.isMuted) {
            audioProcessors.addAdvancedAudioProcessors(
                audioEffects = clipEffects,
                speed = safeSpeed,
                pitch = if (normalized.maintainPitch) 1f else safeSpeed
            )
        }

        val editedItem = EditedMediaItem.Builder(builder.build())
            .setRemoveAudio(normalized.isPhoto || normalized.isMuted)
        if (normalized.isPhoto) {
            editedItem
                .setDurationUs(imageDurationMs * 1_000L)
                .setFrameRate(30)
        }
        if (videoEffects.isNotEmpty() || audioProcessors.isNotEmpty()) {
            editedItem.setEffects(Effects(audioProcessors, videoEffects))
        }
        return editedItem.build()
    }

    private fun buildAudioSequences(
        state: EditorState,
        clips: List<MediaClip>,
        videoDurationMs: Long
    ): List<EditedMediaItemSequence> {
        return state.audioClips.mapNotNull { audioClip ->
            if (videoDurationMs <= 0L) return@mapNotNull null
            val sourceUri = audioClip.sourceUri
                ?: audioClip.sourceClipId?.let { id -> clips.find { it.id == id }?.sourceUri }
                ?: return@mapNotNull null
            val timelineStartMs = audioClip.startTimeOnTimelineMs.coerceIn(0L, (videoDurationMs - 1L).coerceAtLeast(0L))
            val segments = audioClip.buildExportSegments(videoDurationMs)
            if (segments.isEmpty()) return@mapNotNull null
            val sequenceBuilder = EditedMediaItemSequence.Builder()
            val startTimeUs = timelineStartMs * 1_000L
            if (startTimeUs > 0L) sequenceBuilder.addGap(startTimeUs)
            segments.forEach { segment ->
                sequenceBuilder.addItem(
                    buildAudioEditedItem(
                        audioClip = audioClip,
                        sourceUri = sourceUri,
                        segment = segment,
                        masterVolume = state.canvasSettings.masterVolume
                    )
                )
            }
            sequenceBuilder.build()
        }
    }

    private fun buildAudioEditedItem(
        audioClip: AudioClip,
        sourceUri: String,
        segment: AudioExportSegment,
        masterVolume: Float
    ): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(sourceUri))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(segment.sourceStartMs)
                    .setEndPositionMs(segment.sourceEndMs)
                    .build()
            )
            .build()
        val processors = mutableListOf<AudioProcessor>()
        val effectiveVolume = (audioClip.volume * masterVolume).coerceIn(0f, 1f)
        val audioEffects = audioClip.audioEffects
        val volumeKeyframes = audioClip.keyframes["volume"].orEmpty()
            .filter { it.timeMs in segment.sourceStartMs..segment.sourceEndMs }
            .map { keyframe ->
                keyframe.copy(
                    timeMs = (keyframe.timeMs - segment.sourceStartMs).coerceIn(0L, segment.durationMs),
                    value = keyframe.value.coerceIn(0f, 1f) * masterVolume
                )
            }
        val hasAudioAutomation = volumeKeyframes.isNotEmpty() ||
            audioEffects.fadeInMs > 0L || audioEffects.fadeOutMs > 0L
        if (!audioClip.isMuted && hasAudioAutomation) {
            processors += KeyframedVolumeAudioProcessor(
                baseGain = effectiveVolume,
                durationMs = segment.durationMs,
                fadeInMs = audioEffects.fadeInMs,
                fadeOutMs = audioEffects.fadeOutMs,
                volumeKeyframes = volumeKeyframes
            )
        } else if (!audioClip.isMuted && effectiveVolume != 1f) {
            processors += VolumeAudioProcessor(effectiveVolume)
        }
        if (!audioClip.isMuted) {
            processors.addAdvancedAudioProcessors(audioEffects)
        }
        return EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .setRemoveAudio(audioClip.isMuted)
            .setEffects(Effects(processors, emptyList()))
            .build()
    }

    private fun publishAndValidate(
        temporaryFile: File,
        requestedName: String
    ): Triple<Uri, Long, Long>? {
        if (!temporaryFile.exists() || temporaryFile.length() <= 0L) return null
        val safeName = requestedName
            .substringAfterLast(File.separatorChar)
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .let { if (it.endsWith(".mp4", ignoreCase = true)) it else "$it.mp4" }
        val published = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishToMediaStore(temporaryFile, safeName)
        } else {
            publishToAppExternalFiles(temporaryFile, safeName)
        } ?: return null

        val durationMs = readVideoDuration(published.first)
        if (durationMs <= 0L) {
            deletePublished(published)
            return null
        }
        return Triple(published.first, durationMs, published.second)
    }

    private fun publishToMediaStore(file: File, name: String): Pair<Uri, Long>? {
        publishedAppFile = null
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, MimeTypes.VIDEO_MP4)
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Clipp")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = appContext.contentResolver.insert(collection, values) ?: return null
        return runCatching {
            appContext.contentResolver.openOutputStream(uri)?.use { destination ->
                file.inputStream().use { source -> source.copyTo(destination) }
            } ?: error("Could not open MediaStore output")
            val ready = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            appContext.contentResolver.update(uri, ready, null, null)
            uri to file.length()
        }.getOrElse {
            appContext.contentResolver.delete(uri, null, null)
            null
        }
    }

    private fun publishToAppExternalFiles(file: File, name: String): Pair<Uri, Long>? {
        val directory = File(
            appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "Clipp"
        )
        if (!directory.exists() && !directory.mkdirs()) return null
        val destination = File(directory, name)
        return runCatching {
            file.copyTo(destination, overwrite = true)
            publishedAppFile = destination
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                destination
            ) to destination.length()
        }.getOrElse {
            destination.delete()
            null
        }
    }

    private fun readVideoDuration(uri: Uri): Long {
        val retriever = android.media.MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(appContext, uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        }.getOrDefault(0L).also { runCatching { retriever.release() } }
    }

    private fun deletePublished(published: Triple<Uri, Long, Long>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appContext.contentResolver.delete(published.first, null, null)
        } else {
            publishedAppFile?.delete()
            publishedAppFile = null
        }
    }

    private fun deletePublished(published: Pair<Uri, Long>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appContext.contentResolver.delete(published.first, null, null)
        } else {
            publishedAppFile?.delete()
            publishedAppFile = null
        }
    }

    private fun cleanupTemporaryOutput() {
        temporaryOutput?.delete()
        temporaryOutput = null
    }
}

/**
 * Media3 exposes a constant-speed effect, so variable curves are rendered as
 * bounded source windows. Each window uses the curve's average speed for that
 * interval; the visual/audio edits are remapped to the window's local clock.
 */
private fun MediaClip.toExportSpeedSegments(): List<MediaClip> {
    val normalized = normalized()
    val curve = normalized.speedCurve ?: return listOf(normalized)
    val sourceDurationMs = normalized.effectiveTrimEndMs - normalized.effectiveTrimStartMs
    if (sourceDurationMs <= 0L) return listOf(normalized.copy(speedCurve = null))

    val timeline = SpeedCurveTimeline(sourceDurationMs, curve)
    if (curve.isEffectivelyConstant()) {
        return listOf(
            normalized.copy(
                playbackSpeed = curve.getSpeedAt(0.5f),
                speedCurve = null
            )
        )
    }

    val windows = timeline.sampledWindows()
    if (windows.isEmpty()) return listOf(normalized.copy(speedCurve = null))

    return windows.mapIndexedNotNull { index, window ->
        val sourceDeltaMs = window.sourceEndMs - window.sourceStartMs
        if (sourceDeltaMs <= 0L) return@mapIndexedNotNull null
        val speed = (sourceDeltaMs.toDouble() / window.playbackDurationMs.toDouble())
            .toFloat()
            .coerceIn(0.1f, 10f)
        val segmentStartMs = window.playbackStartMs
        val segmentEndMs = window.playbackEndMs.coerceAtLeast(segmentStartMs + 1L)
        val segmentDurationMs = (segmentEndMs - segmentStartMs).coerceAtLeast(1L)
        val startSourceMs = normalized.effectiveTrimStartMs + window.sourceStartMs
        val endSourceMs = normalized.effectiveTrimStartMs + window.sourceEndMs

        normalized.copy(
            trimStartMs = startSourceMs,
            trimEndMs = endSourceMs,
            playbackSpeed = speed,
            speedCurve = null,
            effects = normalized.effects.mapNotNull {
                it.remappedForSpeedSegment(
                    segmentStartMs,
                    segmentEndMs,
                    segmentDurationMs
                )
            },
            keyframes = normalized.keyframes.remappedForSpeedSegment(
                clip = normalized,
                segmentStartMs = segmentStartMs,
                segmentEndMs = segmentEndMs,
                segmentDurationMs = segmentDurationMs
            ),
            audioEffects = normalized.audioEffects.copy(fadeInMs = 0L, fadeOutMs = 0L),
            transitionNext = if (index == windows.lastIndex) normalized.transitionNext else Transition()
        )
    }
}

private fun AppliedEffect.remappedForSpeedSegment(
    segmentStartMs: Long,
    segmentEndMs: Long,
    segmentDurationMs: Long
): AppliedEffect? {
    val effectEndMs = if (endTimeMs == -1L) segmentEndMs else endTimeMs
    val overlapStartMs = maxOf(startTimeMs, segmentStartMs)
    val overlapEndMs = minOf(effectEndMs, segmentEndMs)
    if (overlapEndMs < overlapStartMs) return null
    val sourceSpanMs = (segmentEndMs - segmentStartMs).coerceAtLeast(1L)
    fun localTime(timeMs: Long): Long = (
        (timeMs - segmentStartMs).toDouble() / sourceSpanMs.toDouble() * segmentDurationMs
        ).roundToLong().coerceIn(0L, segmentDurationMs)

    return copy(
        startTimeMs = localTime(overlapStartMs),
        endTimeMs = if (endTimeMs == -1L) -1L else localTime(overlapEndMs)
    )
}

private fun Map<String, List<Keyframe>>.remappedForSpeedSegment(
    clip: MediaClip,
    segmentStartMs: Long,
    segmentEndMs: Long,
    segmentDurationMs: Long
): Map<String, List<Keyframe>> {
    val remapped = mapValues { (property, values) ->
        if (values.isEmpty()) {
            values
        } else {
            remapKeyframeList(
                property = property,
                values = values,
                clip = clip,
                segmentStartMs = segmentStartMs,
                segmentEndMs = segmentEndMs,
                segmentDurationMs = segmentDurationMs
            )
        }
    }.toMutableMap()

    val audioEffects = clip.audioEffects
    val volumeKeyframes = this["volume"].orEmpty()
    if (volumeKeyframes.isNotEmpty() || audioEffects.fadeInMs > 0L || audioEffects.fadeOutMs > 0L) {
        remapped["volume"] = remapVolumeKeyframesForSpeedSegment(
            clip = clip,
            segmentStartMs = segmentStartMs,
            segmentEndMs = segmentEndMs,
            segmentDurationMs = segmentDurationMs
        )
    }
    return remapped
}

private fun Map<String, List<Keyframe>>.remapKeyframeList(
    property: String,
    values: List<Keyframe>,
    clip: MediaClip,
    segmentStartMs: Long,
    segmentEndMs: Long,
    segmentDurationMs: Long
): List<Keyframe> {
    val segmentSourceSpanMs = (segmentEndMs - segmentStartMs).coerceAtLeast(1L)
    val sourceTimes = buildList {
        add(segmentStartMs)
        add(segmentEndMs)
        values.filter { it.timeMs in segmentStartMs..segmentEndMs }
            .forEach { add(it.timeMs) }
    }.distinct().sorted()

    return sourceTimes.map { sourceTimeMs ->
        val localTimeMs = (
            (sourceTimeMs - segmentStartMs).toDouble() / segmentSourceSpanMs.toDouble() * segmentDurationMs
            ).roundToLong().coerceIn(0L, segmentDurationMs)
        Keyframe(
            id = "speed-segment-$property-$segmentStartMs-$localTimeMs",
            timeMs = localTimeMs,
            value = getValueAtTime(property, sourceTimeMs, clip.defaultKeyframeValue(property)),
            easing = values.lastOrNull { it.timeMs <= sourceTimeMs }?.easing
                ?: values.first().easing
        )
    }.distinctBy { it.timeMs }
}

private fun Map<String, List<Keyframe>>.remapVolumeKeyframesForSpeedSegment(
    clip: MediaClip,
    segmentStartMs: Long,
    segmentEndMs: Long,
    segmentDurationMs: Long
): List<Keyframe> {
    val values = this["volume"].orEmpty()
    val segmentSourceSpanMs = (segmentEndMs - segmentStartMs).coerceAtLeast(1L)
    val sourceTimes = buildList {
        add(segmentStartMs)
        add(segmentEndMs)
        values.filter { it.timeMs in segmentStartMs..segmentEndMs }
            .forEach { add(it.timeMs) }
    }.distinct().sorted()
    val originalDurationMs = clip.durationMs.coerceAtLeast(1L)

    fun fadeGainAt(timeMs: Long): Float {
        val fadeInGain = if (clip.audioEffects.fadeInMs > 0L) {
            (timeMs.toFloat() / clip.audioEffects.fadeInMs).coerceIn(0f, 1f)
        } else {
            1f
        }
        val fadeOutGain = if (clip.audioEffects.fadeOutMs > 0L) {
            ((originalDurationMs - timeMs).toFloat() / clip.audioEffects.fadeOutMs).coerceIn(0f, 1f)
        } else {
            1f
        }
        return fadeInGain * fadeOutGain
    }

    return sourceTimes.map { sourceTimeMs ->
        val localTimeMs = (
            (sourceTimeMs - segmentStartMs).toDouble() / segmentSourceSpanMs.toDouble() * segmentDurationMs
            ).roundToLong().coerceIn(0L, segmentDurationMs)
        Keyframe(
            id = "speed-segment-volume-$segmentStartMs-$localTimeMs",
            timeMs = localTimeMs,
            value = (
                getValueAtTime("volume", sourceTimeMs, clip.volume) * fadeGainAt(sourceTimeMs)
                ).coerceIn(0f, 1f),
            easing = values.lastOrNull { it.timeMs <= sourceTimeMs }?.easing
                ?: values.firstOrNull()?.easing
                ?: EasingType.LINEAR
        )
    }.distinctBy { it.timeMs }
}

private fun MediaClip.defaultKeyframeValue(property: String): Float = when (property) {
    "posX" -> posX
    "posY" -> posY
    "scale" -> scale
    "rotation" -> rotation
    "cropLeft" -> cropRect.left
    "cropTop" -> cropRect.top
    "cropRight" -> cropRect.right
    "cropBottom" -> cropRect.bottom
    "volume" -> volume
    else -> 0f
}

fun MediaClip.hasUnsupportedExportEdits(): Boolean {
    return !playbackSpeed.isFinite() || playbackSpeed !in 0.1f..10f ||
        !volume.isFinite() || volume !in 0f..1f ||
        cropRect.left !in 0f..1f || cropRect.top !in 0f..1f ||
        cropRect.right !in 0f..1f || cropRect.bottom !in 0f..1f ||
        cropRect.right <= cropRect.left || cropRect.bottom <= cropRect.top ||
        scale <= 0f ||
        photoAnimationSettings.type != PhotoAnimationType.NONE ||
        speedCurve?.isExportSafe() == false
}

private fun MutableList<AudioProcessor>.addAdvancedAudioProcessors(
    audioEffects: AudioEffects,
    speed: Float = 1f,
    pitch: Float = 1f
) {
    val requestedPitch = (pitch * 2.0.pow(audioEffects.pitchSemitones.toDouble() / 12.0)).toFloat()
    if (speed != 1f || requestedPitch != 1f) {
        add(SonicAudioProcessor().apply {
            setSpeed(speed)
            setPitch(requestedPitch)
        })
    }
    if (audioEffects.eqPreset != "Flat") {
        add(ParametricEqAudioProcessor(audioEffects.eqPreset))
    }
    if (audioEffects.delayTimeMs > 0L) {
        add(FeedbackDelayAudioProcessor(audioEffects.delayTimeMs, audioEffects.delayFeedback))
    }
    if (audioEffects.reverbPreset != "None" || audioEffects.reverbAmount > 0f) {
        add(MultiTapReverbAudioProcessor(audioEffects.reverbPreset, audioEffects.reverbAmount))
    }
    if (audioEffects.distortion > 0f) {
        add(DistortionAudioProcessor(audioEffects.distortion))
    }
}

private fun requirePcmFormat(inputAudioFormat: AudioProcessor.AudioFormat) {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
        inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
    ) {
        throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
    }
}

private fun processPcmFrames(
    inputBuffer: ByteBuffer,
    inputAudioFormat: AudioProcessor.AudioFormat,
    channelCount: Int,
    outputBuffer: (Int) -> ByteBuffer,
    transform: (Float, Int) -> Float,
    afterFrame: () -> Unit = {}
) {
    val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val bytesPerSample = if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) 2 else 4
    val bytesPerFrame = (channelCount * bytesPerSample).coerceAtLeast(1)
    val completeBytes = input.remaining() - input.remaining() % bytesPerFrame
    val output = outputBuffer(completeBytes).order(ByteOrder.LITTLE_ENDIAN)
    repeat(completeBytes / bytesPerFrame) {
        repeat(channelCount) { channel ->
            val sample = if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
                input.short / 32768f
            } else {
                input.float
            }
            val processed = transform(sample, channel).coerceIn(-1f, 1f)
            if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
                output.putShort((processed * 32767f).roundToInt().coerceIn(-32768, 32767).toShort())
            } else {
                output.putFloat(processed)
            }
        }
        afterFrame()
    }
    output.flip()
}

/** A small deterministic tone-shaping EQ that works on the PCM formats Media3 exports. */
internal class ParametricEqAudioProcessor(
    private val preset: String
) : BaseAudioProcessor() {
    private var sampleRate = 0
    private var channelCount = 0
    private var lowState = FloatArray(0)
    private var midState = FloatArray(0)
    private var highState = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        requirePcmFormat(inputAudioFormat)
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(1)
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        lowState = FloatArray(channelCount)
        midState = FloatArray(channelCount)
        highState = FloatArray(channelCount)
        return inputAudioFormat
    }

    override fun onFlush() {
        lowState.fill(0f)
        midState.fill(0f)
        highState.fill(0f)
    }

    override fun isActive(): Boolean = super.isActive() || preset != "Flat"

    override fun queueInput(inputBuffer: ByteBuffer) {
        val lowAlpha = (2f * PI.toFloat() * 180f / sampleRate).coerceIn(0.001f, 1f)
        val midAlpha = (2f * PI.toFloat() * 2_800f / sampleRate).coerceIn(0.001f, 1f)
        processPcmFrames(inputBuffer, inputAudioFormat, channelCount, outputBuffer = { size -> replaceOutputBuffer(size) }, transform = { sample, channel ->
            lowState[channel] += lowAlpha * (sample - lowState[channel])
            midState[channel] += midAlpha * (sample - midState[channel])
            highState[channel] = midState[channel]
            val bass = lowState[channel]
            val treble = sample - highState[channel]
            val vocal = midState[channel] - bass
            when (preset) {
                "Bass Boost" -> sample + bass * 0.72f
                "Treble Boost" -> sample + treble * 0.62f
                "Vocal" -> sample + vocal * 0.55f
                else -> sample
            }
        })
    }
}

/** Adds a bounded echo while keeping the edited item's duration unchanged. */
internal class FeedbackDelayAudioProcessor(
    delayMs: Long,
    feedback: Float
) : BaseAudioProcessor() {
    private val delayMs = delayMs.coerceIn(1L, MAX_EXPORT_DELAY_MS)
    private val feedback = feedback.coerceIn(0f, MAX_EXPORT_DELAY_FEEDBACK)
    private var channelCount = 0
    private var delayFrames = 1
    private var writeFrame = 0
    private var delayBuffer = Array(0) { FloatArray(0) }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        requirePcmFormat(inputAudioFormat)
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        delayFrames = (inputAudioFormat.sampleRate.coerceAtLeast(1) * delayMs / 1_000L)
            .toInt()
            .coerceAtLeast(1)
        delayBuffer = Array(channelCount) { FloatArray(delayFrames) }
        return inputAudioFormat
    }

    override fun onFlush() {
        delayBuffer.forEach { it.fill(0f) }
        writeFrame = 0
    }

    override fun isActive(): Boolean = super.isActive() || delayMs > 0L

    override fun queueInput(inputBuffer: ByteBuffer) {
        processPcmFrames(inputBuffer, inputAudioFormat, channelCount, outputBuffer = { size -> replaceOutputBuffer(size) }, transform = { sample, channel ->
            val delayed = delayBuffer[channel][writeFrame]
            delayBuffer[channel][writeFrame] = (sample + delayed * feedback).coerceIn(-1f, 1f)
            sample + delayed * 0.65f
        }, afterFrame = {
            writeFrame = (writeFrame + 1) % delayFrames
        })
    }
}

/** A short multi-tap room/hall reverb with no unbounded tail allocation. */
internal class MultiTapReverbAudioProcessor(
    private val preset: String,
    amount: Float
) : BaseAudioProcessor() {
    private val amount = amount.coerceIn(0f, 1f).let { if (it > 0f) it else if (preset == "Hall") 0.5f else 0.34f }
    private val tapDelaysMs = if (preset == "Hall") {
        intArrayOf(73, 109, 151, 223)
    } else {
        intArrayOf(31, 47, 71)
    }
    private val tapWeights = if (preset == "Hall") {
        floatArrayOf(0.34f, 0.25f, 0.18f, 0.12f)
    } else {
        floatArrayOf(0.42f, 0.3f, 0.2f)
    }
    private var sampleRate = 0
    private var channelCount = 0
    private var tapPositions = IntArray(0)
    private var tapBuffers = emptyArray<Array<FloatArray>>()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        requirePcmFormat(inputAudioFormat)
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(1)
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        tapPositions = IntArray(tapDelaysMs.size)
        tapBuffers = tapDelaysMs.map { delayMs ->
            val frames = (sampleRate * delayMs / 1_000L).toInt().coerceAtLeast(1)
            Array(channelCount) { FloatArray(frames) }
        }.toTypedArray()
        return inputAudioFormat
    }

    override fun onFlush() {
        tapPositions.fill(0)
        tapBuffers.forEach { tap -> tap.forEach { it.fill(0f) } }
    }

    override fun isActive(): Boolean = super.isActive() || amount > 0f

    override fun queueInput(inputBuffer: ByteBuffer) {
        processPcmFrames(inputBuffer, inputAudioFormat, channelCount, outputBuffer = { size -> replaceOutputBuffer(size) }, transform = { sample, channel ->
            var wet = 0f
            tapBuffers.indices.forEach { tapIndex ->
                wet += tapBuffers[tapIndex][channel][tapPositions[tapIndex]] * tapWeights[tapIndex]
            }
            tapBuffers.indices.forEach { tapIndex ->
                val position = tapPositions[tapIndex]
                tapBuffers[tapIndex][channel][position] =
                    (sample + wet * 0.18f).coerceIn(-1f, 1f)
            }
            sample + wet * amount
        }, afterFrame = {
            tapBuffers.indices.forEach { tapIndex ->
                val position = tapPositions[tapIndex]
                tapPositions[tapIndex] = (position + 1) % tapBuffers[tapIndex][0].size
            }
        })
    }
}

/** A bounded waveshaper for deliberate lo-fi/distortion processing. */
internal class DistortionAudioProcessor(amount: Float) : BaseAudioProcessor() {
    private val amount = amount.coerceIn(0f, 1f)
    private val drive = 1f + amount * 20f
    private val normalization = 1f / tanh(drive)
    private var channelCount = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        requirePcmFormat(inputAudioFormat)
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        return inputAudioFormat
    }

    override fun isActive(): Boolean = super.isActive() || amount > 0f

    override fun queueInput(inputBuffer: ByteBuffer) {
        processPcmFrames(
            inputBuffer,
            inputAudioFormat,
            channelCount,
            outputBuffer = { size -> replaceOutputBuffer(size) },
            transform = { sample, _ ->
            val shaped = tanh(sample * drive) * normalization
            val mix = amount * 0.65f
            sample * (1f - mix) + shaped * mix
            }
        )
    }
}

/** Applies a clip's simple linear volume change to decoded PCM audio. */
internal class VolumeAudioProcessor(volume: Float) : BaseAudioProcessor() {
    private val gain = volume.coerceIn(0f, 1f)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun isActive(): Boolean = gain != 1f

    override fun queueInput(inputBuffer: ByteBuffer) {
        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = replaceOutputBuffer(input.remaining()).order(ByteOrder.LITTLE_ENDIAN)
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> {
                while (input.remaining() >= 2) {
                    val sample = input.short.toInt()
                    output.putShort((sample * gain).roundToInt().coerceIn(-32768, 32767).toShort())
                }
            }
            C.ENCODING_PCM_FLOAT -> {
                while (input.remaining() >= 4) {
                    output.putFloat((input.float * gain).coerceIn(-1f, 1f))
                }
            }
        }
        output.flip()
    }
}

/** Applies absolute volume keyframes and optional fade envelopes to decoded PCM audio. */
internal class KeyframedVolumeAudioProcessor(
    private val baseGain: Float,
    private val durationMs: Long,
    fadeInMs: Long,
    fadeOutMs: Long,
    volumeKeyframes: List<Keyframe>
) : BaseAudioProcessor() {
    private val fadeInDurationMs = fadeInMs.coerceAtLeast(0L)
    private val fadeOutDurationMs = fadeOutMs.coerceAtLeast(0L)
    private val sortedVolumeKeyframes = volumeKeyframes.sortedBy { it.timeMs }
    private var sampleRate = 0
    private var channelCount = 0
    private var bytesPerSample = 0
    private var processedFrames = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        bytesPerSample = if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) 2 else 4
        return inputAudioFormat
    }

    override fun onFlush() {
        processedFrames = 0L
    }

    override fun isActive(): Boolean = baseGain != 1f ||
        fadeInDurationMs > 0L ||
        fadeOutDurationMs > 0L ||
        sortedVolumeKeyframes.isNotEmpty()

    override fun queueInput(inputBuffer: ByteBuffer) {
        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val bytesPerFrame = (channelCount * bytesPerSample).coerceAtLeast(1)
        val completeBytes = input.remaining() - input.remaining() % bytesPerFrame
        val output = replaceOutputBuffer(completeBytes).order(ByteOrder.LITTLE_ENDIAN)
        val frameCount = completeBytes / bytesPerFrame
        repeat(frameCount) {
            val frameTimeMs = if (sampleRate > 0) {
                ((processedFrames + it) * 1_000L / sampleRate).coerceAtLeast(0L)
            } else {
                0L
            }
            val gain = gainAtTime(frameTimeMs)
            repeat(channelCount) {
                if (bytesPerSample == 2) {
                    val sample = input.short.toInt()
                    output.putShort((sample * gain).roundToInt().coerceIn(-32768, 32767).toShort())
                } else {
                    output.putFloat((input.float * gain).coerceIn(-1f, 1f))
                }
            }
        }
        processedFrames += frameCount
        output.flip()
    }

    private fun gainAtTime(timeMs: Long): Float {
        val keyframedGain = if (sortedVolumeKeyframes.isEmpty()) {
            baseGain
        } else if (timeMs <= sortedVolumeKeyframes.first().timeMs) {
            sortedVolumeKeyframes.first().value
        } else if (timeMs >= sortedVolumeKeyframes.last().timeMs) {
            sortedVolumeKeyframes.last().value
        } else {
            val index = sortedVolumeKeyframes.indexOfLast { it.timeMs <= timeMs }
            val first = sortedVolumeKeyframes[index]
            val second = sortedVolumeKeyframes[index + 1]
            val duration = second.timeMs - first.timeMs
            val progress = if (duration <= 0L) 0f else {
                applyEasing((timeMs - first.timeMs).toFloat() / duration, first.easing)
            }
            first.value + (second.value - first.value) * progress
        }
        val fadeInGain = if (fadeInDurationMs > 0L) {
            (timeMs.toFloat() / fadeInDurationMs).coerceIn(0f, 1f)
        } else {
            1f
        }
        val fadeOutGain = if (fadeOutDurationMs > 0L && durationMs > 0L) {
            ((durationMs - timeMs).toFloat() / fadeOutDurationMs).coerceIn(0f, 1f)
        } else {
            1f
        }
        return (keyframedGain * fadeInGain * fadeOutGain).coerceIn(0f, 1f)
    }
}
