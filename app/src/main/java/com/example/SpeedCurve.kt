package com.example

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

private const val MIN_SPEED_CURVE = 0.1f
private const val MAX_SPEED_CURVE = 10f
private const val SPEED_CURVE_TIMELINE_SAMPLES = 256
internal const val SPEED_CURVE_EXPORT_MIN_SEGMENTS = 16
internal const val SPEED_CURVE_EXPORT_MAX_SEGMENTS = 48

data class SpeedPoint(val x: Float, val y: Float)

data class SpeedCurve(
    val points: List<SpeedPoint> = listOf(SpeedPoint(0f, 1f), SpeedPoint(1f, 1f))
) {
    /** Returns a finite, ordered curve with endpoint samples for safe playback and export. */
    fun normalized(): SpeedCurve {
        val finitePoints = points
            .filter { it.x.isFinite() && it.y.isFinite() }
            .map { point ->
                SpeedPoint(
                    x = point.x.coerceIn(0f, 1f),
                    y = point.y.coerceIn(MIN_SPEED_CURVE, MAX_SPEED_CURVE)
                )
            }
            .sortedBy { it.x }

        if (finitePoints.isEmpty()) return SpeedCurve()

        val uniquePoints = mutableListOf<SpeedPoint>()
        finitePoints.forEach { point ->
            if (uniquePoints.lastOrNull()?.x == point.x) {
                uniquePoints[uniquePoints.lastIndex] = point
            } else {
                uniquePoints += point
            }
        }

        if (uniquePoints.first().x > 0f) {
            uniquePoints.add(0, uniquePoints.first().copy(x = 0f))
        } else {
            uniquePoints[0] = uniquePoints.first().copy(x = 0f)
        }
        if (uniquePoints.last().x < 1f) {
            uniquePoints += uniquePoints.last().copy(x = 1f)
        } else {
            uniquePoints[uniquePoints.lastIndex] = uniquePoints.last().copy(x = 1f)
        }

        return SpeedCurve(uniquePoints)
    }

    /** Invalid serialized values are rejected; repairable endpoint/ordering issues are normalized. */
    internal fun isExportSafe(): Boolean = points.isNotEmpty() && points.all { point ->
        point.x.isFinite() && point.y.isFinite() &&
            point.x in 0f..1f && point.y in MIN_SPEED_CURVE..MAX_SPEED_CURVE
    }

    internal fun isEffectivelyConstant(): Boolean {
        val safePoints = normalized().points
        val speed = safePoints.firstOrNull()?.y ?: 1f
        return safePoints.all { abs(it.y - speed) < 0.0005f }
    }

    /** Smooth Catmull-Rom interpolation used by preview and the export sampler. */
    fun getSpeedAt(x: Float): Float = interpolateSpeed(normalized().points, x)
}

private fun interpolateSpeed(points: List<SpeedPoint>, x: Float): Float {
    if (points.isEmpty()) return 1f
    if (points.size == 1) return points.first().y.coerceIn(MIN_SPEED_CURVE, MAX_SPEED_CURVE)

    val targetX = x.coerceIn(0f, 1f)
    if (targetX <= points.first().x) return points.first().y
    if (targetX >= points.last().x) return points.last().y

    for (i in 0 until points.size - 1) {
        val first = points[i]
        val second = points[i + 1]
        if (targetX < first.x || targetX > second.x) continue
        val span = second.x - first.x
        if (span <= 0f) return first.y
        val t = ((targetX - first.x) / span).coerceIn(0f, 1f)
        val previous = if (i > 0) points[i - 1] else first
        val next = if (i < points.size - 2) points[i + 2] else second
        val t2 = t * t
        val t3 = t2 * t
        val result = 0.5f * (
            (2f * first.y) +
                (-previous.y + second.y) * t +
                (2f * previous.y - 5f * first.y + 4f * second.y - next.y) * t2 +
                (-previous.y + 3f * first.y - 3f * second.y + next.y) * t3
            )
        return result.coerceIn(MIN_SPEED_CURVE, MAX_SPEED_CURVE)
    }
    return points.last().y.coerceIn(MIN_SPEED_CURVE, MAX_SPEED_CURVE)
}

