package com.example

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
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
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class ExportMetadata(
    val durationMs: Long,
    val fileSizeBytes: Long
)

class ExportHandle internal constructor(private val cancelAction: () -> Unit) {
    fun cancel() = cancelAction()
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
        onProgress: (Int) -> Unit,
        onSuccess: (Uri, ExportMetadata) -> Unit,
        onError: (String) -> Unit
    ): ExportHandle {
        val canceled = AtomicBoolean(false)
        val outputFile = File(appContext.cacheDir, "export-${UUID.randomUUID()}.mp4")
        temporaryOutput = outputFile

        val editedItems = clips.mapNotNull { clip -> clip.toEditedMediaItem() }
        if (editedItems.isEmpty()) {
            onError("There are no exportable clips in this project")
            return ExportHandle { canceled.set(true) }
        }

        val composition = Composition.Builder(
            EditedMediaItemSequence(editedItems)
        ).build()

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
                if (!canceled.get()) onError("Could not start export: ${it.message ?: "unknown error"}")
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

    private fun MediaClip.toEditedMediaItem(): EditedMediaItem? {
        if (sourceUri.isBlank()) return null
        val sourceDurationMs = safeOriginalDurationMs.takeIf { it > 0L } ?: return null
        val startMs = effectiveTrimStartMs
        val endMs = effectiveTrimEndMs
        if (endMs <= startMs) return null
        val builder = MediaItem.Builder().setUri(Uri.parse(sourceUri))
        if (isPhoto) {
            builder.setImageDurationMs((endMs - startMs).coerceAtLeast(1L))
        } else {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
        }
        return EditedMediaItem.Builder(builder.build())
            .setRemoveAudio(isPhoto || isMuted)
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

fun MediaClip.hasUnsupportedExportEdits(): Boolean {
    val defaultCrop = androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)
    return playbackSpeed != 1f ||
        !maintainPitch ||
        rotation != 0f ||
        flipHorizontal ||
        flipVertical ||
        cropRect != defaultCrop ||
        scale != 1f ||
        posX != 0.5f ||
        posY != 0.5f ||
        transitionNext.type != TransitionType.NONE ||
        filterType != FilterType.NONE ||
        adjustments != ColorAdjustments() ||
        effects.isNotEmpty() ||
        photoAnimationSettings.type != PhotoAnimationType.NONE ||
        keyframes.isNotEmpty() ||
        speedCurve != null ||
        volume != 1f ||
        audioEffects != AudioEffects()
}
