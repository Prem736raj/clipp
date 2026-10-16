package com.example

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
fun HapticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val view = LocalView.current
    val context = LocalContext.current
    var lastTickValue by remember { mutableFloatStateOf(value) }
    
    Slider(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
            val threshold = (valueRange.endInclusive - valueRange.start) / 20f // tick every 5%
            if (Math.abs(newValue - lastTickValue) >= threshold) {
                com.example.utils.HapticUtil.playSubtleTick(view, context)
                lastTickValue = newValue
            }
        },
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors
    )
}
