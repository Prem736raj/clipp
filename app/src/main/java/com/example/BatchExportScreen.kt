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
import kotlinx.coroutines.delay

data class BatchPlatform(
    val name: String,
    val iconColor: Color,
    val formatInfo: String,
    var isSelected: Boolean = false,
    var progress: Float = 0f,
    var isDone: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchExportScreen(
    videoDurationMs: Long,
    thumbnailUri: String?,
    onClose: () -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0: select, 1: exporting, 2: done
    
    val platforms = remember {
        mutableStateListOf(
            BatchPlatform("Instagram Reels", Color(0xFFE1306C), "9:16 • 1080p • Up to 90s"),
            BatchPlatform("YouTube Shorts", Color(0xFFFF0000), "9:16 • 1080p • Up to 60s"),
            BatchPlatform("TikTok", Color(0xFF000000), "9:16 • 1080p • Up to 10m"),
            BatchPlatform("Facebook", Color(0xFF1877F2), "1:1 • 1080p • Up to 240m"),
            BatchPlatform("Twitter / X", Color(0xFF1DA1F2), "16:9 • 720p • Up to 140s")
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(step) {
        if (step == 1) {
            val selected = platforms.filter { it.isSelected }
            for (p in selected) {
                while (p.progress < 1f) {
                    delay(50)
                    val pIndex = platforms.indexOf(p)
                    platforms[pIndex] = p.copy(progress = p.progress + 0.05f) // fake progress faster
                }
                val pIndex = platforms.indexOf(p)
                platforms[pIndex] = p.copy(progress = 1f, isDone = true)
            }
            NotificationHelper.showExportCompleteNotification(context, "Batch Exports")
            step = 2
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        if (step == 0) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Multi-Platform Export") },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                    }
                )
                
                Text(
                    "Select platforms to export customized versions. We'll automatically adjust the aspect ratio, duration, and encoding settings for each.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(platforms.size) { index ->
                        val p = platforms[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    platforms[index] = p.copy(isSelected = !p.isSelected)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = p.isSelected,
                                onCheckedChange = { platforms[index] = p.copy(isSelected = it) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.size(40.dp).background(p.iconColor, CircleShape), contentAlignment = Alignment.Center) {
                                Text(p.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(p.formatInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                        val selectedCount = platforms.count { it.isSelected }
                        Button(
                            onClick = { if (selectedCount > 0) step = 1 },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = selectedCount > 0
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (selectedCount > 0) "Export $selectedCount Versions" else "Select Platforms", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (step == 1) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Batch Exporting") },
                    navigationIcon = { }
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(platforms.filter { it.isSelected }.size) { i ->
                        val p = platforms.filter { it.isSelected }[i]
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(24.dp).background(p.iconColor, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(p.name.take(1), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.weight(1f))
                                if (p.isDone) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("${(p.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { p.progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = if (p.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (p.isDone) "Done" else "Optimizing ${p.formatInfo}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else if (step == 2) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("All Exports Complete") },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                    }
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("${platforms.count { it.isSelected }} versions saved to Gallery!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    items(platforms.filter { it.isSelected }.size) { i ->
                        val p = platforms.filter { it.isSelected }[i]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.width(60.dp).height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)
                                ) {
                                    if (thumbnailUri != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(thumbnailUri),
                                            contentDescription = "Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Optimized for ${p.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                FilledTonalIconButton(onClick = { /* Share */ }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share")
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
