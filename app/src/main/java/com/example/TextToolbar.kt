package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

data class TextStylePreset(
    val id: String,
    val category: String,
    val fontName: String,
    val textColor: Color,
    val backgroundColor: Color = Color.Transparent,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val strokeColor: Color = Color.Transparent,
    val strokeWidth: Float = 0f,
    val shadowColor: Color = Color.Transparent,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 0f,
    val shadowBlur: Float = 0f,
    val letterSpacing: Float = 0f,
    val is3D: Boolean = false
)

val textStylePresets = listOf(
    // Category: Social (Bold, Trending, High contrast)
    TextStylePreset("TikTok", "Social", "Ubuntu", Color.White, strokeColor = Color.Black, strokeWidth = 3f, shadowColor = Color.Black.copy(alpha=0.5f), shadowOffsetX = 2f, shadowOffsetY = 2f, shadowBlur = 4f, isBold = true),
    TextStylePreset("Instagram", "Social", "Roboto", Color.White, backgroundColor = Color.Black.copy(alpha=0.6f), isBold = true),
    TextStylePreset("Pop", "Social", "Bangers", Color(0xFFFFEB3B), strokeColor = Color.Black, strokeWidth = 8f),
    TextStylePreset("Vlog", "Social", "Montserrat", Color.White, shadowColor = Color.Black, shadowOffsetX = 3f, shadowOffsetY = 3f, shadowBlur = 0f, isBold = true),
    
    // Category: Cinematic (Movie titles, elegant, dramatic)
    TextStylePreset("Blockbuster", "Cinematic", "Cinzel", Color(0xFFE5E5E5), shadowColor = Color.Black, shadowBlur = 8f, letterSpacing = 2f),
    TextStylePreset("Credits", "Cinematic", "Roboto", Color.White, letterSpacing = 1f),
    TextStylePreset("Epic", "Cinematic", "Oswald", Color.White, strokeColor = Color.Red, strokeWidth = 2f, isBold = true, letterSpacing = 1.5f),
    TextStylePreset("Noir", "Cinematic", "Playfair Display", Color.Black, backgroundColor = Color.White, isBold = true),
    TextStylePreset("Sci-Fi", "Cinematic", "Space Mono", Color(0xFF00FFCC), shadowColor = Color(0xFF00FFCC), shadowBlur = 10f),
    
    // Category: Minimal (Clean, modern, subtle)
    TextStylePreset("Clean", "Minimal", "Lato", Color.White),
    TextStylePreset("Thin", "Minimal", "Raleway", Color.White, letterSpacing = 3f),
    TextStylePreset("Modern", "Minimal", "Poppins", Color.DarkGray, isBold = true),
    TextStylePreset("Subtle", "Minimal", "Nunito", Color.LightGray, shadowColor = Color.Black.copy(alpha=0.3f), shadowBlur = 2f),
    
    // Category: Fun (Colorful, bouncy, handwriting)
    TextStylePreset("Comic", "Fun", "Caveat", Color(0xFFFF5722), isBold = true, shadowColor = Color.Black, shadowOffsetX = 4f, shadowOffsetY = 4f),
    TextStylePreset("Bubblegum", "Fun", "Fredoka One", Color(0xFFFF4081), strokeColor = Color.White, strokeWidth = 4f),
    TextStylePreset("Retro", "Fun", "Righteous", Color(0xFFFFC107), shadowColor = Color(0xFF3F51B5), shadowOffsetX = 4f, shadowOffsetY = 4f),
    TextStylePreset("Handwritten", "Fun", "Dancing Script", Color.White, isBold = true, shadowColor = Color.Black, shadowBlur = 4f),
    
    // Category: Glowing (Neon, cyber, radiant)
    TextStylePreset("Neon Green", "Glowing", "Roboto", Color(0xFF69F0AE), shadowColor = Color(0xFF00E676), shadowBlur = 16f, isBold = true),
    TextStylePreset("Synthwave", "Glowing", "Lobster", Color(0xFFFF4081), shadowColor = Color(0xFFE040FB), shadowBlur = 12f),
    TextStylePreset("Cyberpunk", "Glowing", "Russo One", Color(0xFFFFFF00), shadowColor = Color(0xFFFF1744), shadowOffsetX = 2f, shadowOffsetY = 2f, shadowBlur = 8f),
    TextStylePreset("Hologram", "Glowing", "Fira Code", Color(0xFF00E5FF), shadowColor = Color(0xFF00B0FF), shadowBlur = 10f, letterSpacing = 1f),
    
    // Feature: 3D
    TextStylePreset("3D Bold", "3D", "Anton", Color.White, is3D = true, isBold = true)
)

