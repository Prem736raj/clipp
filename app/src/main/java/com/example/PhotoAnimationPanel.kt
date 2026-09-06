package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAnimationPanel(
    clip: MediaClip,
    initialSettings: PhotoAnimationSettings,
    onApplyParams: (PhotoAnimationSettings) -> Unit,
    onClose: () -> Unit
) {
    if (!clip.isPhoto) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("AI Photo Animation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Photo animation is only available for photos.")
                }
            }
        }
        return
    }

    var selectedType by remember { mutableStateOf(initialSettings.type) }
    var intensity by remember { mutableFloatStateOf(initialSettings.intensity) }
    var durationMs by remember { mutableFloatStateOf(initialSettings.animationDurationMs.toFloat()) }
    
    // Auto-apply on change
    LaunchedEffect(selectedType, intensity, durationMs) {
        onApplyParams(
            initialSettings.copy(
                type = selectedType,
                intensity = intensity,
                animationDurationMs = durationMs.toLong()
            )
        )
    }

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
                Text("AI Photo Animation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }

            Text("Select Animation Effect", style = MaterialTheme.typography.labelMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhotoAnimationType.values().take(3).forEach { type ->
                    FilterChip(
                        selected = type == selectedType,
                        onClick = { selectedType = type },
                        label = { Text(type.title) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhotoAnimationType.values().drop(3).forEach { type ->
                    FilterChip(
                        selected = type == selectedType,
                        onClick = { selectedType = type },
                        label = { Text(type.title) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(visible = selectedType != PhotoAnimationType.NONE) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Intensity", style = MaterialTheme.typography.labelMedium)
                            Text("${(intensity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = intensity,
                            onValueChange = { intensity = it },
                            valueRange = 0f..1f
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Duration", style = MaterialTheme.typography.labelMedium)
                            Text("${(durationMs / 1000).toInt()}s", style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = durationMs,
                            onValueChange = { durationMs = it },
                            valueRange = 1000f..10000f
                        )
                        Text(
                            "Sets the visual length before the animation loops or ends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (selectedType == PhotoAnimationType.CINEMAGRAPH) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Tap on the image preview to brush the area you want to animate.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (selectedType == PhotoAnimationType.ANIMATE) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "AI will detect elements like sky, water, and hair to add natural motion.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (selectedType == PhotoAnimationType.KEN_BURNS) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "AI automatically pans and zooms focusing on detected faces and points of interest.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
