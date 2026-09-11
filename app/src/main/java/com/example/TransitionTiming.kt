package com.example

/**
 * Shared transition timing limits and easing. Keeping this outside the UI and
 * exporter prevents a transition from previewing with one duration while the
 * MP4 renderer uses another.
 */
internal const val MIN_TRANSITION_DURATION_MS = 300L
internal const val MAX_TRANSITION_DURATION_MS = 2_000L

internal fun Transition.normalized(): Transition = copy(
    durationMs = durationMs.coerceIn(
        MIN_TRANSITION_DURATION_MS,
        MAX_TRANSITION_DURATION_MS
    )
)

internal fun transitionDurationForClip(
    transition: Transition,
    clipDurationMs: Long
): Long = transition.normalized().durationMs.coerceIn(
    1L,
    clipDurationMs.coerceAtLeast(1L)
)

internal fun easedTransitionProgress(
    progress: Float,
    easing: EasingType
): Float = applyEasing(progress.coerceIn(0f, 1f), easing).coerceIn(0f, 1f)

/**
 * Returns a signed progress for a transition preview centered on the cut:
 * -1 at the start of the outgoing half, 0 at the cut, and 1 at the end of
 * the incoming half.
 */
internal fun signedTransitionProgress(
    progress: Float,
    easing: EasingType
): Float {
    val clamped = progress.coerceIn(-1f, 1f)
    return if (clamped < 0f) {
        easedTransitionProgress(clamped + 1f, easing) - 1f
    } else {
        easedTransitionProgress(clamped, easing)
    }
}
