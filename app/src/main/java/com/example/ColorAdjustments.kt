package com.example

import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap

data class ColorAdjustments(
    val brightness: Float = 0f, // -100 to 100
    val contrast: Float = 0f, // -100 to 100
    val saturation: Float = 0f, // -100 to 100
    val warmth: Float = 0f, // -100 to 100
    val tint: Float = 0f, // -100 to 100
    val highlights: Float = 0f, // -100 to 100
    val shadows: Float = 0f, // -100 to 100
    val sharpness: Float = 0f, // 0 to 100
    val vignette: Float = 0f, // 0 to 100
    val grain: Float = 0f, // 0 to 100
    
    val lutPresetId: String? = null,
    val lutIntensity: Float = 1f
)

fun parseCubeLut(context: android.content.Context, uri: android.net.Uri): FloatArray? {
    try {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
        var size = 0
        var line: String?
        val data = mutableListOf<FloatArray>()
        while (reader.readLine().also { line = it } != null) {
            val l = line!!.trim()
            if (l.isEmpty() || l.startsWith("#")) continue
            if (l.startsWith("LUT_3D_SIZE")) {
                size = l.split(Regex("\\s+"))[1].toInt()
            } else if (!l.startsWith("DOMAIN_") && !l.startsWith("TITLE")) {
                val parts = l.split(Regex("\\s+"))
                if (parts.size >= 3) {
                    val r = parts[0].toFloatOrNull()
                    val g = parts[1].toFloatOrNull()
                    val b = parts[2].toFloatOrNull()
                    if (r != null && g != null && b != null) {
                        data.add(floatArrayOf(r, g, b))
                    }
                }
            }
        }
        reader.close()
        
        if (size > 0 && data.size == size * size * size) {
            val getIdx = { r: Int, g: Int, b: Int -> r + g * size + b * size * size }
            val c000 = data[getIdx(0, 0, 0)]
            val c100 = data[getIdx(size - 1, 0, 0)]
            val c010 = data[getIdx(0, size - 1, 0)]
            val c001 = data[getIdx(0, 0, size - 1)]
            
            val values = FloatArray(20)
            values[4] = c000[0] * 255f
            values[9] = c000[1] * 255f
            values[14] = c000[2] * 255f
            values[19] = 1f 

            values[0] = c100[0] - c000[0]
            values[5] = c100[1] - c000[1]
            values[10] = c100[2] - c000[2]

            values[1] = c010[0] - c000[0]
            values[6] = c010[1] - c000[1]
            values[11] = c010[2] - c000[2]

            values[2] = c001[0] - c000[0]
            values[7] = c001[1] - c000[1]
            values[12] = c001[2] - c000[2]

            values[18] = 1f
            return values
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun getColorMatrixForAdjustments(adjustments: ColorAdjustments): ColorMatrix {
    val matrix = ColorMatrix()
    
    // LUT Preset
    if (adjustments.lutPresetId != null) {
        val lut = LutsManager.builtInLuts.find { it.id == adjustments.lutPresetId } ?: 
                  LutsManager.importedLuts.find { it.id == adjustments.lutPresetId } ?:
                  LutsManager.customGrades.find { it.id == adjustments.lutPresetId }?.let { customPreset ->
                      // If it's a custom preset, should it apply here?
                      null
                  }
        
        if (lut != null) {
            val lutMatrix = ColorMatrix(lut.matrixValues)
            if (adjustments.lutIntensity < 1f) {
                // Interpolate. 
                // Identity for color matrix: values[0]=1, values[6]=1, values[12]=1, values[18]=1, rest 0
                val identity = FloatArray(20).apply { this[0] = 1f; this[6] = 1f; this[12] = 1f; this[18] = 1f }
                val target = lut.matrixValues
                val result = FloatArray(20)
                for (i in 0..19) {
                    result[i] = identity[i] + (target[i] - identity[i]) * adjustments.lutIntensity
                }
                matrix.timesAssign(ColorMatrix(result))
            } else {
                matrix.timesAssign(lutMatrix)
            }
        }
    }
    
    // Brightness: -100 to 100 -> translate rgb
    val b = adjustments.brightness * 255f / 100f
    if (b != 0f) {
        val brightnessMatrix = ColorMatrix().apply {
            values[4] = b; values[9] = b; values[14] = b
        }
        matrix.timesAssign(brightnessMatrix)
    }

    // Contrast: -100 to 100 -> scale rgb around 0.5
    val c = adjustments.contrast
    if (c != 0f) {
        val contrastScale = if (c > 0) 1f + c / 100f else 1f + c / 100f
        val translate = (-0.5f * contrastScale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix().apply {
            values[0] = contrastScale; values[6] = contrastScale; values[12] = contrastScale
            values[4] = translate; values[9] = translate; values[14] = translate
        }
        matrix.timesAssign(contrastMatrix)
    }

    // Saturation: -100 to 100
    val s = adjustments.saturation
    if (s != 0f) {
        val satMatrix = ColorMatrix().apply {
            setToSaturation(1f + s / 100f)
        }
        matrix.timesAssign(satMatrix)
    }

    // Warmth: -100 to 100
    val w = adjustments.warmth
    if (w != 0f) {
        val rScale = 1f + w / 200f
        val bScale = 1f - w / 200f
        val warmthMatrix = ColorMatrix().apply {
            values[0] = rScale
            values[12] = bScale
        }
        matrix.timesAssign(warmthMatrix)
    }

    // Tint: -100 to 100 (Green to Magenta)
    val t = adjustments.tint
    if (t != 0f) {
        val gScale = 1f - t / 200f
        val rbScale = 1f + t / 400f
        val tintMatrix = ColorMatrix().apply {
            values[0] = rbScale
            values[6] = gScale
            values[12] = rbScale
        }
        matrix.timesAssign(tintMatrix)
    }

    // Shadows: -100 to 100 (Approximation using contrast & brightness)
    val sh = adjustments.shadows
    if (sh != 0f) {
        val shScale = 1f - sh / 200f
        val shTranslate = sh * 2.55f / 2f
        val shMatrix = ColorMatrix().apply {
            values[0] = shScale; values[6] = shScale; values[12] = shScale
            values[4] = shTranslate; values[9] = shTranslate; values[14] = shTranslate
        }
        matrix.timesAssign(shMatrix)
    }

    // Highlights: -100 to 100
    val hl = adjustments.highlights
    if (hl != 0f) {
        val hlScale = 1f + hl / 200f
        val hlTranslate = -hl * 2.55f / 2f
        val hlMatrix = ColorMatrix().apply {
            values[0] = hlScale; values[6] = hlScale; values[12] = hlScale
            values[4] = hlTranslate; values[9] = hlTranslate; values[14] = hlTranslate
        }
        matrix.timesAssign(hlMatrix)
    }

    return matrix
}

private var noiseBitmap: ImageBitmap? = null

fun Modifier.vignetteAndGrain(vignette: Float, grain: Float): Modifier = this.drawWithContent {
    drawContent()
    
    if (vignette > 0f) {
        val intensity = vignette / 100f
        val radius = max(size.width, size.height) * 0.75f
        val brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = intensity * 0.8f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = radius
        )
        drawRect(brush = brush)
    }
    
    if (grain > 0f) {
        val intensity = (grain / 100f) * 0.15f // Keep it subtle
        if (noiseBitmap == null || noiseBitmap!!.width != 256 || noiseBitmap!!.height != 256) {
            val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(256 * 256)
            for (i in pixels.indices) {
                val noise = Random.nextInt(256)
                // White noise with alpha
                pixels[i] = android.graphics.Color.argb(255, noise, noise, noise)
            }
            bmp.setPixels(pixels, 0, 256, 0, 0, 256, 256)
            noiseBitmap = bmp.asImageBitmap()
        }
        
        noiseBitmap?.let { bmp ->
            // Tile the noise bitmap across the screen
            val numCols = kotlin.math.ceil(size.width / bmp.width).toInt()
            val numRows = kotlin.math.ceil(size.height / bmp.height).toInt()
            for (x in 0 until numCols) {
                for (y in 0 until numRows) {
                    drawImage(
                        image = bmp,
                        topLeft = Offset(x * bmp.width.toFloat(), y * bmp.height.toFloat()),
                        alpha = intensity,
                        blendMode = androidx.compose.ui.graphics.BlendMode.Overlay // Overlay blend mode for grain
                    )
                }
            }
        }
    }
}
