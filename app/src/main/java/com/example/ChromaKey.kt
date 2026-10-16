package com.example

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.max
import kotlin.math.sqrt

/** Settings for removing a selected color from an image or video overlay frame. */
data class ChromaKeySettings(
    val enabled: Boolean = false,
    val keyColorArgb: Long = 0xff00ff00L,
    val similarity: Float = 0.28f,
    val smoothness: Float = 0.08f,
    val spillSuppression: Float = 0f
)

/**
 * Applies a soft chroma-key alpha matte. The work is intentionally shared by
 * preview and export so an overlay does not look opaque in one path and
 * transparent in the other.
 */
internal fun applyChromaKey(bitmap: Bitmap, settings: ChromaKeySettings): Bitmap {
    if (!settings.enabled) return bitmap

    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    for (y in 0 until result.height) {
        for (x in 0 until result.width) {
            result.setPixel(x, y, chromaKeyPixelArgb(result.getPixel(x, y), settings))
        }
    }
    return result
}

internal fun chromaKeyPixelArgb(argb: Int, settings: ChromaKeySettings): Int {
    if (!settings.enabled) return argb
    val key = settings.keyColorArgb.toInt()
    val keyRed = (key ushr 16) and 0xff
    val keyGreen = (key ushr 8) and 0xff
    val keyBlue = key and 0xff
    val red = (argb ushr 16) and 0xff
    val green = (argb ushr 8) and 0xff
    val blue = argb and 0xff
    val distance = sqrt(
        ((red - keyRed) * (red - keyRed) +
            (green - keyGreen) * (green - keyGreen) +
            (blue - keyBlue) * (blue - keyBlue)).toFloat()
    ) / 441.67294f
    val matte = ((distance - settings.similarity.coerceIn(0f, 1f)) /
        settings.smoothness.coerceIn(0.0001f, 1f)).coerceIn(0f, 1f)
    val alpha = (((argb ushr 24) and 0xff) * matte).toInt().coerceIn(0, 255)
    val spill = settings.spillSuppression.coerceIn(0f, 1f)
    val nonSpillGreen = if (spill > 0f) {
        val strongestOtherChannel = max(red, blue)
        (green.toFloat() - (green - strongestOtherChannel).coerceAtLeast(0) * spill).toInt()
    } else {
        green
    }
    return (alpha shl 24) or
        (red shl 16) or
        (nonSpillGreen.coerceIn(0, 255) shl 8) or
        blue
}

/** Coil preview transformation for decoded photo or video overlay frames. */
class ChromaKeyTransformation(
    private val settings: ChromaKeySettings
) : Transformation {
    override val cacheKey: String = "clipp-chroma-key-${settings.hashCode()}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap = applyChromaKey(input, settings)
}
