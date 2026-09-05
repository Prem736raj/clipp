package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReframePanel(
    clip: MediaClip,
    onApplyParams: (Float, Float, Float) -> Unit, // cropScale, cropOffsetX, cropOffsetY (fake generated)
    onClose: () -> Unit
) {
    var mode by remember { mutableStateOf("Subject Tracking") } // "Subject Tracking" or "Focus on Face"
    var targetRatio by remember { mutableStateOf("9:16") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var isApplied by remember { mutableStateOf(false) }

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
                Text("AI Smart Reframe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }

            if (isAnalyzing) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(progress = analysisProgress)
                    Text("Tracking Subject... ${(analysisProgress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
            } else if (!isApplied) {
                // Settings Selection
                Text("Target Aspect Ratio", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ratios = listOf("9:16", "1:1", "4:5", "16:9")
                    items(ratios.size) { i ->
                        val ratio = ratios[i]
                        FilterChip(
                            selected = targetRatio == ratio,
                            onClick = { targetRatio = ratio },
                            label = { Text(ratio) }
                        )
                    }
                }

                Text("Tracking Mode", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        onClick = { mode = "Subject Tracking" },
                        modifier = Modifier.weight(1f).height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (mode == "Subject Tracking") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Person, contentDescription = "Subject")
                            Text("Subject Track", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Card(
                        onClick = { mode = "Focus on Face" },
                        modifier = Modifier.weight(1f).height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (mode == "Focus on Face") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Face, contentDescription = "Face")
                            Text("Focus Face", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isAnalyzing = true
                            for(i in 0..100 step 10) {
                                analysisProgress = i / 100f
                                delay(100)
                            }
                            isAnalyzing = false
                            isApplied = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start AI Tracking")
                }
            } else {
                // Split Screen Comparison
                Text("Preview: Original vs Reframed", style = MaterialTheme.typography.labelMedium)
                
                Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Original View
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                        AsyncImage(
                            model = clip.sourceUri,
                            contentDescription = "Original",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxHeight()
                                .fillMaxWidth(0.56f) // approx 9:16 inside 16:9
                                .border(2.dp, Color.Yellow)
                        )
                        Text("Original", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(4.dp).background(Color.Black.copy(alpha=0.5f)))
                    }
                    
                    // Reframed View
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                        AsyncImage(
                            model = clip.sourceUri,
                            contentDescription = "Reframed",
                            contentScale = ContentScale.Crop, // implies zooming to fill the target aspect ratio
                            modifier = Modifier.fillMaxSize()
                        )
                        Text("Reframed ($mode)", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(4.dp).background(Color.Black.copy(alpha=0.5f)))
                    }
                }
                
                Text(
                    text = "Keyframes automatically added. You can fine-tune positions in the manual Transform tool.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { isApplied = false; analysisProgress = 0f }) { Text("Back") }
                    Button(onClick = { onApplyParams(1.5f, 0.2f, 0f) }) { Text("Apply Reframe") }
                }
            }
        }
    }
}
