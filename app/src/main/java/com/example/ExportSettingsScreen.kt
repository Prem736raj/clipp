package com.example

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ExportSettingsScreen(
    clips: List<MediaClip>,
    editorState: EditorState = EditorState(clips = clips),
    videoDurationMs: Long,
    thumbnailUri: String?,
    onClose: () -> Unit,
    onExportComplete: () -> Unit
) {
    val context = LocalContext.current
    val exporter = remember { VideoExporter(context) }
    var isExporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var outputMetadata by remember { mutableStateOf<ExportMetadata?>(null) }
    var activeExportHandle by remember { mutableStateOf<ExportHandle?>(null) }

    DisposableEffect(exporter) {
        onDispose { exporter.close() }
    }

    val uri = outputUri
    val metadata = outputMetadata
    val unsupportedReasons = editorState.exportUnsupportedReasons()
    fun cancelExport() {
        activeExportHandle?.cancel()
        activeExportHandle = null
        isExporting = false
        errorMessage = "Export cancelled"
    }

    if (uri != null && metadata != null) {
        ExportSuccessScreen(
            outputUri = uri,
            thumbnailUri = thumbnailUri,
            videoDurationMs = metadata.durationMs,
            fileSizeBytes = metadata.fileSizeBytes,
            onClose = onExportComplete
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Export") },
                navigationIcon = {
                    IconButton(onClick = { if (isExporting) cancelExport() else onClose() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = if (isExporting) "Cancel export" else "Close"
                        )
                    }
                }
            )
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("MP4 export", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Clipp will render the source timeline, trims, speed, crop, filters, basic visual layers, supported layer animations, fades, audio volume envelopes, and audio tracks into a real MP4.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Info, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (unsupportedReasons.isNotEmpty()) {
                                    "This project still contains export-blocked items: ${unsupportedReasons.distinct().joinToString(", ")}. Remove them before exporting."
                                } else {
                                    "The current renderer includes clip positioning and transform keyframes, static text, captions, supported text/sticker/image-overlay animations, drawings, frames, image overlays, crop, filters, blur, supported fades, audio volume keyframes/fade envelopes, and separate audio tracks."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                item {
                    Text("${clips.size} source clip(s) · ${formatExportDuration(videoDurationMs)}", style = MaterialTheme.typography.titleMedium)
                }
                if (errorMessage != null) {
                    item {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (isExporting) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rendering MP4... $progress%", fontWeight = FontWeight.SemiBold)
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Keep Clipp open until rendering finishes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = ::cancelExport) {
                                Text("Cancel export")
                            }
                        }
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        errorMessage = null
                        progress = 0
                        isExporting = true
                        activeExportHandle = exporter.export(
                            clips = clips,
                            editorState = editorState,
                            onProgress = { progress = it },
                            onSuccess = { publishedUri, metadataValue ->
                                isExporting = false
                                activeExportHandle = null
                                outputUri = publishedUri
                                outputMetadata = metadataValue
                                com.example.utils.AnalyticsManager.trackVideoExported(metadataValue.durationMs)
                            },
                            onError = { message ->
                                isExporting = false
                                activeExportHandle = null
                                errorMessage = message
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                    enabled = !isExporting && clips.isNotEmpty() && unsupportedReasons.isEmpty()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isExporting) "Rendering..." else "Export MP4", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatExportDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
