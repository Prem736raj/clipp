package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.VideoStable
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

data class AIEnhanceSettings(
    val autoEnhanceApplied: Boolean = false,
    val upscaled: Boolean = false,
    val stabilizationStrength: Float = 0f // 0f means off, 1f means max
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIEnhancePanel(
    clip: MediaClip,
    initialSettings: AIEnhanceSettings,
    onApplyParams: (AIEnhanceSettings) -> Unit,
    onClose: () -> Unit
) {
    var mode by remember { mutableStateOf("Auto Enhance") } // "Auto Enhance", "Upscale", "Stabilize"
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    var localSettings by remember { mutableStateOf(initialSettings) }
    var currentStabilization by remember { mutableFloatStateOf(initialSettings.stabilizationStrength) }
    var isPreviewingOriginal by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AI Video Enhance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = mode == "Auto Enhance",
                        onClick = { mode = "Auto Enhance" },
                        label = { Text("Auto Enhance") }
                    )
                }
                item {
                    FilterChip(
                        selected = mode == "Upscale",
                        onClick = { mode = "Upscale" },
                        label = { Text("Upscale") }
                    )
                }
                item {
                    FilterChip(
                        selected = mode == "Stabilize",
                        onClick = { mode = "Stabilize" },
                        label = { Text("Stabilize") }
                    )
                }
            }

            if (isAnalyzing) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(progress = analysisProgress)
                    Text(
                        if (mode == "Upscale") "Upscaling to 4K... ${(analysisProgress * 100).toInt()}%" 
                        else "Analyzing Frame... ${(analysisProgress * 100).toInt()}%", 
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                when (mode) {
                    "Auto Enhance" -> {
                        OutlinedButton(
                            onClick = { isPreviewingOriginal = !isPreviewingOriginal },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (isPreviewingOriginal) "Show Enhanced" else "Hold to Compare Original")
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                            // Dummy contrast/brightness color matrix to simulate auto enhancement
                            val matrix = if (localSettings.autoEnhanceApplied && !isPreviewingOriginal) {
                                ColorMatrix().apply {
                                    setToScale(1.1f, 1.1f, 1.2f, 1f)
                                }
                            } else {
                                ColorMatrix()
                            }
                            
                            AsyncImage(
                                model = clip.sourceUri,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                                colorFilter = ColorFilter.colorMatrix(matrix)
                            )
                            
                            if (localSettings.autoEnhanceApplied && !isPreviewingOriginal) {
                                Text("Enhanced", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                            } else if (isPreviewingOriginal) {
                                Text("Original", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isAnalyzing = true
                                    for(i in 0..100 step 15) {
                                        analysisProgress = i / 100f
                                        delay(100)
                                    }
                                    isAnalyzing = false
                                    localSettings = localSettings.copy(autoEnhanceApplied = true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !localSettings.autoEnhanceApplied
                        ) {
                            Text(if (localSettings.autoEnhanceApplied) "Enhancement Applied" else "Apply Auto Enhance")
                        }
                    }
                    "Upscale" -> {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                            AsyncImage(
                                model = clip.sourceUri,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (localSettings.upscaled) {
                                Text("4K Upscaled", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Blue.copy(alpha=0.7f), RoundedCornerShape(4.dp)).padding(4.dp))
                            } else {
                                Text("720p Original", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isAnalyzing = true
                                    for(i in 0..100 step 5) {
                                        analysisProgress = i / 100f
                                        delay(150) // Slower for upscale
                                    }
                                    isAnalyzing = false
                                    localSettings = localSettings.copy(upscaled = true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !localSettings.upscaled
                        ) {
                            Text(if (localSettings.upscaled) "Upscaling Complete" else "Upscale to 4K")
                        }
                    }
                    "Stabilize" -> {
                        OutlinedButton(
                            onClick = { isPreviewingOriginal = !isPreviewingOriginal },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (isPreviewingOriginal) "Show Stabilized" else "Show Original")
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                            val zoomScale = if (!isPreviewingOriginal && currentStabilization > 0f) {
                                1f + (currentStabilization * 0.3f) // Max 30% crop for stabilization
                            } else {
                                1f
                            }
                            
                            AsyncImage(
                                model = clip.sourceUri,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoomScale, scaleY = zoomScale)
                            )
                            
                            val labelText = if (isPreviewingOriginal) "Original" else "Stabilized (Crop ${(currentStabilization*30).toInt()}%)"
                            Text(labelText, style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Stabilization Strength: ${(currentStabilization * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = currentStabilization,
                                onValueChange = { currentStabilization = it },
                                onValueChangeFinished = {
                                    localSettings = localSettings.copy(stabilizationStrength = currentStabilization)
                                }
                            )
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onClose) { Text("Cancel") }
                    Button(onClick = { onApplyParams(localSettings) }) { Text("Apply") }
                }
            }
        }
    }
}
