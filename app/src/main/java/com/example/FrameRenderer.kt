package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun FrameRenderer(
    frameOverlay: FrameOverlay,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val typeId = frameOverlay.typeId
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            
            when {
                typeId.startsWith("clean") -> {
                    val thickPx = minDim * frameOverlay.thickness
                    val cornerPx = if (typeId == "clean_rounded") minDim * frameOverlay.cornerRadius else 0f
                    
                    drawRoundRect(
                        color = frameOverlay.color,
                        topLeft = Offset(thickPx / 2f, thickPx / 2f),
                        size = Size(w - thickPx, h - thickPx),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = thickPx)
                    )
                }
                typeId == "mockup_iphone" -> {
                    // Draw a mockup iPhone frame
                    val thickXPx = w * 0.05f
                    val thickYPx = h * 0.08f
                    val cornerPx = minDim * 0.15f
                    
                    // outer chassis
                    drawRoundRect(
                        color = frameOverlay.color,
                        topLeft = Offset(thickXPx / 2f, thickYPx / 2f),
                        size = Size(w - thickXPx, h - thickYPx),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = maxOf(thickXPx, thickYPx))
                    )
                    
                    // Notch
                    val notchWidth = w * 0.4f
                    val notchHeight = h * 0.04f
                    val notchPath = Path().apply {
                        moveTo((w - notchWidth) / 2f, thickYPx / 2f)
                        lineTo((w - notchWidth) / 2f, thickYPx / 2f + notchHeight)
                        lineTo((w + notchWidth) / 2f, thickYPx / 2f + notchHeight)
                        lineTo((w + notchWidth) / 2f, thickYPx / 2f)
                        close()
                    }
                    drawPath(notchPath, color = frameOverlay.color)
                }
                typeId == "mockup_android" -> {
                    val thickXPx = w * 0.04f
                    val thickYPx = h * 0.06f
                    val cornerPx = minDim * 0.08f
                    drawRoundRect(
                        color = frameOverlay.color,
                        topLeft = Offset(thickXPx / 2f, thickYPx / 2f),
                        size = Size(w - thickXPx, h - thickYPx),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = maxOf(thickXPx, thickYPx))
                    )
                    // camera punch hole
                    drawCircle(
                        color = frameOverlay.color,
                        radius = minDim * 0.02f,
                        center = Offset(w / 2f, thickYPx + minDim * 0.02f)
                    )
                }
                typeId == "mockup_laptop" -> {
                    val thickPx = minDim * 0.05f
                    val paddingBottom = h * 0.15f // Keyboard/base
                     drawRoundRect(
                        color = frameOverlay.color,
                        topLeft = Offset(thickPx / 2f, thickPx / 2f),
                        size = Size(w - thickPx, h - thickPx - paddingBottom),
                        cornerRadius = CornerRadius(thickPx, thickPx),
                        style = Stroke(width = thickPx)
                    )
                    // Base
                    drawRect(
                        color = frameOverlay.color.copy(alpha = 0.8f),
                        topLeft = Offset(0f, h - paddingBottom),
                        size = Size(w, paddingBottom)
                    )
                }
                typeId == "social_insta" -> {
                    // Instagram style border
                    drawRect(
                        color = frameOverlay.color,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h * 0.1f) // Top header
                    )
                    drawRect(
                        color = frameOverlay.color,
                        topLeft = Offset(0f, h * 0.9f),
                        size = Size(w, h * 0.1f) // Bottom footer
                    )
                }
                typeId == "social_imessage" -> {
                    // iMessage bubble style
                    val pad = minDim * 0.05f
                    drawRoundRect(
                        color = frameOverlay.color.copy(alpha=0.3f), // bg tint
                        topLeft = Offset(pad, pad),
                        size = Size(w - 2*pad, h - 2*pad),
                        cornerRadius = CornerRadius(pad*4, pad*4)
                    )
                    drawRoundRect(
                        color = frameOverlay.color,
                        topLeft = Offset(pad, pad),
                        size = Size(w - 2*pad, h - 2*pad),
                        cornerRadius = CornerRadius(pad*4, pad*4),
                        style = Stroke(width = pad)
                    )
                }
                typeId == "deco_film" -> {
                    val border = w * 0.1f
                    drawRect(color = frameOverlay.color, topLeft = Offset(0f, 0f), size = Size(border, h))
                    drawRect(color = frameOverlay.color, topLeft = Offset(w - border, 0f), size = Size(border, h))
                    
                    // Holes
                    val holeSize = border * 0.5f
                    var yOffset = holeSize
                    while (yOffset < h) {
                        drawRect(color = Color.Black, topLeft = Offset(border * 0.25f, yOffset), size = Size(holeSize, holeSize))
                        drawRect(color = Color.Black, topLeft = Offset(w - border * 0.75f, yOffset), size = Size(holeSize, holeSize))
                        yOffset += holeSize * 2.5f
                    }
                }
                typeId == "deco_polaroid" -> {
                    val padSides = w * 0.08f
                    val padTop = h * 0.08f
                    val padBottom = h * 0.25f
                    
                     drawRect(
                        color = frameOverlay.color,
                        topLeft = Offset(0f, 0f),
                        size = Size(padSides, h)
                    )
                    drawRect(
                        color = frameOverlay.color,
                        topLeft = Offset(w - padSides, 0f),
                        size = Size(padSides, h)
                    )
                    drawRect(
                        color = frameOverlay.color,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, padTop)
                    )
                    drawRect(
                        color = frameOverlay.color,
                        topLeft = Offset(0f, h - padBottom),
                        size = Size(w, padBottom)
                    )
                }
                typeId == "deco_torn" -> {
                    val border = minDim * 0.06f
                    drawRect(
                        color = frameOverlay.color,
                        topLeft = Offset(border / 2f, border / 2f),
                        size = Size(w - border, h - border),
                        style = Stroke(width = border)
                    )
                    // draw zigzags on edges
                    val zipPath = Path().apply {
                        var x = 0f
                        moveTo(0f, 0f)
                        while(x < w) {
                            lineTo(x + border, border)
                            lineTo(x + border*2, 0f)
                            x += border*2
                        }
                    }
                    drawPath(zipPath, color = frameOverlay.color)
                    val zipPathB = Path().apply {
                        var x = 0f
                        moveTo(0f, h)
                        while(x < w) {
                            lineTo(x + border, h - border)
                            lineTo(x + border*2, h)
                            x += border*2
                        }
                    }
                    drawPath(zipPathB, color = frameOverlay.color)
                }
                typeId == "festive_xmas" -> {
                    val thickPx = minDim * 0.05f
                    drawRect(
                        color = Color.Red.copy(alpha=0.8f),
                        topLeft = Offset(thickPx / 2f, thickPx / 2f),
                        size = Size(w - thickPx, h - thickPx),
                        style = Stroke(width = thickPx)
                    )
                    // Draw little green corner decorations
                    val decSize = thickPx * 3
                    drawRect(color = Color.Green, topLeft = Offset(0f, 0f), size = Size(decSize, decSize))
                    drawRect(color = Color.Green, topLeft = Offset(w - decSize, 0f), size = Size(decSize, decSize))
                    drawRect(color = Color.Green, topLeft = Offset(0f, h - decSize), size = Size(decSize, decSize))
                    drawRect(color = Color.Green, topLeft = Offset(w - decSize, h - decSize), size = Size(decSize, decSize))
                }
                typeId == "festive_party" -> {
                    // Confetti border
                    val thickPx = minDim * 0.08f
                     drawRect(
                        color = frameOverlay.color.copy(alpha=0.5f),
                        topLeft = Offset(thickPx / 2f, thickPx / 2f),
                        size = Size(w - thickPx, h - thickPx),
                        style = Stroke(width = thickPx)
                    )
                    val colors = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green, Color.Magenta)
                    var cx = 0f
                    while(cx < w) {
                        drawCircle(color = colors.random(), radius = thickPx*0.4f, center = Offset(cx, thickPx/2f))
                        drawCircle(color = colors.random(), radius = thickPx*0.4f, center = Offset(cx, h - thickPx/2f))
                        cx += thickPx * 1.5f
                    }
                    var cy = 0f
                    while(cy < h) {
                        drawCircle(color = colors.random(), radius = thickPx*0.4f, center = Offset(thickPx/2f, cy))
                        drawCircle(color = colors.random(), radius = thickPx*0.4f, center = Offset(w - thickPx/2f, cy))
                        cy += thickPx * 1.5f
                    }
                }
            }
        }
    }
}
