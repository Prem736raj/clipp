package com.example

import androidx.compose.ui.geometry.Offset

data class BgRemovalSettings(
    val enabled: Boolean = false,
    val isAutoProcessing: Boolean = false,
    val edgeSmoothing: Float = 0.5f,
    val replacementMode: BgReplacementMode = BgReplacementMode.TRANSPARENT,
    val replacementColor: Long = 0xFF000000,
    val replacementUri: String? = null,
    val manualRefinePoints: List<Offset> = emptyList() // brush points
)

enum class BgReplacementMode(val label: String) {
    TRANSPARENT("Transparent"),
    SOLID_COLOR("Solid Color"),
    IMAGE("Image"),
    VIDEO("Video")
}
