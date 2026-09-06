package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITextToSpeechPanel(
    onAddAudio: (uri: String, title: String, durationMs: Long) -> Unit,
    onClose: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    val voices = listOf(
        "Male (Deep)", "Male (Natural)", 
        "Female (Warm)", "Female (Clear)", 
        "Child", "Professional Narrator",
        "Indian English (Male)", "Indian English (Female)",
        "Hindi (Male)", "Hindi (Female)"
    )
    var selectedVoice by remember { mutableStateOf(voices.first()) }
    var expanded by remember { mutableStateOf(false) }
    
    var speechRate by remember { mutableFloatStateOf(1f) }
    var pitch by remember { mutableFloatStateOf(1f) }
    
    var isGenerating by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    
    val scope = rememberCoroutineScope()
    
    // Using a sample MP3 for mock
    val DUMMY_AUDIO_URI = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp),
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
                Text("AI Text-to-Speech", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }

            if (isGenerating) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(progress = progress)
                    Text("Generating speech... ${(progress * 100).toInt()}%")
                }
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = { Text("Enter text to narrate. Use '...' for pauses.") },
                    maxLines = 10
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedVoice,
                        onValueChange = { },
                        label = { Text("Voice") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        voices.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(text = selectionOption) },
                                onClick = {
                                    selectedVoice = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Speech Rate", style = MaterialTheme.typography.labelMedium)
                        Text("${"%.1f".format(speechRate)}x", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = { speechRate = it },
                        valueRange = 0.5f..2f
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pitch", style = MaterialTheme.typography.labelMedium)
                        Text("${"%.1f".format(pitch)}x", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = pitch,
                        onValueChange = { pitch = it },
                        valueRange = 0.5f..2f
                    )
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            progress = 0f
                            for (i in 0..100 step 10) {
                                progress = i / 100f
                                delay(100)
                            }
                            isGenerating = false
                            
                            val words = text.split(" ").size
                            val estimatedDurationMs = (words * 300L / speechRate).toLong().coerceAtLeast(3000L)
                            
                            onAddAudio(DUMMY_AUDIO_URI, "AI: $selectedVoice", estimatedDurationMs)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = text.isNotBlank()
                ) {
                    Text("Generate & Add to Timeline")
                }
            }
        }
    }
}
