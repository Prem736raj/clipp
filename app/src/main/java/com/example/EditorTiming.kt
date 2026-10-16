package com.example

enum class TransitionType(val label: String) {
    NONE("None"),
    CROSSFADE("Crossfade"),
    FADE_TO_BLACK("Fade to Black"),
    FADE_TO_WHITE("Fade to White"),
    SLIDE_LEFT("Slide Left"),
    SLIDE_RIGHT("Slide Right"),
    SLIDE_UP("Slide Up"),
    SLIDE_DOWN("Slide Down"),
    PUSH_LEFT("Push Left"),
    PUSH_RIGHT("Push Right"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    WIPE_LEFT("Wipe Left"),
    WIPE_RIGHT("Wipe Right"),
    CLOCK_WIPE("Clock Wipe"),
    SPIN("Spin"),
    FLIP("Flip")
}

data class Transition(
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = 500L,
    val easing: EasingType = EasingType.EASE_IN_OUT
)

enum class EasingType {
    LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, BOUNCE, ELASTIC
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class Keyframe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timeMs: Long,
    val value: Float,
    val easing: EasingType = EasingType.EASE_IN_OUT
)

fun applyEasing(t: Float, type: EasingType): Float {
    return when (type) {
        EasingType.LINEAR -> t
        EasingType.EASE_IN -> t * t
        EasingType.EASE_OUT -> t * (2f - t)
        EasingType.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
        EasingType.BOUNCE -> {
            var n1 = 7.5625f
            var d1 = 2.75f
            var time = t
            if (time < 1f / d1) {
                n1 * time * time
            } else if (time < 2f / d1) {
                time -= 1.5f / d1
                n1 * time * time + 0.75f
            } else if (time < 2.5f / d1) {
                time -= 2.25f / d1
                n1 * time * time + 0.9375f
            } else {
                time -= 2.625f / d1
                n1 * time * time + 0.984375f
            }
        }
        EasingType.ELASTIC -> {
            val c4 = (2f * Math.PI.toFloat()) / 3f
            if (t == 0f) 0f
            else if (t == 1f) 1f
            else Math.pow(2.0, (-10f * t).toDouble()).toFloat() * kotlin.math.sin((t * 10f - 0.75f) * c4) + 1f
        }
    }
}

fun Map<String, List<Keyframe>>.getValueAtTime(
    propName: String,
    timeMs: Long,
    defaultValue: Float
): Float {
    val kfs = this[propName] ?: return defaultValue
    if (kfs.isEmpty()) return defaultValue
    if (kfs.size == 1) return kfs.first().value

    val sorted = kfs.sortedBy { it.timeMs }
    val first = sorted.first()
    val last = sorted.last()

    if (timeMs <= first.timeMs) return first.value
    if (timeMs >= last.timeMs) return last.value

    val k1Idx = sorted.indexOfLast { it.timeMs <= timeMs }
    val k1 = sorted[k1Idx]
    val k2 = sorted[k1Idx + 1]

    val dt = k2.timeMs - k1.timeMs
    if (dt == 0L) return k1.value
    val t = (timeMs - k1.timeMs).toFloat() / dt
    val easedT = applyEasing(t, k1.easing)

    return k1.value + (k2.value - k1.value) * easedT
}
