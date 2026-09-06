package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.ProjectEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportRecord(
    val id: String,
    val date: Long,
    val resolution: String,
    val size: String,
    val format: String,
    val isQueued: Boolean = false
)

val mockExports = listOf(
    ExportRecord("1", System.currentTimeMillis() - 86400000, "1080p", "45 MB", "MP4"),
    ExportRecord("2", System.currentTimeMillis() - 172800000, "720p", "25 MB", "MP4"),
    ExportRecord("3", System.currentTimeMillis() - 259200000, "1080p", "12 MB", "GIF")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    onProjectClick: (String) -> Unit
) {
    var showQueueScreen by remember { mutableStateOf(false) }
    var storageUsed by remember { mutableStateOf("4.2 GB") }
    
    if (showQueueScreen) {
        ExportQueueScreen(onClose = { showQueueScreen = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects & Exports") },
                actions = {
                    IconButton(onClick = { showQueueScreen = true }) {
                        Icon(Icons.Filled.QueuePlayNext, contentDescription = "Export Queue")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Storage Used", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(storageUsed, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Button(
                            onClick = { storageUsed = "1.1 GB" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Clean Up")
                        }
                    }
                }
            }
            
            if (projects.isEmpty()) {
                item {
                    Text("No projects yet.", modifier = Modifier.padding(16.dp))
                }
            }
            
            items(projects) { project ->
                ProjectExportCard(
                    project = project,
                    exports = mockExports.take(project.name.length % 3 + 1), // random-ish exports
                    onClick = { onProjectClick(project.id) }
                )
            }
        }
    }
}

@Composable
fun ProjectExportCard(project: ProjectEntity, exports: List<ExportRecord>, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(60.dp).background(Color.DarkGray, RoundedCornerShape(8.dp))) {
                    if (project.thumbnailUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(project.thumbnailUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.VideoFile, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Last edited ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(project.lastEdited))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
            
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Export History", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    exports.forEach { export ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(export.date)), style = MaterialTheme.typography.bodyMedium)
                                Text("${export.resolution} • ${export.size} • ${export.format}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalButton(
                                onClick = { /* Re-export logic, mock only */ },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Re-Export", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportQueueScreen(onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Queue") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Pending Exports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vlog Day 1", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Processing... 45%", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { }) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Summer Trip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Queued", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { }) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                }
            }
        }
    }
}
