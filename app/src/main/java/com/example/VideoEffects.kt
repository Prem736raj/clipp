package com.example

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.geometry.*
import kotlin.math.*
import kotlin.random.Random
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

enum class EffectCategory(val title: String) {
    GLITCH("Glitch"),
    BLUR("Blur"),
    DISTORTION("Distortion"),
    LIGHT("Light"),
    STYLE("Style"),
    CINEMATIC("Cinematic")
}

enum class EffectType(val category: EffectCategory, val label: String) {
    DIGITAL_GLITCH(EffectCategory.GLITCH, "Digital Glitch"),
    RGB_SPLIT(EffectCategory.GLITCH, "RGB Split"),
    SIGNAL_ERROR(EffectCategory.GLITCH, "Signal Error"),
    DATAMOSH(EffectCategory.GLITCH, "Datamosh"),

    GAUSSIAN_BLUR(EffectCategory.BLUR, "Gaussian Blur"),
    MOTION_BLUR(EffectCategory.BLUR, "Motion Blur"),
    RADIAL_BLUR(EffectCategory.BLUR, "Radial Blur"),
    TILT_SHIFT(EffectCategory.BLUR, "Tilt Shift"),

    MIRROR(EffectCategory.DISTORTION, "Mirror"),
    KALEIDOSCOPE(EffectCategory.DISTORTION, "Kaleidoscope"),
    FISHEYE(EffectCategory.DISTORTION, "Fisheye"),
    WAVE(EffectCategory.DISTORTION, "Wave"),
    PIXELATE(EffectCategory.DISTORTION, "Pixelate"),

    LIGHT_LEAK(EffectCategory.LIGHT, "Light Leak"),
    LENS_FLARE(EffectCategory.LIGHT, "Lens Flare"),
    BOKEH(EffectCategory.LIGHT, "Bokeh"),
    PRISM(EffectCategory.LIGHT, "Prism/Rainbow"),
    SPARKLE(EffectCategory.LIGHT, "Sparkle"),

    COMIC_BOOK(EffectCategory.STYLE, "Comic Book"),
    PENCIL_SKETCH(EffectCategory.STYLE, "Pencil Sketch"),
    OIL_PAINTING(EffectCategory.STYLE, "Oil Painting"),
    NEON_GLOW(EffectCategory.STYLE, "Neon Glow"),
    POP_ART(EffectCategory.STYLE, "Pop Art"),

    LETTERBOX(EffectCategory.CINEMATIC, "Letterbox Bars"),
    FILM_GRAIN(EffectCategory.CINEMATIC, "Film Grain"),
    ANAMORPHIC_FLARE(EffectCategory.CINEMATIC, "Anamorphic Flare"),
    SHAKE(EffectCategory.CINEMATIC, "Shake/Handheld")
}

data class AppliedEffect(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: EffectType,
    val intensity: Float = 0.5f,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = -1L
)

fun Modifier.applyAllVisualEffects(effects: List<AppliedEffect>, timeMs: Long, clipDurationMs: Long): Modifier {
    var mod = this
    for (eff in effects) {
        val start = eff.startTimeMs
        val end = if (eff.endTimeMs == -1L) clipDurationMs else eff.endTimeMs
        if (timeMs in start..end) {
            mod = mod.applyVisualEffectModifier(eff, timeMs)
        }
    }
    return mod
}

