package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateReplacementScreen(
    templateId: String,
    onBack: () -> Unit,
    onCustomize: (VideoTemplate, List<String>, List<String>) -> Unit,
    onQuickExport: (VideoTemplate, List<String>, List<String>) -> Unit
) {
    val template = TemplateRepo.templates.find { it.id == templateId }
    if (template == null) {
        onBack()
        return
    }

    var selectedMedias by remember { mutableStateOf(MutableList<String?>(template.slots) { null }) }
    var editedTexts by remember { mutableStateOf(template.textPlaceholders.toMutableList()) }

    var showingMediaPickerForSlot by remember { mutableStateOf<Int?>(null) }
    
    var showExportDialog by remember { mutableStateOf(false) }

    if (showingMediaPickerForSlot != null) {
        MediaPickerScreen(
            onClose = { showingMediaPickerForSlot = null },
            onGoToEditor = { },
            isSelectingForExisting = true,
            onMediaSelected = { uris ->
                if (uris.isNotEmpty()) {
                    val newMedias = selectedMedias.toMutableList()
                    newMedias[showingMediaPickerForSlot!!] = uris.first()
                    selectedMedias = newMedias
                }
                showingMediaPickerForSlot = null
            }
        )
        return
    }

    var showShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(template.title, style = MaterialTheme.typography.titleMedium, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (template.isCustom) {
                        IconButton(onClick = { showShareDialog = true }) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Share, contentDescription = "Share Template")
                        }
                    }
                    TextButton(onClick = { 
                        onQuickExport(template, selectedMedias.filterNotNull(), editedTexts)
                        showExportDialog = true
                    }) {
                        Text("Quick Export")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filledSlots = selectedMedias.count { it != null }
                    Text(
                        text = "$filledSlots / ${template.slots} Media Added",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Button(
                        onClick = { 
                            onCustomize(template, selectedMedias.filterNotNull(), editedTexts)
                        },
                        enabled = filledSlots > 0 // Or disable checking full
                    ) {
                        Text("Customize (Editor)")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Show either original preview or selected user content
                    val firstMedia = selectedMedias.firstOrNull { it != null }
                    if (firstMedia != null) {
                        AsyncImage(
                            model = firstMedia,
                            contentDescription = "Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clickable { /* mock play preview */ }
                        )
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                        }
                    } else {
                        AsyncImage(
                            model = template.imageUrl,
                            contentDescription = "Template Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Replace Media",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(selectedMedias) { index, mediaUri ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { showingMediaPickerForSlot = index },
                        contentAlignment = Alignment.Center
                    ) {
                        if (mediaUri != null) {
                            AsyncImage(
                                model = mediaUri,
                                contentDescription = "Slot $index",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Slot ${index + 1}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(if (mediaUri == null) "Select media" else "Added", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    TextButton(onClick = { showingMediaPickerForSlot = index }) {
                        Text(if (mediaUri == null) "Add" else "Replace")
                    }
                }
            }
            
            if (template.textPlaceholders.isNotEmpty()) {
                item {
                    Text(
                        text = "Edit Text",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                itemsIndexed(editedTexts) { index, text ->
                    OutlinedTextField(
                        value = text,
                        onValueChange = { newT -> 
                            val mut = editedTexts.toMutableList()
                            mut[index] = newT
                            editedTexts = mut
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Text ${index + 1}") },
                        trailingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                    )
                }
            }
        }
        
        if (showExportDialog) {
            var exportProgress by remember { mutableStateOf(0f) }
            
            LaunchedEffect(Unit) {
                while(exportProgress < 1f) {
                    delay(100)
                    exportProgress += 0.05f
                }
                showExportDialog = false    
            }
            
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Quick Exporting...") },
                text = {
                    Column {
                        Text("Applying template ${template.title} and exporting video...")
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { exportProgress }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    if (exportProgress >= 1f) {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("Done")
                        }
                    }
                }
            )
        }
        
        if (showShareDialog) {
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("Share Template") },
                text = { 
                    Text("Generating a shareable template file for ${template.title}. Anyone with Clipp can import this template.")
                },
                confirmButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Share")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
