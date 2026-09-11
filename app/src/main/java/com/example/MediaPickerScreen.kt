package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ProjectEntity
import com.example.viewmodel.ProjectViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class MediaType { VIDEO, PHOTO }

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val duration: Long,
    val folder: String
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    onClose: () -> Unit,
    onGoToEditor: (String) -> Unit,
    projectViewModel: ProjectViewModel,
    isSelectingForExisting: Boolean = false,
    onMediaSelected: ((List<String>) -> Unit)? = null,
    onSlideshowCreator: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentFilter by remember { mutableStateOf("All") }
    var currentFolder by remember { mutableStateOf("All Media") }
    var expandedFolderDropdown by remember { mutableStateOf(false) }
    var showSetupScreen by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        scope.launch {
            isLoading = true
            errorMessage = null
            selectedItems = emptyList()
            val resolvedItems = uris.mapNotNull { uri ->
                persistReadPermission(context, uri)
                MediaMetadataReader.read(context, uri)?.let { metadata ->
                    MediaItem(
                        id = uri.toString().hashCode().toLong(),
                        uri = uri,
                        type = if (metadata.mimeType.startsWith("video/")) MediaType.VIDEO else MediaType.PHOTO,
                        duration = metadata.durationMs,
                        folder = "Selected media"
                    )
                }
            }
            mediaItems = resolvedItems
            if (uris.isNotEmpty() && resolvedItems.isEmpty()) {
                errorMessage = "The selected files could not be opened. Choose a supported video or image again."
            } else if (uris.size != resolvedItems.size) {
                errorMessage = "Some selected files could not be opened and were removed."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        mediaPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    if (showSetupScreen) {
        ProjectSetupScreen(
            onBack = { showSetupScreen = false },
            onCreateProject = { name, aspect, res, fps ->
                val durationMs = selectedItems.sumOf {
                    if (it.type == MediaType.VIDEO) it.duration else PHOTO_DEFAULT_DURATION_MS
                }
                val project = ProjectEntity(
                    name = name,
                    duration = formatDuration(durationMs),
                    aspectRatio = aspect,
                    resolution = res,
                    frameRate = fps,
                    sourceMediaPaths = selectedItems.map { it.uri.toString() },
                    isDirty = false
                )
                projectViewModel.addProject(project)
                onGoToEditor(project.id)
            }
        )
    } else {
        val folders = remember(mediaItems) {
            listOf("All Media") + mediaItems.map { it.folder }.distinct().sorted()
        }
        val displayedItems = remember(mediaItems, currentFilter, currentFolder) {
            mediaItems.filter {
                (currentFilter == "All" ||
                    (currentFilter == "Videos" && it.type == MediaType.VIDEO) ||
                    (currentFilter == "Photos" && it.type == MediaType.PHOTO)) &&
                    (currentFolder == "All Media" || it.folder == currentFolder)
            }
        }

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
                            TextButton(
                                onClick = {
                                    mediaPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                            ) {
                                Text("Choose media")
                            }
                            if (onSlideshowCreator != null) {
                                IconButton(onClick = onSlideshowCreator) {
                                    Icon(Icons.Filled.MovieCreation, contentDescription = "Create slideshow")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Videos", "Photos").forEach { filter ->
                            val selected = currentFilter == filter
                            Surface(
                                shape = CircleShape,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                onClick = { currentFilter = filter }
                            ) {
                                Text(
                                    text = filter,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
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
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (displayedItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            if (errorMessage == null) "No media selected" else errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                mediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                        ) {
                            Text("Choose videos or photos")
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
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
                                    contentDescription = if (item.type == MediaType.VIDEO) "Video" else "Photo",
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
                                            .background(
                                                Color.Black.copy(alpha = 0.6f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    )
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
                                }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .navigationBarsPadding(),
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
                                        showSetupScreen = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    if (isSelectingForExisting) "Add Media" else "Next",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
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

const val PHOTO_DEFAULT_DURATION_MS = 5_000L

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
