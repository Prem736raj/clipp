package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AIMusicPanel(
    videoDurationMs: Long,
    onAddMusic: (uri: String, title: String, durationMs: Long) -> Unit,
    onClose: () -> Unit
) {
    var selectedGenre by remember { mutableStateOf("Happy") }
    val genres = listOf("Happy", "Sad", "Energetic", "Calm", "Cinematic", "Electronic", "Hip-Hop", "Acoustic", "Ambient", "Dramatic", "Inspirational")
    var tempo by remember { mutableFloatStateOf(120f) }
    var matchVideo by remember { mutableStateOf(false) }
    
    var isGenerating by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var generatedVariations by remember { mutableStateOf<List<AIMusicVariation>>(emptyList()) }
    
    val scope = rememberCoroutineScope()
    
    // Using a sample MP3 for mock
    val DUMMY_AUDIO_URI = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"

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
                Text("AI Music Generation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }

            if (generatedVariations.isEmpty() && !isGenerating) {
                Text("Genre / Mood", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    genres.forEach { genre ->
                        FilterChip(
                            selected = genre == selectedGenre,
                            onClick = { selectedGenre = genre },
                            label = { Text(genre) }
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text("Tempo / BPM: ${tempo.toInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = tempo,
                    onValueChange = { tempo = it },
                    valueRange = 60f..200f
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = matchVideo, onCheckedChange = { matchVideo = it })
                    Text("Match Video (Sync to cuts and pacing)", style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            progress = 0f
                            for (i in 0..100 step 5) {
                                progress = i / 100f
                                delay(100)
                            }
                            isGenerating = false
                            generatedVariations = listOf(
                                AIMusicVariation("Variation 1 ($selectedGenre)", DUMMY_AUDIO_URI, videoDurationMs.coerceAtLeast(10000L).coerceAtMost(600000L)),
                                AIMusicVariation("Variation 2 ($selectedGenre, alternative)", DUMMY_AUDIO_URI, videoDurationMs.coerceAtLeast(10000L).coerceAtMost(600000L)),
                                AIMusicVariation("Variation 3 ($selectedGenre, upbeat)", DUMMY_AUDIO_URI, videoDurationMs.coerceAtLeast(10000L).coerceAtMost(600000L))
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Music")
                }
            } else if (isGenerating) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(progress = progress)
                    Text("Composing $selectedGenre music at ${tempo.toInt()} BPM... ${(progress * 100).toInt()}%")
                    if (matchVideo) {
                        Text("Analyzing video pacing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text("Generated Variations", style = MaterialTheme.typography.titleSmall)
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(generatedVariations) { variation ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                onAddMusic(variation.uri, variation.title, variation.durationMs)
                                onClose()
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Column {
                                        Text(variation.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Royalty-free • ${variation.durationMs / 1000}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { /* Preview mock */ }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Preview")
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { generatedVariations = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate Again")
                }
            }
        }
    }
}

data class AIMusicVariation(
    val title: String,
    val uri: String,
    val durationMs: Long
)
