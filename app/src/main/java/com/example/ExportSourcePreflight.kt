package com.example

import android.content.Context
import android.net.Uri
import java.io.File

internal enum class ExportSourceKind {
    PRIMARY_CLIP,
    OVERLAY,
    AUDIO
}

internal data class ExportSourceReference(
    val uri: String,
    val kind: ExportSourceKind
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
        references += ExportSourceReference(clip.sourceUri, ExportSourceKind.PRIMARY_CLIP)
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
            references += ExportSourceReference(overlay.sourceUri, ExportSourceKind.OVERLAY)
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

internal fun EditorState.exportSourceBlockingReason(context: Context): String? {
    val plan = exportSourcePlan()
    plan.blockingReason?.let { return it }
    val unreadable = plan.references.firstOrNull { !isExportSourceReadable(context, it.uri) } ?: return null
    return when (unreadable.kind) {
        ExportSourceKind.PRIMARY_CLIP ->
            "A source clip is missing or no longer readable. Re-link it and try again."
        ExportSourceKind.OVERLAY ->
            "An overlay source is missing or no longer readable. Re-link it and try again."
        ExportSourceKind.AUDIO ->
            "An audio source is missing or no longer readable. Re-link it and try again."
    }
}

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
