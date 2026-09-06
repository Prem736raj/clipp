package com.example

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke

data class NormalizedOffset(val x: Float, val y: Float)

data class PaintedStroke(
    val points: List<NormalizedOffset>,
    val isErase: Boolean = false,
    val strokeWidth: Float = 0.05f
)

enum class BlurStyle(val label: String) {
    GAUSSIAN("Gaussian"), 
    BOKEH("Bokeh"), 
    MOTION("Motion")
}

data class BgBlurSettings(
    val enabled: Boolean = false,
    val intensity: Float = 0.5f,
    val style: BlurStyle = BlurStyle.GAUSSIAN,
    val showMask: Boolean = false,
    val aiTracking: Boolean = true,
    val manualStrokes: List<PaintedStroke> = emptyList()
)

fun Modifier.applyBgBlurSettings(settings: BgBlurSettings, timeMs: Long, clipDurationMs: Long): Modifier {
    if (!settings.enabled) return this
    return this.drawWithContent {
        val t = timeMs / 1000f
        val intensityIndex = settings.intensity
        val passes = (10 * intensityIndex).toInt() + 1
        
        // 1. Draw blurred background
        if (settings.style == BlurStyle.MOTION) {
            drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
            for (i in 0..passes) {
                val alpha = 1f / (i + 1)
                val shift = size.width * 0.1f * intensityIndex * (i / passes.toFloat())
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = alpha })
                translate(left = shift, top = 0f) { this@drawWithContent.drawContent() }
                drawContext.canvas.restore()
            }
            drawContext.canvas.restore()
        } else if (settings.style == BlurStyle.BOKEH) {
            drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
            for (i in 0..passes) {
                val alpha = 1f / (i + 1)
                val px = kotlin.math.sin(i.toFloat()) * 20f * intensityIndex
                val py = kotlin.math.cos(i.toFloat()) * 20f * intensityIndex
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = alpha })
                translate(left = px, top = py) { this@drawWithContent.drawContent() }
                drawContext.canvas.restore()
            }
            drawContext.canvas.restore()
        } else {
            drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
            for (i in 0..passes) {
                val alpha = 1f / (i + 1)
                drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { this.alpha = alpha })
                translate(left = i * 2f * intensityIndex, top = i * 2f * intensityIndex) { this@drawWithContent.drawContent() }
                translate(left = -i * 2f * intensityIndex, top = -i * 2f * intensityIndex) { this@drawWithContent.drawContent() }
                drawContext.canvas.restore()
            }
            drawContext.canvas.restore()
        }

        // 2. Wrap original sharp content and clip to subject mask
        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
        
        drawContent() // Sharp content
        
        // Mask it using DstIn
        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { blendMode = BlendMode.DstIn })
        
        // Fill clear first
        drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
        
        if (settings.aiTracking) {
            val cx = size.width * 0.5f + kotlin.math.sin(t * 2f) * size.width * 0.05f
            val cy = size.height * 0.6f + kotlin.math.cos(t * 1.5f) * size.height * 0.05f
            val rx = size.width * 0.35f
            val ry = size.height * 0.45f
            val brush = Brush.radialGradient(
                *arrayOf(0f to Color.Black, 0.7f to Color.Black, 1f to Color.Transparent),
                center = Offset(cx, cy),
                radius = rx.coerceAtLeast(1f)
            )
            drawOval(brush = brush, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))
        }
        
        settings.manualStrokes.forEach { stroke ->
            val strokePath = Path()
            if (stroke.points.isNotEmpty()) {
                strokePath.moveTo(stroke.points[0].x * size.width, stroke.points[0].y * size.height)
                for (i in 1 until stroke.points.size) {
                    strokePath.lineTo(stroke.points[i].x * size.width, stroke.points[i].y * size.height)
                }
            }
            drawPath(
                path = strokePath,
                color = if (stroke.isErase) Color.Transparent else Color.Black,
                style = Stroke(
                    width = stroke.strokeWidth * size.width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = if (stroke.isErase) BlendMode.Clear else BlendMode.SrcOver
            )
        }
        
        drawContext.canvas.restore() // End mask DstIn layer
        drawContext.canvas.restore() // End sharp content layer

        // 3. Show Mask overlay if required
        if (settings.showMask) {
            drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { alpha = 0.5f })
            drawRect(Color.Red)
            drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint().apply { blendMode = BlendMode.DstIn })
            
            if (settings.aiTracking) {
                val cx = size.width * 0.5f + kotlin.math.sin(t * 2f) * size.width * 0.05f
                val cy = size.height * 0.6f + kotlin.math.cos(t * 1.5f) * size.height * 0.05f
                val rx = size.width * 0.35f
                val ry = size.height * 0.45f
                val brush = Brush.radialGradient(
                    *arrayOf(0f to Color.Black, 0.7f to Color.Black, 1f to Color.Transparent),
                    center = Offset(cx, cy),
                    radius = rx.coerceAtLeast(1f)
                )
                drawOval(brush = brush, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))
            }
            
            settings.manualStrokes.forEach { stroke ->
                val strokePath = Path()
                if (stroke.points.isNotEmpty()) {
                    strokePath.moveTo(stroke.points[0].x * size.width, stroke.points[0].y * size.height)
                    for (i in 1 until stroke.points.size) {
                        strokePath.lineTo(stroke.points[i].x * size.width, stroke.points[i].y * size.height)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = if (stroke.isErase) Color.Transparent else Color.Black,
                    style = Stroke(
                        width = stroke.strokeWidth * size.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    blendMode = if (stroke.isErase) BlendMode.Clear else BlendMode.SrcOver
                )
            }
            
            drawContext.canvas.restore()
            drawContext.canvas.restore()
        }
    }
}
