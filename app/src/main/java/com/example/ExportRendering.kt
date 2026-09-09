package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.media3.common.Effect
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.GaussianBlur
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextureOverlay
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val EXPORT_DEFAULT_CROP = androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)
private val EXPORT_CROP_KEYFRAMES = setOf("cropLeft", "cropTop", "cropRight", "cropBottom")
private val EXPORT_SUPPORTED_CLIP_KEYFRAMES = setOf("posX", "posY", "scale", "rotation") + EXPORT_CROP_KEYFRAMES
private val EXPORT_SUPPORTED_OVERLAY_KEYFRAMES = setOf("posX", "posY", "scaleX", "scaleY", "rotation", "opacity")
internal val EXPORT_SUPPORTED_AUDIO_KEYFRAMES = setOf("volume")
private val EXPORT_SUPPORTED_EFFECTS = setOf(
    EffectType.GAUSSIAN_BLUR,
    EffectType.LETTERBOX,
    EffectType.MIRROR,
    EffectType.SHAKE,
    EffectType.COMIC_BOOK,
    EffectType.PENCIL_SKETCH,
    EffectType.POP_ART,
    EffectType.FILM_GRAIN,
    EffectType.ANAMORPHIC_FLARE,
    EffectType.SPARKLE,
    EffectType.LIGHT_LEAK,
    EffectType.LENS_FLARE,
    EffectType.BOKEH
) + EXPORT_SHADER_EFFECTS
private val EXPORT_TIMED_OVERLAY_EFFECTS = setOf(
    EffectType.FILM_GRAIN,
    EffectType.ANAMORPHIC_FLARE,
    EffectType.SPARKLE,
    EffectType.LIGHT_LEAK,
    EffectType.LENS_FLARE,
    EffectType.BOKEH
) + EXPORT_SHADER_EFFECTS
private val EXPORT_VIDEO_TRANSITIONS = setOf(
    TransitionType.CROSSFADE,
    TransitionType.SLIDE_LEFT,
    TransitionType.SLIDE_RIGHT,
    TransitionType.SLIDE_UP,
    TransitionType.SLIDE_DOWN,
    TransitionType.ZOOM_IN,
    TransitionType.ZOOM_OUT,
    TransitionType.SPIN,
    TransitionType.FLIP
)
private val EXPORT_PHOTO_TRANSITIONS = EXPORT_VIDEO_TRANSITIONS + setOf(
    TransitionType.WIPE_LEFT,
    TransitionType.WIPE_RIGHT,
    TransitionType.CLOCK_WIPE
)
private val EXPORT_SUPPORTED_TEXT_ANIM_IN = setOf(
    TextAnimIn.NONE,
    TextAnimIn.FADE_IN,
    TextAnimIn.SCALE_IN,
    TextAnimIn.ROTATE_IN
)
private val EXPORT_SUPPORTED_TEXT_ANIM_LOOP = setOf(
    TextAnimLoop.NONE,
    TextAnimLoop.PULSE,
    TextAnimLoop.WAVE,
    TextAnimLoop.SWING
)
private val EXPORT_SUPPORTED_TEXT_ANIM_OUT = setOf(
    TextAnimOut.NONE,
    TextAnimOut.FADE_OUT,
    TextAnimOut.SCALE_OUT
)

/** A small, deterministic overlay implementation used by the Media3 renderer. */
private class TimedBitmapOverlay(
    private val windows: List<BitmapWindow>,
    private val blankBitmap: Bitmap,
    private val settingsAt: (Long) -> OverlaySettings
) : BitmapOverlay() {
    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1_000L
        return windows.firstOrNull { timeMs >= it.startMs && timeMs < it.endMs }?.bitmap ?: blankBitmap
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return settingsAt(presentationTimeUs / 1_000L)
    }
}

/**
 * Renders a still-image transition over the tail of a photo clip. The next
 * photo is already the first frame after the cut, so this avoids duplicating
 * or shortening the timeline while still producing a real transition.
 */
private class PhotoTransitionBitmapOverlay(
    private val type: TransitionType,
    private val sourceBitmap: Bitmap,
    private val blankBitmap: Bitmap,
    private val windowStartMs: Long,
    private val windowEndMs: Long
) : BitmapOverlay() {
    private companion object {
        const val SAMPLE_INTERVAL_MS = 33L
    }

    private var cachedSampleTimeMs = Long.MIN_VALUE
    private var cachedBitmap: Bitmap? = null
    private var released = false

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val localTimeMs = presentationTimeUs / 1_000L
        if (localTimeMs < windowStartMs || localTimeMs >= windowEndMs) return blankBitmap
        if (type != TransitionType.WIPE_LEFT &&
            type != TransitionType.WIPE_RIGHT &&
            type != TransitionType.CLOCK_WIPE
        ) return sourceBitmap

        val sampleTimeMs = (localTimeMs / SAMPLE_INTERVAL_MS) * SAMPLE_INTERVAL_MS
        synchronized(this) {
            if (released) return blankBitmap
            if (sampleTimeMs == cachedSampleTimeMs) return cachedBitmap ?: blankBitmap
            val progress = transitionProgress(localTimeMs)
            val next = Bitmap.createBitmap(
                sourceBitmap.width.coerceAtLeast(1),
                sourceBitmap.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(next)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            canvas.save()
            when (type) {
                TransitionType.WIPE_LEFT -> canvas.clipRect(
                    0f,
                    0f,
                    sourceBitmap.width * progress,
                    sourceBitmap.height.toFloat()
                )
                TransitionType.WIPE_RIGHT -> canvas.clipRect(
                    sourceBitmap.width * (1f - progress),
                    0f,
                    sourceBitmap.width.toFloat(),
                    sourceBitmap.height.toFloat()
                )
                TransitionType.CLOCK_WIPE -> {
                    val centerX = sourceBitmap.width / 2f
                    val centerY = sourceBitmap.height / 2f
                    val radius = max(sourceBitmap.width, sourceBitmap.height).toFloat()
                    val path = Path().apply {
                        moveTo(centerX, centerY)
                        lineTo(centerX, centerY - radius)
                        arcTo(
                            RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                            -90f,
                            360f * progress,
                            false
                        )
                        close()
                    }
                    canvas.clipPath(path)
                }
                else -> Unit
            }
            canvas.drawBitmap(sourceBitmap, 0f, 0f, null)
            canvas.restore()
            cachedBitmap?.takeIf { !it.isRecycled }?.recycle()
            cachedBitmap = next
            cachedSampleTimeMs = sampleTimeMs
            return next
        }
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return transitionOverlaySettings(
            type = type,
            localTimeMs = presentationTimeUs / 1_000L,
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs
        )
    }

    override fun release() {
        synchronized(this) {
            if (released) return
            released = true
            cachedBitmap?.takeIf { !it.isRecycled && it !== sourceBitmap }?.recycle()
            cachedBitmap = null
            sourceBitmap.takeIf { !it.isRecycled }?.recycle()
        }
        super.release()
    }

    private fun transitionProgress(localTimeMs: Long): Float {
        return transitionProgress(localTimeMs, windowStartMs, windowEndMs)
    }
}

private fun transitionProgress(localTimeMs: Long, windowStartMs: Long, windowEndMs: Long): Float {
    val duration = (windowEndMs - windowStartMs).coerceAtLeast(1L)
    return ((localTimeMs - windowStartMs).toFloat() / duration).coerceIn(0f, 1f)
}