/** One sampled export item and its original curve-time range. */
internal data class SpeedCurveSegmentWindow(
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val playbackStartMs: Long,
    val playbackEndMs: Long
) {
    val playbackDurationMs: Long
        get() = (playbackEndMs - playbackStartMs).coerceAtLeast(1L)
}

/**
 * Shared source/playback clock for variable-speed clips.
 *
 * The curve values are absolute playback multipliers. The lookup table keeps
 * preview seeking, timeline mapping, thumbnails, and export segmentation on
 * the same deterministic integration path.
 */
internal class SpeedCurveTimeline(
    val sourceDurationMs: Long,
    curve: SpeedCurve
) {
    private val safeCurve = curve.normalized()
    private val fractions = DoubleArray(SPEED_CURVE_TIMELINE_SAMPLES + 1) { index ->
        index.toDouble() / SPEED_CURVE_TIMELINE_SAMPLES.toDouble()
    }
    private val playbackTimesMs = DoubleArray(SPEED_CURVE_TIMELINE_SAMPLES + 1)

    val durationMs: Long

    init {
        val safeSourceDurationMs = sourceDurationMs.coerceAtLeast(0L)
        for (index in 0 until SPEED_CURVE_TIMELINE_SAMPLES) {
            val startFraction = fractions[index].toFloat()
            val endFraction = fractions[index + 1].toFloat()
            val midpoint = (startFraction + endFraction) / 2f
            val speed = interpolateSpeed(safeCurve.points, midpoint)
                .toDouble()
                .coerceIn(MIN_SPEED_CURVE.toDouble(), MAX_SPEED_CURVE.toDouble())
            val sourceFractionDelta = endFraction - startFraction
            playbackTimesMs[index + 1] = playbackTimesMs[index] +
                (safeSourceDurationMs.toDouble() * sourceFractionDelta / speed)
        }
        durationMs = playbackTimesMs.last().roundToLong().coerceAtLeast(if (safeSourceDurationMs > 0L) 1L else 0L)
    }

    fun playbackTimeAtSourceFraction(sourceFraction: Float): Long {
        if (sourceDurationMs <= 0L) return 0L
        val target = sourceFraction.coerceIn(0f, 1f).toDouble()
        if (target <= 0.0) return 0L
        if (target >= 1.0) return durationMs
        val position = target * SPEED_CURVE_TIMELINE_SAMPLES
        val index = position.toInt().coerceIn(0, SPEED_CURVE_TIMELINE_SAMPLES - 1)
        val localFraction = position - index.toDouble()
        val time = playbackTimesMs[index] +
            (playbackTimesMs[index + 1] - playbackTimesMs[index]) * localFraction
        return time.roundToLong().coerceIn(0L, durationMs)
    }

    fun sourceFractionAtPlaybackTime(playbackTimeMs: Long): Float {
        if (sourceDurationMs <= 0L || durationMs <= 0L) return 0f
        val target = playbackTimeMs.coerceIn(0L, durationMs).toDouble()
        if (target <= 0.0) return 0f
        if (target >= durationMs.toDouble()) return 1f

        var low = 0
        var high = SPEED_CURVE_TIMELINE_SAMPLES
        while (high - low > 1) {
            val middle = (low + high) ushr 1
            if (playbackTimesMs[middle] <= target) low = middle else high = middle
        }
        val timeSpan = playbackTimesMs[high] - playbackTimesMs[low]
        val localFraction = if (timeSpan <= 0.0) 0.0 else {
            ((target - playbackTimesMs[low]) / timeSpan).coerceIn(0.0, 1.0)
        }
        return (fractions[low] + (fractions[high] - fractions[low]) * localFraction)
            .toFloat()
            .coerceIn(0f, 1f)
    }

    /** Creates bounded constant-speed windows, with boundaries at curve points. */
    fun sampledWindows(): List<SpeedCurveSegmentWindow> {
        if (sourceDurationMs <= 0L) return emptyList()
        val boundaryFractions = buildExportBoundaryFractions(safeCurve)
        return boundaryFractions.zipWithNext().mapNotNull { (startFraction, endFraction) ->
            val sourceStartMs = (sourceDurationMs.toDouble() * startFraction).roundToLong()
            val sourceEndMs = (sourceDurationMs.toDouble() * endFraction).roundToLong()
            if (sourceEndMs <= sourceStartMs) return@mapNotNull null
            val actualStartFraction = sourceStartMs.toFloat() / sourceDurationMs.toFloat()
            val actualEndFraction = sourceEndMs.toFloat() / sourceDurationMs.toFloat()
            SpeedCurveSegmentWindow(
                sourceStartMs = sourceStartMs,
                sourceEndMs = sourceEndMs,
                playbackStartMs = playbackTimeAtSourceFraction(actualStartFraction),
                playbackEndMs = playbackTimeAtSourceFraction(actualEndFraction)
            )
        }
    }
}

