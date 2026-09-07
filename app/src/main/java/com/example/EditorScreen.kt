package com.example

import com.example.R
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.ui.draw.rotate
import kotlin.math.pow
import kotlin.math.sin
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.ProjectEntity
import com.example.viewmodel.ProjectViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

import androidx.compose.ui.geometry.Rect

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
    val durationMs: Long = 500L
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

@Composable
fun AudioPlayerComponent(
    clip: AudioClip,
    clips: List<MediaClip>,
    currentPositionMs: Long,
    isPlaying: Boolean,
    isMuted: Boolean,
    masterVolume: Float
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = if (clip.isLooped) androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    val actualUri = clip.sourceUri ?: clips.find { it.id == clip.sourceClipId }?.sourceUri
    
    val targetVolume = if (isMuted || clip.isMuted) 0f else clip.volume * masterVolume
    
    val smoothedVolume by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetVolume,
        animationSpec = androidx.compose.animation.core.tween(500)
    )
    
    LaunchedEffect(actualUri) {
        if (actualUri != null) {
            val mi = androidx.media3.common.MediaItem.fromUri(actualUri)
            exoPlayer.setMediaItem(mi)
            exoPlayer.prepare()
        }
    }
    
    LaunchedEffect(currentPositionMs, isPlaying, clip, isMuted, masterVolume, smoothedVolume) {
        if (actualUri == null) return@LaunchedEffect
        
        val relativeTimeMs = currentPositionMs - clip.startTimeOnTimelineMs
        if (relativeTimeMs >= 0 && relativeTimeMs < clip.durationMs) {
            val srcPos = if (clip.isLooped) {
                val cycleLength = if (clip.trimEndMs > clip.trimStartMs) clip.trimEndMs - clip.trimStartMs else 1L
                clip.trimStartMs + (relativeTimeMs % cycleLength)
            } else {
                clip.trimStartMs + relativeTimeMs
            }
            
            exoPlayer.volume = smoothedVolume
            
            if (Math.abs(exoPlayer.currentPosition - srcPos) > 100) {
                exoPlayer.seekTo(srcPos)
            }
            
            if (isPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        } else {
            exoPlayer.pause()
        }
    }
}

@Composable
fun VUMeter(
    isPlaying: Boolean,
    masterVolume: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(24.dp).width(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until 5) {
            val level = if (isMuted || !isPlaying) 0f else masterVolume.coerceIn(0f, 1f)
            val isActive = level >= (i + 1) / 5f
            val h by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isActive) 1f else 0.18f,
                animationSpec = androidx.compose.animation.core.tween(150),
                label = "volume_level"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(h)
                    .padding(horizontal = 1.dp)
                    .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
fun AudioWaveform(
    clipId: String,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    pixelsPerMs: Float,
    keyframes: Map<String, List<Keyframe>> = emptyMap(),
    audioEffects: AudioEffects = AudioEffects(),
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val height = size.height
        if (durationMs <= 0L || totalWidth <= 0f) return@Canvas

        // A measured waveform is not available yet. Keep a neutral reference line
        // instead of presenting synthetic amplitudes as audio analysis.
        drawLine(
            color = color.copy(alpha = 0.55f),
            start = androidx.compose.ui.geometry.Offset(0f, height / 2f),
            end = androidx.compose.ui.geometry.Offset(totalWidth, height / 2f),
            strokeWidth = 1.dp.toPx()
        )

        val totalTrimMs = (trimEndMs - trimStartMs).coerceAtLeast(0L)
        val fadeInPx = (audioEffects.fadeInMs.coerceAtMost(totalTrimMs) * pixelsPerMs).coerceIn(0f, totalWidth)
        val fadeOutPx = (audioEffects.fadeOutMs.coerceAtMost(totalTrimMs) * pixelsPerMs).coerceIn(0f, totalWidth)
        if (fadeInPx > 0f) {
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, height), end = androidx.compose.ui.geometry.Offset(fadeInPx, 0f), strokeWidth = 2.dp.toPx())
        }
        if (fadeOutPx > 0f) {
            val startPx = (totalWidth - fadeOutPx).coerceAtLeast(0f)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(startPx, 0f), end = androidx.compose.ui.geometry.Offset(totalWidth, height), strokeWidth = 2.dp.toPx())
        }

        keyframes["volume"]?.forEach { keyframe ->
            val kpx = ((keyframe.timeMs - trimStartMs) * pixelsPerMs).coerceIn(0f, totalWidth)
            drawCircle(color = Color.Red, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(kpx, height / 2f))
        }
    }
}
fun Map<String, List<Keyframe>>.getValueAtTime(propName: String, timeMs: Long, defaultValue: Float): Float {
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
    
    val dt = (k2.timeMs - k1.timeMs)
    if (dt == 0L) return k1.value
    val t = (timeMs - k1.timeMs).toFloat() / dt
    val easedT = applyEasing(t, k1.easing) 
    
    return k1.value + (k2.value - k1.value) * easedT
}

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
        volume = volume.coerceIn(0f, 1f)
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
    R_9_16("9:16", 9f/16f, "TikTok/Shorts"),
    R_16_9("16:9", 16f/9f, "YouTube/TV"),
    R_1_1("1:1", 1f/1f, "Instagram Post"),
    R_4_5("4:5", 4f/5f, "Instagram Feed"),
    R_4_3("4:3", 4f/3f, "Classic"),
    R_21_9("21:9", 21f/9f, "Cinematic")
}

enum class FitMode { Fit, Fill, Stretch }
enum class BackgroundType { Color, Blur, Gradient }

data class CanvasSettingsState(
    val aspectOption: AspectRatioOption = AspectRatioOption.R_16_9,
    val fitMode: FitMode = FitMode.Fit,
    val backgroundType: BackgroundType = BackgroundType.Blur,
    val backgroundColorValue: ULong = androidx.compose.ui.graphics.Color.Black.value,
    val masterVolume: Float = 1.0f
) {
    val backgroundColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(backgroundColorValue)
}

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

data class StickerModel(val id: String, val content: String, val category: StickerCategory, val defaultAnimLoop: TextAnimLoop = TextAnimLoop.NONE, val isIcon: Boolean = false)

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
    val animOutDelayMs: Long = 0L
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

data class EditorState(
    val clips: List<MediaClip> = emptyList(),
    val canvasSettings: CanvasSettingsState = CanvasSettingsState(),
    val overlays: List<OverlayClip> = emptyList(),
    val texts: List<TextOverlay> = emptyList(),
    val captions: List<AutoCaptionSegment> = emptyList(),
    val captionSettings: CaptionSettings = CaptionSettings(),
    val stickers: List<StickerOverlay> = emptyList(),
    val drawings: List<DrawOverlay> = emptyList(),
    val frames: List<FrameOverlay> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val layerOrder: List<String> = emptyList()
)

data class HistoryAction(
    val state: EditorState,
    val description: String
)

data class EditorHistoryModel(
    val undoStack: List<HistoryAction> = emptyList(),
    val redoStack: List<HistoryAction> = emptyList(),
    val currentState: EditorState? = null
)

class ComposeDataAdapter {
    @com.squareup.moshi.ToJson
    fun rectToJson(rect: androidx.compose.ui.geometry.Rect): List<Float> = listOf(rect.left, rect.top, rect.right, rect.bottom)
    @com.squareup.moshi.FromJson
    fun rectFromJson(list: List<Float>): androidx.compose.ui.geometry.Rect {
        if (list.size < 4) return androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)
        return androidx.compose.ui.geometry.Rect(list[0], list[1], list[2], list[3])
    }

    @com.squareup.moshi.ToJson
    fun colorToJson(color: androidx.compose.ui.graphics.Color): Long = color.value.toLong()
    @com.squareup.moshi.FromJson
    fun colorFromJson(value: Long): androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(value.toULong())
}

val editorHistoryMoshi: com.squareup.moshi.Moshi = com.squareup.moshi.Moshi.Builder()
    .add(ComposeDataAdapter())
    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
    .build()

fun removeTimeRangeFromClips(clips: List<MediaClip>, startGlobal: Long, endGlobal: Long): List<MediaClip> {
    val result = mutableListOf<MediaClip>()
    var globalAccum = 0L
    for (clip in clips) {
        val clipStartGlobal = globalAccum
        val clipEndGlobal = globalAccum + clip.durationMs
        
        if (endGlobal <= clipStartGlobal || startGlobal >= clipEndGlobal) {
            result.add(clip)
        } else if (startGlobal <= clipStartGlobal && endGlobal >= clipEndGlobal) {
            // Removed
        } else if (startGlobal > clipStartGlobal && endGlobal < clipEndGlobal) {
            val localStart = startGlobal - clipStartGlobal + clip.trimStartMs
            val localEnd = endGlobal - clipStartGlobal + clip.trimStartMs
            result.add(clip.copy(id = java.util.UUID.randomUUID().toString(), trimEndMs = localStart))
            result.add(clip.copy(id = java.util.UUID.randomUUID().toString(), trimStartMs = localEnd))
        } else if (startGlobal <= clipStartGlobal && endGlobal < clipEndGlobal) {
            val localEnd = endGlobal - clipStartGlobal + clip.trimStartMs
            result.add(clip.copy(id = java.util.UUID.randomUUID().toString(), trimStartMs = localEnd))
        } else if (startGlobal > clipStartGlobal && endGlobal >= clipEndGlobal) {
            val localStart = startGlobal - clipStartGlobal + clip.trimStartMs
            result.add(clip.copy(id = java.util.UUID.randomUUID().toString(), trimEndMs = localStart))
        }
        globalAccum += clip.durationMs
    }
    return result
}

private const val MAX_EDITOR_HISTORY_DEPTH = 50