private fun transitionOverlaySettings(
    type: TransitionType,
    localTimeMs: Long,
    windowStartMs: Long,
    windowEndMs: Long
): OverlaySettings {
    val eased = easedProgress(transitionProgress(localTimeMs, windowStartMs, windowEndMs))
    return when (type) {
        TransitionType.CROSSFADE -> overlaySettings(0.5f, 0.5f, 1f, 1f, alpha = eased)
        TransitionType.SLIDE_LEFT -> overlaySettings(
            1.5f - eased, 0.5f, 1f, 1f, allowOffscreen = true
        )
        TransitionType.SLIDE_RIGHT -> overlaySettings(
            -0.5f + eased, 0.5f, 1f, 1f, allowOffscreen = true
        )
        TransitionType.SLIDE_UP -> overlaySettings(
            0.5f, 1.5f - eased, 1f, 1f, allowOffscreen = true
        )
        TransitionType.SLIDE_DOWN -> overlaySettings(
            0.5f, -0.5f + eased, 1f, 1f, allowOffscreen = true
        )
        TransitionType.ZOOM_IN -> overlaySettings(
            0.5f, 0.5f, 0.55f + 0.45f * eased, 0.55f + 0.45f * eased, alpha = eased
        )
        TransitionType.ZOOM_OUT -> overlaySettings(
            0.5f, 0.5f, 1.45f - 0.45f * eased, 1.45f - 0.45f * eased, alpha = eased
        )
        TransitionType.SPIN -> overlaySettings(
            0.5f, 0.5f, 1f, 1f, rotation = (1f - eased) * 180f, alpha = eased
        )
        TransitionType.FLIP -> overlaySettings(
            0.5f, 0.5f, eased.coerceAtLeast(0.02f), 1f, alpha = eased
        )
        else -> overlaySettings(0.5f, 0.5f, 1f, 1f)
    }
}

private data class BitmapWindow(
    val startMs: Long,
    val endMs: Long,
    val bitmap: Bitmap
)

/**
 * Samples a secondary video on the export timeline and exposes the decoded
 * frame as a Media3 texture overlay. The small time bucket keeps repeated
 * texture-size and bitmap requests from decoding the same frame twice.
 */
private class VideoFrameBitmapOverlay(
    context: Context,
    private val sourceUri: String,
    private val sourceTrimStartMs: Long,
    private val sourceTrimEndMs: Long,
    private val windowStartMs: Long,
    private val windowEndMs: Long,
    private val blankBitmap: Bitmap,
    private val chromaKey: ChromaKeySettings,
    private val settingsAt: (Long) -> OverlaySettings
) : BitmapOverlay() {
    private companion object {
        const val SAMPLE_INTERVAL_MS = 33L
        const val MAX_FRAME_DIMENSION = 1600
    }

    private val retriever = MediaMetadataRetriever().also {
        it.setDataSource(context, Uri.parse(sourceUri))
    }
    private var cachedSampleTimeMs = Long.MIN_VALUE
    private var cachedFrame: Bitmap? = null
    private var released = false

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val localTimeMs = presentationTimeUs / 1_000L
        if (localTimeMs < windowStartMs || localTimeMs >= windowEndMs) return blankBitmap

        val overlayRelativeMs = (localTimeMs - windowStartMs).coerceAtLeast(0L)
        val sourceDurationMs = (sourceTrimEndMs - sourceTrimStartMs).coerceAtLeast(1L)
        val sourceTimeMs = (sourceTrimStartMs + overlayRelativeMs).coerceIn(
            sourceTrimStartMs,
            sourceTrimStartMs + sourceDurationMs - 1L
        )
        val sampleTimeMs = (sourceTimeMs / SAMPLE_INTERVAL_MS) * SAMPLE_INTERVAL_MS

        synchronized(this) {
            if (released) return blankBitmap
            if (sampleTimeMs == cachedSampleTimeMs) return cachedFrame ?: blankBitmap

            val decoded = runCatching {
                retriever.getFrameAtTime(
                    sampleTimeMs * 1_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
            }.getOrNull()
            val prepared = decoded?.let(::prepareFrame)
            if (prepared != null) {
                cachedFrame?.takeIf { it !== prepared }?.recycle()
                cachedFrame = prepared
                cachedSampleTimeMs = sampleTimeMs
            }
            return cachedFrame ?: blankBitmap
        }
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return settingsAt(presentationTimeUs / 1_000L)
    }

    override fun release() {
        synchronized(this) {
            if (released) return
            released = true
            cachedFrame?.recycle()
            cachedFrame = null
            runCatching { retriever.release() }
        }
        super.release()
    }

    private fun prepareFrame(decoded: Bitmap): Bitmap {
        val scale = min(
            1f,
            MAX_FRAME_DIMENSION.toFloat() / max(decoded.width, decoded.height).toFloat()
        )
        val scaled = if (scale >= 1f) decoded else Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        ).also { decoded.recycle() }
        if (!chromaKey.enabled) return scaled
        return applyChromaKey(scaled, chromaKey).also { keyed ->
            if (keyed !== scaled) scaled.recycle()
        }
    }
}

/** Decodes an animated GIF frame on demand while keeping only one bitmap alive. */
private class GifFrameBitmapOverlay(
    context: Context,
    private val windowStartMs: Long,
    private val windowEndMs: Long,
    private val blankBitmap: Bitmap,
    private val settingsAt: (Long) -> OverlaySettings,
    sourceUri: String
) : BitmapOverlay() {
    private companion object {
        const val SAMPLE_INTERVAL_MS = 33L
        const val MAX_FRAME_DIMENSION = 1600
    }

    private val movie = context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
        Movie.decodeStream(input)
    } ?: error("GIF overlay could not be opened")
    private val movieWidth = movie.width().coerceAtLeast(1)
    private val movieHeight = movie.height().coerceAtLeast(1)
    private val movieDurationMs = movie.duration().takeIf { it > 0 } ?: 1_000
    private var cachedSampleTimeMs = Long.MIN_VALUE
    private var cachedBitmap: Bitmap? = null
    private var released = false

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val localTimeMs = presentationTimeUs / 1_000L
        if (localTimeMs < windowStartMs || localTimeMs >= windowEndMs) return blankBitmap
        val gifTimeMs = (localTimeMs - windowStartMs) % movieDurationMs
        val sampleTimeMs = (gifTimeMs / SAMPLE_INTERVAL_MS) * SAMPLE_INTERVAL_MS

        synchronized(this) {
            if (released) return blankBitmap
            if (sampleTimeMs == cachedSampleTimeMs) return cachedBitmap ?: blankBitmap

            val scale = min(1f, MAX_FRAME_DIMENSION.toFloat() / max(movieWidth, movieHeight).toFloat())
            val bitmap = Bitmap.createBitmap(
                (movieWidth * scale).toInt().coerceAtLeast(1),
                (movieHeight * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            movie.setTime(sampleTimeMs.toInt())
            canvas.scale(scale, scale)
            movie.draw(canvas, 0f, 0f)
            cachedBitmap?.recycle()
            cachedBitmap = bitmap
            cachedSampleTimeMs = sampleTimeMs
            return bitmap
        }
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return settingsAt(presentationTimeUs / 1_000L)
    }

    override fun release() {
        synchronized(this) {
            if (released) return
            released = true
            cachedBitmap?.recycle()
            cachedBitmap = null
        }
        super.release()
    }
}

