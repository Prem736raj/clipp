package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class MaskShape {
    NONE, CIRCLE, RECTANGLE, HEART, STAR
}

enum class OverlayAnim {
    NONE, FADE, SLIDE, SCALE
}

enum class OverlayBlendModeType {
    NORMAL, MULTIPLY, SCREEN, OVERLAY, SOFT_LIGHT, HARD_LIGHT, DIFFERENCE, ADD
}

enum class TextAlignmentType { Left, Center, Right }

/**
 * Fonts are deliberately local. Legacy saved names are mapped to the closest
 * platform family so opening an old project never triggers a network request.
 */
val availableFonts = listOf("System Sans", "System Serif", "System Monospace")

fun getFontFamily(fontName: String): FontFamily = when {
    fontName.equals("System Serif", ignoreCase = true) ||
        listOf("Merriweather", "Lora", "PT Serif", "Noto Serif", "Libre Baskerville", "Crimson Text", "Playfair Display", "Anton", "Cinzel")
            .any { fontName.equals(it, ignoreCase = true) } -> FontFamily.Serif
    fontName.equals("System Monospace", ignoreCase = true) ||
        listOf("Inconsolata", "Fira Code", "Space Mono", "IBM Plex Mono", "Source Code Pro", "Nanum Gothic Coding")
            .any { fontName.equals(it, ignoreCase = true) } -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

enum class TextAnimIn { NONE, FADE_IN, SLIDE_IN_UP, SLIDE_IN_DOWN, SLIDE_IN_LEFT, SLIDE_IN_RIGHT, SCALE_IN, TYPEWRITER, BOUNCE_IN, BLUR_IN, ROTATE_IN, GLITCH_IN }
enum class TextAnimLoop { NONE, PULSE, BOUNCE, SHAKE, GLOW, WAVE, FLICKER, SWING, FLOAT }
enum class TextAnimOut { NONE, FADE_OUT, SLIDE_OUT_UP, SLIDE_OUT_DOWN, SLIDE_OUT_LEFT, SLIDE_OUT_RIGHT, SCALE_OUT, DISSOLVE, BOUNCE_OUT, BLUR_OUT }

data class TextOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "Text",
    val fontName: String = "System Sans",
    val fontSize: Float = 48f,
    val textColor: Color = Color.White,
    val backgroundColor: Color = Color.Transparent,
    val alignment: TextAlignmentType = TextAlignmentType.Center,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val strokeColor: Color = Color.Transparent,
    val strokeWidth: Float = 0f,
    val shadowColor: Color = Color.Transparent,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 0f,
    val shadowBlur: Float = 0f,
    val letterSpacing: Float = 0f,
    val lineHeightMultiplier: Float = 1f,
    val is3D: Boolean = false,
    val name: String = "Text",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val animIn: TextAnimIn = TextAnimIn.NONE,
    val animInDurationMs: Long = 500L,
    val animInDelayMs: Long = 0L,
    val isTypewriterCursor: Boolean = true,
    val animLoop: TextAnimLoop = TextAnimLoop.NONE,
    val animLoopDurationMs: Long = 1000L,
    val animLoopDelayMs: Long = 0L,
    val animOut: TextAnimOut = TextAnimOut.NONE,
    val animOutDurationMs: Long = 500L,
    val animOutDelayMs: Long = 0L,
    val startTimeOnTimelineMs: Long = 0L,
    val durationMs: Long = 5000L,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val keyframes: Map<String, List<Keyframe>> = emptyMap()
)

enum class StickerCategory(val title: String) {
    EMOJI("Emojis"),
    ANIM_REACTION("Reactions"),
    ANIM_ARROW("Arrows & Pointers"),
    ANIM_SOCIAL("Social Media"),
    ANIM_SPEECH("Speech Bubbles"),
    ANIM_DECOR("Decorative"),
    ANIM_LABEL("Labels"),
    SHAPE("Shapes")
}

data class StickerModel(
    val id: String,
    val content: String,
    val category: StickerCategory,
    val defaultAnimLoop: TextAnimLoop = TextAnimLoop.NONE,
    val isIcon: Boolean = false
)