private fun Modifier.applyVisualEffectModifier(eff: AppliedEffect, timeMs: Long): Modifier {
    val t = timeMs / 1000f
    val intensity = eff.intensity.coerceIn(0f, 1f)
    
    var mod = this
    if (eff.type == EffectType.GAUSSIAN_BLUR) {
        mod = mod.blur(radius = (24 * intensity).dp)
    }

    return mod.drawWithContent {
        val drawSelf = { this@drawWithContent.drawContent() }
        when (eff.type) {
            EffectType.RGB_SPLIT -> {
                val offset = size.width * 0.05f * intensity
                val shiftR = offset * sin(t * 15f)
                val shiftB = -offset * sin(t * 12f)
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { values[0] = 1f; values[6] = 0f; values[12] = 0f })
                    blendMode = BlendMode.Screen
                })
                translate(left = shiftR, top = 0f) { drawSelf() }
                drawContext.canvas.restore()
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { values[0] = 0f; values[6] = 1f; values[12] = 0f })
                    blendMode = BlendMode.Screen
                })
                drawSelf()
                drawContext.canvas.restore()
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { values[0] = 0f; values[6] = 0f; values[12] = 1f })
                    blendMode = BlendMode.Screen
                })
                translate(left = shiftB, top = 0f) { drawSelf() }
                drawContext.canvas.restore()
            }
            EffectType.DIGITAL_GLITCH -> {
                drawSelf()
                if (Random(timeMs).nextFloat() < intensity) {
                    val numSlices = (10 * intensity).toInt()
                    for (i in 0..numSlices) {
                        val y = Random(timeMs + i).nextFloat() * size.height
                        val h = Random(timeMs + i + 1).nextFloat() * 20f + 5f
                        val shift = (Random(timeMs + i + 2).nextFloat() - 0.5f) * size.width * 0.2f * intensity
                        clipRect(left = 0f, top = y, right = size.width, bottom = y + h) {
                            translate(left = shift, top = 0f) { drawSelf() }
                        }
                    }
                }
            }
            EffectType.SIGNAL_ERROR -> {
                translate(left = (Random(timeMs).nextFloat() - 0.5f) * 10f * intensity) {
                    drawSelf()
                }
                val numLines = (20 * intensity).toInt()
                for (i in 0..numLines) {
                    val y = (t * 50f + i * 20f) % size.height
                    drawRect(color = Color.White.copy(alpha = 0.3f), topLeft = Offset(0f, y), size = androidx.compose.ui.geometry.Size(size.width, 2f))
                }
            }
            EffectType.DATAMOSH -> {
                for(i in 0..5) {
                    val scale = 1f + i * 0.05f * intensity
                    val alpha = 0.2f
                    scale(scaleX = scale, scaleY = scale, pivot = Offset(size.width/2, size.height/2)) {
                        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = alpha })
                        drawSelf()
                        drawContext.canvas.restore()
                    }
                }
            }
            EffectType.GAUSSIAN_BLUR -> {
                drawSelf()
            }
            EffectType.MOTION_BLUR -> {
                val passes = 5
                val offset = size.width * 0.1f * intensity
                for (i in 0..passes) {
                    val alpha = 1f / (i + 1)
                    val shift = offset * (i / passes.toFloat())
                    drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = alpha })
                    translate(left = shift, top = 0f) { drawSelf() }
                    drawContext.canvas.restore()
                }
            }
            EffectType.RADIAL_BLUR -> {
                val passes = 5
                for (i in 0..passes) {
                    val alpha = 1f / (i + 1)
                    val scale = 1f + (i * 0.02f * intensity)
                    drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = alpha })
                    scale(scaleX = scale, scaleY = scale, pivot = Offset(size.width/2, size.height/2)) { drawSelf() }
                    drawContext.canvas.restore()
                }
            }
            EffectType.TILT_SHIFT -> {
                drawSelf()
                val blurPasses = (5 * intensity).toInt() + 1
                clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height * 0.3f) {
                    for(i in 1..blurPasses) {
                        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = 0.5f })
                        translate(left = i*2f, top=0f) { drawSelf() }
                        drawContext.canvas.restore()
                    }
                }
                clipRect(left = 0f, top = size.height * 0.7f, right = size.width, bottom = size.height) {
                    for(i in 1..blurPasses) {
                        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = 0.5f })
                        translate(left = i*2f, top=0f) { drawSelf() }
                        drawContext.canvas.restore()
                    }
                }
            }
            EffectType.MIRROR -> {
                clipRect(left = 0f, top = 0f, right = size.width / 2, bottom = size.height) { drawSelf() }
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset(size.width / 2, 0f)) {
                    clipRect(left = 0f, top = 0f, right = size.width / 2, bottom = size.height) { drawSelf() }
                }
            }
            EffectType.KALEIDOSCOPE -> {
                clipRect(left = 0f, top = 0f, right = size.width / 2, bottom = size.height / 2) { drawSelf() }
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset(size.width / 2, 0f)) {
                    clipRect(left = 0f, top = 0f, right = size.width / 2, bottom = size.height / 2) { drawSelf() }
                }
                scale(scaleX = 1f, scaleY = -1f, pivot = Offset(0f, size.height / 2)) {
                    clipRect(left = 0f, top = 0f, right = size.width / 2, bottom = size.height / 2) { drawSelf() }
                    scale(scaleX = -1f, scaleY = 1f, pivot = Offset(size.width / 2, 0f)) {
                        clipRect(left = 0f, top = 0f, right = size.width / 2, bottom = size.height / 2) { drawSelf() }
                    }
                }
            }
            EffectType.FISHEYE -> {
                drawSelf()
            }
            EffectType.WAVE -> {
                val numStrips = 20
                val stripHeight = size.height / numStrips
                for(i in 0 until numStrips) {
                    val y = i * stripHeight
                    val shift = sin(t * 5f + i * 0.5f) * size.width * 0.05f * intensity
                    clipRect(left = 0f, top = y, right = size.width, bottom = y + stripHeight) {
                        translate(left = shift, top = 0f) { drawSelf() }
                    }
                }
            }
            EffectType.PIXELATE -> {
                drawSelf()
            }
            EffectType.LIGHT_LEAK -> {
                drawSelf()
                val moveX = sin(t) * size.width * 0.5f
                val brush = Brush.radialGradient(
                    colors = listOf(Color(255, 100, 50, (150 * intensity).toInt()), Color.Transparent),
                    center = Offset(size.width / 2f + moveX, size.height * 0.2f),
                    radius = size.width * 0.8f
                )
                drawRect(brush = brush, blendMode = BlendMode.Screen)
            }
            EffectType.LENS_FLARE -> {
                drawSelf()
                val pos = Offset(size.width * (0.5f + 0.3f * sin(t)), size.height * (0.5f + 0.3f * cos(t)))
                drawCircle(color = Color.White.copy(alpha = 0.5f * intensity), radius = 50f, center = pos, blendMode = BlendMode.Screen)
                drawCircle(color = Color(100, 200, 255, (100 * intensity).toInt()), radius = 150f, center = Offset(size.width - pos.x, size.height - pos.y), blendMode = BlendMode.Screen)
            }
            EffectType.BOKEH -> {
                drawSelf()
                for(i in 0..5) {
                    val px = (sin(t * 0.5f + i) + 1f) / 2f * size.width
                    val py = (cos(t * 0.4f + i * 2) + 1f) / 2f * size.height
                    drawCircle(color = Color(255, 200, 100, (50 * intensity).toInt()), radius = 60f + i * 10f, center = Offset(px, py), blendMode = BlendMode.Screen)
                }
            }
            EffectType.PRISM -> {
                val shift = size.width * 0.02f * intensity
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { values[0] = 1f; values[6] = 0f; values[12] = 0f })
                    blendMode = BlendMode.Screen
                })
                translate(left = shift, top = shift) { drawSelf() }
                drawContext.canvas.restore()
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { values[0] = 0f; values[6] = 1f; values[12] = 0f })
                    blendMode = BlendMode.Screen
                })
                drawSelf()
                drawContext.canvas.restore()
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { values[0] = 0f; values[6] = 0f; values[12] = 1f })
                    blendMode = BlendMode.Screen
                })
                translate(left = -shift, top = -shift) { drawSelf() }
                drawContext.canvas.restore()
            }
            EffectType.SPARKLE -> {
                drawSelf()
                val random = Random((timeMs / 100).toLong())
                for(i in 0..(10 * intensity).toInt()) {
                    val px = random.nextFloat() * size.width
                    val py = random.nextFloat() * size.height
                    drawCircle(color = Color.White, radius = random.nextFloat() * 5f, center = Offset(px, py), blendMode = BlendMode.Screen)
                }
            }
            EffectType.COMIC_BOOK -> {
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(2f * intensity) })
                })
                drawSelf()
                drawContext.canvas.restore()
            }
            EffectType.PENCIL_SKETCH -> {
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                })
                drawSelf()
                drawContext.canvas.restore()
            }
            EffectType.OIL_PAINTING -> {
                drawSelf()
            }
            EffectType.NEON_GLOW -> {
                val shift = 5f * intensity
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { values[0] = 0f; values[6] = 1f; values[12] = 1f })
                    blendMode = BlendMode.Screen
                })
                translate(left = shift, top = shift) { drawSelf() }
                drawContext.canvas.restore()
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { values[0] = 1f; values[6] = 0f; values[12] = 1f })
                    blendMode = BlendMode.Screen
                })
                translate(left = -shift, top = -shift) { drawSelf() }
                drawContext.canvas.restore()
            }
            EffectType.POP_ART -> {
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(3f * intensity) })
                })
                drawSelf()
                drawContext.canvas.restore()
            }
            EffectType.LETTERBOX -> {
                drawSelf()
                val barHeight = size.height * 0.15f * intensity
                drawRect(color = Color.Black, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, barHeight))
                drawRect(color = Color.Black, topLeft = Offset(0f, size.height - barHeight), size = androidx.compose.ui.geometry.Size(size.width, barHeight))
            }
            EffectType.FILM_GRAIN -> {
                drawSelf()
                val random = Random(timeMs)
                for(i in 0..(1000 * intensity).toInt()) {
                    val px = random.nextFloat() * size.width
                    val py = random.nextFloat() * size.height
                    drawRect(color = Color.Black.copy(alpha = 0.2f), topLeft = Offset(px, py), size = androidx.compose.ui.geometry.Size(2f, 2f))
                }
            }
            EffectType.ANAMORPHIC_FLARE -> {
                drawSelf()
                val moveY = size.height * 0.4f + sin(t) * size.height * 0.1f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(50, 150, 255, (200 * intensity).toInt()), Color.Transparent),
                        startY = moveY - 10f,
                        endY = moveY + 10f
                    ),
                    blendMode = BlendMode.Screen,
                    topLeft = Offset(0f, moveY - 10f),
                    size = androidx.compose.ui.geometry.Size(size.width, 20f)
                )
            }
            EffectType.SHAKE -> {
                val shiftX = (Random(timeMs).nextFloat() - 0.5f) * 20f * intensity
                val shiftY = (Random(timeMs + 1).nextFloat() - 0.5f) * 20f * intensity
                translate(left = shiftX, top = shiftY) { drawSelf() }
            }
        }
    }
}
