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

import com.example.data.FolderEntity
import com.example.data.ProjectEntity
import com.example.data.ProjectStorage
import com.example.viewmodel.ProjectViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToCreate: () -> Unit, projects: List<ProjectEntity>, onProjectClick: (String) -> Unit, projectViewModel: ProjectViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val libraryPrefs = remember {
        context.getSharedPreferences("clipp_library_prefs", android.content.Context.MODE_PRIVATE)
    }
    val actualProjectSizes = remember(projects) {
        projects.associate { project ->
            project.id to ProjectStorage.calculateOwnedSizeBytes(context, project)
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var projectPendingDeletion by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectPendingRename by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectPendingTagEdit by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectDetails by remember { mutableStateOf<ProjectEntity?>(null) }

    val folders by projectViewModel.foldersState.collectAsState(initial = emptyList())
    var currentFolderId by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(ProjectSortOption.LAST_EDITED) }
    var sortDescending by remember { mutableStateOf(true) }
    var archiveFilter by remember { mutableStateOf(ProjectArchiveFilter.ACTIVE) }
    var stateFilter by remember { mutableStateOf(ProjectStateFilter.ALL) }
    var editedRecentlyOnly by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(libraryPrefs.getString("view_mode", "Grid") ?: "Grid") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showManageFoldersSheet by remember { mutableStateOf(false) }
    var folderPendingRename by remember { mutableStateOf<FolderEntity?>(null) }
    var folderPendingDeletion by remember { mutableStateOf<FolderEntity?>(null) }
    var selectedProjectIds by remember { mutableStateOf(emptySet<String>()) }
    var batchDeletePending by remember { mutableStateOf(false) }
    var showCrashRecovery by remember { mutableStateOf(false) }

    val allTags = remember(projects) {
        projects.flatMap { projectTags(it.tags) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val selectedProjects = projects.filter { it.id in selectedProjectIds }
    val selectionMode = selectedProjectIds.isNotEmpty()


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
            text = { Text("Clipp closed unexpectedly. The last saved project state may still be available. Would you like to open your most recently edited project?") },
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

    projectPendingDeletion?.let { projectToDelete ->
        AlertDialog(
            onDismissRequest = { projectPendingDeletion = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete project?") },
            text = {
                Text("Delete \"${projectToDelete.name}\" and its Clipp-owned project files? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        projectViewModel.deleteProject(projectToDelete)
                        if (selectedProject?.id == projectToDelete.id) selectedProject = null
                        projectPendingDeletion = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingDeletion = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    projectPendingRename?.let { projectToRename ->
        var name by remember(projectToRename.id) { mutableStateOf(projectToRename.name) }
        AlertDialog(
            onDismissRequest = { projectPendingRename = null },
            title = { Text("Rename project") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.trim().isNotEmpty() && name.trim() != projectToRename.name,
                    onClick = {
                        projectViewModel.updateProject(
                            projectToRename.copy(name = name.trim(), lastEdited = System.currentTimeMillis())
                        )
                        projectPendingRename = null
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingRename = null }) { Text("Cancel") }
            }
        )
    }

    projectPendingTagEdit?.let { projectToEdit ->
        var tagsText by remember(projectToEdit.id) { mutableStateOf(projectToEdit.tags) }
        AlertDialog(
            onDismissRequest = { projectPendingTagEdit = null },
            title = { Text("Edit tags") },
            text = {
                Column {
                    Text(
                        "Use commas to separate tags. Tags become searchable and available as filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("Tags") },
                        placeholder = { Text("Client Work, Urgent") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    projectViewModel.updateProject(
                        projectToEdit.copy(tags = normalizeTags(tagsText), lastEdited = System.currentTimeMillis())
                    )
                    projectPendingTagEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingTagEdit = null }) { Text("Cancel") }
            }
        )
    }

    projectDetails?.let { project ->
        val folderName = folders.firstOrNull { it.id == project.folderId }?.name ?: "None"
        val size = actualProjectSizes[project.id] ?: project.sizeBytes
        AlertDialog(
            onDismissRequest = { projectDetails = null },
            title = { Text(project.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Status", project.projectState)
                    DetailRow("Duration", project.duration)
                    DetailRow("Format", "${project.aspectRatio} · ${project.resolution} · ${project.frameRate}")
                    DetailRow("Source media", project.sourceMediaPaths.size.toString())
                    DetailRow("Folder", folderName)
                    DetailRow("Tags", project.tags.ifBlank { "None" })
                    DetailRow("Storage", formatBytes(size))
                    DetailRow(
                        "Created",
                        java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                            .format(java.util.Date(project.creationDate))
                    )
                    DetailRow(
                        "Last edited",
                        java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                            .format(java.util.Date(project.lastEdited))
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { projectDetails = null }) { Text("Done") }
            }
        )
    }

    if (batchDeletePending) {
        AlertDialog(
            onDismissRequest = { batchDeletePending = false },
            icon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
            title = { Text("Delete ${selectedProjects.size} projects?") },
            text = { Text("Their Clipp-owned project files will also be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedProjects.forEach(projectViewModel::deleteProject)
                    selectedProjectIds = emptySet()
                    batchDeletePending = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { batchDeletePending = false }) { Text("Cancel") }
            }
        )
    }

    folderPendingRename?.let { folder ->
        var folderName by remember(folder.id) { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { folderPendingRename = null },
            title = { Text("Rename folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = folderName.trim().isNotEmpty() && folderName.trim() != folder.name,
                    onClick = {
                        projectViewModel.renameFolder(folder, folderName)
                        folderPendingRename = null
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { folderPendingRename = null }) { Text("Cancel") }
            }
        )
    }

    folderPendingDeletion?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderPendingDeletion = null },
            icon = { Icon(Icons.Filled.FolderDelete, contentDescription = null) },
            title = { Text("Delete folder?") },
            text = { Text("Projects in \"${folder.name}\" will stay in Clipp and move back to All projects.") },
            confirmButton = {
                TextButton(onClick = {
                    projectViewModel.deleteFolder(folder)
                    if (currentFolderId == folder.id) currentFolderId = null
                    folderPendingDeletion = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderPendingDeletion = null }) { Text("Cancel") }
            }
        )
    }

    val filteredProjects = filterAndSortProjects(
        projects = projects,
        filter = ProjectLibraryFilter(
            query = searchQuery,
            folderId = currentFolderId,
            tag = selectedTag,
            archiveFilter = archiveFilter,
            stateFilter = stateFilter,
            editedRecentlyOnly = editedRecentlyOnly,
            sortOption = sortOption,
            descending = sortDescending
        ),
        actualSizes = actualProjectSizes
    )
    val activeFilterCount = listOf(
        selectedTag != null,
        archiveFilter != ProjectArchiveFilter.ACTIVE,
        stateFilter != ProjectStateFilter.ALL,
        editedRecentlyOnly
    ).count { it }

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
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
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
                if (folders.isNotEmpty()) {
                    item {
                        AssistChip(
                            onClick = { showManageFoldersSheet = true },
                            label = { Text("Manage") },
                            leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
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
                            Text(sortOption.label)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                            ProjectSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = {
                                        if (sortOption == option) Icon(Icons.Filled.Check, contentDescription = null)
                                    },
                                    onClick = { sortOption = option; expandedSort = false }
                                )
                            }
                        }
                    }

                    IconButton(onClick = { sortDescending = !sortDescending }) {
                        Icon(
                            imageVector = if (sortDescending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                            contentDescription = if (sortDescending) "Sort descending" else "Sort ascending"
                        )
                    }

                    var expandedFilter by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expandedFilter = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (activeFilterCount == 0) "Filter" else "Filter ($activeFilterCount)")
                        }
                        DropdownMenu(expanded = expandedFilter, onDismissRequest = { expandedFilter = false }) {
                            Text("Visibility", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            ProjectArchiveFilter.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = { if (archiveFilter == option) Icon(Icons.Filled.Check, contentDescription = null) },
                                    onClick = { archiveFilter = option; expandedFilter = false }
                                )
                            }
                            HorizontalDivider()
                            Text("Status", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            ProjectStateFilter.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = { if (stateFilter == option) Icon(Icons.Filled.Check, contentDescription = null) },
                                    onClick = { stateFilter = option; expandedFilter = false }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Edited in last 7 days") },
                                leadingIcon = { if (editedRecentlyOnly) Icon(Icons.Filled.Check, contentDescription = null) },
                                onClick = { editedRecentlyOnly = !editedRecentlyOnly; expandedFilter = false }
                            )
                            if (allTags.isNotEmpty()) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Any tag") },
                                    leadingIcon = { if (selectedTag == null) Icon(Icons.Filled.Check, contentDescription = null) },
                                    onClick = { selectedTag = null; expandedFilter = false }
                                )
                                allTags.forEach { tag ->
                                    DropdownMenuItem(
                                        text = { Text("Tag: $tag") },
                                        leadingIcon = { if (selectedTag.equals(tag, ignoreCase = true)) Icon(Icons.Filled.Check, contentDescription = null) },
                                        onClick = { selectedTag = tag; expandedFilter = false }
                                    )
                                }
                            }
                        }
                    }
                }
                Row {
                    IconButton(onClick = {
                        viewMode = if (viewMode == "Grid") "List" else "Grid"
                        libraryPrefs.edit().putString("view_mode", viewMode).apply()
                    }) {
                        Icon(
                            imageVector = if (viewMode == "Grid") Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                }
            }

            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedProjectIds = emptySet() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit selection")
                    }
                    Text("${selectedProjects.size} selected", fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { selectedProjectIds = filteredProjects.map { it.id }.toSet() }) {
                        Text("Select all")
                    }
                    IconButton(onClick = {
                        val shouldUnarchive = selectedProjects.all { it.isArchived }
                        projectViewModel.updateProjects(
                            selectedProjects.map { it.copy(isArchived = !shouldUnarchive, lastEdited = System.currentTimeMillis()) }
                        )
                    }) {
                        Icon(
                            if (selectedProjects.all { it.isArchived }) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            contentDescription = "Archive selection"
                        )
                    }
                    IconButton(onClick = {
                        val markDraft = selectedProjects.all { it.projectState.equals("Completed", ignoreCase = true) }
                        projectViewModel.updateProjects(
                            selectedProjects.map {
                                it.copy(projectState = if (markDraft) "Draft" else "Completed", lastEdited = System.currentTimeMillis())
                            }
                        )
                    }) {
                        Icon(Icons.Filled.TaskAlt, contentDescription = "Change project status")
                    }
                    var batchMoveExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { batchMoveExpanded = true }) {
                            Icon(Icons.Filled.DriveFileMove, contentDescription = "Move selection")
                        }
                        DropdownMenu(expanded = batchMoveExpanded, onDismissRequest = { batchMoveExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("No folder") },
                                onClick = {
                                    projectViewModel.updateProjects(selectedProjects.map { it.copy(folderId = null, lastEdited = System.currentTimeMillis()) })
                                    batchMoveExpanded = false
                                }
                            )
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        projectViewModel.updateProjects(selectedProjects.map { it.copy(folderId = folder.id, lastEdited = System.currentTimeMillis()) })
                                        batchMoveExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    var batchTagExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { batchTagExpanded = true }) {
                            Icon(Icons.Filled.Label, contentDescription = "Tag selection")
                        }
                        DropdownMenu(expanded = batchTagExpanded, onDismissRequest = { batchTagExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Clear tags") },
                                onClick = {
                                    projectViewModel.updateProjects(selectedProjects.map { it.copy(tags = "", lastEdited = System.currentTimeMillis()) })
                                    batchTagExpanded = false
                                }
                            )
                            allTags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        projectViewModel.updateProjects(selectedProjects.map { it.copy(tags = tag, lastEdited = System.currentTimeMillis()) })
                                        batchTagExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { batchDeletePending = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Delete selection", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayedProjects = filteredProjects

            if (projects.isEmpty()) {
                EmptyStateView(onCreateClick = onNavigateToCreate)
            } else if (displayedProjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No projects match these filters", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                selected = project.id in selectedProjectIds,
                                selectionMode = selectionMode,
                                onClick = { onProjectClick(project.id) },
                                onSelectionToggle = {
                                    selectedProjectIds = selectedProjectIds.toggle(project.id)
                                },
                                onMore = {
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
                                project = project.copy(sizeBytes = actualProjectSizes[project.id] ?: project.sizeBytes),
                                selected = project.id in selectedProjectIds,
                                selectionMode = selectionMode,
                                onClick = { onProjectClick(project.id) },
                                onSelectionToggle = {
                                    selectedProjectIds = selectedProjectIds.toggle(project.id)
                                },
                                onMore = {
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

        if (showManageFoldersSheet) {
            ModalBottomSheet(
                onDismissRequest = { showManageFoldersSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text("Manage folders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    folders.forEach { folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(folder.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = {
                                showManageFoldersSheet = false
                                folderPendingRename = folder
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Rename ${folder.name}")
                            }
                            IconButton(onClick = {
                                showManageFoldersSheet = false
                                folderPendingDeletion = folder
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete ${folder.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
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
                    item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }

                    item {
                        BottomSheetItem(icon = Icons.Filled.Edit, text = "Rename") {
                            projectPendingRename = selectedProject
                            showBottomSheet = false
                        }
                    }

                    item {
                        BottomSheetItem(
                            icon = if (selectedProject!!.projectState.equals("Completed", ignoreCase = true)) Icons.Filled.EditNote else Icons.Filled.TaskAlt,
                            text = if (selectedProject!!.projectState.equals("Completed", ignoreCase = true)) "Mark as Draft" else "Mark as Completed"
                        ) {
                            val newState = if (selectedProject!!.projectState.equals("Completed", ignoreCase = true)) "Draft" else "Completed"
                            projectViewModel.updateProject(
                                selectedProject!!.copy(projectState = newState, lastEdited = System.currentTimeMillis())
                            )
                            showBottomSheet = false
                        }
                    }

                    item {
                        BottomSheetItem(icon = Icons.Filled.Info, text = "Project details") {
                            projectDetails = selectedProject
                            showBottomSheet = false
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
                                        projectViewModel.updateProject(selectedProject!!.copy(folderId = null, lastEdited = System.currentTimeMillis()))
                                        showBottomSheet = false
                                        moveExpanded = false
                                    }
                                )
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = { 
                                            projectViewModel.updateProject(selectedProject!!.copy(folderId = folder.id, lastEdited = System.currentTimeMillis()))
                                            showBottomSheet = false
                                            moveExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        BottomSheetItem(icon = Icons.Filled.Label, text = "Edit tags") {
                            projectPendingTagEdit = selectedProject
                            showBottomSheet = false
                        }
                    }

                    item {
                        BottomSheetItem(
                            icon = if (selectedProject!!.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            text = if (selectedProject!!.isArchived) "Unarchive" else "Archive"
                        ) {
                            projectViewModel.updateProject(
                                selectedProject!!.copy(
                                    isArchived = !selectedProject!!.isArchived,
                                    lastEdited = System.currentTimeMillis()
                                )
                            )
                            showBottomSheet = false
                        }
                    }

                    item {
                        BottomSheetItem(icon = Icons.Filled.Delete, text = "Delete", isDestructive = true) {
                            projectPendingDeletion = selectedProject
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
fun ProjectListItem(
    project: ProjectEntity,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = { if (selectionMode) onSelectionToggle() else onClick() },
                onLongClick = onSelectionToggle
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
                if (selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
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
            
            IconButton(onClick = onMore) {
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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    bytes < 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
    else -> "${"%.1f".format(bytes.toDouble() / (1024L * 1024L * 1024L))} GB"
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: ProjectEntity,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = { if (selectionMode) onSelectionToggle() else onClick() },
                onLongClick = onSelectionToggle
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
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Project options",
                        tint = Color.White
                    )
                }
                if (selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
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
