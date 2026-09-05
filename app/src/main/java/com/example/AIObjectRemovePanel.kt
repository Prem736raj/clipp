package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ObjectRemovalSettings(
    val hasRemovedObjects: Boolean = false,
    val paths: List<Path> = emptyList() // simplified tracking
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIObjectRemovePanel(
    clip: MediaClip,
    initialSettings: ObjectRemovalSettings,
    onApplyParams: (ObjectRemovalSettings) -> Unit,
    onClose: () -> Unit
) {
    var mode by remember { mutableStateOf("Brush") } // "Brush", "Person"
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    var localSettings by remember { mutableStateOf(initialSettings) }
    
    // Drawing specific
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var brushSize by remember { mutableFloatStateOf(40f) }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AI Object Removal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = mode == "Brush",
                        onClick = { mode = "Brush" },
                        label = { Text("Brush Selection") },
                        leadingIcon = { Icon(Icons.Filled.Brush, contentDescription = "Brush") }
                    )
                }
                item {
                    FilterChip(
                        selected = mode == "Person",
                        onClick = { mode = "Person" },
                        label = { Text("Tap Person") },
                        leadingIcon = { Icon(Icons.Filled.PersonRemove, contentDescription = "Person") }
                    )
                }
            }

            if (isAnalyzing) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(progress = analysisProgress)
                    Text("Inpainting and Tracking Area... ${(analysisProgress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                    AsyncImage(
                        model = clip.sourceUri,
                        contentDescription = "Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    if (localSettings.hasRemovedObjects) {
                        // Show a simulated "inpainted" effect over the area
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            localSettings.paths.forEach { path ->
                                drawPath(
                                    path = path,
                                    color = Color.Black.copy(alpha = 0.3f), // simulate patching
                                    style = Stroke(width = brushSize, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                    
                    if (!localSettings.hasRemovedObjects) {
                        Canvas(
                            modifier = Modifier.fillMaxSize().pointerInput(mode) {
                                if (mode == "Brush") {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                        },
                                        onDrag = { change, _ ->
                                            currentPath?.lineTo(change.position.x, change.position.y)
                                        },
                                        onDragEnd = {
                                            if (currentPath != null) {
                                                val p = currentPath!!
                                                currentPath = null
                                                scope.launch {
                                                    isAnalyzing = true
                                                    for (i in 0..100 step 10) {
                                                        analysisProgress = i / 100f
                                                        delay(80)
                                                    }
                                                    isAnalyzing = false
                                                    val newPaths = localSettings.paths.toMutableList().apply { add(p) }
                                                    localSettings = localSettings.copy(hasRemovedObjects = true, paths = newPaths)
                                                }
                                            }
                                        }
                                    )
                                } else if (mode == "Person") {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val pointerEvent = awaitPointerEvent()
                                            if (pointerEvent.changes.any { it.pressed }) {
                                                val offset = pointerEvent.changes.first().position
                                                // Create a fake bounding box around tap for a person
                                                val p = Path().apply {
                                                    moveTo(offset.x, offset.y - 100f)
                                                    lineTo(offset.x, offset.y + 100f)
                                                }
                                                scope.launch {
                                                    isAnalyzing = true
                                                    for (i in 0..100 step 15) {
                                                        analysisProgress = i / 100f
                                                        delay(100)
                                                    }
                                                    isAnalyzing = false
                                                    val newPaths = localSettings.paths.toMutableList().apply { add(p) }
                                                    localSettings = localSettings.copy(hasRemovedObjects = true, paths = newPaths)
                                                }
                                                break // only handle one tap
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            localSettings.paths.forEach { path ->
                                drawPath(
                                    path = path,
                                    color = Color.Red.copy(alpha = 0.5f),
                                    style = Stroke(width = brushSize, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                            currentPath?.let {
                                drawPath(
                                    path = it,
                                    color = Color.Red.copy(alpha = 0.5f),
                                    style = Stroke(width = brushSize, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                    
                    if (localSettings.hasRemovedObjects) {
                        IconButton(
                            onClick = {
                                val current = localSettings.paths.toMutableList()
                                if (current.isNotEmpty()) {
                                    current.removeLast()
                                }
                                localSettings = localSettings.copy(hasRemovedObjects = current.isNotEmpty(), paths = current)
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo", tint = Color.White)
                        }
                    } else if (mode == "Brush") {
                        Text("Paint over object to remove", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                    } else {
                        Text("Tap on person to remove", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                    }
                }
                
                if (mode == "Brush" && !localSettings.hasRemovedObjects) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Brush Size", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = brushSize,
                            onValueChange = { brushSize = it },
                            valueRange = 10f..100f
                        )
                    }
                }
                Text("AI is generating background replacement and tracking the removal area across frames.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onClose) { Text("Cancel") }
                    Button(onClick = { onApplyParams(localSettings) }) { Text("Apply Remove") }
                }
            }
        }
    }
}