/** Renders deterministic per-frame cinematic textures without changing the source track. */
private class ProceduralEffectBitmapOverlay(
    private val type: EffectType,
    private val intensity: Float,
    private val blankBitmap: Bitmap,
    private val windowStartMs: Long,
    private val windowEndMs: Long
) : BitmapOverlay() {
    private companion object {
        const val BITMAP_SIZE = 256
        const val SAMPLE_INTERVAL_MS = 33L
    }

    private var cachedSampleTimeMs = Long.MIN_VALUE
    private var cachedBitmap: Bitmap? = null
    private var released = false

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val localTimeMs = presentationTimeUs / 1_000L
        if (localTimeMs < windowStartMs || localTimeMs >= windowEndMs) return blankBitmap
        val sampleTimeMs = (localTimeMs / SAMPLE_INTERVAL_MS) * SAMPLE_INTERVAL_MS
        synchronized(this) {
            if (released) return blankBitmap
            if (sampleTimeMs == cachedSampleTimeMs) return cachedBitmap ?: blankBitmap
            val next = render(sampleTimeMs)
            cachedBitmap?.recycle()
            cachedBitmap = next
            cachedSampleTimeMs = sampleTimeMs
            return next
        }
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings(0.5f, 0.5f, 1f, 1f)
    }

    override fun release() {
        synchronized(this) {
            if (released) return
            released = true
            cachedBitmap?.recycle()
            cachedBitmap = null
        }
        super.release()
    }

    private fun render(sampleTimeMs: Long): Bitmap {
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val safeIntensity = intensity.coerceIn(0f, 1f)
        when (type) {
            EffectType.FILM_GRAIN -> {
                val random = Random(sampleTimeMs)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                repeat((2_500 * safeIntensity).toInt()) {
                    val shade = if (random.nextBoolean()) 255 else 0
                    paint.color = android.graphics.Color.argb(
                        (48f * safeIntensity).toInt().coerceIn(1, 48),
                        shade,
                        shade,
                        shade
                    )
                    val x = random.nextInt(BITMAP_SIZE).toFloat()
                    val y = random.nextInt(BITMAP_SIZE).toFloat()
                    canvas.drawRect(x, y, x + 2f, y + 2f, paint)
                }
            }
            EffectType.SPARKLE -> {
                val random = Random(sampleTimeMs + 17L)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(
                        (210f * safeIntensity).toInt().coerceIn(1, 210),
                        255,
                        255,
                        255
                    )
                }
                repeat((18 * safeIntensity).toInt()) {
                    val radius = (1.5f + random.nextFloat() * 4f) * safeIntensity.coerceAtLeast(0.2f)
                    canvas.drawCircle(
                        random.nextFloat() * BITMAP_SIZE,
                        random.nextFloat() * BITMAP_SIZE,
                        radius,
                        paint
                    )
                }
            }
            EffectType.ANAMORPHIC_FLARE -> {
                val moveY = BITMAP_SIZE * 0.4f + sin(sampleTimeMs / 1_000f) * BITMAP_SIZE * 0.1f
                val halfHeight = 10f.coerceAtLeast(2f * safeIntensity)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f,
                        moveY - halfHeight,
                        0f,
                        moveY + halfHeight,
                        intArrayOf(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.argb((200f * safeIntensity).toInt(), 50, 150, 255),
                            android.graphics.Color.TRANSPARENT
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, moveY - halfHeight, BITMAP_SIZE.toFloat(), moveY + halfHeight, paint)
            }
            EffectType.LIGHT_LEAK -> {
                val moveX = sin(sampleTimeMs / 1_000f) * BITMAP_SIZE * 0.5f
                val centerX = BITMAP_SIZE * 0.5f + moveX
                val centerY = BITMAP_SIZE * 0.2f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        centerX,
                        centerY,
                        BITMAP_SIZE * 0.8f,
                        android.graphics.Color.argb((150f * safeIntensity).toInt(), 255, 100, 50),
                        android.graphics.Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, BITMAP_SIZE.toFloat(), BITMAP_SIZE.toFloat(), paint)
            }
            EffectType.LENS_FLARE -> {
                val timeSeconds = sampleTimeMs / 1_000f
                val posX = BITMAP_SIZE * (0.5f + 0.3f * sin(timeSeconds))
                val posY = BITMAP_SIZE * (0.5f + 0.3f * kotlin.math.cos(timeSeconds))
                val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb((128f * safeIntensity).toInt(), 255, 255, 255)
                }
                val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb((100f * safeIntensity).toInt(), 100, 200, 255)
                }
                canvas.drawCircle(posX, posY, 50f, whitePaint)
                canvas.drawCircle(BITMAP_SIZE - posX, BITMAP_SIZE - posY, 150f, bluePaint)
            }
            EffectType.BOKEH -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb((50f * safeIntensity).toInt(), 255, 200, 100)
                }
                repeat(6) { index ->
                    val px = (sin(sampleTimeMs / 1_000f * 0.5f + index) + 1f) / 2f * BITMAP_SIZE
                    val py = (kotlin.math.cos(sampleTimeMs / 1_000f * 0.4f + index * 2) + 1f) / 2f * BITMAP_SIZE
                    canvas.drawCircle(px, py, 60f + index * 10f, paint)
                }
            }
            else -> Unit
        }
        return bitmap
    }
}

private class ExportRgbMatrix(private val matrix: FloatArray) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix.copyOf()
}

/**
 * Applies the same position, scale, rotation and flip interpolation used by
 * the editor preview to every exported frame. MatrixTransformation operates
 * in Media3's normalized device coordinates, so a project position of 0.5 is
 * the centre and values outside the frame are deliberately clipped.
 */
private class KeyframedClipTransformation(
    private val clip: MediaClip,
    private val animateCrop: Boolean
) : MatrixTransformation {
    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val relativeTimeMs = presentationTimeUs / 1_000L
        val cropLeft = clip.keyframes
            .getValueAtTime("cropLeft", relativeTimeMs, clip.cropRect.left)
            .coerceIn(0f, 1f)
        val cropTop = clip.keyframes
            .getValueAtTime("cropTop", relativeTimeMs, clip.cropRect.top)
            .coerceIn(0f, 1f)
        val cropRight = clip.keyframes
            .getValueAtTime("cropRight", relativeTimeMs, clip.cropRect.right)
            .coerceIn(0f, 1f)
        val cropBottom = clip.keyframes
            .getValueAtTime("cropBottom", relativeTimeMs, clip.cropRect.bottom)
            .coerceIn(0f, 1f)
        val cropWidth = (cropRight - cropLeft).coerceAtLeast(0.01f)
        val cropHeight = (cropBottom - cropTop).coerceAtLeast(0.01f)
        val cropCenterX = (cropLeft + cropRight) / 2f
        val cropCenterY = (cropTop + cropBottom) / 2f
        val scale = clip.keyframes
            .getValueAtTime("scale", relativeTimeMs, clip.scale)
            .coerceIn(0.05f, 10f)
        val rotation = clip.keyframes
            .getValueAtTime("rotation", relativeTimeMs, clip.rotation)
        val posX = clip.keyframes
            .getValueAtTime("posX", relativeTimeMs, clip.posX)
        val posY = clip.keyframes
            .getValueAtTime("posY", relativeTimeMs, clip.posY)
        return Matrix().apply {
            if (animateCrop) {
                // Expand the selected normalized source rectangle to the full
                // output frame. The translation is divided by the crop scale
                // because it is appended after postScale().
                postScale(1f / cropWidth, 1f / cropHeight)
                postTranslate(
                    ((0.5f - cropCenterX) * 2f) / cropWidth,
                    ((cropCenterY - 0.5f) * 2f) / cropHeight
                )
            }
            postScale(
                (if (clip.flipHorizontal) -1f else 1f) * scale,
                (if (clip.flipVertical) -1f else 1f) * scale
            )
            postRotate(rotation)
            postTranslate((posX - 0.5f) * 2f, (0.5f - posY) * 2f)
        }
    }
}

