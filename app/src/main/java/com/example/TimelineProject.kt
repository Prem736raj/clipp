package com.example

import androidx.compose.ui.geometry.Rect

/**
 * Versioned, non-destructive representation of an editor project.
 *
 * The existing editor state predates a unified timeline and stores each layer
 * type in a separate collection. This document is the compatibility boundary
 * for the new editor architecture: every visual or audio item has one timing
 * range, one ordering value, one shared property bag, and one typed payload.
 * The legacy state remains supported while callers migrate through the
 * [EditorState.toTimelineProject] and [TimelineProject.toEditorState] adapters.
 */
internal const val TIMELINE_PROJECT_SCHEMA_VERSION = 1

enum class TimelineTrackType {
    PRIMARY_VIDEO,
    VISUAL_OVERLAY,
    CAPTIONS,
    AUDIO
}

enum class TimelineLayerKind {
    VIDEO_CLIP,
    IMAGE_OVERLAY,
    TEXT,
    CAPTION,
    STICKER,
    DRAWING,
    FRAME,
    AUDIO
}

/** Shared transform values used by the preview, timeline tools, and exporter. */
data class TimelineTransform(
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationDegrees: Float = 0f,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
) {
    fun normalized(): TimelineTransform = copy(
        positionX = positionX.safeFinite(0.5f).coerceIn(0f, 1f),
        positionY = positionY.safeFinite(0.5f).coerceIn(0f, 1f),
        scaleX = scaleX.safeFinite(1f).coerceAtLeast(0.001f),
        scaleY = scaleY.safeFinite(1f).coerceAtLeast(0.001f),
        rotationDegrees = rotationDegrees.safeFinite(0f),
        cropLeft = cropLeft.safeFinite(0f).coerceIn(0f, 1f),
        cropTop = cropTop.safeFinite(0f).coerceIn(0f, 1f),
        cropRight = cropRight.safeFinite(1f).coerceIn(0f, 1f),
        cropBottom = cropBottom.safeFinite(1f).coerceIn(0f, 1f)
    )

    fun asRect(): Rect = Rect(cropLeft, cropTop, cropRight, cropBottom)
}

/** Values that can eventually be animated by the generic keyframe engine. */
data class TimelineLayerProperties(
    val opacity: Float = 1f,
    val transform: TimelineTransform = TimelineTransform(),
    val blendMode: OverlayBlendModeType = OverlayBlendModeType.NORMAL,
    val keyframes: Map<String, List<Keyframe>> = emptyMap(),
    val volume: Float = 1f,
    val isMuted: Boolean = false
) {
    fun normalized(): TimelineLayerProperties = copy(
        opacity = opacity.safeFinite(1f).coerceIn(0f, 1f),
        transform = transform.normalized(),
        volume = volume.safeFinite(1f).coerceIn(0f, 1f),
        keyframes = keyframes.mapValues { (_, values) -> values.sortedBy { it.timeMs } }
    )
}

/**
 * Flat payload keeps this document serializable with the existing Moshi
 * configuration while retaining a single layer list. Exactly one payload
 * field should be populated according to [TimelineLayer.kind].
 */
data class TimelineLayerPayload(
    val mediaClip: MediaClip? = null,
    val overlay: OverlayClip? = null,
    val text: TextOverlay? = null,
    val caption: AutoCaptionSegment? = null,
    val sticker: StickerOverlay? = null,
    val drawing: DrawOverlay? = null,
    val frame: FrameOverlay? = null,
    val audioClip: AudioClip? = null
)

