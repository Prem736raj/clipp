package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSettingsScreen(
    videoDurationMs: Long,
    thumbnailUri: String?,
    onClose: () -> Unit,
    onExportComplete: () -> Unit,
    onOpenBatchExport: () -> Unit,
    isPro: Boolean = false,
    hasProjectProFeatures: Boolean = false,
    isGracePeriod: Boolean = false,
    onProFeatureTap: () -> Unit = {}
) {
    var resolution by remember { mutableStateOf("1080p") }
    var frameRate by remember { mutableStateOf("30") }
    var format by remember { mutableStateOf("MP4") }
    var quality by remember { mutableStateOf("High") }
    var audioQuality by remember { mutableStateOf("320kbps") }
    
    var includeSubtitles by remember { mutableStateOf(false) }
    var subtitleFormat by remember { mutableStateOf("Burn-in") } // Burn-in or SRT
    
    var exportAsGif by remember { mutableStateOf(false) }
    var exportAudioOnly by remember { mutableStateOf(false) }
    
    var showAdvanced by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var isDone by remember { mutableStateOf(false) }
    var currentProcessingStep by remember { mutableStateOf("Initializing...") }

    val resolutions = listOf(
        Pair("720p", "HD"),
        Pair("1080p", "FHD"),
        Pair("2K", "QHD"),
        Pair("4K", "UHD")
    )
    val frameRates = listOf("24", "30", "60")
    val formats = listOf("MP4", "MOV", "WebM")
    val qualities = listOf(
        Pair("Low", "~5 Mbps"),
        Pair("Medium", "~10 Mbps"),
        Pair("High", "~20 Mbps"),
        Pair("Maximum", "~40 Mbps")
    )
    val audioQualities = listOf("128kbps", "192kbps", "320kbps")
    
    // Calculate estimated file size
    val estSizeMB = remember(resolution, frameRate, quality, videoDurationMs, exportAsGif, exportAudioOnly) {
        if (exportAudioOnly) {
            (videoDurationMs / 1000f) * 0.04f
        } else if (exportAsGif) {
            (videoDurationMs / 1000f) * 1.5f
        } else {
            var baseMbPerSec = when (quality) {
                "Low" -> 0.6f
                "Medium" -> 1.25f
                "High" -> 2.5f
                "Maximum" -> 5.0f
                else -> 2.5f
            }
            if (resolution == "4K") baseMbPerSec *= 4f
            if (resolution == "2K") baseMbPerSec *= 2f
            if (resolution == "720p") baseMbPerSec *= 0.5f
            if (frameRate == "60") baseMbPerSec *= 1.5f
            (videoDurationMs / 1000f) * baseMbPerSec
        }
    }
    
    val estTimeSec = (estSizeMB * 0.5f).toInt().coerceAtLeast(1)
    
    val context = androidx.compose.ui.platform.LocalContext.current

    val taskId = remember { java.util.UUID.randomUUID().toString() }
    
    LaunchedEffect(isExporting) {
        if (isExporting) {
            com.example.viewmodel.BackgroundTaskManager.addTask(
                com.example.viewmodel.BackgroundTask(
                    id = taskId,
                    title = if (exportAudioOnly) "Exporting Audio" else if (exportAsGif) "Generating GIF" else "Exporting Video",
                    type = com.example.viewmodel.TaskType.EXPORT,
                    status = com.example.viewmodel.TaskStatus.RUNNING
                )
            )
            onClose() // Dismiss dialog to let user continue editing
            android.widget.Toast.makeText(context, "Export started in background. Check Task Manager.", android.widget.Toast.LENGTH_LONG).show()
            
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.GlobalScope.launch {
                var progress = 0f
                var step = "Preparing MediaCodec hardware encoder..."
                delay(500)
                var hasFailedSim = false
                while (progress < 1f) {
                    delay(500) // Increase delay to make it take a bit of time
                    progress += 0.05f
                    if (progress > 1f) progress = 1f
                    
                    if (progress > 0.4f && !hasFailedSim && resolution == "4K") {
                         hasFailedSim = true
                         com.example.viewmodel.BackgroundTaskManager.updateProgress(taskId, progress, "Error: OutOfMemoryError")
                         com.example.viewmodel.BackgroundTaskManager.updateStatus(taskId, com.example.viewmodel.TaskStatus.FAILED)
                         com.example.NotificationHelper.cancelNotification(context, taskId.hashCode())
                         return@launch
                    }
                    
                    if (progress < 0.3f) step = "Rendering effects and filters..."
                    else if (progress < 0.6f) step = "Mixing audio and voiceovers..."
                    else if (progress < 0.9f) step = "Encoding video frames..."
                    else step = "Finalizing file..."
                    
                    com.example.viewmodel.BackgroundTaskManager.updateProgress(taskId, progress, step)
                    NotificationHelper.showProgressNotification(context, taskId.hashCode(), "Exporting", step, (progress*100).toInt())
                }
                NotificationHelper.cancelNotification(context, taskId.hashCode())
                NotificationHelper.showExportCompleteNotification(context, "Project")
                com.example.viewmodel.BackgroundTaskManager.updateStatus(taskId, com.example.viewmodel.TaskStatus.COMPLETED)
                val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                mainHandler.post {
                     val v = (context as? android.app.Activity)?.window?.decorView
                     if (v != null) {
                         com.example.utils.HapticUtil.playSuccess(v, context)
                     }
                }
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        if (isDone) {
            ExportSuccessScreen(
                thumbnailUri = thumbnailUri,
                estSizeMB = estSizeMB,
                videoDurationMs = videoDurationMs,
                isAudioOnly = exportAudioOnly,
                exportAsGif = exportAsGif,
                onClose = onExportComplete
            )
        } else if (isExporting) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (exportAudioOnly) "Exporting Audio..." else if (exportAsGif) "Generating GIF..." else "Exporting Video...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                // Live preview mock
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    if (thumbnailUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(thumbnailUri),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.5f + (exportProgress * 0.5f) // fake progress effect
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(currentProcessingStep, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = { exportProgress },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${(exportProgress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val timeRem = (estTimeSec - (exportProgress * estTimeSec)).toInt().coerceAtLeast(0)
                    Text("~${timeRem}s remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("You can leave the app safely. We'll notify you when it's done.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Export Settings") },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                    }
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header & Preview
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                if (thumbnailUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(thumbnailUri),
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha=0.6f), RoundedCornerShape(4.dp)).padding(horizontal=4.dp, vertical=2.dp)
                                ) {
                                    Text(String.format("%02d:%02d", (videoDurationMs / 1000) / 60, (videoDurationMs / 1000) % 60), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Estimated Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", estSizeMB)} MB", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Export Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("~${estTimeSec}s", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    // Special Overrides
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = exportAudioOnly,
                                onClick = { 
                                    exportAudioOnly = !exportAudioOnly
                                    if (exportAudioOnly) exportAsGif = false
                                },
                                label = { Text("Audio Only (MP3)") },
                                leadingIcon = if (exportAudioOnly) { { Icon(Icons.Filled.Check, null) } } else null
                            )
                            if (videoDurationMs <= 15000L) {
                                FilterChip(
                                    selected = exportAsGif,
                                    onClick = { 
                                        exportAsGif = !exportAsGif
                                        if (exportAsGif) exportAudioOnly = false
                                    },
                                    label = { Text("Export as GIF") },
                                    leadingIcon = if (exportAsGif) { { Icon(Icons.Filled.Check, null) } } else null
                                )
                            }
                        }
                    }

                    if (!exportAudioOnly && !exportAsGif) {
                        // Resolution
                        item {
                            Text("Resolution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                resolutions.forEach { (res, label) ->
                                    val isProRes = res == "2K" || res == "4K"
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable {
                                            resolution = res
                                        }.height(60.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (resolution == res) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (resolution == res) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(res, fontWeight = FontWeight.Bold)
                                                if (isProRes && !isPro) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Star, contentDescription = "Pro", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Text(label, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                            if (resolution == "720p" || resolution == "1080p") {
                                Text("Free • No Watermark", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        // Quality
                        item {
                            Text("Quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                qualities.forEach { (q, bitrate) ->
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable { quality = q }.height(56.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (quality == q) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (quality == q) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(q, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text(bitrate, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Advanced Settings Toggle
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Advanced Settings", fontWeight = FontWeight.SemiBold)
                                Icon(
                                    if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    // Advanced Settings Segment
                    if (showAdvanced) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (!exportAudioOnly && !exportAsGif) {
                                    // Frame Rate
                                    Column {
                                        Text("Frame Rate", style = MaterialTheme.typography.titleMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                            frameRates.forEach { fps ->
                                                FilterChip(
                                                    selected = frameRate == fps,
                                                    onClick = { frameRate = fps },
                                                    label = { Text("${fps}fps") }
                                                )
                                            }
                                        }
                                    }

                                    // Format
                                    Column {
                                        Text("Format", style = MaterialTheme.typography.titleMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                            formats.forEach { f ->
                                                FilterChip(
                                                    selected = format == f,
                                                    onClick = { format = f },
                                                    label = { Text(f) }
                                                )
                                            }
                                        }
                                    }

                                    // Subtitles
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Text("Include Subtitles", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                            Switch(checked = includeSubtitles, onCheckedChange = { includeSubtitles = it })
                                        }
                                        if (includeSubtitles) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                                FilterChip(
                                                    selected = subtitleFormat == "Burn-in",
                                                    onClick = { subtitleFormat = "Burn-in" },
                                                    label = { Text("Burn into video") }
                                                )
                                                FilterChip(
                                                    selected = subtitleFormat == "SRT",
                                                    onClick = { subtitleFormat = "SRT" },
                                                    label = { Text("Separate SRT file") }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Audio Quality
                                if (!exportAsGif) {
                                    Column {
                                        Text("Audio Quality", style = MaterialTheme.typography.titleMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                            audioQualities.forEach { aq ->
                                                FilterChip(
                                                    selected = audioQuality == aq,
                                                    onClick = { audioQuality = aq },
                                                    label = { Text(aq) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(
                            onClick = onOpenBatchExport,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Filled.DynamicFeed, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export for Multiple Platforms", fontWeight = FontWeight.Bold)
                        }
                    }
                } // End LazyColumn

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var showBatteryWarning by remember { mutableStateOf(false) }

                    if (showBatteryWarning) {
                        AlertDialog(
                            onDismissRequest = { showBatteryWarning = false },
                            title = { Text("Low Battery Warning") },
                            text = { Text("This will take ~10 minutes. Consider plugging in your charger.") },
                            confirmButton = {
                                TextButton(onClick = { 
                                    showBatteryWarning = false
                                    isExporting = true
                                }) {
                                    Text("Continue Anyway")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBatteryWarning = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()
                    ) {
                        Button(
                            onClick = { 
                                val isProRes = resolution == "2K" || resolution == "4K"
                                if (!isPro) {
                                    if (isProRes || (hasProjectProFeatures && !isGracePeriod)) {
                                        onProFeatureTap()
                                        return@Button
                                    }
                                }
                                
                                val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
                                val batteryPct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                if (batteryPct < 20 && !bm.isCharging) {
                                    showBatteryWarning = true
                                } else {
                                    isExporting = true 
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