private class ShakeTransformation(
    private val intensity: Float
) : MatrixTransformation {
    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val timeSeconds = presentationTimeUs / 1_000_000f
        val amplitude = 0.025f * intensity.coerceIn(0f, 1f)
        return Matrix().apply {
            postTranslate(
                sin(timeSeconds * 31f) * amplitude,
                sin(timeSeconds * 43f + 0.7f) * amplitude
            )
        }
    }
}

private data class LayerMotion(
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f
)

private fun easedProgress(value: Float): Float = sin(value.coerceIn(0f, 1f) * PI.toFloat() / 2f)

private fun textLayerMotion(
    relativeTimeMs: Long,
    durationMs: Long,
    animIn: TextAnimIn,
    animInDurationMs: Long,
    animInDelayMs: Long,
    animLoop: TextAnimLoop,
    animLoopDurationMs: Long,
    animLoopDelayMs: Long,
    animOut: TextAnimOut,
    animOutDurationMs: Long,
    animOutDelayMs: Long
): LayerMotion {
    var alpha = 1f
    var scale = 1f
    var rotation = 0f
    val inDelay = animInDelayMs.coerceAtLeast(0L)
    val inDuration = animInDurationMs.coerceAtLeast(1L)

    if (animIn != TextAnimIn.NONE) {
        if (relativeTimeMs < inDelay) return LayerMotion(alpha = 0f)
        if (relativeTimeMs < inDelay + inDuration) {
            val progress = (relativeTimeMs - inDelay).toFloat() / inDuration
            val ease = easedProgress(progress)
            when (animIn) {
                TextAnimIn.FADE_IN -> alpha = ease
                TextAnimIn.SCALE_IN -> {
                    alpha = ease
                    scale *= ease.coerceAtLeast(0.001f)
                }
                TextAnimIn.ROTATE_IN -> {
                    alpha = ease
                    scale *= ease.coerceAtLeast(0.001f)
                    rotation -= (1f - ease) * 180f
                }
                else -> Unit
            }
        }
    }

    val exitStart = (durationMs - animOutDurationMs.coerceAtLeast(0L) - animOutDelayMs.coerceAtLeast(0L)).coerceAtLeast(0L)
    val loopStartsAt = inDelay + inDuration + animLoopDelayMs.coerceAtLeast(0L)
    if (relativeTimeMs >= loopStartsAt &&
        (animOut == TextAnimOut.NONE || relativeTimeMs < exitStart) &&
        animLoopDurationMs > 0L
    ) {
        val loopProgress = ((relativeTimeMs - loopStartsAt) % animLoopDurationMs).toFloat() / animLoopDurationMs
        when (animLoop) {
            TextAnimLoop.PULSE -> scale *= 1f + 0.1f * sin(loopProgress * PI.toFloat() * 2f)
            TextAnimLoop.WAVE -> rotation += 5f * sin(loopProgress * PI.toFloat() * 2f)
            TextAnimLoop.SWING -> rotation += 15f * sin(loopProgress * PI.toFloat() * 2f)
            else -> Unit
        }
    }

    if (animOut != TextAnimOut.NONE && relativeTimeMs >= exitStart) {
        val outDelay = animOutDelayMs.coerceAtLeast(0L)
        if (relativeTimeMs < exitStart + outDelay) {
            return LayerMotion(alpha = alpha.coerceIn(0f, 1f), scale = scale, rotationDegrees = rotation)
        }
        val outDuration = animOutDurationMs.coerceAtLeast(1L)
        val progress = ((relativeTimeMs - exitStart - outDelay).toFloat() / outDuration).coerceIn(0f, 1f)
        val inverseEase = 1f - easedProgress(progress)
        when (animOut) {
            TextAnimOut.FADE_OUT -> alpha *= inverseEase
            TextAnimOut.SCALE_OUT -> {
                alpha *= inverseEase
                scale *= inverseEase.coerceAtLeast(0.001f)
            }
            else -> Unit
        }
    }

    return LayerMotion(alpha = alpha.coerceIn(0f, 1f), scale = scale, rotationDegrees = rotation)
}

private fun overlaySettings(
    posX: Float,
    posY: Float,
    scaleX: Float,
    scaleY: Float,
    rotation: Float = 0f,
    alpha: Float = 1f,
    allowOffscreen: Boolean = false
): OverlaySettings {
    // Media3 uses a centred coordinate system with positive Y upwards.
    val frameAnchorX = if (allowOffscreen) {
        posX * 2f - 1f
    } else {
        (posX.coerceIn(0f, 1f) * 2f) - 1f
    }
    val frameAnchorY = if (allowOffscreen) {
        1f - posY * 2f
    } else {
        1f - (posY.coerceIn(0f, 1f) * 2f)
    }
    return OverlaySettings.Builder()
        .setAlphaScale(alpha.coerceIn(0f, 1f))
        .setBackgroundFrameAnchor(
            frameAnchorX,
            frameAnchorY
        )
        .setOverlayFrameAnchor(0f, 0f)
        .setScale(scaleX.coerceAtLeast(0.001f), scaleY.coerceAtLeast(0.001f))
        .setRotationDegrees(rotation)
        .build()
}

private fun toMedia3ColorMatrix(matrix: ColorMatrix): FloatArray {
    val values = matrix.values
    return floatArrayOf(
        values[0], values[1], values[2], values[3],
        values[5], values[6], values[7], values[8],
        values[10], values[11], values[12], values[13],
        values[15], values[16], values[17], values[18]
    )
}

private fun buildExportEffectColorMatrix(effect: AppliedEffect): FloatArray? {
    val intensity = effect.intensity.coerceIn(0f, 1f)
    val matrix = ColorMatrix()
    when (effect.type) {
        EffectType.COMIC_BOOK -> matrix.setToSaturation(1f + 1.5f * intensity)
        EffectType.PENCIL_SKETCH -> matrix.setToSaturation(1f - intensity)
        EffectType.POP_ART -> matrix.setToSaturation(1f + 2f * intensity)
        else -> return null
    }
    return toMedia3ColorMatrix(matrix)
}

private fun renderTextBitmap(
    context: Context,
    text: String,
    fontName: String,
    fontSize: Float,
    textColor: Int,
    backgroundColor: Int,
    strokeColor: Int,
    strokeWidth: Float,
    shadowColor: Int,
    shadowOffsetX: Float,
    shadowOffsetY: Float,
    shadowBlur: Float,
    isBold: Boolean,
    isItalic: Boolean,
    alignment: TextAlignmentType,
    lineHeightMultiplier: Float
): Bitmap {
    val density = context.resources.displayMetrics.density
    val padding = (24f * density).coerceAtLeast(12f)
    val maxWidth = (context.resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtLeast(320)
    val typefaceStyle = when {
        isBold && isItalic -> Typeface.BOLD_ITALIC
        isBold -> Typeface.BOLD
        isItalic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        this.textSize = (fontSize.coerceIn(10f, 240f) * density).coerceAtLeast(10f)
        this.typeface = Typeface.create(
            when {
                fontName.contains("serif", ignoreCase = true) -> Typeface.SERIF
                fontName.contains("mono", ignoreCase = true) -> Typeface.MONOSPACE
                else -> Typeface.SANS_SERIF
            },
            typefaceStyle
        )
        this.color = textColor
        this.isDither = true
    }
    val layoutAlignment = when (alignment) {
        TextAlignmentType.Left -> Layout.Alignment.ALIGN_NORMAL
        TextAlignmentType.Right -> Layout.Alignment.ALIGN_OPPOSITE
        TextAlignmentType.Center -> Layout.Alignment.ALIGN_CENTER
    }
    val layoutWidth = maxWidth - (padding * 2f).toInt()
    val layout = StaticLayout.Builder
        .obtain(text.ifBlank { " " }, 0, text.ifBlank { " " }.length, textPaint, layoutWidth)
        .setAlignment(layoutAlignment)
        .setIncludePad(true)
        .setLineSpacing(0f, lineHeightMultiplier.coerceIn(0.75f, 2f))
        .build()
    val width = max(1, min(maxWidth, layout.width + (padding * 2f).toInt()))
    val height = max(1, layout.height + (padding * 2f).toInt())
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    if (backgroundColor ushr 24 > 0) {
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            18f * density,
            18f * density,
            backgroundPaint
        )
    }

    canvas.save()
    canvas.translate(padding, padding)
    if (shadowColor ushr 24 > 0 && shadowBlur > 0f) {
        textPaint.setShadowLayer(shadowBlur * density, shadowOffsetX * density, shadowOffsetY * density, shadowColor)
    }
    if (strokeColor ushr 24 > 0 && strokeWidth > 0f) {
        textPaint.style = Paint.Style.STROKE
        textPaint.strokeWidth = strokeWidth * density
        textPaint.color = strokeColor
        layout.draw(canvas)
    }
    textPaint.style = Paint.Style.FILL
    textPaint.color = textColor
    layout.draw(canvas)
    textPaint.clearShadowLayer()
    canvas.restore()
    return bitmap
}

