package com.example

/**
 * A resolved, time-specific view of the canonical timeline.
 *
 * Preview and export both need the same answers to three questions: which
 * layers are active, what is their local time, and which keyframed properties
 * should be used for this frame. Keeping those answers outside either UI or
 * Media3 makes the timeline semantics deterministic and testable.
 */
data class TimelineRenderLayer(
    val layer: TimelineLayer,
    val localTimeMs: Long,
    val properties: TimelineLayerProperties
) {
    val id: String get() = layer.id
    val kind: TimelineLayerKind get() = layer.kind
    val track: TimelineTrackType get() = layer.track
}

data class TimelineRenderFrame(
    val timeMs: Long,
    val primaryVideo: TimelineRenderLayer?,
    val visualLayers: List<TimelineRenderLayer>,
    val audioLayers: List<TimelineRenderLayer>
) {
    /** All visible renderable layers in the order in which they should be composited. */
    val compositedLayers: List<TimelineRenderLayer>
        get() = buildList {
            primaryVideo?.let(::add)
            addAll(visualLayers)
        }

    fun visualLayer(id: String): TimelineRenderLayer? =
        visualLayers.firstOrNull { it.id == id }

    fun visualZIndex(id: String, fallback: Float = 0f): Float =
        visualLayer(id)?.layer?.zIndex?.toFloat() ?: fallback
}

class TimelineRenderGraph(project: TimelineProject) {
    /** Normalization is performed once so preview and export see identical data. */
    val project: TimelineProject = project.normalized()

    private val visualLayerOrder: List<TimelineLayer> = this.project.layers
        .asSequence()
        .filter { it.track == TimelineTrackType.VISUAL_OVERLAY || it.track == TimelineTrackType.CAPTIONS }
        .sortedWith(layerOrderComparator)
        .toList()

    /** Stable layer order for exporters that create one overlay per timeline layer. */
    fun visualLayersInOrder(): List<TimelineLayer> = visualLayerOrder

    /** Resolve the complete frame plan at a global project time. */
    fun frameAt(timeMs: Long): TimelineRenderFrame {
        val resolvedTimeMs = timeMs.coerceAtLeast(0L)
        val activeLayers = project.layers.asSequence()
            .filter { it.isVisible && it.contains(resolvedTimeMs) }
            .map { layer ->
                TimelineRenderLayer(
                    layer = layer,
                    localTimeMs = (resolvedTimeMs - layer.startTimeMs).coerceAtLeast(0L),
                    properties = layer.properties.atTime(resolvedTimeMs - layer.startTimeMs)
                )
            }
            .toList()

        val primaryVideo = activeLayers
            .asSequence()
            .filter { it.track == TimelineTrackType.PRIMARY_VIDEO && it.kind == TimelineLayerKind.VIDEO_CLIP }
            .sortedWith(renderLayerOrderComparator)
            .firstOrNull()

        val visualLayers = activeLayers
            .asSequence()
            .filter { it.track == TimelineTrackType.VISUAL_OVERLAY || it.track == TimelineTrackType.CAPTIONS }
            .sortedWith(renderLayerOrderComparator)
            .toList()

        val audioLayers = activeLayers
            .asSequence()
            .filter { it.track == TimelineTrackType.AUDIO }
            .sortedWith(renderLayerOrderComparator)
            .toList()

        return TimelineRenderFrame(
            timeMs = resolvedTimeMs,
            primaryVideo = primaryVideo,
            visualLayers = visualLayers,
            audioLayers = audioLayers
        )
    }

    /** Resolve one layer without requiring callers to duplicate timing logic. */
    fun layerAt(id: String, timeMs: Long): TimelineRenderLayer? =
        frameAt(timeMs).let { frame ->
            frame.primaryVideo?.takeIf { it.id == id }
                ?: frame.visualLayer(id)
                ?: frame.audioLayers.firstOrNull { it.id == id }
        }

    private companion object {
        val layerOrderComparator = compareBy<TimelineLayer> { it.zIndex }
            .thenBy { it.startTimeMs }
            .thenBy { it.id }

        val renderLayerOrderComparator = compareBy<TimelineRenderLayer> { it.layer.zIndex }
            .thenBy { it.layer.startTimeMs }
            .thenBy { it.id }
    }
}

/** Resolve shared properties for a layer's local timeline time. */
fun TimelineLayerProperties.atTime(localTimeMs: Long): TimelineLayerProperties {
    val safeTimeMs = localTimeMs.coerceAtLeast(0L)
    val resolvedTransform = transform.atTime(keyframes, safeTimeMs)
    return copy(
        opacity = keyframes.getValueAtTime("opacity", safeTimeMs, opacity)
            .finiteOr(opacity)
            .coerceIn(0f, 1f),
        transform = resolvedTransform,
        volume = keyframes.getValueAtTime("volume", safeTimeMs, volume)
            .finiteOr(volume)
            .coerceIn(0f, 1f)
    )
}

private fun TimelineTransform.atTime(
    keyframes: Map<String, List<Keyframe>>,
    localTimeMs: Long
): TimelineTransform {
    val scaleKeyframe = keyframes.getValueAtTime("scale", localTimeMs, Float.NaN)
    val scaleXDefault = if (scaleKeyframe.isFinite()) scaleKeyframe else scaleX
    val scaleYDefault = if (scaleKeyframe.isFinite()) scaleKeyframe else scaleY

    return copy(
        positionX = keyframes.getValueAtTime("posX", localTimeMs, positionX)
            .finiteOr(positionX)
            .coerceIn(0f, 1f),
        positionY = keyframes.getValueAtTime("posY", localTimeMs, positionY)
            .finiteOr(positionY)
            .coerceIn(0f, 1f),
        scaleX = keyframes.getValueAtTime("scaleX", localTimeMs, scaleXDefault)
            .finiteOr(scaleXDefault)
            .coerceAtLeast(0.001f),
        scaleY = keyframes.getValueAtTime("scaleY", localTimeMs, scaleYDefault)
            .finiteOr(scaleYDefault)
            .coerceAtLeast(0.001f),
        rotationDegrees = keyframes.getValueAtTime("rotation", localTimeMs, rotationDegrees)
            .finiteOr(rotationDegrees),
        cropLeft = keyframes.getValueAtTime("cropLeft", localTimeMs, cropLeft)
            .finiteOr(cropLeft)
            .coerceIn(0f, 1f),
        cropTop = keyframes.getValueAtTime("cropTop", localTimeMs, cropTop)
            .finiteOr(cropTop)
            .coerceIn(0f, 1f),
        cropRight = keyframes.getValueAtTime("cropRight", localTimeMs, cropRight)
            .finiteOr(cropRight)
            .coerceIn(0f, 1f),
        cropBottom = keyframes.getValueAtTime("cropBottom", localTimeMs, cropBottom)
            .finiteOr(cropBottom)
            .coerceIn(0f, 1f)
    ).normalized()
}

private fun Float.finiteOr(fallback: Float): Float = takeIf { isFinite() } ?: fallback
