package com.example

import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Rect

fun Modifier.colorFilterOverlay(matrix: ColorMatrix): Modifier = this.drawWithContent {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(matrix)
        }
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
    }
    drawContent()
    drawIntoCanvas { canvas ->
        canvas.restore()
    }
}

enum class FilterType(val category: String, val label: String) {
    NONE("Basic", "None/Original"),
    
    // Natural
    WARM("Natural", "Warm"),
    COOL("Natural", "Cool"),
    VIVID("Natural", "Vivid"),
    SOFT("Natural", "Soft"),
    BRIGHT("Natural", "Bright"),
    
    // Cinematic
    TEAL_ORANGE("Cinematic", "Teal & Orange"),
    FILM_NOIR("Cinematic", "Film Noir"),
    VINTAGE_FILM("Cinematic", "Vintage Film"),
    BLOCKBUSTER("Cinematic", "Blockbuster"),
    GOLDEN_HOUR("Cinematic", "Golden Hour"),
    
    // Mood
    DRAMATIC("Mood", "Dramatic"),
    DREAMY("Mood", "Dreamy"),
    MELANCHOLY("Mood", "Melancholy"),
    ENERGETIC("Mood", "Energetic"),
    COZY("Mood", "Cozy"),
    
    // Retro
    VHS("Retro", "VHS"),
    POLAROID("Retro", "Polaroid"),
    SEPIA("Retro", "Sepia"),
    SEVENTIES("Retro", "70s"),
    EIGHTIES("Retro", "80s"),
    NINETIES_NOSTALGIA("Retro", "90s Nostalgia"),
    
    // B&W
    CLASSIC_BW("B&W", "Classic B&W"),
    HIGH_CONTRAST_BW("B&W", "High Contrast B&W"),
    SILVER("B&W", "Silver"),
    NEWSPAPER("B&W", "Newspaper"),
    
    // Social
    IG_WARM("Social", "Instagram Warm"),
    BRIGHT_AIRY("Social", "Bright & Airy"),
    MOODY_DARK("Social", "Moody Dark"),
    CLEAN_STUDIO("Social", "Clean Studio"),
    CYBERPUNK("Social", "Cyberpunk")
}

