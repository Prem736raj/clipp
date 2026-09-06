package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionsPanel(
    captions: List<AutoCaptionSegment>,
    settings: CaptionSettings,
    onSettingsChanged: (CaptionSettings) -> Unit,
    onGenerateCaptions: () -> Unit,
    onCaptionsChanged: (List<AutoCaptionSegment>) -> Unit,
    onClose: () -> Unit,
    aiUsageString: String? = null
) {
    var isProcessing by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
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
                Text("Auto Captions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            if (captions.isEmpty()) {
                // Generate controls
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Translate, contentDescription = "Language", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = settings.language,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Language") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            SUPPORTED_CAPTION_LANGUAGES.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        onSettingsChanged(settings.copy(language = lang))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                var showBatteryWarning by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current
                
                if (showBatteryWarning) {
                    AlertDialog(
                        onDismissRequest = { showBatteryWarning = false },
                        title = { Text("Low Battery Warning") },
                        text = { Text("Auto generating captions uses AI processing and might drain remaining battery quickly. Consider plugging in your charger.") },
                        confirmButton = {
                            TextButton(onClick = { 
                                showBatteryWarning = false
                                isProcessing = true
                                onGenerateCaptions()
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

                Button(
                    onClick = {
                        val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
                        val batteryPct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        if (batteryPct < 20 && !bm.isCharging) {
                            showBatteryWarning = true
                        } else {
                            isProcessing = true
                            onGenerateCaptions()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Text(if (isProcessing) "Queued for background processing..." else "Generate Auto Captions")
                }
                
                if (aiUsageString != null) {
                    Text(aiUsageString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            } else {
                // Edit controls & generated list
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    var translateExpanded by remember { mutableStateOf(false) }
                    Text("Translate to:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = translateExpanded,
                        onExpandedChange = { translateExpanded = !translateExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = settings.language,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = translateExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenu(
                            expanded = translateExpanded,
                            onDismissRequest = { translateExpanded = false }
                        ) {
                            SUPPORTED_CAPTION_LANGUAGES.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        onSettingsChanged(settings.copy(language = lang))
                                        val translatedCaps = captions.map { seg ->
                                            seg.copy(
                                                text = "[Translated to $lang] ${seg.text}",
                                                words = seg.words.map { w -> w.copy(word = "[$lang]") }
                                            )
                                        }
                                        onCaptionsChanged(translatedCaps)
                                        translateExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Text("Caption Style", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaptionStyle.values().forEach { style ->
                        val isSelected = style == settings.style
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSettingsChanged(settings.copy(style = style)) },
                            label = { Text(style.name.replace("_", " ")) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Subtitles Editor (Tap to edit)", style = MaterialTheme.typography.labelMedium)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(captions) { caption ->
                        var localText by remember(caption) { mutableStateOf(caption.text) }
                        OutlinedTextField(
                            value = localText,
                            onValueChange = { newT ->
                                localText = newT
                                val newCaps = captions.map { if (it.id == caption.id) it.copy(text = newT) else it }
                                onCaptionsChanged(newCaps)
                            },
                            label = { Text("%.1fs - %.1fs".format(caption.startTimeMs / 1000f, (caption.startTimeMs + caption.durationMs) / 1000f)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Button(onClick = { onCaptionsChanged(emptyList()) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Clear All Captions")
                }
            }
        }
    }
}
