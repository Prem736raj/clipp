package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileCopy

@Composable
fun StickerToolbar(
    stickerOverlay: StickerOverlay,
    onCloseToolbar: () -> Unit,
    onUpdate: (StickerOverlay) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 350.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sticker Edit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Filled.FileCopy, contentDescription = "Duplicate")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onCloseToolbar) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }
            }
            
            val presetColors = listOf(Color.White, Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray, Color.Transparent)
            
            if (stickerOverlay.category == StickerCategory.SHAPE) {
                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetColors.size) { i ->
                        val color = presetColors[i]
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(2.dp, if (stickerOverlay.color == color) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape).clickable {
                            onUpdate(stickerOverlay.copy(color = color))
                        })
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            
            Text("Duration (${stickerOverlay.durationMs / 1000f}s)", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = stickerOverlay.durationMs.toFloat(),
                onValueChange = { onUpdate(stickerOverlay.copy(durationMs = it.toLong())) },
                valueRange = 500f..10000f
            )
            
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Animations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            var selectedAnimTab by remember { mutableStateOf(0) }
            TabRow(selectedTabIndex = selectedAnimTab) {
                Tab(selected = selectedAnimTab == 0, onClick = { selectedAnimTab = 0 }, text = { Text("In") })
                Tab(selected = selectedAnimTab == 1, onClick = { selectedAnimTab = 1 }, text = { Text("Loop") })
                Tab(selected = selectedAnimTab == 2, onClick = { selectedAnimTab = 2 }, text = { Text("Out") })
            }
            when (selectedAnimTab) {
                0 -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                        val anims = TextAnimIn.values()
                        items(anims.size) { i ->
                            val anim = anims[i]
                            val isSelected = stickerOverlay.animIn == anim
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onUpdate(stickerOverlay.copy(animIn = anim)) }
                                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedTextPreview(animIn = anim, text = anim.name.lowercase().replace("_", " ").capitalize())
                            }
                        }
                    }
                    if (stickerOverlay.animIn != TextAnimIn.NONE) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Duration (${stickerOverlay.animInDurationMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = stickerOverlay.animInDurationMs.toFloat(),
                                onValueChange = { onUpdate(stickerOverlay.copy(animInDurationMs = it.toLong())) },
                                valueRange = 200f..2000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Delay (${stickerOverlay.animInDelayMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = stickerOverlay.animInDelayMs.toFloat(),
                                onValueChange = { onUpdate(stickerOverlay.copy(animInDelayMs = it.toLong())) },
                                valueRange = 0f..5000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
                1 -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                        val anims = TextAnimLoop.values()
                        items(anims.size) { i ->
                            val anim = anims[i]
                            val isSelected = stickerOverlay.animLoop == anim
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onUpdate(stickerOverlay.copy(animLoop = anim)) }
                                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedTextPreview(animLoop = anim, text = anim.name.lowercase().replace("_", " ").capitalize())
                            }
                        }
                    }
                    if (stickerOverlay.animLoop != TextAnimLoop.NONE) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Duration (${stickerOverlay.animLoopDurationMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = stickerOverlay.animLoopDurationMs.toFloat(),
                                onValueChange = { onUpdate(stickerOverlay.copy(animLoopDurationMs = it.toLong())) },
                                valueRange = 200f..2000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Delay (${stickerOverlay.animLoopDelayMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = stickerOverlay.animLoopDelayMs.toFloat(),
                                onValueChange = { onUpdate(stickerOverlay.copy(animLoopDelayMs = it.toLong())) },
                                valueRange = 0f..5000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
                2 -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                        val anims = TextAnimOut.values()
                        items(anims.size) { i ->
                            val anim = anims[i]
                            val isSelected = stickerOverlay.animOut == anim
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onUpdate(stickerOverlay.copy(animOut = anim)) }
                                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedTextPreview(animOut = anim, text = anim.name.lowercase().replace("_", " ").capitalize())
                            }
                        }
                    }
                    if (stickerOverlay.animOut != TextAnimOut.NONE) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Duration (${stickerOverlay.animOutDurationMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = stickerOverlay.animOutDurationMs.toFloat(),
                                onValueChange = { onUpdate(stickerOverlay.copy(animOutDurationMs = it.toLong())) },
                                valueRange = 200f..2000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Delay (${stickerOverlay.animOutDelayMs / 1000f}s)", modifier = Modifier.weight(1f))
                            Slider(
                                value = stickerOverlay.animOutDelayMs.toFloat(),
                                onValueChange = { onUpdate(stickerOverlay.copy(animOutDelayMs = it.toLong())) },
                                valueRange = 0f..5000f,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
