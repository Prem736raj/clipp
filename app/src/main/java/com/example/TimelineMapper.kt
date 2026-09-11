package com.example

/**
 * Canonical mapping between the project timeline and the source position used by
 * the Media3 player. A clip's timeline duration is playback-time duration, while
 * the player seeks within the clipped source window.
 */
data class TimelinePosition(
    val clipIndex: Int,
    val timelineStartMs: Long,
    val timelineOffsetMs: Long,
    val sourcePositionMs: Long
)

data class PlayerSeekPosition(
    val clipIndex: Int,
    val positionInClippedSourceMs: Long
)

class TimelineMapper(clips: List<MediaClip>) {
    private data class Segment(
        val clipIndex: Int,
        val clip: MediaClip,
        val startMs: Long,
        val durationMs: Long,
        val speedTimeline: SpeedCurveTimeline?
    )

    private val segments = buildList {
        var startMs = 0L
        clips.forEachIndexed { index, clip ->
            val normalizedClip = clip.normalized()
            val sourceDurationMs = normalizedClip.effectiveTrimEndMs - normalizedClip.effectiveTrimStartMs
            val speedTimeline = normalizedClip.speedCurve
                ?.takeIf { it.points.isNotEmpty() && sourceDurationMs > 0L }
                ?.let { SpeedCurveTimeline(sourceDurationMs, it) }
            val durationMs = (speedTimeline?.durationMs ?: normalizedClip.durationMs)
                .coerceAtLeast(0L)
            add(Segment(index, normalizedClip, startMs, durationMs, speedTimeline))
            startMs += durationMs
        }
    }

    val totalDurationMs: Long = segments.lastOrNull()?.let { it.startMs + it.durationMs } ?: 0L

    fun locate(globalPositionMs: Long): TimelinePosition? {
        val segment = segmentAt(globalPositionMs) ?: return null
        val global = globalPositionMs.coerceIn(0L, totalDurationMs)
        val timelineOffsetMs = (global - segment.startMs).coerceIn(0L, segment.durationMs)
        return TimelinePosition(
            clipIndex = segment.clipIndex,
            timelineStartMs = segment.startMs,
            timelineOffsetMs = timelineOffsetMs,
            sourcePositionMs = sourcePositionForTimelineOffset(segment, timelineOffsetMs)
        )
    }

    fun playerSeekPosition(globalPositionMs: Long): PlayerSeekPosition? {
        val position = locate(globalPositionMs) ?: return null
        val clip = segments[position.clipIndex].clip
        val sourceOffsetMs = (position.sourcePositionMs - clip.effectiveTrimStartMs)
            .coerceIn(0L, sourceDurationMs(clip))
        return PlayerSeekPosition(position.clipIndex, sourceOffsetMs)
    }

    fun globalPositionForPlayer(clipIndex: Int, positionInClippedSourceMs: Long): Long {
        val segment = segments.getOrNull(clipIndex) ?: return 0L
        val sourceOffsetMs = positionInClippedSourceMs.coerceIn(0L, sourceDurationMs(segment.clip))
        val timelineOffsetMs = timelineOffsetForSourceOffset(segment, sourceOffsetMs)
        return (segment.startMs + timelineOffsetMs).coerceIn(0L, totalDurationMs)
    }

    fun globalPositionForSource(clipIndex: Int, sourcePositionMs: Long): Long {
        val segment = segments.getOrNull(clipIndex) ?: return 0L
        val sourceOffsetMs = (sourcePositionMs - segment.clip.effectiveTrimStartMs)
            .coerceIn(0L, sourceDurationMs(segment.clip))
        return globalPositionForPlayer(clipIndex, sourceOffsetMs)
    }

    fun clipLocalToGlobal(clipIndex: Int, localTimelineMs: Long): Long {
        val segment = segments.getOrNull(clipIndex) ?: return 0L
        return (segment.startMs + localTimelineMs.coerceIn(0L, segment.durationMs))
            .coerceIn(0L, totalDurationMs)
    }

    private fun segmentAt(globalPositionMs: Long): Segment? {
        if (segments.isEmpty()) return null
        val global = globalPositionMs.coerceIn(0L, totalDurationMs)
        return segments.firstOrNull { segment ->
            global < segment.startMs + segment.durationMs
        } ?: segments.lastOrNull()
    }

    private fun sourcePositionForTimelineOffset(segment: Segment, timelineOffsetMs: Long): Long {
        val clip = segment.clip
        val sourceDurationMs = sourceDurationMs(clip)
        if (sourceDurationMs <= 0L) return clip.trimStartMs
        val sourceOffsetMs = if (segment.speedTimeline != null) {
            val fraction = segment.speedTimeline.sourceFractionAtPlaybackTime(timelineOffsetMs)
            (fraction * sourceDurationMs).toLong()
        } else {
            (timelineOffsetMs * clip.playbackSpeed.coerceIn(0.1f, 10f)).toLong()
        }
        return clip.effectiveTrimStartMs + sourceOffsetMs.coerceIn(0L, sourceDurationMs)
    }

    private fun timelineOffsetForSourceOffset(segment: Segment, sourceOffsetMs: Long): Long {
        val clip = segment.clip
        val safeSourceDurationMs = sourceDurationMs(clip)
        val safeSourceOffsetMs = sourceOffsetMs.coerceIn(0L, safeSourceDurationMs)
        return if (segment.speedTimeline != null) {
            segment.speedTimeline.playbackTimeAtSourceFraction(
                safeSourceOffsetMs.toFloat() / safeSourceDurationMs.coerceAtLeast(1L).toFloat()
            )
        } else {
            (safeSourceOffsetMs / clip.playbackSpeed.coerceIn(0.1f, 10f)).toLong()
        }.coerceIn(0L, segment.durationMs.coerceAtLeast(0L))
    }

    private fun sourceDurationMs(clip: MediaClip): Long =
        clip.effectiveTrimEndMs - clip.effectiveTrimStartMs
}
