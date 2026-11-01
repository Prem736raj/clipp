package com.example

import android.media.MediaCodecInfo
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.EncoderSelector
import kotlin.math.roundToInt

enum class ExportResolution(val label: String, val shortSide: Int, val recommendedBitrate: Int) {
    SD("480p", 480, 2_000_000),
    HD("720p", 720, 5_000_000),
    FULL_HD("1080p", 1080, 8_000_000),
    UHD("4K", 2160, 40_000_000)
}

data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.HD,
    val frameRate: Int = 30,
    val videoBitrate: Int = 5_000_000
) {
    internal fun validationError(): String? = when {
        frameRate !in EXPORT_FRAME_RATES -> "Choose a frame rate of 24, 30, or 60 fps."
        videoBitrate !in EXPORT_VIDEO_BITRATES -> "Choose one of the available video bitrates."
        else -> null
    }
}

internal val EXPORT_FRAME_RATES = listOf(24, 30, 60)
internal val EXPORT_VIDEO_BITRATES = listOf(1, 2, 3, 5, 8, 12, 20, 40, 60).map { it * 1_000_000 }
internal const val EXPORT_AUDIO_BITRATE = 128_000

internal data class ExportDimensions(val width: Int, val height: Int)

internal fun ExportSettings.dimensions(aspectRatio: Float): ExportDimensions {
    require(aspectRatio.isFinite() && aspectRatio > 0f)
    fun even(value: Float) = ((value / 2f).roundToInt() * 2).coerceAtLeast(2)
    val shortSide = resolution.shortSide
    return if (aspectRatio >= 1f) {
        ExportDimensions(even(shortSide * aspectRatio), shortSide)
    } else {
        ExportDimensions(shortSide, even(shortSide / aspectRatio))
    }
}

/** Target bitrate estimate, including audio and a small MP4 overhead allowance. */
internal fun ExportSettings.estimatedBytes(durationMs: Long, hasAudio: Boolean): Long {
    val bitsPerSecond = videoBitrate.toLong() + if (hasAudio) EXPORT_AUDIO_BITRATE else 0
    return (durationMs.coerceAtLeast(0L).toDouble() / 1_000.0 * bitsPerSecond / 8.0 * 1.03)
        .coerceIn(0.0, Long.MAX_VALUE.toDouble()).toLong()
}

internal data class ExportQualitySupport(
    val supportedSettings: Set<ExportSettings> = emptySet(),
    val error: String? = null
) {
    fun blockingReason(settings: ExportSettings): String? = settings.validationError() ?: when {
        error != null -> error
        settings !in supportedSettings ->
            "This device cannot export with these settings. Try a lower resolution, frame rate, or bitrate."
        else -> null
    }

    fun preferredSettings(
        resolution: ExportResolution,
        frameRate: Int,
        preferredBitrate: Int = resolution.recommendedBitrate
    ): ExportSettings? = supportedSettings
        .filter { it.resolution == resolution && it.frameRate == frameRate }
        .minByOrNull { kotlin.math.abs(it.videoBitrate.toLong() - preferredBitrate) }

    fun settingsForResolution(resolution: ExportResolution, preferredFrameRate: Int): ExportSettings? =
        preferredSettings(resolution, preferredFrameRate) ?: supportedSettings
            .filter { it.resolution == resolution }
            .minWithOrNull(compareBy<ExportSettings> { kotlin.math.abs(it.frameRate - preferredFrameRate) }
                .thenBy { kotlin.math.abs(it.videoBitrate - resolution.recommendedBitrate) })

    fun defaultSettings(): ExportSettings? = preferredSettings(ExportResolution.HD, 30)
        ?: supportedSettings.sortedWith(
            compareBy<ExportSettings> { it.resolution.shortSide }
                .thenBy { kotlin.math.abs(it.frameRate - 30) }
                .thenBy { kotlin.math.abs(it.videoBitrate - it.resolution.recommendedBitrate) }
        ).firstOrNull()
}

internal fun inspectExportQualitySupport(aspectRatio: Float): ExportQualitySupport = runCatching {
    val encoders = EncoderSelector.DEFAULT.selectEncoderInfos(MimeTypes.VIDEO_H264)
    val supported = buildSet {
        ExportResolution.entries.forEach { resolution ->
            EXPORT_FRAME_RATES.forEach { frameRate ->
                EXPORT_VIDEO_BITRATES.forEach { bitrate ->
                    val settings = ExportSettings(resolution, frameRate, bitrate)
                    val dimensions = settings.dimensions(aspectRatio)
                    if (encoders.any { it.supportsExportSettings(settings, dimensions) }) add(settings)
                }
            }
        }
    }
    ExportQualitySupport(supportedSettings = supported)
}.getOrElse {
    ExportQualitySupport(error = "Clipp could not check this device's export options. Close and reopen Export to try again.")
}

/** The same predicate is used for UI choices and the encoder actually selected at export. */
internal fun MediaCodecInfo.supportsExportSettings(
    settings: ExportSettings,
    dimensions: ExportDimensions
): Boolean = runCatching {
    val capabilities = getCapabilitiesForType(MimeTypes.VIDEO_H264)
    val video = capabilities.videoCapabilities ?: return@runCatching false
    val dimensionsSupported = video.areSizeAndRateSupported(
        dimensions.width, dimensions.height, settings.frameRate.toDouble()
    ) || video.areSizeAndRateSupported(
        dimensions.height, dimensions.width, settings.frameRate.toDouble()
    )
    dimensionsSupported && video.bitrateRange.contains(settings.videoBitrate) &&
        capabilities.encoderCapabilities?.isBitrateModeSupported(
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
        ) == true && capabilities.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
}.getOrDefault(false)