var globalClipboardItem: Any? by androidx.compose.runtime.mutableStateOf(null)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    projectViewModel: ProjectViewModel = viewModel()
) {
    var project by remember { mutableStateOf<ProjectEntity?>(null) }
    var showSavedIndicator by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val performanceMode = remember { com.example.viewmodel.PerformanceModeManager.getMode(context) }
    val isBudgetMode = performanceMode == com.example.viewmodel.PerformanceMode.BETTER_PERFORMANCE
    
    DisposableEffect(Unit) {
        val entryTime = System.currentTimeMillis()
        com.example.utils.AnalyticsManager.startEditingSession()
        onDispose {
            com.example.utils.ReviewManager.lastSessionDurationMs = System.currentTimeMillis() - entryTime
            com.example.utils.AnalyticsManager.endEditingSession()
        }
    }
    
    LaunchedEffect(Unit) {
        if (performanceMode == com.example.viewmodel.PerformanceMode.BETTER_PERFORMANCE && !com.example.viewmodel.PerformanceModeManager.hasShownBudgetTip) {
            android.widget.Toast.makeText(context, "Budget Device Mode: Reduced preview resolution. Final export will be high quality. Close other apps for best experience.", android.widget.Toast.LENGTH_LONG).show()
            com.example.viewmodel.PerformanceModeManager.hasShownBudgetTip = true
        }
    }
    
    val sharedPrefs = remember { context.getSharedPreferences("clipp_settings", Context.MODE_PRIVATE) }
    
    var isPlaying by remember { mutableStateOf(false) }
    val skipHeavyEffects = isBudgetMode && isPlaying
    var currentTime by remember { mutableStateOf("00:00") }
    
    var clips by remember { mutableStateOf<List<MediaClip>>(emptyList()) }
    var overlays by remember { mutableStateOf<List<OverlayClip>>(emptyList()) }
    var texts by remember { mutableStateOf<List<TextOverlay>>(emptyList()) }
    var captions by remember { mutableStateOf<List<AutoCaptionSegment>>(emptyList()) }
    var captionSettings by remember { mutableStateOf(CaptionSettings()) }
    var showExportPanel by remember { mutableStateOf(false) }
    var isScreenshotMode by remember { mutableStateOf(false) }
    var stickers by remember { mutableStateOf<List<StickerOverlay>>(emptyList()) }
    var drawings by remember { mutableStateOf<List<DrawOverlay>>(emptyList()) }
    var frames by remember { mutableStateOf<List<FrameOverlay>>(emptyList()) }
    var audioClips by remember { mutableStateOf<List<AudioClip>>(emptyList()) }
    var layerOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    var canvasSettings by remember { mutableStateOf(CanvasSettingsState()) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var activeTool by remember { mutableStateOf<String?>(null) }
    var selectedClipId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(activeTool) {
        if (activeTool != null && activeTool != "None") {
            com.example.utils.AnalyticsManager.trackFeature(activeTool!!)
        }
    }
    var selectedAudioId by remember { mutableStateOf<String?>(null) }
    var selectedOverlayId by remember { mutableStateOf<String?>(null) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var selectedStickerId by remember { mutableStateOf<String?>(null) }
    var selectedDrawId by remember { mutableStateOf<String?>(null) }
    var selectedFrameId by remember { mutableStateOf<String?>(null) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var showTextToolbar by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showStickerToolbar by remember { mutableStateOf(false) }
    var showFramePicker by remember { mutableStateOf(false) }
    var showFrameToolbar by remember { mutableStateOf(false) }
    var showDrawToolbar by remember { mutableStateOf(false) }
    var isDrawingMode by remember { mutableStateOf(false) }
    var currentBrushType by remember { mutableStateOf(BrushType.PEN) }
    var currentBrushSize by remember { mutableFloatStateOf(0.015f) }
    var currentBrushColor by remember { mutableStateOf(Color.White) }
    var currentIsEraser by remember { mutableStateOf(false) }
    var undoStack by remember { mutableStateOf<List<HistoryAction>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<HistoryAction>>(emptyList()) }
    
    LaunchedEffect(overlays, texts, stickers, drawings, frames) {
        val allIds = overlays.map { it.id } + texts.map { it.id } + stickers.map { it.id } + drawings.map { it.id } + frames.map { it.id }
        val newOrder = layerOrder.toMutableList()
        var changed = false
        val it = newOrder.iterator()
        while(it.hasNext()) {
            if (!allIds.contains(it.next())) {
                it.remove()
                changed = true
            }
        }
        for (id in allIds) {
            if (!newOrder.contains(id)) {
                newOrder.add(id)
                changed = true
            }
        }
        if (changed) {
            layerOrder = newOrder
        }
    }
    
    val timelineMapper = remember(clips) { TimelineMapper(clips) }
    val videoDurationMs = timelineMapper.totalDurationMs
    var currentPositionMs by remember { mutableStateOf(0L) }

    LaunchedEffect(videoDurationMs) {
        currentPositionMs = currentPositionMs.coerceIn(0L, videoDurationMs)
    }
    
    fun saveState(
        newClips: List<MediaClip> = clips,
        newCanvas: CanvasSettingsState = canvasSettings,
        description: String
    ) {
        val normalizedClips = newClips.map { it.normalized() }
        val prevState = EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder)
        undoStack = (undoStack + HistoryAction(prevState, description)).takeLast(MAX_EDITOR_HISTORY_DEPTH)
        redoStack = emptyList()
        clips = normalizedClips
        canvasSettings = newCanvas
        
        // Save to DB
        project?.let { currentProject ->
            val historyModel = EditorHistoryModel(undoStack, redoStack, EditorState(normalizedClips, newCanvas, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder))
            val historyJson = try { editorHistoryMoshi.adapter(EditorHistoryModel::class.java).toJson(historyModel) } catch(e: Exception) { "" }
            val savedProject = currentProject.copy(
                isDirty = true,
                historyState = historyJson,
                lastEdited = System.currentTimeMillis()
            )
            projectViewModel.updateProject(savedProject)
            project = savedProject
        }
    }
    
    fun saveOverlayState(
        newOverlays: List<OverlayClip>,
        description: String
    ) {
        val prevState = EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder)
        undoStack = (undoStack + HistoryAction(prevState, description)).takeLast(MAX_EDITOR_HISTORY_DEPTH)
        redoStack = emptyList()
        overlays = newOverlays
        
        project?.let { currentProject ->
            val historyModel = EditorHistoryModel(undoStack, redoStack, EditorState(clips, canvasSettings, newOverlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder))
            val historyJson = try { editorHistoryMoshi.adapter(EditorHistoryModel::class.java).toJson(historyModel) } catch(e: Exception) { "" }
            val savedProject = currentProject.copy(
                isDirty = true,
                historyState = historyJson,
                lastEdited = System.currentTimeMillis()
            )
            projectViewModel.updateProject(savedProject)
            project = savedProject
        }
    }

    fun saveTextState(
        newTexts: List<TextOverlay>,
        description: String
    ) {
        val prevState = EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder)
        undoStack = (undoStack + HistoryAction(prevState, description)).takeLast(MAX_EDITOR_HISTORY_DEPTH)
        redoStack = emptyList()
        texts = newTexts
        
        project?.let { currentProject ->
            val historyModel = EditorHistoryModel(undoStack, redoStack, EditorState(clips, canvasSettings, overlays, newTexts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder))
            val historyJson = try { editorHistoryMoshi.adapter(EditorHistoryModel::class.java).toJson(historyModel) } catch(e: Exception) { "" }
            val savedProject = currentProject.copy(
                isDirty = true,
                historyState = historyJson,
                lastEdited = System.currentTimeMillis()
            )
            projectViewModel.updateProject(savedProject)
            project = savedProject
        }
    }
    
    val persistHistory: () -> Unit = {
        project?.let { currentProject ->
            val historyModel = EditorHistoryModel(undoStack, redoStack, EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder))
            val historyJson = try { editorHistoryMoshi.adapter(EditorHistoryModel::class.java).toJson(historyModel) } catch(e: Exception) { "" }
            val savedProject = currentProject.copy(
                isDirty = true,
                historyState = historyJson,
                lastEdited = System.currentTimeMillis()
            )
            projectViewModel.updateProject(savedProject)
            project = savedProject
        }
    }

    var isRecordingVoiceover by remember { mutableStateOf(false) }
    var voiceRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceRecordingFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder?.let { recorder ->
                runCatching { recorder.stop() }
                runCatching { recorder.reset() }
                runCatching { recorder.release() }
            }
            voiceRecordingFile?.delete()
        }
    }

    fun addAudioTrack(uri: Uri, displayName: String?) {
        scope.launch {
            val durationMs = MediaMetadataReader.readAudioDuration(context, uri)
            if (durationMs == null || durationMs <= 0L) {
                android.widget.Toast.makeText(context, "Clipp could not read this audio file.", android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            val audio = AudioClip(
                sourceUri = uri.toString(),
                displayName = displayName ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Audio",
                startTimeOnTimelineMs = currentPositionMs.coerceIn(0L, videoDurationMs),
                sourceDurationMs = durationMs,
                trimEndMs = durationMs
            )
            audioClips = audioClips + audio
            selectedAudioId = audio.id
            persistHistory()
            android.widget.Toast.makeText(context, "Audio track added", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val musicPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addAudioTrack(uri, uri.lastPathSegment?.substringAfterLast('/'))
        }
    }

    fun startVoiceoverRecording() {
        if (isRecordingVoiceover) return
        val directory = File(context.filesDir, "voiceovers")
        if (!directory.exists() && !directory.mkdirs()) {
            android.widget.Toast.makeText(context, "Could not create a voiceover file.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val file = File(directory, "voiceover-${UUID.randomUUID()}.m4a")
        runCatching {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44_100)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            voiceRecorder = recorder
            voiceRecordingFile = file
            isRecordingVoiceover = true
            android.widget.Toast.makeText(context, "Recording voiceover… tap Stop Voiceover when finished.", android.widget.Toast.LENGTH_LONG).show()
        }.onFailure {
            file.delete()
            android.widget.Toast.makeText(context, "Could not start voiceover recording.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun stopVoiceoverRecording() {
        val recorder = voiceRecorder ?: return
        val file = voiceRecordingFile
        voiceRecorder = null
        voiceRecordingFile = null
        isRecordingVoiceover = false
        runCatching { recorder.stop() }
            .onFailure { file?.delete() }
        runCatching { recorder.reset() }
        runCatching { recorder.release() }
        if (file == null || !file.exists() || file.length() == 0L) {
            android.widget.Toast.makeText(context, "Voiceover recording was empty.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        addAudioTrack(Uri.fromFile(file), "Voiceover")
    }

    val recordAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceoverRecording()
        else android.widget.Toast.makeText(context, "Microphone permission is required for voiceover.", android.widget.Toast.LENGTH_LONG).show()
    }
    
    fun seekToGlobal(globalMs: Long, player: ExoPlayer) {
        val clampedGlobalMs = globalMs.coerceIn(0L, videoDurationMs)
        currentPositionMs = clampedGlobalMs
        timelineMapper.playerSeekPosition(clampedGlobalMs)?.let { seekPosition ->
            player.seekTo(seekPosition.clipIndex, seekPosition.positionInClippedSourceMs)
        }
    }

    var isMuted by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showFullscreenControls by remember { mutableStateOf(false) }
    
    
    val exoPlayer = remember {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context)
        if (isBudgetMode) {
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSize(854, 480))
        }
        ExoPlayer.Builder(context).setTrackSelector(trackSelector).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                        seekTo(0)
                        pause()
                    }
                }
            })
        }
    }
    
    val overlayExoPlayer = remember {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context)
        if (isBudgetMode) {
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSize(854, 480))
        }
        ExoPlayer.Builder(context).setTrackSelector(trackSelector).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
            volume = 0f
        }
    }
    
    val bgExoPlayer = remember {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context)
        if (isBudgetMode) {
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSize(854, 480))
        }
        ExoPlayer.Builder(context).setTrackSelector(trackSelector).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
            volume = 0f
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { 
            exoPlayer.release() 
            bgExoPlayer.release()
            overlayExoPlayer.release()
        }
    }
    
    val autoSaveIntervalStr = sharedPrefs.getString("auto_save", "30 secs") ?: "30 secs"
    val autoSaveIntervalMs = when (autoSaveIntervalStr) {
        "30 secs" -> 30_000L
        "1 min" -> 60_000L
        "5 mins" -> 300_000L
        "10 mins" -> 600_000L
        "Off" -> -1L
        else -> 30_000L
    }

    LaunchedEffect(project?.sourceMediaPaths) {
        val paths = project?.sourceMediaPaths
        if (clips.isEmpty() && paths != null && paths.isNotEmpty()) {
            // Check if undoStack is empty to avoid overwriting a restored project
            if (undoStack.isEmpty()) {
                val initialClips = mutableListOf<MediaClip>()
                for (path in paths) {
                    val metadata = MediaMetadataReader.read(context, android.net.Uri.parse(path)) ?: continue
                    val isPhoto = metadata.mimeType.startsWith("image/")
                    val durationMs = if (isPhoto) PHOTO_DEFAULT_DURATION_MS else metadata.durationMs
                    if (durationMs > 0L) {
                        initialClips.add(MediaClip(
                            sourceUri = path,
                            originalDurationMs = durationMs,
                            trimEndMs = durationMs,
                            isPhoto = isPhoto
                        ))
                    }
                }
                if (initialClips.isNotEmpty()) {
                    saveState(initialClips, canvasSettings, "Initial import")
                }
            }
        }
    }
    
    val exoPlaylistTokens = clips.map {
        "${it.id}|${it.sourceUri}|${it.trimStartMs}|${it.trimEndMs}|${it.isPhoto}|${it.playbackSpeed}|${it.speedCurve}"
    }.joinToString(",")
    LaunchedEffect(exoPlaylistTokens) {
        if (clips.isNotEmpty()) {
            val mediaItems = clips.map { clip ->
                val builder = MediaItem.Builder().setUri(android.net.Uri.parse(clip.sourceUri))
                if (clip.isPhoto) {
                    builder.setImageDurationMs((clip.effectiveTrimEndMs - clip.effectiveTrimStartMs).coerceAtLeast(1L))
                } else {
                    builder.setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.effectiveTrimStartMs)
                            .setEndPositionMs(clip.effectiveTrimEndMs.coerceAtLeast(clip.effectiveTrimStartMs + 1L))
                            .build()
                    )
                }
                builder.build()
            }
            val wasPlaying = exoPlayer.isPlaying
            val globalPos = currentPositionMs
            
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
            
            bgExoPlayer.setMediaItems(mediaItems)
            bgExoPlayer.prepare()
            
            seekToGlobal(globalPos, exoPlayer)
            seekToGlobal(globalPos, bgExoPlayer)
            if (wasPlaying) {
                exoPlayer.play()
                bgExoPlayer.play()
            }
        }
    }

    LaunchedEffect(projectId) {
        val loadedProject = projectViewModel.getProject(projectId)
        
        loadedProject?.let {
            // Restore from history
            if (it.historyState.isNotBlank() && !it.historyState.startsWith("Auto-saved")) {
                try {
                    val historyModel = editorHistoryMoshi.adapter(EditorHistoryModel::class.java).fromJson(it.historyState)
                    if (historyModel != null) {
                        undoStack = historyModel.undoStack.takeLast(MAX_EDITOR_HISTORY_DEPTH)
                        redoStack = historyModel.redoStack.takeLast(MAX_EDITOR_HISTORY_DEPTH)
                        if (historyModel.currentState != null) {
                            clips = historyModel.currentState.clips
                            canvasSettings = historyModel.currentState.canvasSettings
                            overlays = historyModel.currentState.overlays ?: emptyList()
                            texts = historyModel.currentState.texts ?: emptyList()
                            captions = historyModel.currentState.captions ?: emptyList()
                            captionSettings = historyModel.currentState.captionSettings ?: CaptionSettings()
                            stickers = historyModel.currentState.stickers ?: emptyList()
                            drawings = historyModel.currentState.drawings ?: emptyList()
                            frames = historyModel.currentState.frames ?: emptyList()
                            audioClips = historyModel.currentState.audioClips ?: emptyList()
                            layerOrder = historyModel.currentState.layerOrder ?: emptyList()
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            
            // Opening a project is not an edit. Keep the persisted dirty flag so
            // recovery only represents an interrupted edit or an actual change.
            val updated = it.copy(lastEdited = System.currentTimeMillis())
            projectViewModel.updateProject(updated)
            project = updated
        }
        
        if (autoSaveIntervalMs > 0) {
            while (true) {
                delay(autoSaveIntervalMs)
                project?.let { currentProject ->
                    // Just update timestamp, history is auto-saved in saveState()
                    val savedProject = currentProject.copy(
                        lastEdited = System.currentTimeMillis()
                    )
                    projectViewModel.autoSaveProject(savedProject)
                    project = savedProject
                    
                    showSavedIndicator = true
                    delay(2000)
                    showSavedIndicator = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val bgSyncThresh = 200L
            if (bgExoPlayer.currentWindowIndex != exoPlayer.currentWindowIndex || Math.abs(exoPlayer.currentPosition - bgExoPlayer.currentPosition) > bgSyncThresh) {
                bgExoPlayer.seekTo(exoPlayer.currentWindowIndex, exoPlayer.currentPosition)
            }
            if (exoPlayer.isPlaying != bgExoPlayer.isPlaying) {
                if (exoPlayer.isPlaying) bgExoPlayer.play() else bgExoPlayer.pause()
            }
            if (exoPlayer.playbackParameters != bgExoPlayer.playbackParameters) {
                bgExoPlayer.playbackParameters = exoPlayer.playbackParameters
            }

            if (clips.isNotEmpty()) {
                val windowIndex = exoPlayer.currentWindowIndex
                if (windowIndex in clips.indices) {
                    val clip = clips[windowIndex]
                    val srcDur = Math.max(1L, clip.trimEndMs - clip.trimStartMs)
                    val currentFraction = (exoPlayer.currentPosition.toFloat() / srcDur).coerceIn(0f, 1f)
                    
                    val currentSpeed = if (clip.speedCurve != null && clip.speedCurve.points.isNotEmpty()) {
                        clip.speedCurve.getSpeedAt(currentFraction)
                    } else {
                        clip.playbackSpeed
                    }
                    
                    val params = androidx.media3.common.PlaybackParameters(currentSpeed, if (clip.maintainPitch) 1f else currentSpeed)
                    if (exoPlayer.playbackParameters.speed != params.speed || exoPlayer.playbackParameters.pitch != params.pitch) {
                        exoPlayer.playbackParameters = params
                    }
                    if (isPlaying) {
                        currentPositionMs = timelineMapper.globalPositionForPlayer(
                            windowIndex,
                            exoPlayer.currentPosition
                        )
                    }
                }
            } else if (isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
            }
            if (isPlaying) {
                val secs = currentPositionMs / 1000
                val m = secs / 60
                val s = secs % 60
                currentTime = String.format("%02d:%02d", m, s)
            }
            delay(50)
        }
    }
    
    // Also track scroll for timeline
    val timelineScrollState = rememberScrollState()
    
    // Format duration string
    val durationSecs = videoDurationMs / 1000
    val durationText = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60)

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizePercent(0.15) // Limit to 15% for 4GB RAM devices
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("thumbnail_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var showSpeedPanel by remember { mutableStateOf(false) }
        var isCurveMode by remember { mutableStateOf(false) }
        var showOverlayPanel by remember { mutableStateOf(false) }
        var showTransformPanel by remember { mutableStateOf(false) }
        var showLayerPanel by remember { mutableStateOf(false) }
        var showFiltersPanel by remember { mutableStateOf(false) }
        var showAdjustPanel by remember { mutableStateOf(false) }
        var showLutPanel by remember { mutableStateOf(false) }
        var showEffectsPanel by remember { mutableStateOf(false) }
        var showMediaPicker by remember { mutableStateOf(false) }
        var isPickingOverlay by remember { mutableStateOf(false) }
        var showTransitionPickerForClipId by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(projectId) {
            if (project != null) {
                try {
                    val json = org.json.JSONObject(project!!.historyState)
                    if (json.has("zoom")) zoom = json.getDouble("zoom").toFloat()
                    if (json.has("pos")) currentPositionMs = json.getLong("pos").coerceAtLeast(0L)
                    // Do not reopen editor panels from stale history. Unsupported
                    // layers and effects are intentionally not exposed by the
                    // current export-safe editor surface.
                    activeTool = null
                } catch (e: Exception) {}
            }
        }

        val saveDraft = {
            project?.let { p ->
                val json = org.json.JSONObject()
                try {
                    if (p.historyState.isNotEmpty()) {
                        val oldJson = org.json.JSONObject(p.historyState)
                        oldJson.keys().forEach { json.put(it, oldJson.get(it)) }
                    }
                } catch (e: Exception) {}
                json.put("zoom", zoom.toDouble())
                json.put("pos", currentPositionMs)
                json.put("tool", activeTool ?: "")
                
                val finalProject = p.copy(
                    isDirty = false, 
                    lastEdited = System.currentTimeMillis(), 
                    projectState = "Draft",
                    historyState = json.toString()
                )
                projectViewModel.updateProject(finalProject)
                android.widget.Toast.makeText(context, "Saved Draft", android.widget.Toast.LENGTH_SHORT).show()
            }
            onBack()
        }

        
        val context = androidx.compose.ui.platform.LocalContext.current
        val lutFilePicker = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                // Parse the LUT
                val parsedMatrix = parseCubeLut(context, uri)
                if (parsedMatrix != null) {
                    val lutId = java.util.UUID.randomUUID().toString()
                    val lutName = uri.lastPathSegment ?: "Custom LUT"
                    
                    val newLut = LutPreset(lutId, lutName, parsedMatrix, true, uri.toString())
                    LutsManager.importedLuts.add(newLut)
                    
                    // Apply to current clip
                    val clipId = selectedClipId
                    val clipIndex = clips.indexOfFirst { it.id == clipId }
                    val clip = clips.getOrNull(clipIndex)
                    if (clip != null) {
                        val newClips = clips.toMutableList()
                        val newAdjustments = clip.adjustments.copy(lutPresetId = lutId, lutIntensity = 1f)
                        newClips[clipIndex] = clip.copy(adjustments = newAdjustments)
                        clips = newClips
                        saveState(newClips, canvasSettings, "Apply Custom LUT")
                    }
                } else {
                    android.widget.Toast.makeText(context, "Could not parse LUT file", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        var isComparePressed by remember { mutableStateOf(false) }

    Scaffold(
            topBar = {
            if (!isScreenshotMode) {
                TopAppBar(
                    title = { Text(project?.name ?: "Loading...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            project?.let {
                                val finalProject = it.copy(isDirty = false, lastEdited = System.currentTimeMillis())
                                projectViewModel.updateProject(finalProject)
                            }
                            onBack()
                        }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        var showUndoDropdown by remember { mutableStateOf(false) }

                    var showRedoDropdown by remember { mutableStateOf(false) }

                    Box {
                        IconButton(
                            onClick = { 
                                if (undoStack.isNotEmpty()) {
                                    val previousState = undoStack.last()
                                    // When we undo, we save current state to redo stack
                                    redoStack = (redoStack + HistoryAction(EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder), "Redo " + previousState.description.replace("Undid ", ""))).takeLast(MAX_EDITOR_HISTORY_DEPTH)
                                    undoStack = undoStack.dropLast(1)
                                    clips = previousState.state.clips
                                    canvasSettings = previousState.state.canvasSettings
                                    overlays = previousState.state.overlays
                                    texts = previousState.state.texts
                                    captions = previousState.state.captions
                                    captionSettings = previousState.state.captionSettings
                                    stickers = previousState.state.stickers
                                    drawings = previousState.state.drawings
                                    frames = previousState.state.frames
                                    audioClips = previousState.state.audioClips ?: emptyList()
                                    layerOrder = previousState.state.layerOrder
                                    
                                    // Make sure ExoPlayer refreshes
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                        isPlaying = false
                                    }
                                    persistHistory()
                                }
                            },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.pointerInput(undoStack) {
                                detectTapGestures(
                                    onLongPress = { 
                                        if (undoStack.isNotEmpty()) showUndoDropdown = true 
                                    }
                                )
                            }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (undoStack.isNotEmpty()) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(undoStack.size.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Undo, contentDescription = "Undo")
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showUndoDropdown,
                            onDismissRequest = { showUndoDropdown = false }
                        ) {
                            undoStack.reversed().forEachIndexed { index, historyItem ->
                                DropdownMenuItem(
                                    text = { Text("Undo: ${historyItem.description}") },
                                    onClick = {
                                        val itemsToUndo = index + 1
                                        val tailActions = undoStack.takeLast(itemsToUndo)
                                        
                                        // Save current state into redo stack first
                                        var currentRedoList = redoStack.toMutableList()
                                        currentRedoList.add(HistoryAction(EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder), "Jump forward"))
                                        
                                        redoStack = currentRedoList
                                        undoStack = undoStack.dropLast(itemsToUndo)
                                        
                                        clips = historyItem.state.clips
                                        canvasSettings = historyItem.state.canvasSettings
                                        overlays = historyItem.state.overlays
                                        texts = historyItem.state.texts
                                        captions = historyItem.state.captions
                                        captionSettings = historyItem.state.captionSettings
                                        stickers = historyItem.state.stickers
                                        drawings = historyItem.state.drawings
                                        frames = historyItem.state.frames
                                        audioClips = historyItem.state.audioClips ?: emptyList()
                                        layerOrder = historyItem.state.layerOrder
                                        showUndoDropdown = false
                                        persistHistory()
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { 
                                if (redoStack.isNotEmpty()) {
                                    val nextState = redoStack.last()
                                    undoStack = (undoStack + HistoryAction(EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder), "Undid " + nextState.description.replace("Redo ", ""))).takeLast(MAX_EDITOR_HISTORY_DEPTH)
                                    redoStack = redoStack.dropLast(1)
                                    clips = nextState.state.clips
                                    canvasSettings = nextState.state.canvasSettings
                                    overlays = nextState.state.overlays
                                    texts = nextState.state.texts
                                    captions = nextState.state.captions
                                    captionSettings = nextState.state.captionSettings
                                    stickers = nextState.state.stickers
                                    drawings = nextState.state.drawings
                                    frames = nextState.state.frames
                                    audioClips = nextState.state.audioClips ?: emptyList()
                                    layerOrder = nextState.state.layerOrder
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                        isPlaying = false
                                    }
                                }
                            },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.pointerInput(redoStack) {
                                detectTapGestures(
                                    onLongPress = { 
                                        if (redoStack.isNotEmpty()) showRedoDropdown = true 
                                    }
                                )
                            }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (redoStack.isNotEmpty()) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                            Text(redoStack.size.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Redo, contentDescription = "Redo")
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showRedoDropdown,
                            onDismissRequest = { showRedoDropdown = false }
                        ) {
                            redoStack.reversed().forEachIndexed { index, historyItem ->
                                DropdownMenuItem(
                                    text = { Text(historyItem.description) },
                                    onClick = {
                                        val itemsToRedo = index + 1
                                        
                                        var currentUndoList = undoStack.toMutableList()
                                        currentUndoList.add(HistoryAction(EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder), "Jump backward"))
                                        
                                        undoStack = currentUndoList
                                        redoStack = redoStack.dropLast(itemsToRedo)
                                        
                                        clips = historyItem.state.clips
                                        canvasSettings = historyItem.state.canvasSettings
                                        overlays = historyItem.state.overlays
                                        texts = historyItem.state.texts
                                        captions = historyItem.state.captions
                                        captionSettings = historyItem.state.captionSettings
                                        stickers = historyItem.state.stickers
                                        drawings = historyItem.state.drawings
                                        frames = historyItem.state.frames
                                        audioClips = historyItem.state.audioClips ?: emptyList()
                                        layerOrder = historyItem.state.layerOrder
                                        showRedoDropdown = false
                                        persistHistory()
                                    }
                                )
                            }
                        }
                        
                        // Keyframe Toggle Button
                        if (selectedClipId != null || selectedOverlayId != null) {
                            val hasKeyframeHere = remember(selectedClipId, selectedOverlayId, currentPositionMs, clips, overlays) {
                                var found = false
                                if (selectedClipId != null) {
                                    val tc = clips.find { it.id == selectedClipId }
                                    if (tc != null) {
                                        var accumT = 0L
                                        for (c in clips) {
                                            if (c.id == tc.id) break
                                            accumT += c.durationMs
                                        }
                                        val relT = currentPositionMs - accumT
                                        found = tc.keyframes.values.any { kfs -> kfs.any { Math.abs(it.timeMs - relT) < 50 } }
                                    }
                                } else if (selectedOverlayId != null) {
                                    val to = overlays.find { it.id == selectedOverlayId }
                                    if (to != null) {
                                        val relT = currentPositionMs - to.startTimeOnTimelineMs
                                        found = to.keyframes.values.any { kfs -> kfs.any { Math.abs(it.timeMs - relT) < 50 } }
                                    }
                                }
                                found
                            }
                            
                            var showEasingMenu by remember { mutableStateOf(false) }
                            
                            IconButton(onClick = {
                                if (hasKeyframeHere) {
                                    // Remove keyframe logic...
                                    if (selectedClipId != null) {
                                        val idx = clips.indexOfFirst { it.id == selectedClipId }
                                        if (idx != -1) {
                                            val tc = clips[idx]
                                            var accumT = 0L
                                            for (c in clips) {
                                                if (c.id == tc.id) break
                                                accumT += c.durationMs
                                            }
                                            val relT = currentPositionMs - accumT
                                            val mKfs = tc.keyframes.toMutableMap()
                                            for (p in mKfs.keys) {
                                                mKfs[p] = mKfs[p]!!.filter { Math.abs(it.timeMs - relT) >= 50 }
                                            }
                                            val mClips = clips.toMutableList()
                                            mClips[idx] = tc.copy(keyframes = mKfs)
                                            saveState(mClips, canvasSettings, "Remove Keyframe")
                                        }
                                    } else if (selectedOverlayId != null) {
                                        val idx = overlays.indexOfFirst { it.id == selectedOverlayId }
                                        if (idx != -1) {
                                            val to = overlays[idx]
                                            val relT = currentPositionMs - to.startTimeOnTimelineMs
                                            val mKfs = to.keyframes.toMutableMap()
                                            for (p in mKfs.keys) {
                                                mKfs[p] = mKfs[p]!!.filter { Math.abs(it.timeMs - relT) >= 50 }
                                            }
                                            val mOvers = overlays.toMutableList()
                                            mOvers[idx] = to.copy(keyframes = mKfs)
                                            saveOverlayState(mOvers, "Remove Keyframe")
                                        }
                                    }
                                } else {
                                    // Add keyframe logic...
                                    if (selectedClipId != null) {
                                        val idx = clips.indexOfFirst { it.id == selectedClipId }
                                        if (idx != -1) {
                                            val tc = clips[idx]
                                            var accumT = 0L
                                            for (c in clips) {
                                                if (c.id == tc.id) break
                                                accumT += c.durationMs
                                            }
                                            val relT = currentPositionMs - accumT
                                            val mKfs = tc.keyframes.toMutableMap()
                                            val props = listOf("posX", "posY", "scale", "rotation", "cropLeft", "cropTop", "cropRight", "cropBottom")
                                            for (p in props) {
                                                val v = mKfs.getValueAtTime(p, relT, when(p) { "posX", "posY" -> 0.5f; "scale" -> 1f; "cropRight", "cropBottom" -> 1f; else -> 0f })
                                                val lst = mKfs.getOrDefault(p, emptyList()).toMutableList()
                                                lst.add(Keyframe(timeMs = relT, value = v))
                                                mKfs[p] = lst
                                            }
                                            val mClips = clips.toMutableList()
                                            mClips[idx] = tc.copy(keyframes = mKfs)
                                            saveState(mClips, canvasSettings, "Add Keyframe")
                                        }
                                    } else if (selectedOverlayId != null) {
                                        val idx = overlays.indexOfFirst { it.id == selectedOverlayId }
                                        if (idx != -1) {
                                            val to = overlays[idx]
                                            val relT = currentPositionMs - to.startTimeOnTimelineMs
                                            val mKfs = to.keyframes.toMutableMap()
                                            val props = listOf("posX", "posY", "scaleX", "scaleY", "rotation", "opacity")
                                            for (p in props) {
                                                val v = mKfs.getValueAtTime(p, relT, when(p) { "posX", "posY", "scaleX", "scaleY" -> 0.5f; "opacity" -> 1f; else -> 0f })
                                                val lst = mKfs.getOrDefault(p, emptyList()).toMutableList()
                                                lst.add(Keyframe(timeMs = relT, value = v))
                                                mKfs[p] = lst
                                            }
                                            val mOvers = overlays.toMutableList()
                                            mOvers[idx] = to.copy(keyframes = mKfs)
                                            saveOverlayState(mOvers, "Add Keyframe")
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Keyframe",
                                    tint = if (hasKeyframeHere) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            if (hasKeyframeHere) {
                                IconButton(onClick = { showEasingMenu = true }) {
                                    Icon(Icons.Filled.Tune, contentDescription = "Easing", modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(expanded = showEasingMenu, onDismissRequest = { showEasingMenu = false }) {
                                    val types = EasingType.values()
                                    for (t in types) {
                                        DropdownMenuItem(
                                            text = { Text(t.name) },
                                            onClick = {
                                                showEasingMenu = false
                                                if (selectedClipId != null) {
                                                    val idx = clips.indexOfFirst { it.id == selectedClipId }
                                                    if (idx != -1) {
                                                        val tc = clips[idx]
                                                        var accumT = 0L
                                                        for (c in clips) {
                                                            if (c.id == tc.id) break
                                                            accumT += c.durationMs
                                                        }
                                                        val relT = currentPositionMs - accumT
                                                        val mKfs = tc.keyframes.toMutableMap()
                                                        for (p in mKfs.keys) {
                                                            mKfs[p] = mKfs[p]!!.map { if (Math.abs(it.timeMs - relT) < 50) it.copy(easing = t) else it }
                                                        }
                                                        val mClips = clips.toMutableList()
                                                        mClips[idx] = tc.copy(keyframes = mKfs)
                                                        saveState(mClips, canvasSettings, "Change Keyframe Easing")
                                                    }
                                                } else if (selectedOverlayId != null) {
                                                    val idx = overlays.indexOfFirst { it.id == selectedOverlayId }
                                                    if (idx != -1) {
                                                        val to = overlays[idx]
                                                        val relT = currentPositionMs - to.startTimeOnTimelineMs
                                                        val mKfs = to.keyframes.toMutableMap()
                                                        for (p in mKfs.keys) {
                                                            mKfs[p] = mKfs[p]!!.map { if (Math.abs(it.timeMs - relT) < 50) it.copy(easing = t) else it }
                                                        }
                                                        val mOvers = overlays.toMutableList()
                                                        mOvers[idx] = to.copy(keyframes = mKfs)
                                                        saveOverlayState(mOvers, "Change Keyframe Easing")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(onClick = { 
                        isPickingOverlay = false
                        showMediaPicker = true 
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Media")
                    }
                    IconButton(onClick = { showLayerPanel = true }) {
                        Icon(Icons.Filled.Layers, contentDescription = "Layers")
                    }
                    TextButton(onClick = saveDraft) {
                        Text("Save Draft", color = MaterialTheme.colorScheme.primary)
                    }
                    FilledTonalButton(
                        onClick = { 
                            persistHistory()
                            showExportPanel = true 
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                    ) {
                        Text("Export", fontWeight = FontWeight.Bold)
                    }

                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Screenshot Mode") },
                                onClick = {
                                    showMoreMenu = false
                                    isScreenshotMode = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isScreenshotMode) {
            // Bottom Editor Toolbar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tools = mutableListOf(
                        "Split" to Icons.Filled.ContentCut,
                        "Mute" to Icons.Filled.VolumeOff,
                        "Music" to Icons.Filled.LibraryMusic,
                        "Voiceover" to Icons.Filled.RecordVoiceOver,
                    )
                    items(tools.size) { index ->
                        val toolName = tools[index].first
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { 
                                activeTool = null
                                when (toolName) {
                                    "Mute" -> {
                                        val clipIndex = clips.indexOfFirst { it.id == selectedClipId }
                                        if (clipIndex >= 0) {
                                            val clip = clips[clipIndex]
                                            val newClips = clips.toMutableList()
                                            newClips[clipIndex] = clip.copy(isMuted = !clip.isMuted)
                                            saveState(newClips, canvasSettings, if (clip.isMuted) "Unmute clip" else "Mute clip")
                                        } else {
                                            android.widget.Toast.makeText(context, "Select a video clip first.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "Split" -> if (clips.isNotEmpty()) {
                                        val timelinePosition = timelineMapper.locate(currentPositionMs)
                                        val clipIndex = timelinePosition?.clipIndex ?: -1
                                        val clip = clips.getOrNull(clipIndex)
                                        if (clip != null && timelinePosition != null &&
                                            timelinePosition.timelineOffsetMs > 0L &&
                                            timelinePosition.timelineOffsetMs < clip.durationMs
                                        ) {
                                            val sourceSplitTimeMs = timelinePosition.sourcePositionMs
                                            if (sourceSplitTimeMs > clip.trimStartMs && sourceSplitTimeMs < clip.trimEndMs) {
                                                val clip1 = clip.copy(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    trimEndMs = sourceSplitTimeMs,
                                                    transitionNext = Transition()
                                                )
                                                val clip2 = clip.copy(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    trimStartMs = sourceSplitTimeMs
                                                )

                                                val newClips = clips.toMutableList()
                                                newClips.removeAt(clipIndex)
                                                newClips.add(clipIndex, clip1)
                                                newClips.add(clipIndex + 1, clip2)
                                                saveState(newClips, canvasSettings, "Split clip")
                                                com.example.utils.HapticUtil.playSharpTap(view, context)
                                            }
                                        }
                                    }
                                    "Music" -> musicPicker.launch(arrayOf("audio/*"))
                                    "Voiceover" -> if (isRecordingVoiceover) {
                                        stopVoiceoverRecording()
                                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        startVoiceoverRecording()
                                    } else {
                                        recordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        ) {
                            Box {
                                Icon(
                                    if (toolName == "Voiceover" && isRecordingVoiceover) Icons.Filled.Stop else tools[index].second,
                                    contentDescription = toolName,
                                    modifier = Modifier.padding(8.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(text = if (toolName == "Voiceover" && isRecordingVoiceover) "Stop Voiceover" else toolName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            
            // Render audio players
            for (audioClip in audioClips) {
                AudioPlayerComponent(
                    clip = audioClip,
                    clips = clips,
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    masterVolume = canvasSettings.masterVolume
                )
            }
            
            // Video Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isScreenshotMode) 1f else 0.55f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val aspectFloat = canvasSettings.aspectOption.ratio
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(aspectFloat)
                        .clipToBounds()
                        .background(Color.Transparent)
                        .clickable {
                            isFullscreen = true
                            showFullscreenControls = true
                        }
                ) {
                    // Render Background
                    if (canvasSettings.fitMode == FitMode.Fit) {
                        val bgMod = Modifier.fillMaxSize()
                        when (canvasSettings.backgroundType) {
                            BackgroundType.Color -> {
                                Box(bgMod.background(canvasSettings.backgroundColor))
                            }
                            BackgroundType.Gradient -> {
                                Box(bgMod.background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(canvasSettings.backgroundColor, Color.Black))))
                            }
                            BackgroundType.Blur -> {
                                AndroidView(
                                    factory = { ctx ->
                                        androidx.media3.ui.PlayerView(ctx).apply {
                                            player = bgExoPlayer
                                            useController = false
                                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        }
                                    },
                                    modifier = bgMod.blur(24.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded).graphicsLayer { alpha = 0.5f }
                                )
                            }
                        }
                    }

                    val currentClipForTransform = if (clips.isNotEmpty() && exoPlayer.currentWindowIndex in clips.indices) clips[exoPlayer.currentWindowIndex] else null

                    val mediaPath = project?.sourceMediaPaths?.firstOrNull()
                    if (mediaPath != null) {
                        var relativeTimeForClip = 0L
                        if (currentClipForTransform != null) {
                            var accumT = 0L
                            for (c in clips) {
                                if (c.id == currentClipForTransform.id) {
                                    relativeTimeForClip = currentPositionMs - accumT
                                    break
                                }
                                accumT += c.durationMs
                            }
                        }
                        
                        LaunchedEffect(isMuted, currentClipForTransform?.isMuted, currentClipForTransform?.volume, canvasSettings.masterVolume) {
                            val clipVolume = currentClipForTransform?.volume ?: 1f
                            exoPlayer.volume = if (isMuted || currentClipForTransform?.isMuted == true) {
                                0f
                            } else {
                                (clipVolume * canvasSettings.masterVolume).coerceIn(0f, 1f)
                            }
                        }
                        
                        val isPreviewingCrop = showTransformPanel && selectedClipId == currentClipForTransform?.id
                        
                        val tRot = currentClipForTransform?.keyframes?.getValueAtTime("rotation", relativeTimeForClip, currentClipForTransform.rotation) ?: 0f
                        val animRotation by animateFloatAsState(tRot, label = "rotation", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("rotation") == true) snap() else tween(500))
                        
                        // We calculate the final scale and translation first because we want to animate those too
                        var targetScaleX = if (currentClipForTransform?.flipHorizontal == true) -1f else 1f
                        var targetScaleY = if (currentClipForTransform?.flipVertical == true) -1f else 1f
                        
                        val tScaleRaw = currentClipForTransform?.keyframes?.getValueAtTime("scale", relativeTimeForClip, currentClipForTransform.scale) ?: 1f
                        targetScaleX *= tScaleRaw
                        targetScaleY *= tScaleRaw
                        
                        val tPosX = currentClipForTransform?.keyframes?.getValueAtTime("posX", relativeTimeForClip, currentClipForTransform.posX) ?: 0.5f
                        val tPosY = currentClipForTransform?.keyframes?.getValueAtTime("posY", relativeTimeForClip, currentClipForTransform.posY) ?: 0.5f
                        
                        var activeTransitionType by remember { mutableStateOf<TransitionType?>(null) }
                        var transitionProgress by remember { mutableStateOf(0f) } // -1f to 1f
                        
                        LaunchedEffect(currentPositionMs, clips) {
                            var accum = 0L
                            var foundTransition: TransitionType? = null
                            var foundProgress = 0f
                            for (i in 0 until clips.lastIndex) {
                                val clip = clips[i]
                                val trans = clip.transitionNext
                                val cutPoint = accum + clip.durationMs
                                if (trans.type != TransitionType.NONE) {
                                    val tHalf = trans.durationMs / 2L
                                    val tStart = cutPoint - tHalf
                                    val tEnd = cutPoint + tHalf
                                    if (currentPositionMs in tStart..tEnd) {
                                        foundTransition = trans.type
                                        foundProgress = (currentPositionMs - cutPoint).toFloat() / tHalf.toFloat()
                                        break
                                    }
                                }
                                accum += clip.durationMs
                            }
                            activeTransitionType = foundTransition
                            transitionProgress = foundProgress
                        }
                        
                        // We need the layout size for translation, but we can't get size cleanly here before layout.
                        // However, we can use graphicsLayer properties, but animation requires state outside.
                        // Actually, calculating translation requires `this.size.width` which is inside graphicsLayer.
                        // Is it okay if crops jump? The requirement says 'animate the change being reverted'.
                        // Animating rotation and flip scale will be super noticeable and satisfy it!
                        
                        val animScaleX by animateFloatAsState(targetScaleX, label = "scaleX", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("scale") == true) snap() else tween(500))
                        val animScaleY by animateFloatAsState(targetScaleY, label = "scaleY", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("scale") == true) snap() else tween(500))
                        val animPosX by animateFloatAsState(tPosX, label = "posX", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("posX") == true) snap() else tween(500))
                        val animPosY by animateFloatAsState(tPosY, label = "posY", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("posY") == true) snap() else tween(500))
                        
                        val tCropLeft = currentClipForTransform?.keyframes?.getValueAtTime("cropLeft", relativeTimeForClip, currentClipForTransform.cropRect.left) ?: 0f
                        val tCropTop = currentClipForTransform?.keyframes?.getValueAtTime("cropTop", relativeTimeForClip, currentClipForTransform.cropRect.top) ?: 0f
                        val tCropRight = currentClipForTransform?.keyframes?.getValueAtTime("cropRight", relativeTimeForClip, currentClipForTransform.cropRect.right) ?: 1f
                        val tCropBottom = currentClipForTransform?.keyframes?.getValueAtTime("cropBottom", relativeTimeForClip, currentClipForTransform.cropRect.bottom) ?: 1f
                        
                        val animCropLeft by animateFloatAsState(tCropLeft, label = "cropLeft", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("cropLeft") == true) snap() else tween(500))
                        val animCropTop by animateFloatAsState(tCropTop, label = "cropTop", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("cropTop") == true) snap() else tween(500))
                        val animCropRight by animateFloatAsState(tCropRight, label = "cropRight", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("cropRight") == true) snap() else tween(500))
                        val animCropBottom by animateFloatAsState(tCropBottom, label = "cropBottom", animationSpec = if (isPlaying && currentClipForTransform?.keyframes?.containsKey("cropBottom") == true) snap() else tween(500))
                        
                        // Filter
                        val filterType = currentClipForTransform?.filterType ?: FilterType.NONE
                        val filterIntensity = currentClipForTransform?.filterIntensity ?: 1f
                        val filterColorMatrix = getColorMatrixForFilter(filterType, filterIntensity)
                        var finalColorMatrix = filterColorMatrix
                        val adjustments = currentClipForTransform?.adjustments ?: ColorAdjustments()
                        val isComparing = isComparePressed
                        if (!isComparing) {
                            val adjustMatrix = getColorMatrixForAdjustments(adjustments)
                            finalColorMatrix = androidx.compose.ui.graphics.ColorMatrix().apply {
                                timesAssign(filterColorMatrix)
                                timesAssign(adjustMatrix)
                            }
                        }
                        
                        AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                }
                            },
                            update = { view ->
                                view.resizeMode = when (canvasSettings.fitMode) {
                                    FitMode.Fit -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    FitMode.Fill -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    FitMode.Stretch -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(currentClipForTransform?.id) {
                                    if (selectedClipId != null && selectedClipId == currentClipForTransform?.id && !isPreviewingCrop) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val finalClips = clips.toMutableList()
                                            val idx = finalClips.indexOfFirst { it.id == selectedClipId }
                                            if (idx != -1) {
                                                val c = finalClips[idx]
                                                val mKfs = c.keyframes.toMutableMap()
                                                var accumT = 0L
                                                for (clipIt in clips) {
                                                    if (clipIt.id == c.id) break
                                                    accumT += clipIt.durationMs
                                                }
                                                val relT = currentPositionMs - accumT
                                                
                                                val currX = mKfs.getValueAtTime("posX", relT, c.posX)
                                                val currY = mKfs.getValueAtTime("posY", relT, c.posY)
                                                val currScale = mKfs.getValueAtTime("scale", relT, c.scale)
                                                
                                                val newX = currX + (pan.x / w)
                                                val newY = currY + (pan.y / h)
                                                val newScale = (currScale * zoom).coerceIn(0.05f, 10f)
                                                
                                                val props = listOf("posX" to newX, "posY" to newY, "scale" to newScale)
                                                var updatedKeyframes = false
                                                for ((pName, pVal) in props) {
                                                    val lst = mKfs[pName]
                                                    if (lst != null && lst.isNotEmpty()) {
                                                        updatedKeyframes = true
                                                        val existingIdx = lst.indexOfFirst { Math.abs(it.timeMs - relT) < 50 }
                                                        val mLst = lst.toMutableList()
                                                        if (existingIdx != -1) {
                                                            mLst[existingIdx] = mLst[existingIdx].copy(value = pVal)
                                                        } else {
                                                            mLst.add(Keyframe(timeMs = relT, value = pVal))
                                                            mLst.sortBy { it.timeMs }
                                                        }
                                                        mKfs[pName] = mLst
                                                    }
                                                }
                                                
                                                if (updatedKeyframes) {
                                                    finalClips[idx] = c.copy(keyframes = mKfs)
                                                } else {
                                                    finalClips[idx] = c.copy(posX = newX, posY = newY, scale = newScale)
                                                }
                                                clips = finalClips
                                            }
                                        }
                                    }
                                }
                                .colorFilterOverlay(finalColorMatrix)
                                .then(if (!isComparing && !skipHeavyEffects) Modifier.vignetteAndGrain(adjustments.vignette, adjustments.grain) else Modifier)
                                .applyAllVisualEffects(if (!isComparing && !skipHeavyEffects && currentClipForTransform != null) currentClipForTransform.effects else emptyList(), currentPositionMs, currentClipForTransform?.durationMs ?: 0L)
                                .graphicsLayer {
                                    if (currentClipForTransform != null) {
                                        this.rotationZ = animRotation
                                        this.scaleX = animScaleX
                                        this.scaleY = animScaleY
                                        this.translationX = (animPosX - 0.5f) * this.size.width
                                        this.translationY = (animPosY - 0.5f) * this.size.height
                                        
                                        if (activeTransitionType != null) {
                                            val tType = if (isBudgetMode) TransitionType.CROSSFADE else activeTransitionType
                                            val p = transitionProgress
                                            val isOld = p < 0f
                                            
                                            when (tType) {
                                                TransitionType.CROSSFADE -> {
                                                    this.alpha = if (isOld) -p else p 
                                                }
                                                TransitionType.SLIDE_LEFT, TransitionType.PUSH_LEFT -> {
                                                    this.translationX = if (isOld) (1f + p) * -this.size.width else (1f - p) * this.size.width
                                                }
                                                TransitionType.SLIDE_RIGHT, TransitionType.PUSH_RIGHT -> {
                                                    this.translationX = if (isOld) (1f + p) * this.size.width else (1f - p) * -this.size.width
                                                }
                                                TransitionType.SLIDE_UP -> {
                                                    this.translationY = if (isOld) (1f + p) * -this.size.height else (1f - p) * this.size.height
                                                }
                                                TransitionType.SLIDE_DOWN -> {
                                                    this.translationY = if (isOld) (1f + p) * this.size.height else (1f - p) * -this.size.height
                                                }
                                                TransitionType.ZOOM_IN -> {
                                                    val s = if (isOld) 1f + (1f + p) else p
                                                    this.scaleX *= s
                                                    this.scaleY *= s
                                                }
                                                TransitionType.ZOOM_OUT -> {
                                                    val s = if (isOld) -p else 2f - p
                                                    this.scaleX *= s
                                                    this.scaleY *= s
                                                }
                                                TransitionType.SPIN -> {
                                                    this.rotationZ += if (isOld) (1f + p) * 180f else (p - 1f) * 180f
                                                    val s = if (isOld) -p else p
                                                    this.scaleX *= s
                                                    this.scaleY *= s
                                                }
                                                TransitionType.FLIP -> {
                                                    this.rotationY += if (isOld) (1f + p) * 90f else (p - 1f) * -90f
                                                }
                                                else -> {}
                                            }
                                        }
                                        
                                        if (!isPreviewingCrop) {
                                            val cWidth = animCropRight - animCropLeft
                                            val cHeight = animCropBottom - animCropTop
                                            if (cWidth < 1f || cHeight < 1f) {
                                                val zoom = maxOf(1f / cWidth, 1f / cHeight)
                                                this.scaleX *= zoom
                                                this.scaleY *= zoom
                                                val cx = animCropLeft + cWidth / 2f
                                                val cy = animCropTop + cHeight / 2f
                                                this.translationX = (0.5f - cx) * this.size.width * zoom
                                                this.translationY = (0.5f - cy) * this.size.height * zoom
                                            }
                                        }
                                    }
                                }
                        )
                        
                        if (activeTransitionType != null) {
                            val tType = if (isBudgetMode) TransitionType.CROSSFADE else activeTransitionType
                            val p = transitionProgress
                            val isOld = p < 0f
                            val absP = Math.abs(p) // 1 at start, 0 at cut, 1 at end
                            
                            when (tType) {
                                TransitionType.FADE_TO_BLACK -> {
                                    val alpha = 1f - absP
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)))
                                }
                                TransitionType.FADE_TO_WHITE -> {
                                    val alpha = 1f - absP
                                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = alpha)))
                                }
                                TransitionType.WIPE_LEFT -> {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                                        val fillWidth = if (isOld) (1f + p) * size.width else (1f - p) * size.width
                                        drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size.width - fillWidth, 0f), size = androidx.compose.ui.geometry.Size(fillWidth, size.height))
                                    }
                                }
                                TransitionType.WIPE_RIGHT -> {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                                        val fillWidth = if (isOld) (1f + p) * size.width else (1f - p) * size.width
                                        drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(fillWidth, size.height))
                                    }
                                }
                                TransitionType.CLOCK_WIPE -> {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                                        val angle = if (isOld) (1f + p) * 360f else (1f - p) * 360f
                                        val radius = kotlin.math.max(size.width, size.height)
                                        drawArc(
                                            color = Color.Black,
                                            startAngle = -90f,
                                            sweepAngle = angle,
                                            useCenter = true,
                                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                            topLeft = androidx.compose.ui.geometry.Offset(size.width / 2f - radius, size.height / 2f - radius)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                        
                        // Crop Overlay
                        if (isPreviewingCrop && currentClipForTransform != null) {
                            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val rect = currentClipForTransform.cropRect
                                    val dx = dragAmount.x / size.width
                                    val dy = dragAmount.y / size.height
                                    
                                    // simple implementation: if tapping near edges, adjust edge. Otherwise move whole rect.
                                    val pos = change.position
                                    val rx = pos.x / size.width
                                    val ry = pos.y / size.height
                                    
                                    val edgeT = 0.05f
                                    val leftDist = Math.abs(rx - rect.left)
                                    val rightDist = Math.abs(rx - rect.right)
                                    val topDist = Math.abs(ry - rect.top)
                                    val bottomDist = Math.abs(ry - rect.bottom)
                                    
                                    var newL = rect.left
                                    var newT = rect.top
                                    var newR = rect.right
                                    var newB = rect.bottom
                                    
                                    if (leftDist < edgeT) newL = (newL + dx).coerceIn(0f, newR - 0.1f)
                                    else if (rightDist < edgeT) newR = (newR + dx).coerceIn(newL + 0.1f, 1f)
                                    else if (topDist < edgeT) newT = (newT + dy).coerceIn(0f, newB - 0.1f)
                                    else if (bottomDist < edgeT) newB = (newB + dy).coerceIn(newT + 0.1f, 1f)
                                    else {
                                        // center drag
                                        newL = (newL + dx).coerceIn(0f, 1f - rect.width)
                                        newR = newL + rect.width
                                        newT = (newT + dy).coerceIn(0f, 1f - rect.height)
                                        newB = newT + rect.height
                                    }
                                    
                                    val newClips = clips.toMutableList()
                                    val idx = newClips.indexOfFirst { it.id == currentClipForTransform.id }
                                    newClips[idx] = currentClipForTransform.copy(cropRect = Rect(newL, newT, newR, newB))
                                    saveState(newClips, canvasSettings, "Crop clip")
                                }
                            }) {
                                val rect = currentClipForTransform.cropRect
                                val l = rect.left * size.width
                                val t = rect.top * size.height
                                val r = rect.right * size.width
                                val b = rect.bottom * size.height
                                
                                // Dimmed background
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                                    addRect(androidx.compose.ui.geometry.Rect(l, t, r, b))
                                    fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                                }
                                drawPath(path, Color.Black.copy(alpha = 0.6f))
                                
                                // Crop border
                                drawRect(
                                    color = Color.White,
                                    topLeft = androidx.compose.ui.geometry.Offset(l, t),
                                    size = androidx.compose.ui.geometry.Size(r - l, b - t),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                )
                                
                                // Rule of thirds grid
                                val thirdW = (r - l) / 3f
                                val thirdH = (b - t) / 3f
                                drawLine(Color.White.copy(alpha=0.5f), androidx.compose.ui.geometry.Offset(l + thirdW, t), androidx.compose.ui.geometry.Offset(l + thirdW, b), 2f)
                                drawLine(Color.White.copy(alpha=0.5f), androidx.compose.ui.geometry.Offset(l + 2*thirdW, t), androidx.compose.ui.geometry.Offset(l + 2*thirdW, b), 2f)
                                drawLine(Color.White.copy(alpha=0.5f), androidx.compose.ui.geometry.Offset(l, t + thirdH), androidx.compose.ui.geometry.Offset(r, t + thirdH), 2f)
                                drawLine(Color.White.copy(alpha=0.5f), androidx.compose.ui.geometry.Offset(l, t + 2*thirdH), androidx.compose.ui.geometry.Offset(r, t + 2*thirdH), 2f)
                            }
                        }
                        
                        // Render Overlays
                        for (overlay in overlays) {
                            if (!overlay.isVisible) continue
                            if (currentPositionMs >= overlay.startTimeOnTimelineMs && currentPositionMs < overlay.startTimeOnTimelineMs + overlay.durationMs) {
                                val isSelected = selectedOverlayId == overlay.id
                                val relativeTimeMs = currentPositionMs - overlay.startTimeOnTimelineMs
                                
                                val animPosX = overlay.keyframes.getValueAtTime("posX", relativeTimeMs, overlay.posX)
                                val animPosY = overlay.keyframes.getValueAtTime("posY", relativeTimeMs, overlay.posY)
                                val animScaleX = overlay.keyframes.getValueAtTime("scaleX", relativeTimeMs, overlay.scaleX)
                                val animScaleY = overlay.keyframes.getValueAtTime("scaleY", relativeTimeMs, overlay.scaleY)
                                val animRotation = overlay.keyframes.getValueAtTime("rotation", relativeTimeMs, overlay.rotation)
                                val animOpacity = overlay.keyframes.getValueAtTime("opacity", relativeTimeMs, overlay.opacity)
                                
                                val cBlendMode = when (overlay.blendMode) {
                                    OverlayBlendModeType.MULTIPLY -> androidx.compose.ui.graphics.BlendMode.Multiply
                                    OverlayBlendModeType.SCREEN -> androidx.compose.ui.graphics.BlendMode.Screen
                                    OverlayBlendModeType.OVERLAY -> androidx.compose.ui.graphics.BlendMode.Overlay
                                    OverlayBlendModeType.SOFT_LIGHT -> androidx.compose.ui.graphics.BlendMode.Softlight
                                    OverlayBlendModeType.HARD_LIGHT -> androidx.compose.ui.graphics.BlendMode.Hardlight
                                    OverlayBlendModeType.DIFFERENCE -> androidx.compose.ui.graphics.BlendMode.Difference
                                    OverlayBlendModeType.ADD -> androidx.compose.ui.graphics.BlendMode.Plus
                                    else -> androidx.compose.ui.graphics.BlendMode.SrcOver
                                }
                                
                                val cMaskShape = when (overlay.maskShape) {
                                    MaskShape.CIRCLE -> CircleShape
                                    MaskShape.RECTANGLE -> RoundedCornerShape(0.dp) // Wait, just no cut or rect
                                    MaskShape.HEART -> RoundedCornerShape(0.dp) // placeholder
                                    MaskShape.STAR -> RoundedCornerShape(0.dp) // placeholder
                                    else -> androidx.compose.ui.graphics.RectangleShape
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex((layerOrder.indexOf(overlay.id).takeIf { it >= 0 } ?: 0).toFloat())
                                        // Pinch/drag input
                                        .pointerInput(overlay.id) {
                                            if (isSelected && !overlay.isLocked) {
                                                detectTransformGestures { _, pan, zoom, rotation ->
                                                    val w = size.width.toFloat()
                                                    val h = size.height.toFloat()
                                                    
                                                    val finalOverlays = overlays.toMutableList()
                                                    val idx = finalOverlays.indexOfFirst { it.id == overlay.id }
                                                    if (idx != -1) {
                                                        val o = finalOverlays[idx]
                                                        
                                                        val relT = currentPositionMs - o.startTimeOnTimelineMs
                                                        val mKfs = o.keyframes.toMutableMap()
                                                        
                                                        val currX = mKfs.getValueAtTime("posX", relT, o.posX)
                                                        val currY = mKfs.getValueAtTime("posY", relT, o.posY)
                                                        val currScaleX = mKfs.getValueAtTime("scaleX", relT, o.scaleX)
                                                        val currScaleY = mKfs.getValueAtTime("scaleY", relT, o.scaleY)
                                                        val currRotation = mKfs.getValueAtTime("rotation", relT, o.rotation)
                                                        
                                                        val newX = currX + (pan.x / w)
                                                        val newY = currY + (pan.y / h)
                                                        val newScaleX = (currScaleX * zoom).coerceIn(0.05f, 5f)
                                                        val newScaleY = (currScaleY * zoom).coerceIn(0.05f, 5f)
                                                        val newRot = currRotation + rotation
                                                        
                                                        val props = listOf("posX" to newX, "posY" to newY, "scaleX" to newScaleX, "scaleY" to newScaleY, "rotation" to newRot)
                                                        var updatedKeyframes = false
                                                        for ((pName, pVal) in props) {
                                                            val lst = mKfs[pName]
                                                            if (lst != null && lst.isNotEmpty()) {
                                                                updatedKeyframes = true
                                                                val existingIdx = lst.indexOfFirst { Math.abs(it.timeMs - relT) < 50 }
                                                                val mLst = lst.toMutableList()
                                                                if (existingIdx != -1) {
                                                                    mLst[existingIdx] = mLst[existingIdx].copy(value = pVal)
                                                                } else {
                                                                    mLst.add(Keyframe(timeMs = relT, value = pVal))
                                                                    mLst.sortBy { it.timeMs }
                                                                }
                                                                mKfs[pName] = mLst
                                                            }
                                                        }
                                                        
                                                        if (updatedKeyframes) {
                                                            finalOverlays[idx] = o.copy(keyframes = mKfs)
                                                        } else {
                                                            finalOverlays[idx] = o.copy(posX = newX, posY = newY, scaleX = newScaleX, scaleY = newScaleY, rotation = newRot)
                                                        }
                                                        overlays = finalOverlays
                                                    }
                                                }
                                            }
                                        }
                                        .graphicsLayer {
                                            translationX = (animPosX - 0.5f) * size.width
                                            translationY = (animPosY - 0.5f) * size.height
                                            scaleX = animScaleX * 2f
                                            scaleY = animScaleY * 2f
                                            rotationZ = animRotation
                                            alpha = animOpacity
                                            
                                            val tIn = relativeTimeMs.toFloat() / 500f
                                            val tOut = (overlay.durationMs - relativeTimeMs).toFloat() / 500f
                                            if (tIn >= 0 && tIn < 1f && overlay.entranceAnim != OverlayAnim.NONE) {
                                                when (overlay.entranceAnim) {
                                                    OverlayAnim.FADE -> alpha *= tIn
                                                    OverlayAnim.SLIDE -> translationY += (1f - tIn) * size.height
                                                    OverlayAnim.SCALE -> { scaleX *= tIn; scaleY *= tIn }
                                                    else -> {}
                                                }
                                            }
                                            if (tOut >= 0 && tOut < 1f && overlay.exitAnim != OverlayAnim.NONE) {
                                                when (overlay.exitAnim) {
                                                    OverlayAnim.FADE -> alpha *= tOut
                                                    OverlayAnim.SLIDE -> translationY += (1f - tOut) * size.height
                                                    OverlayAnim.SCALE -> { scaleX *= tOut; scaleY *= tOut }
                                                    else -> {}
                                                }
                                            }
                                            
                                            if (overlay.shadowRadius > 0f && overlay.shadowColor != Color.Transparent) {
                                                shadowElevation = overlay.shadowRadius
                                                spotShadowColor = overlay.shadowColor
                                                ambientShadowColor = overlay.shadowColor
                                            }
                                        }
                                        .border(
                                            width = overlay.borderWidth.dp,
                                            color = overlay.borderColor,
                                            shape = cMaskShape
                                        )
                                        .clip(cMaskShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (overlay.isPhoto || overlay.isGif) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(context)
                                                .data(overlay.sourceUri)
                                                .decoderFactory(
                                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                                        coil.decode.ImageDecoderDecoder.Factory()
                                                    } else {
                                                        coil.decode.GifDecoder.Factory()
                                                    }
                                                )
                                                .build(),
                                            contentDescription = "Overlay Image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        // We will just show an AsyncImage of the first frame for now 
                                        // since multiple active exoplayers might be heavy. 
                                        // But wait, the user wants animated overlay.
                                        // I'll use the single overlayExoPlayer.
                                        // If this is the active playing overlay, use overlayExoPlayer!
                                        
                                        // But only if it's currently rendering, wait, we can just attach AndroidView
                                        AndroidView(
                                            factory = { ctx ->
                                                androidx.media3.ui.PlayerView(ctx).apply {
                                                    player = overlayExoPlayer
                                                    useController = false
                                                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        
                                        LaunchedEffect(overlay.id, overlay.sourceUri) {
                                            val mi = androidx.media3.common.MediaItem.fromUri(overlay.sourceUri)
                                            overlayExoPlayer.setMediaItem(mi)
                                            overlayExoPlayer.prepare()
                                        }
                                        
                                        LaunchedEffect(relativeTimeMs, isPlaying) {
                                            val overlayPos = overlay.trimStartMs + relativeTimeMs
                                            if (Math.abs(overlayExoPlayer.currentPosition - overlayPos) > 100) {
                                                overlayExoPlayer.seekTo(overlayPos.toLong())
                                            }
                                            if (isPlaying) {
                                                overlayExoPlayer.play()
                                            } else {
                                                overlayExoPlayer.pause()
                                            }
                                        }
                                    }
                                }
                                
                                // Selection Border
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                            .graphicsLayer {
                                                translationX = (animPosX - 0.5f) * size.width
                                                translationY = (animPosY - 0.5f) * size.height
                                                scaleX = animScaleX * 2f
                                                scaleY = animScaleY * 2f
                                                rotationZ = animRotation
                                            }
                                            .border(2.dp, Color.White)
                                    )
                                }
                            }
                        }
                        
                        // Frame Overlays
                        frames.forEach { frameOverlay ->
                            if (!frameOverlay.isVisible) return@forEach
                            if (currentPositionMs >= frameOverlay.startTimeOnTimelineMs && currentPositionMs < frameOverlay.startTimeOnTimelineMs + frameOverlay.durationMs) {
                                FrameRenderer(frameOverlay = frameOverlay, modifier = Modifier.fillMaxSize().zIndex((layerOrder.indexOf(frameOverlay.id).takeIf { it >= 0 } ?: 0).toFloat()).graphicsLayer { alpha = frameOverlay.opacity })
                            }
                        }
                        
                        CaptionRenderer(
                            captions = captions,
                            settings = captionSettings,
                            currentPositionMs = currentPositionMs,
                            modifier = Modifier.zIndex(100f) // Keep captions on top
                        )

                        // Text Overlays
                        texts.forEach { textOverlay ->
                            if (!textOverlay.isVisible) return@forEach
                            if (currentPositionMs >= textOverlay.startTimeOnTimelineMs && currentPositionMs < textOverlay.startTimeOnTimelineMs + textOverlay.durationMs) {
                                val isSelected = textOverlay.id == selectedTextId
                                
                                var currentPosX by remember(textOverlay.id) { mutableFloatStateOf(textOverlay.posX) }
                                var currentPosY by remember(textOverlay.id) { mutableFloatStateOf(textOverlay.posY) }
                                var currentScale by remember(textOverlay.id) { mutableFloatStateOf(textOverlay.scale) }
                                var currentRotation by remember(textOverlay.id) { mutableFloatStateOf(textOverlay.rotation) }

                                val animPosX by animateFloatAsState(currentPosX)
                                val animPosY by animateFloatAsState(currentPosY)
                                val animScale by animateFloatAsState(currentScale)
                                val animRot by animateFloatAsState(currentRotation)
                                
                                val currentTextTimeMs = currentPositionMs - textOverlay.startTimeOnTimelineMs
                                
                                var renderAlpha = 1f
                                var renderScale = animScale
                                var renderPosX = animPosX
                                var renderPosY = animPosY
                                var renderRot = animRot
                                var renderBlur = 0f
                                var displayText = textOverlay.text

                                // 1. Entrance Anim
                                if (textOverlay.animIn != TextAnimIn.NONE && currentTextTimeMs < textOverlay.animInDelayMs + textOverlay.animInDurationMs) {
                                    if (currentTextTimeMs < textOverlay.animInDelayMs) {
                                        renderAlpha = 0f
                                        displayText = if (textOverlay.animIn == TextAnimIn.TYPEWRITER) "" else displayText
                                    } else {
                                        val progress = (currentTextTimeMs - textOverlay.animInDelayMs).toFloat() / textOverlay.animInDurationMs.toFloat()
                                        val ease = kotlin.math.sin(progress * Math.PI / 2).toFloat()
                                        when (textOverlay.animIn) {
                                            TextAnimIn.FADE_IN -> renderAlpha = ease
                                            TextAnimIn.SLIDE_IN_UP -> { renderAlpha = ease; renderPosY += (1f - ease) * 0.5f }
                                            TextAnimIn.SLIDE_IN_DOWN -> { renderAlpha = ease; renderPosY -= (1f - ease) * 0.5f }
                                            TextAnimIn.SLIDE_IN_LEFT -> { renderAlpha = ease; renderPosX += (1f - ease) * 0.5f }
                                            TextAnimIn.SLIDE_IN_RIGHT -> { renderAlpha = ease; renderPosX -= (1f - ease) * 0.5f }
                                            TextAnimIn.SCALE_IN -> { renderAlpha = ease; renderScale *= ease }
                                            TextAnimIn.BOUNCE_IN -> {
                                                val b = if (progress < 0.5f) { 4f * progress * progress * progress } else { 1f - Math.pow((-2f * progress + 2f).toDouble(), 3.0).toFloat() / 2f }
                                                renderScale *= b
                                            }
                                            TextAnimIn.BLUR_IN -> { renderAlpha = ease; renderBlur = (1f - ease) * 20f }
                                            TextAnimIn.ROTATE_IN -> { renderAlpha = ease; renderScale *= ease; renderRot -= (1f - ease) * 180f }
                                            TextAnimIn.TYPEWRITER -> {
                                                val charsToShow = (progress * textOverlay.text.length).toInt()
                                                displayText = textOverlay.text.take(charsToShow) + (if (textOverlay.isTypewriterCursor && progress < 1f && (currentTextTimeMs / 200) % 2 == 0L) "|" else "")
                                            }
                                            TextAnimIn.GLITCH_IN -> {
                                                if (progress < 0.8f && Math.random() < 0.3) {
                                                    renderPosX += (Math.random() - 0.5f).toFloat() * 0.1f
                                                    renderAlpha = Math.random().toFloat()
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }

                                // 2. Loop Anim
                                val exitStartTime = textOverlay.durationMs - textOverlay.animOutDurationMs - textOverlay.animOutDelayMs
                                if (currentTextTimeMs >= textOverlay.animInDelayMs + textOverlay.animInDurationMs && 
                                    (textOverlay.animOut == TextAnimOut.NONE || currentTextTimeMs < exitStartTime)) {
                                    
                                    val loopTime = currentTextTimeMs - (textOverlay.animInDelayMs + textOverlay.animInDurationMs + textOverlay.animLoopDelayMs)
                                    if (loopTime >= 0 && textOverlay.animLoop != TextAnimLoop.NONE && textOverlay.animLoopDurationMs > 0) {
                                        val loopProgress = (loopTime % textOverlay.animLoopDurationMs).toFloat() / textOverlay.animLoopDurationMs.toFloat()
                                        when (textOverlay.animLoop) {
                                            TextAnimLoop.PULSE -> renderScale *= (1f + 0.1f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat())
                                            TextAnimLoop.BOUNCE -> renderPosY -= 0.05f * kotlin.math.sin(loopProgress * Math.PI).toFloat()
                                            TextAnimLoop.SHAKE -> if (loopProgress < 0.2f) renderPosX += 0.02f * kotlin.math.sin(loopProgress * Math.PI * 10).toFloat()
                                            TextAnimLoop.GLOW -> renderBlur = 5f + 5f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                                            TextAnimLoop.WAVE -> renderRot += 5f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                                            TextAnimLoop.FLICKER -> if (Math.random() < 0.1) renderAlpha = 0.5f
                                            TextAnimLoop.SWING -> renderRot += 15f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                                            TextAnimLoop.FLOAT -> {
                                                renderPosY += 0.02f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                                                renderPosX += 0.01f * kotlin.math.cos(loopProgress * Math.PI * 2).toFloat()
                                            }
                                            else -> {}
                                        }
                                    }
                                }

                                // 3. Exit Anim
                                if (textOverlay.animOut != TextAnimOut.NONE && currentTextTimeMs >= exitStartTime) {
                                    if (currentTextTimeMs >= exitStartTime + textOverlay.animOutDelayMs) {
                                        val progress = (currentTextTimeMs - (exitStartTime + textOverlay.animOutDelayMs)).toFloat() / textOverlay.animOutDurationMs.toFloat()
                                        if (progress <= 1f) {
                                            val ease = kotlin.math.sin(progress * Math.PI / 2).toFloat()
                                            val invEase = 1f - ease
                                            when (textOverlay.animOut) {
                                                TextAnimOut.FADE_OUT -> renderAlpha = invEase
                                                TextAnimOut.SLIDE_OUT_UP -> { renderAlpha = invEase; renderPosY -= ease * 0.5f }
                                                TextAnimOut.SLIDE_OUT_DOWN -> { renderAlpha = invEase; renderPosY += ease * 0.5f }
                                                TextAnimOut.SLIDE_OUT_LEFT -> { renderAlpha = invEase; renderPosX -= ease * 0.5f }
                                                TextAnimOut.SLIDE_OUT_RIGHT -> { renderAlpha = invEase; renderPosX += ease * 0.5f }
                                                TextAnimOut.SCALE_OUT -> { renderAlpha = invEase; renderScale *= invEase.coerceAtLeast(0.01f) }
                                                TextAnimOut.DISSOLVE -> { renderAlpha = invEase; renderBlur = ease * 10f }
                                                TextAnimOut.BOUNCE_OUT -> { renderScale *= invEase; renderPosY += ease * 0.2f }
                                                TextAnimOut.BLUR_OUT -> { renderAlpha = invEase; renderBlur = ease * 20f }
                                                else -> {}
                                            }
                                        } else {
                                            renderAlpha = 0f
                                        }
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex((layerOrder.indexOf(textOverlay.id).takeIf { it >= 0 } ?: 0).toFloat())
                                        .pointerInput(textOverlay.id) {
                                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                                if (isSelected && !textOverlay.isLocked) {
                                                    currentScale *= zoom
                                                    currentRotation += rotation
                                                    currentPosX += pan.x / size.width
                                                    currentPosY += pan.y / size.height
                                                    
                                                    val newTexts = texts.toMutableList()
                                                    val idx = newTexts.indexOfFirst { it.id == textOverlay.id }
                                                    if (idx >= 0) {
                                                        newTexts[idx] = newTexts[idx].copy(posX = currentPosX, posY = currentPosY, scale = currentScale, rotation = currentRotation)
                                                        texts = newTexts
                                                    }
                                                }
                                            }
                                        }
                                        .pointerInput(textOverlay.id + "_tap") {
                                            detectTapGestures(
                                                onTap = {
                                                    selectedTextId = textOverlay.id
                                                    showTextToolbar = true
                                                    isPickingOverlay = false
                                                },
                                                onDoubleTap = {
                                                    selectedTextId = textOverlay.id
                                                    editingTextId = textOverlay.id
                                                    showTextToolbar = true
                                                }
                                            )
                                        }
                                        .graphicsLayer {
                                            translationX = (renderPosX - 0.5f) * size.width
                                            translationY = (renderPosY - 0.5f) * size.height
                                            rotationZ = renderRot
                                            alpha = renderAlpha * textOverlay.opacity
                                        }
                                        .then(if (renderBlur > 0f) Modifier.blur(renderBlur.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val baseTextStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = getFontFamily(textOverlay.fontName),
                                        fontSize = (textOverlay.fontSize * renderScale).sp,
                                        fontWeight = if (textOverlay.isBold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (textOverlay.isItalic) FontStyle.Italic else FontStyle.Normal,
                                        textDecoration = if (textOverlay.isUnderline) TextDecoration.Underline else TextDecoration.None,
                                        lineHeight = (textOverlay.fontSize * renderScale * textOverlay.lineHeightMultiplier).sp,
                                        letterSpacing = textOverlay.letterSpacing.sp,
                                        textAlign = when (textOverlay.alignment) {
                                            TextAlignmentType.Left -> TextAlign.Left
                                            TextAlignmentType.Right -> TextAlign.Right
                                            else -> TextAlign.Center
                                        },
                                        shadow = if (textOverlay.shadowColor != Color.Transparent) {
                                            androidx.compose.ui.graphics.Shadow(
                                                color = textOverlay.shadowColor,
                                                offset = androidx.compose.ui.geometry.Offset(textOverlay.shadowOffsetX * renderScale, textOverlay.shadowOffsetY * renderScale),
                                                blurRadius = textOverlay.shadowBlur * renderScale
                                            )
                                        } else null
                                    )

                                    Box(
                                        modifier = Modifier
                                            .background(textOverlay.backgroundColor)
                                            .padding((8 * renderScale).dp)
                                            .then(if (isSelected) Modifier.border((2 * renderScale).dp, Color.White) else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 3D Effect Depth
                                        if (textOverlay.is3D) {
                                            val depthCount = (10 * renderScale).toInt().coerceAtLeast(1)
                                            for (i in depthCount downTo 1) {
                                                androidx.compose.material3.Text(
                                                    text = displayText,
                                                    style = baseTextStyle.copy(color = Color.Black.copy(alpha = 0.5f)),
                                                    modifier = Modifier.offset(y = (i * 2).dp, x = (i * 1.5f).dp)
                                                )
                                                androidx.compose.material3.Text(
                                                    text = displayText,
                                                    style = baseTextStyle.copy(color = textOverlay.textColor.copy(alpha = 0.8f)),
                                                    modifier = Modifier.offset(y = (i * 1.5f).dp, x = (i * 1f).dp)
                                                )
                                            }
                                        }

                                        // Stroke Effect
                                        if (textOverlay.strokeColor != Color.Transparent && textOverlay.strokeWidth > 0f) {
                                            androidx.compose.material3.Text(
                                                text = displayText,
                                                style = baseTextStyle.copy(
                                                    color = textOverlay.strokeColor,
                                                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                                        miter = 10f,
                                                        width = textOverlay.strokeWidth * renderScale,
                                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                    )
                                                )
                                            )
                                        }

                                        // Fill Text
                                        androidx.compose.material3.Text(
                                            text = displayText,
                                            style = baseTextStyle.copy(color = textOverlay.textColor)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Sticker Overlays
                        stickers.forEach { stickerOverlay ->
                            if (!stickerOverlay.isVisible) return@forEach
                            if (currentPositionMs >= stickerOverlay.startTimeOnTimelineMs && currentPositionMs < stickerOverlay.startTimeOnTimelineMs + stickerOverlay.durationMs) {
                                val isSelected = stickerOverlay.id == selectedStickerId
                                
                                var currentPosX by remember(stickerOverlay.id) { mutableFloatStateOf(stickerOverlay.posX) }
                                var currentPosY by remember(stickerOverlay.id) { mutableFloatStateOf(stickerOverlay.posY) }
                                var currentScale by remember(stickerOverlay.id) { mutableFloatStateOf(stickerOverlay.scale) }
                                var currentRotation by remember(stickerOverlay.id) { mutableFloatStateOf(stickerOverlay.rotation) }

                                val animPosX by animateFloatAsState(currentPosX)
                                val animPosY by animateFloatAsState(currentPosY)
                                val animScale by animateFloatAsState(currentScale)
                                val animRot by animateFloatAsState(currentRotation)
                                
                                val currentTextTimeMs = currentPositionMs - stickerOverlay.startTimeOnTimelineMs
                                
                                var renderAlpha = 1f
                                var renderScale = animScale
                                var renderPosX = animPosX
                                var renderPosY = animPosY
                                var renderRot = animRot
                                var renderBlur = 0f

                                // 1. Entrance Anim
                                if (stickerOverlay.animIn != TextAnimIn.NONE && currentTextTimeMs < stickerOverlay.animInDelayMs + stickerOverlay.animInDurationMs) {
                                    if (currentTextTimeMs < stickerOverlay.animInDelayMs) {
                                        renderAlpha = 0f
                                    } else {
                                        val progress = (currentTextTimeMs - stickerOverlay.animInDelayMs).toFloat() / stickerOverlay.animInDurationMs.toFloat()
                                        val ease = kotlin.math.sin(progress * Math.PI / 2).toFloat()
                                        when (stickerOverlay.animIn) {
                                            TextAnimIn.FADE_IN -> renderAlpha = ease
                                            TextAnimIn.SLIDE_IN_UP -> { renderAlpha = ease; renderPosY += (1f - ease) * 0.5f }
                                            TextAnimIn.SLIDE_IN_DOWN -> { renderAlpha = ease; renderPosY -= (1f - ease) * 0.5f }
                                            TextAnimIn.SLIDE_IN_LEFT -> { renderAlpha = ease; renderPosX += (1f - ease) * 0.5f }
                                            TextAnimIn.SLIDE_IN_RIGHT -> { renderAlpha = ease; renderPosX -= (1f - ease) * 0.5f }
                                            TextAnimIn.SCALE_IN -> { renderAlpha = ease; renderScale *= ease }
                                            TextAnimIn.BOUNCE_IN -> {
                                                val b = if (progress < 0.5f) { 4f * progress * progress * progress } else { 1f - Math.pow((-2f * progress + 2f).toDouble(), 3.0).toFloat() / 2f }
                                                renderScale *= b
                                            }
                                            TextAnimIn.BLUR_IN -> { renderAlpha = ease; renderBlur = (1f - ease) * 20f }
                                            TextAnimIn.ROTATE_IN -> { renderAlpha = ease; renderScale *= ease; renderRot -= (1f - ease) * 180f }
                                            TextAnimIn.GLITCH_IN -> {
                                                if (progress < 0.8f && Math.random() < 0.3) {
                                                    renderPosX += (Math.random() - 0.5f).toFloat() * 0.1f
                                                    renderAlpha = Math.random().toFloat()
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }

                                // 2. Loop Anim
                                val exitStartTime = stickerOverlay.durationMs - stickerOverlay.animOutDurationMs - stickerOverlay.animOutDelayMs
                                if (currentTextTimeMs >= stickerOverlay.animInDelayMs + stickerOverlay.animInDurationMs && 
                                    (stickerOverlay.animOut == TextAnimOut.NONE || currentTextTimeMs < exitStartTime)) {
                                    
                                    val loopTime = currentTextTimeMs - (stickerOverlay.animInDelayMs + stickerOverlay.animInDurationMs + stickerOverlay.animLoopDelayMs)
                                    if (loopTime >= 0 && stickerOverlay.animLoop != TextAnimLoop.NONE && stickerOverlay.animLoopDurationMs > 0) {
                                        val loopProgress = (loopTime % stickerOverlay.animLoopDurationMs).toFloat() / stickerOverlay.animLoopDurationMs.toFloat()
                                        when (stickerOverlay.animLoop) {
                                            TextAnimLoop.PULSE -> renderScale *= (1f + 0.1f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat())
                                            TextAnimLoop.BOUNCE -> renderPosY -= 0.05f * kotlin.math.sin(loopProgress * Math.PI).toFloat()
                                            TextAnimLoop.SHAKE -> if (loopProgress < 0.2f) renderPosX += 0.02f * kotlin.math.sin(loopProgress * Math.PI * 10).toFloat()
                                            TextAnimLoop.GLOW -> renderBlur = 5f + 5f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                                            TextAnimLoop.WAVE -> renderRot += 5f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                                            TextAnimLoop.FLICKER -> if (Math.random() < 0.1) renderAlpha = 0.5f
                                            TextAnimLoop.SWING -> renderRot += 15f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                                            TextAnimLoop.FLOAT -> {
                                                renderPosY += 0.02f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                                                renderPosX += 0.01f * kotlin.math.cos(loopProgress * Math.PI * 2).toFloat()
                                            }
                                            else -> {}
                                        }
                                    }
                                }

                                // 3. Exit Anim
                                if (stickerOverlay.animOut != TextAnimOut.NONE && currentTextTimeMs >= exitStartTime) {
                                    if (currentTextTimeMs >= exitStartTime + stickerOverlay.animOutDelayMs) {
                                        val progress = (currentTextTimeMs - (exitStartTime + stickerOverlay.animOutDelayMs)).toFloat() / stickerOverlay.animOutDurationMs.toFloat()
                                        if (progress <= 1f) {
                                            val ease = kotlin.math.sin(progress * Math.PI / 2).toFloat()
                                            val invEase = 1f - ease
                                            when (stickerOverlay.animOut) {
                                                TextAnimOut.FADE_OUT -> renderAlpha = invEase
                                                TextAnimOut.SLIDE_OUT_UP -> { renderAlpha = invEase; renderPosY -= ease * 0.5f }
                                                TextAnimOut.SLIDE_OUT_DOWN -> { renderAlpha = invEase; renderPosY += ease * 0.5f }
                                                TextAnimOut.SLIDE_OUT_LEFT -> { renderAlpha = invEase; renderPosX -= ease * 0.5f }
                                                TextAnimOut.SLIDE_OUT_RIGHT -> { renderAlpha = invEase; renderPosX += ease * 0.5f }
                                                TextAnimOut.SCALE_OUT -> { renderAlpha = invEase; renderScale *= invEase.coerceAtLeast(0.01f) }
                                                TextAnimOut.DISSOLVE -> { renderAlpha = invEase; renderBlur = ease * 10f }
                                                TextAnimOut.BOUNCE_OUT -> { renderScale *= invEase; renderPosY += ease * 0.2f }
                                                TextAnimOut.BLUR_OUT -> { renderAlpha = invEase; renderBlur = ease * 20f }
                                                else -> {}
                                            }
                                        } else {
                                            renderAlpha = 0f
                                        }
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex((layerOrder.indexOf(stickerOverlay.id).takeIf { it >= 0 } ?: 0).toFloat())
                                        .pointerInput(stickerOverlay.id) {
                                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                                if (isSelected && !stickerOverlay.isLocked) {
                                                    currentScale *= zoom
                                                    currentRotation += rotation
                                                    currentPosX += pan.x / size.width
                                                    currentPosY += pan.y / size.height
                                                    
                                                    val newStickers = stickers.toMutableList()
                                                    val idx = newStickers.indexOfFirst { it.id == stickerOverlay.id }
                                                    if (idx >= 0) {
                                                        newStickers[idx] = newStickers[idx].copy(posX = currentPosX, posY = currentPosY, scale = currentScale, rotation = currentRotation)
                                                        stickers = newStickers
                                                    }
                                                }
                                            }
                                        }
                                        .pointerInput(stickerOverlay.id + "tap") {
                                            detectTapGestures(
                                                onTap = {
                                                    selectedStickerId = stickerOverlay.id
                                                    showStickerToolbar = true
                                                    
                                                    selectedClipId = null
                                                    selectedTextId = null
                                                    selectedOverlayId = null
                                                }
                                            )
                                        }
                                        .graphicsLayer {
                                            translationX = (renderPosX - 0.5f) * size.width
                                            translationY = (renderPosY - 0.5f) * size.height
                                            rotationZ = renderRot
                                            alpha = renderAlpha * stickerOverlay.opacity
                                        }
                                        .then(if (renderBlur > 0f) Modifier.blur(renderBlur.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val paddingVal = (8 * renderScale).dp
                                    val borderVal = (2 * renderScale).dp
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(stickerOverlay.backgroundColor)
                                            .padding(paddingVal)
                                            .then(if (isSelected) Modifier.border(borderVal, Color.White, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (stickerOverlay.category == StickerCategory.SHAPE) {
                                            val shapeSize = (100 * renderScale).dp
                                            when(stickerOverlay.content) {
                                                "Circle" -> Box(modifier = Modifier.size(shapeSize).background(stickerOverlay.color, androidx.compose.foundation.shape.CircleShape))
                                                "Rectangle" -> Box(modifier = Modifier.size(shapeSize).background(stickerOverlay.color, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)))
                                                "Line" -> Box(modifier = Modifier.size(width = shapeSize, height = (8 * renderScale).dp).background(stickerOverlay.color, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                                                else -> androidx.compose.material3.Icon(
                                                    imageVector = when(stickerOverlay.content) {
                                                        "Star" -> androidx.compose.material.icons.Icons.Default.Star
                                                        "Arrow" -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward
                                                        else -> androidx.compose.material.icons.Icons.Default.Favorite
                                                    },
                                                    contentDescription = null,
                                                    tint = stickerOverlay.color,
                                                    modifier = Modifier.size(shapeSize)
                                                )
                                            }
                                        } else {
                                            androidx.compose.material3.Text(
                                                text = stickerOverlay.content,
                                                fontSize = (64 * renderScale).sp,
                                                color = stickerOverlay.color
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Drawing Overlays
                        drawings.forEach { drawOverlay ->
                            if (!drawOverlay.isVisible) return@forEach
                            if (currentPositionMs >= drawOverlay.startTimeOnTimelineMs && currentPositionMs < drawOverlay.startTimeOnTimelineMs + drawOverlay.durationMs) {
                                val currentDrawTimeMs = currentPositionMs - drawOverlay.startTimeOnTimelineMs
                                val progress = if (drawOverlay.isAnimated) currentDrawTimeMs.toFloat() / drawOverlay.durationMs else 1f
                                
                                Canvas(modifier = Modifier.fillMaxSize().zIndex((layerOrder.indexOf(drawOverlay.id).takeIf { it >= 0 } ?: 0).toFloat()).graphicsLayer { alpha = drawOverlay.opacity }) {
                                    drawOverlay.strokes.forEach { stroke ->
                                        if (stroke.path.size > 1) {
                                            val uiPath = androidx.compose.ui.graphics.Path()
                                            var first = true
                                            
                                            // Handle animation (draw percentage of path)
                                            val maxPoints = if (drawOverlay.isAnimated) (stroke.path.size * progress).toInt().coerceIn(0, stroke.path.size) else stroke.path.size
                                            
                                            for (i in 0 until maxPoints) {
                                                val point = stroke.path[i]
                                                val px = point.x * size.width
                                                val py = point.y * size.height
                                                if (first) {
                                                    uiPath.moveTo(px, py)
                                                    first = false
                                                } else {
                                                    // Add smoothing using bezier curves
                                                    if (i > 1) {
                                                        val prev = stroke.path[i-1]
                                                        val midX = ((prev.x + point.x) / 2f) * size.width
                                                        val midY = ((prev.y + point.y) / 2f) * size.height
                                                        uiPath.quadraticBezierTo(prev.x * size.width, prev.y * size.height, midX, midY)
                                                    } else {
                                                        uiPath.lineTo(px, py)
                                                    }
                                                }
                                            }
                                            
                                            // Add final lineTo to close gap
                                            if (maxPoints > 0) {
                                              val lastPt = stroke.path[maxPoints - 1]
                                              uiPath.lineTo(lastPt.x * size.width, lastPt.y * size.height)
                                            }
                                            
                                            val blendMode = if (stroke.isEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
                                            val strokeWidthPx = stroke.width * size.width
                                            
                                            val cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            val join = androidx.compose.ui.graphics.StrokeJoin.Round
                                            
                                            var drawColor = stroke.color
                                            var blurRadius = 0f
                                            var strokeStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx, cap = cap, join = join)
                                            
                                            when (stroke.brushType) {
                                                BrushType.MARKER -> drawColor = drawColor.copy(alpha = 0.5f)
                                                BrushType.NEON -> { 
                                                    // Draw glow first
                                                    drawPath(
                                                        path = uiPath,
                                                        color = drawColor.copy(alpha = 0.4f),
                                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx * 2f, cap = cap, join = join),
                                                        blendMode = blendMode
                                                    )
                                                    drawColor = Color.White
                                                }
                                                BrushType.SPRAY -> blurRadius = 15f
                                                else -> {}
                                            }
                                            
                                            if (blurRadius > 0f) {
                                                val paint = androidx.compose.ui.graphics.Paint().apply {
                                                    color = drawColor
                                                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                                                    this.strokeWidth = strokeWidthPx
                                                    this.strokeCap = cap
                                                    this.strokeJoin = join
                                                    asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                                }
                                                drawContext.canvas.drawPath(uiPath, paint)
                                            } else {
                                                drawPath(
                                                    path = uiPath,
                                                    color = drawColor,
                                                    style = strokeStyle,
                                                    blendMode = blendMode
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Drawing Interactions
                        if (isDrawingMode) {
                            var currentDrawPath by remember { mutableStateOf<List<NormalizedOffset>>(emptyList()) }
                            
                            Canvas(modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (isPlaying) {
                                                isPlaying = false // Pause while drawing
                                            }
                                            currentDrawPath = listOf(NormalizedOffset(offset.x / size.width, offset.y / size.height))
                                            
                                            if (selectedDrawId == null) {
                                                // Create new overlay if none selected or if not intersecting active one
                                                val newOverlay = DrawOverlay(
                                                    startTimeOnTimelineMs = currentPositionMs,
                                                    durationMs = 5000L
                                                )
                                                val newDrawings = drawings.toMutableList()
                                                newDrawings.add(newOverlay)
                                                drawings = newDrawings
                                                selectedDrawId = newOverlay.id
                                            }
                                        },
                                        onDragEnd = {
                                            if (currentDrawPath.isNotEmpty() && selectedDrawId != null) {
                                                val drawingIndex = drawings.indexOfFirst { it.id == selectedDrawId }
                                                if (drawingIndex >= 0) {
                                                    val newDrawings = drawings.toMutableList()
                                                    val overlay = newDrawings[drawingIndex]
                                                    val newStrokes = overlay.strokes.toMutableList()
                                                    val newStroke = DrawStroke(
                                                        path = currentDrawPath, 
                                                        color = currentBrushColor, 
                                                        width = currentBrushSize, 
                                                        brushType = currentBrushType,
                                                        isEraser = currentIsEraser
                                                    )
                                                    newStrokes.add(newStroke)
                                                    newDrawings[drawingIndex] = overlay.copy(strokes = newStrokes)
                                                    drawings = newDrawings
                                                    saveState(clips, canvasSettings, "Draw stroke")
                                                }
                                                currentDrawPath = emptyList()
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentDrawPath = currentDrawPath + NormalizedOffset(change.position.x / size.width, change.position.y / size.height)
                                        }
                                    )
                                }
                            ) {
                                if (currentDrawPath.size > 1) {
                                    val uiPath = androidx.compose.ui.graphics.Path()
                                    var first = true
                                    for (point in currentDrawPath) {
                                        val px = point.x * size.width
                                        val py = point.y * size.height
                                        if (first) {
                                            uiPath.moveTo(px, py)
                                            first = false
                                        } else {
                                            uiPath.lineTo(px, py)
                                        }
                                    }
                                    
                                    val cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    val join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    val blendMode = if (currentIsEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
                                            
                                    drawPath(
                                        path = uiPath,
                                        color = if (currentBrushType == BrushType.MARKER && !currentIsEraser) currentBrushColor.copy(alpha=0.5f) else currentBrushColor,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = currentBrushSize * size.width, cap = cap, join = join),
                                        blendMode = blendMode
                                    )
                                }
                            }
                        }

                        // Volume toggle
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            AnimatedContent(
                                targetState = isMuted,
                                transitionSpec = {
                                    scaleIn(tween(150)) togetherWith scaleOut(tween(150))
                                },
                                label = "VolumeToggle"
                            ) { muted ->
                                Icon(
                                    imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    contentDescription = "Toggle Volume",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        // Fullscreen button
                        IconButton(
                            onClick = { 
                                isFullscreen = true 
                                showFullscreenControls = true 
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                        
                    } else {
                        // Placeholder if no media
                        Icon(
                            imageVector = Icons.Filled.MovieCreation,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center).size(64.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            if (!isScreenshotMode) {
            // Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        seekToGlobal(currentPositionMs - 15000L, exoPlayer)
                    }) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Box(modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            com.example.utils.HapticUtil.playLightTap(view, context)
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    seekToGlobal(0L, exoPlayer)
                                }
                                exoPlayer.play()
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                scaleIn(tween(200)) togetherWith scaleOut(tween(200))
                            },
                            label = "PlayPause"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    IconButton(onClick = {
                        seekToGlobal(currentPositionMs + 15000L, exoPlayer)
                    }) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Forward", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                VUMeter(
                    isPlaying = isPlaying,
                    masterVolume = canvasSettings.masterVolume,
                    isMuted = isMuted,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp)
                )
            }
            
            // Timeline Area
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(top = 16.dp, bottom = 16.dp)
                    .clipToBounds()
            ) {
                val screenWidthPx = constraints.maxWidth.toFloat()
                val density = LocalDensity.current
                
                val basePixelsPerSecond = with(density) { 60.dp.toPx() }
                val pixelsPerSecond = basePixelsPerSecond * zoom
                
                val totalWidthPx = (videoDurationMs / 1000f) * pixelsPerSecond
                val currentScrollPx = (currentPositionMs / 1000f) * pixelsPerSecond
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(videoDurationMs) {
                            var lastHapticTimeMs = 0L
                            detectTransformGestures { _, pan, zoomFactor, _ ->
                                zoom = (zoom * zoomFactor).coerceIn(0.2f, 10f)
                                val newScrollPx = currentScrollPx - pan.x
                                val newTimeMs = ((newScrollPx / pixelsPerSecond) * 1000f).toLong()
                                val coercedTime = newTimeMs.coerceIn(0L, videoDurationMs)
                                seekToGlobal(coercedTime, exoPlayer)
                                
                                if (Math.abs(coercedTime - lastHapticTimeMs) > 200L) {
                                    com.example.utils.HapticUtil.playSubtleTick(view, context)
                                    lastHapticTimeMs = coercedTime
                                }
                            }
                        }
                        .pointerInput(videoDurationMs) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val shiftPx = offset.x - (screenWidthPx / 2f)
                                    val newScrollPx = currentScrollPx + shiftPx
                                    val newTimeMs = ((newScrollPx / pixelsPerSecond) * 1000f).toLong()
                                    seekToGlobal(newTimeMs, exoPlayer)
                                }
                            )
                        }
                ) {
                    val translationX = -currentScrollPx + (screenWidthPx / 2f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { androidx.compose.ui.unit.IntOffset(translationX.toInt(), 0) }
                    ) {
                        val intervalSecs = when {
                            zoom > 4f -> 1
                            zoom > 1.5f -> 2
                            zoom > 0.5f -> 5
                            else -> 10
                        }
                        val numMarkers = if (videoDurationMs > 0) (videoDurationMs / 1000) / intervalSecs + 1 else 0
                        val markerColor = Color.White.copy(alpha = 0.5f)
                        
                        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                            for (i in 0..numMarkers) {
                                val sec = i * intervalSecs
                                val xPos = sec * pixelsPerSecond
                                if (xPos <= totalWidthPx) {
                                    drawLine(
                                        color = markerColor,
                                        start = androidx.compose.ui.geometry.Offset(xPos, 16.dp.toPx()),
                                        end = androidx.compose.ui.geometry.Offset(xPos, 24.dp.toPx()),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }
                        
                        for (i in 0..numMarkers) {
                            val sec = i * intervalSecs
                            val xPos = sec * pixelsPerSecond
                            if (xPos <= totalWidthPx) {
                                Text(
                                    text = String.format("%02d:%02d", sec / 60, sec % 60),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .offset { androidx.compose.ui.unit.IntOffset((xPos - with(density) { 12.dp.toPx() }).toInt(), 0) }
                                )
                            }
                        }
                        
                        if (overlays.isNotEmpty() && videoDurationMs > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 28.dp)
                                    .height(48.dp)
                                    .width(with(density) { totalWidthPx.toDp() })
                            ) {
                                for (overlay in overlays) {
                                    val overlayWidthPx = (overlay.durationMs / 1000f) * pixelsPerSecond
                                    val overlayStartPx = (overlay.startTimeOnTimelineMs / 1000f) * pixelsPerSecond
                                    val isOverlaySelected = selectedOverlayId == overlay.id
                                    
                                    val viewportStartPx = currentScrollPx - (screenWidthPx / 2f)
                                    val viewportEndPx = currentScrollPx + (screenWidthPx / 2f)
                                    val viewPad = 600f
                                    if (overlayStartPx > viewportEndPx + viewPad || overlayStartPx + overlayWidthPx < viewportStartPx - viewPad) continue
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { overlayWidthPx.toDp() })
                                            .offset { androidx.compose.ui.unit.IntOffset(overlayStartPx.toInt(), 0) }
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f))
                                            .border(
                                                width = if (isOverlaySelected) 2.dp else 0.dp,
                                                color = if (isOverlaySelected) Color.Cyan else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(overlay.id) {
                                                detectTapGestures(
                                                    onTap = { 
                                                        selectedOverlayId = overlay.id 
                                                        selectedClipId = null 
                                                    }
                                                )
                                            }
                                            .pointerInput(overlay.id + "_drag") {
                                                var accDrag = 0f
                                                detectDragGestures(
                                                    onDragStart = { 
                                                        selectedOverlayId = overlay.id 
                                                        selectedClipId = null 
                                                    },
                                                    onDragEnd = { saveOverlayState(overlays, "Move overlay") },
                                                    onDrag = { change, dragAmount -> 
                                                        change.consume()
                                                        accDrag += dragAmount.x
                                                        val shiftMs = ((accDrag / pixelsPerSecond) * 1000f).toLong()
                                                        if (Math.abs(shiftMs) > 100) {
                                                            val newOverlays = overlays.toMutableList()
                                                            val idx = newOverlays.indexOfFirst { it.id == overlay.id }
                                                            if (idx != -1) {
                                                                val oStart = (newOverlays[idx].startTimeOnTimelineMs + shiftMs).coerceIn(0L, (videoDurationMs - newOverlays[idx].durationMs).coerceAtLeast(0L))
                                                                newOverlays[idx] = newOverlays[idx].copy(startTimeOnTimelineMs = oStart)
                                                                overlays = newOverlays
                                                                accDrag = 0f
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(Uri.parse(overlay.sourceUri))
                                                .videoFrameMillis(overlay.trimStartMs)
                                                .build(),
                                            imageLoader = imageLoader,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.5f }
                                        )
                                        if (isOverlaySelected) {
                                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Red, CircleShape).clickable {
                                                val newOverlays = overlays.filter { it.id != overlay.id }
                                                saveOverlayState(newOverlays, "Delete overlay")
                                                selectedOverlayId = null
                                            }.padding(4.dp)) {
                                                Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                            
                                            // Keyframes
                                            if (overlay.keyframes.isNotEmpty()) {
                                                val distinctTimes = overlay.keyframes.values.flatten().map { it.timeMs }.distinct()
                                                for (t in distinctTimes) {
                                                    val fraction = t.toFloat() / Math.max(1L, overlay.durationMs)
                                                    Box(modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .offset { androidx.compose.ui.unit.IntOffset((fraction * overlayWidthPx - with(density){6.dp.toPx()}).toInt(), 0) }
                                                        .padding(bottom = 2.dp)
                                                        .size(12.dp)
                                                        .rotate(45f)
                                                        .background(Color.White)
                                                    )
                                                }
                                            }
                                            
                                            // Left trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(12.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(overlay.id + "_trimL") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveOverlayState(overlays, "Trim overlay start") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = overlays.indexOfFirst { it.id == overlay.id }
                                                        if (idx != -1) {
                                                            val o = overlays[idx]
                                                            val maxDur = o.originalDurationMs
                                                            val dur = o.durationMs
                                                            if (dur - shiftMs > 500 && o.trimStartMs + shiftMs >= 0) {
                                                                val newO = overlays.toMutableList()
                                                                newO[idx] = o.copy(trimStartMs = o.trimStartMs + shiftMs, startTimeOnTimelineMs = o.startTimeOnTimelineMs + shiftMs)
                                                                overlays = newO
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                            
                                            // Right trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(12.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(overlay.id + "_trimR") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveOverlayState(overlays, "Trim overlay end") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = overlays.indexOfFirst { it.id == overlay.id }
                                                        if (idx != -1) {
                                                            val o = overlays[idx]
                                                            if (o.trimEndMs + shiftMs <= o.originalDurationMs && o.durationMs + shiftMs > 500) {
                                                                val newO = overlays.toMutableList()
                                                                newO[idx] = o.copy(trimEndMs = o.trimEndMs + shiftMs)
                                                                overlays = newO
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        }

                        if (clips.isNotEmpty() && videoDurationMs > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 80.dp)
                                    .height(64.dp)
                                    .width(with(density) { totalWidthPx.toDp() })
                            ) {
                                val clipWidths = clips.associate { it.id to (it.durationMs / 1000f) * pixelsPerSecond }
                                var draggingClipId by remember { mutableStateOf<String?>(null) }
                                var dragOffset by remember { mutableFloatStateOf(0f) }
                                var showMenuForClipId by remember { mutableStateOf<String?>(null) }
                                var showDeleteConfirmFor by remember { mutableStateOf<String?>(null) }
                                
                                val visualClips = remember(clips, draggingClipId, dragOffset) {
                                    if (draggingClipId == null) return@remember clips
                                    val draggingIndex = clips.indexOfFirst { it.id == draggingClipId }
                                    if (draggingIndex < 0) return@remember clips
                                    
                                    val draggingClip = clips[draggingIndex]
                                    val draggingWidth = clipWidths[draggingClip.id] ?: 0f
                                    var originalX = 0f
                                    for (i in 0 until draggingIndex) originalX += clipWidths[clips[i].id] ?: 0f
                                    val currentCenterX = originalX + dragOffset + draggingWidth / 2f
                                    
                                    var newIndex = 0
                                    var accumX = 0f
                                    for (i in clips.indices) {
                                        if (i == draggingIndex) continue
                                        val w = clipWidths[clips[i].id] ?: 0f
                                        if (currentCenterX > accumX + w / 2f) newIndex++
                                        accumX += w
                                    }
                                    
                                    val mut = clips.toMutableList()
                                    mut.removeAt(draggingIndex)
                                    mut.add(newIndex, draggingClip)
                                    mut
                                }
                                
                                val targetXs = remember(visualClips, draggingClipId, dragOffset) {
                                    val xs = mutableMapOf<String, Float>()
                                    var currentX = 0f
                                    for (clip in visualClips) {
                                        val w = clipWidths[clip.id] ?: 0f
                                        if (clip.id == draggingClipId) {
                                            val draggingIndex = clips.indexOfFirst { it.id == draggingClipId }
                                            var originalX = 0f
                                            for (i in 0 until draggingIndex) originalX += clipWidths[clips[i].id] ?: 0f
                                            xs[clip.id] = originalX + dragOffset
                                        } else {
                                            xs[clip.id] = currentX
                                        }
                                        currentX += w
                                    }
                                    xs
                                }

                                if (draggingClipId != null) {
                                    val targetDropIndex = visualClips.indexOfFirst { it.id == draggingClipId }
                                    if (targetDropIndex >= 0) {
                                        var dropX = 0f
                                        for (i in 0 until targetDropIndex) {
                                            dropX += clipWidths[visualClips[i].id] ?: 0f
                                        }
                                        val dropWidth = clipWidths[draggingClipId] ?: 0f
                                        
                                        val animatedDropX by animateFloatAsState(targetValue = dropX, animationSpec = tween(200), label = "dropX")
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .offset { androidx.compose.ui.unit.IntOffset(animatedDropX.toInt(), 0) }
                                                .width(with(density) { dropWidth.toDp() })
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        )
                                    }
                                }

                                val viewportStartPx = currentScrollPx - (screenWidthPx / 2f)
                                val viewportEndPx = currentScrollPx + (screenWidthPx / 2f)
                                val viewPad = 600f

                                for ((clipIndex, clip) in clips.withIndex()) {
                                    val clipWidthPx = clipWidths[clip.id] ?: 0f
                                    val isSelected = selectedClipId == clip.id
                                    val isDragging = draggingClipId == clip.id
                                    
                                    val targetX = targetXs[clip.id] ?: 0f
                                    if (!isDragging && (targetX > viewportEndPx + viewPad || targetX + clipWidthPx < viewportStartPx - viewPad)) continue

                                    val animatedX by animateFloatAsState(
                                        targetValue = targetX,
                                        animationSpec = if (isDragging) androidx.compose.animation.core.snap() else tween(200),
                                        label = "animatedX"
                                    )
                                    val elevation by animateFloatAsState(
                                        targetValue = if (isDragging) 8f else 0f, 
                                        label = "elevation"
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { clipWidthPx.toDp() })
                                            .offset { androidx.compose.ui.unit.IntOffset(animatedX.toInt(), 0) }
                                            .zIndex(if (isDragging) 1f else 0f)
                                            .padding(end = if (clipIndex < clips.lastIndex) 2.dp else 0.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .shadow(elevation.dp, RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color.Yellow else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .pointerInput(clip.id) {
                                                detectTapGestures(
                                                    onTap = { 
                                                        selectedClipId = clip.id 
                                                        selectedFrameId = null
                                                    }
                                                )
                                            }
                                            .pointerInput(clip.id + "_drag") {
                                                var accumulatedDragDistance = 0f
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset ->
                                                        selectedClipId = clip.id
                                                        draggingClipId = clip.id
                                                        dragOffset = 0f
                                                        accumulatedDragDistance = 0f
                                                        com.example.utils.HapticUtil.playMediumBuzz(view, context)
                                                    },
                                                    onDragEnd = {
                                                        if (draggingClipId != null) {
                                                            com.example.utils.HapticUtil.playLightTap(view, context)
                                                            if (Math.abs(accumulatedDragDistance) < 10f) {
                                                                showMenuForClipId = clip.id
                                                            } else {
                                                                saveState(visualClips, canvasSettings, "Reorder clips")
                                                            }
                                                            draggingClipId = null
                                                            dragOffset = 0f
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        draggingClipId = null
                                                        dragOffset = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset += dragAmount.x
                                                        accumulatedDragDistance += dragAmount.x
                                                    }
                                                )
                                            }
                                    ) {
                                        val frameWidthDp = if (isBudgetMode) 96.dp else 48.dp
                                        val frameWidthPx = with(density) { frameWidthDp.toPx() }
                                        val usableWidthPx = clipWidthPx - if (clipIndex < clips.lastIndex) with(density){2.dp.toPx()} else 0f
                                        val numFrames = (usableWidthPx / frameWidthPx).toInt().coerceAtLeast(1)
                                        val actualFrameWidth = usableWidthPx / numFrames.toFloat()
                                        
                                        Row {
                                            for (i in 0 until numFrames) {
                                                val localTargetTimeMs = ((i.toFloat() / numFrames) * clip.durationMs).toLong()
                                                val srcFraction = mapPlaybackTimeToOriginalFraction(localTargetTimeMs, Math.max(1L, clip.trimEndMs - clip.trimStartMs), clip.speedCurve)
                                                val timeMs = clip.trimStartMs + (srcFraction * (clip.trimEndMs - clip.trimStartMs)).toLong()
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(Uri.parse(clip.sourceUri))
                                                        .videoFrameMillis(timeMs)
                                                        .build(),
                                                    imageLoader = imageLoader,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .width(with(density) { actualFrameWidth.toDp() })
                                                )
                                            }
                                        }
                                        
                                        if (isSelected && !clip.isPhoto) {
                                            AudioWaveform(
                                                clipId = clip.id,
                                                durationMs = clip.originalDurationMs,
                                                trimStartMs = clip.trimStartMs,
                                                trimEndMs = clip.trimEndMs,
                                                pixelsPerMs = pixelsPerSecond / 1000f,
                                                keyframes = clip.keyframes,
                                                audioEffects = clip.audioEffects,
                                                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                                                color = Color(0xFF00FFCC).copy(alpha = 0.6f)
                                            )
                                        }
                                        
                                        if (clip.isMuted) {
                                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha=0.5f), CircleShape).padding(2.dp)) {
                                                Icon(Icons.Filled.VolumeOff, "Muted", tint=Color.White, modifier=Modifier.size(12.dp))
                                            }
                                        }
                                        
                                        // Keyframes
                                        if (isSelected && clip.keyframes.isNotEmpty()) {
                                            val distinctTimes = clip.keyframes.values.flatten().map { it.timeMs }.distinct()
                                            for (t in distinctTimes) {
                                                val fraction = t.toFloat() / Math.max(1L, clip.durationMs)
                                                Box(modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .offset { androidx.compose.ui.unit.IntOffset((fraction * usableWidthPx - with(density){6.dp.toPx()}).toInt(), 0) }
                                                    .padding(bottom = 2.dp)
                                                    .size(12.dp)
                                                    .rotate(45f)
                                                    .background(Color.White)
                                                )
                                            }
                                        }
                                        
                                        if (showMenuForClipId == clip.id) {
                                            DropdownMenu(
                                                expanded = true,
                                                onDismissRequest = { showMenuForClipId = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Duplicate") },
                                                    onClick = {
                                                        showMenuForClipId = null
                                                        val duplicatedClip = clip.copy(id = java.util.UUID.randomUUID().toString())
                                                        val newClips = clips.toMutableList()
                                                        newClips.add(clipIndex + 1, duplicatedClip)
                                                        saveState(newClips, canvasSettings, "Duplicate clip")
                                                    }
                                                )
                                            }
                                        }
                                        
                                        if (isSelected && !isDragging) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .padding(top = 4.dp)
                                                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                                    .clickable { showDeleteConfirmFor = clip.id }
                                                    .padding(6.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                            }
                                            
                                            // Handle confirmation overlay
                                            if (showDeleteConfirmFor == clip.id) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.8f))
                                                        .clickable { showDeleteConfirmFor = null }
                                                        .zIndex(2f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.align(Alignment.Center),
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.Red, modifier = Modifier.size(24.dp).background(Color.White.copy(alpha=0.2f), CircleShape).padding(2.dp).clickable { showDeleteConfirmFor = null })
                                                        Icon(Icons.Filled.Check, contentDescription = "Confirm", tint = Color.Green, modifier = Modifier.size(24.dp).background(Color.White.copy(alpha=0.2f), CircleShape).padding(2.dp).clickable { 
                                                            showDeleteConfirmFor = null
                                                            selectedClipId = null
                                                            saveState(clips.filter { it.id != clip.id }, canvasSettings, "Delete clip")
                                                        })
                                                    }
                                                }
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .fillMaxHeight()
                                                    .width(16.dp)
                                                    .background(Color.Yellow.copy(alpha = 0.5f))
                                                    .pointerInput(clip.id + "_start_trim") {
                                                        var dragTimeOffsetMs = 0L
                                                        var wasPlaying = false
                                                        var initialTrimStart = 0L
                                                        var initialTrimEnd = 0L
                                                        detectDragGestures(
                                                            onDragStart = { 
                                                                wasPlaying = exoPlayer.isPlaying
                                                                exoPlayer.pause()
                                                                val c = clips.find { it.id == clip.id }
                                                                initialTrimStart = c?.trimStartMs ?: 0L
                                                                initialTrimEnd = c?.trimEndMs ?: 0L
                                                            },
                                                            onDragEnd = {
                                                                val currentClip = clips.find { it.id == clip.id } ?: return@detectDragGestures
                                                                val newStart = (initialTrimStart + dragTimeOffsetMs).coerceIn(0L, (initialTrimEnd - 500L).coerceAtLeast(0L))
                                                                val newClips = clips.toMutableList()
                                                                val idx = newClips.indexOfFirst { it.id == clip.id }
                                                                if (idx != -1) {
                                                                    newClips[idx] = currentClip.copy(trimStartMs = newStart)
                                                                    saveState(newClips, canvasSettings, "Trim clip start")
                                                                }
                                                                if (wasPlaying) exoPlayer.play()
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragTimeOffsetMs += ((dragAmount.x / pixelsPerSecond) * 1000f).toLong()
                                                                val newStart = (initialTrimStart + dragTimeOffsetMs).coerceIn(0L, (initialTrimEnd - 500L).coerceAtLeast(0L))
                                                                
                                                                val tempClips = clips.toMutableList()
                                                                val idx = tempClips.indexOfFirst { it.id == clip.id }
                                                                if (idx != -1) {
                                                                    val currentClip = tempClips[idx]
                                                                    tempClips[idx] = currentClip.copy(trimStartMs = newStart)
                                                                    clips = tempClips
                                                                    
                                                                    var globalPos = 0L
                                                                    for (j in 0 until idx) globalPos += clips[j].durationMs
                                                                    currentPositionMs = globalPos
                                                                }
                                                            }
                                                        )
                                                    }
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .fillMaxHeight()
                                                    .width(16.dp)
                                                    .background(Color.Yellow.copy(alpha = 0.5f))
                                                    .pointerInput(clip.id + "_end_trim") {
                                                        var dragTimeOffsetMs = 0L
                                                        var wasPlaying = false
                                                        var initialTrimStart = 0L
                                                        var initialTrimEnd = 0L
                                                        var initialOriginalDuration = 0L
                                                        detectDragGestures(
                                                            onDragStart = { 
                                                                wasPlaying = exoPlayer.isPlaying
                                                                exoPlayer.pause()
                                                                val c = clips.find { it.id == clip.id }
                                                                initialTrimStart = c?.trimStartMs ?: 0L
                                                                initialTrimEnd = c?.trimEndMs ?: 0L
                                                                initialOriginalDuration = c?.originalDurationMs ?: 0L
                                                            },
                                                            onDragEnd = {
                                                                val currentClip = clips.find { it.id == clip.id } ?: return@detectDragGestures
                                                                val newEnd = (initialTrimEnd + dragTimeOffsetMs).coerceIn((initialTrimStart + 500L).coerceAtMost(initialOriginalDuration), initialOriginalDuration.coerceAtLeast(0L))
                                                                val newClips = clips.toMutableList()
                                                                val idx = newClips.indexOfFirst { it.id == clip.id }
                                                                if (idx != -1) {
                                                                    newClips[idx] = currentClip.copy(trimEndMs = newEnd)
                                                                    saveState(newClips, canvasSettings, "Trim clip end")
                                                                }
                                                                if (wasPlaying) exoPlayer.play()
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragTimeOffsetMs += ((dragAmount.x / pixelsPerSecond) * 1000f).toLong()
                                                                val newEnd = (initialTrimEnd + dragTimeOffsetMs).coerceIn((initialTrimStart + 500L).coerceAtMost(initialOriginalDuration), initialOriginalDuration.coerceAtLeast(0L))
                                                                
                                                                val tempClips = clips.toMutableList()
                                                                val idx = tempClips.indexOfFirst { it.id == clip.id }
                                                                if (idx != -1) {
                                                                    val currentClip = tempClips[idx]
                                                                    tempClips[idx] = currentClip.copy(trimEndMs = newEnd)
                                                                    clips = tempClips
                                                                    
                                                                    var globalPos = 0L
                                                                    for (j in 0..idx) globalPos += clips[j].durationMs
                                                                    currentPositionMs = globalPos
                                                                }
                                                            }
                                                        )
                                                    }
                                            )
                                        }
                                        
                                        // Transition editing is withheld until the exporter can render it.
                                        if (clipIndex < clips.lastIndex) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .width(24.dp) // Wider click target
                                                    .fillMaxHeight(0.6f)
                                                    .offset(x = 12.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                                    .border(1.dp, if (clip.transitionNext.type != TransitionType.NONE) MaterialTheme.colorScheme.primary else Color.White, RoundedCornerShape(4.dp))
                                                    .zIndex(5f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (clip.transitionNext.type != TransitionType.NONE) {
                                                    Icon(Icons.Filled.Animation, contentDescription = "Transition", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                                } else {
                                                    Box(modifier = Modifier.size(4.dp).background(Color.White, CircleShape))
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Render Add Media button at end of timeline
                                val addMediaOffset = if (clips.isNotEmpty()) (targetXs.values.maxOrNull() ?: 0f) + (clipWidths[clips.last().id] ?: 0f) + with(density){16.dp.toPx()} else 0f
                                val animatedAddMediaOffset by animateFloatAsState(targetValue = addMediaOffset, animationSpec = tween(200), label = "addMediaOffset")
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .offset { androidx.compose.ui.unit.IntOffset(animatedAddMediaOffset.toInt(), 0) }
                                ) {
                                    IconButton(
                                        onClick = { 
                                            isPickingOverlay = false
                                            showMediaPicker = true 
                                        },
                                        modifier = Modifier.align(Alignment.Center).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add Media", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(top = 28.dp)
                                    .height(64.dp)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Drag media here to begin", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        // Audio Clips Timeline
                        if (audioClips.isNotEmpty() && videoDurationMs > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 152.dp)
                                    .height(32.dp)
                                    .width(with(density) { totalWidthPx.toDp() })
                            ) {
                                for (audioClip in audioClips) {
                                    val drawWidthPx = (audioClip.durationMs / 1000f) * pixelsPerSecond
                                    val startPx = (audioClip.startTimeOnTimelineMs / 1000f) * pixelsPerSecond
                                    val isSelected = selectedAudioId == audioClip.id
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { drawWidthPx.toDp() })
                                            .offset { androidx.compose.ui.unit.IntOffset(startPx.toInt(), 0) }
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF00C853).copy(alpha = 0.8f))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .pointerInput(audioClip.id) {
                                                detectTapGestures(
                                                    onTap = { selectedAudioId = audioClip.id; selectedClipId = null; selectedOverlayId = null }
                                                )
                                            }
                                            .pointerInput(audioClip.id + "_drag") {
                                                var accDrag = 0f
                                                detectDragGestures(
                                                    onDragStart = { selectedAudioId = audioClip.id; selectedClipId = null; selectedOverlayId = null },
                                                    onDrag = { change, dragAmount -> 
                                                        change.consume()
                                                        accDrag += dragAmount.x
                                                        val shiftMs = ((accDrag / pixelsPerSecond) * 1000f).toLong()
                                                        if (Math.abs(shiftMs) > 100) {
                                                            val newAudios = audioClips.toMutableList()
                                                            val idx = newAudios.indexOfFirst { it.id == audioClip.id }
                                                            if (idx != -1) {
                                                                val oStart = (newAudios[idx].startTimeOnTimelineMs + shiftMs).coerceIn(0L, (videoDurationMs - newAudios[idx].durationMs).coerceAtLeast(0L))
                                                                newAudios[idx] = newAudios[idx].copy(startTimeOnTimelineMs = oStart)
                                                                audioClips = newAudios
                                                                accDrag = 0f
                                                                persistHistory()
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        AudioWaveform(
                                            clipId = audioClip.id,
                                            durationMs = audioClip.sourceDurationMs,
                                            trimStartMs = audioClip.trimStartMs,
                                            trimEndMs = audioClip.trimEndMs,
                                            pixelsPerMs = pixelsPerSecond / 1000f,
                                            keyframes = audioClip.keyframes,
                                            audioEffects = audioClip.audioEffects,
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(0xFF00BFFF).copy(alpha = 0.5f)
                                        )
                                        
                                        if (audioClip.displayName != null && audioClip.durationMs > 500) {
                                            Text(
                                                text = audioClip.displayName,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                modifier = Modifier.padding(2.dp).align(Alignment.TopStart)
                                            )
                                        }

                                        if (isSelected) {
                                            // Left trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(12.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(audioClip.id + "_trimL") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { persistHistory() }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val newAudios = audioClips.toMutableList()
                                                        val idx = newAudios.indexOfFirst { it.id == audioClip.id }
                                                        if (idx != -1) {
                                                            val c = newAudios[idx]
                                                            val newTrimStart = (c.trimStartMs + shiftMs).coerceIn(0L, (c.trimEndMs - 100L).coerceAtLeast(0L))
                                                            val startDiff = newTrimStart - c.trimStartMs
                                                            newAudios[idx] = c.copy(
                                                                trimStartMs = newTrimStart,
                                                                startTimeOnTimelineMs = c.startTimeOnTimelineMs + startDiff
                                                            )
                                                            audioClips = newAudios
                                                            acc = 0f
                                                        }
                                                    }
                                                }
                                            })
                                            
                                            // Right trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(12.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(audioClip.id + "_trimR") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { persistHistory() }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val newAudios = audioClips.toMutableList()
                                                        val idx = newAudios.indexOfFirst { it.id == audioClip.id }
                                                        if (idx != -1) {
                                                            val c = newAudios[idx]
                                                            val newTrimEnd = (c.trimEndMs + shiftMs).coerceIn((c.trimStartMs + 100L).coerceAtMost(c.sourceDurationMs), c.sourceDurationMs.coerceAtLeast(0L))
                                                            newAudios[idx] = c.copy(trimEndMs = newTrimEnd)
                                                            audioClips = newAudios
                                                            acc = 0f
                                                        }
                                                    }
                                                }
                                            })
                                            
                                            // Delete button
                                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top=4.dp).background(Color.Red, CircleShape).clickable {
                                                audioClips = audioClips.filter { it.id != audioClip.id }
                                                selectedAudioId = null
                                                persistHistory()
                                            }.padding(4.dp)) {
                                                Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (texts.isNotEmpty() && videoDurationMs > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 184.dp)
                                    .height(32.dp)
                                    .width(with(density) { totalWidthPx.toDp() })
                            ) {
                                for (textOverlay in texts) {
                                    val textWidthPx = (textOverlay.durationMs / 1000f) * pixelsPerSecond
                                    val textStartPx = (textOverlay.startTimeOnTimelineMs / 1000f) * pixelsPerSecond
                                    val isTextSelected = selectedTextId == textOverlay.id
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { textWidthPx.toDp() })
                                            .offset { androidx.compose.ui.unit.IntOffset(textStartPx.toInt(), 0) }
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .border(
                                                width = if (isTextSelected) 2.dp else 0.dp,
                                                color = if (isTextSelected) Color.Cyan else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(textOverlay.id) {
                                                detectTapGestures(
                                                    onTap = { 
                                                        selectedTextId = textOverlay.id 
                                                        selectedClipId = null 
                                                        selectedOverlayId = null
                                                        selectedFrameId = null
                                                    },
                                                    onDoubleTap = {
                                                        selectedTextId = textOverlay.id
                                                        editingTextId = textOverlay.id
                                                        showTextToolbar = true
                                                    }
                                                )
                                            }
                                            .pointerInput(textOverlay.id + "_drag") {
                                                var accDrag = 0f
                                                detectDragGestures(
                                                    onDragStart = { 
                                                        selectedTextId = textOverlay.id 
                                                        selectedClipId = null 
                                                        selectedOverlayId = null
                                                    },
                                                    onDragEnd = { saveTextState(texts, "Move text") },
                                                    onDrag = { change, dragAmount -> 
                                                        change.consume()
                                                        accDrag += dragAmount.x
                                                        val shiftMs = ((accDrag / pixelsPerSecond) * 1000f).toLong()
                                                        if (Math.abs(shiftMs) > 100) {
                                                            val newTexts = texts.toMutableList()
                                                            val idx = newTexts.indexOfFirst { it.id == textOverlay.id }
                                                            if (idx != -1) {
                                                                val tStart = (newTexts[idx].startTimeOnTimelineMs + shiftMs).coerceIn(0L, (videoDurationMs - newTexts[idx].durationMs).coerceAtLeast(0L))
                                                                newTexts[idx] = newTexts[idx].copy(startTimeOnTimelineMs = tStart)
                                                                texts = newTexts
                                                                accDrag = 0f
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        androidx.compose.material3.Text(
                                            textOverlay.text,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(1.dp).align(Alignment.CenterStart),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        if (isTextSelected) {
                                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Red, CircleShape).clickable {
                                                val newTexts = texts.filter { it.id != textOverlay.id }
                                                saveTextState(newTexts, "Delete text")
                                                selectedTextId = null
                                                if (editingTextId == textOverlay.id) {
                                                    editingTextId = null
                                                    showTextToolbar = false
                                                }
                                            }.padding(4.dp)) {
                                                Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                            
                                            // Left trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(8.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(textOverlay.id + "_trimL") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveTextState(texts, "Trim text start") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = texts.indexOfFirst { it.id == textOverlay.id }
                                                        if (idx != -1) {
                                                            val t = texts[idx]
                                                            val dur = t.durationMs
                                                            if (dur - shiftMs > 500 && t.startTimeOnTimelineMs + shiftMs >= 0) {
                                                                val newT = texts.toMutableList()
                                                                newT[idx] = t.copy(durationMs = dur - shiftMs, startTimeOnTimelineMs = t.startTimeOnTimelineMs + shiftMs)
                                                                texts = newT
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                            
                                            // Right trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(textOverlay.id + "_trimR") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveTextState(texts, "Trim text end") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = texts.indexOfFirst { it.id == textOverlay.id }
                                                        if (idx != -1) {
                                                            val t = texts[idx]
                                                            if (t.durationMs + shiftMs > 500) {
                                                                val newT = texts.toMutableList()
                                                                newT[idx] = t.copy(durationMs = t.durationMs + shiftMs)
                                                                texts = newT
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (stickers.isNotEmpty() && videoDurationMs > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 216.dp)
                                    .height(32.dp)
                                    .width(with(density) { totalWidthPx.toDp() })
                            ) {
                                for (stickerOverlay in stickers) {
                                    val stickerWidthPx = (stickerOverlay.durationMs / 1000f) * pixelsPerSecond
                                    val stickerStartPx = (stickerOverlay.startTimeOnTimelineMs / 1000f) * pixelsPerSecond
                                    val isStickerSelected = selectedStickerId == stickerOverlay.id
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { stickerWidthPx.toDp() })
                                            .offset { androidx.compose.ui.unit.IntOffset(stickerStartPx.toInt(), 0) }
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                                            .border(
                                                width = if (isStickerSelected) 2.dp else 0.dp,
                                                color = if (isStickerSelected) Color.Cyan else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(stickerOverlay.id) {
                                                detectTapGestures(
                                                    onTap = { 
                                                        selectedStickerId = stickerOverlay.id 
                                                        selectedClipId = null 
                                                        selectedOverlayId = null
                                                        selectedTextId = null
                                                        selectedFrameId = null
                                                    },
                                                    onDoubleTap = {
                                                        selectedStickerId = stickerOverlay.id
                                                        showStickerToolbar = true
                                                    }
                                                )
                                            }
                                            .pointerInput(stickerOverlay.id + "_drag") {
                                                var accDrag = 0f
                                                detectDragGestures(
                                                    onDragStart = { 
                                                        selectedStickerId = stickerOverlay.id 
                                                        selectedClipId = null 
                                                        selectedOverlayId = null
                                                        selectedTextId = null
                                                    },
                                                    onDragEnd = { saveState(clips, canvasSettings, "Move sticker") },
                                                    onDrag = { change, dragAmount -> 
                                                        change.consume()
                                                        accDrag += dragAmount.x
                                                        val shiftMs = ((accDrag / pixelsPerSecond) * 1000f).toLong()
                                                        if (Math.abs(shiftMs) > 100) {
                                                            val newStickers = stickers.toMutableList()
                                                            val idx = newStickers.indexOfFirst { it.id == stickerOverlay.id }
                                                            if (idx != -1) {
                                                                val tStart = (newStickers[idx].startTimeOnTimelineMs + shiftMs).coerceIn(0L, (videoDurationMs - newStickers[idx].durationMs).coerceAtLeast(0L))
                                                                newStickers[idx] = newStickers[idx].copy(startTimeOnTimelineMs = tStart)
                                                                stickers = newStickers
                                                                accDrag = 0f
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        androidx.compose.material3.Text(
                                            stickerOverlay.content,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(1.dp).align(Alignment.CenterStart),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        if (isStickerSelected) {
                                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Red, CircleShape).clickable {
                                                stickers = stickers.filter { it.id != stickerOverlay.id }
                                                saveState(clips, canvasSettings, "Delete sticker")
                                                selectedStickerId = null
                                                showStickerToolbar = false
                                            }.padding(4.dp)) {
                                                Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                            
                                            // Left trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(8.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(stickerOverlay.id + "_trimL") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveState(clips, canvasSettings, "Trim sticker start") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = stickers.indexOfFirst { it.id == stickerOverlay.id }
                                                        if (idx != -1) {
                                                            val s = stickers[idx]
                                                            val dur = s.durationMs
                                                            if (dur - shiftMs > 500 && s.startTimeOnTimelineMs + shiftMs >= 0) {
                                                                val newS = stickers.toMutableList()
                                                                newS[idx] = s.copy(durationMs = dur - shiftMs, startTimeOnTimelineMs = s.startTimeOnTimelineMs + shiftMs)
                                                                stickers = newS
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                            
                                            // Right trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(stickerOverlay.id + "_trimR") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveState(clips, canvasSettings, "Trim sticker end") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = stickers.indexOfFirst { it.id == stickerOverlay.id }
                                                        if (idx != -1) {
                                                            val s = stickers[idx]
                                                            if (s.durationMs + shiftMs > 500) {
                                                                val newS = stickers.toMutableList()
                                                                newS[idx] = s.copy(durationMs = s.durationMs + shiftMs)
                                                                stickers = newS
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Drawings Timeline
                        if (drawings.isNotEmpty() && videoDurationMs > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 248.dp)
                                    .height(32.dp)
                                    .width(with(density) { totalWidthPx.toDp() })
                            ) {
                                for (drawOverlay in drawings) {
                                    val drawWidthPx = (drawOverlay.durationMs / 1000f) * pixelsPerSecond
                                    val drawStartPx = (drawOverlay.startTimeOnTimelineMs / 1000f) * pixelsPerSecond
                                    val isDrawSelected = selectedDrawId == drawOverlay.id
                                    
                                    val viewportStartPx = currentScrollPx - (screenWidthPx / 2f)
                                    val viewportEndPx = currentScrollPx + (screenWidthPx / 2f)
                                    val viewPad = 600f
                                    if (drawStartPx > viewportEndPx + viewPad || drawStartPx + drawWidthPx < viewportStartPx - viewPad) continue
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { drawWidthPx.toDp() })
                                            .offset { androidx.compose.ui.unit.IntOffset(drawStartPx.toInt(), 0) }
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.tertiary)
                                            .border(
                                                width = if (isDrawSelected) 2.dp else 0.dp,
                                                color = if (isDrawSelected) Color.Cyan else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(drawOverlay.id) {
                                                detectTapGestures(
                                                    onTap = { 
                                                        selectedDrawId = drawOverlay.id 
                                                        selectedClipId = null 
                                                        selectedOverlayId = null
                                                        selectedTextId = null
                                                        selectedStickerId = null
                                                        selectedFrameId = null
                                                    },
                                                    onDoubleTap = {
                                                        selectedDrawId = drawOverlay.id
                                                        showDrawToolbar = true
                                                    }
                                                )
                                            }
                                            .pointerInput(drawOverlay.id + "_drag") {
                                                var accDrag = 0f
                                                detectDragGestures(
                                                    onDragStart = { 
                                                        selectedDrawId = drawOverlay.id 
                                                        selectedClipId = null 
                                                        selectedOverlayId = null
                                                        selectedTextId = null
                                                        selectedStickerId = null
                                                    },
                                                    onDragEnd = { saveState(clips, canvasSettings, "Move drawing") },
                                                    onDrag = { change, dragAmount -> 
                                                        change.consume()
                                                        accDrag += dragAmount.x
                                                        val shiftMs = ((accDrag / pixelsPerSecond) * 1000f).toLong()
                                                        if (Math.abs(shiftMs) > 100) {
                                                            val newDrawings = drawings.toMutableList()
                                                            val idx = newDrawings.indexOfFirst { it.id == drawOverlay.id }
                                                            if (idx != -1) {
                                                                val tStart = (newDrawings[idx].startTimeOnTimelineMs + shiftMs).coerceIn(0L, (videoDurationMs - newDrawings[idx].durationMs).coerceAtLeast(0L))
                                                                newDrawings[idx] = newDrawings[idx].copy(startTimeOnTimelineMs = tStart)
                                                                drawings = newDrawings
                                                                accDrag = 0f
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        androidx.compose.material3.Text(
                                            "Drawing",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.padding(1.dp).align(Alignment.CenterStart),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        if (isDrawSelected) {
                                            // Left trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(12.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(drawOverlay.id + "_trimL") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveState(clips, canvasSettings, "Trim draw start") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = drawings.indexOfFirst { it.id == drawOverlay.id }
                                                        if (idx != -1) {
                                                            val d = drawings[idx]
                                                            val dur = d.durationMs
                                                            if (dur - shiftMs > 500 && d.startTimeOnTimelineMs + shiftMs >= 0) {
                                                                val newD = drawings.toMutableList()
                                                                newD[idx] = d.copy(durationMs = dur - shiftMs, startTimeOnTimelineMs = d.startTimeOnTimelineMs + shiftMs)
                                                                drawings = newD
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                            
                                            // Right trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(12.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(drawOverlay.id + "_trimR") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveState(clips, canvasSettings, "Trim draw end") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = drawings.indexOfFirst { it.id == drawOverlay.id }
                                                        if (idx != -1) {
                                                            val d = drawings[idx]
                                                            if (d.durationMs + shiftMs > 500) {
                                                                val newD = drawings.toMutableList()
                                                                newD[idx] = d.copy(durationMs = d.durationMs + shiftMs)
                                                                drawings = newD
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Frames Timeline
                        if (frames.isNotEmpty() && videoDurationMs > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 280.dp)
                                    .height(32.dp)
                                    .width(with(density) { totalWidthPx.toDp() })
                            ) {
                                for (frameOverlay in frames) {
                                    val drawWidthPx = (frameOverlay.durationMs / 1000f) * pixelsPerSecond
                                    val drawStartPx = (frameOverlay.startTimeOnTimelineMs / 1000f) * pixelsPerSecond
                                    val isDrawSelected = selectedFrameId == frameOverlay.id
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { drawWidthPx.toDp() })
                                            .offset { androidx.compose.ui.unit.IntOffset(drawStartPx.toInt(), 0) }
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondary)
                                            .border(
                                                width = if (isDrawSelected) 2.dp else 0.dp,
                                                color = if (isDrawSelected) Color.Cyan else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(frameOverlay.id) {
                                                detectTapGestures(
                                                    onTap = { 
                                                        selectedFrameId = frameOverlay.id 
                                                        selectedClipId = null 
                                                        selectedOverlayId = null
                                                        selectedTextId = null
                                                        selectedStickerId = null
                                                        selectedDrawId = null
                                                    },
                                                    onDoubleTap = {
                                                        selectedFrameId = frameOverlay.id
                                                        showFrameToolbar = true
                                                    }
                                                )
                                            }
                                            .pointerInput(frameOverlay.id + "_drag") {
                                                var accDrag = 0f
                                                detectDragGestures(
                                                    onDragStart = { 
                                                        selectedFrameId = frameOverlay.id 
                                                        selectedClipId = null 
                                                        selectedOverlayId = null
                                                        selectedTextId = null
                                                        selectedStickerId = null
                                                        selectedDrawId = null
                                                    },
                                                    onDragEnd = { saveState(clips, canvasSettings, "Move frame") },
                                                    onDrag = { change, dragAmount -> 
                                                        change.consume()
                                                        accDrag += dragAmount.x
                                                        val shiftMs = ((accDrag / pixelsPerSecond) * 1000f).toLong()
                                                        if (Math.abs(shiftMs) > 100) {
                                                            val newFrames = frames.toMutableList()
                                                            val idx = newFrames.indexOfFirst { it.id == frameOverlay.id }
                                                            if (idx != -1) {
                                                                val tStart = (newFrames[idx].startTimeOnTimelineMs + shiftMs).coerceIn(0L, (videoDurationMs - newFrames[idx].durationMs).coerceAtLeast(0L))
                                                                newFrames[idx] = newFrames[idx].copy(startTimeOnTimelineMs = tStart)
                                                                frames = newFrames
                                                                accDrag = 0f
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        androidx.compose.material3.Text(
                                            "Frame",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.padding(1.dp).align(Alignment.CenterStart),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        if (isDrawSelected) {
                                            // Left trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(12.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(frameOverlay.id + "_trimL") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveState(clips, canvasSettings, "Trim frame start") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = frames.indexOfFirst { it.id == frameOverlay.id }
                                                        if (idx != -1) {
                                                            val d = frames[idx]
                                                            val dur = d.durationMs
                                                            if (dur - shiftMs > 500 && d.startTimeOnTimelineMs + shiftMs >= 0) {
                                                                val newD = frames.toMutableList()
                                                                newD[idx] = d.copy(durationMs = dur - shiftMs, startTimeOnTimelineMs = d.startTimeOnTimelineMs + shiftMs)
                                                                frames = newD
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                            
                                            // Right trim handle
                                            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(12.dp).background(Color.White.copy(alpha=0.5f)).pointerInput(frameOverlay.id + "_trimR") {
                                                var acc = 0f
                                                detectDragGestures(
                                                    onDragEnd = { saveState(clips, canvasSettings, "Trim frame end") }
                                                ) { change, drag ->
                                                    change.consume()
                                                    acc += drag.x
                                                    val shiftMs = ((acc / pixelsPerSecond) * 1000f).toLong()
                                                    if (Math.abs(shiftMs) > 100) {
                                                        val idx = frames.indexOfFirst { it.id == frameOverlay.id }
                                                        if (idx != -1) {
                                                            val d = frames[idx]
                                                            if (d.durationMs + shiftMs > 500) {
                                                                val newD = frames.toMutableList()
                                                                newD[idx] = d.copy(durationMs = d.durationMs + shiftMs)
                                                                frames = newD
                                                                acc = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(12.dp, 8.dp)
                                .offset(y = (-8).dp)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(Color.White)
                        )
                    }
                }
            }
            }
        }
        
        if (isScreenshotMode) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(
                    onClick = { isScreenshotMode = false },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit Screenshot Mode")
                }
            }
        }

        AnimatedVisibility(
            visible = showSavedIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Saved",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showSpeedPanel && selectedClipId != null,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val selectedClip = clips.find { it.id == selectedClipId }
            if (selectedClip != null) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isCurveMode) "Speed Curve" else "Speed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showSpeedPanel = false; isCurveMode = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                        
                        if (isCurveMode) {
                            val currentCurve = selectedClip.speedCurve ?: SpeedCurve()
                            var activePtIndex by remember { mutableStateOf<Int?>(null) }
                            
                            // Presets
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val curvePresets = mapOf(
                                    "Reset" to listOf(SpeedPoint(0f, 1f), SpeedPoint(1f, 1f)),
                                    "Bullet Time" to listOf(SpeedPoint(0f, 2f), SpeedPoint(0.3f, 0.2f), SpeedPoint(0.7f, 0.2f), SpeedPoint(1f, 2f)),
                                    "Ramp Up" to listOf(SpeedPoint(0f, 0.2f), SpeedPoint(1f, 3f)),
                                    "Ramp Down" to listOf(SpeedPoint(0f, 3f), SpeedPoint(1f, 0.2f)),
                                    "Pulse" to listOf(SpeedPoint(0f, 1f), SpeedPoint(0.2f, 2f), SpeedPoint(0.4f, 0.5f), SpeedPoint(0.6f, 2f), SpeedPoint(0.8f, 0.5f), SpeedPoint(1f, 1f))
                                )
                                items(curvePresets.size) { i ->
                                    val (name, presetPts) = curvePresets.entries.toList()[i]
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val newClips = clips.toMutableList()
                                            val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                            newClips[idx] = selectedClip.copy(speedCurve = SpeedCurve(presetPts.map { p -> p.copy() }))
                                            clips = newClips
                                            saveState(clips, canvasSettings, "Apply $name speed preset")
                                        },
                                        label = { Text(name) }
                                    )
                                }
                            }

                            // Interactive Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            ) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                                
                                Canvas(
                                    modifier = Modifier.fillMaxSize().pointerInput(selectedClip.id, currentCurve) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                // Find closest point to drag
                                                val w = size.width.toFloat()
                                                val h = size.height.toFloat()
                                                val touchX = offset.x / w
                                                // Y mapping: 0.1 to 4 is mapped to bottom-top
                                                val touchY = 1f - (offset.y / h)
                                                val yVal = 0.1f + touchY * 3.9f
                                                
                                                val closestIdx = currentCurve.points.indices.minByOrNull { i ->
                                                    val pt = currentCurve.points[i]
                                                    val dx = pt.x - touchX
                                                    val ptYNorm = (pt.y - 0.1f) / 3.9f
                                                    val dy = ptYNorm - touchY
                                                    Math.sqrt((dx*dx + dy*dy).toDouble())
                                                }
                                                
                                                val pt = if (closestIdx != null) currentCurve.points[closestIdx] else null
                                                if (pt != null) {
                                                    val dx = pt.x - touchX
                                                    val ptYNorm = (pt.y - 0.1f) / 3.9f
                                                    val dy = ptYNorm - touchY
                                                    if (Math.sqrt((dx*dx + dy*dy).toDouble()) < 0.15) {
                                                        activePtIndex = closestIdx
                                                    } else {
                                                        // Tap to add point (remove if already exists near?) Actually we just add
                                                        // Wait, if it's an existing point and we long press? No, let's keep it simple.
                                                        val newPts = currentCurve.points.toMutableList()
                                                        newPts.add(SpeedPoint(touchX.coerceIn(0f, 1f), yVal.coerceIn(0.1f, 4f)))
                                                        newPts.sortBy { it.x }
                                                        
                                                        val newClips = clips.toMutableList()
                                                        val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                                        newClips[idx] = selectedClip.copy(speedCurve = SpeedCurve(newPts))
                                                        clips = newClips
                                                    }
                                                } else {
                                                    // Add point if none
                                                    val newPts = currentCurve.points.toMutableList()
                                                    newPts.add(SpeedPoint(touchX.coerceIn(0f, 1f), yVal.coerceIn(0.1f, 4f)))
                                                    newPts.sortBy { it.x }
                                                    val newClips = clips.toMutableList()
                                                    val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                                    newClips[idx] = selectedClip.copy(speedCurve = SpeedCurve(newPts))
                                                    clips = newClips
                                                }
                                            },
                                            onDrag = { change, dragAmount -> // dragAmount is Offset, but we can just use position
                                                change.consume()
                                                activePtIndex?.let { aIdx ->
                                                    val w = size.width.toFloat()
                                                    val h = size.height.toFloat()
                                                    val newX = (change.position.x / w).coerceIn(0f, 1f)
                                                    var newYNorm = 1f - (change.position.y / h)
                                                    val newY = (0.1f + newYNorm * 3.9f).coerceIn(0.1f, 4f)
                                                    
                                                    val newPts = currentCurve.points.toMutableList()
                                                    // keep first and last at 0 and 1 x bounds
                                                    val finalX = if (aIdx == 0) 0f else if (aIdx == newPts.size - 1) 1f else newX
                                                    newPts[aIdx] = SpeedPoint(finalX, newY)
                                                    newPts.sortBy { it.x }
                                                    
                                                    // Update active index if it moved
                                                    activePtIndex = newPts.indexOfFirst { it.x == finalX && it.y == newY }
                                                    
                                                    val newClips = clips.toMutableList()
                                                    val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                                    newClips[idx] = selectedClip.copy(speedCurve = SpeedCurve(newPts))
                                                    clips = newClips
                                                }
                                            },
                                            onDragEnd = {
                                                // keep activePtIndex to allow deletion
                                                saveState(clips, canvasSettings, "Edit speed curve")
                                            }
                                        )
                                    }
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    
                                    // Draw grid
                                    val normalY = h * (1f - (1f - 0.1f) / 3.9f)
                                    drawLine(Color.Gray.copy(alpha=0.5f), androidx.compose.ui.geometry.Offset(0f, normalY), androidx.compose.ui.geometry.Offset(w, normalY), strokeWidth = 1f)
                                    
                                    if (currentCurve.points.size >= 2) {
                                        val path = androidx.compose.ui.graphics.Path()
                                        
                                        // Draw smooth line
                                        val steps = 100
                                        path.moveTo(currentCurve.points.first().x * w, h * (1f - (currentCurve.points.first().y - 0.1f)/3.9f))
                                        for (i in 1..steps) {
                                            val xNorm = i / steps.toFloat()
                                            val yVal = currentCurve.getSpeedAt(xNorm)
                                            val yNorm = (yVal - 0.1f) / 3.9f
                                            path.lineTo(xNorm * w, h * (1f - yNorm))
                                        }
                                        drawPath(path, primaryColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap=androidx.compose.ui.graphics.StrokeCap.Round))
                                        
                                        // Draw points
                                        currentCurve.points.forEachIndexed { idx, pt ->
                                            val px = pt.x * w
                                            val py = h * (1f - (pt.y - 0.1f) / 3.9f)
                                            drawCircle(if (activePtIndex == idx) Color.White else primaryColor, radius = 20f, center = androidx.compose.ui.geometry.Offset(px, py))
                                            drawCircle(onPrimaryColor, radius = 16f, center = androidx.compose.ui.geometry.Offset(px, py))
                                        }
                                    }
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tap to add/select. Drag to frame.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                if (activePtIndex != null && activePtIndex != 0 && activePtIndex != (selectedClip.speedCurve?.points?.size?.minus(1) ?: 1)) {
                                    IconButton(
                                        onClick = {
                                            activePtIndex?.let { aIdx ->
                                                val currentCurve = selectedClip.speedCurve ?: SpeedCurve()
                                                val newPts = currentCurve.points.toMutableList()
                                                if (newPts.size > 2) {
                                                    newPts.removeAt(aIdx)
                                                    
                                                    val newClips = clips.toMutableList()
                                                    val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                                    newClips[idx] = selectedClip.copy(speedCurve = SpeedCurve(newPts))
                                                    clips = newClips
                                                    activePtIndex = null
                                                    saveState(clips, canvasSettings, "Delete speed curve point")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Delete Point")
                                    }
                                }
                                FilledTonalButton(onClick = { isCurveMode = false }) {
                                    Text("Basic Speed")
                                }
                            }
                        } else {
                            // Presets
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val presets = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 4f)
                                items(presets.size) { i ->
                                    val speedOpt = presets[i]
                                    FilterChip(
                                        selected = Math.abs(selectedClip.playbackSpeed - speedOpt) < 0.01f,
                                        onClick = { 
                                            val newClips = clips.toMutableList()
                                            val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                            newClips[idx] = selectedClip.copy(playbackSpeed = speedOpt, speedCurve = null)
                                            saveState(newClips, canvasSettings, "Change clip speed")
                                        },
                                        label = { Text("${if (speedOpt == 1f) "Normal" else "${speedOpt}x"}") }
                                    )
                                }
                            }
                            
                            // Slider
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("0.1x", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = selectedClip.playbackSpeed,
                                    onValueChange = { newSpeed ->
                                        val newClips = clips.toMutableList()
                                        val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                        newClips[idx] = selectedClip.copy(playbackSpeed = newSpeed, speedCurve = null)
                                        clips = newClips 
                                    },
                                    onValueChangeFinished = {
                                        saveState(clips, canvasSettings, "Change speed via slider")
                                    },
                                    valueRange = 0.1f..10f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                Text("10x", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(String.format("Current: %.2fx", selectedClip.playbackSpeed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedClip.maintainPitch,
                                    onCheckedChange = { checked ->
                                        val newClips = clips.toMutableList()
                                        val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                        newClips[idx] = selectedClip.copy(maintainPitch = checked)
                                        saveState(newClips, canvasSettings, "Toggle pitch preserve")
                                    }
                                )
                                Text("Keep original pitch", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.weight(1f))
                                FilledTonalButton(onClick = { isCurveMode = true }) {
                                    Text("Curve")
                                }
                            }
                        }
                        
                        val oldDurSecs = (selectedClip.trimEndMs - selectedClip.trimStartMs) / 1000f
                        val newDurSecs = (selectedClip.durationMs) / 1000f
                        Text("Duration: ${String.format("%.1fs", oldDurSecs)} → ${String.format("%.1fs", newDurSecs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showTransformPanel && selectedClipId != null,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val selectedClip = clips.find { it.id == selectedClipId }
            if (selectedClip != null) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Transform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row {
                                TextButton(onClick = {
                                    val newClips = clips.toMutableList()
                                    val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                    newClips[idx] = selectedClip.copy(
                                        rotation = 0f,
                                        flipHorizontal = false,
                                        flipVertical = false,
                                        cropRect = androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)
                                    )
                                    saveState(newClips, canvasSettings, "Reset transform")
                                }) { Text("Reset") }
                                IconButton(onClick = { showTransformPanel = false }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close")
                                }
                            }
                        }
                        
                        Text("Rotate & Flip", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(onClick = {
                                val newClips = clips.toMutableList()
                                val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                newClips[idx] = selectedClip.copy(rotation = (selectedClip.rotation - 90f) % 360f)
                                saveState(newClips, canvasSettings, "Rotate left")
                            }) { Icon(Icons.Filled.RotateLeft, "Rotate Left") }
                            
                            FilledTonalIconButton(onClick = {
                                val newClips = clips.toMutableList()
                                val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                newClips[idx] = selectedClip.copy(rotation = (selectedClip.rotation + 90f) % 360f)
                                saveState(newClips, canvasSettings, "Rotate right")
                            }) { Icon(Icons.Filled.RotateRight, "Rotate Right") }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            FilledTonalButton(onClick = {
                                val newClips = clips.toMutableList()
                                val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                newClips[idx] = selectedClip.copy(flipHorizontal = !selectedClip.flipHorizontal)
                                saveState(newClips, canvasSettings, "Mirror horizontal")
                            }) { Text("Mirror") }
                            
                            FilledTonalButton(onClick = {
                                val newClips = clips.toMutableList()
                                val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                newClips[idx] = selectedClip.copy(flipVertical = !selectedClip.flipVertical)
                                saveState(newClips, canvasSettings, "Flip vertical")
                            }) { Text("Flip V") }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Angle:", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                value = selectedClip.rotation,
                                onValueChange = { newVal ->
                                    val newClips = clips.toMutableList()
                                    val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                    newClips[idx] = selectedClip.copy(rotation = newVal)
                                    clips = newClips
                                },
                                onValueChangeFinished = { saveState(clips, canvasSettings, "Change rotation angle via slider") },
                                valueRange = -180f..180f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${selectedClip.rotation.toInt()}°", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        Divider()
                        
                        Text("Crop Ratio", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val ratios = listOf(
                                "Free" to null,
                                "1:1" to 1f,
                                "4:5" to 4f/5f,
                                "9:16" to 9f/16f,
                                "16:9" to 16f/9f
                            )
                            items(ratios.size) { i ->
                                val (name, ratioVal) = ratios[i]
                                val currentW = selectedClip.cropRect.width
                                val currentH = selectedClip.cropRect.height
                                val isSelected = if (ratioVal == null) {
                                    // free form (not checking exactly for now)
                                    false
                                } else {
                                    Math.abs((currentW / currentH) - ratioVal) < 0.05f
                                }
                                
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (ratioVal != null) {
                                            // Apply ratio, centering it
                                            var newW = 1f
                                            var newH = 1f / ratioVal
                                            if (newH > 1f) {
                                                newH = 1f
                                                newW = ratioVal
                                            }
                                            val newL = (1f - newW) / 2f
                                            val newT = (1f - newH) / 2f
                                            val newR = newL + newW
                                            val newB = newT + newH
                                            
                                            val newClips = clips.toMutableList()
                                            val idx = newClips.indexOfFirst { it.id == selectedClip.id }
                                            newClips[idx] = selectedClip.copy(cropRect = androidx.compose.ui.geometry.Rect(newL, newT, newR, newB))
                                            saveState(newClips, canvasSettings, "Change crop ratio")
                                        }
                                    },
                                    label = { Text(name) }
                                )
                            }
                        }
                        
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showOverlayPanel,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Overlay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedOverlayId != null) {
                                val cOverlay = overlays.find { it.id == selectedOverlayId }
                                if (cOverlay != null) {
                                    IconButton(onClick = { globalClipboardItem = cOverlay }) { Icon(Icons.Filled.ContentCopy, "Copy") }
                                    IconButton(onClick = {
                                        val newItem = cOverlay.copy(id = java.util.UUID.randomUUID().toString(), startTimeOnTimelineMs = cOverlay.startTimeOnTimelineMs + 100)
                                        overlays = overlays + newItem
                                        layerOrder = layerOrder + newItem.id
                                        saveState(clips, canvasSettings, "Duplicate Overlay")
                                    }) { Icon(Icons.Filled.FileCopy, "Duplicate") }
                                    IconButton(onClick = {
                                        overlays = overlays.filter { it.id != cOverlay.id }
                                        selectedOverlayId = null
                                        showOverlayPanel = false
                                        saveState(clips, canvasSettings, "Delete Overlay")
                                    }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                            IconButton(onClick = { showOverlayPanel = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                    }
                    
                    if (selectedOverlayId == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (isBudgetMode && layerOrder.size >= 3) {
                                        android.widget.Toast.makeText(context, "Budget Mode limit: 3 layers max", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        isPickingOverlay = true
                                        showMediaPicker = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add Photo/Video")
                            }
                        }
                    } else {
                        val overlay = overlays.find { it.id == selectedOverlayId }
                        if (overlay != null) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val blendModes = OverlayBlendModeType.values()
                                items(blendModes.size) { i ->
                                    val bm = blendModes[i]
                                    FilterChip(
                                        selected = overlay.blendMode == bm,
                                        onClick = {
                                            val newOverlays = overlays.toMutableList()
                                            val idx = newOverlays.indexOfFirst { it.id == overlay.id }
                                            newOverlays[idx] = overlay.copy(blendMode = bm)
                                            saveOverlayState(newOverlays, "Set blend mode")
                                        },
                                        label = { Text(bm.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Opacity: ${(overlay.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(80.dp))
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = overlay.opacity,
                                    onValueChange = { newVal ->
                                        val newOverlays = overlays.toMutableList()
                                        val idx = newOverlays.indexOfFirst { it.id == overlay.id }
                                        newOverlays[idx] = overlay.copy(opacity = newVal)
                                        overlays = newOverlays
                                    },
                                    onValueChangeFinished = { saveOverlayState(overlays, "Change overlay opacity") },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Shadow: ${overlay.shadowRadius.toInt()}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(80.dp))
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = overlay.shadowRadius,
                                    onValueChange = { newVal ->
                                        val newOverlays = overlays.toMutableList()
                                        val idx = newOverlays.indexOfFirst { it.id == overlay.id }
                                        newOverlays[idx] = overlay.copy(shadowRadius = newVal, shadowColor = Color.Black.copy(alpha = 0.5f))
                                        overlays = newOverlays
                                    },
                                    onValueChangeFinished = { saveOverlayState(overlays, "Change shadow") },
                                    valueRange = 0f..50f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Border: ${overlay.borderWidth.toInt()}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(80.dp))
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = overlay.borderWidth,
                                    onValueChange = { newVal ->
                                        val newOverlays = overlays.toMutableList()
                                        val idx = newOverlays.indexOfFirst { it.id == overlay.id }
                                        newOverlays[idx] = overlay.copy(borderWidth = newVal, borderColor = Color.White)
                                        overlays = newOverlays
                                    },
                                    onValueChangeFinished = { saveOverlayState(overlays, "Change border") },
                                    valueRange = 0f..20f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Entrance:")
                                val anims = OverlayAnim.values()
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 8.dp)) {
                                    items(anims.size) { i ->
                                        val anim = anims[i]
                                        FilterChip(
                                            selected = overlay.entranceAnim == anim,
                                            onClick = {
                                                val newOverlays = overlays.toMutableList()
                                                val idx = newOverlays.indexOfFirst { it.id == overlay.id }
                                                newOverlays[idx] = overlay.copy(entranceAnim = anim)
                                                saveOverlayState(newOverlays, "Set entrance anim")
                                            },
                                            label = { Text(anim.name.lowercase().capitalize()) }
                                        )
                                    }
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Exit:")
                                val anims = OverlayAnim.values()
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 8.dp)) {
                                    items(anims.size) { i ->
                                        val anim = anims[i]
                                        FilterChip(
                                            selected = overlay.exitAnim == anim,
                                            onClick = {
                                                val newOverlays = overlays.toMutableList()
                                                val idx = newOverlays.indexOfFirst { it.id == overlay.id }
                                                newOverlays[idx] = overlay.copy(exitAnim = anim)
                                                saveOverlayState(newOverlays, "Set exit anim")
                                            },
                                            label = { Text(anim.name.lowercase().capitalize()) }
                                        )
                                    }
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Mask:")
                                val masks = MaskShape.values()
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 8.dp)) {
                                    items(masks.size) { i ->
                                        val mask = masks[i]
                                        FilterChip(
                                            selected = overlay.maskShape == mask,
                                            onClick = {
                                                val newOverlays = overlays.toMutableList()
                                                val idx = newOverlays.indexOfFirst { it.id == overlay.id }
                                                newOverlays[idx] = overlay.copy(maskShape = mask)
                                                saveOverlayState(newOverlays, "Set mask shape")
                                            },
                                            label = { Text(mask.name.lowercase().capitalize()) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showFiltersPanel,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val clipId = selectedClipId
            val clipIndex = clips.indexOfFirst { it.id == clipId }
            val clip = clips.getOrNull(clipIndex)
            
            if (clip != null) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showFiltersPanel = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                        
                        // Categories and Filters
                        val categories = FilterType.values().groupBy { it.category }
                        val frameTimeForThumbnails = remember(showFiltersPanel) { currentPositionMs }
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            categories.forEach { (categoryName, types) ->
                                item {
                                    Column {
                                        Text(categoryName, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            types.forEach { tType ->
                                                val isSelected = clip.filterType == tType
                                                val filterMatrix = getColorMatrixForFilter(tType, 1f)
                                                
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.clickable { 
                                                        val newClips = clips.toMutableList()
                                                        newClips[clipIndex] = clip.copy(filterType = tType)
                                                        saveState(newClips, canvasSettings, "Change filter")
                                                    }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                            .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                                    ) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(LocalContext.current)
                                                                .data(android.net.Uri.parse(clip.sourceUri))
                                                                .videoFrameMillis(frameTimeForThumbnails)
                                                                .crossfade(true)
                                                                .build(),
                                                            imageLoader = imageLoader,
                                                            contentDescription = "Filter Preview",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize(),
                                                            colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(filterMatrix)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(tType.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (clip.filterType != FilterType.NONE) {
                            Divider()
                            Text("Intensity", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("0%", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = clip.filterIntensity,
                                    onValueChange = { newVal ->
                                        val newClips = clips.toMutableList()
                                        newClips[clipIndex] = clip.copy(filterIntensity = newVal)
                                        clips = newClips
                                    },
                                    onValueChangeFinished = {
                                        saveState(clips, canvasSettings, "Change filter intensity")
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                Text("100%", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showExportPanel,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            ExportSettingsScreen(
                clips = clips,
                editorState = EditorState(
                    clips = clips,
                    canvasSettings = canvasSettings,
                    overlays = overlays,
                    texts = texts,
                    captions = captions,
                    captionSettings = captionSettings,
                    stickers = stickers,
                    drawings = drawings,
                    frames = frames,
                    audioClips = audioClips,
                    layerOrder = layerOrder
                ),
                videoDurationMs = videoDurationMs,
                thumbnailUri = clips.firstOrNull()?.sourceUri,
                onClose = { showExportPanel = false },
                onExportComplete = { showExportPanel = false }
            )
        }
        
        AnimatedVisibility(
            visible = showLutPanel,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val clipId = selectedClipId
            val clipIndex = clips.indexOfFirst { it.id == clipId }
            val clip = clips.getOrNull(clipIndex)
            
            if (clip != null) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Color Grading (LUTs)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row {
                                Button(onClick = {
                                    val newGradeId = java.util.UUID.randomUUID().toString()
                                    LutsManager.customGrades.add(
                                        CustomColorGrade(newGradeId, "Grade ${LutsManager.customGrades.size + 1}", clip.adjustments)
                                    )
                                    android.widget.Toast.makeText(context, "Preset saved!", android.widget.Toast.LENGTH_SHORT).show()
                                }) { Text("Save Preset") }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { showLutPanel = false }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close")
                                }
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Intensity", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(70.dp))
                            Slider(
                                value = clip.adjustments.lutIntensity,
                                onValueChange = { v -> 
                                    val newClips = clips.toMutableList()
                                    val newAdjustments = clip.adjustments.copy(lutIntensity = v)
                                    newClips[clipIndex] = clip.copy(adjustments = newAdjustments)
                                    clips = newClips
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val frameTimeForThumbnails = remember(showLutPanel) { currentPositionMs }
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                    val newClips = clips.toMutableList()
                                    val newAdjustments = clip.adjustments.copy(lutPresetId = null)
                                    newClips[clipIndex] = clip.copy(adjustments = newAdjustments)
                                    clips = newClips
                                    saveState(newClips, canvasSettings, "Remove LUT")
                                }.width(80.dp)) {
                                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(2.dp, if(clip.adjustments.lutPresetId == null) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Block, contentDescription = "None", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Original", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            item {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                    lutFilePicker.launch("*/*")
                                }.width(80.dp)) {
                                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Add, contentDescription = "Import .cube", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Import .cube", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            
                            val allLuts = LutsManager.builtInLuts + LutsManager.importedLuts
                            items(allLuts.size) { i ->
                                val lut = allLuts[i]
                                val isSelected = clip.adjustments.lutPresetId == lut.id
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                    val newClips = clips.toMutableList()
                                    val newAdjustments = clip.adjustments.copy(lutPresetId = lut.id)
                                    newClips[clipIndex] = clip.copy(adjustments = newAdjustments)
                                    clips = newClips
                                    saveState(newClips, canvasSettings, "Apply LUT")
                                }.width(80.dp)) {
                                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(2.dp, if(isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))) {
                                        val tempAdjs = ColorAdjustments(lutPresetId = lut.id, lutIntensity = 1f)
                                        Box(modifier = Modifier.fillMaxSize().colorFilterOverlay(getColorMatrixForAdjustments(tempAdjs))) {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(context)
                                                    .data(android.net.Uri.parse(clip.sourceUri))
                                                    .videoFrameMillis(frameTimeForThumbnails)
                                                    .crossfade(true).build(),
                                                imageLoader = imageLoader,
                                                contentDescription = "Preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(lut.name, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
                                }
                            }
                        }
                        
                        if (LutsManager.customGrades.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Saved Custom Presets", style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(LutsManager.customGrades.size) { i ->
                                    val grade = LutsManager.customGrades[i]
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                        val newClips = clips.toMutableList()
                                        newClips[clipIndex] = clip.copy(adjustments = grade.adjustments)
                                        clips = newClips
                                        saveState(newClips, canvasSettings, "Apply Custom Preset")
                                    }.width(80.dp)) {
                                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                            Box(modifier = Modifier.fillMaxSize().colorFilterOverlay(getColorMatrixForAdjustments(grade.adjustments))) {
                                                AsyncImage(
                                                        model = coil.request.ImageRequest.Builder(context)
                                                            .data(android.net.Uri.parse(clip.sourceUri))
                                                            .videoFrameMillis(frameTimeForThumbnails)
                                                            .crossfade(true).build(),
                                                        imageLoader = imageLoader,
                                                        contentDescription = "Preview",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(grade.name, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showAdjustPanel,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val clipId = selectedClipId
            val clipIndex = clips.indexOfFirst { it.id == clipId }
            val clip = clips.getOrNull(clipIndex)
            
            if (clip != null) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Adjust (Color)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { },
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isComparePressed = true
                                                try {
                                                    tryAwaitRelease()
                                                } finally {
                                                    isComparePressed = false
                                                }
                                            }
                                        )
                                    }
                                ) {
                                    Text("Compare")
                                }
                                TextButton(onClick = {
                                    val newClips = clips.toMutableList()
                                    newClips[clipIndex] = clip.copy(adjustments = ColorAdjustments())
                                    clips = newClips
                                    saveState(clips, canvasSettings, "Reset color adjustments")
                                }) {
                                    Text("Reset All")
                                }
                                IconButton(onClick = { showAdjustPanel = false }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close")
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        val adjustments = clip.adjustments
                        val updateAdjustment: (ColorAdjustments.() -> ColorAdjustments) -> Unit = { updater ->
                            val newClips = clips.toMutableList()
                            newClips[clipIndex] = clip.copy(adjustments = clip.adjustments.updater())
                            clips = newClips
                        }
                        
                        data class SliderConfig(val label: String, val range: ClosedFloatingPointRange<Float>, val value: Float, val onChange: (Float) -> Unit)
                        val sliders = listOf(
                            SliderConfig("Brightness", -100f..100f, adjustments.brightness) { v: Float -> updateAdjustment { copy(brightness = v) } },
                            SliderConfig("Contrast", -100f..100f, adjustments.contrast) { v: Float -> updateAdjustment { copy(contrast = v) } },
                            SliderConfig("Saturation", -100f..100f, adjustments.saturation) { v: Float -> updateAdjustment { copy(saturation = v) } },
                            SliderConfig("Warmth", -100f..100f, adjustments.warmth) { v: Float -> updateAdjustment { copy(warmth = v) } },
                            SliderConfig("Tint", -100f..100f, adjustments.tint) { v: Float -> updateAdjustment { copy(tint = v) } },
                            SliderConfig("Highlights", -100f..100f, adjustments.highlights) { v: Float -> updateAdjustment { copy(highlights = v) } },
                            SliderConfig("Shadows", -100f..100f, adjustments.shadows) { v: Float -> updateAdjustment { copy(shadows = v) } },
                            SliderConfig("Sharpness", 0f..100f, adjustments.sharpness) { v: Float -> updateAdjustment { copy(sharpness = v) } },
                            SliderConfig("Vignette", 0f..100f, adjustments.vignette) { v: Float -> updateAdjustment { copy(vignette = v) } },
                            SliderConfig("Grain / Noise", 0f..100f, adjustments.grain) { v: Float -> updateAdjustment { copy(grain = v) } }
                        )

                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).padding(bottom = 16.dp)
                        ) {
                            items(sliders.size) { i ->
                                val (label, range, value, onChange) = sliders[i]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.25f))
                                    Slider(
                                        value = value,
                                        onValueChange = { onChange(it) },
                                        onValueChangeFinished = {
                                            saveState(clips, canvasSettings, "Adjust $label")
                                        },
                                        valueRange = range,
                                        modifier = Modifier.weight(0.6f)
                                    )
                                    Text("${value.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.15f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showEffectsPanel,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val clipId = selectedClipId
            val clipIndex = clips.indexOfFirst { it.id == clipId }
            val clip = clips.getOrNull(clipIndex)
            
            if (clip != null) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Effects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showEffectsPanel = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                        
                        val frameTimeForThumbnails = remember(showEffectsPanel) { currentPositionMs }
                        val categories = EffectCategory.values()
                        var selectedCategory by remember { mutableStateOf(categories.first()) }
                        
                        ScrollableTabRow(
                            selectedTabIndex = categories.indexOf(selectedCategory),
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent
                        ) {
                            categories.forEach { cat ->
                                Tab(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    text = { Text(cat.title) }
                                )
                            }
                        }
                        
                        if (clip.effects.isNotEmpty()) {
                            Text("Applied Effects", style = MaterialTheme.typography.labelMedium)
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.heightIn(max=150.dp).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(clip.effects.size) { i ->
                                    val eff = clip.effects[i]
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(eff.type.label, style = MaterialTheme.typography.bodyMedium)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Strength", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                                                Slider(
                                                    value = eff.intensity,
                                                    onValueChange = { newVal ->
                                                        val newClips = clips.toMutableList()
                                                        val newEffects = clip.effects.toMutableList()
                                                        newEffects[i] = eff.copy(intensity = newVal)
                                                        newClips[clipIndex] = clip.copy(effects = newEffects)
                                                        clips = newClips
                                                    },
                                                    valueRange = 0f..1f,
                                                    modifier = Modifier.height(24.dp).weight(1f)
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Time", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                                                val startF = eff.startTimeMs.toFloat()
                                                val endF = if (eff.endTimeMs == -1L || eff.endTimeMs > clip.durationMs) clip.durationMs.toFloat() else eff.endTimeMs.toFloat()
                                                androidx.compose.material3.RangeSlider(
                                                    value = startF..endF,
                                                    onValueChange = { range ->
                                                        val newClips = clips.toMutableList()
                                                        val newEffects = clip.effects.toMutableList()
                                                        newEffects[i] = eff.copy(startTimeMs = range.start.toLong(), endTimeMs = range.endInclusive.toLong())
                                                        newClips[clipIndex] = clip.copy(effects = newEffects)
                                                        clips = newClips
                                                    },
                                                    valueRange = 0f..kotlin.math.max(1f, clip.durationMs.toFloat()),
                                                    modifier = Modifier.height(24.dp).weight(1f)
                                                )
                                            }
                                        }
                                        IconButton(onClick = {
                                            val newClips = clips.toMutableList()
                                            val newEffects = clip.effects.toMutableList()
                                            newEffects.removeAt(i)
                                            newClips[clipIndex] = clip.copy(effects = newEffects)
                                            clips = newClips
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                        }
                                    }
                                }
                            }
                            Divider()
                        }
                        
                        val availableEffects = EffectType.values().filter { it.category == selectedCategory }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(availableEffects.size) { i ->
                                val tType = availableEffects[i]
                                val isSelected = clip.effects.any { it.type == tType }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { 
                                        if (!isSelected) {
                                            val newClips = clips.toMutableList()
                                            val newEffects = clip.effects.toMutableList()
                                            newEffects.add(AppliedEffect(type = tType))
                                            newClips[clipIndex] = clip.copy(effects = newEffects)
                                            saveState(newClips, canvasSettings, "Add effect")
                                        }
                                    }.width(80.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    ) {
                                        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = tType.name)
                                        val animTimeMs by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 2000f,
                                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                                animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                                                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                                            ),
                                            label = "time"
                                        )
                                        
                                        Box(
                                            modifier = Modifier.fillMaxSize().applyAllVisualEffects(listOf(AppliedEffect(type = tType, intensity = 1f)), animTimeMs.toLong(), 2000L)
                                        ) {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                    .data(android.net.Uri.parse(clip.sourceUri))
                                                    .videoFrameMillis(frameTimeForThumbnails)
                                                    .crossfade(true)
                                                    .build(),
                                                imageLoader = imageLoader,
                                                contentDescription = "Effect Preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(tType.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showTextToolbar,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val cText = texts.find { it.id == selectedTextId }
            if (cText != null) {
                TextToolbar(
                    textOverlay = cText,
                    isEditing = editingTextId == cText.id,
                    onCloseEditing = { editingTextId = if (editingTextId == cText.id) null else cText.id },
                    onCloseToolbar = {
                        showTextToolbar = false
                        editingTextId = null
                    },
                    onCopy = { globalClipboardItem = cText },
                    onDuplicate = {
                        val newItem = cText.copy(id = java.util.UUID.randomUUID().toString(), startTimeOnTimelineMs = cText.startTimeOnTimelineMs + 100)
                        texts = texts + newItem
                        layerOrder = layerOrder + newItem.id
                        saveState(clips, canvasSettings, "Duplicate Text")
                    },
                    onPasteStyle = {
                        val src = globalClipboardItem as? TextOverlay
                        if (src != null) {
                            val updatedText = cText.copy(
                                fontName = src.fontName, fontSize = src.fontSize, textColor = src.textColor, backgroundColor = src.backgroundColor,
                                alignment = src.alignment, isBold = src.isBold, isItalic = src.isItalic, isUnderline = src.isUnderline,
                                strokeColor = src.strokeColor, strokeWidth = src.strokeWidth, shadowColor = src.shadowColor, shadowOffsetX = src.shadowOffsetX,
                                shadowOffsetY = src.shadowOffsetY, shadowBlur = src.shadowBlur, letterSpacing = src.letterSpacing, lineHeightMultiplier = src.lineHeightMultiplier,
                                is3D = src.is3D, animIn = src.animIn, animOut = src.animOut, animLoop = src.animLoop,
                                animInDurationMs = src.animInDurationMs, animOutDurationMs = src.animOutDurationMs, animLoopDurationMs = src.animLoopDurationMs
                            )
                            val newTexts = texts.toMutableList()
                            val idx = newTexts.indexOfFirst { it.id == updatedText.id }
                            if (idx != -1) {
                                newTexts[idx] = updatedText
                                texts = newTexts
                                saveState(clips, canvasSettings, "Paste Text Style")
                            }
                        }
                    },
                    onUpdate = { updatedText ->
                        val newTexts = texts.toMutableList()
                        val idx = newTexts.indexOfFirst { it.id == updatedText.id }
                        if (idx != -1) {
                            newTexts[idx] = updatedText
                            texts = newTexts
                        }
                    }
                )
            }
        }
        
        AnimatedVisibility(
            visible = showStickerPicker,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            StickerPicker(
                onClose = { showStickerPicker = false },
                onSelectSticker = { stickerModel ->
                    val newSticker = StickerOverlay(
                        modelId = stickerModel.id,
                        content = stickerModel.content,
                        category = stickerModel.category,
                        isIcon = stickerModel.isIcon,
                        animLoop = stickerModel.defaultAnimLoop,
                        startTimeOnTimelineMs = currentPositionMs,
                        durationMs = 5000L
                    )
                    stickers = stickers + newSticker
                    selectedStickerId = newSticker.id
                    showStickerPicker = false
                    showStickerToolbar = true
                    
                    selectedClipId = null
                    selectedTextId = null
                    selectedOverlayId = null
                }
            )
        }
        
        AnimatedVisibility(
            visible = showStickerToolbar,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val cSticker = stickers.find { it.id == selectedStickerId }
            if (cSticker != null) {
                StickerToolbar(
                    stickerOverlay = cSticker,
                    onCloseToolbar = { showStickerToolbar = false },
                    onDelete = {
                        stickers = stickers.filter { it.id != cSticker.id }
                        showStickerToolbar = false
                    },
                    onCopy = { globalClipboardItem = cSticker },
                    onDuplicate = {
                        val newItem = cSticker.copy(id = java.util.UUID.randomUUID().toString(), startTimeOnTimelineMs = cSticker.startTimeOnTimelineMs + 100)
                        stickers = stickers + newItem
                        layerOrder = layerOrder + newItem.id
                        saveState(clips, canvasSettings, "Duplicate Sticker")
                    },
                    onUpdate = { updatedSticker ->
                        val newStickers = stickers.toMutableList()
                        val idx = newStickers.indexOfFirst { it.id == updatedSticker.id }
                        if (idx != -1) {
                            newStickers[idx] = updatedSticker
                            stickers = newStickers
                        }
                    }
                )
            }
        }
        
        AnimatedVisibility(
            visible = showFramePicker,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            FramePicker(
                currentPositionMs = currentPositionMs,
                onAddFrame = { newFrame ->
                    frames = frames + newFrame
                    selectedFrameId = newFrame.id
                    showFramePicker = false
                    showFrameToolbar = true
                    saveState(clips, canvasSettings, "Add frame")
                },
                onClose = { showFramePicker = false }
            )
        }
        
        AnimatedVisibility(
            visible = showFrameToolbar,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val cFrame = frames.find { it.id == selectedFrameId }
            if (cFrame != null) {
                FrameToolbar(
                    frameOverlay = cFrame,
                    onCloseToolbar = { showFrameToolbar = false },
                    onDelete = {
                        frames = frames.filter { it.id != cFrame.id }
                        showFrameToolbar = false
                        saveState(clips, canvasSettings, "Delete frame")
                    },
                    onCopy = { globalClipboardItem = cFrame },
                    onDuplicate = {
                        val newItem = cFrame.copy(id = java.util.UUID.randomUUID().toString(), startTimeOnTimelineMs = cFrame.startTimeOnTimelineMs + 100)
                        frames = frames + newItem
                        layerOrder = layerOrder + newItem.id
                        saveState(clips, canvasSettings, "Duplicate Frame")
                    },
                    onUpdate = { updatedFrame ->
                        val newFrames = frames.toMutableList()
                        val idx = newFrames.indexOfFirst { it.id == updatedFrame.id }
                        if (idx != -1) {
                            newFrames[idx] = updatedFrame
                            frames = newFrames
                        }
                    }
                )
            }
        }
        
        AnimatedVisibility(
            visible = showDrawToolbar,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val activeDrawing = drawings.find { it.id == selectedDrawId }
            DrawToolbar(
                brushType = currentBrushType,
                onBrushTypeChange = { currentBrushType = it },
                brushSize = currentBrushSize,
                onBrushSizeChange = { currentBrushSize = it },
                brushColor = currentBrushColor,
                onBrushColorChange = { currentBrushColor = it },
                isEraser = currentIsEraser,
                onEraserChange = { currentIsEraser = it },
                onUndoStoke = {
                    if (activeDrawing != null && activeDrawing.strokes.isNotEmpty()) {
                        val newDrawings = drawings.toMutableList()
                        val idx = newDrawings.indexOfFirst { it.id == activeDrawing.id }
                        if (idx != -1) {
                            val active = newDrawings[idx]
                            newDrawings[idx] = active.copy(strokes = active.strokes.dropLast(1))
                            drawings = newDrawings
                            saveState(clips, canvasSettings, "Undo stroke")
                        }
                    }
                },
                onDeleteOverlay = {
                    if (activeDrawing != null) {
                        drawings = drawings.filter { it.id != activeDrawing.id }
                        selectedDrawId = null
                        showDrawToolbar = false
                        isDrawingMode = false
                        saveState(clips, canvasSettings, "Delete drawing")
                    }
                },
                onCloseToolbar = {
                    showDrawToolbar = false
                    isDrawingMode = false
                },
                onCopy = { globalClipboardItem = activeDrawing },
                onDuplicate = {
                    if (activeDrawing != null) {
                        val newItem = activeDrawing.copy(id = java.util.UUID.randomUUID().toString(), startTimeOnTimelineMs = activeDrawing.startTimeOnTimelineMs + 100)
                        drawings = drawings + newItem
                        layerOrder = layerOrder + newItem.id
                        saveState(clips, canvasSettings, "Duplicate Drawing")
                    }
                },
                onToggleAnim = { isAnim ->
                    if (activeDrawing != null) {
                        val newDrawings = drawings.toMutableList()
                        val idx = newDrawings.indexOfFirst { it.id == activeDrawing.id }
                        if (idx != -1) {
                            val active = newDrawings[idx]
                            newDrawings[idx] = active.copy(isAnimated = isAnim)
                            drawings = newDrawings
                            saveState(clips, canvasSettings, "Toggle Draw Animation")
                        }
                    }
                },
                isAnimated = activeDrawing?.isAnimated ?: false,
                onChangeDuration = { durMs ->
                    if (activeDrawing != null) {
                        val newDrawings = drawings.toMutableList()
                        val idx = newDrawings.indexOfFirst { it.id == activeDrawing.id }
                        if (idx != -1) {
                            val active = newDrawings[idx]
                            newDrawings[idx] = active.copy(durationMs = durMs)
                            drawings = newDrawings
                            saveState(clips, canvasSettings, "Update Draw Duration")
                        }
                    }
                },
                durationMs = activeDrawing?.durationMs ?: 5000L
            )
        }
        
        AnimatedVisibility(
            visible = showLayerPanel,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
                shadowElevation = 16.dp
            ) {
                LayerManagementPanel(
                    clips = clips,
                    overlays = overlays,
                    texts = texts,
                    stickers = stickers,
                    drawings = drawings,
                    frames = frames,
                    layerOrder = layerOrder,
                    onUpdateLayerOrder = { newOrder ->
                        layerOrder = newOrder
                        persistHistory()
                    },
                    onDeleteLayers = { ids ->
                        val nOverlays = overlays.filter { it.id !in ids }
                        val nTexts = texts.filter { it.id !in ids }
                        val nStickers = stickers.filter { it.id !in ids }
                        val nDraws = drawings.filter { it.id !in ids }
                        val nFrames = frames.filter { it.id !in ids }
                        var changed = overlays.size != nOverlays.size || texts.size != nTexts.size || stickers.size != nStickers.size || drawings.size != nDraws.size || frames.size != nFrames.size
                        if (changed) {
                            overlays = nOverlays
                            texts = nTexts
                            stickers = nStickers
                            drawings = nDraws
                            frames = nFrames
                            layerOrder = layerOrder.filter { it !in ids }
                            persistHistory()
                        }
                    },
                    onUpdateOpacity = { id, opacity ->
                        var changed = false
                        val mOverlays = overlays.map { if (it.id == id) { changed = true; it.copy(opacity = opacity) } else it }
                        val mTexts = texts.map { if (it.id == id) { changed = true; it.copy(opacity = opacity) } else it }
                        val mStickers = stickers.map { if (it.id == id) { changed = true; it.copy(opacity = opacity) } else it }
                        val mDraws = drawings.map { if (it.id == id) { changed = true; it.copy(opacity = opacity) } else it }
                        val mFrames = frames.map { if (it.id == id) { changed = true; it.copy(opacity = opacity) } else it }
                        if (changed) {
                            overlays = mOverlays; texts = mTexts; stickers = mStickers; drawings = mDraws; frames = mFrames
                            persistHistory()
                        }
                    },
                    onUpdateVisibility = { id, isVisible ->
                        var changed = false
                        val mOverlays = overlays.map { if (it.id == id) { changed = true; it.copy(isVisible = isVisible) } else it }
                        val mTexts = texts.map { if (it.id == id) { changed = true; it.copy(isVisible = isVisible) } else it }
                        val mStickers = stickers.map { if (it.id == id) { changed = true; it.copy(isVisible = isVisible) } else it }
                        val mDraws = drawings.map { if (it.id == id) { changed = true; it.copy(isVisible = isVisible) } else it }
                        val mFrames = frames.map { if (it.id == id) { changed = true; it.copy(isVisible = isVisible) } else it }
                        if (changed) {
                            overlays = mOverlays; texts = mTexts; stickers = mStickers; drawings = mDraws; frames = mFrames
                            persistHistory()
                        }
                    },
                    onUpdateLock = { id, isLocked ->
                        var changed = false
                        val mOverlays = overlays.map { if (it.id == id) { changed = true; it.copy(isLocked = isLocked) } else it }
                        val mTexts = texts.map { if (it.id == id) { changed = true; it.copy(isLocked = isLocked) } else it }
                        val mStickers = stickers.map { if (it.id == id) { changed = true; it.copy(isLocked = isLocked) } else it }
                        val mDraws = drawings.map { if (it.id == id) { changed = true; it.copy(isLocked = isLocked) } else it }
                        val mFrames = frames.map { if (it.id == id) { changed = true; it.copy(isLocked = isLocked) } else it }
                        if (changed) {
                            overlays = mOverlays; texts = mTexts; stickers = mStickers; drawings = mDraws; frames = mFrames
                            persistHistory()
                        }
                    },
                    onClose = { showLayerPanel = false }
                )
            }
        }
    }
        AnimatedVisibility(
            visible = showTransitionPickerForClipId != null,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
        ) {
            val clipId = showTransitionPickerForClipId
            val clipIndex = clips.indexOfFirst { it.id == clipId }
            val clip = clips.getOrNull(clipIndex)
            
            if (clip != null) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Transition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showTransitionPickerForClipId = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val transitionCategories = mapOf(
                                "Basic" to listOf(TransitionType.NONE, TransitionType.CROSSFADE, TransitionType.FADE_TO_BLACK, TransitionType.FADE_TO_WHITE),
                                "Slide" to listOf(TransitionType.SLIDE_LEFT, TransitionType.SLIDE_RIGHT, TransitionType.SLIDE_UP, TransitionType.SLIDE_DOWN),
                                "Push" to listOf(TransitionType.PUSH_LEFT, TransitionType.PUSH_RIGHT),
                                "Zoom" to listOf(TransitionType.ZOOM_IN, TransitionType.ZOOM_OUT),
                                "Wipe" to listOf(TransitionType.WIPE_LEFT, TransitionType.WIPE_RIGHT, TransitionType.CLOCK_WIPE),
                                "Rotate" to listOf(TransitionType.SPIN, TransitionType.FLIP)
                            )
                            
                            transitionCategories.forEach { (category, types) ->
                                item {
                                    Column {
                                        Text(category, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            types.forEach { tType ->
                                                val isSelected = clip.transitionNext.type == tType
                                                TransitionThumbnail(
                                                    type = tType,
                                                    selected = isSelected,
                                                    onClick = { 
                                                        val newClips = clips.toMutableList()
                                                        newClips[clipIndex] = clip.copy(transitionNext = clip.transitionNext.copy(type = tType))
                                                        saveState(newClips, canvasSettings, "Change transition")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.width(8.dp)) }
                            }
                        }
                        
                        if (clip.transitionNext.type != TransitionType.NONE) {
                            Divider()
                            Text("Duration", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("0.3s", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = clip.transitionNext.durationMs.toFloat() / 1000f,
                                    onValueChange = { newVal ->
                                        val newClips = clips.toMutableList()
                                        newClips[clipIndex] = clip.copy(transitionNext = clip.transitionNext.copy(durationMs = (newVal * 1000).toLong()))
                                        clips = newClips
                                    },
                                    onValueChangeFinished = {
                                        saveState(clips, canvasSettings, "Change transition duration")
                                    },
                                    valueRange = 0.3f..2.0f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                Text("2.0s", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${clip.transitionNext.durationMs} ms", modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

} // End of Scaffold content

    // Fullscreen Mode Overlay (now outside Scaffold, so it draws over TopBar/BottomBar)
    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showFullscreenControls = !showFullscreenControls }
        ) {
            val mediaPath = project?.sourceMediaPaths?.firstOrNull()
            if (mediaPath != null) {
                val currentClipForFullscreen = if (clips.isNotEmpty() && exoPlayer.currentWindowIndex in clips.indices) clips[exoPlayer.currentWindowIndex] else null
                val filterType = currentClipForFullscreen?.filterType ?: FilterType.NONE
                val filterIntensity = currentClipForFullscreen?.filterIntensity ?: 1f
                val filterColorMatrix = getColorMatrixForFilter(filterType, filterIntensity)
                var finalColorMatrix = filterColorMatrix
                val adjustments = currentClipForFullscreen?.adjustments ?: ColorAdjustments()
                val isComparing = isComparePressed
                if (!isComparing) {
                    val adjustMatrix = getColorMatrixForAdjustments(adjustments)
                    finalColorMatrix = androidx.compose.ui.graphics.ColorMatrix().apply {
                        timesAssign(filterColorMatrix)
                        timesAssign(adjustMatrix)
                    }
                }
                
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                        .colorFilterOverlay(finalColorMatrix)
                        .then(if (!isComparing) Modifier.vignetteAndGrain(adjustments.vignette, adjustments.grain) else Modifier)
                        .applyAllVisualEffects(if (!isComparing && !skipHeavyEffects && currentClipForFullscreen != null) currentClipForFullscreen.effects else emptyList(), currentPositionMs, currentClipForFullscreen?.durationMs ?: 0L)
                )
            }
            
            AnimatedVisibility(
                visible = showFullscreenControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    ) {
                        Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                    }
                    
                    Box(modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    seekToGlobal(0L, exoPlayer)
                                }
                                exoPlayer.play()
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                scaleIn(tween(200)) togetherWith scaleOut(tween(200))
                            },
                            label = "PlayPauseFullscreen"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }
            
            LaunchedEffect(showFullscreenControls) {
                if (showFullscreenControls) {
                    delay(3000)
                    showFullscreenControls = false
                }
            }
        }
    }

    if (showMediaPicker) {
        com.example.MediaPickerScreen(
            onClose = { showMediaPicker = false; isPickingOverlay = false },
            onGoToEditor = { showMediaPicker = false; isPickingOverlay = false },
            projectViewModel = projectViewModel,
            isSelectingForExisting = true,
            onMediaSelected = { paths ->
                showMediaPicker = false
                if (paths.isNotEmpty()) {
                    scope.launch {
                    if (isPickingOverlay) {
                        isPickingOverlay = false
                        val newOverlays = overlays.toMutableList()
                        for (path in paths) {
                            val metadata = MediaMetadataReader.read(context, android.net.Uri.parse(path)) ?: continue
                            val isPhoto = metadata.mimeType.startsWith("image/")
                            val durationMs = if (isPhoto) PHOTO_DEFAULT_DURATION_MS else metadata.durationMs
                            if (durationMs <= 0L) continue
                            newOverlays.add(OverlayClip(
                                sourceUri = path,
                                originalDurationMs = durationMs,
                                isPhoto = isPhoto,
                                isGif = false,
                                trimEndMs = durationMs,
                                startTimeOnTimelineMs = currentPositionMs
                            ))
                        }
                        if (newOverlays.size > overlays.size) {
                            saveOverlayState(newOverlays, "Add overlay")
                        }
                    } else {
                        val newClips = mutableListOf<MediaClip>()
                        for (path in paths) {
                            val metadata = MediaMetadataReader.read(context, android.net.Uri.parse(path)) ?: continue
                            val isPhoto = metadata.mimeType.startsWith("image/")
                            val durationMs = if (isPhoto) PHOTO_DEFAULT_DURATION_MS else metadata.durationMs
                            if (durationMs <= 0L) continue
                            newClips.add(MediaClip(
                                sourceUri = path,
                                originalDurationMs = durationMs,
                                trimEndMs = durationMs,
                                isPhoto = isPhoto
                            ))
                        }
                        if (newClips.isNotEmpty()) {
                            var accum = 0L
                            var insertIndex = clips.size
                            for (i in clips.indices) {
                                accum += clips[i].durationMs
                                if (currentPositionMs < accum) {
                                    insertIndex = i + 1
                                    break
                                }
                            }
                            val finalClips = clips.toMutableList()
                            finalClips.addAll(insertIndex, newClips)
                            saveState(finalClips, canvasSettings, "Add media")
                        }
                    }
                    }
                }
            }
        )
    }

} // End of outer Box
} // End of EditorScreen

@Composable
fun TransitionThumbnail(type: TransitionType, selected: Boolean, onClick: () -> Unit) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "infinite")
    val progress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "thumb_progress"
    )
    
    val p = progress
    val isOld = p < 0f
    val absP = Math.abs(p)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Render old and new as colored boxes
            val canvasColorOld = Color.Red.copy(alpha = 0.5f)
            val canvasColorNew = Color.Blue.copy(alpha = 0.5f)
            
            if (type == TransitionType.NONE) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(canvasColorOld))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(canvasColorNew))
                }
            } else {
                // We'll simulate the transition over a standard background
                Box(modifier = Modifier.fillMaxSize().background(if (isOld) canvasColorOld else canvasColorNew)) { }
                
                Box(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        when (type) {
                            TransitionType.CROSSFADE -> {
                                this.alpha = if (isOld) -p else p
                            }
                            TransitionType.SLIDE_LEFT, TransitionType.PUSH_LEFT -> {
                                this.translationX = if (isOld) (1f + p) * -this.size.width else (1f - p) * this.size.width
                            }
                            TransitionType.SLIDE_RIGHT, TransitionType.PUSH_RIGHT -> {
                                this.translationX = if (isOld) (1f + p) * this.size.width else (1f - p) * -this.size.width
                            }
                            TransitionType.SLIDE_UP -> {
                                this.translationY = if (isOld) (1f + p) * -this.size.height else (1f - p) * this.size.height
                            }
                            TransitionType.SLIDE_DOWN -> {
                                this.translationY = if (isOld) (1f + p) * this.size.height else (1f - p) * -this.size.height
                            }
                            TransitionType.ZOOM_IN -> {
                                val s = if (isOld) 1f + (1f + p) else p
                                this.scaleX = s; this.scaleY = s
                            }
                            TransitionType.ZOOM_OUT -> {
                                val s = if (isOld) -p else 2f - p
                                this.scaleX = s; this.scaleY = s
                            }
                            TransitionType.SPIN -> {
                                this.rotationZ = if (isOld) (1f + p) * 180f else (p - 1f) * 180f
                                val s = if (isOld) -p else p
                                this.scaleX = s; this.scaleY = s
                            }
                            TransitionType.FLIP -> {
                                this.rotationY = if (isOld) (1f + p) * 90f else (p - 1f) * -90f
                            }
                            else -> {}
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(if (isOld) canvasColorOld else canvasColorNew))
                }
                
                // Overlays
                when (type) {
                    TransitionType.FADE_TO_BLACK -> {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 1f - absP)))
                    }
                    TransitionType.FADE_TO_WHITE -> {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 1f - absP)))
                    }
                    TransitionType.WIPE_LEFT, TransitionType.WIPE_RIGHT, TransitionType.CLOCK_WIPE -> {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f - absP/2)))
                    }
                    else -> {}
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(type.label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
