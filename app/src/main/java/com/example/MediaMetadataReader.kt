package com.example

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class MediaMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val mimeType: String,
    val hasAudio: Boolean
)

/**
 * Reads metadata from the URI granted by the system picker. A missing stream,
 * empty file, unsupported MIME type, or corrupt metadata returns null so the
 * caller can show a real recovery action.
 */
object MediaMetadataReader {
    suspend fun read(context: Context, uri: Uri): MediaMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            if (uri.scheme != "file") {
                resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    if (descriptor.length == 0L) return@withContext null
                }
            }

            val mimeType = resolver.getType(uri) ?: inferMimeType(uri)
            when {
                mimeType.startsWith("video/") -> readVideo(context, uri, mimeType)
                mimeType.startsWith("image/") -> readImage(context, uri, mimeType)
                else -> null
            }
        }.getOrNull()
    }

    suspend fun readAudioDuration(context: Context, uri: Uri): Long? = withContext(Dispatchers.IO) {
        runCatching {
            if (uri.scheme != "file") {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    if (descriptor.length == 0L) return@withContext null
                }
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSourceCompat(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private fun readVideo(context: Context, uri: Uri, mimeType: String): MediaMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSourceCompat(context, uri)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val width = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: 0
            if (duration <= 0L || width <= 0 || height <= 0) {
                null
            } else {
                MediaMetadata(
                    durationMs = duration,
                    width = width,
                    height = height,
                    rotationDegrees = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                    )?.toIntOrNull() ?: 0,
                    mimeType = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                    ) ?: mimeType,
                    hasAudio = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
                    ) == "yes"
                )
            }
        } finally {
            retriever.release()
        }
    }

    private fun readImage(context: Context, uri: Uri, mimeType: String): MediaMetadata? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        var opened = false
        if (uri.scheme == "file") {
            val file = uri.path?.let(::File) ?: return null
            file.inputStream().use { stream ->
                opened = true
                BitmapFactory.decodeStream(stream, null, options)
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                opened = true
                BitmapFactory.decodeStream(stream, null, options)
            }
        }
        if (!opened) return null

        return if (options.outWidth > 0 && options.outHeight > 0) {
            MediaMetadata(
                durationMs = 0L,
                width = options.outWidth,
                height = options.outHeight,
                rotationDegrees = 0,
                mimeType = mimeType,
                hasAudio = false
            )
        } else {
            null
        }
    }

    private fun inferMimeType(uri: Uri): String {
        return when (uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()) {
            "mp4" -> "video/mp4"
            "m4v" -> "video/x-m4v"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "heic" -> "image/heic"
            "gif" -> "image/gif"
            else -> "application/octet-stream"
        }
    }

    private fun MediaMetadataRetriever.setDataSourceCompat(context: Context, uri: Uri) {
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            setDataSource(uri.path)
        } else {
            setDataSource(context, uri)
        }
    }
}