private fun renderStickerBitmap(context: Context, sticker: StickerOverlay): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (360f * density).toInt().coerceIn(180, 720)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = sticker.color.toArgb()
        textAlign = Paint.Align.CENTER
        textSize = size * 0.62f
        typeface = Typeface.DEFAULT
    }
    if (sticker.backgroundColor.toArgb() ushr 24 > 0) {
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sticker.backgroundColor.toArgb() }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.46f, backgroundPaint)
    }
    canvas.drawText(sticker.content.ifBlank { "★" }, size / 2f, size * 0.68f, paint)
    return bitmap
}

private fun renderDrawingBitmap(drawings: List<DrawOverlay>): Bitmap {
    val size = 1024
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    for (drawing in drawings) {
        if (!drawing.isVisible) continue
        for (stroke in drawing.strokes) {
            if (stroke.path.isEmpty()) continue
            val path = Path()
            path.moveTo(stroke.path.first().x * size, stroke.path.first().y * size)
            stroke.path.drop(1).forEach { point -> path.lineTo(point.x * size, point.y * size) }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = (stroke.width * size).coerceIn(1f, size.toFloat())
                color = stroke.color.toArgb()
                alpha = (stroke.color.alpha * drawing.opacity.coerceIn(0f, 1f) * 255f).toInt()
                if (stroke.brushType == BrushType.MARKER) alpha = (alpha * 0.55f).toInt()
                if (stroke.brushType == BrushType.NEON) {
                    setShadowLayer(stroke.width * size * 2f, 0f, 0f, color)
                }
                if (stroke.isEraser) {
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                }
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}

private fun renderFrameBitmap(frame: FrameOverlay): Bitmap {
    val size = 1024
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = frame.color.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = (frame.thickness.coerceIn(0.005f, 0.25f) * size).coerceAtLeast(2f)
        alpha = (frame.opacity.coerceIn(0f, 1f) * 255f).toInt()
    }
    val inset = paint.strokeWidth / 2f
    val radius = (frame.cornerRadius.coerceIn(0f, 0.5f) * size)
    if (frame.typeId == "clean_rounded" || radius > 0f) {
        canvas.drawRoundRect(RectF(inset, inset, size - inset, size - inset), radius, radius, paint)
    } else {
        canvas.drawRect(inset, inset, size - inset, size - inset, paint)
    }
    return bitmap
}

private fun renderSolidBitmap(color: Int): Bitmap {
    return Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888).also { bitmap ->
        Canvas(bitmap).drawColor(color)
    }
}

