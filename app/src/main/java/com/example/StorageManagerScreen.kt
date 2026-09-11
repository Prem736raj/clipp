package com.example

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectStorage
import com.example.viewmodel.ProjectViewModel
import java.io.File

private data class StorageBreakdown(
    val appFiles: Long,
    val cache: Long,
    val exports: Long,
    val deviceTotal: Long,
    val deviceFree: Long
) {
    val appOwned: Long get() = appFiles + cache + exports
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val projectViewModel: ProjectViewModel = viewModel()
    val projects by projectViewModel.uiState.collectAsState()
    var isClearing by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableStateOf(0) }

    val storage = remember(refreshToken, projects) { readStorageBreakdown(context) }
    val projectSizes = remember(projects, refreshToken) {
        projects.associate { it.id to ProjectStorage.calculateOwnedSizeBytes(context, it) }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        formatStorageSize(storage.appOwned),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Clipp app-owned storage", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Device free: ${formatStorageSize(storage.deviceFree)} of ${formatStorageSize(storage.deviceTotal)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    LegendItem("App files and database", formatStorageSize(storage.appFiles), MaterialTheme.colorScheme.primary)
                    LegendItem("Cache", formatStorageSize(storage.cache), MaterialTheme.colorScheme.tertiary)
                    LegendItem("Clipp exports", formatStorageSize(storage.exports), MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(24.dp))
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Button(
                        onClick = {
                            if (!isClearing) {
                                isClearing = true
                                val before = directorySize(context.cacheDir) +
                                    directorySize(context.externalCacheDir)
                                clearDirectoryContents(context.cacheDir)
                                context.externalCacheDir?.let(::clearDirectoryContents)
                                val after = directorySize(context.cacheDir) +
                                    directorySize(context.externalCacheDir)
                                isClearing = false
                                refreshToken++
                                Toast.makeText(
                                    context,
                                    "Cache cleared (${formatStorageSize((before - after).coerceAtLeast(0L))} freed)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        enabled = !isClearing
                    ) {
                        if (isClearing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Clear cache", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    "Only Clipp cache files are removed. Projects and source media stay intact.",
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            item {
                StorageActionItem(
                    icon = Icons.Filled.Movie,
                    title = "Clipp exports",
                    subtitle = if (storage.exports == 0L) {
                        "No Clipp exports found in Movies/Clipp"
                    } else {
                        "${formatStorageSize(storage.exports)} in Movies/Clipp; manage these files in Gallery"
                    },
                    onClick = {
                        Toast.makeText(
                            context,
                            "Manage exported videos from your Gallery app",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            item {
                Text(
                    "Per-Project Storage Usage",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
            if (projects.isEmpty()) {
                item {
                    Text(
                        "No local projects.",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(project.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Last edited ${android.text.format.DateFormat.format("MMM dd", java.util.Date(project.lastEdited))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            formatStorageSize(projectSizes[project.id] ?: 0L),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
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
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StorageActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun readStorageBreakdown(context: Context): StorageBreakdown {
    val stat = StatFs(context.filesDir.absolutePath)
    val blockSize = stat.blockSizeLong
    return StorageBreakdown(
        appFiles = directorySize(context.filesDir) + directorySize(context.getExternalFilesDir(null)),
        cache = directorySize(context.cacheDir) + directorySize(context.externalCacheDir),
        exports = queryClippExportBytes(context),
        deviceTotal = stat.blockCountLong * blockSize,
        deviceFree = stat.availableBlocksLong * blockSize
    )
}

private fun queryClippExportBytes(context: Context): Long {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0L
    val projection = arrayOf(MediaStore.Video.Media.SIZE, MediaStore.Video.Media.RELATIVE_PATH)
    val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
    val args = arrayOf("${Environment.DIRECTORY_MOVIES}/Clipp/%")
    return runCatching {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            var total = 0L
            while (cursor.moveToNext()) {
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) total += cursor.getLong(sizeColumn)
            }
            total
        } ?: 0L
    }.getOrDefault(0L)
}

private fun directorySize(file: File?): Long {
    if (file == null || !file.exists()) return 0L
    if (file.isFile) return file.length()
    return file.listFiles()?.sumOf(::directorySize) ?: 0L
}

private fun clearDirectoryContents(directory: File) {
    directory.listFiles()?.forEach { it.deleteRecursively() }
}

private fun formatStorageSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    bytes < 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
    else -> "${bytes / (1024L * 1024L * 1024L)} GB"
}
