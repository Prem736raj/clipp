package com.example

import androidx.compose.ui.graphics.Color

data class CaptionWord(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

data class AutoCaptionSegment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val words: List<CaptionWord>,
    val startTimeMs: Long,
    val durationMs: Long
)

enum class CaptionStyle {
    STANDARD,
    WORD_BY_WORD_HIGHLIGHT,
    KARAOKE,
    BOUNCE,
    POP
}

val SUPPORTED_CAPTION_LANGUAGES = listOf(
    "Auto-Detect Language", "English", "Hindi", "Tamil", "Telugu", "Bengali", "Marathi",
    "Kannada", "Gujarati", "Malayalam", "Punjabi", "Urdu", "Hinglish (Roman)"
)

data class CaptionSettings(
    val style: CaptionStyle = CaptionStyle.WORD_BY_WORD_HIGHLIGHT,
    val fontName: String = "Roboto",
    val fontSize: Float = 48f,
    val textColor: Color = Color.White,
    val highlightColor: Color = Color.Yellow,
    val backgroundColor: Color = Color.Transparent,
    val alignment: TextAlignmentType = TextAlignmentType.Center,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 4f,
    val posX: Float = 0.5f,
    val posY: Float = 0.85f,
    val language: String = "Auto-Detect Language"
)
