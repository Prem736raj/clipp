package com.example

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

data class SpeedPoint(val x: Float, val y: Float)

data class SpeedCurve(
    val points: List<SpeedPoint> = listOf(SpeedPoint(0f, 1f), SpeedPoint(1f, 1f))
) {
    // Basic linear interpolation for a target X (0f to 1f)
    fun getSpeedAt(x: Float): Float {
        if (points.isEmpty()) return 1f
        if (points.size == 1) return points.first().y
        if (x <= points.first().x) return points.first().y
        if (x >= points.last().x) return points.last().y

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (x >= p1.x && x <= p2.x) {
                if (p2.x - p1.x == 0f) return p1.y
                val t = (x - p1.x) / (p2.x - p1.x)
                
                val p0 = if (i > 0) points[i - 1] else p1
                val p3 = if (i < points.size - 2) points[i + 2] else p2
                
                val t2 = t * t
                val t3 = t2 * t
                val result = 0.5f * ((2f * p1.y) +
                        (-p0.y + p2.y) * t +
                        (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                        (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3)
                
                return result.coerceIn(0.1f, 10f) // Keep within logical speed bounds
            }
        }
        return 1f
    }
}

fun calculateDurationWithSpeedCurve(sourceDurationMs: Long, curve: SpeedCurve?): Long {
    return calculateDurationUpTo(sourceDurationMs, sourceDurationMs, curve)
}

fun calculateDurationUpTo(partialSourceDurationMs: Long, totalSourceDurationMs: Long, curve: SpeedCurve?): Long {
    if (curve == null || curve.points.isEmpty()) return partialSourceDurationMs
    if (partialSourceDurationMs <= 0) return 0L
    
    val steps = 100
    val stepSize = 1f / steps
    val targetFraction = partialSourceDurationMs.toFloat() / totalSourceDurationMs.coerceAtLeast(1)
    var totalTimeCalculated = 0f
    
    val limitIndex = (targetFraction * steps).toInt()
    
    for (i in 0 until limitIndex) {
        val midX = (i + 0.5f) * stepSize
        val speedAtX = curve.getSpeedAt(midX).coerceAtLeast(0.1f)
        totalTimeCalculated += stepSize / speedAtX
    }
    
    // Add remainder
    val remainderFraction = targetFraction - limitIndex * stepSize
    if (remainderFraction > 0) {
        val speedAtX = curve.getSpeedAt(targetFraction).coerceAtLeast(0.1f)
        totalTimeCalculated += remainderFraction / speedAtX
    }
    
    return (totalSourceDurationMs * totalTimeCalculated).toLong()
}

// Maps a playback time (0 to durationWithSpeedCurve) back to the original media fraction (0 to 1) 
fun mapPlaybackTimeToOriginalFraction(playbackTimeMs: Long, originalDurationMs: Long, curve: SpeedCurve?): Float {
    if (curve == null || curve.points.isEmpty()) return playbackTimeMs.toFloat() / originalDurationMs.coerceAtLeast(1)
    
    val steps = 100
    val stepSize = 1f / steps
    var accumulatedPlaybackTime = 0f
    val targetPlaybackTime = playbackTimeMs.toFloat() / originalDurationMs
    
    for (i in 0 until steps) {
        val midX = (i + 0.5f) * stepSize
        val speedAtX = curve.getSpeedAt(midX).coerceAtLeast(0.1f)
        val deltaPlayTime = stepSize / speedAtX
        
        if (accumulatedPlaybackTime + deltaPlayTime >= targetPlaybackTime) {
            val remain = targetPlaybackTime - accumulatedPlaybackTime
            return (i * stepSize) + (remain / deltaPlayTime) * stepSize
        }
        accumulatedPlaybackTime += deltaPlayTime
    }
    return 1f
}
