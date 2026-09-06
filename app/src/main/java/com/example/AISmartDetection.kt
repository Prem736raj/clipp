package com.example

data class SceneMarker(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timeMs: Long,
    val type: String = "Cut", // Cut, Highlight
    var isSelected: Boolean = true
)

data class SilenceGap(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    var isSelected: Boolean = true
)