data class TimelineLayer(
    val id: String,
    val kind: TimelineLayerKind,
    val track: TimelineTrackType,
    val startTimeMs: Long,
    val durationMs: Long,
    /** Z-order for visual layers, sequence order for the primary video track. */
    val zIndex: Int,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val properties: TimelineLayerProperties = TimelineLayerProperties(),
    val payload: TimelineLayerPayload = TimelineLayerPayload()
) {
    val endTimeMs: Long get() = startTimeMs + durationMs.coerceAtLeast(0L)

    fun contains(timeMs: Long): Boolean =
        timeMs >= startTimeMs && timeMs < endTimeMs

    fun normalized(): TimelineLayer = copy(
        startTimeMs = startTimeMs.coerceAtLeast(0L),
        durationMs = durationMs.coerceAtLeast(0L),
        properties = properties.normalized()
    )

    fun hasExpectedPayload(): Boolean = when (kind) {
        TimelineLayerKind.VIDEO_CLIP -> payload.mediaClip != null
        TimelineLayerKind.IMAGE_OVERLAY -> payload.overlay != null
        TimelineLayerKind.TEXT -> payload.text != null
        TimelineLayerKind.CAPTION -> payload.caption != null
        TimelineLayerKind.STICKER -> payload.sticker != null
        TimelineLayerKind.DRAWING -> payload.drawing != null
        TimelineLayerKind.FRAME -> payload.frame != null
        TimelineLayerKind.AUDIO -> payload.audioClip != null
    }

    fun hasExpectedTrack(): Boolean = when (kind) {
        TimelineLayerKind.VIDEO_CLIP -> track == TimelineTrackType.PRIMARY_VIDEO
        TimelineLayerKind.CAPTION -> track == TimelineTrackType.CAPTIONS
        TimelineLayerKind.AUDIO -> track == TimelineTrackType.AUDIO
        TimelineLayerKind.IMAGE_OVERLAY,
        TimelineLayerKind.TEXT,
        TimelineLayerKind.STICKER,
        TimelineLayerKind.DRAWING,
        TimelineLayerKind.FRAME -> track == TimelineTrackType.VISUAL_OVERLAY
    }
}

data class TimelineProject(
    val schemaVersion: Int = TIMELINE_PROJECT_SCHEMA_VERSION,
    val canvasSettings: CanvasSettingsState = CanvasSettingsState(),
    val captionSettings: CaptionSettings = CaptionSettings(),
    val layers: List<TimelineLayer> = emptyList()
) {
    /** Duration is driven by visual content; audio cannot extend the project. */
    val durationMs: Long
        get() = layers
            .asSequence()
            .filter { it.track != TimelineTrackType.AUDIO }
            .map { it.endTimeMs }
            .maxOrNull()
            ?: 0L

    fun normalized(): TimelineProject = copy(
        layers = layers.map(TimelineLayer::normalized)
    )

    fun visualLayersAt(timeMs: Long): List<TimelineLayer> = layers
        .asSequence()
        .filter { it.track != TimelineTrackType.AUDIO && it.isVisible && it.contains(timeMs) }
        .sortedWith(compareBy<TimelineLayer> { it.zIndex }.thenBy { it.startTimeMs }.thenBy { it.id })
        .toList()

    /** Returns stable structural errors instead of silently dropping bad data. */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (schemaVersion <= 0) errors += "invalid schema version"

        val duplicateIds = layers
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        duplicateIds.forEach { errors += "duplicate layer id: $it" }

        layers.forEach { layer ->
            if (layer.id.isBlank()) errors += "blank layer id"
            if (layer.startTimeMs < 0L) errors += "negative layer start: ${layer.id}"
            if (layer.durationMs <= 0L) errors += "non-positive layer duration: ${layer.id}"
            if (!layer.hasExpectedPayload()) errors += "missing layer payload: ${layer.id}"
            if (!layer.hasExpectedTrack()) errors += "invalid layer track: ${layer.id}"
            if (!layer.properties.opacity.isFinite() || layer.properties.opacity !in 0f..1f) {
                errors += "invalid layer opacity: ${layer.id}"
            }
            if (!layer.properties.volume.isFinite() || layer.properties.volume !in 0f..1f) {
                errors += "invalid layer volume: ${layer.id}"
            }
        }
        return errors
    }
}

