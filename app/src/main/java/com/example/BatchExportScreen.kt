package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ProjectEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class BatchItemStatus { Pending, Rendering, Completed, Failed }

private data class BatchItemState(
    val status: BatchItemStatus = BatchItemStatus.Pending,
    val progress: Int = 0,
    val errorMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchExportScreen(
    projects: List<ProjectEntity>,
    onClose: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedIds by remember(projects) { mutableStateOf(projects.map { it.id }.toSet()) }
    var itemStates by remember(projects) {
        mutableStateOf(projects.associate { it.id to BatchItemState() })
    }
    var isExporting by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<String?>(null) }
    var exportJob by remember { mutableStateOf<Job?>(null) }
    val latestExportJob = rememberUpdatedState(exportJob)

    DisposableEffect(Unit) {
        onDispose { latestExportJob.value?.cancel() }
    }

    fun cancelAndClose() {
        exportJob?.cancel()
        exportJob = null
        onClose()
    }

    val selectedProjects = projects.filter { it.id in selectedIds }
    val completedCount = itemStates.values.count { it.status == BatchItemStatus.Completed }
    val failedCount = itemStates.values.count { it.status == BatchItemStatus.Failed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Export") },
                navigationIcon = {
                    IconButton(onClick = if (isExporting) ::cancelAndClose else onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = if (isExporting) "Cancel batch export" else "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Export saved projects", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Each selected project is rendered sequentially through the verified local MP4 exporter and saved to Movies/Clipp.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { selectedIds = projects.map { it.id }.toSet() },
                            enabled = !isExporting && projects.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.SelectAll, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 2.dp))
                            Text("Select all")
                        }
                        OutlinedButton(
                            onClick = { selectedIds = emptySet() },
                            enabled = !isExporting && selectedIds.isNotEmpty()
                        ) { Text("Clear") }
                    }
                }
                if (projects.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text(
                                "No local projects are available for batch export.",
                                modifier = Modifier.padding(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        BatchProjectCard(
                            project = project,
                            selected = project.id in selectedIds,
                            state = itemStates[project.id] ?: BatchItemState(),
                            enabled = !isExporting,
                            onToggle = {
                                selectedIds = if (project.id in selectedIds) {
                                    selectedIds - project.id
                                } else {
                                    selectedIds + project.id
                                }
                            }
                        )
                    }
                }
                if (isExporting) {
                    item {
                        Text("Rendering $completedCount of ${selectedProjects.size} selected project(s)...")
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "You can cancel from the back button. Completed files remain saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (summary != null) {
                    item {
                        Text(summary!!, color = if (failedCount == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                }
            }
            Button(
                onClick = {
                    summary = null
                    itemStates = projects.associate { it.id to BatchItemState() }
                    isExporting = true
                    exportJob = scope.launch {
                        try {
                            val results = BatchExportRunner(context).export(
                                projects = selectedProjects,
                                onProjectStarted = { project ->
                                    itemStates = itemStates + (project.id to BatchItemState(BatchItemStatus.Rendering))
                                },
                                onProjectProgress = { project, progress ->
                                    val current = itemStates[project.id] ?: BatchItemState(BatchItemStatus.Rendering)
                                    itemStates = itemStates + (project.id to current.copy(status = BatchItemStatus.Rendering, progress = progress))
                                },
                                onProjectFinished = { project, result ->
                                    itemStates = itemStates + (
                                        project.id to BatchItemState(
                                            status = if (result.succeeded) BatchItemStatus.Completed else BatchItemStatus.Failed,
                                            progress = if (result.succeeded) 100 else 0,
                                            errorMessage = result.errorMessage
                                        )
                                    )
                                }
                            )
                            val successful = results.count { it.succeeded }
                            val failed = results.size - successful
                            summary = if (failed == 0) {
                                "$successful project(s) exported successfully."
                            } else {
                                "$successful project(s) exported; $failed project(s) need attention."
                            }
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (error: Exception) {
                            summary = error.message?.takeIf(String::isNotBlank) ?: "Batch export failed"
                        } finally {
                            isExporting = false
                            exportJob = null
                        }
                    }
                },
                enabled = !isExporting && selectedProjects.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(if (isExporting) "Rendering..." else "Export ${selectedProjects.size} project(s)")
            }
        }
    }
}

@Composable
private fun BatchProjectCard(
    project: ProjectEntity,
    selected: Boolean,
    state: BatchItemState,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = enabled)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(project.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${project.sourceMediaPaths.size} source item(s) · ${project.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when (state.status) {
                    BatchItemStatus.Rendering -> Text("Rendering ${state.progress}%", style = MaterialTheme.typography.bodySmall)
                    BatchItemStatus.Completed -> Text("Saved to device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    BatchItemStatus.Failed -> Text(state.errorMessage ?: "Export failed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    BatchItemStatus.Pending -> Unit
                }
            }
            when (state.status) {
                BatchItemStatus.Completed -> Icon(Icons.Filled.CheckCircle, contentDescription = "Exported", tint = MaterialTheme.colorScheme.primary)
                BatchItemStatus.Failed -> Icon(Icons.Filled.ErrorOutline, contentDescription = "Export failed", tint = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}
