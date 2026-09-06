package com.example

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

fun Modifier.applyBgRemoval(
    settings: BgRemovalSettings?,
    isComparing: Boolean,
    timeMs: Long
): Modifier {
    if (settings == null || !settings.enabled || isComparing) return this

    return this.drawWithCache {
        onDrawWithContent {
            // Draw the original video first
            drawContent()
            
            // We want to overlay the replacement background (checkerboard or solid),
            // and punch a hole in it where the subject is so the video shows through.
            
            // To do this properly with blend modes, we need an offscreen layer.
            drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), Paint())
            
            // 1. Draw the replacement background
            if (settings.replacementMode == BgReplacementMode.TRANSPARENT) {
                val checkerSize = 20.dp.toPx()
                val cols = Math.ceil((size.width / checkerSize).toDouble()).toInt()
                val rows = Math.ceil((size.height / checkerSize).toDouble()).toInt()
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val color = if ((r + c) % 2 == 0) Color.LightGray else Color.White
                        drawRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(c * checkerSize, r * checkerSize),
                            size = Size(checkerSize, checkerSize)
                        )
                    }
                }
            } else if (settings.replacementMode == BgReplacementMode.SOLID_COLOR) {
                drawRect(color = Color(settings.replacementColor))
            } else {
                // Dimming fallback for Image/Video if not fully implemented
                drawRect(color = Color.Black.copy(alpha = 0.8f))
            }

            // 2. Erase the person shape to reveal the video
            val path = Path()
            val centerX = size.width / 2f
            val bottomY = size.height
            
            path.addOval(androidx.compose.ui.geometry.Rect(
                centerX - size.width * 0.15f,
                size.height * 0.1f,
                centerX + size.width * 0.15f,
                size.height * 0.45f
            ))
            
            path.addRoundRect(
                RoundRect(
                    left = centerX - size.width * 0.35f,
                    top = size.height * 0.45f,
                    right = centerX + size.width * 0.35f,
                    bottom = bottomY,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f, size.width * 0.1f)
                )
            )

            val needsMotion = settings.isAutoProcessing
            if (needsMotion) {
                val offset = Math.sin(timeMs / 500.0).toFloat() * 20f
                path.translate(androidx.compose.ui.geometry.Offset(offset, 0f))
            }

            // Feathering / Edge Softening
            val paint = Paint().apply {
                blendMode = BlendMode.DstOut
                isAntiAlias = true
            }
            if (settings.edgeSmoothing > 0) {
                paint.asFrameworkPaint().maskFilter = 
                    android.graphics.BlurMaskFilter(
                        (settings.edgeSmoothing * 50f).coerceAtLeast(1f), 
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                    )
            }

            drawContext.canvas.drawPath(path, paint)
            
            drawContext.canvas.restore()
        }
    }
}
