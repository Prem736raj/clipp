package com.example

internal const val MAX_EXPORT_CROSSFADE_MS = 5_000L
internal const val AUTO_DUCK_RAMP_MS = 160L
internal const val AUTO_DUCK_GAIN = 0.25f

internal data class AudioDuckingWindow(
    val startTimeMs: Long,
    val endTimeMs: Long
)

internal fun AudioClip.timelineDurationMs(projectDurationMs: Long): Long {
    val startMs = startTimeOnTimelineMs.coerceIn(0L, projectDurationMs.coerceAtLeast(0L))
    val remainingMs = (projectDurationMs - startMs).coerceAtLeast(0L)
    return if (isLooped) remainingMs else durationMs.coerceAtMost(remainingMs)
}

/**
 * Foreground audio windows used by both preview and export auto-ducking.
 * Primary clip audio and non-ducking audio tracks are treated as foreground.
 */
internal fun EditorState.buildAudioDuckingWindows(projectDurationMs: Long): List<AudioDuckingWindow> {
    if (projectDurationMs <= 0L) return emptyList()

    val windows = mutableListOf<AudioDuckingWindow>()
    var clipStartMs = 0L
    clips.forEach { clip ->
        val clipEndMs = (clipStartMs + clip.durationMs).coerceAtMost(projectDurationMs)
        if (!clip.isPhoto && !clip.isMuted && clip.volume > 0f && clipEndMs > clipStartMs) {
            windows += AudioDuckingWindow(clipStartMs, clipEndMs)
        }
        clipStartMs = clipEndMs
    }

    audioClips.asSequence()
        .filter { !it.autoDucking && !it.isMuted && it.volume > 0f }
        .forEach { audioClip ->
            val startMs = audioClip.startTimeOnTimelineMs.coerceIn(0L, projectDurationMs)
            val endMs = (startMs + audioClip.timelineDurationMs(projectDurationMs))
                .coerceAtMost(projectDurationMs)
            if (endMs > startMs) windows += AudioDuckingWindow(startMs, endMs)
        }

    return windows
        .sortedBy { it.startTimeMs }
        .fold(mutableListOf()) { merged, window ->
            val previous = merged.lastOrNull()
            if (previous != null && window.startTimeMs <= previous.endTimeMs) {
                merged[merged.lastIndex] = previous.copy(endTimeMs = maxOf(previous.endTimeMs, window.endTimeMs))
            } else {
                merged += window
            }
            merged
        }
}

internal fun AudioClip.crossfadeGainAt(
    projectTimeMs: Long,
    projectDurationMs: Long
): Float {
    val activeDurationMs = timelineDurationMs(projectDurationMs)
    if (activeDurationMs <= 0L) return 0f
    val localTimeMs = projectTimeMs - startTimeOnTimelineMs
    if (localTimeMs !in 0L until activeDurationMs) return 0f

    val crossfadeMs = audioEffects.crossfadeMs
        .coerceIn(0L, minOf(MAX_EXPORT_CROSSFADE_MS, activeDurationMs / 2L))
    if (crossfadeMs <= 0L) return 1f

    val fadeInGain = (localTimeMs.toFloat() / crossfadeMs.toFloat()).coerceIn(0f, 1f)
    val fadeOutGain = ((activeDurationMs - localTimeMs).toFloat() / crossfadeMs.toFloat()).coerceIn(0f, 1f)
    return minOf(fadeInGain, fadeOutGain).coerceIn(0f, 1f)
}

internal fun duckingGainAt(
    projectTimeMs: Long,
    windows: List<AudioDuckingWindow>,
    rampMs: Long = AUTO_DUCK_RAMP_MS,
    duckGain: Float = AUTO_DUCK_GAIN
): Float {
    if (windows.isEmpty()) return 1f
    val safeRampMs = rampMs.coerceAtLeast(0L)
    val safeDuckGain = duckGain.coerceIn(0f, 1f)
    var gain = 1f

    windows.forEach { window ->
        if (window.endTimeMs <= window.startTimeMs) return@forEach
        val windowGain = when {
            projectTimeMs in window.startTimeMs until window.endTimeMs -> safeDuckGain
            safeRampMs > 0L && projectTimeMs >= window.startTimeMs - safeRampMs && projectTimeMs < window.startTimeMs -> {
                val progress = (projectTimeMs - (window.startTimeMs - safeRampMs)).toFloat() / safeRampMs.toFloat()
                1f + (safeDuckGain - 1f) * progress.coerceIn(0f, 1f)
            }
            safeRampMs > 0L && projectTimeMs >= window.endTimeMs && projectTimeMs < window.endTimeMs + safeRampMs -> {
                val progress = (projectTimeMs - window.endTimeMs).toFloat() / safeRampMs.toFloat()
                safeDuckGain + (1f - safeDuckGain) * progress.coerceIn(0f, 1f)
            }
            else -> 1f
        }
        gain = minOf(gain, windowGain)
    }
    return gain.coerceIn(0f, 1f)
}

internal fun AudioClip.timelineMixGainAt(
    projectTimeMs: Long,
    projectDurationMs: Long,
    duckingWindows: List<AudioDuckingWindow>
): Float {
    val crossfadeGain = crossfadeGainAt(projectTimeMs, projectDurationMs)
    val duckGain = if (autoDucking) duckingGainAt(projectTimeMs, duckingWindows) else 1f
    return (crossfadeGain * duckGain).coerceIn(0f, 1f)
}
