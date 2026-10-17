package com.example

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File

internal enum class ExportSourceKind {
    PRIMARY_CLIP,
    OVERLAY,
    AUDIO
}

internal data class ExportSourceReference(
    val uri: String,
    val kind: ExportSourceKind,
    val expectsImage: Boolean? = null
)

internal data class ExportSourcePlan(
    val references: List<ExportSourceReference>,
    val blockingReason: String? = null
)

internal fun EditorState.exportSourcePlan(): ExportSourcePlan {
    val references = mutableListOf<ExportSourceReference>()
    clips.forEach { clip ->
        if (clip.sourceUri.isBlank()) {
            return ExportSourcePlan(
                references = references,
                blockingReason = "A source clip is missing or no longer readable. Re-link it and try again."
            )
        }
        references += ExportSourceReference(
            clip.sourceUri,
            ExportSourceKind.PRIMARY_CLIP,
            expectsImage = clip.isPhoto
        )
    }

    overlays
        .filter { it.isVisible && it.durationMs > 0L }
        .forEach { overlay ->
            if (overlay.sourceUri.isBlank()) {
                return ExportSourcePlan(
                    references = references,
                    blockingReason = "An overlay source is missing or no longer readable. Re-link it and try again."
                )
            }
            references += ExportSourceReference(
                overlay.sourceUri,
                ExportSourceKind.OVERLAY,
                expectsImage = overlay.isPhoto || overlay.isGif
            )
        }

    val projectDurationMs = clips.sumOf { it.durationMs }
    val clipsById = clips.associateBy { it.id }
    audioClips.forEach { audioClip ->
        if (audioClip.buildExportSegments(projectDurationMs).isEmpty()) return@forEach
        val sourceUri = audioClip.sourceUri?.takeIf { it.isNotBlank() }
            ?: audioClip.sourceClipId
                ?.takeIf { it.isNotBlank() }
                ?.let { clipsById[it]?.sourceUri }
                ?.takeIf { it.isNotBlank() }
        if (sourceUri == null) {
            return ExportSourcePlan(
                references = references,
                blockingReason = "An audio source is missing or no longer available. Re-link it and try again."
            )
        }
        references += ExportSourceReference(sourceUri, ExportSourceKind.AUDIO)
    }

    return ExportSourcePlan(references.distinct())
}

internal suspend fun EditorState.exportSourceBlockingReason(context: Context): String? {
    val plan = exportSourcePlan()
    plan.blockingReason?.let { return it }
    val unreadable = plan.references.firstOrNull { !isExportSourceReadable(context, it.uri) }
    if (unreadable != null) {
        return when (unreadable.kind) {
            ExportSourceKind.PRIMARY_CLIP ->
                "A source clip is missing or no longer readable. Re-link it and try again."
            ExportSourceKind.OVERLAY ->
                "An overlay source is missing or no longer readable. Re-link it and try again."
            ExportSourceKind.AUDIO ->
                "An audio source is missing or no longer readable. Re-link it and try again."
        }
    }

    val invalid = plan.references.firstOrNull { !isExportSourceDecodable(context, it) } ?: return null
    return when (invalid.kind) {
        ExportSourceKind.PRIMARY_CLIP ->
            "A source clip is corrupt or unsupported. Replace or re-link it and try again."
        ExportSourceKind.OVERLAY ->
            "An overlay source is corrupt or unsupported. Replace or re-link it and try again."
        ExportSourceKind.AUDIO ->
            "An audio source is corrupt, unsupported, or contains no audio track. Replace or re-link it and try again."
    }
}

private suspend fun isExportSourceDecodable(
    context: Context,
    reference: ExportSourceReference
): Boolean {
    val uri = Uri.parse(reference.uri)
    return when (reference.kind) {
        ExportSourceKind.AUDIO -> hasAudioTrack(context, uri)
        ExportSourceKind.PRIMARY_CLIP, ExportSourceKind.OVERLAY -> {
            val metadata = MediaMetadataReader.read(context, uri) ?: return false
            when (reference.expectsImage) {
                true -> metadata.mimeType.startsWith("image/")
                false -> metadata.mimeType.startsWith("video/")
                null -> true
            }
        }
    }
}

private fun hasAudioTrack(context: Context, uri: Uri): Boolean = runCatching {
    val extractor = MediaExtractor()
    try {
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            extractor.setDataSource(uri.path!!)
        } else {
            extractor.setDataSource(context, uri, null)
        }
        (0 until extractor.trackCount).any { index ->
            extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        }
    } finally {
        extractor.release()
    }
}.getOrDefault(false)

private fun isExportSourceReadable(context: Context, sourceUri: String): Boolean {
    return runCatching {
        val uri = Uri.parse(sourceUri)
        when (uri.scheme?.lowercase()) {
            null, "" -> {
                val file = File(sourceUri)
                file.isFile && file.canRead() && file.length() > 0L
            }
            "file" -> {
                val path = uri.path ?: return@runCatching false
                val file = File(path)
                file.isFile && file.canRead() && file.length() > 0L
            }
            else -> {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    descriptor.length != 0L
                } ?: context.contentResolver.openInputStream(uri)?.use { input ->
                    input.read() >= 0
                } ?: false
            }
        }
    }.getOrDefault(false)
}
