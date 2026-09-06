package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailGeneratorScreen(
    videoDurationMs: Long,
    onClose: () -> Unit,
    onThumbnailSaved: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("YouTube") } // YouTube, Instagram, Tiktok
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(10f) }
    var showBorder by remember { mutableStateOf(false) }
    var showShadow by remember { mutableStateOf(false) }
    var textOverlay by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("AI Frames") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thumbnail Generator") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                },
                actions = {
                    Button(onClick = onThumbnailSaved, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            // Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Wrapper for aspect ratio
                val aspectRatio = if (selectedFormat == "YouTube") 16f/9f else if (selectedFormat == "Instagram") 1f/1f else 9f/16f
                
                Box(
                    modifier = Modifier
                        .aspectRatio(aspectRatio)
                        .background(Color.DarkGray)
                        .then(if (showBorder) Modifier.border(4.dp, Color.White) else Modifier)
                        .then(if (showShadow) Modifier.shadow(8.dp) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    
                    if (textOverlay.isNotEmpty()) {
                        Text(
                            text = textOverlay,
                            color = Color.White,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        )
                    }
                }
            }
            
            // Format Selection
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(3) { index ->
                    val format = listOf("YouTube", "Instagram", "TikTok")[index]
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { selectedFormat = format },
                        label = { Text(format) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs for tools
            TabRow(selectedTabIndex = listOf("AI Frames", "Text", "Adjust").indexOf(activeTab)) {
                listOf("AI Frames", "Text", "Adjust").forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab) }
                    )
                }
            }

            // Tools Content
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp)) {
                when (activeTab) {
                    "AI Frames" -> {
                        Column {
                            Text("Best Frames (AI Auto-Selected)", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(5) {
                                    Box(
                                        modifier = Modifier.size(100.dp, 60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("00:0${it+1}")
                                    }
                                }
                            }
                        }
                    }
                    "Text" -> {
                        Column {
                            OutlinedTextField(
                                value = textOverlay,
                                onValueChange = { textOverlay = it },
                                label = { Text("Add Text") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    "Adjust" -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Brightness", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
                                Slider(value = brightness, onValueChange = { brightness = it }, modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Contrast", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
                                Slider(value = contrast, onValueChange = { contrast = it }, modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showBorder, onCheckedChange = { showBorder = it })
                                    Text("Border", style = MaterialTheme.typography.labelSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showShadow, onCheckedChange = { showShadow = it })
                                    Text("Shadow", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
