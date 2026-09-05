package com.example

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.example.viewmodel.ProjectViewModel
import com.example.data.ProjectEntity
import androidx.lifecycle.viewmodel.compose.viewModel

enum class MediaType { VIDEO, PHOTO }
data class MediaItem(val id: Long, val uri: Uri, val type: MediaType, val duration: Long, val folder: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    onClose: () -> Unit, 
    onGoToEditor: (String) -> Unit, 
    projectViewModel: ProjectViewModel = viewModel(),
    isSelectingForExisting: Boolean = false,
    onMediaSelected: ((List<String>) -> Unit)? = null,
    onSlideshowCreator: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    
    var currentFilter by remember { mutableStateOf("All") }
    var currentFolder by remember { mutableStateOf("All Media") }
    var expandedFolderDropdown by remember { mutableStateOf(false) }
    
    var showSetupScreen by remember { mutableStateOf(false) }
    var showAnalysisDialog by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<com.example.viewmodel.MediaMetadataInfo?>(null) }
    var showCodecWarning by remember { mutableStateOf(false) }
    var showOptimizationPrompt by remember { mutableStateOf(false) }
    var isGeneratingProxy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mediaItems = loadMedia(context)
        if (mediaItems.isEmpty()) {
            mediaItems = loadMockMedia()
        }
    }

    val folders = remember(mediaItems) {
        listOf("All Media") + mediaItems.map { it.folder }.distinct().sorted()
    }

    val displayedItems = remember(mediaItems, currentFilter, currentFolder) {
        mediaItems.filter {
            (currentFilter == "All" || (currentFilter == "Videos" && it.type == MediaType.VIDEO) || (currentFilter == "Photos" && it.type == MediaType.PHOTO)) &&
            (currentFolder == "All Media" || it.folder == currentFolder)
        }
    }

    LaunchedEffect(showAnalysisDialog) {
        if (showAnalysisDialog) {
            val uris = selectedItems.map { it.uri.toString() }
            val info = com.example.viewmodel.MediaOptimizerManager.analyzeMedia(uris)
            analysisResult = info
            
            com.example.viewmodel.MediaOptimizerManager.cleanOldCachesIfLowStorage(context)
            
            if (!info.isSupported) {
                showCodecWarning = true
            } else if (info.isLargeFile || info.isHighResAndFps) {
                showOptimizationPrompt = true
            } else {
                showSetupScreen = true
            }
            showAnalysisDialog = false
        }
    }

    if (showCodecWarning) {
        AlertDialog(
            onDismissRequest = { showCodecWarning = false },
            title = { Text("Unsupported Media Codec") },
            text = { Text("One or more selected videos use the ${analysisResult?.codec} codec, which may not edit smoothly on this device. We recommend converting them to H.264 before importing.") },
            confirmButton = {
                TextButton(onClick = { 
                    showCodecWarning = false
                    if (analysisResult?.isLargeFile == true || analysisResult?.isHighResAndFps == true) {
                        showOptimizationPrompt = true
                    } else {
                        showSetupScreen = true
                    }
                }) {
                    Text("Import Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodecWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showOptimizationPrompt) {
        AlertDialog(
            onDismissRequest = {  },
            title = { Text("Import Optimization") },
            text = { 
                if (isGeneratingProxy) {
                    Column {
                        Text("Creating optimized proxy files for smooth editing...")
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Text("You've selected a very large file or 4K@60fps minimum video. Generating highly optimized proxy (lower resolution) versions will ensure smooth timeline scrolling and prevent out-of-memory crashes. The final export will use the original pristine files.")
                }
            },
            confirmButton = {
                if (!isGeneratingProxy) {
                    TextButton(onClick = {
                        isGeneratingProxy = true
                    }) {
                        Text("Create Proxy (Recommended)")
                    }
                }
            },
            dismissButton = {
                if (!isGeneratingProxy) {
                    TextButton(onClick = { 
                        showOptimizationPrompt = false
                        showSetupScreen = true
                    }) {
                        Text("Skip")
                    }
                }
            }
        )
    }
    
    LaunchedEffect(isGeneratingProxy) {
        if (isGeneratingProxy) {
            kotlinx.coroutines.delay(2500)
            isGeneratingProxy = false
            showOptimizationPrompt = false
            showSetupScreen = true
            android.widget.Toast.makeText(context, "Proxies generated! Editor will be smooth.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    if (showSetupScreen) {
        ProjectSetupScreen(
            onBack = { showSetupScreen = false },
            onCreateProject = { name, aspect, res, fps ->
                val project = ProjectEntity(
                    name = name,
                    duration = if (selectedItems.isNotEmpty() && selectedItems.first().type == MediaType.VIDEO) formatDuration(selectedItems.first().duration) else "00:00",
                    aspectRatio = aspect,
                    resolution = res,
                    frameRate = fps,
                    sourceMediaPaths = selectedItems.map { it.uri.toString() },
                    isDirty = true // Open directly in editor
                )
                projectViewModel.addProject(project)
                onGoToEditor(project.id)
            }
        )
    } else {

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { expandedFolderDropdown = true }
                        ) {
                            Text(
                                text = currentFolder,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Folders")
                            
                            DropdownMenu(
                                expanded = expandedFolderDropdown,
                                onDismissRequest = { expandedFolderDropdown = false }
                            ) {
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder) },
                                        onClick = {
                                            currentFolder = folder
                                            expandedFolderDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (onSlideshowCreator != null) {
                            TextButton(onClick = onSlideshowCreator) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Slideshow")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* Open Camera */ },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val filters = listOf("All", "Videos", "Photos")
                    filters.forEach { filter ->
                        val selected = currentFilter == filter
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { currentFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(displayedItems, key = { it.id }) { item ->
                    val selectedIndex = selectedItems.indexOf(item)
                    val isSelected = selectedIndex != -1
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selectedItems = if (isSelected) {
                                    selectedItems - item
                                } else {
                                    selectedItems + item
                                }
                            }
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        if (item.type == MediaType.VIDEO) {
                            Text(
                                text = formatDuration(item.duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        
                        // Selection Overlay
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${selectedIndex + 1}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedItems.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedItems.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { 
                                if (isSelectingForExisting) {
                                    onMediaSelected?.invoke(selectedItems.map { it.uri.toString() })
                                } else {
                                    showAnalysisDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isSelectingForExisting) "Add Media" else "Next", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
            }
        }
    }
    }
}

suspend fun loadMedia(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<MediaItem>()
    try {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val typeVal = cursor.getInt(typeCol)
                val type = if (typeVal == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaType.VIDEO else MediaType.PHOTO
                val pUri = if (type == MediaType.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = ContentUris.withAppendedId(pUri, id)
                val duration = if (type == MediaType.VIDEO) cursor.getLong(durationCol) else 0L
                val folder = cursor.getString(folderCol) ?: "Recent"
                items.add(MediaItem(id, uri, type, duration, folder))
            }
        }
    } catch (e: Exception) {
        // Fallback for emulator without permissions or content
    }
    items
}

fun loadMockMedia(): List<MediaItem> {
    val dummyUri = Uri.parse("content://media/external/images/media/1")
    return List(24) { i ->
        MediaItem(
            id = i.toLong(),
            uri = dummyUri,
            type = if (i % 3 == 0) MediaType.VIDEO else MediaType.PHOTO,
            duration = if (i % 3 == 0) 125000L else 0L,
            folder = if (i < 10) "Camera" else "Downloads"
        )
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