fun getColorMatrixForFilter(filter: FilterType, intensity: Float): ColorMatrix {
    val matrix = ColorMatrix()
    if (filter == FilterType.NONE || intensity <= 0f) return matrix
    
    // Base matrices for different filters
    when (filter) {
        // Natural
        FilterType.WARM -> {
            matrix.setToScale(1f + 0.2f * intensity, 1f + 0.1f * intensity, 1f - 0.1f * intensity, 1f)
        }
        FilterType.COOL -> {
            matrix.setToScale(1f - 0.1f * intensity, 1f + 0.1f * intensity, 1f + 0.2f * intensity, 1f)
        }
        FilterType.VIVID -> {
            matrix.setToSaturation(1f + 1.5f * intensity)
        }
        FilterType.SOFT -> {
            // Lower contrast
            val c = 1f - 0.3f * intensity
            val o = 255f * 0.15f * intensity
            matrix.values.fill(0f)
            matrix.values[0] = c; matrix.values[6] = c; matrix.values[12] = c; matrix.values[18] = 1f
            matrix.values[4] = o; matrix.values[9] = o; matrix.values[14] = o
        }
        FilterType.BRIGHT -> {
            val brightness = 255f * 0.2f * intensity
            matrix.values[4] = brightness; matrix.values[9] = brightness; matrix.values[14] = brightness
        }
        
        // Cinematic
        FilterType.TEAL_ORANGE -> {
            matrix.values.fill(0f)
            matrix.values[0] = 1f + 0.3f * intensity // R
            matrix.values[6] = 1f + 0.1f * intensity // G
            matrix.values[12] = 1f + 0.4f * intensity // B
            matrix.values[18] = 1f
        }
        FilterType.FILM_NOIR -> {
            matrix.setToSaturation(1f - intensity)
            val c = 1f + 0.5f * intensity
            val o = -255f * 0.1f * intensity
            val contrastMatrix = ColorMatrix().apply {
                values[0] = c; values[6] = c; values[12] = c; values[18] = 1f
                values[4] = o; values[9] = o; values[14] = o
            }
            matrix.timesAssign(contrastMatrix)
        }
        FilterType.VINTAGE_FILM -> {
            matrix.setToSaturation(1f - 0.3f * intensity)
            val temp = ColorMatrix().apply {
                setToScale(1f + 0.1f * intensity, 1f, 1f - 0.2f * intensity, 1f)
            }
            matrix.timesAssign(temp)
        }
        FilterType.BLOCKBUSTER -> {
            val c = 1f + 0.3f * intensity
            val o = -255f * 0.15f * intensity
            matrix.values[0] = c; matrix.values[6] = c; matrix.values[12] = c; matrix.values[18] = 1f
            matrix.values[4] = o; matrix.values[9] = o; matrix.values[14] = o
            val sat = ColorMatrix().apply { setToSaturation(1f + 0.2f * intensity) }
            matrix.timesAssign(sat)
        }
        FilterType.GOLDEN_HOUR -> {
            matrix.setToScale(1f + 0.3f * intensity, 1f + 0.15f * intensity, 1f - 0.2f * intensity, 1f)
            matrix.values[4] = 20f * intensity; matrix.values[9] = 10f * intensity
        }
        
        // Mood
        FilterType.DRAMATIC -> {
            val c = 1f + 0.4f * intensity
            val o = -255f * 0.2f * intensity
            matrix.values[0] = c; matrix.values[6] = c; matrix.values[12] = c; matrix.values[18] = 1f
            matrix.values[4] = o; matrix.values[9] = o; matrix.values[14] = o
            val sat = ColorMatrix().apply { setToSaturation(1f - 0.2f * intensity) }
            matrix.timesAssign(sat)
        }
        FilterType.DREAMY -> {
            val c = 1f - 0.2f * intensity
            val o = 255f * 0.2f * intensity
            matrix.values[0] = c; matrix.values[6] = c; matrix.values[12] = c; matrix.values[18] = 1f
            matrix.values[4] = o; matrix.values[9] = o; matrix.values[14] = o
            val sat = ColorMatrix().apply { setToSaturation(1f - 0.3f * intensity) }
            matrix.timesAssign(sat)
        }
        FilterType.MELANCHOLY -> {
            matrix.setToSaturation(1f - 0.5f * intensity)
            val temp = ColorMatrix().apply {
                setToScale(1f - 0.2f * intensity, 1f - 0.1f * intensity, 1f + 0.1f * intensity, 1f)
            }
            matrix.timesAssign(temp)
        }
        FilterType.ENERGETIC -> {
            matrix.setToSaturation(1f + 0.4f * intensity)
            val c = 1f + 0.2f * intensity
            val o = -255f * 0.1f * intensity
            val temp = ColorMatrix().apply {
                values[0] = c; values[6] = c; values[12] = c; values[18] = 1f
                values[4] = o; values[9] = o; values[14] = o
            }
            matrix.timesAssign(temp)
        }
        FilterType.COZY -> {
            matrix.setToScale(1f + 0.2f * intensity, 1f + 0.1f * intensity, 1f - 0.1f * intensity, 1f)
            val sat = ColorMatrix().apply { setToSaturation(1f - 0.1f * intensity) }
            matrix.timesAssign(sat)
        }
        
        // Retro
        FilterType.VHS -> {
            matrix.setToSaturation(1f + 0.5f * intensity)
            val c = 1f - 0.2f * intensity
            val temp = ColorMatrix().apply {
                values[0] = c; values[6] = c; values[12] = c; values[18] = 1f
            }
            matrix.timesAssign(temp)
        }
        FilterType.POLAROID -> {
            matrix.setToSaturation(1f - 0.2f * intensity)
            val temp = ColorMatrix().apply {
                setToScale(1f + 0.1f * intensity, 1f + 0.1f * intensity, 1f - 0.1f * intensity, 1f)
                values[4] = 20f * intensity; values[9] = 20f * intensity; values[14] = 20f * intensity
            }
            matrix.timesAssign(temp)
        }
        FilterType.SEPIA -> {
            val inv = 1f - intensity
            val newValues = floatArrayOf(
                inv + intensity * 0.393f, intensity * 0.769f, intensity * 0.189f, 0f, 0f,
                intensity * 0.349f, inv + intensity * 0.686f, intensity * 0.168f, 0f, 0f,
                intensity * 0.272f, intensity * 0.534f, inv + intensity * 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            for (i in 0..19) { matrix.values[i] = newValues[i] }
        }
        FilterType.SEVENTIES -> {
            matrix.setToSaturation(1f - 0.3f * intensity)
            val temp = ColorMatrix().apply {
                setToScale(1f + 0.2f * intensity, 1f + 0.1f * intensity, 1f - 0.3f * intensity, 1f)
            }
            matrix.timesAssign(temp)
        }
        FilterType.EIGHTIES -> {
            matrix.setToSaturation(1f + 0.3f * intensity)
            val temp = ColorMatrix().apply {
                setToScale(1f - 0.1f * intensity, 1f + 0.2f * intensity, 1f + 0.3f * intensity, 1f)
            }
            matrix.timesAssign(temp)
        }
        FilterType.NINETIES_NOSTALGIA -> {
            matrix.setToSaturation(1f - 0.2f * intensity)
            val c = 1f - 0.1f * intensity
            val temp = ColorMatrix().apply {
                values[0] = c; values[6] = c; values[12] = c; values[18] = 1f
            }
            matrix.timesAssign(temp)
        }
        
        // B&W
        FilterType.CLASSIC_BW -> {
            matrix.setToSaturation(1f - intensity)
        }
        FilterType.HIGH_CONTRAST_BW -> {
            matrix.setToSaturation(1f - intensity)
            val c = 1f + 0.6f * intensity
            val o = -255f * 0.3f * intensity
            val temp = ColorMatrix().apply {
                values[0] = c; values[6] = c; values[12] = c; values[18] = 1f
                values[4] = o; values[9] = o; values[14] = o
            }
            matrix.timesAssign(temp)
        }
        FilterType.SILVER -> {
            matrix.setToSaturation(1f - intensity)
            val c = 1f - 0.2f * intensity
            val o = 255f * 0.2f * intensity
            val temp = ColorMatrix().apply {
                values[0] = c; values[6] = c; values[12] = c; values[18] = 1f
                values[4] = o; values[9] = o; values[14] = o
            }
            matrix.timesAssign(temp)
        }
        FilterType.NEWSPAPER -> {
            matrix.setToSaturation(1f - intensity)
            val c = 1f + 1.0f * intensity
            val o = -255f * 0.5f * intensity
            val temp = ColorMatrix().apply {
                values[0] = c; values[6] = c; values[12] = c; values[18] = 1f
                values[4] = o; values[9] = o; values[14] = o
            }
            matrix.timesAssign(temp)
        }
        
        // Social
        FilterType.IG_WARM -> {
            matrix.setToScale(1f + 0.15f * intensity, 1f + 0.05f * intensity, 1f - 0.15f * intensity, 1f)
            val sat = ColorMatrix().apply { setToSaturation(1f + 0.2f * intensity) }
            matrix.timesAssign(sat)
        }
        FilterType.BRIGHT_AIRY -> {
            val c = 1f - 0.1f * intensity
            val o = 255f * 0.15f * intensity
            matrix.values[0] = c; matrix.values[6] = c; matrix.values[12] = c; matrix.values[18] = 1f
            matrix.values[4] = o; matrix.values[9] = o; matrix.values[14] = o
            val sat = ColorMatrix().apply { setToSaturation(1f - 0.1f * intensity) }
            matrix.timesAssign(sat)
        }
        FilterType.MOODY_DARK -> {
            val c = 1f + 0.2f * intensity
            val o = -255f * 0.2f * intensity
            matrix.values[0] = c; matrix.values[6] = c; matrix.values[12] = c; matrix.values[18] = 1f
            matrix.values[4] = o; matrix.values[9] = o; matrix.values[14] = o
            val sat = ColorMatrix().apply { setToSaturation(1f - 0.3f * intensity) }
            matrix.timesAssign(sat)
        }
        FilterType.CLEAN_STUDIO -> {
            matrix.setToSaturation(1f + 0.1f * intensity)
            val c = 1f + 0.1f * intensity
            val temp = ColorMatrix().apply {
                values[0] = c; values[6] = c; values[12] = c; values[18] = 1f
            }
            matrix.timesAssign(temp)
        }
        FilterType.CYBERPUNK -> {
            matrix.values.fill(0f)
            // Push towards cyan and magenta
            matrix.values[0] = 1f + 0.5f * intensity // R
            matrix.values[6] = 1f - 0.2f * intensity // G
            matrix.values[12] = 1f + 0.5f * intensity // B
            matrix.values[18] = 1f
            
            val contrast = 1f + 0.4f * intensity
            val o = -255f * 0.2f * intensity
            val temp = ColorMatrix().apply {
                values[0] = contrast; values[6] = contrast; values[12] = contrast; values[18] = 1f
                values[4] = o; values[9] = o; values[14] = o
            }
            matrix.timesAssign(temp)
        }
        else -> {}
    }
    return matrix
}