/** Converts the current split editor state into the canonical layer document. */
fun EditorState.toTimelineProject(): TimelineProject {
    val layers = mutableListOf<TimelineLayer>()
    var primaryStartMs = 0L

    clips.forEachIndexed { index, clip ->
        val normalized = clip.normalized()
        layers += TimelineLayer(
            id = normalized.id,
            kind = TimelineLayerKind.VIDEO_CLIP,
            track = TimelineTrackType.PRIMARY_VIDEO,
            startTimeMs = primaryStartMs,
            durationMs = normalized.durationMs,
            zIndex = index,
            properties = normalized.toTimelineProperties(),
            payload = TimelineLayerPayload(mediaClip = normalized)
        )
        primaryStartMs += normalized.durationMs
    }

    val visualIds = overlays.map { it.id } +
        texts.map { it.id } +
        captions.map { it.id } +
        stickers.map { it.id } +
        drawings.map { it.id } +
        frames.map { it.id }
    val orderedVisualIds = (layerOrder + visualIds)
        .filter { it in visualIds }
        .distinct()

    fun visualZIndex(id: String): Int = orderedVisualIds.indexOf(id).takeIf { it >= 0 } ?: orderedVisualIds.size

    overlays.forEach { overlay ->
        layers += TimelineLayer(
            id = overlay.id,
            kind = TimelineLayerKind.IMAGE_OVERLAY,
            track = TimelineTrackType.VISUAL_OVERLAY,
            startTimeMs = overlay.startTimeOnTimelineMs.coerceAtLeast(0L),
            durationMs = overlay.durationMs.coerceAtLeast(0L),
            zIndex = visualZIndex(overlay.id),
            isVisible = overlay.isVisible,
            isLocked = overlay.isLocked,
            properties = overlay.toTimelineProperties(),
            payload = TimelineLayerPayload(overlay = overlay)
        )
    }
    texts.forEach { text ->
        layers += TimelineLayer(
            id = text.id,
            kind = TimelineLayerKind.TEXT,
            track = TimelineTrackType.VISUAL_OVERLAY,
            startTimeMs = text.startTimeOnTimelineMs.coerceAtLeast(0L),
            durationMs = text.durationMs.coerceAtLeast(0L),
            zIndex = visualZIndex(text.id),
            isVisible = text.isVisible,
            isLocked = text.isLocked,
            properties = text.toTimelineProperties(),
            payload = TimelineLayerPayload(text = text)
        )
    }
    captions.forEach { caption ->
        layers += TimelineLayer(
            id = caption.id,
            kind = TimelineLayerKind.CAPTION,
            track = TimelineTrackType.CAPTIONS,
            startTimeMs = caption.startTimeMs.coerceAtLeast(0L),
            durationMs = caption.durationMs.coerceAtLeast(0L),
            zIndex = visualZIndex(caption.id),
            payload = TimelineLayerPayload(caption = caption)
        )
    }
    stickers.forEach { sticker ->
        layers += TimelineLayer(
            id = sticker.id,
            kind = TimelineLayerKind.STICKER,
            track = TimelineTrackType.VISUAL_OVERLAY,
            startTimeMs = sticker.startTimeOnTimelineMs.coerceAtLeast(0L),
            durationMs = sticker.durationMs.coerceAtLeast(0L),
            zIndex = visualZIndex(sticker.id),
            isVisible = sticker.isVisible,
            isLocked = sticker.isLocked,
            properties = sticker.toTimelineProperties(),
            payload = TimelineLayerPayload(sticker = sticker)
        )
    }
    drawings.forEach { drawing ->
        layers += TimelineLayer(
            id = drawing.id,
            kind = TimelineLayerKind.DRAWING,
            track = TimelineTrackType.VISUAL_OVERLAY,
            startTimeMs = drawing.startTimeOnTimelineMs.coerceAtLeast(0L),
            durationMs = drawing.durationMs.coerceAtLeast(0L),
            zIndex = visualZIndex(drawing.id),
            isVisible = drawing.isVisible,
            isLocked = drawing.isLocked,
            properties = drawing.toTimelineProperties(),
            payload = TimelineLayerPayload(drawing = drawing)
        )
    }
    frames.forEach { frame ->
        layers += TimelineLayer(
            id = frame.id,
            kind = TimelineLayerKind.FRAME,
            track = TimelineTrackType.VISUAL_OVERLAY,
            startTimeMs = frame.startTimeOnTimelineMs.coerceAtLeast(0L),
            durationMs = frame.durationMs.coerceAtLeast(0L),
            zIndex = visualZIndex(frame.id),
            isVisible = frame.isVisible,
            isLocked = frame.isLocked,
            properties = frame.toTimelineProperties(),
            payload = TimelineLayerPayload(frame = frame)
        )
    }
    audioClips.forEachIndexed { index, audio ->
        layers += TimelineLayer(
            id = audio.id,
            kind = TimelineLayerKind.AUDIO,
            track = TimelineTrackType.AUDIO,
            startTimeMs = audio.startTimeOnTimelineMs.coerceAtLeast(0L),
            durationMs = audio.durationMs.coerceAtLeast(0L),
            zIndex = index,
            properties = audio.toTimelineProperties(),
            payload = TimelineLayerPayload(audioClip = audio)
        )
    }

    return TimelineProject(
        canvasSettings = canvasSettings,
        captionSettings = captionSettings,
        layers = layers
    )
}