private fun renderLetterboxBitmap(intensity: Float): Bitmap {
    val size = 1024
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val barHeight = size * 0.15f * intensity.coerceIn(0f, 1f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
    canvas.drawRect(0f, 0f, size.toFloat(), barHeight, paint)
    canvas.drawRect(0f, size - barHeight, size.toFloat(), size.toFloat(), paint)
    return bitmap
}

private fun loadBitmap(context: Context, uri: String): Bitmap? {
    return runCatching {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()?.let { source ->
        val maxDimension = 1600
        val scale = min(1f, maxDimension.toFloat() / max(source.width, source.height).toFloat())
        if (scale >= 1f) source else Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        ).also { if (it !== source) source.recycle() }
    }
}

private fun loadVideoFrame(context: Context, uri: String, timeMs: Long): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return runCatching {
        retriever.setDataSource(context, Uri.parse(uri))
        retriever.getFrameAtTime(timeMs.coerceAtLeast(0L) * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    }.getOrNull().also { runCatching { retriever.release() } }
}

private fun renderOverlayClipBitmap(context: Context, overlay: OverlayClip): Bitmap? {
    val bitmap = if (overlay.isPhoto) loadBitmap(context, overlay.sourceUri)
    else loadVideoFrame(context, overlay.sourceUri, overlay.trimStartMs)
    if (bitmap == null || !overlay.chromaKey.enabled) return bitmap
    return applyChromaKey(bitmap, overlay.chromaKey).also { keyed ->
        if (keyed !== bitmap) bitmap.recycle()
    }
}

private fun buildTextSettings(text: TextOverlay, globalTimeMs: Long): OverlaySettings {
    val relative = (globalTimeMs - text.startTimeOnTimelineMs).coerceAtLeast(0L)
    val motion = textLayerMotion(
        relativeTimeMs = relative,
        durationMs = text.durationMs,
        animIn = text.animIn,
        animInDurationMs = text.animInDurationMs,
        animInDelayMs = text.animInDelayMs,
        animLoop = text.animLoop,
        animLoopDurationMs = text.animLoopDurationMs,
        animLoopDelayMs = text.animLoopDelayMs,
        animOut = text.animOut,
        animOutDurationMs = text.animOutDurationMs,
        animOutDelayMs = text.animOutDelayMs
    )
    return overlaySettings(
        posX = text.keyframes.getValueAtTime("posX", relative, text.posX),
        posY = text.keyframes.getValueAtTime("posY", relative, text.posY),
        scaleX = 0.55f * text.keyframes.getValueAtTime("scale", relative, text.scale) * motion.scale,
        scaleY = 0.55f * text.keyframes.getValueAtTime("scale", relative, text.scale) * motion.scale,
        rotation = text.keyframes.getValueAtTime("rotation", relative, text.rotation) + motion.rotationDegrees,
        alpha = text.keyframes.getValueAtTime("opacity", relative, text.opacity) * motion.alpha
    )
}

private fun buildStickerSettings(sticker: StickerOverlay, globalTimeMs: Long): OverlaySettings {
    val motion = textLayerMotion(
        relativeTimeMs = (globalTimeMs - sticker.startTimeOnTimelineMs).coerceAtLeast(0L),
        durationMs = sticker.durationMs,
        animIn = sticker.animIn,
        animInDurationMs = sticker.animInDurationMs,
        animInDelayMs = sticker.animInDelayMs,
        animLoop = sticker.animLoop,
        animLoopDurationMs = sticker.animLoopDurationMs,
        animLoopDelayMs = sticker.animLoopDelayMs,
        animOut = sticker.animOut,
        animOutDurationMs = sticker.animOutDurationMs,
        animOutDelayMs = sticker.animOutDelayMs
    )
    return overlaySettings(
        sticker.posX,
        sticker.posY,
        0.28f * sticker.scale * motion.scale,
        0.28f * sticker.scale * motion.scale,
        sticker.rotation + motion.rotationDegrees,
        sticker.opacity * motion.alpha
    )
}

private fun buildOverlaySettings(overlay: OverlayClip, globalTimeMs: Long): OverlaySettings {
    val relative = (globalTimeMs - overlay.startTimeOnTimelineMs).coerceAtLeast(0L)
    val entranceDurationMs = 500L
    val exitDurationMs = 500L
    var alpha = 1f
    var scale = 1f
    if (overlay.entranceAnim != OverlayAnim.NONE && relative < entranceDurationMs) {
        val progress = easedProgress(relative.toFloat() / entranceDurationMs)
        when (overlay.entranceAnim) {
            OverlayAnim.FADE -> alpha *= progress
            OverlayAnim.SCALE -> {
                alpha *= progress
                scale *= progress.coerceAtLeast(0.001f)
            }
            else -> Unit
        }
    }
    val exitStartMs = (overlay.durationMs - exitDurationMs).coerceAtLeast(0L)
    if (overlay.exitAnim != OverlayAnim.NONE && relative >= exitStartMs) {
        val progress = ((relative - exitStartMs).toFloat() / exitDurationMs).coerceIn(0f, 1f)
        val inverseEase = 1f - easedProgress(progress)
        when (overlay.exitAnim) {
            OverlayAnim.FADE -> alpha *= inverseEase
            OverlayAnim.SCALE -> {
                alpha *= inverseEase
                scale *= inverseEase.coerceAtLeast(0.001f)
            }
            else -> Unit
        }
    }
    return overlaySettings(
        posX = overlay.keyframes.getValueAtTime("posX", relative, overlay.posX),
        posY = overlay.keyframes.getValueAtTime("posY", relative, overlay.posY),
        scaleX = overlay.keyframes.getValueAtTime("scaleX", relative, overlay.scaleX) * scale,
        scaleY = overlay.keyframes.getValueAtTime("scaleY", relative, overlay.scaleY) * scale,
        rotation = overlay.keyframes.getValueAtTime("rotation", relative, overlay.rotation),
        alpha = overlay.keyframes.getValueAtTime("opacity", relative, overlay.opacity) * alpha
    )
}

internal fun buildExportOverlays(
    context: Context,
    state: EditorState,
    clipStartMs: Long,
    clipDurationMs: Long,
    clip: MediaClip
): List<TextureOverlay> {
    if (clipDurationMs <= 0L) return emptyList()
    val blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val overlays = mutableListOf<TextureOverlay>()

    fun addWindowed(
        bitmap: Bitmap,
        startGlobalMs: Long,
        endGlobalMs: Long,
        settingsAt: (Long) -> OverlaySettings
    ) {
        val start = max(0L, startGlobalMs - clipStartMs)
        val end = min(clipDurationMs, endGlobalMs - clipStartMs)
        if (end > start) overlays += TimedBitmapOverlay(
            listOf(BitmapWindow(start, end, bitmap)),
            blank,
            settingsAt
        )
    }

    state.overlays.filter { it.isVisible && it.durationMs > 0L }.forEach { overlay ->
        val start = max(0L, overlay.startTimeOnTimelineMs - clipStartMs)
        val end = min(clipDurationMs, overlay.startTimeOnTimelineMs + overlay.durationMs - clipStartMs)
        if (end <= start) return@forEach

        if (overlay.isGif) {
            runCatching {
                GifFrameBitmapOverlay(
                    context = context,
                    windowStartMs = start,
                    windowEndMs = end,
                    blankBitmap = blank,
                    settingsAt = { localTime -> buildOverlaySettings(overlay, clipStartMs + localTime) },
                    sourceUri = overlay.sourceUri
                )
            }.onSuccess { overlays += it }
        } else if (overlay.isPhoto) {
            val bitmap = renderOverlayClipBitmap(context, overlay) ?: return@forEach
            addWindowed(
                bitmap,
                overlay.startTimeOnTimelineMs,
                overlay.startTimeOnTimelineMs + overlay.durationMs
            ) { localTime -> buildOverlaySettings(overlay, clipStartMs + localTime) }
        } else {
            runCatching {
                VideoFrameBitmapOverlay(
                    context = context,
                    sourceUri = overlay.sourceUri,
                    sourceTrimStartMs = overlay.trimStartMs,
                    sourceTrimEndMs = overlay.trimEndMs,
                    windowStartMs = start,
                    windowEndMs = end,
                    blankBitmap = blank,
                    chromaKey = overlay.chromaKey,
                    settingsAt = { localTime -> buildOverlaySettings(overlay, clipStartMs + localTime) }
                )
            }.onSuccess { overlays += it }
        }
    }

    state.drawings.filter { it.isVisible && it.strokes.isNotEmpty() }.forEach { drawing ->
        addWindowed(
            renderDrawingBitmap(listOf(drawing)),
            drawing.startTimeOnTimelineMs,
            drawing.startTimeOnTimelineMs + drawing.durationMs
        ) { _ -> overlaySettings(0.5f, 0.5f, 1f, 1f, alpha = drawing.opacity) }
    }

    state.frames.filter { it.isVisible }.forEach { frame ->
        addWindowed(
            renderFrameBitmap(frame),
            frame.startTimeOnTimelineMs,
            frame.startTimeOnTimelineMs + frame.durationMs
        ) { _ -> overlaySettings(0.5f, 0.5f, 1f, 1f, alpha = frame.opacity) }
    }

    state.texts.filter { it.isVisible && it.text.isNotBlank() }.forEach { text ->
        val bitmap = renderTextBitmap(
            context = context,
            text = text.text,
            fontName = text.fontName,
            fontSize = text.fontSize,
            textColor = text.textColor.toArgb(),
            backgroundColor = text.backgroundColor.toArgb(),
            strokeColor = text.strokeColor.toArgb(),
            strokeWidth = text.strokeWidth,
            shadowColor = text.shadowColor.toArgb(),
            shadowOffsetX = text.shadowOffsetX,
            shadowOffsetY = text.shadowOffsetY,
            shadowBlur = text.shadowBlur,
            isBold = text.isBold,
            isItalic = text.isItalic,
            alignment = text.alignment,
            lineHeightMultiplier = text.lineHeightMultiplier
        )
        addWindowed(bitmap, text.startTimeOnTimelineMs, text.startTimeOnTimelineMs + text.durationMs) { localTime ->
            buildTextSettings(text, clipStartMs + localTime)
        }
    }

    state.stickers.filter { it.isVisible }.forEach { sticker ->
        addWindowed(
            renderStickerBitmap(context, sticker),
            sticker.startTimeOnTimelineMs,
            sticker.startTimeOnTimelineMs + sticker.durationMs
        ) { localTime -> buildStickerSettings(sticker, clipStartMs + localTime) }
    }

    state.captions.forEach { caption ->
        val bitmap = renderTextBitmap(
            context = context,
            text = caption.text,
            fontName = state.captionSettings.fontName,
            fontSize = state.captionSettings.fontSize,
            textColor = state.captionSettings.textColor.toArgb(),
            backgroundColor = state.captionSettings.backgroundColor.toArgb(),
            strokeColor = state.captionSettings.strokeColor.toArgb(),
            strokeWidth = state.captionSettings.strokeWidth,
            shadowColor = android.graphics.Color.TRANSPARENT,
            shadowOffsetX = 0f,
            shadowOffsetY = 0f,
            shadowBlur = 0f,
            isBold = true,
            isItalic = false,
            alignment = state.captionSettings.alignment,
            lineHeightMultiplier = 1f
        )
        addWindowed(bitmap, caption.startTimeMs, caption.startTimeMs + caption.durationMs) { _ ->
            overlaySettings(state.captionSettings.posX, state.captionSettings.posY, 0.55f, 0.55f)
        }
    }

    clip.effects.filter { it.type == EffectType.LETTERBOX }.forEach { effect ->
        addWindowed(
            renderLetterboxBitmap(effect.intensity),
            clipStartMs,
            clipStartMs + clipDurationMs
        ) { _ -> overlaySettings(0.5f, 0.5f, 1f, 1f) }
    }

    clip.effects.filter {
        it.type == EffectType.FILM_GRAIN ||
            it.type == EffectType.ANAMORPHIC_FLARE ||
            it.type == EffectType.SPARKLE ||
            it.type == EffectType.LIGHT_LEAK ||
            it.type == EffectType.LENS_FLARE ||
        it.type == EffectType.BOKEH
    }.forEach { effect ->
        val windowStartMs = effect.startTimeMs.coerceIn(0L, clipDurationMs)
        val windowEndMs = if (effect.endTimeMs == -1L) {
            clipDurationMs
        } else {
            effect.endTimeMs.coerceIn(0L, clipDurationMs)
        }
        if (windowEndMs <= windowStartMs) return@forEach
        overlays += ProceduralEffectBitmapOverlay(
            type = effect.type,
            intensity = effect.intensity,
            blankBitmap = blank,
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs
        )
    }

    // These transition variants are safe to render as a deterministic color fade.
    val transition = clip.transitionNext
    if (transition.type == TransitionType.FADE_TO_BLACK || transition.type == TransitionType.FADE_TO_WHITE) {
        val duration = transition.durationMs.coerceIn(1L, clipDurationMs)
        val bitmap = renderSolidBitmap(
            if (transition.type == TransitionType.FADE_TO_BLACK) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        )
        val start = clipDurationMs - duration
        overlays += TimedBitmapOverlay(
            listOf(BitmapWindow(start, clipDurationMs, bitmap)),
            blank
        ) { localTime ->
            overlaySettings(0.5f, 0.5f, 1f, 1f, alpha = ((localTime - start).toFloat() / duration).coerceIn(0f, 1f))
        }
    }
    if (transition.type in EXPORT_PHOTO_TRANSITIONS) {
        val clipIndex = state.clips.indexOfFirst { it.id == clip.id }
        val nextClip = state.clips.getOrNull(clipIndex + 1)
        val duration = transition.durationMs.coerceIn(1L, clipDurationMs)
        val start = clipDurationMs - duration
        if (clip.canRenderTransitionSource() && nextClip?.canRenderTransitionSource() == true) {
            if (nextClip.isPhoto) {
                loadBitmap(context, nextClip.sourceUri)?.let { nextBitmap ->
                    overlays += PhotoTransitionBitmapOverlay(
                        type = transition.type,
                        sourceBitmap = nextBitmap,
                        blankBitmap = blank,
                        windowStartMs = start,
                        windowEndMs = clipDurationMs
                    )
                }
            } else if (transition.type in EXPORT_VIDEO_TRANSITIONS && nextClip.canRenderVideoTransitionSource()) {
                runCatching {
                    VideoFrameBitmapOverlay(
                        context = context,
                        sourceUri = nextClip.sourceUri,
                        sourceTrimStartMs = nextClip.effectiveTrimStartMs,
                        sourceTrimEndMs = nextClip.effectiveTrimEndMs,
                        windowStartMs = start,
                        windowEndMs = clipDurationMs,
                        blankBitmap = blank,
                        chromaKey = ChromaKeySettings(),
                        settingsAt = { localTime ->
                            transitionOverlaySettings(
                                type = transition.type,
                                localTimeMs = localTime,
                                windowStartMs = start,
                                windowEndMs = clipDurationMs
                            )
                        }
                    )
                }.onSuccess { overlays += it }
            }
        }
    }

    return overlays
}

internal fun MediaClip.canRenderPhotoTransitionSource(): Boolean {
    return isPhoto && canRenderTransitionSource()
}

internal fun MediaClip.canRenderVideoTransitionSource(): Boolean {
    return !isPhoto && playbackSpeed == 1f && canRenderTransitionSource()
}

private fun MediaClip.canRenderTransitionSource(): Boolean {
    return sourceUri.isNotBlank() &&
        effectiveTrimEndMs > effectiveTrimStartMs &&
        rotation == 0f &&
        !flipHorizontal &&
        !flipVertical &&
        cropRect == EXPORT_DEFAULT_CROP &&
        scale == 1f &&
        posX == 0.5f &&
        posY == 0.5f &&
        filterType == FilterType.NONE &&
        adjustments == ColorAdjustments() &&
        effects.isEmpty() &&
        photoAnimationSettings.type == PhotoAnimationType.NONE &&
        keyframes.isEmpty()
}

internal fun buildExportVideoEffects(
    context: Context,
    state: EditorState,
    clip: MediaClip,
    clipStartMs: Long,
    clipDurationMs: Long
): List<Effect> {
    val effects = mutableListOf<Effect>()
    val crop = clip.cropRect
    val hasCropKeyframes = clip.keyframes.keys.any { it in EXPORT_CROP_KEYFRAMES }
    if (crop != EXPORT_DEFAULT_CROP && !hasCropKeyframes) {
        effects += Crop(
            crop.left * 2f - 1f,
            crop.right * 2f - 1f,
            1f - crop.bottom * 2f,
            1f - crop.top * 2f
        )
    }
    val hasTransformKeyframes = clip.keyframes.keys.any { it in EXPORT_SUPPORTED_CLIP_KEYFRAMES }
    if (clip.posX != 0.5f || clip.posY != 0.5f || hasTransformKeyframes) {
        effects += KeyframedClipTransformation(clip, animateCrop = hasCropKeyframes)
    } else if (clip.rotation != 0f || clip.flipHorizontal || clip.flipVertical || clip.scale != 1f) {
        effects += ScaleAndRotateTransformation.Builder()
            .setScale(
                (if (clip.flipHorizontal) -1f else 1f) * clip.scale,
                (if (clip.flipVertical) -1f else 1f) * clip.scale
            )
            .setRotationDegrees(clip.rotation)
            .build()
    }

    val filterMatrix = getColorMatrixForFilter(clip.filterType, clip.filterIntensity)
    val adjustmentMatrix = getColorMatrixForAdjustments(clip.adjustments)
    val combined = ColorMatrix().apply {
        timesAssign(filterMatrix)
        timesAssign(adjustmentMatrix)
    }
    val values = combined.values
    val matrix4x4 = floatArrayOf(
        values[0], values[1], values[2], values[3],
        values[5], values[6], values[7], values[8],
        values[10], values[11], values[12], values[13],
        values[15], values[16], values[17], values[18]
    )
    val isIdentity = matrix4x4.contentEquals(floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    ))
    if (!isIdentity) effects += ExportRgbMatrix(matrix4x4)

    clip.effects.forEach { effect ->
        when (effect.type) {
            EffectType.GAUSSIAN_BLUR -> {
                effects += GaussianBlur((24f * effect.intensity.coerceIn(0.05f, 1f)).coerceAtLeast(1f))
            }
            EffectType.MIRROR -> {
                effects += ScaleAndRotateTransformation.Builder()
                    .setScale(-1f, 1f)
                    .build()
            }
            EffectType.SHAKE -> effects += ShakeTransformation(effect.intensity)
            EffectType.COMIC_BOOK,
            EffectType.PENCIL_SKETCH,
            EffectType.POP_ART -> {
                buildExportEffectColorMatrix(effect)?.let { effects += ExportRgbMatrix(it) }
            }
            in EXPORT_SHADER_EFFECTS -> {
                val endTimeMs = if (effect.endTimeMs == -1L) clipDurationMs else effect.endTimeMs
                effects += ShaderDistortionEffect(
                    type = effect.type,
                    intensity = effect.intensity,
                    startTimeMs = effect.startTimeMs,
                    endTimeMs = endTimeMs
                )
            }
            else -> Unit
        }
    }

    val overlays = buildExportOverlays(context, state, clipStartMs, clipDurationMs, clip)
    if (overlays.isNotEmpty()) effects += OverlayEffect(overlays)
    return effects
}

