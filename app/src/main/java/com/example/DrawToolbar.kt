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
import androidx.compose.material.icons.filled.Undo
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
fun DrawToolbar(
    brushType: BrushType,
    onBrushTypeChange: (BrushType) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    brushColor: Color,
    onBrushColorChange: (Color) -> Unit,
    isEraser: Boolean,
    onEraserChange: (Boolean) -> Unit,
    onUndoStoke: () -> Unit,
    onDeleteOverlay: () -> Unit,
    onCloseToolbar: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleAnim: (Boolean) -> Unit,
    isAnimated: Boolean,
    onChangeDuration: (Long) -> Unit,
    durationMs: Long
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
                Text("Draw", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, "Copy") }
                    IconButton(onClick = onDuplicate) { Icon(Icons.Filled.FileCopy, "Duplicate") }
                    IconButton(onClick = onUndoStoke) {
                        Icon(Icons.Filled.Undo, "Undo Stroke")
                    }
                    IconButton(onClick = onDeleteOverlay) {
                        Icon(Icons.Filled.Delete, "Delete Drawing", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onCloseToolbar) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }
            }
            
            val presetColors = listOf(Color.White, Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray)
            
            Text("Color", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                items(presetColors.size) { i ->
                    val color = presetColors[i]
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(2.dp, if (brushColor == color && !isEraser) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape).clickable {
                        onBrushColorChange(color)
                        onEraserChange(false)
                    })
                }
            }
            
            Text("Brushes", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                items(BrushType.values().size) { i ->
                    val bType = BrushType.values()[i]
                    FilterChip(
                        selected = brushType == bType && !isEraser,
                        onClick = { onBrushTypeChange(bType); onEraserChange(false) },
                        label = { Text(bType.title) }
                    )
                }
                item {
                    FilterChip(
                        selected = isEraser,
                        onClick = { onEraserChange(true) },
                        label = { Text("Eraser") }
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size", modifier = Modifier.weight(0.5f))
                Slider(
                    value = brushSize,
                    onValueChange = onBrushSizeChange,
                    valueRange = 0.005f..0.1f,
                    modifier = Modifier.weight(2f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Duration (${durationMs / 1000f}s)", modifier = Modifier.weight(0.5f))
                Slider(
                    value = durationMs.toFloat(),
                    onValueChange = { onChangeDuration(it.toLong()) },
                    valueRange = 500f..10000f,
                    modifier = Modifier.weight(2f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Draw Animation (Replay Strokes)", modifier = Modifier.weight(1f))
                Switch(checked = isAnimated, onCheckedChange = onToggleAnim)
            }
        }
    }
}