/** Converts the canonical document back to the legacy state during migration. */
fun TimelineProject.toEditorState(): EditorState {
    val normalized = normalized()
    val primaryLayers = normalized.layers
        .filter { it.kind == TimelineLayerKind.VIDEO_CLIP && it.track == TimelineTrackType.PRIMARY_VIDEO }
        .sortedWith(compareBy<TimelineLayer> { it.zIndex }.thenBy { it.startTimeMs }.thenBy { it.id })
    val visualLayers = normalized.layers
        .filter { it.track == TimelineTrackType.VISUAL_OVERLAY }
        .sortedWith(compareBy<TimelineLayer> { it.zIndex }.thenBy { it.startTimeMs }.thenBy { it.id })

    val clips = primaryLayers.mapNotNull { layer ->
        layer.payload.mediaClip?.let { clip ->
            val t = layer.properties.transform
            clip.copy(
                posX = t.positionX,
                posY = t.positionY,
                scale = ((t.scaleX + t.scaleY) / 2f).coerceAtLeast(0.001f),
                rotation = t.rotationDegrees,
                cropRect = t.asRect(),
                keyframes = layer.properties.keyframes,
                volume = layer.properties.volume,
                isMuted = layer.properties.isMuted
            ).normalized()
        }
    }
    val overlays = visualLayers.mapNotNull { layer ->
        if (layer.kind != TimelineLayerKind.IMAGE_OVERLAY) return@mapNotNull null
        layer.payload.overlay?.let { overlay ->
            val t = layer.properties.transform
            val safeTrimStart = overlay.trimStartMs.coerceAtLeast(0L)
            val safeOriginalDuration = overlay.originalDurationMs.coerceAtLeast(safeTrimStart + 1L)
            val safeTrimEnd = (safeTrimStart + layer.durationMs.coerceAtLeast(1L))
                .coerceIn(safeTrimStart + 1L, safeOriginalDuration)
            overlay.copy(
                trimStartMs = safeTrimStart,
                trimEndMs = safeTrimEnd,
                startTimeOnTimelineMs = layer.startTimeMs,
                posX = t.positionX,
                posY = t.positionY,
                scaleX = t.scaleX,
                scaleY = t.scaleY,
                rotation = t.rotationDegrees,
                opacity = layer.properties.opacity,
                blendMode = layer.properties.blendMode,
                keyframes = layer.properties.keyframes,
                isVisible = layer.isVisible,
                isLocked = layer.isLocked
            )
        }
    }
    val texts = visualLayers.mapNotNull { layer ->
        if (layer.kind != TimelineLayerKind.TEXT) return@mapNotNull null
        layer.payload.text?.let { text ->
            val t = layer.properties.transform
            text.copy(
                startTimeOnTimelineMs = layer.startTimeMs,
                durationMs = layer.durationMs,
                posX = t.positionX,
                posY = t.positionY,
                scale = ((t.scaleX + t.scaleY) / 2f).coerceAtLeast(0.001f),
                rotation = t.rotationDegrees,
                opacity = layer.properties.opacity,
                keyframes = layer.properties.keyframes,
                isVisible = layer.isVisible,
                isLocked = layer.isLocked
            )
        }
    }
    val stickers = visualLayers.mapNotNull { layer ->
        if (layer.kind != TimelineLayerKind.STICKER) return@mapNotNull null
        layer.payload.sticker?.let { sticker ->
            val t = layer.properties.transform
            sticker.copy(
                startTimeOnTimelineMs = layer.startTimeMs,
                durationMs = layer.durationMs,
                posX = t.positionX,
                posY = t.positionY,
                scale = ((t.scaleX + t.scaleY) / 2f).coerceAtLeast(0.001f),
                rotation = t.rotationDegrees,
                opacity = layer.properties.opacity,
                keyframes = layer.properties.keyframes,
                isVisible = layer.isVisible,
                isLocked = layer.isLocked
            )
        }
    }
    val drawings = visualLayers.mapNotNull { layer ->
        if (layer.kind != TimelineLayerKind.DRAWING) return@mapNotNull null
        layer.payload.drawing?.copy(
            startTimeOnTimelineMs = layer.startTimeMs,
            durationMs = layer.durationMs,
            opacity = layer.properties.opacity,
            isVisible = layer.isVisible,
            isLocked = layer.isLocked
        )
    }
    val frames = visualLayers.mapNotNull { layer ->
        if (layer.kind != TimelineLayerKind.FRAME) return@mapNotNull null
        layer.payload.frame?.copy(
            startTimeOnTimelineMs = layer.startTimeMs,
            durationMs = layer.durationMs,
            opacity = layer.properties.opacity,
            isVisible = layer.isVisible,
            isLocked = layer.isLocked
        )
    }
    val captions = normalized.layers
        .filter { it.kind == TimelineLayerKind.CAPTION && it.track == TimelineTrackType.CAPTIONS }
        .sortedWith(compareBy<TimelineLayer> { it.zIndex }.thenBy { it.startTimeMs }.thenBy { it.id })
        .mapNotNull { layer ->
            layer.payload.caption?.copy(
                startTimeMs = layer.startTimeMs,
                durationMs = layer.durationMs
            )
        }
    val audioClips = normalized.layers
        .filter { it.kind == TimelineLayerKind.AUDIO && it.track == TimelineTrackType.AUDIO }
        .sortedWith(compareBy<TimelineLayer> { it.zIndex }.thenBy { it.startTimeMs }.thenBy { it.id })
        .mapNotNull { layer ->
            layer.payload.audioClip?.let { audio ->
                val safeTrimStart = audio.trimStartMs.coerceAtLeast(0L)
                val safeSourceDuration = audio.sourceDurationMs.coerceAtLeast(safeTrimStart + 1L)
                val safeTrimEnd = (safeTrimStart + layer.durationMs.coerceAtLeast(1L))
                    .coerceIn(safeTrimStart + 1L, safeSourceDuration)
                audio.copy(
                    startTimeOnTimelineMs = layer.startTimeMs,
                    trimStartMs = safeTrimStart,
                    trimEndMs = safeTrimEnd,
                    volume = layer.properties.volume,
                    isMuted = layer.properties.isMuted,
                    keyframes = layer.properties.keyframes
                )
            }
        }

    return EditorState(
        clips = clips,
        canvasSettings = normalized.canvasSettings,
        overlays = overlays,
        texts = texts,
        captions = captions,
        captionSettings = normalized.captionSettings,
        stickers = stickers,
        drawings = drawings,
        frames = frames,
        audioClips = audioClips,
        layerOrder = visualLayers.map { it.id }
    )
}