internal fun EditorState.exportUnsupportedReasons(): List<String> {
    val reasons = mutableListOf<String>()
    clips.forEachIndexed { index, clip ->
        if (clip.hasUnsupportedExportEdits()) reasons += "an unsupported clip edit"
        if (clip.keyframes.keys.any { it !in EXPORT_SUPPORTED_CLIP_KEYFRAMES && it !in EXPORT_SUPPORTED_AUDIO_KEYFRAMES }) {
            reasons += "unsupported clip keyframes"
        }
        if (clip.hasInvalidCropKeyframes()) reasons += "invalid crop keyframes"
        if (clip.keyframes["volume"].orEmpty().any { it.timeMs < 0L || !it.value.isFinite() || it.value !in 0f..1f }) {
            reasons += "invalid clip audio automation"
        }
        if (clip.audioEffects.hasUnsupportedExportAutomation()) reasons += "advanced clip audio automation"
        val transitionType = clip.transitionNext.type
        val isSupportedSourceTransition = transitionType in EXPORT_PHOTO_TRANSITIONS &&
            index < clips.lastIndex &&
            clip.canRenderTransitionSource() &&
            ((clips[index + 1].isPhoto && clips[index + 1].canRenderPhotoTransitionSource()) ||
                (!clips[index + 1].isPhoto &&
                    transitionType in EXPORT_VIDEO_TRANSITIONS &&
                    clips[index + 1].canRenderVideoTransitionSource()))
        val isColorFade = transitionType == TransitionType.NONE ||
            transitionType == TransitionType.FADE_TO_BLACK ||
            transitionType == TransitionType.FADE_TO_WHITE
        if (!isColorFade && !isSupportedSourceTransition) {
            reasons += "this transition type"
        }
        if (clip.effects.any { it.type !in EXPORT_SUPPORTED_EFFECTS }) {
            reasons += "this visual effect"
        }
        if (clip.effects.any {
                it.startTimeMs < 0L ||
                    it.endTimeMs < -1L ||
                    (it.startTimeMs > 0L && it.type !in EXPORT_TIMED_OVERLAY_EFFECTS) ||
                    (it.endTimeMs != -1L && it.endTimeMs < clip.durationMs && it.type !in EXPORT_TIMED_OVERLAY_EFFECTS) ||
                    !it.intensity.isFinite() ||
                    it.intensity !in 0f..1f
            }) {
            reasons += "partial-duration or invalid visual effect settings"
        }
    }
    if (overlays.any { it.blendMode != OverlayBlendModeType.NORMAL || it.maskShape != MaskShape.NONE || it.entranceAnim == OverlayAnim.SLIDE || it.exitAnim == OverlayAnim.SLIDE }) {
        reasons += "unsupported overlay animation"
    }
    if (overlays.any { it.keyframes.keys.any { key -> key !in EXPORT_SUPPORTED_OVERLAY_KEYFRAMES } }) {
        reasons += "unsupported overlay keyframes"
    }
    if (overlays.any { it.hasInvalidExportKeyframes() }) {
        reasons += "invalid overlay keyframes"
    }
    if (overlays.any {
            it.chromaKey.enabled &&
                (it.isGif ||
                    !it.chromaKey.similarity.isFinite() ||
                    !it.chromaKey.smoothness.isFinite() ||
                    !it.chromaKey.spillSuppression.isFinite())
        }) {
        reasons += "unsupported chroma key"
    }
    if (texts.any { it.animIn !in EXPORT_SUPPORTED_TEXT_ANIM_IN || it.animLoop !in EXPORT_SUPPORTED_TEXT_ANIM_LOOP || it.animOut !in EXPORT_SUPPORTED_TEXT_ANIM_OUT }) {
        reasons += "an unsupported text animation"
    }
    if (stickers.any { it.animIn !in EXPORT_SUPPORTED_TEXT_ANIM_IN || it.animLoop !in EXPORT_SUPPORTED_TEXT_ANIM_LOOP || it.animOut !in EXPORT_SUPPORTED_TEXT_ANIM_OUT }) {
        reasons += "an unsupported sticker animation"
    }
    if (audioClips.any { it.sourceUri.isNullOrBlank() && it.sourceClipId.isNullOrBlank() }) reasons += "an audio source"
    if (audioClips.any {
            it.isLooped ||
                it.autoDucking ||
                it.keyframes.keys.any { key -> key !in EXPORT_SUPPORTED_AUDIO_KEYFRAMES } ||
                it.keyframes["volume"].orEmpty().any { keyframe ->
                    keyframe.timeMs < 0L || !keyframe.value.isFinite() || keyframe.value !in 0f..1f
                } ||
                it.audioEffects.hasUnsupportedExportAutomation()
        }) {
        reasons += "advanced audio automation"
    }
    if (canvasSettings.aspectOption != AspectRatioOption.R_16_9 ||
        canvasSettings.fitMode != FitMode.Fit ||
        canvasSettings.backgroundType != BackgroundType.Blur
    ) reasons += "custom canvas output"
    return reasons.distinct()
}

