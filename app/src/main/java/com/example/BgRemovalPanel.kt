package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BgRemovalPanel(
    settings: BgRemovalSettings,
    onSettingsChanged: (BgRemovalSettings) -> Unit
) {
    var isProcessingOffline by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("AI Background Removal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = settings.enabled, onCheckedChange = { onSettingsChanged(settings.copy(enabled = it)) })
        }

        if (settings.enabled) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
                    val batteryPct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    
                    if (batteryPct < 20 && !bm.isCharging) {
                        android.widget.Toast.makeText(context, "Low Battery: Background removal might take longer. Plug in charger for best performance.", android.widget.Toast.LENGTH_LONG).show()
                    }
                    
                    isProcessingOffline = true
                    onSettingsChanged(settings.copy(isAutoProcessing = true))
                    
                    val taskId = java.util.UUID.randomUUID().toString()
                    com.example.viewmodel.BackgroundTaskManager.addTask(
                        com.example.viewmodel.BackgroundTask(
                            id = taskId,
                            title = "AI Background Removal",
                            type = com.example.viewmodel.TaskType.AI_PROCESSING
                        )
                    )
                    
                    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                    kotlinx.coroutines.GlobalScope.launch {
                        for(i in 1..10) {
                            kotlinx.coroutines.delay(200)
                            val p = i / 10f
                            com.example.viewmodel.BackgroundTaskManager.updateProgress(taskId, p, "Detecting subject edges...")
                            com.example.NotificationHelper.showProgressNotification(context, taskId.hashCode(), "AI Processing", "Removing background...", (p*100).toInt())
                        }
                        com.example.NotificationHelper.cancelNotification(context, taskId.hashCode())
                        com.example.viewmodel.BackgroundTaskManager.updateStatus(taskId, com.example.viewmodel.TaskStatus.COMPLETED)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (settings.isAutoProcessing) "Processed (AI Tracking Active)" else "Remove Background (Auto)")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Background Type", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BgReplacementMode.values().forEach { mode ->
                    val isSelected = mode == settings.replacementMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                onSettingsChanged(settings.copy(replacementMode = mode))
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            mode.label,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (settings.replacementMode == BgReplacementMode.SOLID_COLOR) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = listOf(0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00)
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(2.dp, if (settings.replacementColor == c) Color.White else Color.Transparent, CircleShape)
                                .clickable { onSettingsChanged(settings.copy(replacementColor = c)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Edge Softness (Feather)", modifier = Modifier.width(160.dp))
                Slider(
                    value = settings.edgeSmoothing,
                    onValueChange = { i -> onSettingsChanged(settings.copy(edgeSmoothing = i)) },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Manual Refine Brush: Drag on preview to add/remove areas (simulated).", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
