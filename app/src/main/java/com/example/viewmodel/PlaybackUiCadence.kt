package com.example.viewmodel

/**
 * Controls how often playback-derived Compose state is published to the UI.
 *
 * The ExoPlayers continue to run at their configured media rate. These values
 * only bound the frequency of state updates that can invalidate the editor
 * tree while a project is playing.
 */
object PlaybackUiCadence {
    const val IDLE_INTERVAL_MS = 200L
    const val BETTER_PERFORMANCE_INTERVAL_MS = 120L
    const val BALANCED_INTERVAL_MS = 80L
    const val BEST_QUALITY_INTERVAL_MS = 50L

    fun intervalMs(mode: PerformanceMode, isPlaying: Boolean): Long {
        if (!isPlaying) return IDLE_INTERVAL_MS

        return when (mode) {
            PerformanceMode.BETTER_PERFORMANCE -> BETTER_PERFORMANCE_INTERVAL_MS
            PerformanceMode.BALANCED -> BALANCED_INTERVAL_MS
            PerformanceMode.BEST_QUALITY -> BEST_QUALITY_INTERVAL_MS
        }
    }
}