private fun MediaClip.hasInvalidCropKeyframes(): Boolean {
    val cropKeyframes = keyframes.filterKeys { it in EXPORT_CROP_KEYFRAMES }
    if (cropKeyframes.isEmpty()) return false
    val duration = durationMs.coerceAtLeast(0L)
    if (cropKeyframes.values.flatten().any {
            it.timeMs !in 0L..duration ||
                !it.value.isFinite() ||
                it.value !in 0f..1f
        }
    ) return true

    val sampleTimes = (listOf(0L, duration) + cropKeyframes.values.flatten().map { it.timeMs }).distinct()
    return sampleTimes.any { timeMs ->
        val left = keyframes.getValueAtTime("cropLeft", timeMs, cropRect.left)
        val top = keyframes.getValueAtTime("cropTop", timeMs, cropRect.top)
        val right = keyframes.getValueAtTime("cropRight", timeMs, cropRect.right)
        val bottom = keyframes.getValueAtTime("cropBottom", timeMs, cropRect.bottom)
        left !in 0f..1f || top !in 0f..1f || right !in 0f..1f || bottom !in 0f..1f ||
            right <= left || bottom <= top
    }
}

private fun OverlayClip.hasInvalidExportKeyframes(): Boolean {
    if (keyframes.isEmpty()) return false
    val duration = durationMs.coerceAtLeast(0L)
    if (duration <= 0L) return true
    if (keyframes.values.flatten().any { it.timeMs !in 0L..duration || !it.value.isFinite() }) return true
    if (keyframes["scaleX"].orEmpty().any { it.value <= 0f } ||
        keyframes["scaleY"].orEmpty().any { it.value <= 0f } ||
        keyframes["opacity"].orEmpty().any { it.value !in 0f..1f } ||
        keyframes["posX"].orEmpty().any { it.value !in 0f..1f } ||
        keyframes["posY"].orEmpty().any { it.value !in 0f..1f }
    ) return true
    return false
}

internal fun EditorState.hasUnsupportedExportEdits(): Boolean = exportUnsupportedReasons().isNotEmpty()
