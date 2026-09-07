package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextureOverlay
import kotlin.math.max
import kotlin.math.min

private val EXPORT_DEFAULT_CROP = androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)

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

private data class BitmapWindow(
    val startMs: Long,
    val endMs: Long,
    val bitmap: Bitmap
)

private class ExportRgbMatrix(private val matrix: FloatArray) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix.copyOf()
}

private fun overlaySettings(
    posX: Float,
    posY: Float,
    scaleX: Float,
    scaleY: Float,
    rotation: Float = 0f,
    alpha: Float = 1f
): OverlaySettings {
    // Media3 uses a centred coordinate system with positive Y upwards.
    return OverlaySettings.Builder()
        .setAlphaScale(alpha.coerceIn(0f, 1f))
        .setBackgroundFrameAnchor(
            (posX.coerceIn(0f, 1f) * 2f) - 1f,
            1f - (posY.coerceIn(0f, 1f) * 2f)
        )
        .setOverlayFrameAnchor(0f, 0f)
        .setScale(scaleX.coerceAtLeast(0.001f), scaleY.coerceAtLeast(0.001f))
        .setRotationDegrees(rotation)
        .build()
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
    return if (overlay.isPhoto) loadBitmap(context, overlay.sourceUri)
    else loadVideoFrame(context, overlay.sourceUri, overlay.trimStartMs)
}

private fun buildTextSettings(text: TextOverlay, globalTimeMs: Long): OverlaySettings {
    val relative = (globalTimeMs - text.startTimeOnTimelineMs).coerceAtLeast(0L)
    return overlaySettings(
        posX = text.keyframes.getValueAtTime("posX", relative, text.posX),
        posY = text.keyframes.getValueAtTime("posY", relative, text.posY),
        scaleX = 0.55f * text.keyframes.getValueAtTime("scale", relative, text.scale),
        scaleY = 0.55f * text.keyframes.getValueAtTime("scale", relative, text.scale),
        rotation = text.keyframes.getValueAtTime("rotation", relative, text.rotation),
        alpha = text.keyframes.getValueAtTime("opacity", relative, text.opacity)
    )
}

private fun buildStickerSettings(sticker: StickerOverlay): OverlaySettings = overlaySettings(
    sticker.posX,
    sticker.posY,
    0.28f * sticker.scale,
    0.28f * sticker.scale,
    sticker.rotation,
    sticker.opacity
)

private fun buildOverlaySettings(overlay: OverlayClip, globalTimeMs: Long): OverlaySettings {
    val relative = (globalTimeMs - overlay.startTimeOnTimelineMs).coerceAtLeast(0L)
    return overlaySettings(
        posX = overlay.keyframes.getValueAtTime("posX", relative, overlay.posX),
        posY = overlay.keyframes.getValueAtTime("posY", relative, overlay.posY),
        scaleX = overlay.keyframes.getValueAtTime("scaleX", relative, overlay.scaleX),
        scaleY = overlay.keyframes.getValueAtTime("scaleY", relative, overlay.scaleY),
        rotation = overlay.keyframes.getValueAtTime("rotation", relative, overlay.rotation),
        alpha = overlay.keyframes.getValueAtTime("opacity", relative, overlay.opacity)
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

    // Image/video overlays are rendered as a still frame until a full secondary
    // video-compositor track is available. Video overlays remain export-blocked.
    state.overlays.filter { it.isVisible && it.isPhoto && it.durationMs > 0L }.forEach { overlay ->
        val bitmap = renderOverlayClipBitmap(context, overlay) ?: return@forEach
        addWindowed(
            bitmap,
            overlay.startTimeOnTimelineMs,
            overlay.startTimeOnTimelineMs + overlay.durationMs
        ) { localTime -> buildOverlaySettings(overlay, clipStartMs + localTime) }
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
        ) { _ -> buildStickerSettings(sticker) }
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

    return overlays
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
    if (crop != EXPORT_DEFAULT_CROP) {
        effects += Crop(
            crop.left * 2f - 1f,
            crop.right * 2f - 1f,
            1f - crop.bottom * 2f,
            1f - crop.top * 2f
        )
    }
    if (clip.rotation != 0f || clip.flipHorizontal || clip.flipVertical || clip.scale != 1f) {
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

    clip.effects.filter { it.type == EffectType.GAUSSIAN_BLUR }.forEach { effect ->
        effects += GaussianBlur((24f * effect.intensity.coerceIn(0.05f, 1f)).coerceAtLeast(1f))
    }

    val overlays = buildExportOverlays(context, state, clipStartMs, clipDurationMs, clip)
    if (overlays.isNotEmpty()) effects += OverlayEffect(overlays)
    return effects
}

internal fun EditorState.exportUnsupportedReasons(): List<String> {
    val reasons = mutableListOf<String>()
    clips.forEach { clip ->
        if (clip.hasUnsupportedExportEdits()) reasons += "an unsupported clip edit"
        if (clip.posX != 0.5f || clip.posY != 0.5f) reasons += "clip positioning"
        if (clip.keyframes.isNotEmpty()) reasons += "clip keyframes"
        if (clip.audioEffects != AudioEffects()) reasons += "advanced clip audio effects"
        if (clip.transitionNext.type !in setOf(TransitionType.NONE, TransitionType.FADE_TO_BLACK, TransitionType.FADE_TO_WHITE)) {
            reasons += "this transition type"
        }
        if (clip.effects.any { it.type != EffectType.GAUSSIAN_BLUR && it.type != EffectType.LETTERBOX }) {
            reasons += "this visual effect"
        }
    }
    if (overlays.any { !it.isPhoto || it.isGif || it.blendMode != OverlayBlendModeType.NORMAL || it.maskShape != MaskShape.NONE || it.entranceAnim != OverlayAnim.NONE || it.exitAnim != OverlayAnim.NONE }) {
        reasons += "video or animated overlays"
    }
    if (texts.any { it.animIn != TextAnimIn.NONE || it.animLoop != TextAnimLoop.NONE || it.animOut != TextAnimOut.NONE }) {
        reasons += "animated text"
    }
    if (stickers.any { it.animIn != TextAnimIn.NONE || it.animLoop != TextAnimLoop.NONE || it.animOut != TextAnimOut.NONE }) {
        reasons += "animated stickers"
    }
    if (audioClips.any { it.sourceUri.isNullOrBlank() && it.sourceClipId.isNullOrBlank() }) reasons += "an audio source"
    if (audioClips.any { it.isLooped || it.keyframes.isNotEmpty() || it.autoDucking || it.audioEffects != AudioEffects() }) {
        reasons += "advanced audio automation"
    }
    if (canvasSettings.aspectOption != AspectRatioOption.R_16_9 ||
        canvasSettings.fitMode != FitMode.Fit ||
        canvasSettings.backgroundType != BackgroundType.Blur
    ) reasons += "custom canvas output"
    return reasons.distinct()
}

internal fun EditorState.hasUnsupportedExportEdits(): Boolean = exportUnsupportedReasons().isNotEmpty()
