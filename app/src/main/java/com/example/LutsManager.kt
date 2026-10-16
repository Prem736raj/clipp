package com.example

import androidx.compose.runtime.mutableStateListOf

data class LutPreset(
    val id: String,
    val name: String,
    val matrixValues: FloatArray,
    val isCustom: Boolean = false,
    val uri: String? = null
)

data class CustomColorGrade(
    val id: String,
    val name: String,
    val adjustments: ColorAdjustments
)

object LutsManager {
    val builtInLuts = listOf(
        LutPreset("lut1", "Hollywood", floatArrayOf(
            1.2f, 0.1f, 0f, 0f, 10f,
            0f, 1.1f, 0.1f, 0f, -5f,
            0.1f, 0f, 0.9f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut2", "Indie Film", floatArrayOf(
            0.8f, 0.2f, 0.1f, 0f, 15f,
            0.1f, 0.9f, 0f, 0f, 10f,
            0.1f, 0.1f, 0.8f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut3", "Music Video", floatArrayOf(
            1.3f, -0.2f, -0.1f, 0f, 0f,
            -0.1f, 1.2f, -0.1f, 0f, 0f,
            -0.2f, -0.1f, 1.4f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut4", "Travel Vlog", floatArrayOf(
            1.1f, 0f, 0f, 0f, 10f,
            0f, 1.1f, 0f, 0f, 5f,
            0f, 0.1f, 1.2f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut5", "Food", floatArrayOf(
            1.15f, 0f, 0f, 0f, 5f,
            0f, 1.05f, 0f, 0f, 2f,
            0f, 0f, 0.95f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut6", "Fashion", floatArrayOf(
            0.9f, 0.05f, 0.05f, 0f, -5f,
            0.05f, 0.95f, 0f, 0f, 0f,
            0.1f, 0.1f, 0.85f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut7", "Moody", floatArrayOf(
            0.8f, 0.1f, 0.1f, 0f, -15f,
            0f, 0.85f, 0.15f, 0f, -5f,
            0f, 0f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut8", "Sunset Glow", floatArrayOf(
            1.2f, 0.1f, 0f, 0f, 20f,
            0.1f, 1.0f, 0f, 0f, 10f,
            0f, 0f, 0.8f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut9", "Forest Green", floatArrayOf(
            0.9f, 0.1f, 0f, 0f, -5f,
            0f, 1.15f, 0f, 0f, 10f,
            0f, 0.1f, 0.9f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        )),
        LutPreset("lut10", "Urban Night", floatArrayOf(
            0.8f, 0f, 0.2f, 0f, -10f,
            0f, 0.9f, 0.2f, 0f, -5f,
            0f, 0f, 1.25f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        ))
    )

    val customGrades = mutableStateListOf<CustomColorGrade>()
    val importedLuts = mutableStateListOf<LutPreset>()
}