@Composable
fun ColorWheelPicker(
    color: Color,
    onColorChanged: (Color) -> Unit,
    onEyedropperClick: () -> Unit
) {
    var hsv by remember { mutableStateOf(floatArrayOf(0f, 1f, 1f)) }
    
    // Convert current color to HSV on init or when color strongly changes
    LaunchedEffect(color) {
        val hsvTemp = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsvTemp)
        if (hsvTemp[1] > 0.01f || hsvTemp[2] > 0.01f) {
            hsv = hsvTemp
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Hue bar
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red,
                                Color.Yellow,
                                Color.Green,
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta,
                                Color.Red
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val width = size.width.toFloat()
                            val x = change.position.x.coerceIn(0f, width)
                            val hue = (x / width) * 360f
                            hsv = floatArrayOf(hue, hsv[1], hsv[2])
                            onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat()
                            val hue = (offset.x / width) * 360f
                            hsv = floatArrayOf(hue, hsv[1], hsv[2])
                            onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                        }
                    }
            ) {
                // Hue thumb
                val widthPx = constraints.maxWidth.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .offset { androidx.compose.ui.unit.IntOffset((hsv[0] / 360f * (widthPx - 4.dp.toPx())).toInt(), 0) }
                        .background(Color.White)
                )
            }
            
            IconButton(onClick = onEyedropperClick) {
                Icon(Icons.Filled.Colorize, contentDescription = "Eyedropper")
            }
        }
        Spacer(Modifier.height(8.dp))
        // Saturation/Value block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.White, Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))))
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val sat = x / size.width
                        hsv = floatArrayOf(hsv[0], sat, hsv[2])
                        onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val sat = offset.x / size.width
                        hsv = floatArrayOf(hsv[0], sat, hsv[2])
                        onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
                // Thumb
                val x = hsv[1] * size.width
                drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(x, size.height / 2f), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun AnimatedTextPreview(
    animIn: TextAnimIn = TextAnimIn.NONE,
    animLoop: TextAnimLoop = TextAnimLoop.NONE,
    animOut: TextAnimOut = TextAnimOut.NONE,
    text: String
) {
    val durationMs = 1500L
    var currentTimeMs by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(animIn, animLoop, animOut) {
        val startTime = System.currentTimeMillis()
        while (true) {
            withFrameNanos {
                currentTimeMs = (System.currentTimeMillis() - startTime) % durationMs
            }
        }
    }
    
    var renderAlpha = 1f
    var renderScale = 1f
    var renderPosX = 0f
    var renderPosY = 0f
    var renderRot = 0f
    var renderBlur = 0f
    var displayText = text
    
    if (animIn != TextAnimIn.NONE) {
        val progress = currentTimeMs.toFloat() / durationMs.toFloat()
        val ease = kotlin.math.sin(progress * Math.PI / 2).toFloat()
        when (animIn) {
            TextAnimIn.FADE_IN -> renderAlpha = ease
            TextAnimIn.SLIDE_IN_UP -> { renderAlpha = ease; renderPosY += (1f - ease) * 40f }
            TextAnimIn.SLIDE_IN_DOWN -> { renderAlpha = ease; renderPosY -= (1f - ease) * 40f }
            TextAnimIn.SLIDE_IN_LEFT -> { renderAlpha = ease; renderPosX += (1f - ease) * 40f }
            TextAnimIn.SLIDE_IN_RIGHT -> { renderAlpha = ease; renderPosX -= (1f - ease) * 40f }
            TextAnimIn.SCALE_IN -> { renderAlpha = ease; renderScale *= ease }
            TextAnimIn.BOUNCE_IN -> {
                val b = if (progress < 0.5f) { 4f * progress * progress * progress } else { 1f - Math.pow((-2f * progress + 2f).toDouble(), 3.0).toFloat() / 2f }
                renderScale *= b.coerceAtLeast(0.01f)
            }
            TextAnimIn.BLUR_IN -> { renderAlpha = ease; renderBlur = (1f - ease) * 10f }
            TextAnimIn.ROTATE_IN -> { renderAlpha = ease; renderScale *= ease; renderRot -= (1f - ease) * 180f }
            TextAnimIn.TYPEWRITER -> {
                val charsToShow = (progress * text.length).toInt()
                displayText = text.take(charsToShow) + (if (progress < 0.9f && (currentTimeMs / 200) % 2 == 0L) "|" else "")
            }
            TextAnimIn.GLITCH_IN -> {
                if (progress < 0.8f && Math.random() < 0.3) {
                    renderPosX += (Math.random() - 0.5f).toFloat() * 10f
                    renderAlpha = Math.random().toFloat()
                }
            }
            else -> {}
        }
    }
    
    if (animLoop != TextAnimLoop.NONE) {
        val loopProgress = currentTimeMs.toFloat() / durationMs.toFloat()
        when (animLoop) {
            TextAnimLoop.PULSE -> renderScale *= (1f + 0.2f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat())
            TextAnimLoop.BOUNCE -> renderPosY -= 5f * kotlin.math.sin(loopProgress * Math.PI).toFloat()
            TextAnimLoop.SHAKE -> if (loopProgress < 0.2f) renderPosX += 5f * kotlin.math.sin(loopProgress * Math.PI * 10).toFloat()
            TextAnimLoop.GLOW -> renderBlur = 2f + 5f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
            TextAnimLoop.WAVE -> renderRot += 15f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
            TextAnimLoop.FLICKER -> if (Math.random() < 0.1) renderAlpha = 0.5f
            TextAnimLoop.SWING -> renderRot += 15f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
            TextAnimLoop.FLOAT -> {
                renderPosY += 2f * kotlin.math.sin(loopProgress * Math.PI * 2).toFloat()
                renderPosX += 2f * kotlin.math.cos(loopProgress * Math.PI * 2).toFloat()
            }
            else -> {}
        }
    }
    
    if (animOut != TextAnimOut.NONE) {
        val progress = currentTimeMs.toFloat() / durationMs.toFloat()
        val ease = kotlin.math.sin(progress * Math.PI / 2).toFloat()
        val invEase = 1f - ease
        when (animOut) {
            TextAnimOut.FADE_OUT -> renderAlpha = invEase
            TextAnimOut.SLIDE_OUT_UP -> { renderAlpha = invEase; renderPosY -= ease * 40f }
            TextAnimOut.SLIDE_OUT_DOWN -> { renderAlpha = invEase; renderPosY += ease * 40f }
            TextAnimOut.SLIDE_OUT_LEFT -> { renderAlpha = invEase; renderPosX -= ease * 40f }
            TextAnimOut.SLIDE_OUT_RIGHT -> { renderAlpha = invEase; renderPosX += ease * 40f }
            TextAnimOut.SCALE_OUT -> { renderAlpha = invEase; renderScale *= invEase.coerceAtLeast(0.01f) }
            TextAnimOut.DISSOLVE -> { renderAlpha = invEase; renderBlur = ease * 10f }
            TextAnimOut.BOUNCE_OUT -> { renderScale *= invEase.coerceAtLeast(0.01f); renderPosY += ease * 20f }
            TextAnimOut.BLUR_OUT -> { renderAlpha = invEase; renderBlur = ease * 20f }
            else -> {}
        }
    }
    
    Text(
        text = displayText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .graphicsLayer {
                translationX = renderPosX
                translationY = renderPosY
                scaleX = renderScale
                scaleY = renderScale
                rotationZ = renderRot
                alpha = renderAlpha
            }
            .then(if (renderBlur > 0f) Modifier.blur(renderBlur.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier)
    )
}

@Composable
fun TextToolbar(
    textOverlay: TextOverlay,
    isEditing: Boolean,
    onCloseEditing: () -> Unit,
    onCloseToolbar: () -> Unit,
    onEyedropperSelect: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onPasteStyle: () -> Unit,
    onUpdate: (TextOverlay) -> Unit
) {
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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Text Attributes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (globalClipboardItem is TextOverlay) {
                        IconButton(onClick = onPasteStyle) {
                            Icon(Icons.Filled.Style, contentDescription = "Paste Style")
                        }
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Filled.FileCopy, contentDescription = "Duplicate")
                    }
                    IconButton(onClick = onCloseToolbar) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }
            
            if (isEditing) {
                val focusRequester = remember { FocusRequester() }
                OutlinedTextField(
                    value = textOverlay.text,
                    onValueChange = { onUpdate(textOverlay.copy(text = it)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("Enter text...") }
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                Button(onClick = onCloseEditing, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit Text Content")
                }
            }
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item {
                    IconToggleButton(checked = textOverlay.isBold, onCheckedChange = { onUpdate(textOverlay.copy(isBold = it)) }) {
                        Text("B", fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    IconToggleButton(checked = textOverlay.isItalic, onCheckedChange = { onUpdate(textOverlay.copy(isItalic = it)) }) {
                        Text("I", fontStyle = FontStyle.Italic)
                    }
                }
                item {
                    IconToggleButton(checked = textOverlay.isUnderline, onCheckedChange = { onUpdate(textOverlay.copy(isUnderline = it)) }) {
                        Text("U", textDecoration = TextDecoration.Underline)
                    }
                }
                item { Spacer(Modifier.width(8.dp)) }
                item {
                    IconToggleButton(checked = textOverlay.alignment == TextAlignmentType.Left, onCheckedChange = { onUpdate(textOverlay.copy(alignment = TextAlignmentType.Left)) }) {
                        Icon(Icons.Filled.FormatAlignLeft, "Left")
                    }
                }
                item {
                    IconToggleButton(checked = textOverlay.alignment == TextAlignmentType.Center, onCheckedChange = { onUpdate(textOverlay.copy(alignment = TextAlignmentType.Center)) }) {
                        Icon(Icons.Filled.FormatAlignCenter, "Center")
                    }
                }
                item {
                    IconToggleButton(checked = textOverlay.alignment == TextAlignmentType.Right, onCheckedChange = { onUpdate(textOverlay.copy(alignment = TextAlignmentType.Right)) }) {
                        Icon(Icons.Filled.FormatAlignRight, "Right")
                    }
                }
            }
            
            Text("Presets", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val groupByCategory = textStylePresets.groupBy { it.category }
                for ((category, presets) in groupByCategory) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                presets.forEach { preset ->
                                    val presetTextStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = getFontFamily(preset.fontName),
                                        fontSize = 24.sp,
                                        fontWeight = if (preset.isBold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (preset.isItalic) FontStyle.Italic else FontStyle.Normal,
                                        letterSpacing = preset.letterSpacing.sp,
                                        shadow = if (preset.shadowColor != Color.Transparent) {
                                            androidx.compose.ui.graphics.Shadow(
                                                color = preset.shadowColor,
                                                offset = Offset(preset.shadowOffsetX, preset.shadowOffsetY),
                                                blurRadius = preset.shadowBlur
                                            )
                                        } else null,
                                        drawStyle = if (preset.strokeColor != Color.Transparent && preset.strokeWidth > 0f) {
                                            Stroke(width = preset.strokeWidth)
                                        } else null
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray)
                                            .clickable {
                                                onUpdate(textOverlay.copy(
                                                    fontName = preset.fontName,
                                                    textColor = preset.textColor,
                                                    backgroundColor = preset.backgroundColor,
                                                    isBold = preset.isBold,
                                                    isItalic = preset.isItalic,
                                                    strokeColor = preset.strokeColor,
                                                    strokeWidth = preset.strokeWidth,
                                                    shadowColor = preset.shadowColor,
                                                    shadowOffsetX = preset.shadowOffsetX,
                                                    shadowOffsetY = preset.shadowOffsetY,
                                                    shadowBlur = preset.shadowBlur,
                                                    letterSpacing = preset.letterSpacing,
                                                    is3D = preset.is3D
                                                ))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.Text(
                                            "Clipp",
                                            style = presetTextStyle.copy(color = if (preset.strokeColor != Color.Transparent) preset.strokeColor else preset.textColor)
                                        )
                                        if (preset.strokeColor != Color.Transparent) {
                                            androidx.compose.material3.Text("Clipp", style = presetTextStyle.copy(color = preset.textColor, drawStyle = null))
                                        }
                                        if (preset.is3D) {
                                            androidx.compose.material3.Text("Clipp", style = presetTextStyle.copy(color = Color.Black.copy(alpha=0.5f)), modifier = Modifier.offset(2.dp, 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Text("Font", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableFonts.size) { i ->
                    val font = availableFonts[i]
                    FilterChip(
                        selected = textOverlay.fontName == font,
                        onClick = { onUpdate(textOverlay.copy(fontName = font)) },
                        label = { Text(font, fontFamily = getFontFamily(font)) }
                    )
                }
            }
            
            Text("Font Size", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = textOverlay.fontSize,
                onValueChange = { onUpdate(textOverlay.copy(fontSize = it)) },
                valueRange = 12f..200f
            )

            Text("Spacing (Letter & Line)", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Letter Spacing: ${textOverlay.letterSpacing.toInt()}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = textOverlay.letterSpacing,
                        onValueChange = { onUpdate(textOverlay.copy(letterSpacing = it)) },
                        valueRange = -5f..20f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Line Height: ${String.format("%.1f", textOverlay.lineHeightMultiplier)}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = textOverlay.lineHeightMultiplier,
                        onValueChange = { onUpdate(textOverlay.copy(lineHeightMultiplier = it)) },
                        valueRange = 0.5f..3f
                    )
                }
            }

            val presetColors = listOf(Color.White, Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray, Color.Transparent)
            
            Text("Text Color", style = MaterialTheme.typography.labelMedium)
            ColorWheelPicker(
                color = textOverlay.textColor,
                onColorChanged = { onUpdate(textOverlay.copy(textColor = it)) },
                onEyedropperClick = onEyedropperSelect
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetColors.size) { i ->
                    val color = presetColors[i]
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(2.dp, if (textOverlay.textColor == color) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape).clickable {
                        onUpdate(textOverlay.copy(textColor = color))
                    })
                }
            }
            
            Text("Background Color", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetColors.size) { i ->
                    val color = presetColors[i]
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(2.dp, if (textOverlay.backgroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape).clickable {
                        onUpdate(textOverlay.copy(backgroundColor = color))
                    })
                }
            }

            Text("Stroke (Outline)", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetColors.size) { i ->
                    val color = presetColors[i]
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(2.dp, if (textOverlay.strokeColor == color) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape).clickable {
                        onUpdate(textOverlay.copy(strokeColor = color))
                    })
                }
            }
            Slider(
                value = textOverlay.strokeWidth,
                onValueChange = { onUpdate(textOverlay.copy(strokeWidth = it)) },
                valueRange = 0f..20f
            )

            Text("Drop Shadow", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetColors.size) { i ->
                    val color = presetColors[i]
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(2.dp, if (textOverlay.shadowColor == color) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape).clickable {
                        onUpdate(textOverlay.copy(shadowColor = color))
                    })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Offset X", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = textOverlay.shadowOffsetX,
                        onValueChange = { onUpdate(textOverlay.copy(shadowOffsetX = it)) },
                        valueRange = -20f..20f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Offset Y", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = textOverlay.shadowOffsetY,
                        onValueChange = { onUpdate(textOverlay.copy(shadowOffsetY = it)) },
                        valueRange = -20f..20f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Blur", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = textOverlay.shadowBlur,
                        onValueChange = { onUpdate(textOverlay.copy(shadowBlur = it)) },
                        valueRange = 0f..30f
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable 3D Depth Effect", modifier = Modifier.weight(1f))
                Switch(checked = textOverlay.is3D, onCheckedChange = { onUpdate(textOverlay.copy(is3D = it)) })
            }

            Text("Duration", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${textOverlay.durationMs / 1000f}s")
                Slider(
                    value = textOverlay.durationMs.toFloat(),
                    onValueChange = { onUpdate(textOverlay.copy(durationMs = it.toLong())) },
                    valueRange = 500f..30000f,
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                )
            }

            Divider(Modifier.padding(vertical = 8.dp))
            Text("Animations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            var selectedAnimTab by remember { mutableStateOf(0) }
            TabRow(selectedTabIndex = selectedAnimTab) {
                Tab(selected = selectedAnimTab == 0, onClick = { selectedAnimTab = 0 }, text = { Text("In") })
                Tab(selected = selectedAnimTab == 1, onClick = { selectedAnimTab = 1 }, text = { Text("Loop") })
                Tab(selected = selectedAnimTab == 2, onClick = { selectedAnimTab = 2 }, text = { Text("Out") })
            }
            when (selectedAnimTab) {
                0 -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                        val anims = TextAnimIn.values()
                        items(anims.size) { i ->
                            val anim = anims[i]
                            val isSelected = textOverlay.animIn == anim
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onUpdate(textOverlay.copy(animIn = anim)) }
                                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedTextPreview(animIn = anim, text = anim.name.lowercase().replace("_", " ").capitalize())
                            }
                        }
                    }
                    if (textOverlay.animIn != TextAnimIn.NONE) {
                        Spacer(Modifier.height(16.dp))
                        if (textOverlay.animIn == TextAnimIn.TYPEWRITER) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Show Cursor Blink", modifier = Modifier.weight(1f))
                                Switch(checked = textOverlay.isTypewriterCursor, onCheckedChange = { onUpdate(textOverlay.copy(isTypewriterCursor = it)) })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Duration (${textOverlay.animInDurationMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = textOverlay.animInDurationMs.toFloat(),
                                onValueChange = { onUpdate(textOverlay.copy(animInDurationMs = it.toLong())) },
                                valueRange = 200f..2000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Delay (${textOverlay.animInDelayMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = textOverlay.animInDelayMs.toFloat(),
                                onValueChange = { onUpdate(textOverlay.copy(animInDelayMs = it.toLong())) },
                                valueRange = 0f..5000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
                1 -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                        val anims = TextAnimLoop.values()
                        items(anims.size) { i ->
                            val anim = anims[i]
                            val isSelected = textOverlay.animLoop == anim
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onUpdate(textOverlay.copy(animLoop = anim)) }
                                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedTextPreview(animLoop = anim, text = anim.name.lowercase().replace("_", " ").capitalize())
                            }
                        }
                    }
                    if (textOverlay.animLoop != TextAnimLoop.NONE) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Duration (${textOverlay.animLoopDurationMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = textOverlay.animLoopDurationMs.toFloat(),
                                onValueChange = { onUpdate(textOverlay.copy(animLoopDurationMs = it.toLong())) },
                                valueRange = 200f..2000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Delay (${textOverlay.animLoopDelayMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = textOverlay.animLoopDelayMs.toFloat(),
                                onValueChange = { onUpdate(textOverlay.copy(animLoopDelayMs = it.toLong())) },
                                valueRange = 0f..5000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
                2 -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                        val anims = TextAnimOut.values()
                        items(anims.size) { i ->
                            val anim = anims[i]
                            val isSelected = textOverlay.animOut == anim
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onUpdate(textOverlay.copy(animOut = anim)) }
                                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedTextPreview(animOut = anim, text = anim.name.lowercase().replace("_", " ").capitalize())
                            }
                        }
                    }
                    if (textOverlay.animOut != TextAnimOut.NONE) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Duration (${textOverlay.animOutDurationMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = textOverlay.animOutDurationMs.toFloat(),
                                onValueChange = { onUpdate(textOverlay.copy(animOutDurationMs = it.toLong())) },
                                valueRange = 200f..2000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Delay (${textOverlay.animOutDelayMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = textOverlay.animOutDelayMs.toFloat(),
                                onValueChange = { onUpdate(textOverlay.copy(animOutDelayMs = it.toLong())) },
                                valueRange = 0f..5000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
