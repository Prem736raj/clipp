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
import kotlin.math.roundToInt

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
        eqPreset != "Flat" ||
        reverbPreset != "None" ||
        delayTimeMs != 0L ||
        delayFeedback != 0f ||
        pitchSemitones != 0f ||
        noiseReductionIntensity != 0f ||
        isWindNoiseReduction ||
        voicePreset != "None" ||
        voiceEffectIntensity != 1f ||
        distortion != 0f ||
        speed != 1f ||
        reverbAmount != 0f
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

        val editedItems = mutableListOf<EditedMediaItem>()
        var clipStartMs = 0L
        clips.forEachIndexed { index, clip ->
            clip.toEditedMediaItem(
                state = editorState,
                clipIndex = index,
                clipStartMs = clipStartMs
            )?.let { editedItems += it }
            clipStartMs += clip.durationMs
        }
        if (editedItems.isEmpty()) {
            onError("There are no exportable clips in this project")
            return ExportHandle { canceled.set(true) }
        }

        val sequences = mutableListOf(EditedMediaItemSequence(editedItems))
        sequences += buildAudioSequences(editorState, clips, clipStartMs)
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
        if (!normalized.isPhoto && !normalized.isMuted && safeSpeed != 1f) {
            audioProcessors += SonicAudioProcessor().apply {
                setSpeed(safeSpeed)
                setPitch(if (normalized.maintainPitch) 1f else safeSpeed)
            }
        }
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
            val sourceStart = audioClip.trimStartMs.coerceAtLeast(0L)
            val timelineStartMs = audioClip.startTimeOnTimelineMs.coerceIn(0L, (videoDurationMs - 1L).coerceAtLeast(0L))
            val availableTimelineMs = (videoDurationMs - timelineStartMs).coerceAtLeast(1L)
            val safeSourceDurationMs = audioClip.sourceDurationMs.coerceAtLeast(sourceStart + 1L)
            val sourceEnd = audioClip.trimEndMs
                .coerceAtLeast(sourceStart + 1L)
                .coerceAtMost(safeSourceDurationMs)
                .coerceAtMost(sourceStart + availableTimelineMs)
            val durationMs = (sourceEnd - sourceStart).coerceAtLeast(1L)
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(sourceUri))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(sourceStart)
                        .setEndPositionMs(sourceEnd)
                        .build()
                )
                .build()
            val processors = mutableListOf<AudioProcessor>()
            val effectiveVolume = (audioClip.volume * state.canvasSettings.masterVolume).coerceIn(0f, 1f)
            val audioEffects = audioClip.audioEffects
            val volumeKeyframes = audioClip.keyframes["volume"].orEmpty().map { keyframe ->
                keyframe.copy(
                    timeMs = (keyframe.timeMs - sourceStart).coerceAtLeast(0L),
                    value = keyframe.value.coerceIn(0f, 1f) * state.canvasSettings.masterVolume
                )
            }
            val hasAudioAutomation = volumeKeyframes.isNotEmpty() ||
                audioEffects.fadeInMs > 0L || audioEffects.fadeOutMs > 0L
            if (!audioClip.isMuted && hasAudioAutomation) {
                processors += KeyframedVolumeAudioProcessor(
                    baseGain = effectiveVolume,
                    durationMs = durationMs,
                    fadeInMs = audioEffects.fadeInMs,
                    fadeOutMs = audioEffects.fadeOutMs,
                    volumeKeyframes = volumeKeyframes
                )
            } else if (!audioClip.isMuted && effectiveVolume != 1f) {
                processors += VolumeAudioProcessor(effectiveVolume)
            }
            val item = EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true)
                .setRemoveAudio(audioClip.isMuted)
                .setEffects(Effects(processors, emptyList()))
                .build()
            val sequenceBuilder = EditedMediaItemSequence.Builder()
            val startTimeUs = timelineStartMs * 1_000L
            if (startTimeUs > 0L) sequenceBuilder.addGap(startTimeUs)
            sequenceBuilder.addItem(item).build()
        }
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

fun MediaClip.hasUnsupportedExportEdits(): Boolean {
    return !playbackSpeed.isFinite() || playbackSpeed !in 0.1f..10f ||
        !volume.isFinite() || volume !in 0f..1f ||
        cropRect.left !in 0f..1f || cropRect.top !in 0f..1f ||
        cropRect.right !in 0f..1f || cropRect.bottom !in 0f..1f ||
        cropRect.right <= cropRect.left || cropRect.bottom <= cropRect.top ||
        scale <= 0f ||
        photoAnimationSettings.type != PhotoAnimationType.NONE ||
        speedCurve != null
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
