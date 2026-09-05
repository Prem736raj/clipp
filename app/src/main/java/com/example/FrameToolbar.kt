package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileCopy

@Composable
fun FrameToolbar(
    frameOverlay: FrameOverlay,
    onCloseToolbar: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onUpdate: (FrameOverlay) -> Unit
) {
    val presetColors = listOf(
        Color.White, Color.Black, Color.Red, Color.Green, Color.Blue, 
        Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray, Color.DarkGray
    )

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Frame", style = MaterialTheme.typography.titleLarge)
                Row {
                    IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, "Copy") }
                    IconButton(onClick = onDuplicate) { Icon(Icons.Filled.FileCopy, "Duplicate") }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Frame", tint = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onCloseToolbar) {
                        Text("Done")
                    }
                }
            }
            
            val type = FrameRegistry.types.find { it.id == frameOverlay.typeId }
            if (type?.category == FrameCategory.CLEAN) {
                Text("Thickness", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = frameOverlay.thickness,
                    onValueChange = { onUpdate(frameOverlay.copy(thickness = it)) },
                    valueRange = 0.01f..0.3f
                )
                
                if (type.id == "clean_rounded") {
                    Text("Corner Radius", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = frameOverlay.cornerRadius,
                        onValueChange = { onUpdate(frameOverlay.copy(cornerRadius = it)) },
                        valueRange = 0f..0.5f // proportion of thickness/dimension
                    )
                }
            }

            Text("Color", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetColors.size) { i ->
                    val color = presetColors[i]
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            2.dp,
                            if (frameOverlay.color == color) MaterialTheme.colorScheme.primary else Color.Gray,
                            CircleShape
                        )
                        .clickable {
                            onUpdate(frameOverlay.copy(color = color))
                        })
                }
            }
        }
    }
}
