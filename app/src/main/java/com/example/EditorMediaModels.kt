package com.example

import androidx.compose.ui.geometry.Rect

data class AudioEffects(
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val crossfadeMs: Long = 0L,
    val eqPreset: String = "Flat",
    val reverbPreset: String = "None",
    val delayTimeMs: Long = 0L,
    val delayFeedback: Float = 0f,
    val pitchSemitones: Float = 0f,
    val noiseReductionIntensity: Float = 0f,
    val isWindNoiseReduction: Boolean = false,
    val voicePreset: String = "None",
    val voiceEffectIntensity: Float = 1f,
    val distortion: Float = 0f,
    val speed: Float = 1f,
    val reverbAmount: Float = 0f
)

data class MediaClip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceUri: String,
    val originalDurationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long,
    val playbackSpeed: Float = 1f,
    val maintainPitch: Boolean = true,
    val rotation: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRect: Rect = Rect(0f, 0f, 1f, 1f),
    val scale: Float = 1f,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val isPhoto: Boolean = false,
    val transitionNext: Transition = Transition(),
    val filterType: FilterType = FilterType.NONE,
    val filterIntensity: Float = 1f,
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val effects: List<AppliedEffect> = emptyList(),
    val photoAnimationSettings: PhotoAnimationSettings = PhotoAnimationSettings(),
    val keyframes: Map<String, List<Keyframe>> = emptyMap(),
    val speedCurve: SpeedCurve? = null,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val audioEffects: AudioEffects = AudioEffects()
) {
    val safeOriginalDurationMs: Long get() = originalDurationMs.coerceAtLeast(0L)
    val effectiveTrimStartMs: Long
        get() = trimStartMs.coerceIn(0L, safeOriginalDurationMs)
    val effectiveTrimEndMs: Long
        get() = trimEndMs.coerceIn(effectiveTrimStartMs, safeOriginalDurationMs)

    fun normalized(): MediaClip = copy(
        originalDurationMs = safeOriginalDurationMs,
        trimStartMs = effectiveTrimStartMs,
        trimEndMs = effectiveTrimEndMs,
        playbackSpeed = playbackSpeed.coerceIn(0.1f, 10f),
        volume = volume.coerceIn(0f, 1f),
        transitionNext = transitionNext.normalized(),
        speedCurve = speedCurve?.normalized()
    )

    val durationMs: Long get() {
        val sourceDurationMs = effectiveTrimEndMs - effectiveTrimStartMs
        if (sourceDurationMs == 0L) return 0L
        val safeSpeed = playbackSpeed.coerceIn(0.1f, 10f)
        return if (speedCurve != null) {
            calculateDurationWithSpeedCurve(sourceDurationMs, speedCurve).coerceAtLeast(1L)
        } else {
            (sourceDurationMs / safeSpeed).toLong().coerceAtLeast(1L)
        }
    }
}

data class AudioClip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceClipId: String? = null,
    val sourceUri: String? = null,
    val displayName: String? = null,
    val startTimeOnTimelineMs: Long,
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val isLooped: Boolean = false,
    val keyframes: Map<String, List<Keyframe>> = emptyMap(),
    val autoDucking: Boolean = false,
    val audioEffects: AudioEffects = AudioEffects()
) {
    val durationMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

enum class AspectRatioOption(val label: String, val ratio: Float, val subtitle: String) {
    R_9_16("9:16", 9f / 16f, "TikTok/Shorts"),
    R_16_9("16:9", 16f / 9f, "YouTube/TV"),
    R_1_1("1:1", 1f / 1f, "Instagram Post"),
    R_4_5("4:5", 4f / 5f, "Instagram Feed"),
    R_4_3("4:3", 4f / 3f, "Classic"),
    R_21_9("21:9", 21f / 9f, "Cinematic")
}

enum class FitMode { Fit, Fill, Stretch }
enum class BackgroundType { Color, Blur, Gradient }

data class CanvasSettingsState(
    val aspectOption: AspectRatioOption = AspectRatioOption.R_16_9,
    val fitMode: FitMode = FitMode.Fit,
    val backgroundType: BackgroundType = BackgroundType.Blur,
    // Keep the serialized representation JVM-friendly. Compose exposes Color.value
    // as ULong, but Moshi's reflective adapter deliberately rejects Kotlin unsigned
    // platform types. The bit pattern is preserved when converting back to Color.
    val backgroundColorValue: Long = androidx.compose.ui.graphics.Color.Black.value.toLong(),
    val masterVolume: Float = 1.0f
) {
    val backgroundColor: androidx.compose.ui.graphics.Color
        get() = androidx.compose.ui.graphics.Color(backgroundColorValue.toULong())
}
