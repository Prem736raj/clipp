package com.example

data class PhotoAnimationSettings(
    val type: PhotoAnimationType = PhotoAnimationType.NONE,
    val intensity: Float = 0.5f,
    val animationDurationMs: Long = 3000L
)

enum class PhotoAnimationType(val title: String) {
    NONE("None"),
    ANIMATE("Animate Photo"),
    KEN_BURNS("Ken Burns+"),
    ZOOM_3D("3D Zoom"),
    CINEMAGRAPH("Cinemagraph")
}