data class StickerOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val modelId: String,
    val content: String,
    val category: StickerCategory,
    val isIcon: Boolean = false,
    val color: Color = Color.White,
    val backgroundColor: Color = Color.Transparent,
    val name: String = "Sticker",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val startTimeOnTimelineMs: Long = 0L,
    val durationMs: Long = 5000L,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val animIn: TextAnimIn = TextAnimIn.NONE,
    val animInDurationMs: Long = 500L,
    val animInDelayMs: Long = 0L,
    val animLoop: TextAnimLoop = TextAnimLoop.NONE,
    val animLoopDurationMs: Long = 1000L,
    val animLoopDelayMs: Long = 0L,
    val animOut: TextAnimOut = TextAnimOut.NONE,
    val animOutDurationMs: Long = 500L,
    val animOutDelayMs: Long = 0L,
    // Kept on the layer payload so the unified timeline can animate sticker
    // transforms without losing the data during legacy-state migration.
    val keyframes: Map<String, List<Keyframe>> = emptyMap()
)

data class OverlayClip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceUri: String,
    val originalDurationMs: Long,
    val isPhoto: Boolean = false,
    val isGif: Boolean = false,
    val name: String = "Overlay",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val trimStartMs: Long = 0,
    val trimEndMs: Long,
    val startTimeOnTimelineMs: Long = 0,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val scaleX: Float = 0.5f,
    val scaleY: Float = 0.5f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val blendMode: OverlayBlendModeType = OverlayBlendModeType.NORMAL,
    val shadowColor: Color = Color.Transparent,
    val shadowRadius: Float = 0f,
    val borderColor: Color = Color.Transparent,
    val borderWidth: Float = 0f,
    val maskShape: MaskShape = MaskShape.NONE,
    val entranceAnim: OverlayAnim = OverlayAnim.NONE,
    val exitAnim: OverlayAnim = OverlayAnim.NONE,
    val chromaKey: ChromaKeySettings = ChromaKeySettings(),
    val keyframes: Map<String, List<Keyframe>> = emptyMap()
) {
    val durationMs: Long get() = trimEndMs - trimStartMs
}

enum class BrushType(val title: String) {
    PEN("Pen"),
    MARKER("Marker"),
    NEON("Neon"),
    SPRAY("Spray Paint"),
    CALLIGRAPHY("Calligraphy")
}

data class DrawStroke(
    val path: List<NormalizedOffset>,
    val color: Color,
    val width: Float,
    val brushType: BrushType,
    val isEraser: Boolean = false
)

data class DrawOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val strokes: List<DrawStroke> = emptyList(),
    val startTimeOnTimelineMs: Long = 0L,
    val durationMs: Long = 5000L,
    val isAnimated: Boolean = false,
    val name: String = "Drawing",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f
)

enum class FrameCategory(val title: String) {
    CLEAN("Clean Borders"),
    PHONE_MOCKUP("Phone Mockup"),
    SOCIAL_MEDIA("Social Media"),
    DECORATIVE("Decorative"),
    FESTIVE("Festive")
}

data class FrameType(
    val id: String,
    val category: FrameCategory,
    val name: String
)

object FrameRegistry {
    val types = listOf(
        FrameType("clean_solid", FrameCategory.CLEAN, "Solid"),
        FrameType("clean_rounded", FrameCategory.CLEAN, "Rounded"),
        FrameType("mockup_iphone", FrameCategory.PHONE_MOCKUP, "iPhone"),
        FrameType("mockup_android", FrameCategory.PHONE_MOCKUP, "Android"),
        FrameType("mockup_laptop", FrameCategory.PHONE_MOCKUP, "Laptop"),
        FrameType("social_insta", FrameCategory.SOCIAL_MEDIA, "Instagram"),
        FrameType("social_imessage", FrameCategory.SOCIAL_MEDIA, "iMessage"),
        FrameType("deco_film", FrameCategory.DECORATIVE, "Film Strip"),
        FrameType("deco_polaroid", FrameCategory.DECORATIVE, "Polaroid"),
        FrameType("deco_torn", FrameCategory.DECORATIVE, "Torn Paper"),
        FrameType("festive_xmas", FrameCategory.FESTIVE, "Christmas"),
        FrameType("festive_party", FrameCategory.FESTIVE, "Party")
    )
}

data class FrameOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val typeId: String,
    val startTimeOnTimelineMs: Long = 0L,
    val durationMs: Long = 5000L,
    val color: Color = Color.White,
    val thickness: Float = 0.05f,
    val cornerRadius: Float = 0f,
    val name: String = "Frame",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f
)
