package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

val slideshowStyles = listOf(
    "Modern Minimal", "Cinematic", "Fun & Colorful", "Romantic", "Travel Adventure"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideshowCreatorScreen(
    onClose: () -> Unit,
    onGoToEditor: (String) -> Unit,
    projectViewModel: com.example.viewmodel.ProjectViewModel
) {
    var step by remember { mutableStateOf("PICK_MEDIA") }
    var selectedPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedStyle by remember { mutableStateOf(slideshowStyles[0]) }
    var includeTitle by remember { mutableStateOf(true) }
    var includeEnding by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var generateProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            while (generateProgress < 1f) {
                delay(50)
                generateProgress += 0.02f
            }
            // Generate Project
            val projectId = java.util.UUID.randomUUID().toString()
            val newProject = com.example.data.ProjectEntity(
                id = projectId,
                name = "$selectedStyle Slideshow",
                duration = "00:${selectedPhotos.size * 3}", // mock duration
                lastEdited = System.currentTimeMillis(),
                sourceMediaPaths = selectedPhotos
            )
            projectViewModel.addProject(newProject)
            onGoToEditor(projectId)
        }
    }

    if (step == "PICK_MEDIA") {
        MediaPickerScreen(
            onClose = onClose,
            onGoToEditor = { },
            isSelectingForExisting = true,
            onMediaSelected = { uris ->
                selectedPhotos = uris
                step = "CONFIGURE"
            }
        )
    } else if (step == "CONFIGURE") {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Create Slideshow") },
                    navigationIcon = {
                        IconButton(onClick = { step = "PICK_MEDIA" }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = { isGenerating = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGenerating && selectedPhotos.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Slideshow")
                        }
                    }
                }
            }
        ) { padding ->
            if (isGenerating) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Creating $selectedStyle Slideshow...", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { generateProgress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Text("Applying Ken Burns motion effects...", style = MaterialTheme.typography.bodySmall)
                    Text("Syncing photo duration to music tempo...", style = MaterialTheme.typography.bodySmall)
                    Text("Adding transitions...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text("${selectedPhotos.size} Photos Selected", style = MaterialTheme.typography.titleMedium)
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedPhotos) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Music Synchronization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Photos will automatically adjust their display duration to match the tempo of the selected music track.", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Column {
                        Text("Slideshow Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(slideshowStyles) { style ->
                                val isSelected = selectedStyle == style
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedStyle = style },
                                    label = { Text(style) }
                                )
                            }
                        }
                        Text("Style determines transition types and color grading.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }

                    Column {
                        Text("Add Text Slides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Include Title Slide", modifier = Modifier.weight(1f))
                            Switch(checked = includeTitle, onCheckedChange = { includeTitle = it })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Include Ending Slide", modifier = Modifier.weight(1f))
                            Switch(checked = includeEnding, onCheckedChange = { includeEnding = it })
                        }
                    }
                }
            }
        }
    }
}
