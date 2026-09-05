package com.example

import java.util.Calendar
import java.util.UUID
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.example.data.ProjectEntity
import com.example.viewmodel.ProjectViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToCreate: () -> Unit, projects: List<ProjectEntity>, onProjectClick: (String) -> Unit, projectViewModel: ProjectViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var conflictProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    var showQrDialog by remember { mutableStateOf<ProjectEntity?>(null) }
    var showImportMissingMedia by remember { mutableStateOf(false) }

    val folders by projectViewModel.foldersState.collectAsState(initial = emptyList())
    var currentFolderId by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("Last Edited") } // Name, Date Created, Last Edited, Size, Duration
    var showArchived by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("Grid") } // Grid, List
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    var showCrashRecovery by remember { mutableStateOf(false) }
    
    var showPreReviewDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                com.example.utils.ReviewManager.recordAppStart(context)
                if (com.example.utils.ReviewManager.checkOtherPositiveMoments(context)) {
                    showPreReviewDialog = true
                    com.example.utils.ReviewManager.lastSessionDurationMs = 0L
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PreReviewDialogs(
        showPreReview = showPreReviewDialog,
        onDismissPreReview = { showPreReviewDialog = false },
        showFeedback = showFeedbackDialog,
        onDismissFeedback = { showFeedbackDialog = false },
        onShowFeedback = { showFeedbackDialog = true }
    )
    
    PrivacyConsentDialog(onDismiss = {})
    
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("clipp_crash_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("has_crashed", false)) {
            showCrashRecovery = true
            prefs.edit().putBoolean("has_crashed", false).apply()
        }
    }
    
    if (showCrashRecovery) {
        AlertDialog(
            onDismissRequest = { showCrashRecovery = false },
            title = { Text("We recovered your last session") },
            text = { Text("Clipp closed unexpectedly, but we auto-saved your progress. Would you like to return to your last edited project?") },
            confirmButton = {
                TextButton(onClick = { 
                    showCrashRecovery = false
                    val lastProject = projects.maxByOrNull { it.lastEdited }
                    if (lastProject != null) {
                         onProjectClick(lastProject.id)
                    } else {
                         android.widget.Toast.makeText(context, "No recent project found.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Restore Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCrashRecovery = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    val filteredProjects = projects.filter {
        it.name.contains(searchQuery, ignoreCase = true) &&
        (showArchived || !it.isArchived) &&
        (currentFolderId == null || it.folderId == currentFolderId) &&
        (selectedTag == null || it.tags.contains(selectedTag!!, ignoreCase = true))
    }.sortedWith { p1, p2 ->
        when (sortBy) {
            "Name" -> p1.name.compareTo(p2.name)
            "Date Created" -> p2.creationDate.compareTo(p1.creationDate)
            "Size" -> p2.sizeBytes.compareTo(p1.sizeBytes)
            // Default "Last Edited"
            else -> p2.lastEdited.compareTo(p1.lastEdited)
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create")
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Project", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "My Projects",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tasks by com.example.viewmodel.BackgroundTaskManager.tasks.collectAsState()
                    val activeTasks = tasks.count { it.status == com.example.viewmodel.TaskStatus.RUNNING || it.status == com.example.viewmodel.TaskStatus.QUEUED }
                    
                    var showTaskManager by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { showTaskManager = true }) {
                        BadgedBox(
                            badge = {
                                if (activeTasks > 0) {
                                    Badge { Text("$activeTasks") }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.ListAlt, contentDescription = "Task Manager", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    
                    if (showTaskManager) {
                        ModalBottomSheet(
                            onDismissRequest = { showTaskManager = false }
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Background Tasks", style = MaterialTheme.typography.titleLarge)
                                    TextButton(onClick = { com.example.viewmodel.BackgroundTaskManager.clearCompleted() }) {
                                        Text("Clear Completed")
                                    }
                                }
                                
                                if (tasks.isEmpty()) {
                                    Text("No background tasks.", modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally))
                                } else {
                                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                        items(tasks) { task ->
                                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(task.title, fontWeight = FontWeight.Bold)
                                                        Text("Status: ${task.status.name}", style = MaterialTheme.typography.bodySmall)
                                                        if (task.message != null) Text(task.message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                                        
                                                        Spacer(Modifier.height(4.dp))
                                                        if (task.status == com.example.viewmodel.TaskStatus.RUNNING) {
                                                            LinearProgressIndicator(progress = { task.progress }, modifier = Modifier.fillMaxWidth())
                                                        }
                                                    }
                                                    
                                                    Row {
                                                        if (task.status == com.example.viewmodel.TaskStatus.RUNNING) {
                                                            IconButton(onClick = { com.example.viewmodel.BackgroundTaskManager.pauseTask(task.id) }) {
                                                                Icon(Icons.Filled.Pause, "Pause")
                                                            }
                                                        } else if (task.status == com.example.viewmodel.TaskStatus.PAUSED) {
                                                            IconButton(onClick = { com.example.viewmodel.BackgroundTaskManager.resumeTask(task.id) }) {
                                                                Icon(Icons.Filled.PlayArrow, "Resume")
                                                            }
                                                        } else if (task.status == com.example.viewmodel.TaskStatus.FAILED) {
                                                            var showErr by remember { mutableStateOf(false) }
                                                            IconButton(onClick = { showErr = true }) {
                                                                Icon(Icons.Filled.Warning, "View Error", tint = MaterialTheme.colorScheme.error)
                                                            }
                                                            if (showErr) {
                                                                com.example.OperationsErrorDialog(
                                                                    title = "Task Failed: ${task.title}",
                                                                    errorMessage = "The operation failed due to an unexpected system issue.",
                                                                    suggestedFix = "Try clearing some storage space or rebooting the app. If exporting, try a lower resolution.",
                                                                    onDismiss = { showErr = false },
                                                                    onRetry = {
                                                                        showErr = false
                                                                        com.example.viewmodel.BackgroundTaskManager.updateStatus(task.id, com.example.viewmodel.TaskStatus.QUEUED)
                                                                    },
                                                                    onReportIssue = {
                                                                        showErr = false
                                                                        android.widget.Toast.makeText(context, "Issue reported. Thank you!", android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                )
                                                            }
                                                        }
                                                        
                                                        if (task.status == com.example.viewmodel.TaskStatus.QUEUED || task.status == com.example.viewmodel.TaskStatus.RUNNING || task.status == com.example.viewmodel.TaskStatus.PAUSED) {
                                                            IconButton(onClick = { com.example.viewmodel.BackgroundTaskManager.cancelTask(task.id) }) {
                                                                Icon(Icons.Filled.Close, "Cancel")
                                                            }
                                                        }
                                                        if (task.status == com.example.viewmodel.TaskStatus.QUEUED && !task.isHighPriority) {
                                                            IconButton(onClick = { com.example.viewmodel.BackgroundTaskManager.prioritizeTask(task.id) }) {
                                                                Icon(Icons.Filled.KeyboardDoubleArrowUp, "Prioritize")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(32.dp))
                            }
                        }
                    }

                    IconButton(onClick = { showImportMissingMedia = true }) {
                        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = "Import Project", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Recent Exports
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(
                    text = "Recent Exports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) { index ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(9f/16f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.DarkGray)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp), tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search projects...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Controls: Folder Row + Sorting
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = currentFolderId == null,
                        onClick = { currentFolderId = null },
                        label = { Text("All") },
                        leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                items(folders) { folder ->
                    FilterChip(
                        selected = currentFolderId == folder.id,
                        onClick = { currentFolderId = folder.id },
                        label = { Text(folder.name) },
                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { showCreateFolderDialog = true },
                        label = { Text("New Folder") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var expandedSort by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expandedSort = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(sortBy)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                            listOf("Name", "Date Created", "Last Edited", "Size", "Duration").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { sortBy = option; expandedSort = false }
                                )
                            }
                        }
                    }

                    var expandedFilter by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expandedFilter = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedTag ?: "Filter")
                        }
                        DropdownMenu(expanded = expandedFilter, onDismissRequest = { expandedFilter = false }) {
                            DropdownMenuItem(text = { Text("All") }, onClick = { selectedTag = null; expandedFilter = false })
                            listOf("In Progress", "Published", "Client Work").forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = { selectedTag = tag; expandedFilter = false }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (showArchived) "Hide Archived" else "Show Archived") },
                                onClick = { showArchived = !showArchived; expandedFilter = false }
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = { viewMode = if (viewMode == "Grid") "List" else "Grid" }) {
                        Icon(
                            imageVector = if (viewMode == "Grid") Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayedProjects = filteredProjects

            if (displayedProjects.isEmpty() && searchQuery.isEmpty() && currentFolderId == null) {
                EmptyStateView(onCreateClick = onNavigateToCreate)
            } else if (displayedProjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No projects found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (viewMode == "Grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedProjects) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { 
                                    if (project.syncStatus == "conflict") {
                                        conflictProject = project
                                    } else {
                                        onProjectClick(project.id) 
                                    }
                                },
                                onLongPress = {
                                    selectedProject = project
                                    showBottomSheet = true
                                }
                            )
                        }
                        item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedProjects) { project ->
                            ProjectListItem(
                                project = project,
                                onClick = { 
                                    if (project.syncStatus == "conflict") {
                                        conflictProject = project
                                    } else {
                                        onProjectClick(project.id) 
                                    }
                                },
                                onLongPress = {
                                    selectedProject = project
                                    showBottomSheet = true
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
        
        if (conflictProject != null) {
            AlertDialog(
                onDismissRequest = { conflictProject = null },
                title = { Text("Sync Conflict") },
                text = { Text("This project was edited on another device. Which version would you like to keep?") },
                confirmButton = {
                    TextButton(onClick = { 
                        projectViewModel.updateProject(conflictProject!!.copy(syncStatus = "synced"))
                        conflictProject = null 
                    }) {
                        Text("Keep Local Version")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        projectViewModel.updateProject(conflictProject!!.copy(syncStatus = "synced"))
                        conflictProject = null 
                    }) {
                        Text("Keep Cloud Version")
                    }
                }
            )
        }

        if (showQrDialog != null) {
            AlertDialog(
                onDismissRequest = { showQrDialog = null },
                title = { Text("Transfer Project") },
                text = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Scan this QR code from another device to import \"${showQrDialog!!.name}\".")
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.size(200.dp).background(Color.White), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(180.dp), tint = Color.Black)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQrDialog = null }) {
                        Text("Done")
                    }
                }
            )
        }

        if (showImportMissingMedia) {
            AlertDialog(
                onDismissRequest = { showImportMissingMedia = false },
                title = { Text("Importing Project") },
                text = { 
                    Column {
                        Text("Project data imported successfully.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Missing Source Media:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("• IMG_0024.mp4\n• AudioTrack.mp3")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Please re-link these files from your local gallery to restore the project completely.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showImportMissingMedia = false 
                        android.widget.Toast.makeText(context, "Media items re-linked!", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Re-link Media")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportMissingMedia = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCreateFolderDialog) {
            var newFolderName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text("New Folder") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Folder Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = { 
                        if (newFolderName.isNotBlank()) {
                            projectViewModel.createFolder(newFolderName)
                        }
                        showCreateFolderDialog = false 
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showBottomSheet && selectedProject != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    item {
                        Text(
                            text = selectedProject!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    if (selectedProject!!.lastBackupTime > 0) {
                        item {
                            Text(
                                text = "Backed up at: ${android.text.format.DateFormat.format("MMM dd, yyyy h:mm a", java.util.Date(selectedProject!!.lastBackupTime))} (${selectedProject!!.backupSize / 1024} KB)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                    item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
                    
                    if (selectedProject?.syncStatus == "not_backed_up") {
                        item { 
                            BottomSheetItem(icon = Icons.Filled.CloudUpload, text = "Backup to Cloud") {
                                projectViewModel.syncProjectToCloud(selectedProject!!)
                                showBottomSheet = false
                            }
                        }
                    } else if (selectedProject?.syncStatus == "synced") {
                        item {
                            BottomSheetItem(icon = Icons.Filled.Warning, text = "Simulate Sync Conflict") {
                                projectViewModel.updateProject(selectedProject!!.copy(syncStatus = "conflict"))
                                showBottomSheet = false
                            }
                        }
                    }
                    
                    item {
                        var moveExpanded by remember { mutableStateOf(false) }
                        Box {
                            BottomSheetItem(icon = Icons.Filled.DriveFileMove, text = "Move to Folder") {
                                moveExpanded = true
                            }
                            DropdownMenu(expanded = moveExpanded, onDismissRequest = { moveExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = { 
                                        projectViewModel.updateProject(selectedProject!!.copy(folderId = null))
                                        showBottomSheet = false
                                        moveExpanded = false
                                    }
                                )
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = { 
                                            projectViewModel.updateProject(selectedProject!!.copy(folderId = folder.id))
                                            showBottomSheet = false
                                            moveExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        var labelExpanded by remember { mutableStateOf(false) }
                        Box {
                            BottomSheetItem(icon = Icons.Filled.Label, text = "Set Label") {
                                labelExpanded = true
                            }
                            DropdownMenu(expanded = labelExpanded, onDismissRequest = { labelExpanded = false }) {
                                listOf("In Progress" to Color.Blue, "Published" to Color.Green, "Client Work" to Color.Magenta, "None" to Color.Transparent).forEach { (tag, color) ->
                                    DropdownMenuItem(
                                        text = { Text(tag) },
                                        leadingIcon = { Icon(Icons.Filled.Circle, contentDescription = null, tint = color, modifier = Modifier.size(12.dp)) },
                                        onClick = { 
                                            projectViewModel.updateProject(selectedProject!!.copy(tags = if (tag == "None") "" else tag))
                                            showBottomSheet = false
                                            labelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        BottomSheetItem(
                            icon = if (selectedProject!!.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            text = if (selectedProject!!.isArchived) "Unarchive" else "Archive"
                        ) {
                            projectViewModel.updateProject(selectedProject!!.copy(isArchived = !selectedProject!!.isArchived))
                            showBottomSheet = false
                        }
                    }

                    item {
                        BottomSheetItem(icon = Icons.Filled.Edit, text = "Rename") {
                            showBottomSheet = false
                        }
                    }
                    item {
                        BottomSheetItem(icon = Icons.Filled.ContentCopy, text = "Duplicate") {
                            showBottomSheet = false
                        }
                    }
                    item {
                        BottomSheetItem(icon = Icons.Filled.Share, text = "Share") {
                            showBottomSheet = false
                        }
                    }
                    item {
                        BottomSheetItem(icon = Icons.Filled.QrCode, text = "Transfer Project") {
                            showQrDialog = selectedProject
                            showBottomSheet = false
                        }
                    }
                    item {
                        BottomSheetItem(icon = Icons.Filled.IosShare, text = "Export Project File (.clipp)") {
                            android.widget.Toast.makeText(context, "Exported successfully. You can now share ${selectedProject!!.name}.clipp", android.widget.Toast.LENGTH_LONG).show()
                            showBottomSheet = false
                        }
                    }
                    item {
                        BottomSheetItem(icon = Icons.Filled.Delete, text = "Delete", isDestructive = true) {
                            projectViewModel.deleteProject(selectedProject!!)
                            showBottomSheet = false
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectListItem(project: ProjectEntity, onClick: () -> Unit, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .background(Color.DarkGray)
            ) {
                if (project.thumbnailUri != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.graphics.painter.ColorPainter(Color.Gray),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Duration
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.duration,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Edited ${android.text.format.DateFormat.format("MMM dd", java.util.Date(project.lastEdited))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${project.sizeBytes / 1000000} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onLongPress) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options")
            }
        }
    }
}

@Composable
fun BottomSheetItem(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(project: ProjectEntity, onClick: () -> Unit, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                        )
                    )
            ) {
                if (project.thumbnailUri != null) {
                    coil.compose.AsyncImage(
                        model = project.thumbnailUri,
                        contentDescription = "Project Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.VideoFile,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }

                if (project.syncStatus == "synced") {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = "Synced",
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else if (project.syncStatus == "syncing") {
                    Icon(
                        imageVector = Icons.Filled.CloudSync,
                        contentDescription = "Syncing",
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                } else if (project.syncStatus == "conflict") {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Conflict",
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.duration,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            // Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Edited ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(project.lastEdited))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (project.progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { project.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    )
                }
                
                if (project.projectState == "Draft") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continue Editing", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(onCreateClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(y = floatOffset.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MovieCreation,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No projects yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start your creative journey by creating your first video project.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text("Create Your First Video", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
