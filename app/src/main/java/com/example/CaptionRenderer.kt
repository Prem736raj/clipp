package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Uses Android's local fallback chain so caption rendering is offline-safe. */
fun getFontForLanguage(language: String): FontFamily = FontFamily.SansSerif

@Composable
fun CaptionRenderer(
    captions: List<AutoCaptionSegment>,
    settings: CaptionSettings,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    if (captions.isEmpty()) return

    val currentCaption = captions.find {
        currentPositionMs >= it.startTimeMs && currentPositionMs < it.startTimeMs + it.durationMs
    }

    if (currentCaption != null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            val alignX = settings.posX
            val alignY = settings.posY
            val alignment = when {
                alignX < 0.33f -> Alignment.CenterStart
                alignX > 0.66f -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            // Word timing logic
            val activeWordIndex = currentCaption.words.indexOfFirst {
                currentPositionMs >= it.startMs && currentPositionMs < it.endMs
            }.takeIf { it >= 0 } ?: if (currentPositionMs > currentCaption.words.lastOrNull()?.endMs ?: 0L) currentCaption.words.lastIndex else 0

            val activeWord = currentCaption.words.getOrNull(activeWordIndex)

            val textAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(150))
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = textAlpha }
            ) {
                // Determine text style
                val fontWeight = FontWeight.Bold
                val fontSize = settings.fontSize.sp
                val fontFamily = getFontForLanguage(settings.language)
                
                // Construct the text based on style
                val displayStr = buildAnnotatedString {
                    currentCaption.words.forEachIndexed { index, cw ->
                        if (index > 0) append(" ")
                        
                        val isHighlighted = index == activeWordIndex
                        val isPast = index < activeWordIndex

                        when (settings.style) {
                            CaptionStyle.WORD_BY_WORD_HIGHLIGHT -> {
                                val color = if (isHighlighted) settings.highlightColor else settings.textColor
                                withStyle(style = SpanStyle(color = color, fontWeight = fontWeight, fontSize = fontSize, fontFamily = fontFamily)) {
                                    append(cw.word)
                                }
                            }
                            CaptionStyle.KARAOKE -> {
                                val color = if (isHighlighted || isPast) settings.highlightColor else settings.textColor
                                withStyle(style = SpanStyle(color = color, fontWeight = fontWeight, fontSize = fontSize, fontFamily = fontFamily)) {
                                    append(cw.word)
                                }
                            }
                            CaptionStyle.BOUNCE -> {
                                val color = settings.textColor
                                // We can do per-word bounce simply by font size changes, but that's hard in AnnotatedString
                                // We'll just do simple highlight for now due to compose text limitations
                                val sizeMultiplier = if (isHighlighted) 1.2f else 1.0f
                                val finalColor = if (isHighlighted) settings.highlightColor else settings.textColor
                                withStyle(style = SpanStyle(color = finalColor, fontWeight = fontWeight, fontSize = fontSize * sizeMultiplier, fontFamily = fontFamily)) {
                                    append(cw.word)
                                }
                            }
                            CaptionStyle.POP -> {
                                if (isHighlighted) {
                                    withStyle(style = SpanStyle(color = settings.highlightColor, fontWeight = fontWeight, fontSize = fontSize * 1.5f, fontFamily = fontFamily)) {
                                        append(cw.word)
                                    }
                                }
                            }
                            CaptionStyle.STANDARD -> {
                                withStyle(style = SpanStyle(color = settings.textColor, fontWeight = fontWeight, fontSize = fontSize, fontFamily = fontFamily)) {
                                    append(cw.word)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = displayStr,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            translationX = (settings.posX - 0.5f) * size.width
                            translationY = settings.posY * size.height
                        }
                        .background(settings.backgroundColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