private fun buildExportBoundaryFractions(curve: SpeedCurve): List<Float> {
    val safePoints = curve.normalized().points
    val desiredSegmentCount = ((safePoints.size - 1) * 8)
        .coerceIn(SPEED_CURVE_EXPORT_MIN_SEGMENTS, SPEED_CURVE_EXPORT_MAX_SEGMENTS)
    val boundaries = mutableListOf(0f)
    safePoints.zipWithNext().forEach { (first, second) ->
        val intervalWidth = (second.x - first.x).coerceAtLeast(0f)
        val subdivisions = ceil(intervalWidth * desiredSegmentCount.toFloat())
            .toInt()
            .coerceAtLeast(1)
        for (index in 1..subdivisions) {
            boundaries += (first.x + intervalWidth * index / subdivisions).coerceIn(0f, 1f)
        }
    }
    val distinctBoundaries = boundaries.distinct().sorted()
    val boundedBoundaries = if (distinctBoundaries.size <= SPEED_CURVE_EXPORT_MAX_SEGMENTS + 1) {
        distinctBoundaries
    } else {
        (0..SPEED_CURVE_EXPORT_MAX_SEGMENTS).map { index ->
            distinctBoundaries[index * distinctBoundaries.lastIndex / SPEED_CURVE_EXPORT_MAX_SEGMENTS]
        }.distinct()
    }
    return boundedBoundaries.let { values ->
        if (values.lastOrNull() == 1f) values else values + 1f
    }
}

fun calculateDurationWithSpeedCurve(sourceDurationMs: Long, curve: SpeedCurve?): Long {
    if (curve == null || curve.points.isEmpty()) return sourceDurationMs.coerceAtLeast(0L)
    return SpeedCurveTimeline(sourceDurationMs.coerceAtLeast(0L), curve).durationMs
}

fun calculateDurationUpTo(
    partialSourceDurationMs: Long,
    totalSourceDurationMs: Long,
    curve: SpeedCurve?
): Long {
    val safeTotalDurationMs = totalSourceDurationMs.coerceAtLeast(0L)
    val safePartialDurationMs = partialSourceDurationMs.coerceIn(0L, safeTotalDurationMs)
    if (curve == null || curve.points.isEmpty() || safeTotalDurationMs <= 0L) {
        return safePartialDurationMs
    }
    val fraction = safePartialDurationMs.toFloat() / safeTotalDurationMs.toFloat()
    return SpeedCurveTimeline(safeTotalDurationMs, curve)
        .playbackTimeAtSourceFraction(fraction)
}

/** Maps playback time to source time using the same clock used for duration calculation. */
fun mapPlaybackTimeToOriginalFraction(
    playbackTimeMs: Long,
    originalDurationMs: Long,
    curve: SpeedCurve?
): Float {
    val safeDurationMs = originalDurationMs.coerceAtLeast(0L)
    if (safeDurationMs <= 0L) return 0f
    if (curve == null || curve.points.isEmpty()) {
        return (playbackTimeMs.toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f)
    }
    return SpeedCurveTimeline(safeDurationMs, curve).sourceFractionAtPlaybackTime(playbackTimeMs)
}

fun mapOriginalFractionToPlaybackTimeMs(
    sourceFraction: Float,
    originalDurationMs: Long,
    curve: SpeedCurve?
): Long {
    val safeDurationMs = originalDurationMs.coerceAtLeast(0L)
    if (safeDurationMs <= 0L) return 0L
    if (curve == null || curve.points.isEmpty()) {
        return (safeDurationMs.toDouble() * sourceFraction.coerceIn(0f, 1f)).roundToLong()
    }
    return SpeedCurveTimeline(safeDurationMs, curve)
        .playbackTimeAtSourceFraction(sourceFraction)
}
