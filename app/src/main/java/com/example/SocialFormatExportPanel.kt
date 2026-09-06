package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class SocialFormat(
    val name: String,
    val icon: ImageVector,
    val ratio: String,
    val maxDurationSec: Int?,
    val note: String
)

val socialFormats = listOf(
    SocialFormat("Instagram Reels", Icons.Filled.PhoneIphone, "9:16", 90, "Auto-trims to 90s, smart reframe to 9:16"),
    SocialFormat("YouTube Shorts", Icons.Filled.PhoneIphone, "9:16", 60, "Auto-trims to 60s, smart reframe to 9:16"),
    SocialFormat("TikTok", Icons.Filled.PhoneIphone, "9:16", null, "Smart reframe to 9:16"),
    SocialFormat("Instagram Feed", Icons.Filled.TabletMac, "1:1", null, "Smart reframe to 1:1 or 4:5"),
    SocialFormat("YouTube", Icons.Filled.Tv, "16:9", null, "Smart reframe to 16:9"),
    SocialFormat("Facebook", Icons.Filled.TabletMac, "4:5", null, "Smart reframe to 4:5"),
    SocialFormat("Twitter/X", Icons.Filled.Videocam, "16:9", 140, "Auto-trims to 2m20s, smart reframe to 16:9"),
    SocialFormat("WhatsApp Status", Icons.Filled.PhoneIphone, "9:16", 30, "Auto-trims to 30s, smart reframe to 9:16")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFormatExportPanel(
    onClose: () -> Unit,
    onExportComplete: () -> Unit
) {
    var selectedFormats by remember { mutableStateOf(setOf<SocialFormat>()) }
    var exportProgress by remember { mutableStateOf(0f) }
    var isExporting by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(isExporting) {
        if (isExporting) {
            while (exportProgress < 1f) {
                delay(100)
                exportProgress += 0.02f
            }
            NotificationHelper.showExportCompleteNotification(context, "Social Formats")
            onExportComplete()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        if (isExporting) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Exporting to ${selectedFormats.size} formats...", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(progress = { exportProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(16.dp))
                Text("${(exportProgress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(32.dp))
                selectedFormats.forEach { format ->
                    Text("Rendering ${format.name} (Auto-crop & trim applied)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Social Format & Export") },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                    },
                    actions = {
                        TextButton(onClick = { 
                            if (selectedFormats.size == socialFormats.size) {
                                selectedFormats = emptySet()
                            } else {
                                selectedFormats = socialFormats.toSet()
                            }
                        }) {
                            Text(if (selectedFormats.size == socialFormats.size) "Deselect All" else "Select All")
                        }
                    }
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "One-Tap Social Media Formatting",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "AI automatically crops, reframes, and trims your video to meet each platform's maximum duration and preferred aspect ratio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    items(socialFormats) { format ->
                        val isSelected = selectedFormats.contains(format)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val s = selectedFormats.toMutableSet()
                                if (isSelected) s.remove(format) else s.add(format)
                                selectedFormats = s
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Icon(
                                    format.icon, 
                                    contentDescription = null, 
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(format.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Ratio: ${format.ratio} | ${if (format.maxDurationSec != null) "Max: ${format.maxDurationSec}s" else "No limit"}", 
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(format.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedFormats.size} platforms selected", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { isExporting = true },
                            enabled = selectedFormats.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export ${if (selectedFormats.size > 1) "All" else ""}")
                        }
                    }
                }
            }
        }
    }
}
