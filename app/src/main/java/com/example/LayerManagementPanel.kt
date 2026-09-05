package com.example

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

data class LayerItemInfo(
    val id: String,
    val name: String,
    val type: LayerType,
    val isVisible: Boolean,
    val isLocked: Boolean,
    val opacity: Float
)

enum class LayerType {
    OVERLAY, TEXT, STICKER, DRAWING, FRAME, UNKNOWN
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LayerManagementPanel(
    clips: List<MediaClip>,
    overlays: List<OverlayClip>,
    texts: List<TextOverlay>,
    stickers: List<StickerOverlay>,
    drawings: List<DrawOverlay>,
    frames: List<FrameOverlay>,
    layerOrder: List<String>,
    onUpdateLayerOrder: (List<String>) -> Unit,
    onDeleteLayers: (List<String>) -> Unit,
    onUpdateOpacity: (String, Float) -> Unit,
    onUpdateVisibility: (String, Boolean) -> Unit,
    onUpdateLock: (String, Boolean) -> Unit,
    onClose: () -> Unit
) {
    val itemsMap = remember(overlays, texts, stickers, drawings, frames) {
        val map = mutableMapOf<String, LayerItemInfo>()
        overlays.forEach { map[it.id] = LayerItemInfo(it.id, "Overlay Image", LayerType.OVERLAY, it.isVisible, it.isLocked, it.opacity) }
        texts.forEach { map[it.id] = LayerItemInfo(it.id, it.text.take(15) + if (it.text.length > 15) "..." else "", LayerType.TEXT, it.isVisible, it.isLocked, it.opacity) }
        stickers.forEach { map[it.id] = LayerItemInfo(it.id, "Sticker", LayerType.STICKER, it.isVisible, it.isLocked, it.opacity) }
        drawings.forEach { map[it.id] = LayerItemInfo(it.id, "Drawing", LayerType.DRAWING, it.isVisible, it.isLocked, it.opacity) }
        frames.forEach { map[it.id] = LayerItemInfo(it.id, "Frame", LayerType.FRAME, it.isVisible, it.isLocked, it.opacity) }
        map
    }
    
    // We want the highest z-index at the top of the list for UX.
    // layerOrder has index 0 as bottom, index N-1 as top.
    // So the UI list should display reversed layerOrder.
    val displayList = remember(layerOrder, itemsMap) {
        layerOrder.reversed().mapNotNull { id -> itemsMap[id] }
    }
    
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Drag and drop state
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Layers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
        
        if (selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    onDeleteLayers(selectedIds.toList())
                    selectedIds = emptySet()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                }
                
                IconButton(onClick = {
                    val newOrder = layerOrder.toMutableList()
                    val toMove = selectedIds.toList()
                    newOrder.removeAll(toMove)
                    newOrder.addAll(toMove) // bring to front (end of order)
                    onUpdateLayerOrder(newOrder)
                }) {
                    Icon(Icons.Filled.VerticalAlignTop, contentDescription = "Bring to Front")
                }
                
                IconButton(onClick = {
                    val newOrder = layerOrder.toMutableList()
                    val toMove = selectedIds.toList()
                    newOrder.removeAll(toMove)
                    newOrder.addAll(0, toMove) // send to back (start of order)
                    onUpdateLayerOrder(newOrder)
                }) {
                    Icon(Icons.Filled.VerticalAlignBottom, contentDescription = "Send to Back")
                }
                
                IconButton(onClick = {
                    // Move Up (towards front, higher index)
                    var newOrder = layerOrder.toMutableList()
                    val toMove = layerOrder.filter { it in selectedIds }.reversed()
                    for (id in toMove) {
                        val idx = newOrder.indexOf(id)
                        if (idx < newOrder.size - 1) {
                            newOrder.removeAt(idx)
                            newOrder.add(idx + 1, id)
                        }
                    }
                    onUpdateLayerOrder(newOrder)
                }) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up")
                }
                
                IconButton(onClick = {
                    // Move Down (towards back, lower index)
                    var newOrder = layerOrder.toMutableList()
                    val toMove = layerOrder.filter { it in selectedIds }
                    for (id in toMove) {
                        val idx = newOrder.indexOf(id)
                        if (idx > 0) {
                            newOrder.removeAt(idx)
                            newOrder.add(idx - 1, id)
                        }
                    }
                    onUpdateLayerOrder(newOrder)
                }) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down")
                }
            }
        }
        
        if (displayList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No layers yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(displayList, key = { _, item -> item.id }) { index, item ->
                    val isSelected = selectedIds.contains(item.id)
                    val isDragged = draggedItemIndex == index
                    val isDropTarget = dropTargetIndex == index
                    
                    val elevation = if (isDragged) 8.dp else 0.dp
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .zIndex(if (isDragged) 1f else 0f)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ -> draggedItemIndex = index },
                                    onDrag = { change, dragAmount -> 
                                        change.consume()
                                        // Simple drop logic based on Y offset roughly translated to index
                                        var newDropIdx = index + (change.position.y / 60.dp.toPx()).toInt()
                                        newDropIdx = newDropIdx.coerceIn(0, displayList.size - 1)
                                        dropTargetIndex = newDropIdx
                                    },
                                    onDragEnd = {
                                        if (draggedItemIndex != null && dropTargetIndex != null && draggedItemIndex != dropTargetIndex) {
                                            // The list we're seeing is REVERSED layerOrder.
                                            // Moving from draggedItemIndex to dropTargetIndex in displayList
                                            val mList = displayList.toMutableList()
                                            val movedItem = mList.removeAt(draggedItemIndex!!)
                                            mList.add(dropTargetIndex!!, movedItem)
                                            // Convert back to layerOrder (reverse it)
                                            onUpdateLayerOrder(mList.reversed().map { it.id })
                                        }
                                        draggedItemIndex = null
                                        dropTargetIndex = null
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = null
                                        dropTargetIndex = null
                                    }
                                )
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = elevation
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                    }
                                    .padding(8.dp)
                                    .then(if(isDropTarget) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drag Handle
                                Icon(Icons.Filled.DragIndicator, contentDescription = "Drag", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                
                                // Type Icon
                                val typeIcon = when(item.type) {
                                    LayerType.OVERLAY -> Icons.Filled.Image
                                    LayerType.TEXT -> Icons.Filled.TextFields
                                    LayerType.STICKER -> Icons.Filled.EmojiEmotions
                                    LayerType.DRAWING -> Icons.Filled.Brush
                                    LayerType.FRAME -> Icons.Filled.FilterFrames
                                    else -> Icons.Filled.Layers
                                }
                                Icon(typeIcon, contentDescription = item.type.name, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                
                                Text(item.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                
                                // Visibility Toggle
                                IconButton(onClick = { onUpdateVisibility(item.id, !item.isVisible) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (item.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, 
                                        contentDescription = "Toggle Visibility",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (item.isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Lock Toggle
                                IconButton(onClick = { onUpdateLock(item.id, !item.isLocked) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (item.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen, 
                                        contentDescription = "Toggle Lock",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (item.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            if (isSelected) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Opacity, contentDescription = "Opacity", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Slider(
                                        value = item.opacity,
                                        onValueChange = { onUpdateOpacity(item.id, it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