/** Prefer the canonical document when available, while reading old projects. */
fun EditorHistoryModel.restoredEditorState(): EditorState? =
    timelineProject?.toEditorState() ?: currentState

private fun MediaClip.toTimelineProperties(): TimelineLayerProperties = TimelineLayerProperties(
    transform = TimelineTransform(
        positionX = posX,
        positionY = posY,
        scaleX = scale,
        scaleY = scale,
        rotationDegrees = rotation,
        cropLeft = cropRect.left,
        cropTop = cropRect.top,
        cropRight = cropRect.right,
        cropBottom = cropRect.bottom
    ),
    keyframes = keyframes,
    volume = volume,
    isMuted = isMuted
)

private fun OverlayClip.toTimelineProperties(): TimelineLayerProperties = TimelineLayerProperties(
    opacity = opacity,
    transform = TimelineTransform(
        positionX = posX,
        positionY = posY,
        scaleX = scaleX,
        scaleY = scaleY,
        rotationDegrees = rotation
    ),
    blendMode = blendMode,
    keyframes = keyframes
)

private fun TextOverlay.toTimelineProperties(): TimelineLayerProperties = TimelineLayerProperties(
    opacity = opacity,
    transform = TimelineTransform(
        positionX = posX,
        positionY = posY,
        scaleX = scale,
        scaleY = scale,
        rotationDegrees = rotation
    ),
    keyframes = keyframes
)

private fun StickerOverlay.toTimelineProperties(): TimelineLayerProperties = TimelineLayerProperties(
    opacity = opacity,
    transform = TimelineTransform(
        positionX = posX,
        positionY = posY,
        scaleX = scale,
        scaleY = scale,
        rotationDegrees = rotation
    ),
    keyframes = keyframes
)

private fun DrawOverlay.toTimelineProperties(): TimelineLayerProperties =
    TimelineLayerProperties(opacity = opacity)

private fun FrameOverlay.toTimelineProperties(): TimelineLayerProperties =
    TimelineLayerProperties(opacity = opacity)

private fun AudioClip.toTimelineProperties(): TimelineLayerProperties = TimelineLayerProperties(
    keyframes = keyframes,
    volume = volume,
    isMuted = isMuted
)

private fun Float.safeFinite(fallback: Float): Float = takeIf { isFinite() } ?: fallback
