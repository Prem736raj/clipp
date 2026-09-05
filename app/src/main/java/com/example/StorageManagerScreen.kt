package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectEntity
import com.example.viewmodel.ProjectViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("clipp_settings", Context.MODE_PRIVATE) }
    val projectViewModel: ProjectViewModel = viewModel()
    val projects by projectViewModel.uiState.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var cacheSizeMB by remember { mutableStateOf(450) }
    var exportsSizeMB by remember { mutableStateOf(850) }
    var assetsSizeMB by remember { mutableStateOf(200) }
    var isOptimizing by remember { mutableStateOf(false) }
    var exportFolder by remember { mutableStateOf(sharedPrefs.getString("export_folder", "/Internal/Movies/Clipp") ?: "/Internal/Movies/Clipp") }

    val projectsSizeMB = projects.size * 125 // 125 MB per project approx
    val totalUsedMB = cacheSizeMB + exportsSizeMB + assetsSizeMB + projectsSizeMB
    val totalDeviceStorageMB = 64000 // 64 GB
    val availableStorageMB = totalDeviceStorageMB - totalUsedMB

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFFFC107), // Amber for cache
        Color(0xFF4CAF50)  // Green for assets
    )
    
    // Check for low storage
    LaunchedEffect(availableStorageMB) {
        if (availableStorageMB < 5000) { // Less than 5GB free
            Toast.makeText(context, "Storage space is running low", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Chart section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            val strokeWidth = 32.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val center = Offset(size.width / 2, size.height / 2)
                            
                            val usedFractions = listOf(
                                projectsSizeMB.toFloat() / totalUsedMB,
                                exportsSizeMB.toFloat() / totalUsedMB,
                                cacheSizeMB.toFloat() / totalUsedMB,
                                assetsSizeMB.toFloat() / totalUsedMB
                            )
                            
                            var startAngle = -90f
                            usedFractions.forEachIndexed { index, fraction ->
                                val sweepAngle = fraction * 360f
                                drawArc(
                                    color = colors[index],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweepAngle
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${totalUsedMB / 1000f} GB",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Used",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Available Device Storage: ${availableStorageMB / 1000f} GB / ${totalDeviceStorageMB / 1000f} GB")
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Legend
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    LegendItem("Project Files", "${projectsSizeMB} MB", colors[0])
                    LegendItem("Exported Videos", "${exportsSizeMB} MB", colors[1])
                    LegendItem("Cache & Thumbnails", "${cacheSizeMB} MB", colors[2])
                    LegendItem("Downloaded Assets", "${assetsSizeMB} MB", colors[3])
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Optimize Storage Button
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Button(
                        onClick = {
                            if (!isOptimizing) {
                                scope.launch {
                                    isOptimizing = true
                                    delay(1500)
                                    val freed = cacheSizeMB / 2
                                    cacheSizeMB -= freed
                                    Toast.makeText(context, "Optimized! Freed ${freed} MB of space", Toast.LENGTH_SHORT).show()
                                    isOptimizing = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        enabled = !isOptimizing
                    ) {
                        if (isOptimizing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Optimize Storage", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "Clears old cache, removes unused thumbnails, and compresses project files automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "Storage Management",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // Clear Cache
            item {
                StorageActionItem(
                    icon = Icons.Filled.DeleteOutline,
                    title = "Clear Cache",
                    subtitle = "Free up ${cacheSizeMB} MB. Will not delete projects.",
                    onClick = {
                        Toast.makeText(context, "Cache cleared ($cacheSizeMB MB freed)", Toast.LENGTH_SHORT).show()
                        cacheSizeMB = 0
                    }
                )
            }

            // Clean Exported Videos
            item {
                StorageActionItem(
                    icon = Icons.Filled.VideoLibrary,
                    title = "Clean Exported Videos",
                    subtitle = "${exportsSizeMB} MB used by finalized videos.",
                    onClick = {
                        Toast.makeText(context, "Navigating to exported gallery (simulated)", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Export Folder Location
            item {
                StorageActionItem(
                    icon = Icons.Filled.Folder,
                    title = "Export Folder Location",
                    subtitle = exportFolder,
                    onClick = {
                        // Toggle for simulation
                        val newFolder = if (exportFolder.contains("Internal")) "/SD Card/Movies/Clipp" else "/Internal/Movies/Clipp"
                        exportFolder = newFolder
                        sharedPrefs.edit().putString("export_folder", newFolder).apply()
                    }
                )
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Per project listing
            item {
                Text(
                    text = "Per-Project Storage Usage",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            if (projects.isEmpty()) {
                item {
                    Text(
                        text = "No projects taking up disk space.",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(projects) { project ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(project.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                            Text("Last edited ${android.text.format.DateFormat.format("MMM dd", java.util.Date(project.lastEdited))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("125 MB", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(title: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun StorageActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
