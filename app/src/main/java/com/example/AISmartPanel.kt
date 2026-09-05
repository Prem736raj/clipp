package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISmartPanel(
    detectedScenes: List<SceneMarker>,
    detectedSilences: List<SilenceGap>,
    onDetectedScenesChange: (List<SceneMarker>) -> Unit,
    onDetectedSilencesChange: (List<SilenceGap>) -> Unit,
    onAutoCut: (List<SceneMarker>) -> Unit,
    onRemoveSilences: (List<SilenceGap>) -> Unit,
    onGenerateHighlights: (Int) -> Unit,
    onClose: () -> Unit,
    videoDurationMs: Long
) {
    var selectedFeature by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    var highlightDurationOption by remember { mutableStateOf(15) }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
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
                Text(
                    text = if (selectedFeature == null) "AI Smart Tools" else selectedFeature!!,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            if (selectedFeature == null) {
                // List Features
                SmartFeatureItem(title = "Auto Cut", description = "Detect scene changes & auto split", icon = Icons.Filled.MovieCreation) {
                    selectedFeature = "Auto Cut"
                }
                SmartFeatureItem(title = "Highlight Detection", description = "Auto-compile best moments reel", icon = Icons.Filled.Star) {
                    selectedFeature = "Highlight Detection"
                }
                SmartFeatureItem(title = "Silence Remover", description = "Detect & remove silent gaps", icon = Icons.Filled.VolumeOff) {
                    selectedFeature = "Silence Remover"
                }
            } else {
                if (isAnalyzing) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(progress = analysisProgress)
                        Text("Analyzing Video... ${(analysisProgress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    when (selectedFeature) {
                        "Auto Cut" -> {
                            if (detectedScenes.isEmpty()) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isAnalyzing = true
                                            for(i in 0..100 step 10) {
                                                analysisProgress = i / 100f
                                                delay(100)
                                            }
                                            isAnalyzing = false
                                            // Mock scene cuts every ~5 seconds
                                            val scenes = mutableListOf<SceneMarker>()
                                            var time = 5000L
                                            while (time < videoDurationMs) {
                                                scenes.add(SceneMarker(timeMs = time))
                                                time += 5000L + (0..2000).random()
                                            }
                                            onDetectedScenesChange(scenes)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Analyze Video for Cuts")
                                }
                            } else {
                                Text("Found ${detectedScenes.size} scene cuts.", style = MaterialTheme.typography.bodyMedium)
                                
                                // In a full app, we'd list them with checkboxes. Here we just show a summary.
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(onClick = { onDetectedScenesChange(emptyList()); selectedFeature = null }) {
                                        Text("Cancel")
                                    }
                                    Button(onClick = { onAutoCut(detectedScenes.filter { it.isSelected }) }) {
                                        Text("Apply Cuts")
                                    }
                                }
                            }
                        }
                        
                        "Highlight Detection" -> {
                            Text("Select highlight reel length:", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                FilterChip(selected = highlightDurationOption == 15, onClick = { highlightDurationOption = 15 }, label = { Text("15s") })
                                FilterChip(selected = highlightDurationOption == 30, onClick = { highlightDurationOption = 30 }, label = { Text("30s") })
                                FilterChip(selected = highlightDurationOption == 60, onClick = { highlightDurationOption = 60 }, label = { Text("60s") })
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isAnalyzing = true
                                        for(i in 0..100 step 10) {
                                            analysisProgress = i / 100f
                                            delay(150)
                                        }
                                        isAnalyzing = false
                                        onGenerateHighlights(highlightDurationOption)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Highlights Reel")
                            }
                        }

                        "Silence Remover" -> {
                            if (detectedSilences.isEmpty()) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isAnalyzing = true
                                            for(i in 0..100 step 10) {
                                                analysisProgress = i / 100f
                                                delay(120)
                                            }
                                            isAnalyzing = false
                                            
                                            // Mock silence detection
                                            val silences = mutableListOf<SilenceGap>()
                                            var time = 3000L
                                            while (time < videoDurationMs) {
                                                if (time + 1000L < videoDurationMs) {
                                                    silences.add(SilenceGap(startMs = time, endMs = time + 1500L))
                                                }
                                                time += 6000L + (0..3000).random()
                                            }
                                            onDetectedSilencesChange(silences)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Analyze for Silences")
                                }
                            } else {
                                Text("Found ${detectedSilences.size} silent gaps.", style = MaterialTheme.typography.bodyMedium)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(onClick = { onDetectedSilencesChange(emptyList()); selectedFeature = null }) {
                                        Text("Cancel")
                                    }
                                    Button(onClick = { onRemoveSilences(detectedSilences.filter { it.isSelected }) }) {
                                        Text("Remove Silences")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartFeatureItem(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
