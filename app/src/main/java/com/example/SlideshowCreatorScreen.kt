package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.ProjectEntity
import com.example.viewmodel.ProjectViewModel

private val slideshowStyles = listOf("Simple", "Cinematic", "Travel")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideshowCreatorScreen(
    onClose: () -> Unit,
    onGoToEditor: (String) -> Unit,
    projectViewModel: ProjectViewModel = viewModel()
) {
    var selectedPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedStyle by remember { mutableStateOf(slideshowStyles.first()) }

    if (selectedPhotos.isEmpty()) {
        MediaPickerScreen(
            onClose = onClose,
            onGoToEditor = { },
            projectViewModel = projectViewModel,
            isSelectingForExisting = true,
            onMediaSelected = { selectedPhotos = it.filter { uri -> uri.isNotBlank() } }
        )
        return
    }

    val durationMs = selectedPhotos.size * PHOTO_DEFAULT_DURATION_MS
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create slideshow") },
                navigationIcon = {
                    IconButton(onClick = { selectedPhotos = emptyList() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val project = ProjectEntity(
                        name = "$selectedStyle Slideshow",
                        duration = formatDuration(durationMs),
                        lastEdited = System.currentTimeMillis(),
                        sourceMediaPaths = selectedPhotos
                    )
                    projectViewModel.addProject(project)
                    onGoToEditor(project.id)
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create project")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("${selectedPhotos.size} photos · ${formatExportDuration(durationMs)}", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedPhotos) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Each photo is placed on the timeline for five seconds. This step creates a real local project; it does not claim to render transitions or music.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text("Project label", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(slideshowStyles) { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = { selectedStyle = style },
                        label = { Text(style) }
                    )
                }
            }
        }
    }
}
